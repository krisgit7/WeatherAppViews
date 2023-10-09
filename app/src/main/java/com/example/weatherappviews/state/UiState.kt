package com.example.weatherappviews.state

import com.example.weatherappviews.model.data.WeatherContent

sealed class UiState {

    object Loading : UiState()

    data class Success(val weatherContent: WeatherContent) : UiState()

    data class Error(val message: String) : UiState()
}
