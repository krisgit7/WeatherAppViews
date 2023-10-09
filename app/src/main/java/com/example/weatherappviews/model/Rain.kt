package com.example.weatherappviews.model

import com.google.gson.annotations.SerializedName

data class Rain(
    @SerializedName("1h") val oneH: Double = 0.0
)
