package com.example.musebox.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.musebox.R;

public class CircularProgressView extends View {

    private Paint trackPaint;
    private Paint progressPaint;
    private float progress = 0f; // 0–100
    private float strokeWidth = 6f;
    private int trackColor;
    private int progressColor;

    private RectF circleBounds;

    public CircularProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    private void init(@Nullable AttributeSet attrs) {
        // Default colors
        trackColor = getResources().getColor(R.color.gray);
        progressColor = getResources().getColor(R.color.white);

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.CircularProgressView);
            progress = a.getFloat(R.styleable.CircularProgressView_progress, 0f);
            strokeWidth = a.getDimension(R.styleable.CircularProgressView_strokeWidth, 6f);
            trackColor = a.getColor(R.styleable.CircularProgressView_trackColor, trackColor);
            progressColor = a.getColor(R.styleable.CircularProgressView_progressColor, progressColor);
            a.recycle();
        }

        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(trackColor);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setColor(progressColor);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        circleBounds = new RectF();
    }

    public void setProgress(float value) {
        progress = Math.max(0, Math.min(value, 100));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float halfStroke = strokeWidth / 2f;
        circleBounds.set(halfStroke, halfStroke, getWidth() - halfStroke, getHeight() - halfStroke);

        // Draw background track
        canvas.drawArc(circleBounds, 0, 360, false, trackPaint);

        // Draw progress arc
        float sweep = (progress / 100f) * 360f;
        canvas.drawArc(circleBounds, -90, sweep, false, progressPaint);
    }

}