package com.darkgolly.filesharing.security;

import com.darkgolly.filesharing.util.Json;
import com.darkgolly.filesharing.util.SimpleJson;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class Jwt {
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final long TTL_SECONDS = 3600 * 24;

    private final byte[] secret;

    public Jwt() {
        String env = System.getenv("JWT_SECRET_DOCZILLA");
        if (env != null && !env.isBlank()) {
            secret = env.getBytes(StandardCharsets.UTF_8);
        } else {
            secret = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    public String createToken(String username) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        long issuedAt = Instant.now().getEpochSecond();
        long exp = issuedAt + TTL_SECONDS;
        String payloadJson = "{\"sub\":\"" + Json.escape(username) + "\",\"iat\":" + issuedAt + ",\"exp\":" + exp + "}";
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public Optional<String> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        String signingInput = parts[0] + "." + parts[1];
        String expected = sign(signingInput);
        if (!constantTimeEquals(expected, parts[2])) {
            return Optional.empty();
        }
        String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
        String subject = SimpleJson.getString(payloadJson, "sub");
        long exp = SimpleJson.getLong(payloadJson, "exp");
        if (subject == null || exp == 0) {
            return Optional.empty();
        }
        if (Instant.now().getEpochSecond() > exp) {
            return Optional.empty();
        }
        return Optional.of(subject);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret, HMAC_ALGO));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64Url(sig);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign token", e);
        }
    }

    private String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] base64UrlDecode(String data) {
        return Base64.getUrlDecoder().decode(data);
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}


