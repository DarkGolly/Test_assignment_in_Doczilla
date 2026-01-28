package com.darkgolly.weather.service;

import com.darkgolly.weather.cache.InMemoryCache;
import com.darkgolly.weather.client.WeatherClient;
import com.darkgolly.weather.model.WeatherData;
import com.darkgolly.weather.util.Json;
import com.darkgolly.weather.util.QueryParams;
import com.darkgolly.weather.view.TemplateRenderer;
import com.darkgolly.weather.model.WeatherViewModel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public class WeatherService {
    private static final int DEFAULT_PORT = 8080;
    private static final int CACHE_TTL_SECONDS = 15 * 60;

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {
            }
        }

        InMemoryCache<WeatherData> cache = new InMemoryCache<>(Duration.ofSeconds(CACHE_TTL_SECONDS));
        WeatherClient client = new WeatherClient();
        TemplateRenderer renderer = new TemplateRenderer();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/weather", new WeatherHandler(cache, client, renderer));
        server.createContext("/", new RootHandler());
        server.setExecutor(Executors.newFixedThreadPool(6));
        server.start();

        System.out.printf(Locale.ROOT, "WeatherService started on http://localhost:%d%n", port);
    }

    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("/".equals(exchange.getRequestURI().getPath())) {
                sendText(exchange, 200, "WeatherService is running. Use /weather?city=CityName");
                return;
            }
            sendText(exchange, 404, "Not found");
        }
    }

    private static class WeatherHandler implements HttpHandler {
        private final InMemoryCache<WeatherData> cache;
        private final WeatherClient client;
        private final TemplateRenderer renderer;

        private WeatherHandler(InMemoryCache<WeatherData> cache, WeatherClient client, TemplateRenderer renderer) {
            this.cache = cache;
            this.client = client;
            this.renderer = renderer;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }
            Map<String, String> params = QueryParams.parse(exchange.getRequestURI().getRawQuery());
            String city = params.get("city");
            if (city == null || city.isBlank()) {
                sendText(exchange, 400, "city query parameter is required");
                return;
            }

            String format = Optional.ofNullable(params.get("format")).orElse("html");
            String cacheKey = "weather:" + city.toLowerCase(Locale.ROOT).trim();

            WeatherData data = cache.get(cacheKey);
            if (data == null) {
                try {
                    data = client.fetch(city);
                } catch (WeatherClient.CityNotFoundException e) {
                    sendText(exchange, 404, "City not found");
                    return;
                } catch (IOException | InterruptedException e) {
                    sendText(exchange, 502, "Failed to fetch weather data");
                    return;
                }
                cache.put(cacheKey, data);
            }

            if ("json".equalsIgnoreCase(format)) {
                String json = Json.MAPPER.writeValueAsString(data);
                sendBytes(exchange, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
                return;
            }

            WeatherViewModel model = new WeatherViewModel(
                    data.city(),
                    data.country(),
                    data.timezone(),
                    data.fetchedAt(),
                    data.hourly()
            );
            String html = renderer.renderWeatherPage(model);
            sendBytes(exchange, 200, "text/html; charset=utf-8", html.getBytes(StandardCharsets.UTF_8));
        }

    }

    private static void sendText(HttpExchange exchange, int status, String message) throws IOException {
        sendBytes(exchange, status, "text/plain; charset=utf-8", message.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendBytes(HttpExchange exchange, int status, String contentType, byte[] data)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }

    
}
