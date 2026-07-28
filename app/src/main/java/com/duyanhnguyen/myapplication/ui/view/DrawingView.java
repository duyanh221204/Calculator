package com.duyanhnguyen.myapplication.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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

    // Zoom & Pan state
    private float scaleFactor = 1.0f;
    private float translateX = 0.0f;
    private float translateY = 0.0f;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isMultiTouch = false;

    private ScaleGestureDetector scaleDetector;

    public interface OnStrokeCompletedListener {
        void onStrokeCompleted(Ink ink);
    }

    private OnStrokeCompletedListener strokeCompletedListener;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
        paths = new ArrayList<>();
        inkBuilder = Ink.builder();
        initScaleDetector(context);
    }

    private void initScaleDetector(Context context) {
        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                float newScale = scaleFactor * factor;
                newScale = Math.max(0.5f, Math.min(newScale, 5.0f));

                float focusX = detector.getFocusX();
                float focusY = detector.getFocusY();

                translateX -= (focusX - translateX) * (newScale / scaleFactor - 1.0f);
                translateY -= (focusY - translateY) * (newScale / scaleFactor - 1.0f);

                scaleFactor = newScale;
                invalidate();
                return true;
            }
        });
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

    public void resetZoom() {
        scaleFactor = 1.0f;
        translateX = 0.0f;
        translateY = 0.0f;
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
        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        for (Path path : paths) {
            canvas.drawPath(path, drawPaint);
        }
        if (currentPath != null) {
            canvas.drawPath(currentPath, drawPaint);
        }
        if (!resultText.isEmpty()) {
            canvas.drawText(resultText, resultX, resultY, resultPaint);
        }
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);

        int pointerCount = event.getPointerCount();
        long t = System.currentTimeMillis();

        if (pointerCount > 1) {
            isMultiTouch = true;
            if (currentPath != null) {
                currentPath = null;
                strokeBuilder = null;
                invalidate();
            }
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                isMultiTouch = false;
                lastTouchX = event.getX();
                lastTouchY = event.getY();

                float worldX = (event.getX() - translateX) / scaleFactor;
                float worldY = (event.getY() - translateY) / scaleFactor;

                currentPath = new Path();
                currentPath.moveTo(worldX, worldY);
                strokeBuilder = Ink.Stroke.builder();
                strokeBuilder.addPoint(Ink.Point.create(worldX, worldY, t));
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                isMultiTouch = true;
                break;

            case MotionEvent.ACTION_MOVE:
                if (isMultiTouch || scaleDetector.isInProgress()) {
                    if (pointerCount == 2) {
                        float currX = event.getX();
                        float currY = event.getY();
                        float dx = currX - lastTouchX;
                        float dy = currY - lastTouchY;
                        translateX += dx;
                        translateY += dy;
                        lastTouchX = currX;
                        lastTouchY = currY;
                        invalidate();
                    }
                } else if (currentPath != null) {
                    float wX = (event.getX() - translateX) / scaleFactor;
                    float wY = (event.getY() - translateY) / scaleFactor;
                    currentPath.lineTo(wX, wY);
                    if (strokeBuilder != null) {
                        strokeBuilder.addPoint(Ink.Point.create(wX, wY, t));
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!isMultiTouch && currentPath != null) {
                    float wX = (event.getX() - translateX) / scaleFactor;
                    float wY = (event.getY() - translateY) / scaleFactor;
                    currentPath.lineTo(wX, wY);
                    paths.add(currentPath);
                    currentPath = null;

                    if (strokeBuilder != null) {
                        strokeBuilder.addPoint(Ink.Point.create(wX, wY, t));
                        inkBuilder.addStroke(strokeBuilder.build());
                        strokeBuilder = null;
                    }

                    if (strokeCompletedListener != null) {
                        strokeCompletedListener.onStrokeCompleted(inkBuilder.build());
                    }

                    resultX = wX + 30;
                    resultY = wY;
                }
                isMultiTouch = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                currentPath = null;
                strokeBuilder = null;
                isMultiTouch = false;
                break;
        }
        invalidate();
        return true;
    }
}
