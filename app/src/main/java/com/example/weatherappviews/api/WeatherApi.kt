package com.example.weatherappviews.api

import com.example.weatherappviews.model.WeatherInfo
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("weather")
    suspend fun getWeatherInfo(@Query("q") city: String): WeatherInfo

    @GET("weather")
    suspend fun getWeatherInfo(@Query("lat") latitude: Double, @Query("lon") longitude: Double): WeatherInfo
}

fun createWeatherApi(): WeatherApi {
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(ParameterInterceptor())
        .build()

    val gson = GsonBuilder().setLenient().create()
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openweathermap.org/data/2.5/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    return retrofit.create(WeatherApi::class.java)
}