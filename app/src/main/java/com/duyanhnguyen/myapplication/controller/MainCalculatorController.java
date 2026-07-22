package com.duyanhnguyen.myapplication.controller;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.duyanhnguyen.myapplication.R;
import com.duyanhnguyen.myapplication.data.HistoryManager;
import com.duyanhnguyen.myapplication.engine.ExpressionEvaluator;
import com.duyanhnguyen.myapplication.engine.ExpressionValidator;
import com.duyanhnguyen.myapplication.ui.HistoryActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

public class MainCalculatorController {

    private static final String KEY_EXPRESSION = "key_expression";
    private static final String KEY_CURSOR_POS = "key_cursor_pos";
    private static final String KEY_RESULT_SHOWN = "key_result_shown";
    private static final String KEY_RESULT_TEXT = "key_result_text";
    private static final float EXPR_SIZE_TYPING = 36f;
    private static final float RESULT_SIZE_TYPING = 24f;
    private static final float EXPR_SIZE_RESULT = 22f;
    private static final float RESULT_SIZE_RESULT = 48f;
    private static final float EXPR_SIZE_TYPING_LAND = 28f;
    private static final float RESULT_SIZE_TYPING_LAND = 18f;
    private static final float EXPR_SIZE_RESULT_LAND = 16f;
    private static final float RESULT_SIZE_RESULT_LAND = 36f;

    private final AppCompatActivity activity;
    private EditText expressionDisplay;
    private TextView resultDisplay;
    private boolean isResultShown = false;
    private HistoryManager historyManager;
    private final Map<Integer, String> keyMap = new HashMap<>();
    private int colorNormal, colorPreview, colorError;
    private ActivityResultLauncher<Intent> historyLauncher;

    public MainCalculatorController(AppCompatActivity activity) {
        this.activity = activity;
    }

    public void onCreate(Bundle savedInstanceState) {
        activity.setContentView(R.layout.activity_main);

        historyManager = new HistoryManager(activity);

        colorNormal = ContextCompat.getColor(activity, R.color.colorTextPrimary);
        colorPreview = ContextCompat.getColor(activity, R.color.colorTextSecondary);
        colorError = ContextCompat.getColor(activity, R.color.colorError);

        expressionDisplay = activity.findViewById(R.id.text_expression);
        resultDisplay = activity.findViewById(R.id.text_result);

        setupKeyMap();
        setupHistoryLauncher();

        expressionDisplay.setShowSoftInputOnFocus(false);
        expressionDisplay.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (!isResultShown) {
                    refreshPreview();
                }
            }
        });

        View piButton = activity.findViewById(R.id.btn_pi);
        if (piButton != null) {
            piButton.setOnLongClickListener(v -> {
                insertAtCursor("e");
                return true;
            });
        }

        if (savedInstanceState != null) {
            String expr = savedInstanceState.getString(KEY_EXPRESSION, "");
            int cursorPos = savedInstanceState.getInt(KEY_CURSOR_POS, expr.length());
            isResultShown = savedInstanceState.getBoolean(KEY_RESULT_SHOWN, false);
            String resultText = savedInstanceState.getString(KEY_RESULT_TEXT, "0");

            expressionDisplay.setText(expr);
            expressionDisplay.setSelection(Math.min(cursorPos, expr.length()));
            resultDisplay.setText(resultText);
        }

        applyTextSizes();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_EXPRESSION, expressionDisplay.getText().toString());
        outState.putInt(KEY_CURSOR_POS, expressionDisplay.getSelectionStart());
        outState.putBoolean(KEY_RESULT_SHOWN, isResultShown);
        outState.putString(KEY_RESULT_TEXT, resultDisplay.getText().toString());
    }

    public void onButtonClick(View v) {
        int id = v.getId();

        if (id == R.id.btn_clear) {
            clearAll();
        } else if (id == R.id.btn_delete) {
            deleteLast();
        } else if (id == R.id.btn_equals) {
            calculateResult();
        } else if (id == R.id.btn_history) {
            openHistory();
        } else if (keyMap.containsKey(id)) {
            String value = keyMap.get(id);
            appendToExpression(value, isBinaryOperator(value));
        } else {
            String value = getLandscapeKeyValue(id);
            if (value != null) {
                appendToExpression(value, isBinaryOperator(value));
            }
        }
    }

    private void setupKeyMap() {
        keyMap.put(R.id.btn_0, "0");
        keyMap.put(R.id.btn_1, "1");
        keyMap.put(R.id.btn_2, "2");
        keyMap.put(R.id.btn_3, "3");
        keyMap.put(R.id.btn_4, "4");
        keyMap.put(R.id.btn_5, "5");
        keyMap.put(R.id.btn_6, "6");
        keyMap.put(R.id.btn_7, "7");
        keyMap.put(R.id.btn_8, "8");
        keyMap.put(R.id.btn_9, "9");
        keyMap.put(R.id.btn_dot, ".");
        keyMap.put(R.id.btn_plus, "+");
        keyMap.put(R.id.btn_minus, "-");
        keyMap.put(R.id.btn_multiply, "×");
        keyMap.put(R.id.btn_divide, "÷");
        keyMap.put(R.id.btn_percent, "%");
        keyMap.put(R.id.btn_open_paren, "(");
        keyMap.put(R.id.btn_close_paren, ")");
    }

    private void setupHistoryLauncher() {
        historyLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK && result.getData() != null) {
                        String selected = result.getData().getStringExtra(HistoryActivity.EXTRA_SELECTED_EXPRESSION);
                        if (selected != null) {
                            expressionDisplay.setText(selected);
                            expressionDisplay.setSelection(selected.length());
                            isResultShown = false;
                            applyTextSizes();
                            refreshPreview();
                        }
                    }
                });
    }

    private String getLandscapeKeyValue(int id) {
        if (id == R.id.btn_sin) return "sin(";
        if (id == R.id.btn_cos) return "cos(";
        if (id == R.id.btn_tan) return "tan(";
        if (id == R.id.btn_log) return "log(";
        if (id == R.id.btn_ln) return "ln(";
        if (id == R.id.btn_sqrt) return "√(";
        if (id == R.id.btn_power) return "^";
        if (id == R.id.btn_factorial) return "!";
        if (id == R.id.btn_pi) return "π";
        if (id == R.id.btn_e) return "e";
        if (id == R.id.btn_open_paren_land) return "(";
        if (id == R.id.btn_close_paren_land) return ")";
        return null;
    }

    private void openHistory() {
        historyLauncher.launch(new Intent(activity, HistoryActivity.class));
    }

    private boolean isBinaryOperator(String v) {
        return v.equals("+") || v.equals("-") || v.equals("×") || v.equals("÷") || v.equals("^");
    }

    private void appendToExpression(String value, boolean isOperator) {
        if (isResultShown) {
            isResultShown = false;
            if (!isOperator) {
                expressionDisplay.setText("");
            }
            applyTextSizes();
        }

        String currentText = expressionDisplay.getText().toString();
        int cursor = expressionDisplay.getSelectionStart();
        if (cursor < 0 || cursor > currentText.length()) cursor = currentText.length();

        if (value.equals(".") && currentNumberHasDecimal(currentText, cursor)) {
            return;
        }

        if (isOperator && !value.equals("-") && currentText.isEmpty()) {
            return;
        }

        insertAtCursor(value);
    }

    private void insertAtCursor(String value) {
        Editable text = expressionDisplay.getText();
        int cursor = expressionDisplay.getSelectionStart();
        if (cursor < 0 || cursor > text.length()) cursor = text.length();
        text.insert(cursor, value);
        expressionDisplay.setSelection(cursor + value.length());
    }

    private boolean currentNumberHasDecimal(String s, int cursorPos) {
        for (int i = cursorPos - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c == '.') return true;
            if (!Character.isDigit(c)) break;
        }
        for (int i = cursorPos; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '.') return true;
            if (!Character.isDigit(c)) break;
        }
        return false;
    }

    private void clearAll() {
        expressionDisplay.setText("");
        resultDisplay.setText("0");
        resultDisplay.setTextColor(colorPreview);
        isResultShown = false;
        applyTextSizes();
    }

    private void deleteLast() {
        if (isResultShown) {
            clearAll();
            return;
        }
        Editable text = expressionDisplay.getText();
        int cursor = expressionDisplay.getSelectionStart();
        if (cursor > 0 && cursor <= text.length()) {
            text.delete(cursor - 1, cursor);
            expressionDisplay.setSelection(cursor - 1);
        }
    }

    private void refreshPreview() {
        String expr = expressionDisplay.getText().toString();

        if (expr.trim().isEmpty()) {
            resultDisplay.setText("0");
            resultDisplay.setTextColor(colorPreview);
            return;
        }

        ExpressionValidator.Result validation = ExpressionValidator.validate(expr);
        if (validation.valid) {
            try {
                double preview = ExpressionEvaluator.evaluate(expr, true);
                resultDisplay.setText(formatResult(preview));
                resultDisplay.setTextColor(colorPreview);
            } catch (Exception ignored) {
            }
        }
    }

    private void calculateResult() {
        String expr = expressionDisplay.getText().toString();
        if (expr.trim().isEmpty()) return;

        ExpressionValidator.Result validation = ExpressionValidator.validate(expr);
        if (!validation.valid) {
            showInvalid(validation.message);
            return;
        }

        try {
            double value = ExpressionEvaluator.evaluate(expr, true);
            String formatted = formatResult(value);

            resultDisplay.setText(formatted);
            resultDisplay.setTextColor(colorNormal);

            historyManager.addEntry(expr, formatted);

            expressionDisplay.setText(formatted);
            expressionDisplay.setSelection(formatted.length());
            isResultShown = true;
            applyTextSizes();
        } catch (Exception e) {
            showInvalid(e.getMessage());
        }
    }

    private void showInvalid(String detail) {
        resultDisplay.setText(R.string.invalid_expression);
        resultDisplay.setTextColor(colorError);
        if (detail != null) {
            Toast.makeText(activity, detail, Toast.LENGTH_SHORT).show();
        }
        isResultShown = false;
    }

    private String formatResult(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return activity.getString(R.string.invalid_expression);
        }
        if (Math.abs(value - Math.round(value)) < 1e-9 && Math.abs(value) < 1e15) {
            return String.valueOf(Math.round(value));
        }
        BigDecimal bd = new BigDecimal(value).setScale(10, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
    }

    private void applyTextSizes() {
        boolean landscape = activity.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        if (isResultShown) {
            expressionDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    landscape ? EXPR_SIZE_RESULT_LAND : EXPR_SIZE_RESULT);
            resultDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    landscape ? RESULT_SIZE_RESULT_LAND : RESULT_SIZE_RESULT);
            resultDisplay.setTextColor(colorNormal);
        } else {
            expressionDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    landscape ? EXPR_SIZE_TYPING_LAND : EXPR_SIZE_TYPING);
            resultDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP,
                    landscape ? RESULT_SIZE_TYPING_LAND : RESULT_SIZE_TYPING);
            resultDisplay.setTextColor(colorPreview);
        }
    }

}
