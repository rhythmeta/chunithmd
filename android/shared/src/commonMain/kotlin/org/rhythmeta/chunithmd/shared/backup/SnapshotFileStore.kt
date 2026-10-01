package org.rhythmeta.chunithmd.shared.backup

import kotlin.time.Clock

/** The iOS catalog shell has no personal database yet. Retain every portable field durably. */
class SnapshotFile(val bytes: ByteArray?)
interface SnapshotFiles {
    @Throws(Exception::class) fun read(name: String): SnapshotFile
    @Throws(Exception::class) fun write(name: String, bytes: ByteArray)
    @Throws(Exception::class) fun delete(name: String)
}
class SnapshotFileStore(private val files: SnapshotFiles, private val clientVersion: String) : LocalSnapshotStore {
    override suspend fun exportSnapshot(): BackupSnapshot {
        val stored = files.read("personal.pb.gz").bytes?.let { BackupCodec.decode(it) } ?: initial().also {
            files.write("personal.pb.gz", BackupCodec.encode(it))
        }
        return stored.copy(createdAt=Clock.System.now().toEpochMilliseconds(), clientVersion=clientVersion)
    }
    override suspend fun replaceSnapshot(snapshot: BackupSnapshot) { files.write("personal.pb.gz", BackupCodec.encode(snapshot)) }
    override suspend fun readRollback() = files.read("restore-pending.pb.gz").bytes
    override suspend fun writeRollback(bytes: ByteArray) { files.write("restore-pending.pb.gz", bytes) }
    override suspend fun clearRollback() { files.delete("restore-pending.pb.gz") }
    private fun initial(): BackupSnapshot {
        val bytes = secureRandomBytes(16)
        bytes[6] = ((bytes[6].toInt() and 15) or 64).toByte()
        bytes[8] = ((bytes[8].toInt() and 63) or 128).toByte()
        val hex = bytes.joinToString("") { (it.toInt() and 255).toString(16).padStart(2,'0') }
        val id = "${hex.take(8)}-${hex.substring(8,12)}-${hex.substring(12,16)}-${hex.substring(16,20)}-${hex.substring(20)}"
        val now = Clock.System.now().toEpochMilliseconds()
        return BackupSnapshot(magic="RHYTHMETA_BACKUP",formatVersion=1,game="chunithmd",createdAt=now,clientVersion=clientVersion,
            profiles=listOf(BackupProfile(id=id,name="我的档案",server="jp",active=true,createdAt=now)))
    }
}
