package com.github.timeaissr.behaviortracker.export;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates a backup completely before the current database is replaced. */
public final class BackupValidator {

    private BackupValidator() {}

    /**
     * Checks required fields before Gson can replace omitted primitive values with
     * constructor defaults.
     */
    public static boolean hasRequiredJsonFields(JsonObject root) {
        if (root == null || !hasValue(root, "version")
                || !root.has("behaviors") || !root.get("behaviors").isJsonArray()) {
            return false;
        }

        for (JsonElement element : root.getAsJsonArray("behaviors")) {
            if (!element.isJsonObject()) {
                return false;
            }
            JsonObject behavior = element.getAsJsonObject();
            if (!hasValue(behavior, "id") || !hasValue(behavior, "name")
                    || !hasValue(behavior, "recordType")
                    || !hasValue(behavior, "createdAt")) {
                return false;
            }
        }

        if (!root.has("records") || root.get("records").isJsonNull()) {
            return true;
        }
        if (!root.get("records").isJsonArray()) {
            return false;
        }
        for (JsonElement element : root.getAsJsonArray("records")) {
            if (!element.isJsonObject()) {
                return false;
            }
            JsonObject record = element.getAsJsonObject();
            if (!hasValue(record, "id") || !hasValue(record, "behaviorId")
                    || !hasValue(record, "timestamp") || !hasValue(record, "value")) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValue(JsonObject object, String field) {
        return object.has(field) && !object.get(field).isJsonNull();
    }

    public static boolean isValid(ExportData data) {
        if (data == null || data.getVersion() < 1 || data.getVersion() > 2
                || data.getBehaviors() == null) {
            return false;
        }

        Map<Long, RecordType> behaviorTypes = new HashMap<>();
        for (Behavior behavior : data.getBehaviors()) {
            if (behavior == null || behavior.getId() <= 0
                    || behavior.getName() == null || behavior.getName().trim().isEmpty()
                    || behavior.getRecordType() == null
                    || behavior.getCreatedAt() <= 0
                    || behaviorTypes.put(behavior.getId(), behavior.getRecordType()) != null) {
                return false;
            }
            if (behavior.getRecordType() == RecordType.NUMERIC
                    && (behavior.getUnit() == null || behavior.getUnit().trim().isEmpty())) {
                return false;
            }
        }

        List<Record> records = data.getRecords();
        if (records == null) {
            return true;
        }

        Set<Long> recordIds = new HashSet<>();
        for (Record record : records) {
            RecordType recordType = record == null
                    ? null : behaviorTypes.get(record.getBehaviorId());
            if (record == null || record.getId() <= 0
                    || !recordIds.add(record.getId())
                    || recordType == null
                    || !Double.isFinite(record.getValue())
                    || (recordType == RecordType.BOOLEAN
                            && Double.compare(record.getValue(), 1.0) != 0)) {
                return false;
            }
        }
        return true;
    }
}
