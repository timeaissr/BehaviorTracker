package com.github.timeaissr.behaviortracker.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import java.util.Calendar;
import java.util.TimeZone;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.databinding.ActivityMainBinding;
import com.github.timeaissr.behaviortracker.ui.add.AddBehaviorActivity;
import com.github.timeaissr.behaviortracker.ui.detail.BehaviorDetailActivity;
import com.github.timeaissr.behaviortracker.ui.settings.SettingsActivity;
import com.github.timeaissr.behaviortracker.util.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class MainActivity extends AppCompatActivity implements BehaviorAdapter.OnBehaviorClickListener {

    private ActivityMainBinding binding;
    private MainViewModel viewModel;
    private BehaviorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupFab();
        observeData();
    }

    private void setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        adapter = new BehaviorAdapter(this, viewModel, this);
        binding.recyclerBehaviors.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerBehaviors.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabAdd.setOnClickListener(v ->
                startActivity(new Intent(this, AddBehaviorActivity.class)));
    }

    private void observeData() {
        viewModel.getAllActiveBehaviors().observe(this, behaviors -> {
            if (behaviors == null || behaviors.isEmpty()) {
                binding.layoutEmpty.setVisibility(View.VISIBLE);
                binding.recyclerBehaviors.setVisibility(View.GONE);
            } else {
                binding.layoutEmpty.setVisibility(View.GONE);
                binding.recyclerBehaviors.setVisibility(View.VISIBLE);
                adapter.submitList(behaviors);
            }
        });
    }

    // ==================== BehaviorAdapter.OnBehaviorClickListener ====================

    @Override
    public void onBehaviorClick(Behavior behavior) {
        Intent intent = new Intent(this, BehaviorDetailActivity.class);
        intent.putExtra(BehaviorDetailActivity.EXTRA_BEHAVIOR_ID, behavior.getId());
        startActivity(intent);
    }

    @Override
    public void onBooleanLogClick(Behavior behavior) {
        showBooleanLogDialog(behavior);
    }

    @Override
    public void onNumericLogClick(Behavior behavior) {
        showNumericInputDialog(behavior);
    }

    private void showBooleanLogDialog(Behavior behavior) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_boolean_input, null);
        
        MaterialButton btnPickDatetime = dialogView.findViewById(R.id.btn_pick_datetime);
        final long[] selectedTimestamp = {System.currentTimeMillis()};
        
        btnPickDatetime.setOnClickListener(v -> showDateTimePicker(selectedTimestamp, btnPickDatetime));

        new MaterialAlertDialogBuilder(this)
                .setTitle(behavior.getName())
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    viewModel.quickLogBoolean(behavior.getId(), selectedTimestamp[0], success ->
                            runOnUiThread(() -> {
                                if (!canHandleAsyncResult()) return;
                                Snackbar.make(binding.getRoot(),
                                        success
                                                ? behavior.getName() + " - "
                                                        + getString(R.string.record_added)
                                                : getString(R.string.save_error),
                                        Snackbar.LENGTH_SHORT).show();
                            }));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showNumericInputDialog(Behavior behavior) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_numeric_input, null);
        EditText editValue = dialogView.findViewById(R.id.edit_value);
        EditText editNote = dialogView.findViewById(R.id.edit_note);
        MaterialButton btnPickDatetime = dialogView.findViewById(R.id.btn_pick_datetime);

        String unit = behavior.getUnit() != null ? " (" + behavior.getUnit() + ")" : "";
        final long[] selectedTimestamp = {System.currentTimeMillis()};
        
        btnPickDatetime.setOnClickListener(v -> showDateTimePicker(selectedTimestamp, btnPickDatetime));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(behavior.getName() + unit)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String valueStr = editValue.getText().toString().trim();
                    double value;
                    try {
                        value = Double.parseDouble(valueStr);
                    } catch (NumberFormatException e) {
                        editValue.setError(getString(R.string.error_invalid_value));
                        return;
                    }
                    if (!Double.isFinite(value)) {
                        editValue.setError(getString(R.string.error_invalid_value));
                        return;
                    }
                    String note = editNote.getText().toString().trim();
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    viewModel.quickLogNumeric(behavior.getId(), value,
                            note.isEmpty() ? null : note, selectedTimestamp[0], success ->
                                    runOnUiThread(() -> {
                                        if (!canHandleAsyncResult()) return;
                                        Snackbar.make(binding.getRoot(),
                                                success
                                                        ? behavior.getName() + " - "
                                                                + getString(R.string.record_added)
                                                        : getString(R.string.save_error),
                                                Snackbar.LENGTH_SHORT).show();
                                        if (success) {
                                            dialog.dismiss();
                                        } else {
                                            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                                                    .setEnabled(true);
                                        }
                                    }));
                }));
        dialog.show();
    }

    private void showDateTimePicker(final long[] timestampHolder, MaterialButton button) {
        // Show date picker first
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择日期")
                .setSelection(DateUtils.toDatePickerSelection(timestampHolder[0]))
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // After date is selected, show time picker
            Calendar selectedDateUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            selectedDateUtc.setTimeInMillis(selection);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestampHolder[0]);
            calendar.set(Calendar.YEAR, selectedDateUtc.get(Calendar.YEAR));
            calendar.set(Calendar.MONTH, selectedDateUtc.get(Calendar.MONTH));
            calendar.set(Calendar.DAY_OF_MONTH, selectedDateUtc.get(Calendar.DAY_OF_MONTH));
            
            MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_24H)
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText("选择时间")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(Calendar.MINUTE, timePicker.getMinute());
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                
                timestampHolder[0] = calendar.getTimeInMillis();
                
                // Update button text
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                button.setText(sdf.format(new java.util.Date(timestampHolder[0])));
            });

            timePicker.show(getSupportFragmentManager(), "time_picker");
        });

        datePicker.show(getSupportFragmentManager(), "date_picker");
    }

    private boolean canHandleAsyncResult() {
        return !isFinishing() && !isDestroyed();
    }
}
