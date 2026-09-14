package com.github.timeaissr.behaviortracker.export;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Validates a backup completely before the current database is replaced. */
public final class BackupValidator {

    private BackupValidator() {}

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
                    || record.getTimestamp() <= 0
                    || !Double.isFinite(record.getValue())
                    || (recordType == RecordType.BOOLEAN
                            && Double.compare(record.getValue(), 1.0) != 0)) {
                return false;
            }
        }
        return true;
    }
}
