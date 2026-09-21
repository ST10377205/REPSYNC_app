package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class AdviceSlip(
    @SerializedName("slip") val slip: Tip
)

data class Tip(
    @SerializedName("id") val id: Int,
    @SerializedName("advice") val advice: String
)

// Data models for the REST API Send/Receive requirement
data class CloudWorkout(
    val userId: Int,
    val workoutTitle: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class CloudResponse(
    @SerializedName("name") val id: String?, // Firebase returns the unique generated key inside the "name" field
    val message: String?
)
