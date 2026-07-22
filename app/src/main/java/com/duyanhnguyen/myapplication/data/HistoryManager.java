package com.duyanhnguyen.myapplication.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.duyanhnguyen.myapplication.model.HistoryItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HistoryManager {

    private static final String PREFS_NAME = "calculator_history_prefs";
    private static final String KEY_HISTORY = "history_json";
    private static final int MAX_ENTRIES = 50;

    private final SharedPreferences prefs;

    public HistoryManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void addEntry(String expression, String result) {
        List<HistoryItem> items = getAll();
        items.add(0, new HistoryItem(expression, result, System.currentTimeMillis()));
        while (items.size() > MAX_ENTRIES) {
            items.remove(items.size() - 1);
        }
        save(items);
    }

    public List<HistoryItem> getAll() {
        List<HistoryItem> items = new ArrayList<>();
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return items;

        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); ++i) {
                JSONObject o = arr.getJSONObject(i);
                items.add(new HistoryItem(o.getString("expr"), o.getString("result"), o.getLong("time")));
            }
        } catch (Exception ignored) {
        }

        return items;
    }

    public void clear() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }

    private void save(List<HistoryItem> items) {
        JSONArray arr = new JSONArray();
        try {
            for (HistoryItem item : items) {
                JSONObject o = new JSONObject();
                o.put("expr", item.getExpression());
                o.put("result", item.getResult());
                o.put("time", item.getTimestamp());
                arr.put(o);
            }
            prefs.edit().putString(KEY_HISTORY, arr.toString()).apply();
        } catch (Exception ignored) {
        }
    }

}
