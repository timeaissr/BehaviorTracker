package com.github.timeaissr.behaviortracker.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a behavior definition that the user wants to track.
 */
@Entity(tableName = "behaviors")
public class Behavior {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;

    private RecordType recordType;

    /** Unit for numeric records (e.g., "ml", "km"). Null for boolean type. */
    private String unit;

    /** Color hex string for the behavior card. */
    private String color;

    /** Timestamp when this behavior was created. */
    private long createdAt;

    /** Whether this behavior is hidden from the active behavior list. */
    private boolean archived;

    public Behavior() {
        this.createdAt = System.currentTimeMillis();
        this.archived = false;
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(RecordType recordType) {
        this.recordType = recordType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

}
