package com.example.weatherappviews.model.data

data class WeatherContent(
    var date: String = "",
    var temperatureInFahrenheit: String = "0",
    var cityAndCountry: String = "",
    var weatherIconUrl: String = "",
    var weatherIconDescription: String = "",
    var humidity: String = "",
    var pressure: String = ""
)
