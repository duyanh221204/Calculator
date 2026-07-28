package com.duyanhnguyen.myapplication.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.duyanhnguyen.myapplication.R;
import com.duyanhnguyen.myapplication.controller.MathInkManager;
import com.duyanhnguyen.myapplication.core.ExpressionEvaluator;
import com.duyanhnguyen.myapplication.core.ExpressionPreprocessor;
import com.duyanhnguyen.myapplication.ui.view.DrawingView;

public class MathNoteActivity extends AppCompatActivity {

    private DrawingView drawingView;
    private TextView tvStatus;
    private MathInkManager inkManager;
    private Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable recognizeRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_note);

        drawingView = findViewById(R.id.drawing_view);
        tvStatus = findViewById(R.id.tv_status);
        View btnClear = findViewById(R.id.btn_clear);
        View btnResetZoom = findViewById(R.id.btn_reset_zoom);
        View btnUndo = findViewById(R.id.btn_undo);
        ImageButton btnMode = findViewById(R.id.btn_mode);
        View btnBack = findViewById(R.id.btn_back);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnClear != null) {
            btnClear.setOnClickListener(v -> drawingView.clear());
        }

        if (btnResetZoom != null) {
            btnResetZoom.setOnClickListener(v -> drawingView.resetZoom());
        }

        if (btnUndo != null) {
            btnUndo.setOnClickListener(v -> drawingView.undo());
        }

        if (btnMode != null) {
            btnMode.setOnClickListener(v -> {
                if (drawingView.getMode() == DrawingView.Mode.DRAW) {
                    drawingView.setMode(DrawingView.Mode.ERASE);
                    btnMode.setImageResource(android.R.drawable.ic_menu_delete);
                    btnMode.setColorFilter(android.graphics.Color.parseColor("#F44336"));
                    Toast.makeText(this, "Eraser Mode", Toast.LENGTH_SHORT).show();
                } else {
                    drawingView.setMode(DrawingView.Mode.DRAW);
                    btnMode.setImageResource(android.R.drawable.ic_menu_edit);
                    btnMode.setColorFilter(android.graphics.Color.parseColor("#FF9800"));
                    Toast.makeText(this, "Pen Mode", Toast.LENGTH_SHORT).show();
                }
            });
        }

        inkManager = new MathInkManager();

        inkManager.downloadModelIfNeeded(new MathInkManager.InkCallback() {
            @Override
            public void onResult(String recognizedText) {
                processRecognizedText(recognizedText);
            }

            @Override
            public void onError(String error) {
                tvStatus.setText("Error: " + error);
            }

            @Override
            public void onModelDownloaded() {
                tvStatus.setText("Ready to draw!");
            }
        });

        drawingView.setStrokeCompletedListener(ink -> {

            if (recognizeRunnable != null) {
                debounceHandler.removeCallbacks(recognizeRunnable);
            }

            recognizeRunnable = () -> inkManager.recognize(ink, new MathInkManager.InkCallback() {
                @Override
                public void onResult(String recognizedText) {
                    processRecognizedText(recognizedText);
                }

                @Override
                public void onError(String error) {
                    tvStatus.setText("Error: " + error);
                }

                @Override
                public void onModelDownloaded() {
                }
            });

            debounceHandler.postDelayed(recognizeRunnable, 800);
        });

        btnClear.setOnClickListener(v -> drawingView.clear());
    }

    private void processRecognizedText(String rawText) {
        if (rawText == null || rawText.isEmpty()) return;

        Log.d("MathNote", "Raw OCR: " + rawText);
        String preprocessed = ExpressionPreprocessor.normalize(rawText);
        Log.d("MathNote", "Preprocessed: " + preprocessed);

        try {

            double result = ExpressionEvaluator.evaluate(preprocessed, false);

            String resStr = String.valueOf(result);
            if (resStr.endsWith(".0")) {
                resStr = resStr.substring(0, resStr.length() - 2);
            }

            tvStatus.setText("Calculated: " + preprocessed + " = " + resStr);

            drawingView.setResultText(resStr, drawingView.getWidth() - 200, drawingView.getHeight() / 2f);
        } catch (Exception e) {
            tvStatus.setText("Recognized: " + rawText + " (Cannot evaluate)");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        inkManager.close();
        if (recognizeRunnable != null) {
            debounceHandler.removeCallbacks(recognizeRunnable);
        }
    }
}
