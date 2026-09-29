package org.rhythmeta.chunithmd.score

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreRecordDao {
    @Query("SELECT * FROM score_records WHERE profileId = :profileId AND songId = :songId ORDER BY playedAt DESC")
    fun observeForSong(profileId: String, songId: String): Flow<List<ScoreRecordEntity>>

    @Query("SELECT * FROM score_records WHERE profileId = :profileId ORDER BY playedAt DESC")
    fun observeForProfile(profileId: String): Flow<List<ScoreRecordEntity>>

    @Query("SELECT * FROM score_records WHERE id = :id LIMIT 1")
    suspend fun find(id: String): ScoreRecordEntity?

    @Insert
    suspend fun insert(record: ScoreRecordEntity)

    @Delete
    suspend fun delete(record: ScoreRecordEntity)
}
