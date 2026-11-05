package com.example.myapplication

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [User::class, Workout::class, WorkoutImportState::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS workouts (id TEXT NOT NULL, localUserId INTEGER NOT NULL, ownerUid TEXT, title TEXT NOT NULL, completedAt INTEGER NOT NULL, durationSeconds INTEGER NOT NULL, totalVolume REAL NOT NULL, completedSets INTEGER NOT NULL, notes TEXT NOT NULL, syncState TEXT NOT NULL, PRIMARY KEY(localUserId, id))")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_workouts_localUserId_syncState ON workouts (localUserId, syncState)")
                db.execSQL("CREATE TABLE IF NOT EXISTS workout_import_state (localUserId INTEGER NOT NULL, legacyDurationSeconds INTEGER NOT NULL, legacyExtraWorkouts INTEGER NOT NULL, PRIMARY KEY(localUserId))")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE users ADD COLUMN googleUid TEXT")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_users_googleUid ON users (googleUid)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "repsync_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}