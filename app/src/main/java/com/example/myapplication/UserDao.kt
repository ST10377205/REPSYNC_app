package com.example.myapplication

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Int): User?

    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE")
    suspend fun getUsersByEmail(email: String): List<User>

    @Query("SELECT * FROM users WHERE googleUid = :uid LIMIT 1")
    suspend fun getUserByGoogleUid(uid: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun signup(user: User): Long

    @Update
    suspend fun updateUser(user: User)
}