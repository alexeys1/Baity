package com.shyeuar.baity.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParseUtils {

    private static final Pattern DURATION_PART_PATTERN = Pattern.compile(
            "(\\d+)\\s*(weeks?|w|days?|d|hours?|h|minutes?|mins?|min|m|seconds?|secs?|sec|s)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private DurationParseUtils() {
    }

    public static long parseLongDurationToSeconds(String text) {
        if (text == null || text.isEmpty()) {
            return 0L;
        }
        String normalized = text.toLowerCase()
                .replace(',', ' ')
                .replace(" and ", " ");
        Matcher matcher = DURATION_PART_PATTERN.matcher(normalized);
        long total = 0L;
        boolean matched = false;
        while (matcher.find()) {
            matched = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toLowerCase();
            if (unit.startsWith("w")) {
                total += value * 7L * 24L * 3600L;
            } else if (unit.startsWith("d")) {
                total += value * 24L * 3600L;
            } else if (unit.startsWith("h")) {
                total += value * 3600L;
            } else if (unit.startsWith("m")) {
                total += value * 60L;
            } else if (unit.startsWith("s")) {
                total += value;
            }
        }
        return matched ? total : 0L;
    }
}