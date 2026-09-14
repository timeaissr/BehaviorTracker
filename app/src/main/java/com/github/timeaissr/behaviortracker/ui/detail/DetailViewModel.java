package com.github.timeaissr.behaviortracker.ui.detail;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.model.NumericStats;
import com.github.timeaissr.behaviortracker.data.repository.BehaviorRepository;
import com.github.timeaissr.behaviortracker.util.StatsCalculator;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class DetailViewModel extends AndroidViewModel {

    private final BehaviorRepository repository;
    private final AtomicLong statsGeneration = new AtomicLong();

    public DetailViewModel(@NonNull Application application) {
        super(application);
        repository = new BehaviorRepository(application);
    }

    public LiveData<Behavior> getBehavior(long id) {
        return repository.getBehaviorById(id);
    }

    public LiveData<List<Record>> getRecords(long behaviorId) {
        return repository.getRecordsForBehavior(behaviorId);
    }

    public LiveData<List<Record>> getRecordsInRange(long behaviorId, long startTime, long endTime) {
        return repository.getRecordsInRange(behaviorId, startTime, endTime);
    }

    /**
     * Calculate stats in background and post results.
     */
    public LiveData<StatsData> calculateStats(long behaviorId, boolean isBoolean) {
        if (!isBoolean) {
            return calculateNumericStats(behaviorId);
        }

        MediatorLiveData<StatsData> result = new MediatorLiveData<>();
        LiveData<List<Record>> recordsSource = repository.getRecordsForBehavior(behaviorId);
        result.addSource(recordsSource, records -> {
            long generation = statsGeneration.incrementAndGet();
            repository.getExecutor().execute(() -> {
                StatsCalculator.Result calculated = StatsCalculator.calculate(
                        records, true, LocalDate.now());
                StatsData stats = toStatsData(calculated);
                if (statsGeneration.get() == generation) {
                    result.postValue(stats);
                }
            });
        });
        return result;
    }

    private LiveData<StatsData> calculateNumericStats(long behaviorId) {
        MediatorLiveData<StatsData> result = new MediatorLiveData<>();
        LiveData<NumericStats> statsSource = repository.getNumericStats(behaviorId);
        result.addSource(statsSource, snapshot -> {
            long generation = statsGeneration.incrementAndGet();
            repository.getExecutor().execute(() -> {
                NumericStats safeSnapshot = snapshot == null ? new NumericStats() : snapshot;
                StatsCalculator.Result calculated = StatsCalculator.calculateNumeric(
                        safeSnapshot.totalCount,
                        safeSnapshot.totalSum,
                        safeSnapshot.earliestTimestamp,
                        LocalDate.now());
                if (statsGeneration.get() == generation) {
                    result.postValue(toStatsData(calculated));
                }
            });
        });
        return result;
    }

    private static StatsData toStatsData(StatsCalculator.Result calculated) {
        StatsData stats = new StatsData();
        stats.totalCount = calculated.totalCount;
        stats.totalSum = calculated.totalSum;
        stats.currentStreak = calculated.currentStreak;
        stats.longestStreak = calculated.longestStreak;
        stats.dailyAverage = calculated.dailyAverage;
        return stats;
    }

    public void insertBooleanRecord(long behaviorId, long timestamp,
                                    BehaviorRepository.OnOperationCallback callback) {
        Record record = new Record();
        record.setBehaviorId(behaviorId);
        record.setValue(1.0);
        record.setTimestamp(timestamp);
        repository.insertRecord(record, callback);
    }

    public void insertNumericRecord(long behaviorId, double value, String note, long timestamp,
                                    BehaviorRepository.OnOperationCallback callback) {
        if (!Double.isFinite(value)) {
            if (callback != null) {
                callback.onComplete(false);
            }
            return;
        }
        Record record = new Record();
        record.setBehaviorId(behaviorId);
        record.setValue(value);
        record.setNote(note);
        record.setTimestamp(timestamp);
        repository.insertRecord(record, callback);
    }

    public void deleteRecord(Record record) {
        repository.deleteRecord(record);
    }

    @Override
    protected void onCleared() {
        repository.shutdown();
    }

    /** Data holder for computed statistics. */
    public static class StatsData {
        public int totalCount;
        public double totalSum;
        public int currentStreak;
        public int longestStreak;
        public double dailyAverage;
    }
}
