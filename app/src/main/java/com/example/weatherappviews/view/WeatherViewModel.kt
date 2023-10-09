package com.example.weatherappviews.view

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.weatherappviews.WeatherApplication
import com.example.weatherappviews.api.WeatherApi
import com.example.weatherappviews.api.createWeatherApi
import com.example.weatherappviews.location.SharedPreferenceUtil
import com.example.weatherappviews.model.WeatherInfo
import com.example.weatherappviews.model.data.WeatherContent
import com.example.weatherappviews.state.UiState
import com.example.weatherappviews.utils.kelvinToFahrenheit
import com.example.weatherappviews.utils.timestampToDateTimeString
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val weatherApi: WeatherApi = createWeatherApi()
) : BaseViewModel<UiState>() {

    fun getWeatherInfoFromPreferences() {
        uiState.value = UiState.Loading

        try {
            val weatherInfo = SharedPreferenceUtil.getLastWeatherInfo(WeatherApplication.context)
            Log.d(TAG, "weatherInfo: $weatherInfo")
            val weatherContent = mapWeatherInfoToWeatherContent(weatherInfo)
            uiState.value = UiState.Success(weatherContent)
        } catch (exception: Exception) {
            uiState.value = UiState.Error(exception.localizedMessage ?: "")
        }
    }

    fun getWeatherInfoForCity(city: String) {
        uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val weatherInfo = weatherApi.getWeatherInfo(city)
                saveToSharedPreferences(weatherInfo)
                val weatherContent = mapWeatherInfoToWeatherContent(weatherInfo)
                uiState.value = UiState.Success(weatherContent)
            } catch (exception: Exception) {
                uiState.value = UiState.Error(exception.localizedMessage ?: "")
            }
        }
    }

    fun getWeatherInfoByCoordinate(latitude: Double, longitude: Double) {
        uiState.value = UiState.Loading

        viewModelScope.launch {
            try {
                val weatherInfo = weatherApi.getWeatherInfo(latitude, longitude)
                saveToSharedPreferences(weatherInfo)
                val weatherContent = mapWeatherInfoToWeatherContent(weatherInfo)
                uiState.value = UiState.Success(weatherContent)
            } catch (exception: Exception) {
                uiState.value = UiState.Error(exception.localizedMessage ?: "")
            }
        }
    }

    private fun saveToSharedPreferences(weatherInfo: WeatherInfo) {
        SharedPreferenceUtil.saveLastWeatherInfo(WeatherApplication.context, weatherInfo)
    }

    private fun mapWeatherInfoToWeatherContent(weatherInfo: WeatherInfo): WeatherContent {
        return WeatherContent(
            date = weatherInfo.dt.timestampToDateTimeString(),
            temperatureInFahrenheit = weatherInfo.main.temp.kelvinToFahrenheit().toString(),
            cityAndCountry = "${weatherInfo.name}, ${weatherInfo.sys.country}",
            weatherIconUrl = "$BASE_ICON_URL${weatherInfo.weather[0].icon}.png",
            weatherIconDescription = weatherInfo.weather[0].description,
            humidity = "${weatherInfo.main.humidity}%",
            pressure = "${weatherInfo.main.pressure} mBar"
        )
    }

    companion object {
        private const val BASE_ICON_URL = "https://openweathermap.org/img/wn/"
        private const val TAG = "WeatherViewModel"
    }
}