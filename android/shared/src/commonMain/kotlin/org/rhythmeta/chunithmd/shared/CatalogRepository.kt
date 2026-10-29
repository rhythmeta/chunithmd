package org.rhythmeta.chunithmd.shared

import org.rhythmeta.chunithmd.shared.localization.tr

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    /**
     * Marks that some jackets may still be missing on disk. The catalog itself is usable without
     * them, so the marker only decides whether the image pass should run again — including after the
     * process was killed mid-download.
     */
    private val imageSyncPendingPath: Path = cacheDirectory.toPath() / "jackets-sync-pending"
    private val imageSyncMutex = Mutex()

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

    /** True while jacket images may still be missing, so a sync should be (re)started. */
    fun isImageSyncPending(): Boolean = fileSystem.exists(imageSyncPendingPath)

    private fun setImageSyncPending(pending: Boolean) {
        if (pending) {
            fileSystem.createDirectories(imageSyncPendingPath.parent!!)
            fileSystem.write(imageSyncPendingPath) { writeUtf8("1") }
        } else {
            fileSystem.delete(imageSyncPendingPath, mustExist = false)
        }
    }

    suspend fun checkForUpdate(): CatalogUpdateCheck {
        val manifest = fetchManifest()
        val cached = runCatching { loadLocal()?.manifest }.getOrNull()
        return CatalogUpdateCheck(manifest, cached?.sha256 != manifest.sha256)
    }

    private suspend fun fetchManifest(): StaticManifest {
        val response = client.get(manifestUrl)
        check(response.status.isSuccess()) { tr("Manifest request failed: HTTP {0}.", response.status.value) }
        val manifest = CatalogJson.decodeManifest(response.bodyAsText())
        validateManifest(manifest)
        return manifest
    }

    /**
     * Downloads and applies the catalog bundle only.
     *
     * Jacket images are deliberately left out: every cover falls back to a network fetch, so the
     * images are a cache warm-up rather than a correctness requirement and must never block the app
     * from opening. [downloadMissingJackets] picks them up afterwards.
     */
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
            expectedTotalBytes = check.manifest.sizes?.bundleBytes?.takeIf { it > 0L },
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
        jacketNames(bundle)
        onState(CatalogSyncState(CatalogSyncStage.Applying))
        val snapshot = CachedSnapshot(check.manifest, bundleJson)
        fileSystem.createDirectories(snapshotPath.parent!!)
        val temporary = ("$snapshotPath.tmp").toPath()
        fileSystem.write(temporary) { writeUtf8(CatalogJson.encodeSnapshot(snapshot)) }
        fileSystem.atomicMove(temporary, snapshotPath)
        // The catalog is committed at this point. Record the image work before anything else can
        // fail, so a broken image download never forces the whole catalog to be fetched again.
        setImageSyncPending(true)
        onState(CatalogSyncState(CatalogSyncStage.Ready))
        return snapshot
    }

    /**
     * Downloads every jacket that is still missing from the local cache.
     *
     * Per-image failures are tolerated: a run that reaches the end counts as a success even when
     * some files could not be fetched, and every missing file keeps working through the manifest
     * URL. Only a run that dies early leaves [isImageSyncPending] set, so the next launch retries.
     */
    suspend fun downloadMissingJackets(onState: (CatalogSyncState) -> Unit = {}): Boolean = imageSyncMutex.withLock {
        val snapshot = runCatching { loadLocal() }.getOrNull() ?: return@withLock false
        val manifest = try {
            fetchManifest()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // A cold process may not be able to reach the manifest; the cached one still describes
            // the jacket tree that the catalog refers to.
            snapshot.manifest
        }

        var reachedEnd = false
        try {
            val imageNames = jacketNames(CatalogJson.decodeBundle(snapshot.bundleJson))
            downloadJackets(manifest, imageNames, onState)
            reachedEnd = true
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        } finally {
            if (reachedEnd) setImageSyncPending(false)
        }
    }

    private suspend fun downloadJackets(
        manifest: StaticManifest,
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
        val baseUrl = manifest.assets.jacketBaseUrl.trimEnd('/')
        require(baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
            tr("Manifest has an invalid jacket base URL.")
        }
        fileSystem.createDirectories(jacketDirectory)
        val reporter = DownloadProgressReporter(
            stage = CatalogSyncStage.Downloading,
            totalItems = missingNames.size,
            message = tr("正在下载封面"),
            // Start from what is already on disk so a resumed run, or a settings-page update,
            // reports "how much of the catalog I have" instead of "how much moved this run".
            baselineBytes = (manifest.sizes?.bundleBytes ?: 0L) + installedJacketBytes(imageNames),
            expectedTotalBytes = manifest.sizes?.totalBytes?.takeIf { it > 0L },
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
                        val temporary = ("$destination.tmp").toPath()
                        fileSystem.write(temporary) { write(bytes) }
                        fileSystem.atomicMove(temporary, destination)
                        reporter.onItemCompleted()
                    }
                }
            }.awaitAll()
        }
        reporter.complete()
    }

    /** On-disk size of the jackets that are already cached, used as the resume baseline. */
    private fun installedJacketBytes(imageNames: Collection<String>): Long {
        if (imageNames.isEmpty()) return 0L
        val expected = imageNames.toHashSet()
        return runCatching {
            fileSystem.listOrNull(jacketDirectory).orEmpty()
                .filter { it.name in expected }
                .sumOf { fileSystem.metadataOrNull(it)?.size ?: 0L }
        }.getOrDefault(0L)
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

/** Callback facade retained for the native Swift catalog store. */
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

    fun query(
        bundleJson: String,
        search: String,
        sort: String,
        ascending: Boolean,
        categories: List<String>,
        versions: List<String>,
        difficulties: List<String>,
        types: List<String>,
        playableOnly: Boolean,
        hideDeleted: Boolean = false,
        playableRegion: String = "jp",
        favoriteSongIds: List<String> = emptyList(),
        favoritesOnly: Boolean = false,
    ): String = CatalogQuery.searchAndFilterJson(
        bundleJson, search, sort, ascending, categories, versions, difficulties, types, playableOnly,
        hideDeleted, playableRegion, favoriteSongIds, favoritesOnly,
    )

    fun refresh(completion: (String?, String?) -> Unit) {
        refreshWithProgress({}, completion)
    }

    /** Downloads the catalog bundle only; jacket images continue through [downloadMissingImages]. */
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

    /** True while jacket images may still be missing, so the host should warm the cache again. */
    fun isImageSyncPending(): Boolean = runCatching { repository.isImageSyncPending() }.getOrDefault(false)

    /**
     * Warms the jacket cache without touching the catalog. [completion] receives `null` when the run
     * reached the end (even if some images could not be fetched), otherwise a message to show.
     */
    fun downloadMissingImages(onProgress: (String) -> Unit, completion: (String?) -> Unit) {
        scope.launch {
            try {
                val reachedEnd = repository.downloadMissingJackets { state ->
                    onProgress(CatalogJson.codec.encodeToString(state))
                }
                completion(if (reachedEnd) null else tr("封面下载未完成，将在下次启动时继续。"))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                completion(error.message ?: tr("封面下载失败"))
            }
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
