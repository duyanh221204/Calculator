package com.duyanhnguyen.myapplication.ui.activity;

import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.duyanhnguyen.myapplication.R;
import com.duyanhnguyen.myapplication.core.FunctionParser;
import com.duyanhnguyen.myapplication.ui.view.GraphView;

public class GraphActivity extends AppCompatActivity {

    private GraphView graphView;
    private CardView collapsedBar;
    private View editorOverlay;
    private TextView tvFunctionPreview;
    private TextView tvError;
    private EditText etFunction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int requestedOrientation = getIntent().getIntExtra("EXTRA_ORIENTATION", android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (requestedOrientation != android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            setRequestedOrientation(requestedOrientation);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        graphView = findViewById(R.id.graphView);
        collapsedBar = findViewById(R.id.collapsedBar);
        editorOverlay = findViewById(R.id.editorOverlay);
        tvFunctionPreview = findViewById(R.id.tvFunctionPreview);
        tvError = findViewById(R.id.tvError);
        etFunction = findViewById(R.id.etFunction);

        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        findViewById(R.id.btnResetView).setOnClickListener(v -> graphView.resetView());
        collapsedBar.setOnClickListener(v -> openEditor());
        findViewById(R.id.btnCancel).setOnClickListener(v -> closeEditor());
        findViewById(R.id.btnPlot).setOnClickListener(v -> tryPlot());

        setupKeypad();

        etFunction.setText("x^3-3*x");
        tryPlot();
    }

    private void openEditor() {
        editorOverlay.setVisibility(View.VISIBLE);
        collapsedBar.setVisibility(View.GONE);
        etFunction.requestFocus();
    }

    private void closeEditor() {
        editorOverlay.setVisibility(View.GONE);
        collapsedBar.setVisibility(View.VISIBLE);
    }

    private void tryPlot() {
        String text = etFunction.getText().toString().trim();
        if (text.isEmpty()) {
            tvError.setText("Nhập hàm số trước đã");
            return;
        }
        try {
            FunctionParser.Expr expr = FunctionParser.parse(text);

            expr.eval(1.0);

            graphView.setFunction(expr, text);
            tvFunctionPreview.setText("y = " + text);
            tvError.setText("");
            closeEditor();
        } catch (FunctionParser.ParseException e) {
            tvError.setText("Lỗi cú pháp: " + e.getMessage());
        } catch (Exception e) {
            tvError.setText("Không vẽ được: " + e.getMessage());
        }
    }

    private void insertAtCursor(String text) {
        Editable editable = etFunction.getText();
        int start = etFunction.getSelectionStart();
        int end = etFunction.getSelectionEnd();
        if (start < 0) start = editable.length();
        if (end < 0) end = editable.length();
        editable.replace(Math.min(start, end), Math.max(start, end), text);
        etFunction.setSelection(Math.min(start, end) + text.length());
    }

    private void setupKeypad() {
        bindInsert(R.id.btnX, "x");
        bindInsert(R.id.btnPow, "^");
        bindInsert(R.id.btnLParen, "(");
        bindInsert(R.id.btnRParen, ")");
        bindInsert(R.id.btnDiv, "/");
        bindInsert(R.id.btnMul, "*");
        bindInsert(R.id.btnPlus, "+");
        bindInsert(R.id.btnMinus, "-");
        bindInsert(R.id.btnSin, "sin(");
        bindInsert(R.id.btnCos, "cos(");
        bindInsert(R.id.btnTan, "tan(");
        bindInsert(R.id.btnSqrt, "sqrt(");
        bindInsert(R.id.btnLn, "ln(");
        bindInsert(R.id.btnAbs, "abs(");
        bindInsert(R.id.btnPi, "pi");

        Button btnClear = findViewById(R.id.btnClearEntry);
        btnClear.setOnClickListener(v -> etFunction.setText(""));

        Button btnBackspace = findViewById(R.id.btnBackspace);
        btnBackspace.setOnClickListener(v -> {
            int start = etFunction.getSelectionStart();
            if (start > 0) {
                etFunction.getText().delete(start - 1, start);
            }
        });
    }

    private void bindInsert(int viewId, String text) {
        Button b = findViewById(viewId);
        b.setOnClickListener(v -> insertAtCursor(text));
    }
}
