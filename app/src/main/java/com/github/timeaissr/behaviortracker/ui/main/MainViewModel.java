package com.github.timeaissr.behaviortracker.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.github.timeaissr.behaviortracker.data.repository.BehaviorRepository;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private final BehaviorRepository repository;
    private final LiveData<List<Behavior>> allBehaviors;

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = new BehaviorRepository(application);
        allBehaviors = repository.getAllBehaviors();
    }

    public LiveData<List<Behavior>> getAllBehaviors() {
        return allBehaviors;
    }

    public LiveData<Integer> getRecordCountForDay(long behaviorId, long dayStart, long dayEnd) {
        return repository.getRecordCountForDay(behaviorId, dayStart, dayEnd);
    }

    public LiveData<Double> getSumInRange(long behaviorId, long startTime, long endTime) {
        return repository.getSumInRange(behaviorId, startTime, endTime);
    }

    /** Quick-log a boolean behavior (just inserts a record with value 1). */
    public void quickLogBoolean(long behaviorId) {
        quickLogBoolean(behaviorId, System.currentTimeMillis(), null);
    }

    /** Quick-log a boolean behavior with custom timestamp. */
    public void quickLogBoolean(long behaviorId, long timestamp) {
        quickLogBoolean(behaviorId, timestamp, null);
    }

    public void quickLogBoolean(long behaviorId, long timestamp,
                                BehaviorRepository.OnOperationCallback callback) {
        Record record = new Record();
        record.setBehaviorId(behaviorId);
        record.setValue(1.0);
        record.setTimestamp(timestamp);
        repository.insertRecord(record, callback);
    }

    /** Quick-log a numeric behavior with a given value and optional note. */
    public void quickLogNumeric(long behaviorId, double value, String note) {
        quickLogNumeric(behaviorId, value, note, System.currentTimeMillis());
    }

    /** Quick-log a numeric behavior with custom timestamp. */
    public void quickLogNumeric(long behaviorId, double value, String note, long timestamp) {
        quickLogNumeric(behaviorId, value, note, timestamp, null);
    }

    public void quickLogNumeric(long behaviorId, double value, String note, long timestamp,
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

    @Override
    protected void onCleared() {
        repository.shutdown();
    }
}
