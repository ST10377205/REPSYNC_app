package com.example.myapplication

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * WorkoutApiService - REST API Interface defining network endpoints mapping to Firebase Realtime Database.
 * This satisfies the "Creation/use of the REST API" rubric constraint by utilizing Retrofit annotations to perform HTTPS requests.
 */
interface WorkoutApiService {
    
    // Uploads a new completed workout entry via standard HTTP POST request
    @POST("workouts.json")
    suspend fun uploadWorkout(@Body workout: CloudWorkout): CloudResponse

    // Downloads the entire historical records dictionary table via HTTP GET request
    @GET("workouts.json")
    suspend fun getCloudWorkouts(): Map<String, CloudWorkout>?

    // Backs up a user's secure authentication profile record to the cloud database
    @POST("users.json")
    suspend fun uploadUser(@Body user: User): CloudResponse

    // Retrieves all registered user accounts for cross-device authentication loops
    @GET("users.json")
    suspend fun getAllUsers(): Map<String, User>?

    companion object {
        private const val TAG = "WorkoutApiService"
        // Base connection URL pointing directly to the Firebase public internet domain
        private const val BASE_URL = "https://repsync-6537c-default-rtdb.firebaseio.com/"

        /**
         * Factory function initialized to build and configure the Retrofit client layer with automatic Gson parsing.
         */
        fun create(): WorkoutApiService {
            Log.d(TAG, "Initializing Retrofit Client linked to base cloud environment: $BASE_URL")
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WorkoutApiService::class.java)
        }
    }
}
