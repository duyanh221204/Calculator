package com.duyanhnguyen.myapplication.controller;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.duyanhnguyen.myapplication.R;
import com.duyanhnguyen.myapplication.controller.helper.CalculatorInputManager;
import com.duyanhnguyen.myapplication.controller.helper.KeyMappingContext;
import com.duyanhnguyen.myapplication.data.HistoryManager;
import com.duyanhnguyen.myapplication.core.ExpressionEvaluator;
import com.duyanhnguyen.myapplication.core.ExpressionValidator;
import com.duyanhnguyen.myapplication.ui.activity.HistoryActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    private static final long DELETE_REPEAT_DELAY_MS = 80L;

    private final AppCompatActivity activity;
    private MainUiShellController shellController;
    private EditText expressionDisplay;
    private TextView resultDisplay;
    private boolean isResultShown = false;
    private HistoryManager historyManager;
    private int colorNormal, colorPreview, colorError;
    private ActivityResultLauncher<Intent> historyLauncher;
    private final Handler deleteHandler = new Handler();
    private Runnable deleteRunnable;

    public MainCalculatorController(AppCompatActivity activity, MainUiShellController shellController) {
        this.activity = activity;
        this.shellController = shellController;
    }

    public void onCreate(Bundle savedInstanceState) {
        activity.setContentView(R.layout.activity_main);

        historyManager = new HistoryManager(activity);

        colorNormal = ContextCompat.getColor(activity, R.color.colorTextPrimary);
        colorPreview = ContextCompat.getColor(activity, R.color.colorTextSecondary);
        colorError = ContextCompat.getColor(activity, R.color.colorError);

        expressionDisplay = activity.findViewById(R.id.text_expression);
        resultDisplay = activity.findViewById(R.id.text_result);

        setupHistoryLauncher();

        expressionDisplay.setShowSoftInputOnFocus(false);
        expressionDisplay.requestFocus();
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
                appendToExpression("e", false);
                return true;
            });
        }

        if (savedInstanceState != null) {
            String expr = savedInstanceState.getString(KEY_EXPRESSION, "");
            int cursorPos = savedInstanceState.getInt(KEY_CURSOR_POS, expr.length());
            isResultShown = savedInstanceState.getBoolean(KEY_RESULT_SHOWN, false);
            String resultText = savedInstanceState.getString(KEY_RESULT_TEXT, "");

            expressionDisplay.setText(expr);
            expressionDisplay.setSelection(Math.min(cursorPos, expr.length()));
            resultDisplay.setText(resultText);
        }

        applyTextSizes();

        // Long-press delete: repeat every DELETE_REPEAT_DELAY_MS
        View deleteBtn = activity.findViewById(R.id.btn_delete);
        if (deleteBtn != null) {
            deleteBtn.setOnLongClickListener(v -> {
                deleteRunnable = new Runnable() {
                    @Override
                    public void run() {
                        deleteLast();
                        deleteHandler.postDelayed(this, DELETE_REPEAT_DELAY_MS);
                    }
                };
                deleteHandler.postDelayed(deleteRunnable, DELETE_REPEAT_DELAY_MS);
                return true;
            });
            deleteBtn.setOnTouchListener((v, event) -> {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if (deleteRunnable != null) {
                        deleteHandler.removeCallbacks(deleteRunnable);
                        deleteRunnable = null;
                    }
                }
                return false; // let normal click still work
            });
        }
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
        } else {
            String value = KeyMappingContext.getKeyValue(id);
            if (value != null) {
                appendToExpression(value, KeyMappingContext.isBinaryOperator(value));
            }
        }
    }

    public void onDegRadChanged() {
        if (isResultShown) {
            calculateResult();
        } else {
            refreshPreview();
        }
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

    private void openHistory() {
        historyLauncher.launch(new Intent(activity, HistoryActivity.class));
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

        if (value.equals(".") && CalculatorInputManager.currentNumberHasDecimal(currentText, cursor)) {
            return;
        }

        if (isOperator && !value.equals("-") && currentText.isEmpty()) {
            return;
        }

        // Block '!' if there's nothing valid (digit, closing paren or !) before the cursor
        if (value.equals("!")) {
            if (cursor == 0) return;
            char prev = currentText.charAt(cursor - 1);
            if (!Character.isDigit(prev) && prev != ')' && prev != '!') return;
        }

        // Leading zero removal
        int removeZeroCount = CalculatorInputManager.getLeadingZeroToRemove(currentText, cursor, value);
        if (removeZeroCount > 0) {
            Editable text = expressionDisplay.getText();
            text.delete(cursor - removeZeroCount, cursor);
            cursor -= removeZeroCount;
            currentText = text.toString();
        }

        // Implicit multiply
        String stringToInsert = value;
        if (CalculatorInputManager.shouldAddImplicitMultiply(value, currentText, cursor)) {
            stringToInsert = "×" + value;
        }

        // Simulate new text and guard invalid leading zero
        StringBuilder sb = new StringBuilder(currentText);
        sb.insert(cursor, stringToInsert);
        if (CalculatorInputManager.hasInvalidLeadingZero(sb.toString())) {
            return;
        }

        insertAtCursor(stringToInsert);
    }

    private void insertAtCursor(String value) {
        Editable text = expressionDisplay.getText();
        int cursor = expressionDisplay.getSelectionStart();
        if (cursor < 0 || cursor > text.length()) cursor = text.length();
        text.insert(cursor, value);
        expressionDisplay.setSelection(cursor + value.length());
    }

    private void clearAll() {
        expressionDisplay.setText("");
        resultDisplay.setText("");
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
            int deleteLen = CalculatorInputManager.getFunctionDeleteLength(text.toString(), cursor);
            text.delete(cursor - deleteLen, cursor);
            expressionDisplay.setSelection(cursor - deleteLen);
        }
    }

    private void refreshPreview() {
        String expr = expressionDisplay.getText().toString();

        if (expr.trim().isEmpty()) {
            resultDisplay.setText("");
            resultDisplay.setTextColor(colorPreview);
            return;
        }

        ExpressionValidator.Result validation = ExpressionValidator.validate(expr);
        if (validation.valid) {
            try {
                double preview = ExpressionEvaluator.evaluate(expr, shellController.isDegMode());
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
            double value = ExpressionEvaluator.evaluate(expr, shellController.isDegMode());
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
