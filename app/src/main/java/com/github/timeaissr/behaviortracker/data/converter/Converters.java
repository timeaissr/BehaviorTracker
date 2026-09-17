package com.github.timeaissr.behaviortracker.data.converter;

import androidx.room.TypeConverter;

import com.github.timeaissr.behaviortracker.data.entity.RecordType;

/**
 * Room TypeConverters for enum types.
 */
public class Converters {

    @TypeConverter
    public static String fromRecordType(RecordType type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static RecordType toRecordType(String value) {
        return value == null ? null : RecordType.valueOf(value);
    }
}
