package com.darkgolly.filesharing.service;

import com.darkgolly.filesharing.model.User;
import com.darkgolly.filesharing.repository.UserRepository;
import com.darkgolly.filesharing.security.Jwt;
import com.darkgolly.filesharing.security.Passwords;
import com.sun.net.httpserver.HttpExchange;

import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;
    private final Jwt jwt;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.jwt = new Jwt();
    }

    public Optional<String> register(String username, String password) {
        String normalized = normalize(username);
        if (normalized == null || password == null || password.isBlank()) {
            return Optional.empty();
        }
        if (userRepository.findByUsername(normalized).isPresent()) {
            return Optional.empty();
        }
        String salt = Passwords.salt();
        String hash = Passwords.hash(password, salt);
        if (!userRepository.create(normalized, hash, salt)) {
            return Optional.empty();
        }
        return Optional.of(jwt.createToken(normalized));
    }

    public Optional<String> login(String username, String password) {
        String normalized = normalize(username);
        if (normalized == null || password == null) {
            return Optional.empty();
        }
        Optional<User> userOpt = userRepository.findByUsername(normalized);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        String expected = Passwords.hash(password, user.salt());
        if (!expected.equals(user.passwordHash())) {
            return Optional.empty();
        }
        return Optional.of(jwt.createToken(normalized));
    }

    public Optional<String> requireUser(HttpExchange exchange) {
        return jwt.verify(extractToken(exchange));
    }

    private String extractToken(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Authorization");
        if (header == null) {
            return null;
        }
        if (header.toLowerCase().startsWith("bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    private String normalize(String username) {
        if (username == null) {
            return null;
        }
        String trimmed = username.trim().toLowerCase();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
