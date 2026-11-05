package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface FitnessApiService {
    @GET("exerciseinfo/")
    suspend fun getExercises(
        @Query("limit") limit: Int = PAGE_SIZE,
        @Query("offset") offset: Int = 0,
        @Query("language") language: Int = 2,
        @Query("format") format: String = "json"
    ): ExercisePage

    companion object {
        const val PAGE_SIZE = 20
        private const val BASE_URL = "https://wger.de/api/v2/"
        private val service by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(25, TimeUnit.SECONDS)
                    .callTimeout(40, TimeUnit.SECONDS)
                    .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(FitnessApiService::class.java)
        }

        fun create(): FitnessApiService {
            return service
        }
    }
}
