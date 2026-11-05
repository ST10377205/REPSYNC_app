package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE localUserId = :userId ORDER BY completedAt DESC, id DESC")
    fun observe(userId: Int): Flow<List<Workout>>

    @Query("SELECT * FROM workouts WHERE localUserId = :userId ORDER BY completedAt DESC, id DESC")
    suspend fun all(userId: Int): List<Workout>

    @Query("SELECT * FROM workouts WHERE localUserId = :userId AND id = :id LIMIT 1")
    suspend fun get(userId: Int, id: String): Workout?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(workout: Workout): Long

    @Upsert
    suspend fun upsert(workout: Workout)

    @Query("SELECT * FROM workouts WHERE localUserId = :userId AND ownerUid = :uid AND syncState = 'PENDING' ORDER BY completedAt")
    suspend fun pending(userId: Int, uid: String): List<Workout>

    @Query("UPDATE workouts SET syncState = 'SYNCED' WHERE localUserId = :userId AND id = :id AND ownerUid = :uid")
    suspend fun markSynced(userId: Int, id: String, uid: String)

    @Query("UPDATE workouts SET ownerUid = :uid, syncState = 'PENDING' WHERE localUserId = :userId AND ownerUid IS NULL")
    suspend fun bindOwner(userId: Int, uid: String)

    @Query("SELECT * FROM workout_import_state WHERE localUserId = :userId LIMIT 1")
    suspend fun importState(userId: Int): WorkoutImportState?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun recordImport(state: WorkoutImportState)
}
