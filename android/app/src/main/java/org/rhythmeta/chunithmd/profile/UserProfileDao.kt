package org.rhythmeta.chunithmd.profile

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles ORDER BY isActive DESC, createdAt ASC, id ASC")
    fun observeAll(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE isActive = 1 LIMIT 1")
    suspend fun active(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles ORDER BY createdAt ASC, id ASC LIMIT 1")
    suspend fun first(): UserProfileEntity?

    @Query("UPDATE user_profiles SET isActive = 0")
    suspend fun clearActive()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Delete
    suspend fun delete(profile: UserProfileEntity)
}
