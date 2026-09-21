package com.example.myapplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

interface FitnessApiService {
    @GET("advice")
    suspend fun getDailyTip(): AdviceSlip

    companion object {
        private const val BASE_URL = "https://api.adviceslip.com/"

        fun create(): FitnessApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FitnessApiService::class.java)
        }
    }
}
