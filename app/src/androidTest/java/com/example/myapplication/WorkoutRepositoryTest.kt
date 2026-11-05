package com.example.myapplication

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class WorkoutRepositoryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun workout(id: String = UUID.randomUUID().toString()) = CompletedWorkout(id, "Home Workout", System.currentTimeMillis(), 90, 120.0, 3, "Offline notes")

    @Test
    fun versionTwoUpgradePreservesGoogleAccountAndCreatesWorkoutTables() = runBlocking {
        val name = "workout-migration-${UUID.randomUUID()}"
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fullName TEXT NOT NULL, email TEXT NOT NULL, password TEXT NOT NULL, googleUid TEXT)")
            db.execSQL("CREATE UNIQUE INDEX index_users_googleUid ON users (googleUid)")
            db.execSQL("INSERT INTO users VALUES (41, 'Existing Athlete', 'athlete@example.test', '', 'owner-a')")
            db.version = 2
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_2_3).build()
        try {
            assertEquals("owner-a", database.userDao().getUserById(41)!!.googleUid)
            assertTrue(database.workoutDao().all(41).isEmpty())
            database.workoutDao().recordImport(WorkoutImportState(41, 600, 1))
            assertEquals(600L, database.workoutDao().importState(41)!!.legacyDurationSeconds)
        } finally { database.close(); context.deleteDatabase(name) }
    }

    @Test
    fun legacyImportRunsOnceAndPreservesExistingDurationAndExtraWorkouts() = runBlocking {
        val prefsName = "workout-prefs-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = database.userDao().signup(User(fullName = "Athlete", email = "a@example.test", password = "hash")).toInt()
            prefs.edit().putString("user_history_$id", "Old Workout • 50 kg • 1700000000000\n")
                .putInt("user_total_workouts_$id", 3).putInt("user_duration_$id", 20).commit()
            val repository = WorkoutRepository(context, database, prefs, false)
            repository.ensureLegacyImported(id); repository.ensureLegacyImported(id)
            assertEquals(1, database.workoutDao().all(id).size)
            repository.save(id, workout())
            assertEquals(4, prefs.getInt("user_total_workouts_$id", -1))
            assertEquals(21, prefs.getInt("user_duration_$id", -1))
            assertEquals(2, database.workoutDao().all(id).size)
        } finally { database.close(); context.deleteSharedPreferences(prefsName) }
    }

    @Test
    fun repeatedSaveUsesTheSameIdAndDoesNotDuplicateHistoryOrCounters() = runBlocking {
        val prefsName = "workout-prefs-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = database.userDao().signup(User(fullName = "Google Athlete", email = "a@example.test", password = "", googleUid = "owner-a")).toInt()
            val repository = WorkoutRepository(context, database, prefs, false)
            val completed = workout()
            repository.save(id, completed); repository.save(id, completed)
            val rows = database.workoutDao().all(id)
            assertEquals(1, rows.size)
            assertEquals(WorkoutSyncState.PENDING, rows.single().syncState)
            assertEquals("owner-a", rows.single().ownerUid)
            assertEquals("Offline notes", rows.single().notes)
            assertEquals(1, prefs.getInt("user_total_workouts_$id", -1))
        } finally { database.close(); context.deleteSharedPreferences(prefsName) }
    }

    @Test
    fun onlyTheVerifiedLinkedOwnerCanClaimLocalOnlyWorkouts() = runBlocking {
        val prefsName = "workout-prefs-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = database.userDao().signup(User(fullName = "Athlete", email = "a@example.test", password = "hash")).toInt()
            val repository = WorkoutRepository(context, database, prefs, false)
            repository.save(id, workout())
            assertEquals(WorkoutSyncState.LOCAL_ONLY, database.workoutDao().all(id).single().syncState)
            database.userDao().updateUser(database.userDao().getUserById(id)!!.copy(googleUid = "owner-a"))
            try { repository.bindOwner(id, "owner-b"); fail("Wrong owner must not claim local workouts") } catch (_: IllegalStateException) { }
            assertNull(database.workoutDao().all(id).single().ownerUid)
            repository.bindOwner(id, "owner-a")
            assertEquals("owner-a", database.workoutDao().all(id).single().ownerUid)
            assertEquals(WorkoutSyncState.PENDING, database.workoutDao().all(id).single().syncState)
        } finally { database.close(); context.deleteSharedPreferences(prefsName) }
    }

    @Test
    fun cloudReadsAreIdempotentAndDoNotMixAnotherUsersWorkouts() = runBlocking {
        val prefsName = "workout-prefs-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val id = database.userDao().signup(User(fullName = "Athlete", email = "a@example.test", password = "", googleUid = "owner-a")).toInt()
            val repository = WorkoutRepository(context, database, prefs, false)
            repository.ensureLegacyImported(id)
            val remote = Workout("cloud-workout", id, "owner-a", "Cloud workout", System.currentTimeMillis(), 120, 200.0, 4, "Cloud notes", WorkoutSyncState.SYNCED)
            repository.applyRemote(id, "owner-a", listOf(remote, remote.copy(id = "wrong-owner", ownerUid = "owner-b")))
            repository.applyRemote(id, "owner-a", listOf(remote))
            assertEquals(1, database.workoutDao().all(id).size)
            assertEquals("Cloud notes", database.workoutDao().all(id).single().notes)
            assertEquals(1, prefs.getInt("user_total_workouts_$id", -1))
        } finally { database.close(); context.deleteSharedPreferences(prefsName) }
    }

    @Test
    fun pendingWorkoutSurvivesDatabaseCloseAndReopen() = runBlocking {
        val name = "workout-durability-${UUID.randomUUID()}"
        val prefsName = "workout-prefs-${UUID.randomUUID()}"
        val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        var database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        try {
            val id = database.userDao().signup(User(fullName = "Athlete", email = "a@example.test", password = "", googleUid = "owner-a")).toInt()
            val completed = workout()
            WorkoutRepository(context, database, prefs, false).save(id, completed)
            database.close()
            database = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            val saved = database.workoutDao().pending(id, "owner-a").single()
            assertEquals(completed.id, saved.id)
            assertEquals(completed.notes, saved.notes)
        } finally { database.close(); context.deleteDatabase(name); context.deleteSharedPreferences(prefsName) }
    }
}
