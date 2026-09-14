package com.github.timeaissr.behaviortracker.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.google.gson.JsonParser;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class BackupValidatorTest {

    @Test
    public void acceptsValidCurrentBackup() {
        ExportData data = validBackup();

        assertTrue(BackupValidator.isValid(data));
    }

    @Test
    public void rejectsRecordWhoseBehaviorDoesNotExist() {
        ExportData data = validBackup();
        data.getRecords().get(0).setBehaviorId(999);

        assertFalse(BackupValidator.isValid(data));
    }

    @Test
    public void rejectsNonFiniteNumericValue() {
        ExportData data = validBackup();
        data.getRecords().get(0).setValue(Double.NaN);

        assertFalse(BackupValidator.isValid(data));
    }

    @Test
    public void rejectsUnsupportedBackupVersion() {
        ExportData data = validBackup();
        data.setVersion(3);

        assertFalse(BackupValidator.isValid(data));
    }

    @Test
    public void acceptsMultipleBooleanRecordsOnTheSameDay() {
        Behavior behavior = new Behavior();
        behavior.setId(1);
        behavior.setName("运动");
        behavior.setRecordType(RecordType.BOOLEAN);

        long timestamp = System.currentTimeMillis();
        Record first = record(1, 1, timestamp, 1.0);
        Record second = record(2, 1, timestamp + 1_000, 1.0);

        ExportData data = new ExportData();
        data.setBehaviors(Collections.singletonList(behavior));
        data.setRecords(Arrays.asList(first, second));

        assertTrue(BackupValidator.isValid(data));
    }

    @Test
    public void rejectsBooleanRecordWithNonUnitValue() {
        Behavior behavior = new Behavior();
        behavior.setId(1);
        behavior.setName("运动");
        behavior.setRecordType(RecordType.BOOLEAN);

        ExportData data = new ExportData();
        data.setBehaviors(Collections.singletonList(behavior));
        data.setRecords(Collections.singletonList(
                record(1, 1, System.currentTimeMillis(), 42.0)));

        assertFalse(BackupValidator.isValid(data));
    }

    @Test
    public void rejectsJsonBehaviorWithoutCreatedAt() {
        String json = "{\"version\":2,\"behaviors\":[{\"id\":1,\"name\":\"运动\","
                + "\"recordType\":\"BOOLEAN\"}],\"records\":[]}";

        assertFalse(BackupValidator.hasRequiredJsonFields(
                JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    public void rejectsJsonRecordWithoutTimestamp() {
        String json = "{\"version\":2,\"behaviors\":[],"
                + "\"records\":[{\"id\":1,\"behaviorId\":1,\"value\":1.0}]}";

        assertFalse(BackupValidator.hasRequiredJsonFields(
                JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    public void rejectsJsonRecordWithoutValue() {
        String json = "{\"version\":2,\"behaviors\":[],"
                + "\"records\":[{\"id\":1,\"behaviorId\":1,\"timestamp\":0}]}";

        assertFalse(BackupValidator.hasRequiredJsonFields(
                JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    public void acceptsRecordAtUnixEpoch() {
        ExportData data = validBackup();
        data.getRecords().get(0).setTimestamp(0);

        assertTrue(BackupValidator.isValid(data));
    }

    private static ExportData validBackup() {
        Behavior behavior = new Behavior();
        behavior.setId(1);
        behavior.setName("喝水");
        behavior.setRecordType(RecordType.NUMERIC);
        behavior.setUnit("ml");
        behavior.setColor("#64B5F6");

        Record record = new Record();
        record.setId(1);
        record.setBehaviorId(1);
        record.setTimestamp(System.currentTimeMillis());
        record.setValue(250);

        ExportData data = new ExportData();
        data.setBehaviors(Collections.singletonList(behavior));
        data.setRecords(Collections.singletonList(record));
        return data;
    }

    private static Record record(long id, long behaviorId, long timestamp, double value) {
        Record record = new Record();
        record.setId(id);
        record.setBehaviorId(behaviorId);
        record.setTimestamp(timestamp);
        record.setValue(value);
        return record;
    }
}
