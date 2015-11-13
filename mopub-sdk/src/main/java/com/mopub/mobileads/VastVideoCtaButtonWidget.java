package com.mopub.mobileads;

import android.content.Context;
import android.content.res.Configuration;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.resource.CtaButtonDrawable;
import com.mopub.mobileads.resource.DrawableConstants;

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

        setId((int) Utils.generateUniqueId());

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

        // portrait layout: placed center below video view
        mPortraitLayoutParams = new RelativeLayout.LayoutParams(width, height);
        mPortraitLayoutParams.setMargins(margin, margin, margin, margin);
        mPortraitLayoutParams.addRule(RelativeLayout.BELOW, videoViewId);
        mPortraitLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

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
                setVisibility(View.VISIBLE);
                setLayoutParams(mLandscapeLayoutParams);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                setVisibility(View.VISIBLE);
                setLayoutParams(mPortraitLayoutParams);
                break;
            case Configuration.ORIENTATION_UNDEFINED:
                MoPubLog.d("Screen orientation undefined: CTA button widget defaulting to portrait layout");
                setVisibility(View.VISIBLE);
                setLayoutParams(mPortraitLayoutParams);
                break;
            case Configuration.ORIENTATION_SQUARE:
                MoPubLog.d("Screen orientation is deprecated ORIENTATION_SQUARE: CTA button widget defaulting to portrait layout");
                setVisibility(View.VISIBLE);
                setLayoutParams(mPortraitLayoutParams);
                break;
            default:
                MoPubLog.d("Unrecognized screen orientation: CTA button widget defaulting to portrait layout");
                setVisibility(View.VISIBLE);
                setLayoutParams(mPortraitLayoutParams);
                break;
        }
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
