package com.darkgolly.weather.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.darkgolly.weather.model.TemperaturePoint;
import com.darkgolly.weather.model.WeatherData;
import com.darkgolly.weather.util.Json;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class WeatherClient {
    private final HttpClient httpClient;

    public WeatherClient() {
        this(HttpClient.newHttpClient());
    }

    public WeatherClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public WeatherData fetch(String city) throws IOException, InterruptedException, CityNotFoundException {
        GeocodeResult geo = geocode(city);
        Forecast forecast = forecast(geo.latitude, geo.longitude);
        return new WeatherData(
                geo.name,
                geo.country,
                geo.latitude,
                geo.longitude,
                forecast.timezone,
                Instant.now().toString(),
                forecast.points
        );
    }

    private GeocodeResult geocode(String city) throws IOException, InterruptedException, CityNotFoundException {
        String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Geocoding API responded with status " + response.statusCode());
        }
        JsonNode root = Json.MAPPER.readTree(response.body());
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            throw new CityNotFoundException(city);
        }
        JsonNode first = results.get(0);
        String name = first.path("name").asText(city);
        String country = first.path("country").asText("");
        double latitude = first.path("latitude").asDouble();
        double longitude = first.path("longitude").asDouble();
        return new GeocodeResult(name, country, latitude, longitude);
    }

    private Forecast forecast(double latitude, double longitude) throws IOException, InterruptedException {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                "&longitude=" + longitude +
                "&hourly=temperature_2m&timezone=auto";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Forecast API responded with status " + response.statusCode());
        }
        JsonNode root = Json.MAPPER.readTree(response.body());
        JsonNode hourly = root.path("hourly");
        JsonNode times = hourly.path("time");
        JsonNode temps = hourly.path("temperature_2m");
        int count = Math.min(24, Math.min(times.size(), temps.size()));
        if (count == 0) {
            throw new IOException("Forecast API returned no hourly data");
        }
        List<TemperaturePoint> points = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            points.add(new TemperaturePoint(times.get(i).asText(), temps.get(i).asDouble()));
        }
        String timezone = root.path("timezone").asText("UTC");
        return new Forecast(timezone, points);
    }

    public static final class CityNotFoundException extends Exception {
        public CityNotFoundException(String city) {
            super("City not found: " + city);
        }
    }

    private record GeocodeResult(String name, String country, double latitude, double longitude) {
    }

    private record Forecast(String timezone, List<TemperaturePoint> points) {
    }
}
