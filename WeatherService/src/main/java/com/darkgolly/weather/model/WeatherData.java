package com.darkgolly.weather.model;

import java.util.List;

public record WeatherData(
        String city,
        String country,
        double latitude,
        double longitude,
        String timezone,
        String fetchedAt,
        List<TemperaturePoint> hourly
) {
}
