package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar

class WorkoutRepository(
    context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context),
    private val preferences: SharedPreferences = context.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE),
    private val scheduleSync: Boolean = true
) {
    private val appContext = context.applicationContext
    private val dao = database.workoutDao()
    fun observe(userId: Int) = dao.observe(userId)

    suspend fun ensureLegacyImported(userId: Int) = withContext(Dispatchers.IO) {
        database.withTransaction {
            if (dao.importState(userId) != null) return@withTransaction
            val user = database.userDao().getUserById(userId) ?: return@withTransaction
            val rows = LegacyWorkoutParser.parse(userId, user.googleUid,
                preferences.getString("user_history_$userId", "").orEmpty())
            rows.forEach { dao.insert(it) }
            dao.recordImport(WorkoutImportState(userId,
                preferences.getInt("user_duration_$userId", 0).coerceAtLeast(0).toLong() * 60L,
                (preferences.getInt("user_total_workouts_$userId", 0) - rows.size).coerceAtLeast(0)))
        }
    }

    suspend fun save(userId: Int, completed: CompletedWorkout): Workout = withContext(Dispatchers.IO) {
        ensureLegacyImported(userId)
        val row = database.withTransaction {
            val user = requireNotNull(database.userDao().getUserById(userId))
            val workout = Workout(completed.id, userId, user.googleUid, completed.title,
                completed.completedAt, completed.durationSeconds, completed.totalVolume,
                completed.completedSets, completed.notes,
                if (user.googleUid == null) WorkoutSyncState.LOCAL_ONLY else WorkoutSyncState.PENDING)
            require(workout.valid())
            dao.insert(workout)
            requireNotNull(dao.get(userId, workout.id))
        }
        refreshMetrics(userId)
        if (scheduleSync) WorkoutSyncScheduler.enqueueCurrent(appContext)
        row
    }

    suspend fun bindOwner(userId: Int, uid: String) = withContext(Dispatchers.IO) {
        ensureLegacyImported(userId)
        database.withTransaction {
            check(database.userDao().getUserById(userId)?.googleUid == uid)
            dao.bindOwner(userId, uid)
        }
    }

    suspend fun applyRemote(userId: Int, uid: String, rows: List<Workout>) = withContext(Dispatchers.IO) {
        database.withTransaction {
            check(database.userDao().getUserById(userId)?.googleUid == uid)
            for (row in rows) {
                if (row.localUserId != userId || row.ownerUid != uid || !row.valid()) continue
                val old = dao.get(userId, row.id)
                if (old == null || old.syncState == WorkoutSyncState.SYNCED) dao.upsert(row.copy(syncState = WorkoutSyncState.SYNCED))
                else if (old.ownerUid == uid && old.cloudData() == row.cloudData()) dao.markSynced(userId, row.id, uid)
            }
        }
        refreshMetrics(userId)
    }

    suspend fun refreshMetrics(userId: Int) = withContext(Dispatchers.IO) {
        projectionMutex.withLock {
            val baseline = dao.importState(userId) ?: return@withLock
            val rows = dao.all(userId)
            val weekStart = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val history = rows.sortedWith(compareBy<Workout> { it.completedAt }.thenBy { it.id }).joinToString("", transform = {
                "${it.title.replace('\n', ' ').replace('\r', ' ')} • ${it.totalVolume.toLong()} kg • ${it.completedAt}\n"
            })
            preferences.edit()
                .putString("user_history_$userId", history)
                .putInt("user_total_workouts_$userId", (rows.size.toLong() + baseline.legacyExtraWorkouts).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                .putInt("user_workouts_this_week_$userId", rows.count { it.completedAt >= weekStart })
                .putInt("user_duration_$userId", ((baseline.legacyDurationSeconds + rows.sumOf { it.durationSeconds.toLong() }) / 60L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                .apply()
        }
    }

    companion object { private val projectionMutex = Mutex() }
}
