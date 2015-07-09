package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.mobileads.resource.ProgressBarDrawable;

public class VastVideoProgressBarWidget extends ImageView {
    @NonNull private ProgressBarDrawable mProgressBarDrawable;

    public VastVideoProgressBarWidget(@NonNull final Context context, final int anchorId) {
        super(context);

        setId((int) Utils.generateUniqueId());

        mProgressBarDrawable = new ProgressBarDrawable(context);
        setImageDrawable(mProgressBarDrawable);

        final int progressBarHeight
                = Dips.dipsToIntPixels(DrawableConstants.ProgressBar.HEIGHT_DIPS, context);

        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                progressBarHeight);

        layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, anchorId);

        setLayoutParams(layoutParams);
    }

    void calibrateAndMakeVisible(final int duration, final int skipOffset) {
        mProgressBarDrawable.setDurationAndSkipOffset(duration, skipOffset);
        setVisibility(View.VISIBLE);
    }

    void updateProgress(final int progress) {
        mProgressBarDrawable.setProgress(progress);
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    ProgressBarDrawable getImageViewDrawable() {
        return mProgressBarDrawable;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setImageViewDrawable(@NonNull ProgressBarDrawable drawable) {
        mProgressBarDrawable = drawable;
    }
}
