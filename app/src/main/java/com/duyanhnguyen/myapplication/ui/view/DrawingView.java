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

    public enum Mode {
        DRAW, ERASE
    }

    private static class StrokeItem {
        Path path;
        Ink.Stroke inkStroke;

        StrokeItem(Path path, Ink.Stroke inkStroke) {
            this.path = path;
            this.inkStroke = inkStroke;
        }
    }

    private Paint drawPaint;
    private Paint resultPaint;
    private List<StrokeItem> strokeItems;
    private Path currentPath;

    private Ink.Builder inkBuilder;
    private Ink.Stroke.Builder strokeBuilder;

    private Mode currentMode = Mode.DRAW;

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
    private boolean erasedDuringTouch = false;

    private ScaleGestureDetector scaleDetector;

    public interface OnStrokeCompletedListener {
        void onStrokeCompleted(Ink ink);
    }

    private OnStrokeCompletedListener strokeCompletedListener;

    public DrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPaint();
        strokeItems = new ArrayList<>();
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

    public void setMode(Mode mode) {
        this.currentMode = mode;
    }

    public Mode getMode() {
        return currentMode;
    }

    public void setStrokeCompletedListener(OnStrokeCompletedListener listener) {
        this.strokeCompletedListener = listener;
    }

    public void clear() {
        strokeItems.clear();
        inkBuilder = Ink.builder();
        resultText = "";
        invalidate();
        if (strokeCompletedListener != null) {
            strokeCompletedListener.onStrokeCompleted(inkBuilder.build());
        }
    }

    public void undo() {
        if (strokeItems.isEmpty()) return;
        strokeItems.remove(strokeItems.size() - 1);
        rebuildAndNotifyInk();
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

    private void rebuildAndNotifyInk() {
        Ink.Builder newBuilder = Ink.builder();
        for (StrokeItem item : strokeItems) {
            if (item.inkStroke != null) {
                newBuilder.addStroke(item.inkStroke);
            }
        }
        inkBuilder = newBuilder;
        if (strokeCompletedListener != null) {
            strokeCompletedListener.onStrokeCompleted(inkBuilder.build());
        }
    }

    private boolean eraseStrokesAt(float wX, float wY) {
        boolean erasedAny = false;
        float eraseRadiusSq = (40f / scaleFactor) * (40f / scaleFactor);

        List<StrokeItem> toRemove = new ArrayList<>();
        for (StrokeItem item : strokeItems) {
            if (item.inkStroke == null) continue;
            for (Ink.Point pt : item.inkStroke.getPoints()) {
                float dx = pt.getX() - wX;
                float dy = pt.getY() - wY;
                if (dx * dx + dy * dy <= eraseRadiusSq) {
                    toRemove.add(item);
                    erasedAny = true;
                    break;
                }
            }
        }
        if (erasedAny) {
            strokeItems.removeAll(toRemove);
        }
        return erasedAny;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        canvas.translate(translateX, translateY);
        canvas.scale(scaleFactor, scaleFactor);

        for (StrokeItem item : strokeItems) {
            canvas.drawPath(item.path, drawPaint);
        }
        if (currentPath != null && currentMode == Mode.DRAW) {
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
                erasedDuringTouch = false;
                lastTouchX = event.getX();
                lastTouchY = event.getY();

                float worldX = (event.getX() - translateX) / scaleFactor;
                float worldY = (event.getY() - translateY) / scaleFactor;

                if (currentMode == Mode.ERASE) {
                    if (eraseStrokesAt(worldX, worldY)) {
                        erasedDuringTouch = true;
                    }
                } else {
                    currentPath = new Path();
                    currentPath.moveTo(worldX, worldY);
                    strokeBuilder = Ink.Stroke.builder();
                    strokeBuilder.addPoint(Ink.Point.create(worldX, worldY, t));
                }
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
                } else {
                    float wX = (event.getX() - translateX) / scaleFactor;
                    float wY = (event.getY() - translateY) / scaleFactor;

                    if (currentMode == Mode.ERASE) {
                        if (eraseStrokesAt(wX, wY)) {
                            erasedDuringTouch = true;
                        }
                    } else if (currentPath != null) {
                        currentPath.lineTo(wX, wY);
                        if (strokeBuilder != null) {
                            strokeBuilder.addPoint(Ink.Point.create(wX, wY, t));
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (!isMultiTouch) {
                    float wX = (event.getX() - translateX) / scaleFactor;
                    float wY = (event.getY() - translateY) / scaleFactor;

                    if (currentMode == Mode.ERASE) {
                        if (erasedDuringTouch) {
                            rebuildAndNotifyInk();
                        }
                    } else if (currentPath != null) {
                        currentPath.lineTo(wX, wY);
                        Ink.Stroke stroke = (strokeBuilder != null) ? strokeBuilder.addPoint(Ink.Point.create(wX, wY, t)).build() : null;
                        
                        strokeItems.add(new StrokeItem(currentPath, stroke));
                        currentPath = null;
                        strokeBuilder = null;

                        if (stroke != null) {
                            inkBuilder.addStroke(stroke);
                        }

                        if (strokeCompletedListener != null) {
                            strokeCompletedListener.onStrokeCompleted(inkBuilder.build());
                        }

                        resultX = wX + 30;
                        resultY = wY;
                    }
                }
                isMultiTouch = false;
                erasedDuringTouch = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                currentPath = null;
                strokeBuilder = null;
                isMultiTouch = false;
                erasedDuringTouch = false;
                break;
        }
        invalidate();
        return true;
    }
}
