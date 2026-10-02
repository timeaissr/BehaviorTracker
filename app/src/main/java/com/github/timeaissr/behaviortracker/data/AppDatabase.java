package com.github.timeaissr.behaviortracker.data;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.github.timeaissr.behaviortracker.data.converter.Converters;
import com.github.timeaissr.behaviortracker.data.dao.BehaviorDao;
import com.github.timeaissr.behaviortracker.data.dao.RecordDao;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;

@Database(
    entities = {Behavior.class, Record.class},
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static final int LEGACY_FIXED_REQUEST_CODE_BASE = 10000;
    private static final int LEGACY_INTERVAL_REQUEST_CODE_BASE = 50000;
    private static final String LEGACY_REMINDER_RECEIVER =
            "com.github.timeaissr.behaviortracker.notification.ReminderReceiver";

    private static volatile AppDatabase INSTANCE;

    public abstract BehaviorDao behaviorDao();
    public abstract RecordDao recordDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "behavior_tracker.db"
                    ).addMigrations(createMigration1To2(context.getApplicationContext()),
                            MIGRATION_2_3, MIGRATION_3_4).build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE behaviors ADD COLUMN detailedTime INTEGER NOT NULL DEFAULT 1");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Rebuild the table so removing notes also works on older SQLite versions.
            database.execSQL("CREATE TABLE records_without_notes ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "behaviorId INTEGER NOT NULL, timestamp INTEGER NOT NULL, "
                    + "value REAL NOT NULL, FOREIGN KEY(behaviorId) REFERENCES behaviors(id) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("INSERT INTO records_without_notes (id, behaviorId, timestamp, value) "
                    + "SELECT id, behaviorId, timestamp, value FROM records");
            // Keep the ID sequence even if records with the largest IDs were deleted.
            database.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, "
                    + "COALESCE((SELECT seq FROM sqlite_sequence WHERE name = 'records'), 0)) "
                    + "WHERE name = 'records_without_notes'");
            database.execSQL("DROP TABLE records");
            database.execSQL("ALTER TABLE records_without_notes RENAME TO records");
            database.execSQL("CREATE INDEX index_records_behaviorId ON records (behaviorId)");
            database.execSQL("CREATE INDEX index_records_timestamp ON records (timestamp)");
        }
    };

    private static Migration createMigration1To2(Context context) {
        return new Migration(1, 2) {
            @Override
            public void migrate(SupportSQLiteDatabase database) {
                cancelLegacyReminders(context, database);
                database.execSQL("DROP TABLE IF EXISTS reminders");
                database.execSQL("ALTER TABLE behaviors DROP COLUMN iconName");
            }
        };
    }

    /** Cancels alarms created by v1 before their reminder rows are removed. */
    private static void cancelLegacyReminders(Context context,
                                               SupportSQLiteDatabase database) {
        AlarmManager alarmManager = context.getSystemService(AlarmManager.class);
        NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        try (Cursor cursor = database.query("SELECT id, behaviorId FROM reminders")) {
            int reminderIdColumn = cursor.getColumnIndexOrThrow("id");
            int behaviorIdColumn = cursor.getColumnIndexOrThrow("behaviorId");
            while (cursor.moveToNext()) {
                int reminderId = (int) cursor.getLong(reminderIdColumn);
                cancelLegacyAlarm(context, alarmManager,
                        LEGACY_FIXED_REQUEST_CODE_BASE + reminderId);
                cancelLegacyAlarm(context, alarmManager,
                        LEGACY_INTERVAL_REQUEST_CODE_BASE + reminderId);
                notificationManager.cancel((int) cursor.getLong(behaviorIdColumn));
            }
        }
    }

    private static void cancelLegacyAlarm(Context context, AlarmManager alarmManager,
                                          int requestCode) {
        Intent intent = new Intent().setComponent(
                new ComponentName(context.getPackageName(), LEGACY_REMINDER_RECEIVER));
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }
}
