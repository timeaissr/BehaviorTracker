package com.github.timeaissr.behaviortracker.data;

import android.content.Context;

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
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS reminders");
            database.execSQL("ALTER TABLE behaviors DROP COLUMN archived");
            database.execSQL("ALTER TABLE behaviors DROP COLUMN iconName");
        }
    };

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
                    ).addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return INSTANCE;
    }
}
