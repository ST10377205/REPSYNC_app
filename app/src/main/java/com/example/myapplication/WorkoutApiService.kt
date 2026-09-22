package com.example.myapplication

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * WorkoutApiService - REST API Interface defining network endpoints mapping to Firebase Realtime Database.
 * Optimized with user-specific pathing to prevent data mixing between different users.
 */
interface WorkoutApiService {
    
    // Uploads a new completed workout entry nested under the specific user's ID
    @POST("workouts/{userId}.json")
    suspend fun uploadWorkout(@Path("userId") userId: Int, @Body workout: CloudWorkout): CloudResponse

    // Downloads only the records belonging to the specific logged-in user
    @GET("workouts/{userId}.json")
    suspend fun getUserCloudWorkouts(@Path("userId") userId: Int): Map<String, CloudWorkout>?

    // Backs up a user's secure authentication profile record
    @POST("users.json")
    suspend fun uploadUser(@Body user: User): CloudResponse

    // Retrieves all registered user accounts for authentication checks
    @GET("users.json")
    suspend fun getAllUsers(): Map<String, User>?

    companion object {
        private const val TAG = "WorkoutApiService"
        private const val BASE_URL = "https://repsync-6537c-default-rtdb.firebaseio.com/"

        fun create(): WorkoutApiService {
            Log.d(TAG, "Initializing Cloud API Client: $BASE_URL")
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WorkoutApiService::class.java)
        }
    }
}
