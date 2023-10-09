package com.example.weatherappviews

import android.app.Application
import android.content.Context

class WeatherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        var instance: WeatherApplication? = null
            private set

        val context: Context?
            get() = instance
    }
}