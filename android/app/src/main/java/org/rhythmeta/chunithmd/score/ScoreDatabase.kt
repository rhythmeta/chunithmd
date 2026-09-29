package org.rhythmeta.chunithmd.score

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScoreRecordEntity::class], version = 1, exportSchema = false)
abstract class ScoreDatabase : RoomDatabase() {
    abstract fun records(): ScoreRecordDao
}
