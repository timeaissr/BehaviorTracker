package com.github.timeaissr.behaviortracker.data.entity;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Represents a single record/log entry for a behavior.
 * For BOOLEAN behaviors, the existence of a record means "yes".
 * For NUMERIC behaviors, the value field stores the numeric amount.
 */
@Entity(
    tableName = "records",
    foreignKeys = @ForeignKey(
        entity = Behavior.class,
        parentColumns = "id",
        childColumns = "behaviorId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index("behaviorId"),
        @Index("timestamp")
    }
)
public class Record {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long behaviorId;

    /** Timestamp when this record was logged. */
    private long timestamp;

    /** Numeric value. For boolean type, this is always 1.0. */
    private double value;

    public Record() {
        this.timestamp = System.currentTimeMillis();
        this.value = 1.0;
    }

    // Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBehaviorId() {
        return behaviorId;
    }

    public void setBehaviorId(long behaviorId) {
        this.behaviorId = behaviorId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

}
