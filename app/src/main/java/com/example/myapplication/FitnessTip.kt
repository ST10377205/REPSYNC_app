package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class AdviceSlip(
    @SerializedName("slip") val slip: Tip
)

data class Tip(
    @SerializedName("id") val id: Int,
    @SerializedName("advice") val advice: String
)
