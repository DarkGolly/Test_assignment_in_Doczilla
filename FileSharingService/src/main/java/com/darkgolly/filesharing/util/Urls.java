package com.darkgolly.filesharing.util;

import java.nio.charset.StandardCharsets;

public class Urls {
    private Urls() {
    }

    public static String encode(String value) {
        StringBuilder builder = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xff;
            if (isUnreserved(c)) {
                builder.append((char) c);
            } else {
                builder.append('%');
                builder.append(Character.toUpperCase(Character.forDigit(c >> 4, 16)));
                builder.append(Character.toUpperCase(Character.forDigit(c & 0x0f, 16)));
            }
        }
        return builder.toString();
    }

    private static boolean isUnreserved(int c) {
        return (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9')
                || c == '-' || c == '.' || c == '_' || c == '~';
    }
}

