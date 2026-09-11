package com.github.timeaissr.behaviortracker.ui.detail;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.github.timeaissr.behaviortracker.databinding.ActivityBehaviorDetailBinding;
import com.github.timeaissr.behaviortracker.ui.add.AddBehaviorActivity;
import com.github.timeaissr.behaviortracker.util.DateUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class BehaviorDetailActivity extends AppCompatActivity {

    public static final String EXTRA_BEHAVIOR_ID = "extra_behavior_id";

    private ActivityBehaviorDetailBinding binding;
    private DetailViewModel viewModel;
    private RecordAdapter recordAdapter;
    private LiveData<List<Record>> recordsSource;
    private LiveData<List<Record>> chartSource;
    private LiveData<DetailViewModel.StatsData> statsSource;

    private long behaviorId;
    private Behavior currentBehavior;
    private int selectedDays = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBehaviorDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        behaviorId = getIntent().getLongExtra(EXTRA_BEHAVIOR_ID, -1);
        if (behaviorId < 0) {
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);

        setupToolbar();
        setupChipGroup();
        setupFab();
        observeBehavior();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                Intent intent = new Intent(this, AddBehaviorActivity.class);
                intent.putExtra(AddBehaviorActivity.EXTRA_BEHAVIOR_ID, behaviorId);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void setupChipGroup() {
        binding.chipGroupRange.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_7_days)) {
                selectedDays = 7;
            } else if (checkedIds.contains(R.id.chip_30_days)) {
                selectedDays = 30;
            } else if (checkedIds.contains(R.id.chip_90_days)) {
                selectedDays = 90;
            }
            loadChartData();
        });
    }

    private void setupFab() {
        binding.fabAddRecord.setOnClickListener(v -> {
            if (currentBehavior != null) {
                showAddRecordDialog();
            }
        });
    }

    private void showAddRecordDialog() {
        if (currentBehavior == null) return;

        boolean isBoolean = currentBehavior.getRecordType() == RecordType.BOOLEAN;
        
        if (isBoolean) {
            showBooleanRecordDialog();
        } else {
            showNumericRecordDialog();
        }
    }

    private void showBooleanRecordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_boolean_input, null);
        
        com.google.android.material.button.MaterialButton btnPickDatetime = 
                dialogView.findViewById(R.id.btn_pick_datetime);
        final long[] selectedTimestamp = {System.currentTimeMillis()};
        
        btnPickDatetime.setOnClickListener(v -> showDateTimePicker(selectedTimestamp, btnPickDatetime));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(currentBehavior.getName())
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> viewModel.insertBooleanRecord(
                        behaviorId, selectedTimestamp[0], inserted -> runOnUiThread(() -> {
                            com.google.android.material.snackbar.Snackbar.make(binding.getRoot(),
                                    inserted
                                            ? currentBehavior.getName() + " - "
                                                    + getString(R.string.record_added)
                                            : getString(R.string.record_exists_for_day),
                                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                            if (inserted) {
                                dialog.dismiss();
                            }
                        }))));
        dialog.show();
    }

    private void showNumericRecordDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_numeric_input, null);
        
        android.widget.EditText editValue = dialogView.findViewById(R.id.edit_value);
        android.widget.EditText editNote = dialogView.findViewById(R.id.edit_note);
        com.google.android.material.button.MaterialButton btnPickDatetime = 
                dialogView.findViewById(R.id.btn_pick_datetime);

        String unit = currentBehavior.getUnit() != null ? " (" + currentBehavior.getUnit() + ")" : "";
        final long[] selectedTimestamp = {System.currentTimeMillis()};
        
        btnPickDatetime.setOnClickListener(v -> showDateTimePicker(selectedTimestamp, btnPickDatetime));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(currentBehavior.getName() + unit)
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
                    viewModel.insertNumericRecord(behaviorId, value,
                            note.isEmpty() ? null : note, selectedTimestamp[0]);
                    com.google.android.material.snackbar.Snackbar.make(binding.getRoot(),
                            currentBehavior.getName() + " - " + getString(R.string.record_added),
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showDateTimePicker(final long[] timestampHolder, 
            com.google.android.material.button.MaterialButton button) {
        // Show date picker first
        com.google.android.material.datepicker.MaterialDatePicker<Long> datePicker = 
                com.google.android.material.datepicker.MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择日期")
                .setSelection(timestampHolder[0])
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // After date is selected, show time picker
            java.util.Calendar selectedDateUtc = java.util.Calendar.getInstance(
                    TimeZone.getTimeZone("UTC"));
            selectedDateUtc.setTimeInMillis(selection);
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            calendar.setTimeInMillis(timestampHolder[0]);
            calendar.set(java.util.Calendar.YEAR, selectedDateUtc.get(java.util.Calendar.YEAR));
            calendar.set(java.util.Calendar.MONTH, selectedDateUtc.get(java.util.Calendar.MONTH));
            calendar.set(java.util.Calendar.DAY_OF_MONTH,
                    selectedDateUtc.get(java.util.Calendar.DAY_OF_MONTH));
            
            com.google.android.material.timepicker.MaterialTimePicker timePicker = 
                    new com.google.android.material.timepicker.MaterialTimePicker.Builder()
                    .setTimeFormat(com.google.android.material.timepicker.TimeFormat.CLOCK_24H)
                    .setHour(calendar.get(java.util.Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(java.util.Calendar.MINUTE))
                    .setTitleText("选择时间")
                    .build();

            timePicker.addOnPositiveButtonClickListener(v -> {
                calendar.set(java.util.Calendar.HOUR_OF_DAY, timePicker.getHour());
                calendar.set(java.util.Calendar.MINUTE, timePicker.getMinute());
                calendar.set(java.util.Calendar.SECOND, 0);
                calendar.set(java.util.Calendar.MILLISECOND, 0);
                
                timestampHolder[0] = calendar.getTimeInMillis();
                
                // Update button text
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", 
                        java.util.Locale.getDefault());
                button.setText(sdf.format(new java.util.Date(timestampHolder[0])));
            });

            timePicker.show(getSupportFragmentManager(), "time_picker");
        });

        datePicker.show(getSupportFragmentManager(), "date_picker");
    }

    private void observeBehavior() {
        viewModel.getBehavior(behaviorId).observe(this, behavior -> {
            if (behavior == null) {
                finish();
                return;
            }
            currentBehavior = behavior;
            binding.toolbar.setTitle(behavior.getName());

            setupRecyclerView(behavior);
            loadStats(behavior);
            loadChartData();
            observeRecords();
        });
    }

    private void setupRecyclerView(Behavior behavior) {
        recordAdapter = new RecordAdapter(behavior.getRecordType(), behavior.getUnit(),
                record -> showDeleteRecordDialog(record));
        binding.recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerHistory.setAdapter(recordAdapter);
        if (recordsSource != null && recordsSource.getValue() != null) {
            recordAdapter.submitList(recordsSource.getValue());
        }
    }

    private void observeRecords() {
        if (recordsSource != null) return;
        recordsSource = viewModel.getRecords(behaviorId);
        recordsSource.observe(this, records -> {
            if (recordAdapter != null) {
                recordAdapter.submitList(records);
            }
        });
    }

    private void loadStats(Behavior behavior) {
        if (statsSource != null) return;
        boolean isBoolean = behavior.getRecordType() == RecordType.BOOLEAN;
        statsSource = viewModel.calculateStats(behaviorId, isBoolean);
        statsSource.observe(this, stats -> {
            if (isBoolean) {
                binding.textStatValue1.setText(String.valueOf(stats.currentStreak));
                binding.textStatLabel1.setText(getString(R.string.current_streak));
                binding.textStatValue2.setText(String.valueOf(stats.totalCount));
                binding.textStatLabel2.setText(getString(R.string.total_records));
                binding.textStatValue3.setText(String.valueOf(stats.longestStreak));
                binding.textStatLabel3.setText(getString(R.string.longest_streak));
            } else {
                binding.textStatValue1.setText(String.format("%.1f", stats.dailyAverage));
                binding.textStatLabel1.setText(getString(R.string.daily_average));
                binding.textStatValue2.setText(String.valueOf(stats.totalCount));
                binding.textStatLabel2.setText(getString(R.string.total_records));
                binding.textStatValue3.setText(String.format("%.0f", stats.totalSum));
                binding.textStatLabel3.setText(getString(R.string.total_value));
            }
        });
    }

    private void loadChartData() {
        if (currentBehavior == null) return;

        long endTime = DateUtils.getEndOfDay();
        long startTime = DateUtils.getStartOfDaysAgo(selectedDays - 1);

        if (chartSource != null) {
            chartSource.removeObservers(this);
        }
        chartSource = viewModel.getRecordsInRange(behaviorId, startTime, endTime);
        final int daysForSource = selectedDays;
        chartSource.observe(this, records -> {
            if (records == null) return;
            binding.chartContainer.removeAllViews();

            if (currentBehavior.getRecordType() == RecordType.BOOLEAN) {
                createBarChart(records, daysForSource);
            } else {
                createLineChart(records, daysForSource);
            }
        });
    }

    private void createBarChart(List<Record> records, int days) {
        BarChart chart = new BarChart(this);
        chart.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        // Group records by day
        Map<String, Integer> dailyCounts = new HashMap<>();
        for (Record record : records) {
            String day = DateUtils.formatDate(record.getTimestamp());
            dailyCounts.put(day, dailyCounts.getOrDefault(day, 0) + 1);
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            long dayTimestamp = DateUtils.getStartOfDaysAgo(i);
            String dayStr = DateUtils.formatDate(dayTimestamp);
            labels.add(dayStr.substring(5)); // MM-dd
            int count = dailyCounts.getOrDefault(dayStr, 0);
            entries.add(new BarEntry(days - 1 - i, count));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        String behaviorColor = currentBehavior.getColor();
        if (behaviorColor != null) {
            try {
                dataSet.setColor(Color.parseColor(behaviorColor));
            } catch (IllegalArgumentException ignored) {}
        }
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        chart.setData(barData);

        // Style
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setLabelCount(Math.min(labels.size(), 7));
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setGranularity(1f);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.setFitBars(true);
        chart.animateY(500);

        binding.chartContainer.addView(chart);
    }

    private void createLineChart(List<Record> records, int days) {
        LineChart chart = new LineChart(this);
        chart.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

        // Group records by day (sum values)
        Map<String, Double> dailySums = new HashMap<>();
        for (Record record : records) {
            String day = DateUtils.formatDate(record.getTimestamp());
            dailySums.put(day, dailySums.getOrDefault(day, 0.0) + record.getValue());
        }

        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            long dayTimestamp = DateUtils.getStartOfDaysAgo(i);
            String dayStr = DateUtils.formatDate(dayTimestamp);
            labels.add(dayStr.substring(5)); // MM-dd
            double sum = dailySums.getOrDefault(dayStr, 0.0);
            entries.add(new Entry(days - 1 - i, (float) sum));
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        String behaviorColor = currentBehavior.getColor();
        int chartColor = Color.parseColor("#006C4C");
        if (behaviorColor != null) {
            try {
                chartColor = Color.parseColor(behaviorColor);
            } catch (IllegalArgumentException ignored) {}
        }
        dataSet.setColor(chartColor);
        dataSet.setCircleColor(chartColor);
        dataSet.setCircleRadius(3f);
        dataSet.setLineWidth(2f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(chartColor);
        dataSet.setFillAlpha(30);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Style
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setGranularity(1f);
        chart.getXAxis().setLabelCount(Math.min(labels.size(), 7));
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.animateX(500);

        binding.chartContainer.addView(chart);
    }

    private void showDeleteRecordDialog(Record record) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete)
                .setMessage(R.string.delete_record_confirm)
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        viewModel.deleteRecord(record))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
