package com.example.weatherapp.Helpers.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PointTrendChartView extends View {

    private final List<ChartDataPoint> dataPoints = new ArrayList<>();
    private int primaryColor = Color.parseColor("#FF7043");
    private int pointRadiusPx;
    private int paddingTopPx;
    private int paddingBottomPx;
    private int paddingSidePx;
    private int minItemWidthPx;

    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointOuterPaint;
    private Paint pointInnerPaint;
    private Paint valueTextPaint;
    private Paint labelTextPaint;
    private Paint gridLinePaint;

    private int selectedIndex = -1;

    public PointTrendChartView(Context context) {
        super(context);
        init();
    }

    public PointTrendChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PointTrendChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        pointRadiusPx = dpToPx(5);
        paddingTopPx = dpToPx(38);
        paddingBottomPx = dpToPx(32);
        paddingSidePx = dpToPx(30);
        minItemWidthPx = dpToPx(65);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dpToPx(2.8f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        pointOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointOuterPaint.setStyle(Paint.Style.FILL);
        pointOuterPaint.setColor(Color.WHITE);

        pointInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointInnerPaint.setStyle(Paint.Style.FILL);

        valueTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valueTextPaint.setColor(Color.WHITE);
        valueTextPaint.setTextSize(spToPx(12));
        valueTextPaint.setTextAlign(Paint.Align.CENTER);
        valueTextPaint.setFakeBoldText(true);
        valueTextPaint.setShadowLayer(dpToPx(2), 0, dpToPx(1), Color.parseColor("#80000000"));

        labelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelTextPaint.setColor(Color.parseColor("#D0D7DE"));
        labelTextPaint.setTextSize(spToPx(11));
        labelTextPaint.setTextAlign(Paint.Align.CENTER);

        gridLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridLinePaint.setColor(Color.parseColor("#26FFFFFF"));
        gridLinePaint.setStyle(Paint.Style.STROKE);
        gridLinePaint.setStrokeWidth(dpToPx(1));
        gridLinePaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(4), dpToPx(4)}, 0));
    }

    public void setData(List<ChartDataPoint> points, int color) {
        this.dataPoints.clear();
        if (points != null) {
            for (ChartDataPoint p : points) {
                if (p != null) {
                    this.dataPoints.add(p);
                }
            }
        }
        this.primaryColor = color;
        this.selectedIndex = -1;

        linePaint.setColor(primaryColor);
        pointInnerPaint.setColor(primaryColor);

        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (height <= 0) {
            height = dpToPx(190);
        }

        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int desiredWidth;

        if (dataPoints.isEmpty()) {
            desiredWidth = parentWidth;
        } else if (dataPoints.size() <= 6) {
            desiredWidth = Math.max(parentWidth, dataPoints.size() * minItemWidthPx);
        } else {
            desiredWidth = Math.max(parentWidth, (dataPoints.size() - 1) * minItemWidthPx + (paddingSidePx * 2));
        }

        setMeasuredDimension(desiredWidth, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints.isEmpty() || canvas == null) return;

        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float minVal = Float.MAX_VALUE;
        float maxVal = -Float.MAX_VALUE;
        for (ChartDataPoint p : dataPoints) {
            if (p == null) continue;
            if (p.value < minVal) minVal = p.value;
            if (p.value > maxVal) maxVal = p.value;
        }


        if (minVal > maxVal) return;


        if (minVal == maxVal) {
            minVal -= 1;
            maxVal += 1;
        }

        float usableHeight = height - paddingTopPx - paddingBottomPx;
        if (usableHeight <= 0) usableHeight = 1;
        float valRange = maxVal - minVal;
        if (valRange <= 0) valRange = 1;

        int count = dataPoints.size();
        float stepX;
        if (count <= 1) {
            stepX = 0;
        } else {
            stepX = (float) (width - paddingSidePx * 2) / (count - 1);
        }

        drawGridLine(canvas, paddingTopPx, width);
        drawGridLine(canvas, paddingTopPx + usableHeight / 2f, width);
        drawGridLine(canvas, paddingTopPx + usableHeight, width);


        float[] xCoords = new float[count];
        float[] yCoords = new float[count];

        for (int i = 0; i < count; i++) {
            ChartDataPoint p = dataPoints.get(i);
            float x = (count == 1) ? (width / 2f) : (paddingSidePx + i * stepX);
            float val = (p != null) ? p.value : minVal;
            float normVal = (val - minVal) / valRange;
            if (Float.isNaN(normVal) || Float.isInfinite(normVal)) normVal = 0.5f;
            float y = paddingTopPx + (1f - normVal) * usableHeight;
            xCoords[i] = x;
            yCoords[i] = y;
        }


        Path fillPath = new Path();
        fillPath.moveTo(xCoords[0], height - paddingBottomPx + dpToPx(8));
        fillPath.lineTo(xCoords[0], yCoords[0]);
        for (int i = 1; i < count; i++) {
            fillPath.lineTo(xCoords[i], yCoords[i]);
        }
        fillPath.lineTo(xCoords[count - 1], height - paddingBottomPx + dpToPx(8));
        fillPath.close();

        int startColor = Color.argb(85, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor));
        int endColor = Color.argb(0, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor));
        float startY = paddingTopPx;
        float endY = Math.max(startY + 1, height - paddingBottomPx);
        fillPaint.setShader(new LinearGradient(0, startY, 0, endY, startColor, endColor, Shader.TileMode.CLAMP));
        canvas.drawPath(fillPath, fillPaint);


        Path linePath = new Path();
        linePath.moveTo(xCoords[0], yCoords[0]);
        for (int i = 1; i < count; i++) {
            linePath.lineTo(xCoords[i], yCoords[i]);
        }
        canvas.drawPath(linePath, linePaint);

        for (int i = 0; i < count; i++) {
            ChartDataPoint p = dataPoints.get(i);
            float x = xCoords[i];
            float y = yCoords[i];
            boolean isSelected = (i == selectedIndex);


            int outerRadius = isSelected ? (pointRadiusPx + dpToPx(3)) : (pointRadiusPx + dpToPx(1.5f));
            canvas.drawCircle(x, y, outerRadius, pointOuterPaint);


            int innerRadius = isSelected ? (pointRadiusPx + dpToPx(1)) : pointRadiusPx;
            canvas.drawCircle(x, y, innerRadius, pointInnerPaint);


            String valStr = (p != null && p.displayValue != null) ? p.displayValue : "--";
            canvas.drawText(valStr, x, y - dpToPx(10), valueTextPaint);


            String labelStr = (p != null && p.label != null) ? p.label : "--";
            if (labelStr.contains("\n")) {
                String[] lines = labelStr.split("\n");
                canvas.drawText(lines[0], x, height - dpToPx(18), labelTextPaint);
                if (lines.length > 1) {
                    canvas.drawText(lines[1], x, height - dpToPx(5), labelTextPaint);
                }
            } else {
                canvas.drawText(labelStr, x, height - dpToPx(8), labelTextPaint);
            }
        }
    }

    private void drawGridLine(Canvas canvas, float y, float width) {
        Path gridPath = new Path();
        gridPath.moveTo(paddingSidePx / 2f, y);
        gridPath.lineTo(width - (paddingSidePx / 2f), y);
        canvas.drawPath(gridPath, gridLinePaint);
    }

    private float downTouchX;
    private float downTouchY;
    private boolean isTapGesture = false;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (dataPoints.isEmpty() || event == null) {
            return super.onTouchEvent(event);
        }
        int action = event.getActionMasked();

        if (action == MotionEvent.ACTION_DOWN) {
            downTouchX = event.getX();
            downTouchY = event.getY();
            isTapGesture = true;
            return true;
        } else if (action == MotionEvent.ACTION_MOVE) {
            float dx = Math.abs(event.getX() - downTouchX);
            float dy = Math.abs(event.getY() - downTouchY);

            if (dx > dy && dx > dpToPx(6)) {

                android.view.ViewParent parent = getParent();
                while (parent != null) {
                    if (parent instanceof androidx.core.widget.NestedScrollView) {
                        parent.requestDisallowInterceptTouchEvent(true);
                        break;
                    }
                    parent = parent.getParent();
                }
                isTapGesture = false;
            } else if (dy > dx && dy > dpToPx(8)) {
                isTapGesture = false;
            }
            return false;
        } else if (action == MotionEvent.ACTION_UP) {
            if (isTapGesture) {
                float touchX = event.getX();
                int closestIndex = -1;
                float minDistance = Float.MAX_VALUE;

                int count = dataPoints.size();
                float stepX = (count <= 1) ? 0 : (float) (getWidth() - paddingSidePx * 2) / (count - 1);

                for (int i = 0; i < count; i++) {
                    float x = (count == 1) ? (getWidth() / 2f) : (paddingSidePx + i * stepX);
                    float dist = Math.abs(x - touchX);
                    if (dist < minDistance && dist < dpToPx(35)) {
                        minDistance = dist;
                        closestIndex = i;
                    }
                }

                if (closestIndex != -1) {
                    selectedIndex = (closestIndex == selectedIndex) ? -1 : closestIndex;
                    invalidate();
                    performClick();
                }
            }
            android.view.ViewParent parent = getParent();
            while (parent != null) {
                if (parent instanceof androidx.core.widget.NestedScrollView) {
                    parent.requestDisallowInterceptTouchEvent(false);
                    break;
                }
                parent = parent.getParent();
            }
            return true;
        } else if (action == MotionEvent.ACTION_CANCEL) {
            android.view.ViewParent parent = getParent();
            while (parent != null) {
                if (parent instanceof androidx.core.widget.NestedScrollView) {
                    parent.requestDisallowInterceptTouchEvent(false);
                    break;
                }
                parent = parent.getParent();
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private int dpToPx(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int spToPx(float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }
}
