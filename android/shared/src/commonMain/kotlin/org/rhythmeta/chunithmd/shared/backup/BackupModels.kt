@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package org.rhythmeta.chunithmd.shared.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

// Wire contract: shared/backup.proto. Field numbers must never be reused.
@Serializable
data class BackupSnapshot(
    @ProtoNumber(1) val magic: String = "",
    @ProtoNumber(2) val formatVersion: Int = 0,
    @ProtoNumber(3) val game: String = "",
    @ProtoNumber(4) val createdAt: Long = 0,
    @ProtoNumber(5) val clientVersion: String = "",
    @ProtoNumber(6) val profiles: List<BackupProfile> = emptyList(),
    @ProtoNumber(7) val scores: List<BackupScore> = emptyList(),
    @ProtoNumber(8) val playRecords: List<BackupPlayRecord> = emptyList(),
    @ProtoNumber(9) val collections: List<BackupCollection> = emptyList(),
    @ProtoNumber(10) val collectionItems: List<BackupCollectionItem> = emptyList(),
    @ProtoNumber(11) val favoriteSongIds: List<String> = emptyList(),
    @ProtoNumber(12) val settings: List<BackupSetting> = emptyList(),
)

@Serializable
data class BackupProfile(
    @ProtoNumber(1) val id: String = "",
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val server: String = "",
    @ProtoNumber(4) val avatar: ByteArray = byteArrayOf(),
    @ProtoNumber(5) val avatarUrl: String = "",
    @ProtoNumber(6) val active: Boolean = false,
    @ProtoNumber(7) val createdAt: Long = 0,
    @ProtoNumber(8) val dfUsername: String = "",
    @ProtoNumber(9) val playerRating: Int = 0,
    @ProtoNumber(10) val plate: String = "",
    @ProtoNumber(11) val lastImportDf: Long = 0,
    @ProtoNumber(12) val lastImportLxns: Long = 0,
    @ProtoNumber(13) val b35Count: Int = 0,
    @ProtoNumber(14) val b15Count: Int = 0,
    @ProtoNumber(15) val b35RecLimit: Int = 0,
    @ProtoNumber(16) val b15RecLimit: Int = 0,
    @ProtoNumber(17) val title: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BackupProfile

        if (active != other.active) return false
        if (createdAt != other.createdAt) return false
        if (playerRating != other.playerRating) return false
        if (lastImportDf != other.lastImportDf) return false
        if (lastImportLxns != other.lastImportLxns) return false
        if (b35Count != other.b35Count) return false
        if (b15Count != other.b15Count) return false
        if (b35RecLimit != other.b35RecLimit) return false
        if (b15RecLimit != other.b15RecLimit) return false
        if (id != other.id) return false
        if (name != other.name) return false
        if (server != other.server) return false
        if (!avatar.contentEquals(other.avatar)) return false
        if (avatarUrl != other.avatarUrl) return false
        if (dfUsername != other.dfUsername) return false
        if (plate != other.plate) return false
        if (title != other.title) return false

        return true
    }

    override fun hashCode(): Int {
        var result = active.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + playerRating
        result = 31 * result + lastImportDf.hashCode()
        result = 31 * result + lastImportLxns.hashCode()
        result = 31 * result + b35Count
        result = 31 * result + b15Count
        result = 31 * result + b35RecLimit
        result = 31 * result + b15RecLimit
        result = 31 * result + id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + server.hashCode()
        result = 31 * result + avatar.contentHashCode()
        result = 31 * result + avatarUrl.hashCode()
        result = 31 * result + dfUsername.hashCode()
        result = 31 * result + plate.hashCode()
        result = 31 * result + title.hashCode()
        return result
    }
}

@Serializable
data class BackupScore(
    @ProtoNumber(1) val profileId: String = "",
    @ProtoNumber(2) val chartKey: String = "",
    @ProtoNumber(3) val achievement: Double = 0.0,
    @ProtoNumber(4) val rank: String = "",
    @ProtoNumber(5) val dxScore: Int = 0,
    @ProtoNumber(6) val fc: String = "",
    @ProtoNumber(7) val fs: String = "",
    @ProtoNumber(8) val achievedAt: Long = 0,
    @ProtoNumber(9) val score: Int = 0,
    // Proto3 represents a missing lamp as an empty string; domain models use null.
    @ProtoNumber(10) val clear: String = "",
    @ProtoNumber(11) val songId: String = "",
)

@Serializable
data class BackupPlayRecord(
    @ProtoNumber(1) val id: String = "",
    @ProtoNumber(2) val result: BackupScore = BackupScore(),
)

@Serializable
data class BackupCollection(
    @ProtoNumber(1) val id: String = "",
    @ProtoNumber(2) val name: String = "",
    @ProtoNumber(3) val sortIndex: Int = 0,
    @ProtoNumber(4) val createdAt: Long = 0,
    @ProtoNumber(5) val updatedAt: Long = 0,
)

@Serializable
data class BackupCollectionItem(
    @ProtoNumber(1) val id: String = "",
    @ProtoNumber(2) val collectionId: String = "",
    @ProtoNumber(3) val songId: String = "",
    @ProtoNumber(4) val chartType: String = "",
    @ProtoNumber(5) val difficulty: String = "",
    @ProtoNumber(6) val position: Int = 0,
    @ProtoNumber(7) val createdAt: Long = 0,
    @ProtoNumber(8) val updatedAt: Long = 0,
)

@Serializable
data class BackupSetting(
    @ProtoNumber(1) val key: String = "",
    @ProtoNumber(2) val kind: String = "",
    @ProtoNumber(3) val stringValue: String = "",
    @ProtoNumber(4) val integerValue: Long = 0,
    @ProtoNumber(5) val doubleValue: Double = 0.0,
    @ProtoNumber(6) val boolValue: Boolean = false,
    @ProtoNumber(7) val stringValues: List<String> = emptyList(),
    @ProtoNumber(8) val bytesValue: ByteArray = byteArrayOf(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BackupSetting

        if (integerValue != other.integerValue) return false
        if (doubleValue != other.doubleValue) return false
        if (boolValue != other.boolValue) return false
        if (key != other.key) return false
        if (kind != other.kind) return false
        if (stringValue != other.stringValue) return false
        if (stringValues != other.stringValues) return false
        if (!bytesValue.contentEquals(other.bytesValue)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = integerValue.hashCode()
        result = 31 * result + doubleValue.hashCode()
        result = 31 * result + boolValue.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + stringValue.hashCode()
        result = 31 * result + stringValues.hashCode()
        result = 31 * result + bytesValue.contentHashCode()
        return result
    }
}
