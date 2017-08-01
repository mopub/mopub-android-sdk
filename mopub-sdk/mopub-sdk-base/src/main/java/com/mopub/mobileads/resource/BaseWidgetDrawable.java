package com.mopub.mobileads.resource;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public abstract class BaseWidgetDrawable extends Drawable {
    protected void drawTextWithinBounds(@NonNull final Canvas canvas,
            @NonNull final Paint textPaint, @NonNull final Rect textRect,
            @NonNull final String text) {
        textPaint.getTextBounds(text, 0, text.length(), textRect);
        final float textHeight = textPaint.descent() - textPaint.ascent();
        final float textOffset = (textHeight / 2) - textPaint.descent();
        canvas.drawText(text, getBounds().centerX(), getBounds().centerY() + textOffset, textPaint);
    }

    @Override
    public void setAlpha(int i) {

    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.UNKNOWN;
    }
}
