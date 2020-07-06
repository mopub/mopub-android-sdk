// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.content.res.Configuration;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.mobileads.resource.CtaButtonDrawable;
import com.mopub.mobileads.resource.DrawableConstants;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

public class VastVideoCtaButtonWidget extends ImageView {
    @NonNull private CtaButtonDrawable mCtaButtonDrawable;
    @NonNull private final RelativeLayout.LayoutParams mLandscapeLayoutParams;
    @NonNull private final RelativeLayout.LayoutParams mPortraitLayoutParams;

    private boolean mIsVideoSkippable;
    private boolean mIsVideoComplete;
    private boolean mHasCompanionAd;
    private boolean mHasClickthroughUrl;

    public VastVideoCtaButtonWidget(@NonNull final Context context, final int videoViewId,
            final boolean hasCompanionAd, final boolean hasClickthroughUrl) {
        super(context);

        mHasCompanionAd = hasCompanionAd;
        mHasClickthroughUrl = hasClickthroughUrl;

        setId(View.generateViewId());

        final int width = Dips.dipsToIntPixels(DrawableConstants.CtaButton.WIDTH_DIPS, context);
        final int height = Dips.dipsToIntPixels(DrawableConstants.CtaButton.HEIGHT_DIPS, context);
        final int margin = Dips.dipsToIntPixels(DrawableConstants.CtaButton.MARGIN_DIPS, context);

        mCtaButtonDrawable = new CtaButtonDrawable(context);
        setImageDrawable(mCtaButtonDrawable);

        // landscape layout: placed bottom-right corner of video view
        mLandscapeLayoutParams = new RelativeLayout.LayoutParams(width, height);
        mLandscapeLayoutParams.setMargins(margin, margin, margin, margin);
        mLandscapeLayoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, videoViewId);
        mLandscapeLayoutParams.addRule(RelativeLayout.ALIGN_RIGHT, videoViewId);

        // portrait layout: placed bottom-right corner of screen
        mPortraitLayoutParams = new RelativeLayout.LayoutParams(width, height);
        mPortraitLayoutParams.setMargins(margin, margin, margin, margin);
        mPortraitLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mPortraitLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        updateLayoutAndVisibility();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        updateLayoutAndVisibility();
    }

    void updateCtaText(@NonNull final String customCtaText) {
        mCtaButtonDrawable.setCtaText(customCtaText);
    }

    void notifyVideoSkippable() {
        mIsVideoSkippable = true;
        updateLayoutAndVisibility();
    }

    void notifyVideoComplete() {
        mIsVideoSkippable = true;
        mIsVideoComplete = true;
        updateLayoutAndVisibility();
    }

    void notifyVideoClickable() {
        mIsVideoComplete = true;
        updateLayoutAndVisibility();
    }

    private void updateLayoutAndVisibility() {
        // If the video does not have a clickthrough url, never show the CTA button
        if (!mHasClickthroughUrl) {
            setVisibility(View.GONE);
            return;
        }

        // If video is not skippable yet, do not show CTA button
        if (!mIsVideoSkippable) {
            setVisibility(View.INVISIBLE);
            return;
        }

        // If video has finished playing and there's a companion ad, do not show CTA button
        if (mIsVideoComplete && mHasCompanionAd) {
            setVisibility(View.GONE);
            return;
        }

        final int currentOrientation = getResources().getConfiguration().orientation;

        switch (currentOrientation) {
            case Configuration.ORIENTATION_LANDSCAPE:
                setLayoutParams(mLandscapeLayoutParams);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                setLayoutParams(mPortraitLayoutParams);
                break;
            case Configuration.ORIENTATION_UNDEFINED:
                MoPubLog.log(CUSTOM, "Screen orientation undefined: CTA button widget defaulting to portrait layout");
                setLayoutParams(mPortraitLayoutParams);
                break;
            case Configuration.ORIENTATION_SQUARE:
                MoPubLog.log(CUSTOM, "Screen orientation is deprecated ORIENTATION_SQUARE: CTA button widget defaulting to portrait layout");
                setLayoutParams(mPortraitLayoutParams);
                break;
            default:
                MoPubLog.log(CUSTOM, "Unrecognized screen orientation: CTA button widget defaulting to portrait layout");
                setLayoutParams(mPortraitLayoutParams);
                break;
        }
        setVisibility(View.VISIBLE);
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    String getCtaText() {
        return mCtaButtonDrawable.getCtaText();
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    boolean hasPortraitLayoutParams() {
        return getLayoutParams().equals(mPortraitLayoutParams);
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    boolean hasLandscapeLayoutParams() {
        return getLayoutParams().equals(mLandscapeLayoutParams);
    }
}
