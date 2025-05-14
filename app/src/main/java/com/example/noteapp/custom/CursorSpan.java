package com.example.noteapp.custom;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CursorSpan extends ReplacementSpan {
    private final int color;

    public CursorSpan(int color) {
        this.color = color;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text,
                       int start, int end, @Nullable Paint.FontMetricsInt fm) {
        return 0; // Không chiếm không gian
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text,
                     int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        Paint cursorPaint = new Paint(paint);
        cursorPaint.setColor(color);
        cursorPaint.setStrokeWidth(2);
        canvas.drawLine(x, top, x, bottom, cursorPaint);
    }
}
