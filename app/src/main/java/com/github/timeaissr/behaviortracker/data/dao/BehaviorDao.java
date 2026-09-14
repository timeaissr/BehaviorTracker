package com.github.timeaissr.behaviortracker.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;

import java.util.List;

@Dao
public interface BehaviorDao {

    @Insert
    long insert(Behavior behavior);

    @Update
    void update(Behavior behavior);

    @Delete
    void delete(Behavior behavior);

    @Query("SELECT * FROM behaviors WHERE archived = 0 ORDER BY createdAt DESC")
    LiveData<List<Behavior>> getAllActive();

    @Query("SELECT * FROM behaviors ORDER BY createdAt DESC")
    LiveData<List<Behavior>> getAll();

    @Query("SELECT * FROM behaviors WHERE id = :id")
    LiveData<Behavior> getById(long id);

    @Query("SELECT * FROM behaviors WHERE id = :id")
    Behavior getByIdSync(long id);

    @Query("SELECT * FROM behaviors ORDER BY createdAt DESC")
    List<Behavior> getAllSync();
}
