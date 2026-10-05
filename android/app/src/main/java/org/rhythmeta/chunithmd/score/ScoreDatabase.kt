package org.rhythmeta.chunithmd.score

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ScoreRecordEntity::class], version = 3, exportSchema = false)
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

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite requires a table rebuild to remove a NOT NULL constraint.
                db.execSQL("CREATE TABLE score_records_nullable (id TEXT NOT NULL, profileId TEXT NOT NULL, songId TEXT NOT NULL, sheetKey TEXT NOT NULL, score INTEGER NOT NULL, rank TEXT NOT NULL, playedAt INTEGER NOT NULL, clear TEXT, full_combo TEXT, full_chain TEXT, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO score_records_nullable (id, profileId, songId, sheetKey, score, rank, playedAt, clear, full_combo, full_chain) SELECT id, profileId, songId, sheetKey, score, rank, playedAt, CASE WHEN TRIM(clear) = '' THEN NULL ELSE clear END, full_combo, full_chain FROM score_records")
                db.execSQL("DROP TABLE score_records")
                db.execSQL("ALTER TABLE score_records_nullable RENAME TO score_records")
                db.execSQL("CREATE INDEX index_score_records_profileId_songId ON score_records (profileId, songId)")
                db.execSQL("CREATE INDEX index_score_records_profileId_sheetKey ON score_records (profileId, sheetKey)")
                db.execSQL("CREATE INDEX index_score_records_playedAt ON score_records (playedAt)")
            }
        }

    }
}
