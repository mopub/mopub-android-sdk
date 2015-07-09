package com.mopub.mobileads.resource;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;

public class CtaButtonDrawable extends BaseWidgetDrawable {
    @NonNull private final Paint mBackgroundPaint;
    @NonNull private final Paint mOutlinePaint;
    @NonNull private final Paint mTextPaint;
    @NonNull private final RectF mButtonRect;
    @NonNull private final Rect mTextRect;

    private final int mButtonCornerRadius;
    private String mCtaText;

    public CtaButtonDrawable(@NonNull final Context context) {
        super();

        final int outlineStrokeWidth = Dips.dipsToIntPixels(
                DrawableConstants.CtaButton.OUTLINE_STROKE_WIDTH_DIPS, context);
        final float textSize = Dips.dipsToFloatPixels(
                DrawableConstants.CtaButton.TEXT_SIZE_SP, context);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(DrawableConstants.CtaButton.BACKGROUND_COLOR);
        mBackgroundPaint.setAlpha(DrawableConstants.CtaButton.BACKGROUND_ALPHA);
        mBackgroundPaint.setStyle(DrawableConstants.CtaButton.BACKGROUND_STYLE);
        mBackgroundPaint.setAntiAlias(true);

        mOutlinePaint = new Paint();
        mOutlinePaint.setColor(DrawableConstants.CtaButton.OUTLINE_COLOR);
        mOutlinePaint.setAlpha(DrawableConstants.CtaButton.OUTLINE_ALPHA);
        mOutlinePaint.setStyle(DrawableConstants.CtaButton.OUTLINE_STYLE);
        mOutlinePaint.setStrokeWidth(outlineStrokeWidth);
        mOutlinePaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setColor(DrawableConstants.CtaButton.TEXT_COLOR);
        mTextPaint.setTextAlign(DrawableConstants.CtaButton.TEXT_ALIGN);
        mTextPaint.setTypeface(DrawableConstants.CtaButton.TEXT_TYPEFACE);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setAntiAlias(true);

        mTextRect = new Rect();
        mCtaText = DrawableConstants.CtaButton.DEFAULT_CTA_TEXT;

        mButtonRect = new RectF();
        mButtonCornerRadius = Dips.dipsToIntPixels(DrawableConstants.CtaButton.CORNER_RADIUS_DIPS, context);
    }

    @Override
    public void draw(final Canvas canvas) {
        mButtonRect.set(getBounds());

        // Rounded rectangle background fill
        canvas.drawRoundRect(mButtonRect, mButtonCornerRadius, mButtonCornerRadius, mBackgroundPaint);

        // Rounded rectangle outline
        canvas.drawRoundRect(mButtonRect, mButtonCornerRadius, mButtonCornerRadius, mOutlinePaint);

        // CTA text
        drawTextWithinBounds(canvas, mTextPaint, mTextRect, mCtaText);
    }

    public void setCtaText(@NonNull final String ctaText) {
        mCtaText = ctaText;
        invalidateSelf();
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    public String getCtaText() {
        return mCtaText;
    }
}
