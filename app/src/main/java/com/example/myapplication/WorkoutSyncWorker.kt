package com.example.myapplication

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal suspend fun <T> Task<T>.awaitCloud(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (continuation.isActive) {
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(task.exception ?: IllegalStateException("Cloud task failed"))
        }
    }
}

object WorkoutSyncScheduler {
    fun enqueueCurrent(context: Context) {
        if (!PushNotifications.isConfigured()) return
        val prefs = context.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userId = prefs.getInt("current_user_id", -1)
        if (userId > 0 && prefs.getString("firebase_uid", null) == uid) enqueue(context, userId, uid)
    }

    fun enqueue(context: Context, userId: Int, uid: String) {
        val input = Data.Builder().putInt("userId", userId).putString("uid", uid).build()
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<WorkoutSyncWorker>()
            .setInputData(input)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        // Appending guarantees a save at the end of a running upload is not missed.
        WorkManager.getInstance(context).enqueueUniqueWork("workout-sync-$uid", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        // Periodic recovery also finds rows saved immediately before a process crash.
        val recovery = PeriodicWorkRequestBuilder<WorkoutSyncWorker>(15, TimeUnit.MINUTES)
            .setInputData(input).setConstraints(constraints).setInitialDelay(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("workout-recovery-$uid", ExistingPeriodicWorkPolicy.KEEP, recovery)
    }

    fun cancelCurrent(context: Context) {
        val uid = context.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE).getString("firebase_uid", null) ?: return
        WorkManager.getInstance(context).cancelUniqueWork("workout-sync-$uid")
        WorkManager.getInstance(context).cancelUniqueWork("workout-recovery-$uid")
    }
}

class WorkoutSyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val userId = inputData.getInt("userId", -1)
        val uid = inputData.getString("uid") ?: return Result.failure()
        val prefs = applicationContext.getSharedPreferences("RepSyncPrefs", Context.MODE_PRIVATE)
        fun isCurrentSession(): Boolean = PushNotifications.isConfigured() &&
            prefs.getInt("current_user_id", -1) == userId && prefs.getString("firebase_uid", null) == uid &&
            FirebaseAuth.getInstance().currentUser?.uid == uid
        if (!isCurrentSession()) return Result.success()
        val database = AppDatabase.getDatabase(applicationContext)
        if (database.userDao().getUserById(userId)?.googleUid != uid) return Result.success()
        val state = applicationContext.getSharedPreferences("WorkoutSyncPrefs", Context.MODE_PRIVATE)
        state.edit().putString("status_$userId", "RUNNING").apply()
        return try {
            val repository = WorkoutRepository(applicationContext)
            repository.bindOwner(userId, uid)
            val collection = FirebaseFirestore.getInstance().collection("users").document(uid).collection("workouts")
            for (row in database.workoutDao().pending(userId, uid)) {
                if (!isCurrentSession()) return Result.success()
                withTimeout(45000) { collection.document(row.id).set(row.cloudData()).awaitCloud() }
                database.workoutDao().markSynced(userId, row.id, uid)
            }
            if (!isCurrentSession()) return Result.success()
            val snapshot = withTimeout(45000) { collection.get(Source.SERVER).awaitCloud() }
            val rows = snapshot.documents.mapNotNull { doc -> doc.data?.let { Workout.fromCloud(doc.id, it, userId, uid) } }
            repository.applyRemote(userId, uid, rows)
            state.edit().putString("status_$userId", "COMPLETE").putLong("last_success_$userId", System.currentTimeMillis()).apply()
            Result.success()
        } catch (_: TimeoutCancellationException) {
            state.edit().putString("status_$userId", "RETRY").apply()
            Result.retry()
        } catch (cancelled: CancellationException) {
            state.edit().putString("status_$userId", "RETRY").apply()
            throw cancelled
        } catch (error: Exception) {
            val needsSetup = error is FirebaseFirestoreException && error.code in setOf(
                FirebaseFirestoreException.Code.PERMISSION_DENIED, FirebaseFirestoreException.Code.NOT_FOUND,
                FirebaseFirestoreException.Code.FAILED_PRECONDITION, FirebaseFirestoreException.Code.INVALID_ARGUMENT)
            state.edit().putString("status_$userId", if (needsSetup) "SETUP" else "RETRY").apply()
            if (needsSetup) Result.failure() else Result.retry()
        }
    }
}
