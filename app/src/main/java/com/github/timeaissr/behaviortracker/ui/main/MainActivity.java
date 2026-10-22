package com.github.timeaissr.behaviortracker.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.databinding.ActivityMainBinding;
import com.github.timeaissr.behaviortracker.ui.add.AddBehaviorActivity;
import com.github.timeaissr.behaviortracker.ui.detail.BehaviorDetailActivity;
import com.github.timeaissr.behaviortracker.ui.record.RecordDateTimeInput;
import com.github.timeaissr.behaviortracker.ui.settings.SettingsActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

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
        
        RecordDateTimeInput dateTimeInput = new RecordDateTimeInput(
                this, dialogView, behavior.isDetailedTime());

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(behavior.getName())
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    if (!dateTimeInput.validate()) return;
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    viewModel.quickLogBoolean(behavior.getId(), dateTimeInput.getTimestamp(), success ->
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
                                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                }
                            }));
                }));
        dialog.show();
    }

    private void showNumericInputDialog(Behavior behavior) {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_numeric_input, null);
        EditText editValue = dialogView.findViewById(R.id.edit_value);

        String unit = behavior.getUnit() != null ? " (" + behavior.getUnit() + ")" : "";
        RecordDateTimeInput dateTimeInput = new RecordDateTimeInput(
                this, dialogView, behavior.isDetailedTime());

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
                    if (!dateTimeInput.validate()) return;
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    viewModel.quickLogNumeric(behavior.getId(), value,
                            dateTimeInput.getTimestamp(), success ->
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

    private boolean canHandleAsyncResult() {
        return !isFinishing() && !isDestroyed();
    }
}
