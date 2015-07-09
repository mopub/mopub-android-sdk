package com.mopub.mobileads.resource;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class CloseButtonDrawable extends BaseWidgetDrawable {
    private final Paint closeButtonPaint;

    public CloseButtonDrawable() {
        super();

        closeButtonPaint = new Paint();
        closeButtonPaint.setColor(DrawableConstants.CloseButton.STROKE_COLOR);
        closeButtonPaint.setStrokeWidth(DrawableConstants.CloseButton.STROKE_WIDTH);
        closeButtonPaint.setStrokeCap(DrawableConstants.CloseButton.STROKE_CAP);
    }

    @Override
    public void draw(final Canvas canvas) {
        final int w = getBounds().width();
        final int h = getBounds().height();
        canvas.drawLine(0, h, w, 0, closeButtonPaint);
        canvas.drawLine(0, 0, w, h, closeButtonPaint);
    }
}
