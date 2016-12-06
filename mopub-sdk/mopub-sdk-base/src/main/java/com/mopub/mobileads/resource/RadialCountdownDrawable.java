package com.mopub.mobileads.resource;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Numbers;

public class RadialCountdownDrawable extends BaseWidgetDrawable {
    @NonNull private final Paint mCirclePaint;
    @NonNull private final Paint mArcPaint;
    @NonNull private final Paint mTextPaint;
    @NonNull private Rect mTextRect;

    private int mInitialCountdownMilliseconds;
    private int mSecondsRemaining;
    private float mSweepAngle;

    public RadialCountdownDrawable(@NonNull final Context context) {
        final int circleStrokeWidth = Dips.dipsToIntPixels(
                DrawableConstants.RadialCountdown.CIRCLE_STROKE_WIDTH_DIPS, context);
        final float textSizePixels = Dips.dipsToFloatPixels(
                DrawableConstants.RadialCountdown.TEXT_SIZE_SP, context);

        // Unfilled progress
        mCirclePaint = new Paint();
        mCirclePaint.setColor(DrawableConstants.RadialCountdown.BACKGROUND_COLOR);
        mCirclePaint.setAlpha(DrawableConstants.RadialCountdown.BACKGROUND_ALPHA);
        mCirclePaint.setStyle(DrawableConstants.RadialCountdown.BACKGROUND_STYLE);
        mCirclePaint.setStrokeWidth(circleStrokeWidth);
        mCirclePaint.setAntiAlias(true);

        // Filled progress
        mArcPaint = new Paint();
        mArcPaint.setColor(DrawableConstants.RadialCountdown.PROGRESS_COLOR);
        mArcPaint.setAlpha(DrawableConstants.RadialCountdown.PROGRESS_ALPHA);
        mArcPaint.setStyle(DrawableConstants.RadialCountdown.PROGRESS_STYLE);
        mArcPaint.setStrokeWidth(circleStrokeWidth);
        mArcPaint.setAntiAlias(true);

        // Countdown number text
        mTextPaint = new Paint();
        mTextPaint.setColor(DrawableConstants.RadialCountdown.TEXT_COLOR);
        mTextPaint.setTextAlign(DrawableConstants.RadialCountdown.TEXT_ALIGN);
        mTextPaint.setTextSize(textSizePixels);
        mTextPaint.setAntiAlias(true);

        mTextRect = new Rect();
    }

    @Override
    public void draw(final Canvas canvas) {
        final int centerX = getBounds().centerX();
        final int centerY = getBounds().centerY();
        final int radius = Math.min(centerX, centerY);

        canvas.drawCircle(centerX, centerY, radius, mCirclePaint);

        final String secondsRemainingText = String.valueOf(mSecondsRemaining);
        drawTextWithinBounds(canvas, mTextPaint, mTextRect, secondsRemainingText);

        final RectF circle = new RectF(getBounds());
        canvas.drawArc(circle, DrawableConstants.RadialCountdown.START_ANGLE, mSweepAngle, false, mArcPaint);
    }

    public void setInitialCountdown(final int initialCountdownMilliseconds) {
        mInitialCountdownMilliseconds = initialCountdownMilliseconds;
    }

    public void updateCountdownProgress(final int currentProgressMilliseconds) {
        int remainingCountdownMilliseconds = mInitialCountdownMilliseconds - currentProgressMilliseconds;
        mSecondsRemaining = (int) Numbers.convertMillisecondsToSecondsRoundedUp(remainingCountdownMilliseconds);
        mSweepAngle = 360f * currentProgressMilliseconds / mInitialCountdownMilliseconds;
        invalidateSelf();
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    public int getInitialCountdownMilliseconds() {
        return mInitialCountdownMilliseconds;
    }
}
