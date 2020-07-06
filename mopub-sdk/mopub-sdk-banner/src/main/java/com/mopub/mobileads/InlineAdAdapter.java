// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.BaseAdFactory;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_FAILED;
import static com.mopub.mobileads.MoPubErrorCode.INLINE_SHOW_ERROR;

public class InlineAdAdapter extends AdAdapter {
    private int mImpressionMinVisibleDips = Integer.MIN_VALUE;
    private int mImpressionMinVisibleMs = Integer.MIN_VALUE;
    @Nullable
    protected InlineVisibilityTracker mVisibilityTracker;

    public InlineAdAdapter(@NonNull Context context,
                           @NonNull String className,
                           @NonNull AdData adData) throws BaseAdNotFoundException {
        super(context, className, adData);

        MoPubLog.log(CUSTOM, "Attempting to invoke base ad: " + className);
        try {
            mBaseAd = BaseAdFactory.create(className);
        } catch (Exception exception) {
            throw new BaseAdNotFoundException(exception);
        }

        // Parse banner impression tracking headers to determine if we are in visibility experiment
        parseBannerImpressionTrackingHeaders();
    }

    @Override
    final void show(@Nullable MoPubAd moPubAd) {
        MoPubLog.log(SHOW_ATTEMPTED);

        final BaseAd baseAd = mBaseAd;
        if (isInvalidated() || baseAd == null) {
            return;
        }

        // If visibility impression tracking is enabled for banners, fire all impression
        // tracking URLs (AdServer, MPX, 3rd-party) for both HTML and MRAID banner types when
        // visibility conditions are met.
        //
        // Else, retain old behavior of firing AdServer impression tracking URL if and only if
        // banner is not HTML.
        if (!(moPubAd instanceof MoPubView) || baseAd.getAdView() == null) {
            // There shouldn't be non-MoPubView MoPubAds here
            MoPubLog.log(SHOW_FAILED, INLINE_SHOW_ERROR);
            onAdFailed(INLINE_SHOW_ERROR);
            return;
        }

        final MoPubView moPubView = (MoPubView) moPubAd;
        final View baseAdView = baseAd.getAdView();

        if (isAutomaticImpressionAndClickTrackingEnabled()) {
            // Disable autoRefresh temporarily until an impression happens.
            onAdPauseAutoRefresh();

            mVisibilityTracker = new InlineVisibilityTracker(mContext, moPubView, baseAdView,
                    mImpressionMinVisibleDips, mImpressionMinVisibleMs);
            mVisibilityTracker.setInlineVisibilityTrackerListener(
                    () -> {
                        baseAd.trackMpxAndThirdPartyImpressions();
                        onAdShown();
                        onAdResumeAutoRefresh();
                    });
        }

        baseAd.internalShow(this); // sets `this` as the interaction listener

        final View adView = baseAd.getAdView();
        if (adView != null) {
            moPubAd.setAdContentView(adView);
        }
    }

    void doInvalidate() {
        if (mBaseAd != null) {
            // Base ad classes can be developed by any third party and may not be tested.
            // We catch all exceptions here to prevent crashes from untested code.
            try {
                mBaseAd.onInvalidate();
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE,
                        "Invalidating a base ad banner threw an exception", e);
            }
        }

        if (mVisibilityTracker != null) {
            try {
                mVisibilityTracker.destroy();
            } catch (Exception e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE,
                        "Destroying a banner visibility tracker threw an exception", e);
            }
            mVisibilityTracker = null;
        }
    }

    @Deprecated
    @VisibleForTesting
    int getImpressionMinVisibleDips() {
        return mImpressionMinVisibleDips;
    }

    @Deprecated
    @VisibleForTesting
    int getImpressionMinVisibleMs() {
        return mImpressionMinVisibleMs;
    }

    @Deprecated
    @VisibleForTesting
    void setImpressionMinVisibleDips(final int impressionMinVisibleDips) {
        mImpressionMinVisibleDips = impressionMinVisibleDips;
    }

    @Deprecated
    @VisibleForTesting
    void setImpressionMinVisibleMs(final int impressionMinVisibleMs) {
        mImpressionMinVisibleMs = impressionMinVisibleMs;
    }

    @Nullable
    @Deprecated
    @VisibleForTesting
    InlineVisibilityTracker getVisibilityTracker() {
        return mVisibilityTracker;
    }

    @VisibleForTesting
    void parseBannerImpressionTrackingHeaders() {
        final String impressionMinVisibleDipsString =
                mAdData.getImpressionMinVisibleDips();
        final String impressionMinVisibleMsString =
                mAdData.getImpressionMinVisibleMs();

        if (!TextUtils.isEmpty(impressionMinVisibleDipsString)
                && !TextUtils.isEmpty(impressionMinVisibleMsString)) {
            try {
                mImpressionMinVisibleDips = Integer.parseInt(impressionMinVisibleDipsString);
            } catch (NumberFormatException e) {
                MoPubLog.log(CUSTOM, "Cannot parse integer from header "
                        + DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_DIPS);
            }

            try {
                mImpressionMinVisibleMs = Integer.parseInt(impressionMinVisibleMsString);
            } catch (NumberFormatException e) {
                MoPubLog.log(CUSTOM, "Cannot parse integer from header "
                        + DataKeys.BANNER_IMPRESSION_MIN_VISIBLE_MS);
            }
        }
    }

    @Override
    public void onAdDismissed() { /* NO-OP */ }

    @Override
    public void onAdComplete(@Nullable final MoPubReward moPubReward) { /* NO-OP */ }
}
