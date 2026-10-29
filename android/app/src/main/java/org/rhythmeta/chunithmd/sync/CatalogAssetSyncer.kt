package org.rhythmeta.chunithmd.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.rhythmeta.chunithmd.shared.CatalogRepository
import org.rhythmeta.chunithmd.shared.CatalogSyncState

/**
 * Warms the on-disk jacket cache.
 *
 * This is deliberately decoupled from the catalog download: the app becomes usable as soon as the
 * bundle is imported, while jackets are a best-effort cache that every cover falls back to over the
 * network for. [CatalogAssetSyncService] keeps a run alive after the user leaves the app, and both
 * the service and the UI observe [progress] through this single instance.
 */
class CatalogAssetSyncer(private val repository: CatalogRepository) {
    private val mutex = Mutex()
    private val mutableProgress = MutableStateFlow<CatalogSyncState?>(null)

    /** Non-null only while a run is in flight, so observers can drive a progress notification. */
    val progress: StateFlow<CatalogSyncState?> = mutableProgress.asStateFlow()

    /** True while jacket images may still be missing, including after a killed download. */
    fun isPending(): Boolean = runCatching { repository.isImageSyncPending() }.getOrDefault(false)

    /**
     * Downloads every missing jacket. Returns true when the run reached the end, even if some files
     * could not be fetched; the pending marker then decides whether the next launch retries.
     */
    suspend fun sync(): Boolean {
        if (!isPending()) return false
        return mutex.withLock {
            try {
                repository.downloadMissingJackets { state -> mutableProgress.value = state }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            } finally {
                mutableProgress.value = null
            }
        }
    }
}
