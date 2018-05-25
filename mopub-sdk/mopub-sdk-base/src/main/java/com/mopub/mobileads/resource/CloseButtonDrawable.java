package com.mopub.mobileads.resource;

import android.graphics.Canvas;
import android.graphics.Paint;

public class CloseButtonDrawable extends BaseWidgetDrawable {
    private final Paint closeButtonPaint;
    /**
     * Used to ensure that the rounded edges of the X stay in the bounds of the drawable
     */
    private final float halfStrokeWidth;

    public CloseButtonDrawable() {
        this(DrawableConstants.CloseButton.STROKE_WIDTH);
    }

    public CloseButtonDrawable(float strokeWidth) {
        super();

        halfStrokeWidth = strokeWidth / 2;
        closeButtonPaint = new Paint();
        closeButtonPaint.setColor(DrawableConstants.CloseButton.STROKE_COLOR);
        closeButtonPaint.setStrokeWidth(strokeWidth);
        closeButtonPaint.setStrokeCap(DrawableConstants.CloseButton.STROKE_CAP);
    }

    @Override
    public void draw(final Canvas canvas) {
        final int w = getBounds().width();
        final int h = getBounds().height();
        canvas.drawLine(0+halfStrokeWidth, h-halfStrokeWidth,
                w-halfStrokeWidth, 0+halfStrokeWidth, closeButtonPaint);
        canvas.drawLine(0+halfStrokeWidth, 0+halfStrokeWidth,
                w-halfStrokeWidth, h-halfStrokeWidth, closeButtonPaint);
    }
}
