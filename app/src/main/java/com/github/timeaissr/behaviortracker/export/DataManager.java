package com.github.timeaissr.behaviortracker.export;

import android.content.Context;
import android.net.Uri;

import com.github.timeaissr.behaviortracker.data.AppDatabase;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Handles JSON-based data export and import using Storage Access Framework (SAF).
 * No sensitive storage permissions required.
 */
public class DataManager {

    private final Context context;
    private final AppDatabase db;
    private final Gson gson;

    public DataManager(Context context) {
        this.context = context.getApplicationContext();
        this.db = AppDatabase.getInstance(context);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Export all data to a JSON file via SAF Uri.
     * Must be called on a background thread.
     */
    public boolean exportData(Uri uri) {
        try {
            ExportData exportData = new ExportData();

            // Gather all data synchronously
            List<Behavior> behaviors = db.behaviorDao().getAllSync();
            List<Record> records = db.recordDao().getAllSync();

            exportData.setBehaviors(behaviors);
            exportData.setRecords(records);

            // Write to output stream
            String json = gson.toJson(exportData);
            try (OutputStream os = context.getContentResolver().openOutputStream(uri);
                 OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                writer.write(json);
                writer.flush();
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Import data from a JSON file via SAF Uri.
     * This replaces all existing data.
     * Must be called on a background thread.
     */
    public boolean importData(Uri uri) {
        try {
            // Read JSON
            String json;
            try (InputStream is = context.getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                json = sb.toString();
            }

            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject()
                    || !BackupValidator.hasRequiredJsonFields(root.getAsJsonObject())) {
                return false;
            }

            ExportData importedData = gson.fromJson(root, ExportData.class);
            if (!BackupValidator.isValid(importedData)) {
                return false;
            }

            // Clear existing data and insert imported data within a transaction
            db.runInTransaction(() -> {
                // Clear tables
                db.recordDao().deleteAll();
                // Delete all behaviors (records have already been cleared).
                List<Behavior> existing = db.behaviorDao().getAllSync();
                for (Behavior b : existing) {
                    db.behaviorDao().delete(b);
                }

                // Insert behaviors
                if (importedData.getBehaviors() != null) {
                    for (Behavior b : importedData.getBehaviors()) {
                        db.behaviorDao().insert(b);
                    }
                }

                // Insert records
                if (importedData.getRecords() != null && !importedData.getRecords().isEmpty()) {
                    db.recordDao().insertAll(importedData.getRecords());
                }
            });

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
