package com.github.timeaissr.behaviortracker.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date/time operations.
 */
public final class DateUtils {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateUtils() {}

    /** Get the start of today (00:00:00.000). */
    public static long getStartOfDay() {
        return getStartOfDay(System.currentTimeMillis());
    }

    /** Get the start of the day for a given timestamp. */
    public static long getStartOfDay(long timestamp) {
        return toLocalDate(timestamp).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
    }

    /** Get the end of today (23:59:59.999). */
    public static long getEndOfDay() {
        return getEndOfDay(System.currentTimeMillis());
    }

    /** Get the end of the day for a given timestamp. */
    public static long getEndOfDay(long timestamp) {
        return toLocalDate(timestamp).plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli() - 1;
    }

    /** Get start of N days ago. */
    public static long getStartOfDaysAgo(int days) {
        return LocalDate.now().minusDays(days).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
    }

    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(toZonedDateTime(timestamp));
    }

    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(toZonedDateTime(timestamp));
    }

    public static String formatDateTime(long timestamp) {
        return DATETIME_FORMAT.format(toZonedDateTime(timestamp));
    }

    /** Get day of week (1=Sunday, 7=Saturday). */
    public static int getDayOfWeek(long timestamp) {
        // Preserve java.util.Calendar's numbering: Sunday=1, Saturday=7.
        return toLocalDate(timestamp).getDayOfWeek().getValue() % 7 + 1;
    }

    /** Check if two timestamps are on the same day. */
    public static boolean isSameDay(long timestamp1, long timestamp2) {
        return toLocalDate(timestamp1).equals(toLocalDate(timestamp2));
    }

    public static LocalDate toLocalDate(long timestamp) {
        return toZonedDateTime(timestamp).toLocalDate();
    }

    private static java.time.ZonedDateTime toZonedDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());
    }
}
