package com.darkgolly.filesharing;

import com.darkgolly.filesharing.model.FileRecord;
import com.darkgolly.filesharing.repository.Database;
import com.darkgolly.filesharing.repository.FileRepository;
import com.darkgolly.filesharing.repository.UserRepository;
import com.darkgolly.filesharing.service.AuthService;
import com.darkgolly.filesharing.service.CleanupService;
import com.darkgolly.filesharing.service.FileStorageService;
import com.darkgolly.filesharing.util.Json;
import com.darkgolly.filesharing.util.MultipartFormDataParser;
import com.darkgolly.filesharing.util.SimpleJson;
import com.darkgolly.filesharing.util.Urls;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;

public class FileSharingService {
    private static final int DEFAULT_PORT = 8080;
    private static final Duration EXPIRATION = Duration.ofDays(30);

    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;
        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            try {
                port = Integer.parseInt(portEnv);
            } catch (NumberFormatException ignored) {
            }
        }

        Path storageDir = Path.of("uploads");
        Files.createDirectories(storageDir);
        Path dataDir = Path.of("data");
        Files.createDirectories(dataDir);

        Database database = new Database("jdbc:h2:" + dataDir.resolve("filesharing").toAbsolutePath());
        database.init();

        FileRepository repository = new FileRepository(database);
        UserRepository userRepository = new UserRepository(database);
        FileStorageService storageService = new FileStorageService(storageDir, repository);
        CleanupService cleanupService = new CleanupService(storageDir, repository, EXPIRATION);
        cleanupService.start();

        AuthService authService = new AuthService(userRepository);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/api/register", new AuthHandler(authService, AuthHandler.Mode.REGISTER));
        server.createContext("/api/login", new AuthHandler(authService, AuthHandler.Mode.LOGIN));
        server.createContext("/api/upload", new UploadHandler(storageService, authService));
        server.createContext("/api/files", new StatsHandler(repository, authService));
        server.createContext("/api/download", new DownloadHandler(storageService, authService, "/api/download/"));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.printf(Locale.ROOT, "FileSharingService started on http://localhost:%d%n", port);
    }

    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if ("/".equals(path) || "/index.html".equals(path) || "/login".equals(path)) {
                serveResource(exchange, "/public/login.html", "text/html; charset=utf-8");
                return;
            }
            if ("/profile".equals(path)) {
                serveResource(exchange, "/public/profile.html", "text/html; charset=utf-8");
                return;
            }
            if ("/login.js".equals(path)) {
                serveResource(exchange, "/public/login.js", "text/javascript; charset=utf-8");
                return;
            }
            if ("/profile.js".equals(path)) {
                serveResource(exchange, "/public/profile.js", "text/javascript; charset=utf-8");
                return;
            }
            if ("/download.js".equals(path)) {
                serveResource(exchange, "/public/download.js", "text/javascript; charset=utf-8");
                return;
            }
            if ("/styles.css".equals(path)) {
                serveResource(exchange, "/public/styles.css", "text/css; charset=utf-8");
                return;
            }
            if ("/download".equals(path) || path.startsWith("/download/")) {
                serveResource(exchange, "/public/download.html", "text/html; charset=utf-8");
                return;
            }
            sendText(exchange, 404, "Not found");
        }

        private void serveResource(HttpExchange exchange, String resource, String contentType) throws IOException {
            try (InputStream stream = FileSharingService.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    sendText(exchange, 404, "Not found");
                    return;
                }
                byte[] data = stream.readAllBytes();
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }
            }
        }
    }

    private record AuthHandler(AuthService authService, Mode mode) implements HttpHandler {
            enum Mode {REGISTER, LOGIN}

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendText(exchange, 405, "Method not allowed");
                    return;
                }
                String body = new String(readAllBytes(exchange.getRequestBody()), StandardCharsets.UTF_8);
                String username = SimpleJson.getString(body, "username");
                String password = SimpleJson.getString(body, "password");

                if (username == null || password == null) {
                    sendText(exchange, 400, "username/password required");
                    return;
                }

                Optional<String> token;
                if (mode == Mode.REGISTER) {
                    token = authService.register(username, password);
                } else {
                    token = authService.login(username, password);
                }

                if (token.isEmpty()) {
                    sendText(exchange, 401, "Invalid credentials");
                    return;
                }

                String response = "{\"token\":\"" + Json.escape(token.get()) + "\"}";
                byte[] data = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }
            }
        }

    private record UploadHandler(FileStorageService storageService, AuthService authService) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendText(exchange, 405, "Method not allowed");
                    return;
                }

                Optional<String> user = authService.requireUser(exchange);
                if (user.isEmpty()) {
                    sendText(exchange, 401, "Unauthorized");
                    return;
                }

                Headers headers = exchange.getRequestHeaders();
                String contentType = headers.getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase(Locale.US).startsWith("multipart/form-data")) {
                    sendText(exchange, 400, "Expected multipart/form-data");
                    return;
                }

                String boundary = MultipartFormDataParser.extractBoundary(contentType);
                if (boundary == null) {
                    sendText(exchange, 400, "Missing boundary");
                    return;
                }

                byte[] body = readAllBytes(exchange.getRequestBody());
                Optional<MultipartFormDataParser.Part> filePart = MultipartFormDataParser.parseSingleFile(body, boundary);
                if (filePart.isEmpty()) {
                    sendText(exchange, 400, "No file provided");
                    return;
                }

                MultipartFormDataParser.Part part = filePart.get();
                FileRecord record = storageService.store(user.get(), part.filename(), part.data());

                String host = Optional.ofNullable(headers.getFirst("Host")).orElse("localhost");
                String link = "http://" + host + "/download/" + record.token();
                String response = "{" +
                        "\"token\":\"" + Json.escape(record.token()) + "\"," +
                        "\"name\":\"" + Json.escape(record.originalName()) + "\"," +
                        "\"size\":" + record.size() + "," +
                        "\"uploadedAt\":\"" + record.uploadedAt() + "\"," +
                        "\"link\":\"" + Json.escape(link) + "\"" +
                        "}";
                byte[] data = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }
            }
        }

    private record DownloadHandler(FileStorageService storageService, AuthService authService,
                                   String basePath) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendText(exchange, 405, "Method not allowed");
                    return;
                }
                Optional<String> user = authService.requireUser(exchange);
                String path = exchange.getRequestURI().getPath();
                if (!path.startsWith(basePath)) {
                    sendText(exchange, 404, "Not found");
                    return;
                }
                String token = path.substring(basePath.length());
                if (token.isBlank()) {
                    sendText(exchange, 404, "Not found");
                    return;
                }
                if (user.isEmpty()) {
                    sendText(exchange, 401, "Unauthorized");
                    return;
                }

                Optional<FileRecord> recordOpt = storageService.find(token);
                if (recordOpt.isEmpty()) {
                    sendText(exchange, 404, "File not found");
                    return;
                }
                FileRecord record = recordOpt.get();
                Path filePath = record.storagePath();
                if (!Files.exists(filePath)) {
                    sendText(exchange, 404, "File not found");
                    return;
                }

                String contentType = Optional.ofNullable(URLConnection.guessContentTypeFromName(record.originalName()))
                        .orElse("application/octet-stream");
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename*=UTF-8''" + Urls.encode(record.originalName()));
                exchange.sendResponseHeaders(200, record.size());
                try (OutputStream out = exchange.getResponseBody()) {
                    Files.copy(filePath, out);
                }
                storageService.markDownloaded(token, Instant.now(), user.get());
            }
        }

    private record StatsHandler(FileRepository repository, AuthService authService) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendText(exchange, 405, "Method not allowed");
                    return;
                }
                Optional<String> user = authService.requireUser(exchange);
                if (user.isEmpty()) {
                    sendText(exchange, 401, "Unauthorized");
                    return;
                }
                String json = repository.toJsonForOwner(user.get());
                byte[] data = json.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(data);
                }
            }
        }

    private static void sendText(HttpExchange exchange, int status, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(data);
        }
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[8192];
            int read;
            while ((read = input.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toByteArray();
        }
    }
}
