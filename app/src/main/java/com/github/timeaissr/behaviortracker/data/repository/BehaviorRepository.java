package com.github.timeaissr.behaviortracker.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.github.timeaissr.behaviortracker.data.AppDatabase;
import com.github.timeaissr.behaviortracker.data.dao.BehaviorDao;
import com.github.timeaissr.behaviortracker.data.dao.RecordDao;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.github.timeaissr.behaviortracker.data.model.NumericStats;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source of truth for all data operations.
 * Mediates between ViewModels and Room DAOs.
 */
public class BehaviorRepository {

    private final BehaviorDao behaviorDao;
    private final RecordDao recordDao;
    private final ExecutorService executor;

    public BehaviorRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        behaviorDao = db.behaviorDao();
        recordDao = db.recordDao();
        executor = Executors.newSingleThreadExecutor();
    }

    // ==================== Behavior Operations ====================

    public LiveData<List<Behavior>> getAllBehaviors() {
        return behaviorDao.getAll();
    }

    public LiveData<Behavior> getBehaviorById(long id) {
        return behaviorDao.getById(id);
    }

    public void insertBehavior(Behavior behavior, OnInsertCallback callback) {
        executor.execute(() -> {
            long id;
            try {
                id = behaviorDao.insert(behavior);
            } catch (RuntimeException e) {
                id = -1;
            }
            if (callback != null) {
                callback.onInserted(id);
            }
        });
    }

    /** Update user-editable fields while preserving identity and creation metadata. */
    public void updateBehavior(long behaviorId, Behavior changes, OnOperationCallback callback) {
        executor.execute(() -> {
            boolean success = false;
            try {
                Behavior existing = behaviorDao.getByIdSync(behaviorId);
                if (existing != null) {
                    changes.setId(existing.getId());
                    changes.setCreatedAt(existing.getCreatedAt());
                    // A behavior's type defines how every existing record is interpreted.
                    changes.setRecordType(existing.getRecordType());
                    if (existing.getRecordType() == RecordType.NUMERIC) {
                        if (changes.getUnit() == null || changes.getUnit().trim().isEmpty()) {
                            changes.setUnit(existing.getUnit());
                        }
                    } else {
                        changes.setUnit(null);
                    }
                    behaviorDao.update(changes);
                    success = true;
                }
            } catch (RuntimeException ignored) {
                // Report failure through the callback.
            }
            if (callback != null) {
                callback.onComplete(success);
            }
        });
    }

    public void deleteBehavior(long behaviorId, OnOperationCallback callback) {
        executor.execute(() -> {
            boolean success = false;
            try {
                Behavior behavior = behaviorDao.getByIdSync(behaviorId);
                if (behavior != null) {
                    behaviorDao.delete(behavior);
                    success = true;
                }
            } catch (RuntimeException ignored) {
                // Report failure through the callback.
            }
            if (callback != null) {
                callback.onComplete(success);
            }
        });
    }

    // ==================== Record Operations ====================

    public LiveData<List<Record>> getRecordsForBehavior(long behaviorId) {
        return recordDao.getRecordsForBehavior(behaviorId);
    }

    public LiveData<List<Record>> getRecordsInRange(long behaviorId, long startTime, long endTime) {
        return recordDao.getRecordsInRange(behaviorId, startTime, endTime);
    }

    public LiveData<Integer> getRecordCountForDay(long behaviorId, long dayStart, long dayEnd) {
        return recordDao.getRecordCountForDay(behaviorId, dayStart, dayEnd);
    }

    public LiveData<Double> getSumInRange(long behaviorId, long startTime, long endTime) {
        return recordDao.getSumInRange(behaviorId, startTime, endTime);
    }

    public LiveData<NumericStats> getNumericStats(long behaviorId) {
        return recordDao.getNumericStats(behaviorId);
    }

    public void insertRecord(Record record) {
        insertRecord(record, null);
    }

    public void insertRecord(Record record, OnOperationCallback callback) {
        executor.execute(() -> {
            boolean success = false;
            try {
                recordDao.insert(record);
                success = true;
            } catch (RuntimeException ignored) {
                // Report failure through the callback.
            }
            if (callback != null) {
                callback.onComplete(success);
            }
        });
    }

    public void deleteRecord(Record record) {
        executor.execute(() -> recordDao.delete(record));
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public void shutdown() {
        executor.shutdown();
    }

    // Callback interface
    public interface OnInsertCallback {
        void onInserted(long id);
    }

    public interface OnOperationCallback {
        void onComplete(boolean success);
    }
}
