package com.duyanhnguyen.myapplication.ui;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.duyanhnguyen.myapplication.R;
import com.duyanhnguyen.myapplication.model.HistoryItem;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
    }

    private final List<HistoryItem> items;
    private final OnItemClickListener listener;

    public HistoryAdapter(List<HistoryItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        holder.expressionText.setText(item.getExpression());
        holder.resultText.setText("= " + item.getResult());
        holder.timeText.setText(DateFormat.format("HH:mm dd/MM/yyyy", item.getTimestamp()));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView expressionText;
        final TextView resultText;
        final TextView timeText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            expressionText = itemView.findViewById(R.id.text_expression_item);
            resultText = itemView.findViewById(R.id.text_result_item);
            timeText = itemView.findViewById(R.id.text_time_item);
        }
    }

}
