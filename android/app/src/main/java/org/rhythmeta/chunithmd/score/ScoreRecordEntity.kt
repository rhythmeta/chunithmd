package org.rhythmeta.chunithmd.score

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "score_records",
    indices = [
        Index(value = ["profileId", "songId"]),
        Index(value = ["profileId", "sheetKey"]),
        Index("playedAt"),
    ],
)
data class ScoreRecordEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val songId: String,
    val sheetKey: String,
    val score: Int,
    val rank: String,
    val playedAt: Long,
)
