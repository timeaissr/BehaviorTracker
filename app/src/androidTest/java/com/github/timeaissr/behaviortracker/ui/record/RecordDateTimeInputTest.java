package com.github.timeaissr.behaviortracker.ui.record;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.ui.main.MainActivity;
import com.github.timeaissr.behaviortracker.util.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class RecordDateTimeInputTest {

    private ActivityScenario<MainActivity> scenario;
    private AlertDialog dialog;
    private View content;
    private RecordDateTimeInput input;

    @After
    public void tearDown() {
        if (scenario != null) {
            scenario.onActivity(activity -> dialog.dismiss());
            scenario.close();
        }
    }

    @Test
    public void openingEitherRecordLayoutShowsCurrentTimeWithoutClearingIt() {
        for (int layout : new int[]{R.layout.dialog_boolean_input, R.layout.dialog_numeric_input}) {
            showDialog(layout, true);
            scenario.onActivity(activity -> {
                assertTrue(Math.abs(System.currentTimeMillis() - input.getTimestamp()) < 60_000L);
                var time = Instant.ofEpochMilli(input.getTimestamp()).atZone(ZoneId.systemDefault());
                assertEquals(String.format(Locale.getDefault(), "%02d", time.getHour()), text(R.id.edit_hour));
                assertEquals(String.format(Locale.getDefault(), "%02d", time.getMinute()), text(R.id.edit_minute));
                assertFalse(content.findViewById(R.id.edit_hour).hasFocus());
                assertFalse(content.findViewById(R.id.edit_minute).hasFocus());
            });
            tearDown();
            scenario = null;
        }
    }

    @Test
    public void tappingTimeClearsTheFieldAndTypingReplacesIt() {
        showDialog(R.layout.dialog_boolean_input, true);
        onView(withId(R.id.edit_hour)).perform(click()).check(matches(withText("")));
        onView(withId(R.id.edit_hour)).perform(typeText("7"));
        onView(withId(R.id.edit_minute)).perform(click()).check(matches(withText("")));
        onView(withId(R.id.edit_minute)).perform(typeText("08"), closeSoftKeyboard());
        scenario.onActivity(activity -> {
            assertTrue(input.validate());
            assertEquals("07:08", DateUtils.formatTime(input.getTimestamp()));
        });
    }

    @Test
    public void leavingClearedFieldsEmptyKeepsTheOriginalTime() {
        showDialog(R.layout.dialog_boolean_input, true);
        long[] original = new long[1];
        scenario.onActivity(activity -> original[0] = input.getTimestamp());
        onView(withId(R.id.edit_hour)).perform(click()).check(matches(withText("")));
        onView(withId(R.id.edit_minute)).perform(click()).check(matches(withText("")));
        scenario.onActivity(activity -> {
            assertTrue(input.validate());
            assertEquals(original[0], input.getTimestamp());
        });
    }

    @Test
    public void clearingAnEditedFieldRestoresItsMostRecentValue() {
        showDialog(R.layout.dialog_boolean_input, true);
        onView(withId(R.id.edit_hour)).perform(click(), typeText("7"));
        onView(withId(R.id.edit_minute)).perform(click(), typeText("08"));
        onView(withId(R.id.edit_hour)).perform(click()).check(matches(withText("")));
        scenario.onActivity(activity -> {
            assertTrue(input.validate());
            assertEquals("07:08", DateUtils.formatTime(input.getTimestamp()));
        });
    }

    @Test
    public void minuteButtonCommitsTypedTimeAndUpdatesDateAcrossMidnight() {
        showDialog(R.layout.dialog_boolean_input, true);
        scenario.onActivity(activity -> {
            ((EditText) content.findViewById(R.id.edit_hour)).setText("23");
            ((EditText) content.findViewById(R.id.edit_minute)).setText("59");
            long expected = DateUtils.addMinutes(DateUtils.withTime(input.getTimestamp(), 23, 59), 1);
            content.findViewById(R.id.btn_minute_plus).performClick();
            assertEquals(expected, input.getTimestamp());
            assertTrue(((MaterialButton) content.findViewById(R.id.btn_pick_date)).getText()
                    .toString().contains(DateUtils.formatDate(expected)));
            content.findViewById(R.id.btn_minute_minus).performClick();
            assertEquals(DateUtils.addMinutes(expected, -1), input.getTimestamp());
            content.findViewById(R.id.btn_hour_minus).performClick();
            assertEquals(DateUtils.addMinutes(expected, -61), input.getTimestamp());
            content.findViewById(R.id.btn_hour_plus).performClick();
            assertEquals(DateUtils.addMinutes(expected, -1), input.getTimestamp());
        });
    }

    @Test
    public void invalidTimeBlocksSavingAndAdjustmentUntilCorrected() {
        showDialog(R.layout.dialog_numeric_input, true);
        scenario.onActivity(activity -> {
            long original = input.getTimestamp();
            EditText hour = content.findViewById(R.id.edit_hour);
            EditText minute = content.findViewById(R.id.edit_minute);
            hour.setText("24");
            assertFalse(input.validate());
            content.findViewById(R.id.btn_hour_plus).performClick();
            assertEquals(original, input.getTimestamp());
            hour.setText("00");
            minute.setText("60");
            assertFalse(input.validate());
            minute.setText("00");
            assertTrue(input.validate());
            assertEquals(DateUtils.withTime(original, 0, 0), input.getTimestamp());
        });
    }

    @Test
    public void dateOnlyRecordHidesTimeAndUsesLocalStartOfDay() {
        showDialog(R.layout.dialog_boolean_input, false);
        scenario.onActivity(activity -> {
            assertEquals(View.GONE, content.findViewById(R.id.layout_time).getVisibility());
            assertTrue(input.validate());
            assertEquals(DateUtils.getStartOfDay(input.getTimestamp()), input.getTimestamp());
        });
    }

    private void showDialog(int layout, boolean detailedTime) {
        scenario = ActivityScenario.launch(MainActivity.class);
        scenario.onActivity(activity -> {
            content = activity.getLayoutInflater().inflate(layout, null);
            input = new RecordDateTimeInput(activity, content, detailedTime);
            dialog = new MaterialAlertDialogBuilder(activity)
                    .setTitle("日期时间输入测试")
                    .setView(content)
                    .setPositiveButton(R.string.confirm, null)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    private String text(int id) {
        return ((EditText) content.findViewById(id)).getText().toString();
    }
}
