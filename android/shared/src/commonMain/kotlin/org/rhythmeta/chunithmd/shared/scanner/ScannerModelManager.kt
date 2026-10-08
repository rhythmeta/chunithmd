package org.rhythmeta.chunithmd.shared.scanner

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

@Serializable
data class ScannerModelState(
    val stage: String = "loading",
    val usable: Boolean = false,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val offline: Boolean = false,
    val error: String? = null,
)

/** Both UIs observe this state machine; cancellation never replaces the active revision. */
class ScannerModelManager(private val repository: ScannerModelRepository) {
    constructor(directory: String, platform: String) : this(ScannerModelRepository(directory, platform))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutable = MutableStateFlow(ScannerModelState())
    val state: StateFlow<ScannerModelState> = mutable.asStateFlow()
    private val current = MutableStateFlow<ScannerModelSnapshot?>(null)
    val snapshot: ScannerModelSnapshot? get() = current.value
    private var pending: ScannerModelManifest? = null
    private var operation: Job? = null
    private var prepared = false

    /** Called by screen lifecycle. Restore locally first and inspect updates once per manager. */
    fun prepare() = start {
        if (prepared) return@start
        prepared = true
        inspect(silent = true)
    }

    fun check() = start { inspect(silent = false) }

    private suspend fun inspect(silent: Boolean) {
        mutable.value = ScannerModelState(stage = if (silent) "loading" else "checking", usable = snapshot != null)
        try {
            current.value = repository.loadCached()
            // A network request must not keep an already usable camera behind a checking panel.
            mutable.value = ScannerModelState(stage = if (snapshot != null && silent) "ready" else "checking",
                usable = snapshot != null)
            val manifest = repository.fetchManifest()
            pending = manifest
            mutable.value = ScannerModelState(
                stage = if (repository.matches(manifest, snapshot)) "ready" else if (snapshot != null) "update" else "required",
                usable = snapshot != null, totalBytes = manifest.entries.sumOf { it.size })
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            mutable.value = ScannerModelState(if (snapshot != null) "ready" else "failed", usable = snapshot != null,
                offline = snapshot != null, error = e.message)
        }
    }

    fun download() = start {
        val manifest = pending ?: return@start
        val previous = mutable.value
        mutable.value = ScannerModelState("downloading", snapshot != null, totalBytes = manifest.entries.sumOf { it.size })
        try {
            current.value = repository.download(manifest) { bytes, total ->
                mutable.value = mutable.value.copy(downloadedBytes = bytes, totalBytes = total)
            }
            mutable.value = ScannerModelState("ready", usable = true)
        } catch (e: CancellationException) {
            mutable.value = previous
            throw e
        } catch (e: Exception) {
            mutable.value = mutable.value.copy(stage = "failed", usable = snapshot != null, error = e.message)
        }
    }

    private fun start(block: suspend CoroutineScope.() -> Unit) {
        if (operation?.isActive == true) return
        operation = scope.launch(block = block)
    }
    fun cancelDownload() { if (mutable.value.stage == "downloading") operation?.cancel() }
    fun close() { scope.cancel(); repository.close() }
}

/** Callback ownership/cancellation stays here instead of duplicating download logic in Swift. */
class ScannerModelBridge(directory: String) {
    private val manager = ScannerModelManager(directory, "ios")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observer: Job? = null
    fun observe(onState: (String) -> Unit) {
        observer?.cancel()
        observer = scope.launch { manager.state.collect { onState(scannerModelJson.encodeToString(it)) } }
    }
    fun check() = manager.check()
    fun prepare() = manager.prepare()
    fun download() = manager.download()
    fun cancelDownload() = manager.cancelDownload()
    fun snapshotJson(): String? = manager.snapshot?.let { scannerModelJson.encodeToString(it) }
    fun close() { scope.cancel(); manager.close() }
}
