package com.github.timeaissr.behaviortracker.export;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;

import org.junit.Test;

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
}
