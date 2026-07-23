package com.duyanhnguyen.myapplication.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.duyanhnguyen.myapplication.R;
import com.duyanhnguyen.myapplication.data.HistoryManager;
import com.duyanhnguyen.myapplication.model.HistoryItem;
import com.duyanhnguyen.myapplication.ui.adapter.HistoryAdapter;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_EXPRESSION = "selected_expression";

    private HistoryManager historyManager;
    private RecyclerView recyclerView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyManager = new HistoryManager(this);
        recyclerView = findViewById(R.id.recycler_history);
        emptyView = findViewById(R.id.text_empty_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.button_clear_history).setOnClickListener(v -> {
            historyManager.clear();
            loadHistory();
            Toast.makeText(this, R.string.clear_history, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.button_back).setOnClickListener(v -> finish());

        loadHistory();
    }

    private void loadHistory() {
        List<HistoryItem> items = historyManager.getAll();
        HistoryAdapter adapter = new HistoryAdapter(items, item -> {
            Intent data = new Intent();
            data.putExtra(EXTRA_SELECTED_EXPRESSION, item.getExpression());
            setResult(RESULT_OK, data);
            finish();
        });
        recyclerView.setAdapter(adapter);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

}
