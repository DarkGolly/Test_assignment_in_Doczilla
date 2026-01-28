package com.darkgolly.filesharing.util;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

public class MultipartFormDataParser {
    public record Part(String filename, byte[] data) {
    }

    public static String extractBoundary(String contentType) {
        String[] parts = contentType.split(";");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.toLowerCase(Locale.US).startsWith("boundary=")) {
                return trimmed.substring("boundary=".length());
            }
        }
        return null;
    }

    public static Optional<Part> parseSingleFile(byte[] body, String boundary) {
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        byte[] delimiter = "\r\n\r\n".getBytes(StandardCharsets.ISO_8859_1);

        int start = indexOf(body, boundaryBytes, 0);
        if (start < 0) {
            return Optional.empty();
        }
        int headersStart = start + boundaryBytes.length + 2; // skip \r\n
        int headersEnd = indexOf(body, delimiter, headersStart);
        if (headersEnd < 0) {
            return Optional.empty();
        }

        String headers = new String(body, headersStart, headersEnd - headersStart, StandardCharsets.ISO_8859_1);
        String filename = extractFilename(headers);
        if (filename == null) {
            return Optional.empty();
        }

        int dataStart = headersEnd + delimiter.length;
        int endBoundary = indexOf(body, boundaryBytes, dataStart) - 2; // trim \r\n before boundary
        if (endBoundary < dataStart) {
            return Optional.empty();
        }
        byte[] data = new byte[endBoundary - dataStart];
        System.arraycopy(body, dataStart, data, 0, data.length);
        return Optional.of(new Part(filename, data));
    }

    private static String extractFilename(String headers) {
        String[] lines = headers.split("\r\n");
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.US);
            if (lower.startsWith("content-disposition:")) {
                int idx = line.indexOf("filename=");
                if (idx >= 0) {
                    String value = line.substring(idx + "filename=".length()).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            }
        }
        return null;
    }

    private static int indexOf(byte[] data, byte[] pattern, int from) {
        outer:
        for (int i = from; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}

