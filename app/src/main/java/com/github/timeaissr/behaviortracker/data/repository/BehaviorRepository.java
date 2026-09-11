package com.github.timeaissr.behaviortracker.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.github.timeaissr.behaviortracker.data.AppDatabase;
import com.github.timeaissr.behaviortracker.data.dao.BehaviorDao;
import com.github.timeaissr.behaviortracker.data.dao.RecordDao;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single source of truth for all data operations.
 * Mediates between ViewModels and Room DAOs.
 */
public class BehaviorRepository {

    private final AppDatabase db;
    private final BehaviorDao behaviorDao;
    private final RecordDao recordDao;
    private final ExecutorService executor;

    public BehaviorRepository(Application application) {
        db = AppDatabase.getInstance(application);
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

    public void insertRecord(Record record) {
        executor.execute(() -> recordDao.insert(record));
    }

    public void insertBooleanRecordIfAbsent(Record record, OnBooleanInsertCallback callback) {
        executor.execute(() -> {
            long dayStart = com.github.timeaissr.behaviortracker.util.DateUtils
                    .getStartOfDay(record.getTimestamp());
            long dayEnd = com.github.timeaissr.behaviortracker.util.DateUtils
                    .getEndOfDay(record.getTimestamp());
            final boolean[] inserted = {false};
            try {
                db.runInTransaction(() -> {
                    if (recordDao.getRecordCountForDaySync(
                            record.getBehaviorId(), dayStart, dayEnd) == 0) {
                        recordDao.insert(record);
                        inserted[0] = true;
                    }
                });
            } catch (RuntimeException ignored) {
                // Treat database failures as an unsuccessful insert.
            }
            if (callback != null) {
                callback.onComplete(inserted[0]);
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

    public interface OnBooleanInsertCallback {
        void onComplete(boolean inserted);
    }

    public interface OnOperationCallback {
        void onComplete(boolean success);
    }
}
