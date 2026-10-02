package com.github.timeaissr.behaviortracker.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.github.timeaissr.behaviortracker.R;
import com.github.timeaissr.behaviortracker.data.entity.Record;
import com.github.timeaissr.behaviortracker.data.entity.RecordType;
import com.github.timeaissr.behaviortracker.util.DateUtils;

public class RecordAdapter extends ListAdapter<Record, RecordAdapter.ViewHolder> {

    private final RecordType recordType;
    private final boolean detailedTime;
    private final String unit;
    private OnRecordLongClickListener longClickListener;

    public interface OnRecordLongClickListener {
        void onRecordLongClick(Record record);
    }

    public RecordAdapter(RecordType recordType, boolean detailedTime, String unit,
                         OnRecordLongClickListener listener) {
        super(DIFF_CALLBACK);
        this.recordType = recordType;
        this.detailedTime = detailedTime;
        this.unit = unit;
        this.longClickListener = listener;
    }

    private static final DiffUtil.ItemCallback<Record> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Record>() {
                @Override
                public boolean areItemsTheSame(@NonNull Record oldItem, @NonNull Record newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Record oldItem, @NonNull Record newItem) {
                    return oldItem.getTimestamp() == newItem.getTimestamp()
                            && oldItem.getValue() == newItem.getValue();
                }
            };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Record record = getItem(position);
        holder.bind(record);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView textDate;
        private final TextView textValue;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_record_date);
            textValue = itemView.findViewById(R.id.text_record_value);
        }

        void bind(Record record) {
            textDate.setText(detailedTime ? DateUtils.formatDateTime(record.getTimestamp())
                    : DateUtils.formatDate(record.getTimestamp()));

            if (recordType == RecordType.BOOLEAN) {
                textValue.setText("✓");
            } else {
                String unitStr = unit != null ? " " + unit : "";
                textValue.setText(String.format("%.1f%s", record.getValue(), unitStr));
            }

            // Long press to delete
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    longClickListener.onRecordLongClick(record);
                }
                return true;
            });
        }
    }
}
