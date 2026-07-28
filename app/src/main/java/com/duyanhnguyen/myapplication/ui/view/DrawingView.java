package com.duyanhnguyen.myapplication.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.google.mlkit.vision.digitalink.Ink;

import java.util.ArrayList;
import java.util.List;

public class DrawingView extends View {

    private Paint drawPaint;
    private Paint resultPaint;
    private List<Path> paths;
    private Path currentPath;

    private Ink.Builder inkBuilder;
    private Ink.Stroke.Builder strokeBuilder;

    private String resultText = "";
    private float resultX = 0f;
    private float resultY = 0f;

    public interface OnStrokeCompletedListener {
        void onStrokeCompleted(Ink ink);
    }

    private OnStrokeCompletedListener strokeCompletedListener;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
        paths = new ArrayList<>();
        inkBuilder = Ink.builder();
    }

    private void setupPaint() {
        drawPaint = new Paint();
        drawPaint.setColor(Color.WHITE);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(12f);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        resultPaint = new Paint();
        resultPaint.setColor(Color.parseColor("#FF9800"));
        resultPaint.setTextSize(80f);
        resultPaint.setAntiAlias(true);
        resultPaint.setFakeBoldText(true);
    }

    public void setStrokeCompletedListener(OnStrokeCompletedListener listener) {
        this.strokeCompletedListener = listener;
    }

    public void clear() {
        paths.clear();
        inkBuilder = Ink.builder();
        resultText = "";
        invalidate();
    }

    public void setResultText(String text, float x, float y) {
        this.resultText = text;
        this.resultX = x;
        this.resultY = y;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Path path : paths) {
            canvas.drawPath(path, drawPaint);
        }
        if (currentPath != null) {
            canvas.drawPath(currentPath, drawPaint);
        }
        if (!resultText.isEmpty()) {
            canvas.drawText(resultText, resultX, resultY, resultPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        long t = System.currentTimeMillis();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                strokeBuilder = Ink.Stroke.builder();
                strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                break;
            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                if (strokeBuilder != null) {
                    strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                }
                break;
            case MotionEvent.ACTION_UP:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    paths.add(currentPath);
                    currentPath = null;
                }
                if (strokeBuilder != null) {
                    strokeBuilder.addPoint(Ink.Point.create(x, y, t));
                    inkBuilder.addStroke(strokeBuilder.build());
                    strokeBuilder = null;
                }
                if (strokeCompletedListener != null) {
                    strokeCompletedListener.onStrokeCompleted(inkBuilder.build());
                }

                resultX = x + 30;
                resultY = y;
                break;
        }
        invalidate();
        return true;
    }
}
