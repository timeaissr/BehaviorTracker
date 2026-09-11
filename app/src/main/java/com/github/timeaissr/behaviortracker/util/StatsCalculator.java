package com.github.timeaissr.behaviortracker.util;

import com.github.timeaissr.behaviortracker.data.entity.Record;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Pure date/statistics calculations shared by the detail screen and unit tests. */
public final class StatsCalculator {

    private StatsCalculator() {}

    public static Result calculate(List<Record> records, boolean isBoolean, LocalDate today) {
        Result result = new Result();
        if (records == null || records.isEmpty()) {
            return result;
        }

        long earliestTimestamp = Long.MAX_VALUE;
        Set<LocalDate> uniqueDays = new HashSet<>();
        result.totalCount = records.size();
        for (Record record : records) {
            result.totalSum += record.getValue();
            earliestTimestamp = Math.min(earliestTimestamp, record.getTimestamp());
            uniqueDays.add(DateUtils.toLocalDate(record.getTimestamp()));
        }

        if (isBoolean) {
            calculateStreaks(uniqueDays, today, result);
        } else {
            LocalDate earliestDay = DateUtils.toLocalDate(earliestTimestamp);
            long elapsedDays = Math.max(1, ChronoUnit.DAYS.between(earliestDay, today) + 1);
            result.dailyAverage = result.totalSum / elapsedDays;
        }
        return result;
    }

    private static void calculateStreaks(Set<LocalDate> days, LocalDate today, Result result) {
        LocalDate checkDay = today;
        while (days.contains(checkDay)) {
            result.currentStreak++;
            checkDay = checkDay.minusDays(1);
        }

        LocalDate previous = null;
        int streak = 0;
        for (LocalDate day : new TreeSet<>(days)) {
            if (previous != null && day.equals(previous.plusDays(1))) {
                streak++;
            } else {
                streak = 1;
            }
            result.longestStreak = Math.max(result.longestStreak, streak);
            previous = day;
        }
    }

    public static final class Result {
        public int totalCount;
        public double totalSum;
        public int currentStreak;
        public int longestStreak;
        public double dailyAverage;
    }
}
