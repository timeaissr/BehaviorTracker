package com.github.timeaissr.behaviortracker.ui.add;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.github.timeaissr.behaviortracker.databinding.ActivityAddBehaviorBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class AddBehaviorActivity extends AppCompatActivity {

    public static final String EXTRA_BEHAVIOR_ID = "extra_behavior_id";

    private ActivityAddBehaviorBinding binding;
    private AddBehaviorViewModel viewModel;
    private ColorPickerAdapter colorAdapter;

    private String selectedColor;
    private RecordType selectedRecordType = RecordType.BOOLEAN;

    private static final String[] AVAILABLE_COLORS = {
            "#E57373", "#F06292", "#BA68C8", "#64B5F6",
            "#4DD0E1", "#4DB6AC", "#81C784", "#FFB74D"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddBehaviorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AddBehaviorViewModel.class);

        selectedColor = AVAILABLE_COLORS[0];

        setupToolbar();
        setupRecordTypeToggle();
        setupColorPicker();
        setupSaveButton();
        setupDeleteButton();

        // Check if editing
        long behaviorId = getIntent().getLongExtra(EXTRA_BEHAVIOR_ID, -1);
        if (behaviorId > 0) {
            viewModel.setEditingBehaviorId(behaviorId);
            binding.toolbar.setTitle(R.string.edit_behavior);
            binding.btnDelete.setVisibility(View.VISIBLE);
            binding.btnSave.setEnabled(false);
            // Existing records depend on their behavior type; changing it would reinterpret data.
            binding.btnTypeBoolean.setEnabled(false);
            binding.btnTypeNumeric.setEnabled(false);
            loadExistingBehavior(behaviorId);
        }

        // Observe save
        viewModel.getSaveComplete().observe(this, complete -> {
            if (complete != null && complete) {
                finish();
            } else if (Boolean.FALSE.equals(complete)) {
                binding.btnSave.setEnabled(true);
                Snackbar.make(binding.getRoot(), R.string.save_error,
                        Snackbar.LENGTH_SHORT).show();
            }
        });

        viewModel.getDeleteComplete().observe(this, complete -> {
            if (Boolean.TRUE.equals(complete)) {
                finish();
            } else if (Boolean.FALSE.equals(complete)) {
                binding.btnDelete.setEnabled(true);
                Snackbar.make(binding.getRoot(), R.string.delete_error,
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecordTypeToggle() {
        binding.toggleRecordType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_type_boolean) {
                    selectedRecordType = RecordType.BOOLEAN;
                    binding.layoutUnit.setVisibility(View.GONE);
                } else if (checkedId == R.id.btn_type_numeric) {
                    selectedRecordType = RecordType.NUMERIC;
                    binding.layoutUnit.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setupColorPicker() {
        colorAdapter = new ColorPickerAdapter(AVAILABLE_COLORS, color -> selectedColor = color);
        binding.recyclerColors.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerColors.setAdapter(colorAdapter);
    }

    private void setupSaveButton() {
        binding.btnSave.setOnClickListener(v -> {
            String name = binding.editName.getText().toString().trim();
            if (name.isEmpty()) {
                binding.layoutName.setError(getString(R.string.error_empty_name));
                return;
            }
            binding.layoutName.setError(null);

            if (selectedRecordType == RecordType.NUMERIC) {
                String unit = binding.editUnit.getText().toString().trim();
                if (unit.isEmpty()) {
                    binding.layoutUnit.setError(getString(R.string.error_empty_unit));
                    return;
                }
                binding.layoutUnit.setError(null);
            }

            // Build behavior
            Behavior behavior = new Behavior();
            behavior.setName(name);
            behavior.setRecordType(selectedRecordType);
            behavior.setColor(selectedColor);
            if (selectedRecordType == RecordType.NUMERIC) {
                behavior.setUnit(binding.editUnit.getText().toString().trim());
            }

            binding.btnSave.setEnabled(false);
            viewModel.saveBehavior(behavior);
        });
    }

    private void setupDeleteButton() {
        binding.btnDelete.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.delete)
                        .setMessage(R.string.delete_behavior_confirm)
                        .setPositiveButton(R.string.delete, (dialog, which) -> {
                            binding.btnDelete.setEnabled(false);
                            viewModel.deleteBehavior(viewModel.getEditingBehaviorId());
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show());
    }

    private void loadExistingBehavior(long behaviorId) {
        viewModel.getBehavior(behaviorId).observe(this, behavior -> {
            if (behavior == null) return;

            binding.editName.setText(behavior.getName());

            if (behavior.getRecordType() == RecordType.NUMERIC) {
                binding.toggleRecordType.check(R.id.btn_type_numeric);
                binding.editUnit.setText(behavior.getUnit());
            } else {
                binding.toggleRecordType.check(R.id.btn_type_boolean);
            }

            if (behavior.getColor() != null) {
                selectedColor = behavior.getColor();
                colorAdapter.setSelectedColor(selectedColor);
            }
            binding.btnSave.setEnabled(true);
        });

    }
}
