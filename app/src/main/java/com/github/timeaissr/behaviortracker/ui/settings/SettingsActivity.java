package com.github.timeaissr.behaviortracker.ui.settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.github.timeaissr.behaviortracker.BehaviorTrackerApp;
import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.databinding.ActivitySettingsBinding;
import com.github.timeaissr.behaviortracker.export.DataManager;
import com.google.android.material.snackbar.Snackbar;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private DataManager dataManager;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    // SAF launchers
    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performExport(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> importLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        performImport(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dataManager = new DataManager(this);

        setupToolbar();
        setupTheme();
        setupDataButtons();
        showVersion();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupTheme() {
        // Load current theme preference
        int currentMode = getSharedPreferences(BehaviorTrackerApp.PREFERENCES_NAME, MODE_PRIVATE)
                .getInt(BehaviorTrackerApp.KEY_THEME_MODE,
                        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        switch (currentMode) {
            case AppCompatDelegate.MODE_NIGHT_YES:
                binding.radioDark.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_NO:
                binding.radioLight.setChecked(true);
                break;
            default:
                binding.radioSystem.setChecked(true);
                break;
        }

        binding.radioTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int newMode;
            if (checkedId == R.id.radio_system) {
                newMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            } else if (checkedId == R.id.radio_light) {
                newMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radio_dark) {
                newMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                return;
            }
            getSharedPreferences(BehaviorTrackerApp.PREFERENCES_NAME, MODE_PRIVATE).edit()
                    .putInt(BehaviorTrackerApp.KEY_THEME_MODE, newMode)
                    .apply();
            AppCompatDelegate.setDefaultNightMode(newMode);
        });

        boolean dynamicColors = getSharedPreferences(
                BehaviorTrackerApp.PREFERENCES_NAME, MODE_PRIVATE)
                .getBoolean(BehaviorTrackerApp.KEY_DYNAMIC_COLORS, true);
        binding.switchDynamicColor.setChecked(dynamicColors);
        binding.switchDynamicColor.setOnCheckedChangeListener((button, enabled) -> {
            getSharedPreferences(BehaviorTrackerApp.PREFERENCES_NAME, MODE_PRIVATE).edit()
                    .putBoolean(BehaviorTrackerApp.KEY_DYNAMIC_COLORS, enabled)
                    .apply();
            recreate();
        });
    }

    private void setupDataButtons() {
        binding.btnExport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "behavior_tracker_backup.json");
            exportLauncher.launch(intent);
        });

        binding.btnImport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            importLauncher.launch(intent);
        });
    }

    private void performExport(Uri uri) {
        ioExecutor.execute(() -> {
            boolean success = dataManager.exportData(uri);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                Snackbar.make(binding.getRoot(),
                        success ? R.string.export_success : R.string.export_error,
                        Snackbar.LENGTH_SHORT).show();
            });
        });
    }

    private void performImport(Uri uri) {
        ioExecutor.execute(() -> {
            boolean success = dataManager.importData(uri);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (success) {
                    Snackbar.make(binding.getRoot(), R.string.import_success,
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.import_error,
                            Snackbar.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            binding.textVersion.setText(String.format(getString(R.string.version), pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            binding.textVersion.setText(String.format(getString(R.string.version), "1.0.0"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdown();
    }
}
