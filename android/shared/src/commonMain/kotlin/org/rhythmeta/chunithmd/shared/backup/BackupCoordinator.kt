package org.rhythmeta.chunithmd.shared.backup

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.rhythmeta.chunithmd.shared.account.CloudBackup
import org.rhythmeta.chunithmd.shared.account.RhythmetaClient

/** Platform adapters own durable writes; this shared coordinator owns replacement and rollback. */
interface LocalSnapshotStore {
    suspend fun exportSnapshot(): BackupSnapshot
    suspend fun replaceSnapshot(snapshot: BackupSnapshot)
    suspend fun readRollback(): ByteArray?
    suspend fun writeRollback(bytes: ByteArray)
    suspend fun clearRollback()
}
interface BackupRemote {
    suspend fun backup(snapshot: BackupSnapshot, deviceName: String)
    suspend fun download(backup: CloudBackup): BackupSnapshot
}
class BackupCoordinator(private val client: BackupRemote, private val store: LocalSnapshotStore) {
    private val mutex = Mutex()
    private val mutableBusy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = mutableBusy
    private suspend fun <T> operation(block: suspend () -> T): T = mutex.withLock {
        mutableBusy.value = true
        try { withContext(Dispatchers.Default) { block() } } finally { mutableBusy.value = false }
    }
    suspend fun recover() = operation { recoverPending() }
    private suspend fun recoverPending() {
        store.readRollback()?.let { bytes ->
            withContext(NonCancellable) {
                store.replaceSnapshot(BackupCodec.decode(bytes))
                store.clearRollback()
            }
        }
    }
    suspend fun backup(deviceName: String) = operation {
        recoverPending()
        client.backup(store.exportSnapshot(), deviceName)
    }
    suspend fun restore(backup: CloudBackup) = operation {
        recoverPending()
        val target = client.download(backup)
        val previous = store.exportSnapshot()
        store.writeRollback(BackupCodec.encode(previous))
        withContext(NonCancellable) {
            try {
                store.replaceSnapshot(target)
                store.clearRollback()
            } catch (error: Throwable) {
                try { store.replaceSnapshot(previous); store.clearRollback() }
                catch (rollbackError: Throwable) { error.addSuppressed(rollbackError) }
                throw error
            }
        }
    }
}
