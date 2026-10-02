package com.github.timeaissr.behaviortracker.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.github.timeaissr.behaviortracker.data.entity.Record;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class AppDatabaseMigrationTest {

    private static final String DATABASE_NAME = "notes-migration-test.db";
    private Context context;
    private AppDatabase database;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DATABASE_NAME);
    }

    @After
    public void tearDown() {
        if (database != null) database.close();
        context.deleteDatabase(DATABASE_NAME);
    }

    @Test
    public void freshDatabaseHasNoNotes() {
        openDatabase();

        assertRecordColumns(database.getOpenHelper().getWritableDatabase());
        assertTrue(database.recordDao().getAllSync().isEmpty());
    }

    @Test
    public void upgradeDiscardsNotesAndPreservesRecordsAndConstraints() {
        createVersionThreeDatabase();
        openDatabase();

        // Opening through Room also validates the migrated columns, indices and foreign key.
        SupportSQLiteDatabase sqlite = database.getOpenHelper().getWritableDatabase();
        assertRecordColumns(sqlite);
        assertEquals(3, database.recordDao().getAllSync().size());
        try (Cursor cursor = sqlite.query(
                "SELECT id, behaviorId, timestamp, value FROM records ORDER BY id")) {
            assertRecord(cursor, 7, 1, 1000, 250.5);
            assertRecord(cursor, 8, 2, 2000, 1.0);
            assertRecord(cursor, 9, 1, 1000, 125.0);
            assertFalse(cursor.moveToNext());
        }
        assertEquals("喝水", database.behaviorDao().getByIdSync(1).getName());
        assertFalse(database.behaviorDao().getByIdSync(1).isDetailedTime());
        assertTrue(database.behaviorDao().getByIdSync(2).isDetailedTime());
        assertTrue(database.behaviorDao().getByIdSync(2).isArchived());

        Record newRecord = new Record();
        newRecord.setBehaviorId(1);
        newRecord.setTimestamp(1000);
        newRecord.setValue(50.0);
        assertEquals(101, database.recordDao().insert(newRecord));

        database.behaviorDao().delete(database.behaviorDao().getByIdSync(1));
        assertEquals(1, database.recordDao().getAllSync().size());
        assertEquals(8, database.recordDao().getAllSync().get(0).getId());
        try (Cursor violations = sqlite.query("PRAGMA foreign_key_check")) {
            assertFalse(violations.moveToFirst());
        }
    }

    @Test
    public void upgradePreservesSequenceWhenAllRecordsWereDeleted() {
        createVersionThreeDatabase();
        try (SQLiteDatabase sqlite = context.openOrCreateDatabase(DATABASE_NAME, 0, null)) {
            sqlite.execSQL("DELETE FROM records");
        }
        openDatabase();

        assertTrue(database.recordDao().getAllSync().isEmpty());
        Record record = new Record();
        record.setBehaviorId(1);
        assertEquals(101, database.recordDao().insert(record));
    }

    private void openDatabase() {
        database = Room.databaseBuilder(context, AppDatabase.class, DATABASE_NAME)
                .addMigrations(AppDatabase.MIGRATION_3_4)
                .build();
    }

    private void createVersionThreeDatabase() {
        try (SQLiteDatabase sqlite = context.openOrCreateDatabase(DATABASE_NAME, 0, null)) {
            sqlite.execSQL("CREATE TABLE behaviors (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "name TEXT, recordType TEXT, detailedTime INTEGER NOT NULL DEFAULT 1, "
                    + "unit TEXT, color TEXT, createdAt INTEGER NOT NULL, archived INTEGER NOT NULL)");
            sqlite.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "behaviorId INTEGER NOT NULL, timestamp INTEGER NOT NULL, value REAL NOT NULL, "
                    + "note TEXT, FOREIGN KEY(behaviorId) REFERENCES behaviors(id) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            sqlite.execSQL("CREATE INDEX index_records_behaviorId ON records (behaviorId)");
            sqlite.execSQL("CREATE INDEX index_records_timestamp ON records (timestamp)");
            sqlite.execSQL("INSERT INTO behaviors VALUES "
                    + "(1, '喝水', 'NUMERIC', 0, 'ml', '#64B5F6', 0, 0), "
                    + "(2, '运动', 'BOOLEAN', 1, NULL, NULL, 0, 1)");
            sqlite.execSQL("INSERT INTO records VALUES "
                    + "(7, 1, 1000, 250.5, '旧备注'), (8, 2, 2000, 1.0, NULL), "
                    + "(9, 1, 1000, 125.0, ''), (100, 1, 3000, 10.0, '已删除记录')");
            sqlite.execSQL("DELETE FROM records WHERE id = 100");
            sqlite.setVersion(3);
        }
    }

    private static void assertRecordColumns(SupportSQLiteDatabase sqlite) {
        try (Cursor columns = sqlite.query("PRAGMA table_info(records)")) {
            assertEquals(4, columns.getCount());
            while (columns.moveToNext()) {
                assertFalse("note".equals(columns.getString(columns.getColumnIndexOrThrow("name"))));
            }
        }
    }

    private static void assertRecord(Cursor cursor, long id, long behaviorId,
                                     long timestamp, double value) {
        assertTrue(cursor.moveToNext());
        assertEquals(id, cursor.getLong(0));
        assertEquals(behaviorId, cursor.getLong(1));
        assertEquals(timestamp, cursor.getLong(2));
        assertEquals(value, cursor.getDouble(3), 0.0);
    }
}
