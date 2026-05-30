package com.gonzalocamera.padelcounter.mobile.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MatchEntity::class], version = 3, exportSchema = false)
abstract class PadelDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var INSTANCE: PadelDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN bestOf INTEGER NOT NULL DEFAULT 3")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE matches ADD COLUMN scoringMode TEXT NOT NULL DEFAULT 'DEUCE'")
                db.execSQL("UPDATE matches SET scoringMode = CASE WHEN goldenPoint = 1 THEN 'GOLDEN_POINT' ELSE 'DEUCE' END")
            }
        }

        fun getInstance(context: Context): PadelDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PadelDatabase::class.java,
                    "padel_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
            }
        }
    }
}
