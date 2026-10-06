package org.rhythmeta.chunithmd.shared

import org.rhythmeta.chunithmd.shared.localization.tr

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Buffer
import okio.Path
import okio.Path.Companion.toPath
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

const val StaticBaseUrl = "https://chunithmd-assets.rhythmeta.org"
const val ManifestUrl = "$StaticBaseUrl/manifest.json"

@Serializable
enum class CatalogSyncStage {
    Idle,
    Checking,
    Downloading,
    Validating,
    Applying,
    Ready,
    Failed,
}

@Serializable
data class CatalogSyncState(
    val stage: CatalogSyncStage = CatalogSyncStage.Idle,
    val message: String? = null,
    val progress: Float? = null,
    val completedItems: Int = 0,
    val totalItems: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val bytesPerSecond: Long = 0L,
    val updateAvailable: Boolean = false,
)

private class DownloadSpeedTracker {
    private data class Sample(val timestampMillis: Long, val downloadedBytes: Long)

    private val samples = ArrayDeque<Sample>()
    private val startedAt = kotlin.time.TimeSource.Monotonic.markNow()
    private var downloadedBytes = 0L

    fun addBytes(byteCount: Long): Long {
        val now = startedAt.elapsedNow().inWholeMilliseconds
        downloadedBytes += byteCount.coerceAtLeast(0L)
        samples.addLast(Sample(now, downloadedBytes))
        while (samples.size > 2 && samples[1].timestampMillis <= samples.last().timestampMillis - 2_000L) {
            samples.removeFirst()
        }
        val oldest = samples.first()
        val elapsedMillis = samples.last().timestampMillis - oldest.timestampMillis
        return if (elapsedMillis > 0L) {
            ((downloadedBytes - oldest.downloadedBytes) * 1_000L / elapsedMillis).coerceAtLeast(0L)
        } else {
            0L
        }
    }
}

private fun formatCatalogBytes(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.coerceAtLeast(0L).toDouble()
    var unit = 0
    while (value >= 1024.0 && unit < units.lastIndex) {
        value /= 1024.0
        unit += 1
    }
    val rounded = if (unit == 0) value.toLong().toString() else {
        val tenths = (value * 10).toLong()
        "${tenths / 10}.${tenths % 10}"
    }
    return "$rounded ${units[unit]}"
}

@Serializable
data class CatalogUpdateCheck(
    val manifest: StaticManifest,
    val updateAvailable: Boolean,
)

class CatalogRepository(
    cacheDirectory: String,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
    private val client: HttpClient = HttpClient {
        expectSuccess = false
    },
    private val manifestUrl: String = ManifestUrl,
) {
    private val snapshotPath: Path = cacheDirectory.toPath() / "catalog-snapshot.json"
    private val jacketDirectory: Path = cacheDirectory.toPath() / "jackets"

    fun loadLocal(): CachedSnapshot? {
        if (!fileSystem.exists(snapshotPath)) return null
        val snapshot = CatalogJson.decodeSnapshot(fileSystem.read(snapshotPath) { readUtf8() })
        val bytes = snapshot.bundleJson.encodeUtf8().toByteArray()
        require(sha256(bytes) == snapshot.manifest.sha256) { tr("Cached catalog hash does not match its manifest.") }
        val bundle = CatalogJson.decodeBundle(snapshot.bundleJson)
        requireSupportedBundle(bundle)
        jacketNames(bundle)
        return snapshot
    }

    /** Returns an existing cached jacket path; callers can fall back to the manifest URL. */
    fun localJacketPath(imageName: String): String? {
        val path = jacketPath(imageName) ?: return null
        return path.toString().takeIf { fileSystem.exists(path) }
    }

    suspend fun checkForUpdate(): CatalogUpdateCheck {
        val response = client.get(manifestUrl)
        check(response.status.isSuccess()) { tr("Manifest request failed: HTTP {0}.", response.status.value) }
        val manifest = CatalogJson.decodeManifest(response.bodyAsText())
        validateManifest(manifest)
        val cached = runCatching { loadLocal()?.manifest }.getOrNull()
        return CatalogUpdateCheck(manifest, cached?.sha256 != manifest.sha256)
    }

    suspend fun downloadAndApply(onState: (CatalogSyncState) -> Unit = {}): CachedSnapshot {
        onState(CatalogSyncState(CatalogSyncStage.Checking))
        val check = checkForUpdate()
        val response = client.get(StaticBaseUrl + check.manifest.bundle)
        check(response.status.isSuccess()) { tr("Bundle request failed: HTTP {0}.", response.status.value) }
        val bundleReporter = DownloadProgressReporter(
            stage = CatalogSyncStage.Downloading,
            totalItems = 1,
            message = tr("正在下载静态数据"),
            progressByBytes = true,
            overallStart = 0f,
            overallEnd = 0.35f,
            onState = onState,
        )
        bundleReporter.start()
        val bytes = readResponseBytes(response, bundleReporter::onTransfer)
        bundleReporter.onItemCompleted()
        bundleReporter.complete()
        onState(CatalogSyncState(CatalogSyncStage.Validating))
        require(sha256(bytes) == check.manifest.sha256) { tr("Downloaded catalog hash does not match its manifest.") }
        val bundleJson = bytes.decodeToString()
        val bundle = CatalogJson.decodeBundle(bundleJson)
        requireSupportedBundle(bundle)
        val imageNames = jacketNames(bundle)
        downloadJackets(check.manifest.assets.jacketBaseUrl, imageNames, onState)
        onState(CatalogSyncState(CatalogSyncStage.Applying))
        val snapshot = CachedSnapshot(check.manifest, bundleJson)
        fileSystem.createDirectories(snapshotPath.parent!!)
        val temporary = ("$snapshotPath.tmp").toPath()
        fileSystem.write(temporary) { writeUtf8(CatalogJson.encodeSnapshot(snapshot)) }
        fileSystem.atomicMove(temporary, snapshotPath)
        onState(CatalogSyncState(CatalogSyncStage.Ready))
        return snapshot
    }

    private class DownloadProgressReporter(
        private val stage: CatalogSyncStage,
        private val totalItems: Int,
        private val message: String,
        private val progressByBytes: Boolean = false,
        private val overallStart: Float = 0f,
        private val overallEnd: Float = 1f,
        private val onState: (CatalogSyncState) -> Unit,
    ) {
        private val mutex = Mutex()
        private val speedTracker = DownloadSpeedTracker()
        private var completedItems = 0
        private var downloadedBytes = 0L
        private var totalBytes = 0L
        private var bytesPerSecond = 0L

        suspend fun start() = publish()

        suspend fun onTransfer(byteCount: Long, expectedBytes: Long?) = mutex.withLock {
            if (expectedBytes != null) totalBytes += expectedBytes.coerceAtLeast(0L)
            downloadedBytes += byteCount.coerceAtLeast(0L)
            bytesPerSecond = speedTracker.addBytes(byteCount)
            publishLocked()
        }

        suspend fun onItemCompleted() = mutex.withLock {
            completedItems += 1
            publishLocked()
        }

        suspend fun complete() = mutex.withLock {
            completedItems = totalItems
            publishLocked(progress = overallEnd)
        }

        private suspend fun publish() = mutex.withLock { publishLocked() }

        private fun publishLocked(progress: Float? = calculatedProgress()) {
            onState(
                CatalogSyncState(
                    stage = stage,
                    message = progressMessage(),
                    progress = progress,
                    completedItems = completedItems,
                    totalItems = totalItems,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes.takeIf { it > 0L },
                    bytesPerSecond = bytesPerSecond,
                ),
            )
        }

        private fun progressMessage(): String {
            val speed = if (bytesPerSecond > 0L) "${formatCatalogBytes(bytesPerSecond)}/s" else tr("计算速度中")
            val bytes = totalBytes.takeIf { it > 0L }?.let { "${formatCatalogBytes(downloadedBytes)} / ${formatCatalogBytes(it)}" }
                ?: formatCatalogBytes(downloadedBytes)
            return "$message ($completedItems/$totalItems) · $bytes · $speed"
        }

        private fun calculatedProgress(): Float? = when {
            progressByBytes && totalBytes > 0L -> overallProgress(downloadedBytes.toFloat() / totalBytes)
            totalItems > 0 -> overallProgress(completedItems.toFloat() / totalItems)
            else -> null
        }

        private fun overallProgress(stageProgress: Float): Float =
            (overallStart + (overallEnd - overallStart) * stageProgress.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    }

    private suspend fun downloadJackets(
        jacketBaseUrl: String,
        imageNames: List<String>,
        onState: (CatalogSyncState) -> Unit,
    ) {
        val missingNames = imageNames.filter { localJacketPath(it) == null }
        if (missingNames.isEmpty()) {
            onState(
                CatalogSyncState(
                    stage = CatalogSyncStage.Downloading,
                    message = tr("封面已是最新"),
                    progress = 0.95f,
                ),
            )
            return
        }
        val baseUrl = jacketBaseUrl.trimEnd('/')
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            tr("Manifest has an invalid jacket base URL.")
        }
        fileSystem.createDirectories(jacketDirectory)
        val reporter = DownloadProgressReporter(
            stage = CatalogSyncStage.Downloading,
            totalItems = missingNames.size,
            message = tr("正在下载封面"),
            overallStart = 0.35f,
            overallEnd = 0.95f,
            onState = onState,
        )
        reporter.start()
        val limiter = Semaphore(permits = 8)
        coroutineScope {
            missingNames.map { imageName ->
                async {
                    limiter.withPermit {
                        val response = client.get("$baseUrl/$imageName")
                        check(response.status.isSuccess()) {
                            tr("Jacket request failed for {0}: HTTP {1}.", imageName, response.status.value)
                        }
                        val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
                        require(contentType.isEmpty() || contentType.startsWith("image/", ignoreCase = true)) {
                            tr("Jacket request for {0} returned non-image content type {1}.", imageName, contentType.ifEmpty { "unknown" })
                        }
                        val bytes = readResponseBytes(response, reporter::onTransfer)
                        require(bytes.isNotEmpty()) { tr("Jacket request for {0} returned an empty image.", imageName) }
                        val destination = jacketPath(imageName)!!
                        val temporary = (destination.toString() + ".tmp").toPath()
                        fileSystem.write(temporary) { write(bytes) }
                        fileSystem.atomicMove(temporary, destination)
                        reporter.onItemCompleted()
                    }
                }
            }.awaitAll()
        }
        reporter.complete()
    }

    private suspend fun readResponseBytes(
        response: io.ktor.client.statement.HttpResponse,
        onTransfer: suspend (Long, Long?) -> Unit,
    ): ByteArray {
        val totalBytes = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        onTransfer(0L, totalBytes)
        val channel = response.bodyAsChannel()
        val buffer = Buffer()
        val chunk = ByteArray(16 * 1024)
        while (!channel.isClosedForRead) {
            val count = channel.readAvailable(chunk, 0, chunk.size)
            if (count == -1) break
            if (count > 0) {
                buffer.write(chunk, 0, count)
                onTransfer(count.toLong(), null)
            }
        }
        return buffer.readByteArray()
    }

    private fun jacketNames(bundle: CatalogBundle): List<String> = bundle.catalog.songs
        .asSequence()
        .map { it.imageName }
        .filter(String::isNotBlank)
        .onEach(::requireSafeJacketName)
        .distinct()
        .toList()

    private fun jacketPath(imageName: String): Path? {
        if (imageName.isBlank()) return null
        requireSafeJacketName(imageName)
        return jacketDirectory / imageName
    }

    private fun requireSafeJacketName(imageName: String) {
        require(
            imageName != "." && imageName != ".." &&
                '/' !in imageName && '\\' !in imageName &&
                !imageName.startsWith("/"),
        ) { tr("Catalog contains an invalid jacket path: {0}", imageName) }
    }

    private fun validateManifest(manifest: StaticManifest) {
        require(manifest.schemaVersion == 1) { tr("Unsupported manifest schema version {0}.", manifest.schemaVersion) }
        require(manifest.product == "chunithmd") { tr("Manifest is for {0}, not chunithmd.", manifest.product) }
        require(manifest.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { tr("Manifest has an invalid SHA-256 hash.") }
        require(manifest.bundle.startsWith("/bundles/") && manifest.bundle.endsWith(".json")) {
            tr("Manifest has an invalid bundle path.")
        }
    }

    private fun sha256(bytes: ByteArray): String = bytes.toByteString().sha256().hex()
}

class CatalogBridge(cacheDirectory: String) {
    private val repository = CatalogRepository(cacheDirectory)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun loadSnapshotJson(): String? = runCatching {
        kotlinx.coroutines.runBlocking { repository.loadLocal() }
            ?.let { CatalogJson.encodeBundle(CatalogJson.decodeBundle(it.bundleJson)) }
    }.getOrNull()

    fun loadSnapshotMetadataJson(): String? = runCatching {
        kotlinx.coroutines.runBlocking { repository.loadLocal() }
            ?.let { CatalogJson.codec.encodeToString(it.manifest) }
    }.getOrNull()

    fun localJacketPath(imageName: String): String? = runCatching {
        repository.localJacketPath(imageName)
    }.getOrNull()

    fun query(bundleJson: String, search: String, sort: String, ascending: Boolean,
              categories: List<String>, versions: List<String>, difficulties: List<String>, types: List<String>,
              playableOnly: Boolean, hideDeleted: Boolean = false, playableRegion: String = "jp",
              favoriteSongIds: List<String> = emptyList(), favoritesOnly: Boolean = false): String = CatalogQuery.searchAndFilterJson(
        bundleJson, search, sort, ascending, categories, versions, difficulties, types, playableOnly,
        hideDeleted, playableRegion, favoriteSongIds, favoritesOnly,
    )

    fun refresh(completion: (String?, String?) -> Unit) {
        refreshWithProgress({}, completion)
    }

    fun refreshWithProgress(
        onProgress: (String) -> Unit,
        completion: (String?, String?) -> Unit,
    ) {
        scope.launch {
            runCatching {
                repository.downloadAndApply { state ->
                    onProgress(CatalogJson.codec.encodeToString(state))
                }
            }
                .onSuccess { completion(CatalogJson.encodeBundle(CatalogJson.decodeBundle(it.bundleJson)), null) }
                .onFailure { completion(null, it.message ?: tr("Catalog update failed.")) }
        }
    }

    @Throws(Exception::class)
    suspend fun checkForUpdateJson(): String = CatalogJson.codec.encodeToString(repository.checkForUpdate())

    fun checkForUpdate(completion: (String?, String?) -> Unit) {
        scope.launch {
            runCatching { checkForUpdateJson() }
                .onSuccess { completion(it, null) }
                .onFailure { completion(null, it.message ?: tr("Could not check for updates.")) }
        }
    }
}
