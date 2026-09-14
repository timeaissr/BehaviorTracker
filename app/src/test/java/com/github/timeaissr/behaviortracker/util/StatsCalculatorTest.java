package com.github.timeaissr.behaviortracker.util;

import static org.junit.Assert.assertEquals;

import com.github.timeaissr.behaviortracker.data.entity.Record;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;

public class StatsCalculatorTest {

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
    public void booleanStatsUseUniqueDaysForStreaksButCountEveryRecord() {
        LocalDate today = LocalDate.of(2026, 9, 11);
        StatsCalculator.Result result = StatsCalculator.calculate(Arrays.asList(
                record(today, 1),
                record(today, 1),
                record(today.minusDays(1), 1)), true, today);

        assertEquals(3, result.totalCount);
        assertEquals(2, result.currentStreak);
        assertEquals(2, result.longestStreak);
    }

    @Test
    public void numericAverageUsesCalendarDays() {
        LocalDate today = LocalDate.of(2026, 9, 11);
        StatsCalculator.Result result = StatsCalculator.calculate(Arrays.asList(
                record(today.minusDays(2), 3),
                record(today, 6)), false, today);

        assertEquals(2, result.totalCount);
        assertEquals(9, result.totalSum, 0.0001);
        assertEquals(3, result.dailyAverage, 0.0001);
    }

    @Test
    public void numericAggregateUsesCalendarDays() {
        LocalDate today = LocalDate.of(2026, 9, 11);
        long earliestTimestamp = today.minusDays(2).atTime(12, 0)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        StatsCalculator.Result result = StatsCalculator.calculateNumeric(
                2, 9, earliestTimestamp, today);

        assertEquals(2, result.totalCount);
        assertEquals(9, result.totalSum, 0.0001);
        assertEquals(3, result.dailyAverage, 0.0001);
    }

    private static Record record(LocalDate day, double value) {
        Record record = new Record();
        record.setTimestamp(day.atTime(12, 0).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli());
        record.setValue(value);
        return record;
    }
}
