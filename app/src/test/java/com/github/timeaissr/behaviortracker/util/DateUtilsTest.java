package com.github.timeaissr.behaviortracker.util;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
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
}
