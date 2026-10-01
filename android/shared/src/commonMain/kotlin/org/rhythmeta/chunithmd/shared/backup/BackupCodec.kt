@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package org.rhythmeta.chunithmd.shared.backup

import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.decodeFromByteArray
import okio.ByteString.Companion.toByteString

expect fun gzipBackup(bytes: ByteArray, compress: Boolean): ByteArray
expect fun secureRandomBytes(count: Int): ByteArray

object BackupCodec {
    const val MAX_COMPRESSED = 64 * 1024 * 1024
    const val MAX_RAW = 512 * 1024 * 1024
    private val format = ProtoBuf { encodeDefaults = false }
    fun raw(snapshot: BackupSnapshot): ByteArray { validate(snapshot); return format.encodeToByteArray(snapshot).also { require(it.size <= MAX_RAW) } }
    fun encode(snapshot: BackupSnapshot): ByteArray = gzipBackup(raw(snapshot), true).also { require(it.size <= MAX_COMPRESSED) }
    fun decode(bytes: ByteArray, expectedSize: Long? = null): BackupSnapshot {
        require(bytes.size <= MAX_COMPRESSED)
        val raw = gzipBackup(bytes, false)
        require(raw.size <= MAX_RAW && (expectedSize == null || raw.size.toLong() == expectedSize)) { "Backup size does not match." }
        return format.decodeFromByteArray<BackupSnapshot>(raw).also(::validate)
    }
    fun sha256(bytes: ByteArray): String = bytes.toByteString().sha256().hex()
    fun validate(snapshot: BackupSnapshot) {
        require(snapshot.magic == "RHYTHMETA_BACKUP" && snapshot.formatVersion == 1 && snapshot.game == "chunithmd") { "Unsupported backup format or game." }
        require(snapshot.profiles.size in 1..10000 && snapshot.playRecords.size <= 2_000_000)
        val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        fun ids(values: List<String>) { require(values.toSet().size == values.size && values.all(uuid::matches)) { "Invalid record IDs." } }
        ids(snapshot.profiles.map { it.id }); require(snapshot.profiles.count { it.active } == 1)
        require(snapshot.profiles.all { it.avatar.size <= 16 * 1024 * 1024 })
        val profileIds = snapshot.profiles.map { it.id }.toSet()
        fun score(value: BackupScore) { require(value.profileId in profileIds && value.score in 0..1_010_000 && value.chartKey.isNotBlank() && value.songId.isNotBlank()) { "Invalid score reference." } }
        snapshot.scores.forEach(::score); snapshot.playRecords.forEach { score(it.result) }; ids(snapshot.playRecords.map { it.id })
        ids(snapshot.collections.map { it.id }); ids(snapshot.collectionItems.map { it.id })
        val collectionIds = snapshot.collections.map { it.id }.toSet()
        require(snapshot.collectionItems.all { it.collectionId in collectionIds && it.songId.isNotBlank() })
        require(snapshot.settings.size <= 1000 && snapshot.settings.map { it.key }.toSet().size == snapshot.settings.size)
        snapshot.settings.forEach(BackupSettings::validate)
    }
}
