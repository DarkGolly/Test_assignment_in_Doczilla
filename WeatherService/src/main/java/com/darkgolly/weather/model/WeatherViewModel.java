package com.darkgolly.weather.model;

public record WeatherViewModel(
        String city,
        String country,
        String timezone,
        String fetchedAt,
        java.util.List<com.darkgolly.weather.model.TemperaturePoint> hourly
) {
}
