package org.rhythmeta.chunithmd.score

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

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
    @ColumnInfo(defaultValue = "'clear'") val clear: String = ClearType.Clear.wireValue,
    @ColumnInfo(name = "full_combo") val fullCombo: String? = null,
    @ColumnInfo(name = "full_chain") val fullChain: String? = null,
)
