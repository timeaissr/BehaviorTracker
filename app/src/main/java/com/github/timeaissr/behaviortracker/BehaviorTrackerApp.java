package com.github.timeaissr.behaviortracker;

import android.app.Application;
import android.app.NotificationManager;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.color.DynamicColors;
import com.google.android.material.color.DynamicColorsOptions;

public class BehaviorTrackerApp extends Application {

    private static final String LEGACY_NOTIFICATION_CHANNEL_ID =
            "behavior_reminder_channel";

    public static final String PREFERENCES_NAME = "behavior_tracker_preferences";
    public static final String KEY_THEME_MODE = "theme_mode";
    public static final String KEY_DYNAMIC_COLORS = "dynamic_colors";

    @Override
    public void onCreate() {
        super.onCreate();

        int themeMode = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getInt(
                KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.deleteNotificationChannel(LEGACY_NOTIFICATION_CHANNEL_ID);

        DynamicColorsOptions options = new DynamicColorsOptions.Builder()
                .setPrecondition((activity, theme) -> getSharedPreferences(
                        PREFERENCES_NAME, MODE_PRIVATE).getBoolean(KEY_DYNAMIC_COLORS, true))
                .build();
        DynamicColors.applyToActivitiesIfAvailable(this, options);
    }
}
