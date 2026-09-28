package org.rhythmeta.chunithmd.shared

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString

const val StaticBaseUrl = "https://chunithmd-assets.rhythmeta.org"
const val ManifestUrl = "$StaticBaseUrl/manifest.json"

enum class CatalogSyncStage {
    Idle,
    Checking,
    Downloading,
    Validating,
    Applying,
    Ready,
    Failed,
}

data class CatalogSyncState(
    val stage: CatalogSyncStage = CatalogSyncStage.Idle,
    val message: String? = null,
    val progress: Float? = null,
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

    fun loadLocal(): CachedSnapshot? {
        if (!fileSystem.exists(snapshotPath)) return null
        val snapshot = CatalogJson.decodeSnapshot(fileSystem.read(snapshotPath) { readUtf8() })
        val bytes = snapshot.bundleJson.encodeUtf8().toByteArray()
        require(sha256(bytes) == snapshot.manifest.sha256) { "Cached catalog hash does not match its manifest." }
        val bundle = CatalogJson.decodeBundle(snapshot.bundleJson)
        requireSupportedBundle(bundle)
        return snapshot
    }

    suspend fun checkForUpdate(): CatalogUpdateCheck {
        val response = client.get(manifestUrl)
        check(response.status.isSuccess()) { "Manifest request failed: HTTP ${response.status.value}." }
        val manifest = CatalogJson.decodeManifest(response.bodyAsText())
        validateManifest(manifest)
        val cached = runCatching { loadLocal()?.manifest }.getOrNull()
        return CatalogUpdateCheck(manifest, cached?.sha256 != manifest.sha256)
    }

    suspend fun downloadAndApply(onState: (CatalogSyncState) -> Unit = {}): CachedSnapshot {
        onState(CatalogSyncState(CatalogSyncStage.Checking))
        val check = checkForUpdate()
        onState(CatalogSyncState(CatalogSyncStage.Downloading))
        val response = client.get(StaticBaseUrl + check.manifest.bundle)
        check(response.status.isSuccess()) { "Bundle request failed: HTTP ${response.status.value}." }
        val bytes: ByteArray = response.body()
        onState(CatalogSyncState(CatalogSyncStage.Validating))
        require(sha256(bytes) == check.manifest.sha256) { "Downloaded catalog hash does not match its manifest." }
        val bundleJson = bytes.decodeToString()
        val bundle = CatalogJson.decodeBundle(bundleJson)
        requireSupportedBundle(bundle)
        onState(CatalogSyncState(CatalogSyncStage.Applying))
        val snapshot = CachedSnapshot(check.manifest, bundleJson)
        fileSystem.createDirectories(snapshotPath.parent!!)
        val temporary = ("$snapshotPath.tmp").toPath()
        fileSystem.write(temporary) { writeUtf8(CatalogJson.encodeSnapshot(snapshot)) }
        fileSystem.atomicMove(temporary, snapshotPath)
        onState(CatalogSyncState(CatalogSyncStage.Ready))
        return snapshot
    }

    private fun validateManifest(manifest: StaticManifest) {
        require(manifest.schemaVersion == 1) { "Unsupported manifest schema version ${manifest.schemaVersion}." }
        require(manifest.product == "chunithmd") { "Manifest is for ${manifest.product}, not chunithmd." }
        require(manifest.sha256.matches(Regex("[a-fA-F0-9]{64}"))) { "Manifest has an invalid SHA-256 hash." }
        require(manifest.bundle.startsWith("/bundles/") && manifest.bundle.endsWith(".json")) {
            "Manifest has an invalid bundle path."
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

    fun query(bundleJson: String, search: String, sort: String, ascending: Boolean,
              categories: List<String>, versions: List<String>, difficulties: List<String>, types: List<String>,
              playableOnly: Boolean): String = CatalogQuery.searchAndFilterJson(
        bundleJson, search, sort, ascending, categories, versions, difficulties, types, playableOnly,
    )

    fun refresh(completion: (String?, String?) -> Unit) {
        scope.launch {
            runCatching { repository.downloadAndApply() }
                .onSuccess { completion(CatalogJson.encodeBundle(CatalogJson.decodeBundle(it.bundleJson)), null) }
                .onFailure { completion(null, it.message ?: "Catalog update failed.") }
        }
    }

    fun checkForUpdate(completion: (String?, String?) -> Unit) {
        scope.launch {
            runCatching { repository.checkForUpdate() }
                .onSuccess { completion(CatalogJson.codec.encodeToString(it), null) }
                .onFailure { completion(null, it.message ?: "Could not check for updates.") }
        }
    }
}
