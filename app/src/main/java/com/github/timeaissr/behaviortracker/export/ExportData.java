package com.github.timeaissr.behaviortracker.export;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;

import java.util.List;

/**
 * Data transfer object for exporting/importing all app data as JSON.
 */
public class ExportData {

    private int version;
    private long exportTimestamp;
    private List<Behavior> behaviors;
    private List<Record> records;

    public ExportData() {
        this.version = 2;
        this.exportTimestamp = System.currentTimeMillis();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getExportTimestamp() {
        return exportTimestamp;
    }

    public void setExportTimestamp(long exportTimestamp) {
        this.exportTimestamp = exportTimestamp;
    }

    public List<Behavior> getBehaviors() {
        return behaviors;
    }

    public void setBehaviors(List<Behavior> behaviors) {
        this.behaviors = behaviors;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }
}
