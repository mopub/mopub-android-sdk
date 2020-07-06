// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import org.jetbrains.annotations.NotNull;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;

public abstract class AdAdapter implements AdLifecycleListener.LoadListener, AdLifecycleListener.InteractionListener {

    protected final Handler mHandler;
    protected final Runnable mTimeout;

    @Nullable
    protected BaseAd mBaseAd;
    protected Context mContext;
    protected boolean mInvalidated;
    protected AdData mAdData;

    protected boolean mIsReady = false;

    @Nullable
    protected AdLifecycleListener.LoadListener mLoadListener;
    @Nullable
    protected AdLifecycleListener.InteractionListener mInteractionListener;

    abstract void show(@Nullable MoPubAd moPubAd);

    abstract void doInvalidate();

    public AdAdapter(@NonNull final Context context,
                     @NonNull final String className,
                     @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(className);
        Preconditions.checkNotNull(adData);

        mContext = context;
        mHandler = new Handler();
        mAdData = adData;

        mTimeout = () -> {
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "AdAdapter() failed", NETWORK_TIMEOUT);
            onAdLoadFailed(NETWORK_TIMEOUT);
            invalidate();
        };
    }

    public final void load(@NonNull final AdLifecycleListener.LoadListener loadListener) {
        Preconditions.checkNotNull(loadListener);

        MoPubLog.log(LOAD_ATTEMPTED);

        if (isInvalidated() || mBaseAd == null) {
            return;
        }

        mLoadListener = loadListener;

        mHandler.postDelayed(mTimeout, getTimeoutDelayMilliseconds());

        // Base ad classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mBaseAd.internalLoad(mContext, this, mAdData);
        } catch (Exception e) {
            MoPubLog.log(LOAD_FAILED,
                    MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                    MoPubErrorCode.INTERNAL_ERROR);
            onAdLoadFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    void invalidate() {
        doInvalidate();

        mBaseAd = null;
        mContext = null;
        mAdData = null;
        mLoadListener = null;
        mInteractionListener = null;

        mInvalidated = true;
        mIsReady = false;
    }

    public boolean isReady() {
        return mIsReady;
    }

    private int getTimeoutDelayMilliseconds() {
        return mAdData.getTimeoutDelayMillis();
    }

    @Nullable
    public String getBaseAdClassName() {
        return mBaseAd != null ? mBaseAd.getClass().getName() : null;
    }

    @VisibleForTesting
    protected void setLoadListener(@NonNull final AdLifecycleListener.LoadListener loadListener) {
        Preconditions.checkNotNull(loadListener);

        mLoadListener = loadListener;
    }

    protected void setInteractionListener(@NonNull final AdLifecycleListener.InteractionListener interactionListener) {
        Preconditions.checkNotNull(interactionListener);

        mInteractionListener = interactionListener;
    }

    boolean isInvalidated() {
        return mInvalidated;
    }

    private void cancelTimeout() {
        mHandler.removeCallbacks(mTimeout);
    }

    protected String getAdNetworkId() {
        return mBaseAd != null ? mBaseAd.getAdNetworkId() : "";
    }

    protected boolean isAutomaticImpressionAndClickTrackingEnabled() {
        final BaseAd baseAd = mBaseAd;
        if (baseAd == null) {
            return true;
        }

        return baseAd.isAutomaticImpressionAndClickTrackingEnabled();
    }

    @Override
    public void onAdLoaded() {
        if (isInvalidated()) {
            return;
        }

        MoPubLog.log(LOAD_SUCCESS);
        mIsReady = true;
        cancelTimeout();

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
        }
    }

    @Override
    public void onAdLoadFailed(@NotNull MoPubErrorCode errorCode) {
        Preconditions.checkNotNull(errorCode);
        if (isInvalidated()) {
            return;
        }

        cancelTimeout();

        if (mLoadListener != null) {
            mLoadListener.onAdLoadFailed(errorCode);
        }
    }

    @Override
    public void onAdFailed(@NotNull MoPubErrorCode errorCode) {
        Preconditions.checkNotNull(errorCode);

        if (isInvalidated()) {
            return;
        }

        cancelTimeout();

        if (mInteractionListener != null) {
            mInteractionListener.onAdFailed(errorCode);
        }
    }

    @Override
    public void onAdShown() {
        if (isInvalidated()) {
            return;
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdShown();
        }

        final BaseAd baseAd = mBaseAd;
        if (baseAd == null || baseAd.isAutomaticImpressionAndClickTrackingEnabled()) {
            if (mInteractionListener != null) {
                mInteractionListener.onAdImpression();
            }

            if (baseAd != null) {
                baseAd.trackMpxAndThirdPartyImpressions();
            }
        }
    }

    @Override
    public void onAdClicked() {
        if (isInvalidated()) {
            return;
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdClicked();
        }
    }

    @Override
    public void onAdImpression() {
        if (isInvalidated()) {
            return;
        }

        final BaseAd baseAd = mBaseAd;
        if (baseAd != null && !baseAd.isAutomaticImpressionAndClickTrackingEnabled()) {
            if (mInteractionListener != null) {
                mInteractionListener.onAdImpression();
            }
            baseAd.trackMpxAndThirdPartyImpressions();
        }
    }

    @Override
    public void onAdDismissed() {
        if (isInvalidated()) {
            return;
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdDismissed();
        }
    }

    @Override
    public void onAdComplete(@Nullable final MoPubReward moPubReward) {
        if (isInvalidated()) {
            return;
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdComplete(moPubReward);
        }
    }

    @Override
    public void onAdResumeAutoRefresh() {
        if (mInteractionListener != null) {
            mInteractionListener.onAdResumeAutoRefresh();
        }
    }

    @Override
    public void onAdPauseAutoRefresh() {
        if (mInteractionListener != null) {
            mInteractionListener.onAdPauseAutoRefresh();
        }
    }

    @Override
    public void onAdExpanded() {
        if (isInvalidated()) {
            return;
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdExpanded();
        }
    }

    @Override
    public void onAdCollapsed() {
        if (isInvalidated()) {
            return;
        }

        if (mInteractionListener != null) {
            mInteractionListener.onAdCollapsed();
        }
    }

    public static class BaseAdNotFoundException extends Exception {
        String message;
        Throwable cause;

        public BaseAdNotFoundException(Exception exception) {
            this.message = exception.getMessage();
            this.cause = exception.getCause();
            MoPubLog.log(CUSTOM_WITH_THROWABLE, "AdAdapter.create() " +
                    "failed with exception", exception);
        }
    }
}
