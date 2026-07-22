package com.duyanhnguyen.myapplication.model;

public class HistoryItem {

    private final String expression;
    private final String result;
    private final long timestamp;

    public HistoryItem(String expression, String result, long timestamp) {
        this.expression = expression;
        this.result = result;
        this.timestamp = timestamp;
    }

    public String getExpression() {
        return expression;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getResult() {
        return result;
    }

}
