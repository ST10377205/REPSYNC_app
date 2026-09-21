package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WorkoutApiService {
    
    // Send data directly to your Firebase Realtime Database REST API
    @POST("workouts.json")
    suspend fun uploadWorkout(@Body workout: CloudWorkout): CloudResponse

    // Receive data from Firebase Realtime Database as a dictionary Map
    @GET("workouts.json")
    suspend fun getCloudWorkouts(): Map<String, CloudWorkout>?

    // Send user account directly to your Firebase Realtime Database REST API
    @POST("users.json")
    suspend fun uploadUser(@Body user: User): CloudResponse

    // Receive all users from Firebase Realtime Database as a dictionary Map
    @GET("users.json")
    suspend fun getAllUsers(): Map<String, User>?

    companion object {
        // Connected directly to your specific repsync-6537c project database over the internet!
        private const val BASE_URL = "https://repsync-6537c-default-rtdb.firebaseio.com/"

        fun create(): WorkoutApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WorkoutApiService::class.java)
        }
    }
}
