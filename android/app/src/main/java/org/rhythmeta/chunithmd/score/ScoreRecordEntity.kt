package org.rhythmeta.chunithmd.score

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import org.rhythmeta.chunithmd.shared.ClearType
import org.rhythmeta.chunithmd.shared.ScoreRecord

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
) {
    fun toDomain(): ScoreRecord = ScoreRecord(
        id = id,
        profileId = profileId,
        songId = songId,
        sheetKey = sheetKey,
        score = score,
        rank = rank,
        playedAt = playedAt,
        clear = clear,
        fullCombo = fullCombo,
        fullChain = fullChain,
    )

    companion object {
        fun fromDomain(record: ScoreRecord): ScoreRecordEntity = ScoreRecordEntity(
            id = record.id,
            profileId = record.profileId,
            songId = record.songId,
            sheetKey = record.sheetKey,
            score = record.score,
            rank = record.rank,
            playedAt = record.playedAt,
            clear = record.clear,
            fullCombo = record.fullCombo,
            fullChain = record.fullChain,
        )
    }
}
