package com.github.timeaissr.behaviortracker.export;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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
        data.setVersion(5);

        assertFalse(BackupValidator.isValid(data));
    }

    @Test
    public void requiresDetailedTimeInCurrentBackup() {
        String json = "{\"version\":4,\"behaviors\":[{\"id\":1,\"name\":\"运动\","
                + "\"recordType\":\"BOOLEAN\",\"createdAt\":0,\"archived\":false}],"
                + "\"records\":[]}";

        assertFalse(BackupValidator.hasRequiredJsonFields(
                JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    public void acceptsDetailedTimeInCurrentBackup() {
        String json = "{\"version\":4,\"behaviors\":[{\"id\":1,\"name\":\"运动\","
                + "\"recordType\":\"BOOLEAN\",\"detailedTime\":false,"
                + "\"createdAt\":0,\"archived\":false}],\"records\":[]}";

        assertTrue(BackupValidator.hasRequiredJsonFields(
                JsonParser.parseString(json).getAsJsonObject()));
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
                + "\"recordType\":\"BOOLEAN\",\"archived\":false}],\"records\":[]}";

        assertFalse(BackupValidator.hasRequiredJsonFields(
                JsonParser.parseString(json).getAsJsonObject()));
    }

    @Test
    public void rejectsJsonBehaviorWithoutArchivedState() {
        String json = "{\"version\":2,\"behaviors\":[{\"id\":1,\"name\":\"运动\","
                + "\"recordType\":\"BOOLEAN\",\"createdAt\":0}],\"records\":[]}";

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
    public void rejectsJsonWithoutRecordsArray() {
        String missing = "{\"version\":2,\"behaviors\":[]}";
        String nullRecords = "{\"version\":2,\"behaviors\":[],\"records\":null}";

        assertFalse(BackupValidator.hasRequiredJsonFields(
                JsonParser.parseString(missing).getAsJsonObject()));
        assertFalse(BackupValidator.hasRequiredJsonFields(
                JsonParser.parseString(nullRecords).getAsJsonObject()));
    }

    @Test
    public void acceptsRecordAtUnixEpoch() {
        ExportData data = validBackup();
        data.getRecords().get(0).setTimestamp(0);

        assertTrue(BackupValidator.isValid(data));
    }

    @Test
    public void acceptsBehaviorCreatedAtUnixEpoch() {
        ExportData data = validBackup();
        data.getBehaviors().get(0).setCreatedAt(0);

        assertTrue(BackupValidator.isValid(data));
    }

    @Test
    public void legacyBackupNotesAreDiscardedWithoutChangingRecords() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (int version = 1; version <= 3; version++) {
            JsonObject json = gson.toJsonTree(validBackup()).getAsJsonObject();
            json.addProperty("version", version);
            JsonObject originalRecord = json.getAsJsonArray("records").get(0).getAsJsonObject();
            originalRecord.addProperty("note", "旧备注");

            assertTrue(BackupValidator.hasRequiredJsonFields(json));
            ExportData imported = gson.fromJson(json, ExportData.class);
            assertTrue(BackupValidator.isValid(imported));
            Record record = imported.getRecords().get(0);
            assertEquals(originalRecord.get("id").getAsLong(), record.getId());
            assertEquals(originalRecord.get("behaviorId").getAsLong(), record.getBehaviorId());
            assertEquals(originalRecord.get("timestamp").getAsLong(), record.getTimestamp());
            assertEquals(originalRecord.get("value").getAsDouble(), record.getValue(), 0.0);
            assertFalse(gson.toJsonTree(record).getAsJsonObject().has("note"));
        }
    }

    @Test
    public void currentBackupRoundTripUsesVersionFourWithoutNotes() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject json = gson.toJsonTree(validBackup()).getAsJsonObject();

        assertEquals(4, json.get("version").getAsInt());
        assertTrue(BackupValidator.hasRequiredJsonFields(json));
        JsonObject record = json.getAsJsonArray("records").get(0).getAsJsonObject();
        assertEquals(4, record.size());
        assertFalse(record.has("note"));
        ExportData restored = gson.fromJson(json, ExportData.class);
        assertTrue(BackupValidator.isValid(restored));
        assertFalse(restored.getBehaviors().get(0).isDetailedTime());
        assertEquals(250.0, restored.getRecords().get(0).getValue(), 0.0);
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
