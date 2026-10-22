package com.github.timeaissr.behaviortracker.ui.record;

import android.content.Context;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.util.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

/** Shared date and time editor for all record entry points. */
public final class RecordDateTimeInput {

    private final AppCompatActivity activity;
    private final View root;
    private final boolean detailedTime;
    private final MaterialButton dateButton;
    private final EditText hourInput;
    private final EditText minuteInput;
    private final TextInputLayout hourLayout;
    private final TextInputLayout minuteLayout;
    private long timestamp;

    public RecordDateTimeInput(AppCompatActivity activity, View dialogView, boolean detailedTime) {
        this.activity = activity;
        this.detailedTime = detailedTime;
        root = dialogView.findViewById(R.id.layout_datetime);
        dateButton = dialogView.findViewById(R.id.btn_pick_date);
        hourInput = dialogView.findViewById(R.id.edit_hour);
        minuteInput = dialogView.findViewById(R.id.edit_minute);
        hourLayout = dialogView.findViewById(R.id.input_hour);
        minuteLayout = dialogView.findViewById(R.id.input_minute);

        long now = System.currentTimeMillis();
        timestamp = detailedTime ? now - Math.floorMod(now, 60_000L) : DateUtils.getStartOfDay(now);
        dialogView.findViewById(R.id.layout_time).setVisibility(detailedTime ? View.VISIBLE : View.GONE);
        prepareInput(hourInput, hourLayout, true);
        prepareInput(minuteInput, minuteLayout, false);
        dateButton.setOnClickListener(v -> showDatePicker());
        dialogView.findViewById(R.id.btn_hour_minus).setOnClickListener(v -> adjustTime(-60));
        dialogView.findViewById(R.id.btn_hour_plus).setOnClickListener(v -> adjustTime(60));
        dialogView.findViewById(R.id.btn_minute_minus).setOnClickListener(v -> adjustTime(-1));
        dialogView.findViewById(R.id.btn_minute_plus).setOnClickListener(v -> adjustTime(1));
        minuteInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) return false;
            validate();
            return true;
        });
        refresh();
        // Keep the current time visible until the user deliberately starts editing.
        root.requestFocus();
    }

    private void prepareInput(EditText input, TextInputLayout layout, boolean hour) {
        input.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                layout.setError(null);
                input.setText("");
            } else if (input.getText().toString().trim().isEmpty()) {
                // Tapping a field and leaving it empty keeps its previous value.
                input.setText(formatNumber(currentPart(hour)));
            }
        });
    }

    /** Validate before saving or adjusting; an unfinished edit never silently becomes midnight. */
    public boolean validate() {
        if (!detailedTime) return true;
        Integer hour = readPart(hourInput, hourLayout, true);
        Integer minute = readPart(minuteInput, minuteLayout, false);
        if (hour == null || minute == null) return false;
        timestamp = DateUtils.withTime(timestamp, hour, minute);
        root.requestFocus();
        InputMethodManager keyboard = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(root.getWindowToken(), 0);
        refresh();
        return true;
    }

    public long getTimestamp() {
        return timestamp;
    }

    private Integer readPart(EditText input, TextInputLayout layout, boolean hour) {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            layout.setError(null);
            return currentPart(hour);
        }
        try {
            int value = Integer.parseInt(text);
            if (value >= 0 && value <= (hour ? 23 : 59)) {
                layout.setError(null);
                return value;
            }
        } catch (NumberFormatException ignored) {
            // Show the same range error for pasted non-numeric input.
        }
        layout.setError(activity.getString(hour ? R.string.error_invalid_hour : R.string.error_invalid_minute));
        return null;
    }

    private void adjustTime(int minutes) {
        if (!validate()) return;
        timestamp = DateUtils.addMinutes(timestamp, minutes);
        refresh();
    }

    private void showDatePicker() {
        if (!validate()) return;
        if (activity.getSupportFragmentManager().findFragmentByTag("record_date_picker") != null) return;
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.pick_date)
                .setSelection(DateUtils.toDatePickerSelection(timestamp))
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            timestamp = DateUtils.withDatePickerSelection(timestamp, selection, detailedTime);
            refresh();
        });
        picker.show(activity.getSupportFragmentManager(), "record_date_picker");
    }

    private int currentPart(boolean hour) {
        var time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());
        return hour ? time.getHour() : time.getMinute();
    }

    private void refresh() {
        dateButton.setText(activity.getString(R.string.record_date_edit, DateUtils.formatDate(timestamp)));
        hourInput.setText(formatNumber(currentPart(true)));
        minuteInput.setText(formatNumber(currentPart(false)));
    }

    private String formatNumber(int value) {
        return String.format(Locale.getDefault(), "%02d", value);
    }
}
