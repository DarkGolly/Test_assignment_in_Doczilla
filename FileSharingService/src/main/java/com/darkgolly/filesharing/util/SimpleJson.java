package com.darkgolly.filesharing.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleJson {
    private static final Pattern STRING_PATTERN = Pattern.compile("\\\"%s\\\"\\s*:\\s*\\\"(.*?)\\\"");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\\"%s\\\"\\s*:\\s*(\\d+)");

    private SimpleJson() {
    }

    public static String getString(String json, String key) {
        if (json == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(String.format(STRING_PATTERN.pattern(), Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescape(matcher.group(1));
        }
        return null;
    }

    public static long getLong(String json, String key) {
        if (json == null) {
            return 0;
        }
        Pattern pattern = Pattern.compile(String.format(NUMBER_PATTERN.pattern(), Pattern.quote(key)));
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String unescape(String value) {
        return value.replace("\\\\\"", "\"")
                .replace("\\\\n", "\n")
                .replace("\\\\r", "\r")
                .replace("\\\\t", "\t")
                .replace("\\\\\\\\", "\\");
    }
}

