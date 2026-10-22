package com.github.timeaissr.behaviortracker.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.TimeZone;

public class DateUtilsTest {

    private TimeZone originalTimeZone;

    @Before
    public void setUp() {
        originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Singapore"));
    }

    @After
    public void tearDown() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void datePickerSelectionUsesTheLocalCalendarDateAtUtcMidnight() {
        long localShortlyAfterMidnight = LocalDate.of(2026, 9, 11)
                .atTime(0, 30)
                .atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli();

        assertEquals(
                Instant.parse("2026-09-11T00:00:00Z").toEpochMilli(),
                DateUtils.toDatePickerSelection(localShortlyAfterMidnight));
    }

    @Test
    public void choosingDateKeepsLocalTimeAcrossMonthAndYearBoundaries() {
        long original = localTimestamp("2026-12-31T23:45:51");
        long selection = Instant.parse("2024-02-29T00:00:00Z").toEpochMilli();

        assertEquals(localTimestamp("2024-02-29T23:45:00"),
                DateUtils.withDatePickerSelection(original, selection, true));
    }

    @Test
    public void choosingDateInNegativeOffsetDoesNotSelectThePreviousDay() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        long original = localTimestamp("2026-10-23T18:12:00");
        long selection = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli();

        assertEquals(localTimestamp("2026-10-01T18:12:00"),
                DateUtils.withDatePickerSelection(original, selection, true));
    }

    @Test
    public void dateOnlyRecordUsesLocalMidnight() {
        long original = localTimestamp("2026-10-23T18:12:00");
        long selection = Instant.parse("2026-10-01T00:00:00Z").toEpochMilli();

        assertEquals(localTimestamp("2026-10-01T00:00:00"),
                DateUtils.withDatePickerSelection(original, selection, false));
    }

    @Test
    public void minuteButtonsCarryAcrossMidnightAndYearBoundary() {
        long lastMinute = localTimestamp("2026-12-31T23:59:00");
        long firstMinute = localTimestamp("2027-01-01T00:00:00");

        assertEquals(firstMinute, DateUtils.addMinutes(lastMinute, 1));
        assertEquals(lastMinute, DateUtils.addMinutes(firstMinute, -1));
    }

    @Test
    public void hourButtonsKeepMinutesAndCarryAcrossLeapDay() {
        long original = localTimestamp("2024-03-01T00:30:00");
        long previous = localTimestamp("2024-02-29T23:30:00");

        assertEquals(previous, DateUtils.addMinutes(original, -60));
        assertEquals(original, DateUtils.addMinutes(previous, 60));
    }

    @Test
    public void timeInputKeepsDateAndClearsSecondsAndMilliseconds() {
        long original = localTimestamp("2026-10-23T18:12:51.123");

        assertEquals(localTimestamp("2026-10-23T00:05:00"), DateUtils.withTime(original, 0, 5));
        assertEquals(localTimestamp("2026-10-23T23:59:00"), DateUtils.withTime(original, 23, 59));
    }

    @Test
    public void invalidTimeDoesNotRollIntoAnotherDay() {
        long original = localTimestamp("2026-10-23T18:12:00");

        assertThrows(DateTimeException.class, () -> DateUtils.withTime(original, 24, 0));
        assertThrows(DateTimeException.class, () -> DateUtils.withTime(original, 12, 60));
        assertThrows(DateTimeException.class, () -> DateUtils.withTime(original, -1, 0));
    }

    @Test
    public void minuteButtonsSkipDaylightSavingGap() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        long beforeGap = localTimestamp("2026-03-08T01:59:00");
        long afterGap = localTimestamp("2026-03-08T03:00:00");

        assertEquals(afterGap, DateUtils.addMinutes(beforeGap, 1));
        assertEquals(beforeGap, DateUtils.addMinutes(afterGap, -1));
    }

    @Test
    public void dateAndTimeInputResolveNonexistentLocalTimeConsistently() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        long selection = Instant.parse("2026-03-08T00:00:00Z").toEpochMilli();

        assertEquals(localTimestamp("2026-03-08T03:30:00"), DateUtils.withDatePickerSelection(
                localTimestamp("2026-03-07T02:30:00"), selection, true));
        assertEquals(localTimestamp("2026-03-08T03:30:00"),
                DateUtils.withTime(localTimestamp("2026-03-08T00:00:00"), 2, 30));
    }

    @Test
    public void minuteButtonsCrossDaylightSavingOverlapWithoutLosingTheOffset() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
        long beforeOverlap = Instant.parse("2026-11-01T05:59:00Z").toEpochMilli();
        long afterOverlap = Instant.parse("2026-11-01T06:00:00Z").toEpochMilli();

        assertEquals(afterOverlap, DateUtils.addMinutes(beforeOverlap, 1));
        assertEquals(beforeOverlap, DateUtils.addMinutes(afterOverlap, -1));
        assertEquals(afterOverlap, DateUtils.withTime(afterOverlap, 1, 0));
    }

    private long localTimestamp(String dateTime) {
        return LocalDateTime.parse(dateTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
