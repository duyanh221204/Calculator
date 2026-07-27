package com.duyanhnguyen.myapplication.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.duyanhnguyen.myapplication.core.FunctionParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * View vẽ đồ thị hàm số 2D.
 *
 * Tương tác:
 *  - 1 ngón tay kéo trên đường cong -> chế độ "trace": hiện toạ độ tại điểm
 *    gần nhất theo x của ngón tay.
 *  - 2 ngón tay -> pan (kéo) + pinch zoom toàn khung nhìn.
 *
 * Hệ toạ độ: giữ 1 biến scale "pxPerUnit" áp dụng cho cả trục x và y để
 * hình dạng đồ thị không bị méo khi zoom (không zoom lệch trục).
 */
public class GraphView extends View {

    private static class Extremum {
        double x, y;
        boolean isMax;
        Extremum(double x, double y, boolean isMax) { this.x = x; this.y = y; this.isMax = isMax; }
    }

    private FunctionParser.Expr function;
    private String functionLabel = "";

    // --- viewport: (originPx) là toạ độ pixel ứng với điểm toán học (0,0) ---
    private float originPxX, originPxY;
    private float pxPerUnit = 80f; // 80px = 1 đơn vị lúc khởi tạo
    private static final float MIN_PX_PER_UNIT = 4f;
    private static final float MAX_PX_PER_UNIT = 4000f;

    private final List<Extremum> extrema = new ArrayList<>();
    private static final double EXTREMA_DOMAIN_MIN = -100;
    private static final double EXTREMA_DOMAIN_MAX = 100;

    // --- trace mode (1 ngón tay) ---
    private boolean tracing = false;
    private double traceMathX = 0;

    private int activePointerCount = 0;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private final Paint axisPaint = new Paint();
    private final Paint gridPaint = new Paint();
    private final Paint curvePaint = new Paint();
    private final Paint extremaPaint = new Paint();
    private final Paint tracePaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint labelBgPaint = new Paint();

    public GraphView(Context context, AttributeSet attrs) {
        super(context, attrs);

        axisPaint.setColor(Color.DKGRAY);
        axisPaint.setStrokeWidth(3f);

        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);

        curvePaint.setColor(Color.parseColor("#1E88E5"));
        curvePaint.setStrokeWidth(6f);
        curvePaint.setStyle(Paint.Style.STROKE);
        curvePaint.setAntiAlias(true);

        extremaPaint.setColor(Color.parseColor("#E53935"));
        extremaPaint.setAntiAlias(true);

        tracePaint.setColor(Color.parseColor("#43A047"));
        tracePaint.setStrokeWidth(2f);
        tracePaint.setAntiAlias(true);

        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(dp(13));
        textPaint.setAntiAlias(true);

        labelBgPaint.setColor(Color.parseColor("#DDFFFFFF"));

        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw == 0 && oldh == 0) {
            // căn giữa gốc toạ độ lần đầu
            originPxX = w / 2f;
            originPxY = h / 2f;
        }
    }

    /** Gọi khi người dùng nhập hàm mới. */
    public void setFunction(FunctionParser.Expr expr, String label) {
        this.function = expr;
        this.functionLabel = label;
        computeExtrema();
        tracing = false;
        invalidate();
    }

    public void resetView() {
        originPxX = getWidth() / 2f;
        originPxY = getHeight() / 2f;
        pxPerUnit = 80f;
        invalidate();
    }

    // ---------------------------------------------------------- Toạ độ
    private float mathToScreenX(double x) { return (float) (originPxX + x * pxPerUnit); }
    private float mathToScreenY(double y) { return (float) (originPxY - y * pxPerUnit); }
    private double screenToMathX(float px) { return (px - originPxX) / pxPerUnit; }
    private double screenToMathY(float py) { return (originPxY - py) / pxPerUnit; }

    // ---------------------------------------------------------- Vẽ
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas);
        drawAxes(canvas);
        if (function != null) {
            drawCurve(canvas);
            drawExtrema(canvas);
            if (tracing) drawTraceCursor(canvas);
        }
    }

    private void drawGrid(Canvas canvas) {
        double step = niceStep(100 / (double) pxPerUnit); // ~100px giữa 2 vạch
        double startX = Math.floor(screenToMathX(0) / step) * step;
        double endX = screenToMathX(getWidth());
        for (double x = startX; x <= endX; x += step) {
            float px = mathToScreenX(x);
            canvas.drawLine(px, 0, px, getHeight(), gridPaint);
        }
        double startY = Math.floor(screenToMathY(getHeight()) / step) * step;
        double endY = screenToMathY(0);
        for (double y = startY; y <= endY; y += step) {
            float py = mathToScreenY(y);
            canvas.drawLine(0, py, getWidth(), py, gridPaint);
        }
    }

    private void drawAxes(Canvas canvas) {
        canvas.drawLine(0, originPxY, getWidth(), originPxY, axisPaint); // trục x
        canvas.drawLine(originPxX, 0, originPxX, getHeight(), axisPaint); // trục y

        double step = niceStep(100 / (double) pxPerUnit);
        double startX = Math.floor(screenToMathX(0) / step) * step;
        double endX = screenToMathX(getWidth());
        for (double x = startX; x <= endX; x += step) {
            if (Math.abs(x) < step / 2) continue; // bỏ nhãn ở gốc cho gọn
            float px = mathToScreenX(x);
            canvas.drawText(formatNumber(x), px + 4, originPxY - 6, textPaint);
        }
        double startY = Math.floor(screenToMathY(getHeight()) / step) * step;
        double endY = screenToMathY(0);
        for (double y = startY; y <= endY; y += step) {
            if (Math.abs(y) < step / 2) continue;
            float py = mathToScreenY(y);
            canvas.drawText(formatNumber(y), originPxX + 4, py - 4, textPaint);
        }
    }

    private void drawCurve(Canvas canvas) {
        Path path = new Path();
        boolean penDown = false;
        Float prevScreenY = null;
        float maxJump = getHeight() * 3f; // ngưỡng phát hiện gián đoạn (tiệm cận đứng)

        for (int px = 0; px <= getWidth(); px += 2) {
            double mx = screenToMathX(px);
            double my;
            try {
                my = function.eval(mx);
            } catch (Exception e) {
                my = Double.NaN;
            }

            if (Double.isNaN(my) || Double.isInfinite(my)) {
                penDown = false;
                prevScreenY = null;
                continue;
            }
            float py = mathToScreenY(my);

            if (prevScreenY != null && Math.abs(py - prevScreenY) > maxJump) {
                // nhảy quá lớn -> coi như gián đoạn (vd tan(x) tại pi/2)
                penDown = false;
            }

            if (!penDown) {
                path.moveTo(px, py);
                penDown = true;
            } else {
                path.lineTo(px, py);
            }
            prevScreenY = py;
        }
        canvas.drawPath(path, curvePaint);
    }

    private void drawExtrema(Canvas canvas) {
        double visMinX = screenToMathX(0);
        double visMaxX = screenToMathX(getWidth());
        for (Extremum e : extrema) {
            if (e.x < visMinX || e.x > visMaxX) continue;
            float px = mathToScreenX(e.x);
            float py = mathToScreenY(e.y);
            canvas.drawCircle(px, py, dp(5), extremaPaint);

            String label = (e.isMax ? "Cực đại " : "Cực tiểu ")
                    + "(" + formatNumber(e.x) + ", " + formatNumber(e.y) + ")";
            drawLabel(canvas, label, px, py - dp(14));
        }
    }

    private void drawTraceCursor(Canvas canvas) {
        double my;
        try {
            my = function.eval(traceMathX);
        } catch (Exception e) {
            my = Double.NaN;
        }
        if (Double.isNaN(my) || Double.isInfinite(my)) return;

        float px = mathToScreenX(traceMathX);
        float py = mathToScreenY(my);

        canvas.drawLine(px, 0, px, getHeight(), tracePaint);
        canvas.drawCircle(px, py, dp(6), tracePaint);

        String label = "x=" + formatNumber(traceMathX) + "  y=" + formatNumber(my);
        drawLabel(canvas, label, px, Math.max(py - dp(20), dp(20)));
    }

    private void drawLabel(Canvas canvas, String text, float anchorX, float anchorY) {
        float textWidth = textPaint.measureText(text);
        float padding = dp(4);
        float left = anchorX - textWidth / 2f - padding;
        float right = anchorX + textWidth / 2f + padding;
        // giữ label trong màn hình
        if (left < 0) { right -= left; left = 0; }
        if (right > getWidth()) { left -= (right - getWidth()); right = getWidth(); }

        canvas.drawRect(left, anchorY - textPaint.getTextSize(), right, anchorY + padding, labelBgPaint);
        canvas.drawText(text, left + padding, anchorY, textPaint);
    }

    // ---------------------------------------------------------- Cực trị
    private void computeExtrema() {
        extrema.clear();
        if (function == null) return;

        int steps = 8000;
        double h = (EXTREMA_DOMAIN_MAX - EXTREMA_DOMAIN_MIN) / steps;
        double dx = 1e-4;

        Double prevDeriv = derivativeAt(EXTREMA_DOMAIN_MIN, dx);
        for (int i = 1; i <= steps; i++) {
            double x = EXTREMA_DOMAIN_MIN + i * h;
            Double deriv = derivativeAt(x, dx);
            if (prevDeriv != null && deriv != null
                    && !prevDeriv.isNaN() && !deriv.isNaN()
                    && Math.signum(prevDeriv) != Math.signum(deriv)
                    && Math.signum(prevDeriv) != 0) {

                Double rootX = bisectDerivativeRoot(x - h, x, dx);
                if (rootX != null) {
                    double y;
                    try { y = function.eval(rootX); } catch (Exception e) { y = Double.NaN; }
                    if (!Double.isNaN(y) && !Double.isInfinite(y)) {
                        boolean isMax = prevDeriv > 0 && deriv < 0;
                        extrema.add(new Extremum(rootX, y, isMax));
                    }
                }
            }
            prevDeriv = deriv;
        }
    }

    private Double derivativeAt(double x, double dx) {
        try {
            double y1 = function.eval(x + dx);
            double y2 = function.eval(x - dx);
            double d = (y1 - y2) / (2 * dx);
            if (Double.isNaN(d) || Double.isInfinite(d)) return null;
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    private Double bisectDerivativeRoot(double lo, double hi, double dx) {
        Double fLo = derivativeAt(lo, dx);
        Double fHi = derivativeAt(hi, dx);
        if (fLo == null || fHi == null) return null;
        for (int i = 0; i < 40; i++) {
            double mid = (lo + hi) / 2;
            Double fMid = derivativeAt(mid, dx);
            if (fMid == null) return null;
            if (Math.signum(fMid) == Math.signum(fLo)) {
                lo = mid; fLo = fMid;
            } else {
                hi = mid;
            }
        }
        return (lo + hi) / 2;
    }

    // ---------------------------------------------------------- Touch
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerCount = 1;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                activePointerCount = event.getPointerCount();
                tracing = false; // đủ 2 ngón -> chuyển sang pan/zoom, tắt trace
                break;
            case MotionEvent.ACTION_POINTER_UP:
                activePointerCount = event.getPointerCount() - 1;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerCount = 0;
                break;
        }

        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (activePointerCount == 1 && function != null) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                tracing = true;
                traceMathX = screenToMathX(event.getX());
                invalidate();
            }
        }
        if (event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            tracing = false;
            invalidate();
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            double mathXBefore = screenToMathX(focusX);
            double mathYBefore = screenToMathY(focusY);

            pxPerUnit *= detector.getScaleFactor();
            pxPerUnit = Math.max(MIN_PX_PER_UNIT, Math.min(MAX_PX_PER_UNIT, pxPerUnit));

            originPxX = (float) (focusX - mathXBefore * pxPerUnit);
            originPxY = (float) (focusY + mathYBefore * pxPerUnit);

            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (activePointerCount >= 2) {
                originPxX -= distanceX;
                originPxY -= distanceY;
                invalidate();
            }
            return true;
        }
    }

    // ---------------------------------------------------------- Helpers
    private static double niceStep(double rawStep) {
        if (rawStep <= 0) return 1;
        double exponent = Math.floor(Math.log10(rawStep));
        double fraction = rawStep / Math.pow(10, exponent);
        double niceFraction;
        if (fraction < 1.5) niceFraction = 1;
        else if (fraction < 3) niceFraction = 2;
        else if (fraction < 7) niceFraction = 5;
        else niceFraction = 10;
        return niceFraction * Math.pow(10, exponent);
    }

    private static String formatNumber(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-6) {
            return String.valueOf(Math.round(v));
        }
        return String.format(Locale.US, "%.2f", v);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
