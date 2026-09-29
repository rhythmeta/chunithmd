package org.rhythmeta.chunithmd.score

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ScoreRecordEntity::class], version = 2, exportSchema = false)
abstract class ScoreDatabase : RoomDatabase() {
    abstract fun records(): ScoreRecordDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE score_records ADD COLUMN clear TEXT NOT NULL DEFAULT 'clear'")
                db.execSQL("ALTER TABLE score_records ADD COLUMN full_combo TEXT")
                db.execSQL("ALTER TABLE score_records ADD COLUMN full_chain TEXT")
            }
        }
    }
}
