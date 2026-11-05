package com.example.myapplication

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

object WorkoutSyncState {
    const val LOCAL_ONLY = "LOCAL_ONLY"
    const val PENDING = "PENDING"
    const val SYNCED = "SYNCED"
}

@Entity(tableName = "workouts", primaryKeys = ["localUserId", "id"], indices = [Index(value = ["localUserId", "syncState"])])
data class Workout(
    val id: String,
    val localUserId: Int,
    val ownerUid: String?,
    val title: String,
    val completedAt: Long,
    val durationSeconds: Int,
    val totalVolume: Double,
    val completedSets: Int,
    val notes: String,
    val syncState: String
) {
    fun cloudData(): Map<String, Any> = mapOf(
        "id" to id, "ownerUid" to requireNotNull(ownerUid), "title" to title,
        "completedAt" to completedAt, "durationSeconds" to durationSeconds,
        "totalVolume" to totalVolume, "completedSets" to completedSets, "notes" to notes
    )

    fun valid(): Boolean = id.matches(Regex("[A-Za-z0-9_-]{1,128}")) && localUserId > 0 &&
        title.isNotBlank() && title.length <= 200 && completedAt > 0 &&
        durationSeconds in 0..604800 && totalVolume.isFinite() && totalVolume in 0.0..1_000_000_000.0 &&
        completedSets in 0..10000 && notes.length <= 4000

    companion object {
        fun fromCloud(id: String, data: Map<String, Any>, localUserId: Int, uid: String): Workout? {
            if (data["id"] != id || data["ownerUid"] != uid) return null
            val seconds = (data["durationSeconds"] as? Number)?.toLong() ?: return null
            val sets = (data["completedSets"] as? Number)?.toLong() ?: return null
            if (seconds !in 0..604800 || sets !in 0..10000) return null
            return Workout(id, localUserId, uid, data["title"] as? String ?: return null,
                (data["completedAt"] as? Number)?.toLong() ?: return null,
                seconds.toInt(), (data["totalVolume"] as? Number)?.toDouble() ?: return null,
                sets.toInt(), data["notes"] as? String ?: return null, WorkoutSyncState.SYNCED).takeIf { it.valid() }
        }
    }
}

@Entity(tableName = "workout_import_state")
data class WorkoutImportState(
    @PrimaryKey val localUserId: Int,
    val legacyDurationSeconds: Long,
    val legacyExtraWorkouts: Int
)

data class CompletedWorkout(
    val id: String,
    val title: String,
    val completedAt: Long,
    val durationSeconds: Int,
    val totalVolume: Double,
    val completedSets: Int,
    val notes: String
)
