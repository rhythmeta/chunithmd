package org.rhythmeta.chunithmd.shared.scanner

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.*
import okio.Path.Companion.toPath
import okio.ByteString.Companion.encodeUtf8

internal val scannerModelJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
const val ScannerModelBaseUrl = "https://chunithmd-models.rhythmeta.org"

@Serializable
data class ScannerModelEntry(val filename: String, val sha256: String, val size: Long)
@Serializable
data class ScannerModelManifest(val schemaVersion: Int, val entries: List<ScannerModelEntry>)
@Serializable
data class ScannerModelSnapshot(val revision: String, val directory: String) {
    fun file(filename: String): String = (directory.toPath() / filename).toString()
}

/** Shared download/cache contract. A revision is activated only after every file passes SHA-256. */
class ScannerModelRepository(
    directory: String,
    private val platform: String,
    private val client: HttpClient = HttpClient {
        install(HttpTimeout) { connectTimeoutMillis = 15_000; socketTimeoutMillis = 120_000; requestTimeoutMillis = 600_000 }
    },
    private val baseUrl: String = ScannerModelBaseUrl,
    private val fs: FileSystem = FileSystem.SYSTEM,
) {
    private val root = directory.toPath()
    private val active = root / "active-$platform.json"
    private val mutex = Mutex()
    private val required = when (platform) {
        "android" -> setOf("ScoreDetector.onnx", "PaddleOCRv6Small.onnx", "PaddleOCRv6SmallVocab.json")
        "ios" -> setOf("ScoreDetector.mlpackage/Manifest.json", "ScoreDetector.mlpackage/Data/com.apple.CoreML/model.mlmodel",
            "ScoreDetector.mlpackage/Data/com.apple.CoreML/weights/weight.bin")
        else -> error("Unknown scanner platform")
    }

    internal fun validate(manifest: ScannerModelManifest): ScannerModelManifest {
        require(manifest.schemaVersion == 1 && manifest.entries.size == required.size &&
            manifest.entries.map { it.filename }.toSet() == required) { "Unsupported model manifest" }
        require(manifest.entries.all { it.sha256.matches(Regex("[a-f0-9]{64}")) && it.size in 1..100_000_000 }) { "Invalid model manifest" }
        return manifest.copy(entries = manifest.entries.sortedBy { it.filename })
    }

    suspend fun fetchManifest(): ScannerModelManifest = client.prepareGet("$baseUrl/$platform.json").execute { response ->
        check(response.status.isSuccess()) { "Model manifest HTTP ${response.status.value}" }
        validate(scannerModelJson.decodeFromString<ScannerModelManifest>(response.bodyAsText()))
    }

    private fun revision(manifest: ScannerModelManifest) = scannerModelJson.encodeToString(manifest).encodeUtf8().sha256().hex()
    private fun snapshot(manifest: ScannerModelManifest) = ScannerModelSnapshot(revision(manifest), (root / "revisions" / revision(manifest)).toString())
    private fun valid(path: Path, entry: ScannerModelEntry): Boolean = runCatching {
        if (fs.metadataOrNull(path)?.size != entry.size) return@runCatching false
        val hashing = HashingSource.sha256(fs.source(path))
        hashing.buffer().use { it.readAll(blackholeSink()) }
        hashing.hash.hex() == entry.sha256
    }.getOrDefault(false)

    suspend fun loadCached(): ScannerModelSnapshot? = mutex.withLock {
        val manifest = runCatching { validate(scannerModelJson.decodeFromString<ScannerModelManifest>(fs.read(active) { readUtf8() })) }.getOrNull()
            ?: return@withLock null
        val cached = snapshot(manifest)
        cached.takeIf { manifest.entries.all { valid(cached.file(it.filename).toPath(), it) } }
    }

    fun matches(manifest: ScannerModelManifest, cached: ScannerModelSnapshot?) = cached?.revision == revision(manifest)

    suspend fun download(manifest: ScannerModelManifest, progress: (Long, Long) -> Unit): ScannerModelSnapshot = mutex.withLock {
        val checked = validate(manifest)
        val target = snapshot(checked)
        val total = checked.entries.sumOf { it.size }
        var complete = 0L
        for (entry in checked.entries) {
            currentCoroutineContext().ensureActive()
            val objectPath = root / "objects" / entry.sha256
            fs.createDirectories(objectPath.parent!!)
            if (!valid(objectPath, entry)) {
                val temporary = root / "objects" / "${entry.sha256}.download"
                try {
                    client.prepareGet("$baseUrl/files/${entry.sha256}").execute { response ->
                        check(response.status.isSuccess()) { "Model download HTTP ${response.status.value}" }
                        val channel = response.bodyAsChannel()
                        var received = 0L
                        fs.sink(temporary).buffer().use { sink ->
                            val chunk = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = channel.readAvailable(chunk, 0, chunk.size)
                                if (count == -1) break
                                if (count == 0) continue
                                received += count
                                check(received <= entry.size) { "Model size mismatch: ${entry.filename}" }
                                sink.write(chunk, 0, count)
                                progress(complete + received, total)
                            }
                        }
                    }
                    check(valid(temporary, entry)) { "Model verification failed: ${entry.filename}" }
                    fs.atomicMove(temporary, objectPath)
                } finally { fs.delete(temporary, mustExist = false) }
            }
            val destination = target.file(entry.filename).toPath()
            fs.createDirectories(destination.parent!!)
            if (!valid(destination, entry)) fs.copy(objectPath, destination)
            complete += entry.size
            progress(complete, total)
        }
        currentCoroutineContext().ensureActive()
        check(checked.entries.all { valid(target.file(it.filename).toPath(), it) }) { "Model verification failed" }
        val temporary = root / "active-$platform.tmp"
        try {
            fs.write(temporary) { writeUtf8(scannerModelJson.encodeToString(checked)) }
            fs.atomicMove(temporary, active)
        } finally { fs.delete(temporary, mustExist = false) }
        target
    }

    fun close() = client.close()
}
