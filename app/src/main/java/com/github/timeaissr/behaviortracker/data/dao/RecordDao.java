package com.github.timeaissr.behaviortracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.github.timeaissr.behaviortracker.data.entity.Record;

import java.util.List;

@Dao
public interface RecordDao {

    @Insert
    long insert(Record record);

    @Delete
    void delete(Record record);

    /** Get all records for a behavior, ordered by newest first. */
    @Query("SELECT * FROM records WHERE behaviorId = :behaviorId ORDER BY timestamp DESC")
    LiveData<List<Record>> getRecordsForBehavior(long behaviorId);

    /** Get records for a behavior within a time range. */
    @Query("SELECT * FROM records WHERE behaviorId = :behaviorId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    LiveData<List<Record>> getRecordsInRange(long behaviorId, long startTime, long endTime);

    /** Get all records (for export). */
    @Query("SELECT * FROM records ORDER BY timestamp ASC")
    List<Record> getAllSync();

    /** Check if a boolean behavior has been logged today. */
    @Query("SELECT COUNT(*) FROM records WHERE behaviorId = :behaviorId AND timestamp BETWEEN :dayStart AND :dayEnd")
    LiveData<Integer> getRecordCountForDay(long behaviorId, long dayStart, long dayEnd);

    /** Get sum of values for a behavior within a time range (for numeric stats). */
    @Query("SELECT COALESCE(SUM(value), 0) FROM records WHERE behaviorId = :behaviorId AND timestamp BETWEEN :startTime AND :endTime")
    LiveData<Double> getSumInRange(long behaviorId, long startTime, long endTime);

    /** Insert multiple records (for import). */
    @Insert
    void insertAll(List<Record> records);

    /** Delete all records. */
    @Query("DELETE FROM records")
    void deleteAll();
}
