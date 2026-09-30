package com.github.timeaissr.behaviortracker.ui.main;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.data.entity.Behavior;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.github.timeaissr.behaviortracker.util.DateUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;
import java.util.Objects;

public class BehaviorAdapter extends ListAdapter<Behavior, BehaviorAdapter.ViewHolder> {

    public interface OnBehaviorClickListener {
        void onBehaviorClick(Behavior behavior);
        void onBooleanLogClick(Behavior behavior);
        void onNumericLogClick(Behavior behavior);
    }

    private final OnBehaviorClickListener listener;
    private final MainViewModel viewModel;
    private final LifecycleOwner lifecycleOwner;

    public BehaviorAdapter(OnBehaviorClickListener listener, MainViewModel viewModel,
                           LifecycleOwner lifecycleOwner) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        this.viewModel = viewModel;
        this.lifecycleOwner = lifecycleOwner;
    }

    private static final DiffUtil.ItemCallback<Behavior> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Behavior>() {
                @Override
                public boolean areItemsTheSame(@NonNull Behavior oldItem, @NonNull Behavior newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Behavior oldItem, @NonNull Behavior newItem) {
                    return oldItem.getName().equals(newItem.getName())
                            && oldItem.getRecordType() == newItem.getRecordType()
                            && oldItem.isDetailedTime() == newItem.isDetailedTime()
                            && Objects.equals(oldItem.getUnit(), newItem.getUnit())
                            && Objects.equals(oldItem.getColor(), newItem.getColor());
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_behavior, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Behavior behavior = getItem(position);
        holder.bind(behavior);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.clearObservers();
        super.onViewRecycled(holder);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;
        private final View colorIndicator;
        private final TextView textName;
        private final TextView textStatus;
        private final MaterialButton btnQuickLog;
        private LiveData<Integer> countSource;
        private Observer<Integer> countObserver;
        private LiveData<Double> sumSource;
        private Observer<Double> sumObserver;
        private long boundBehaviorId = -1;
        private int todayCount;
        private double todaySum;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_behavior);
            colorIndicator = itemView.findViewById(R.id.view_color_indicator);
            textName = itemView.findViewById(R.id.text_behavior_name);
            textStatus = itemView.findViewById(R.id.text_behavior_status);
            btnQuickLog = itemView.findViewById(R.id.btn_quick_log);
        }

        void bind(Behavior behavior) {
            clearObservers();
            boundBehaviorId = behavior.getId();
            todayCount = 0;
            todaySum = 0;
            textName.setText(behavior.getName());
            textStatus.setText(itemView.getContext().getString(R.string.not_logged_today));
            updateQuickLogButton(behavior);

            // Set color indicator
            colorIndicator.setBackgroundColor(Color.TRANSPARENT);
            if (behavior.getColor() != null && !behavior.getColor().isEmpty()) {
                try {
                    colorIndicator.setBackgroundColor(Color.parseColor(behavior.getColor()));
                } catch (IllegalArgumentException e) {
                    // fallback
                }
            }

            // Both types show today's record count on the quick-log button.
            long now = System.currentTimeMillis();
            long dayStart = DateUtils.getStartOfDay(now);
            long dayEnd = DateUtils.getEndOfDay(now);
            countSource = viewModel.getRecordCountForDay(
                    behavior.getId(), dayStart, dayEnd);
            countObserver = count -> {
                if (boundBehaviorId == behavior.getId()) {
                    todayCount = count == null ? 0 : count;
                    updateQuickLogButton(behavior);
                    if (behavior.getRecordType() == RecordType.BOOLEAN) {
                        textStatus.setText(todayCount > 0
                                ? itemView.getContext().getString(R.string.logged_today)
                                : itemView.getContext().getString(R.string.not_logged_today));
                    } else {
                        updateNumericStatus(behavior);
                    }
                }
            };
            countSource.observe(lifecycleOwner, countObserver);

            if (behavior.getRecordType() == RecordType.NUMERIC) {
                // Numeric types also show today's sum below the behavior name.
                sumSource = viewModel.getSumInRange(behavior.getId(), dayStart, dayEnd);
                sumObserver = sum -> {
                    if (boundBehaviorId == behavior.getId()) {
                        todaySum = sum == null ? 0 : sum;
                        updateNumericStatus(behavior);
                    }
                };
                sumSource.observe(lifecycleOwner, sumObserver);
            }

            btnQuickLog.setOnClickListener(v -> {
                if (listener != null) {
                    if (behavior.getRecordType() == RecordType.BOOLEAN) {
                        listener.onBooleanLogClick(behavior);
                    } else {
                        listener.onNumericLogClick(behavior);
                    }
                }
            });

            // Card click -> detail
            card.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBehaviorClick(behavior);
                }
            });
        }

        private void updateQuickLogButton(Behavior behavior) {
            btnQuickLog.setText(todayCount > 0
                    ? Integer.toString(todayCount)
                    : itemView.getContext().getString(R.string.today_no_records_symbol));
            btnQuickLog.setContentDescription(itemView.getContext().getString(
                    R.string.quick_log_today_count, behavior.getName(), todayCount));
        }

        private void updateNumericStatus(Behavior behavior) {
            if (todayCount > 0) {
                String unit = behavior.getUnit() != null ? behavior.getUnit() : "";
                textStatus.setText(String.format(Locale.getDefault(),
                        itemView.getContext().getString(R.string.today_value), todaySum, unit));
            } else {
                textStatus.setText(itemView.getContext().getString(R.string.not_logged_today));
            }
        }

        void clearObservers() {
            if (countSource != null && countObserver != null) {
                countSource.removeObserver(countObserver);
            }
            if (sumSource != null && sumObserver != null) {
                sumSource.removeObserver(sumObserver);
            }
            countSource = null;
            countObserver = null;
            sumSource = null;
            sumObserver = null;
            boundBehaviorId = -1;
        }
    }
}
