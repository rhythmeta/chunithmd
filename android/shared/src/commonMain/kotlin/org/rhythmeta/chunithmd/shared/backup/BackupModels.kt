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
)

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
)
