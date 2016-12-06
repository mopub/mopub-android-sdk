package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.mobileads.resource.RadialCountdownDrawable;

public class VastVideoRadialCountdownWidget extends ImageView {
    @NonNull private RadialCountdownDrawable mRadialCountdownDrawable;
    private int mLastProgressMilliseconds;

    public VastVideoRadialCountdownWidget(@NonNull final Context context) {
        super(context);

        setId((int) Utils.generateUniqueId());

        final int sideLength = Dips.dipsToIntPixels(DrawableConstants.RadialCountdown.SIDE_LENGTH_DIPS, context);
        final int topMargin = Dips.dipsToIntPixels(DrawableConstants.RadialCountdown.TOP_MARGIN_DIPS, context);
        final int rightMargin = Dips.dipsToIntPixels(DrawableConstants.RadialCountdown.RIGHT_MARGIN_DIPS, context);
        final int padding = Dips.dipsToIntPixels(DrawableConstants.RadialCountdown.PADDING_DIPS, context);

        mRadialCountdownDrawable = new RadialCountdownDrawable(context);
        setImageDrawable(mRadialCountdownDrawable);
        setPadding(padding, padding, padding, padding);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                sideLength,
                sideLength);
        layoutParams.setMargins(0, topMargin, rightMargin, 0);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP | RelativeLayout.ALIGN_PARENT_RIGHT);
        setLayoutParams(layoutParams);
    }

    void calibrateAndMakeVisible(final int initialCountdownMilliseconds) {
        mRadialCountdownDrawable.setInitialCountdown(initialCountdownMilliseconds);
        setVisibility(VISIBLE);
    }

    void updateCountdownProgress(final int initialCountdownMilliseconds, final int currentProgressMilliseconds) {
        // There exists an Android video player bug where VideoView.getCurrentPosition()
        // temporarily returns 0 right after backgrounding and switching back to the app.
        // Therefore, we check against the last known current position to ensure that it's
        // monotonically increasing.
        if (currentProgressMilliseconds >= mLastProgressMilliseconds) {
            int millisecondsUntilSkippable = initialCountdownMilliseconds - currentProgressMilliseconds;

            // XXX
            // After backgrounding and switching back to the app,
            // this widget becomes erroneously visible.
            if (millisecondsUntilSkippable < 0) {
                setVisibility(GONE);
            } else {
                mRadialCountdownDrawable.updateCountdownProgress(currentProgressMilliseconds);
                mLastProgressMilliseconds = currentProgressMilliseconds;
            }
        }
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    RadialCountdownDrawable getImageViewDrawable() {
        return mRadialCountdownDrawable;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setImageViewDrawable(RadialCountdownDrawable drawable) {
        mRadialCountdownDrawable = drawable;
    }
}
