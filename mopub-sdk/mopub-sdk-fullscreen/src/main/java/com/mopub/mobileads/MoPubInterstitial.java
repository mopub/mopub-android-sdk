// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.view.Window;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.mobileads.factories.AdViewControllerFactory;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.DID_DISAPPEAR;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_SUCCESS;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.DESTROYED;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.IDLE;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.LOADING;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.READY;
import static com.mopub.mobileads.MoPubInterstitial.InterstitialState.SHOWING;

public class MoPubInterstitial implements MoPubAd {
    @VisibleForTesting
    enum InterstitialState {
        /**
         * Waiting to something to happen. There is no interstitial currently loaded.
         */
        IDLE,

        /**
         * Loading an interstitial.
         */
        LOADING,

        /**
         * Loaded and ready to be shown.
         */
        READY,

        /**
         * The interstitial is showing.
         */
        SHOWING,

        /**
         * No longer able to accept events as the internal InterstitialView has been destroyed.
         */
        DESTROYED
    }

    @Nullable protected AdViewController mAdViewController;
    @Nullable private FullscreenAdAdapter mFullscreenAdapter;
    @Nullable private InterstitialAdListener mInterstitialAdListener;
    @NonNull private Activity mActivity;
    @NonNull private Handler mHandler;
    @NonNull private volatile InterstitialState mCurrentInterstitialState;

    public interface InterstitialAdListener {
        void onInterstitialLoaded(MoPubInterstitial interstitial);
        void onInterstitialFailed(MoPubInterstitial interstitial, MoPubErrorCode errorCode);
        void onInterstitialShown(MoPubInterstitial interstitial);
        void onInterstitialClicked(MoPubInterstitial interstitial);
        void onInterstitialDismissed(MoPubInterstitial interstitial);
    }

    public MoPubInterstitial(@NonNull final Activity activity, @NonNull final String adUnitId) {
        mActivity = activity;

        final AdViewController adViewController = AdViewControllerFactory.create(mActivity, this);
        adViewController.setShouldAllowAutoRefresh(false);
        setAdViewController(adViewController);
        setAdUnitId(adUnitId);

        mCurrentInterstitialState = IDLE;

        mHandler = new Handler();
    }

    private boolean attemptStateTransition(@NonNull final InterstitialState endState) {
        return attemptStateTransition(endState, false);
    }

    /**
     * Attempts to transition to the new state. All state transitions should go through this method.
     * Other methods should not be modifying mCurrentInterstitialState.
     *
     * @param endState     The desired end state.
     * @param force Whether or not this is part of a force transition. Force transitions
     *                     can happen from IDLE, LOADING, or READY. It will ignore
     *                     the currently loading or loaded ad and attempt to load another.
     * @return {@code true} if a state change happened, {@code false} if no state change happened.
     */
    @VisibleForTesting
    synchronized boolean attemptStateTransition(@NonNull final InterstitialState endState,
            boolean force) {
        Preconditions.checkNotNull(endState);

        final InterstitialState startState = mCurrentInterstitialState;

        /**
         * There are 50 potential cases. Any combination that is a no op will not be enumerated
         * and returns false. The usual case goes IDLE -> LOADING -> READY -> SHOWING -> IDLE. At
         * most points, having the force refresh flag into IDLE resets MoPubInterstitial and clears
         * the interstitial adapter. This cannot happen while an interstitial is showing. Also,
         * MoPubInterstitial can be destroyed arbitrarily, and once this is destroyed, it no longer
         * can perform any state transitions.
         */
        switch (startState) {
            case IDLE:
                switch(endState) {
                    case LOADING:
                        // Going from IDLE to LOADING is the usual load case
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = LOADING;
                        updatedInsets();
                        if (force) {
                            // Force-load means a pub-initiated force refresh.
                            if (mAdViewController != null) {
                                mAdViewController.forceRefresh();
                            }
                        } else {
                            // Otherwise, do a normal load
                            loadAd();
                        }
                        return true;
                    case READY:
                        MoPubLog.log(CUSTOM, "Attempted transition from IDLE to " +
                                "READY failed due to no known load call.");
                        return false;
                    case SHOWING:
                        MoPubLog.log(CUSTOM, "No interstitial loading or loaded.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case LOADING:
                switch (endState) {
                    case IDLE:
                        // Being forced back into idle while loading resets MoPubInterstitial while
                        // not forced just means the load failed. Either way, it should reset the
                        // state back into IDLE.
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = IDLE;
                        return true;
                    case LOADING:
                        if (!force) {
                            // Cannot load more than one interstitial at a time
                            MoPubLog.log(CUSTOM, "Already loading an interstitial.");
                        }
                        return false;
                    case READY:
                        // This is the usual load finished transition
                        MoPubLog.log(LOAD_SUCCESS);
                        mCurrentInterstitialState = READY;
                        if (mInterstitialAdListener != null) {
                            mInterstitialAdListener.onInterstitialLoaded(this);
                        }
                        return true;
                    case SHOWING:
                        MoPubLog.log(CUSTOM, "Interstitial is not ready to be shown yet.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case READY:
                switch (endState) {
                    case IDLE:
                        if (force) {
                            // This happens on a force refresh or an ad expiration
                            invalidateInterstitialAdapter();
                            mCurrentInterstitialState = IDLE;
                            return true;
                        }
                        return false;
                    case LOADING:
                        // This is to prevent loading another interstitial while one is loaded.
                        MoPubLog.log(CUSTOM, "Interstitial already loaded. Not loading another.");
                        // Let the ad listener know that there's already an ad loaded
                        if (mInterstitialAdListener != null) {
                            mInterstitialAdListener.onInterstitialLoaded(this);
                        }
                        return false;
                    case SHOWING:
                        // This is the usual transition from ready to showing
                        showFullscreen();
                        mCurrentInterstitialState = SHOWING;
                        return true;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case SHOWING:
                switch(endState) {
                    case IDLE:
                        if (force) {
                            MoPubLog.log(CUSTOM, "Cannot force refresh while showing an interstitial.");
                            return false;
                        }
                        // This is the usual transition when done showing this interstitial
                        invalidateInterstitialAdapter();
                        mCurrentInterstitialState = IDLE;
                        return true;
                    case LOADING:
                        if (!force) {
                            MoPubLog.log(CUSTOM, "Interstitial already showing. Not loading another.");
                        }
                        return false;
                    case SHOWING:
                        MoPubLog.log(CUSTOM, "Already showing an interstitial. Cannot show it again.");
                        return false;
                    case DESTROYED:
                        setInterstitialStateDestroyed();
                        return true;
                    default:
                        return false;
                }
            case DESTROYED:
                // Once destroyed, MoPubInterstitial is no longer functional.
                MoPubLog.log(CUSTOM, "MoPubInterstitial destroyed. Ignoring all requests.");
                return false;
            default:
                return false;
        }
    }

    /**
     * Sets MoPubInterstitial to be destroyed. This should only be called by attemptStateTransition.
     */
    private void setInterstitialStateDestroyed() {
        invalidateInterstitialAdapter();
        mInterstitialAdListener = null;
        mCurrentInterstitialState = DESTROYED;
    }

    private void updatedInsets() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            final Window window = mActivity.getWindow();
            if (window == null) {
                return;
            }
            final WindowInsets insets = window.getDecorView().getRootWindowInsets();
            if (insets == null) {
                return;
            }

            if (mAdViewController != null) {
                mAdViewController.setWindowInsets(insets);
            }
        }
    }

    public void load() {
        MoPubLog.log(LOAD_ATTEMPTED);
        attemptStateTransition(LOADING);
    }

    public boolean show() {
        MoPubLog.log(SHOW_ATTEMPTED);
        return attemptStateTransition(SHOWING);
    }

    public void forceRefresh() {
        attemptStateTransition(IDLE, true);
        attemptStateTransition(LOADING, true);
    }

    public boolean isReady() {
        return mCurrentInterstitialState == READY;
    }

    boolean isDestroyed() {
        return mCurrentInterstitialState == DESTROYED;
    }

    private void showFullscreen() {
        if (mAdViewController != null) {
            mAdViewController.show();
        }
    }

    private void invalidateInterstitialAdapter() {
        if (mFullscreenAdapter != null) {
            mFullscreenAdapter.invalidate();
            mFullscreenAdapter = null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @NonNull
    public Activity getActivity() {
        return mActivity;
    }

    public void destroy() {
        attemptStateTransition(DESTROYED);
    }

    public void setInterstitialAdListener(@Nullable final InterstitialAdListener listener) {
        mInterstitialAdListener = listener;
    }

    @Nullable
    public InterstitialAdListener getInterstitialAdListener() {
        return mInterstitialAdListener;
    }

    public void setTesting(boolean testing) {
        if (mAdViewController != null) mAdViewController.setTesting(testing);
    }

    public boolean getTesting() {
        if (mAdViewController != null) return mAdViewController.getTesting();
        else {
            MoPubLog.log(CUSTOM, "Can't get testing status for destroyed AdViewController. " +
                    "Returning false.");
            return false;
        }
    }

    @Override
    public void onAdLoaded() {
        if (isDestroyed()) {
            return;
        }

        attemptStateTransition(READY);
    }

    /*
     * Implements AdLifecycleListener.LoadListener and AdLifecycleListener.InteractionListener
     * Note: All callbacks should be no-ops if the interstitial has been destroyed
     */

    @Override
    public void onAdLoadFailed(@NonNull MoPubErrorCode errorCode) {
        if (isDestroyed()) {
            return;
        }

        MoPubLog.log(LOAD_FAILED, errorCode.getIntCode(), errorCode);
        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialFailed(MoPubInterstitial.this, errorCode);
        }
        attemptStateTransition(IDLE);
    }

    @Override
    public void onAdFailed(@NonNull final MoPubErrorCode errorCode) {
        if (isDestroyed()) {
            return;
        }

        MoPubLog.log(SHOW_FAILED, errorCode.getIntCode(), errorCode);
        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialFailed(MoPubInterstitial.this, errorCode);
        }

        if (loadFailUrl(errorCode)) {
            attemptStateTransition(IDLE);
        }
    }

    @Override
    public void onAdShown() {
        if (isDestroyed()) {
            return;
        }

        MoPubLog.log(SHOW_SUCCESS);

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialShown(this);
        }
    }

    @Override
    public void onAdClicked() {
        if (isDestroyed()) {
            return;
        }
        MoPubLog.log(CLICKED);

        if (mAdViewController != null) {
            mAdViewController.registerClick();
        }

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialClicked(this);
        }
    }

    @Override
    public void onAdDismissed() {
        if (isDestroyed()) {
            return;
        }
        MoPubLog.log(DID_DISAPPEAR);

        attemptStateTransition(IDLE);

        if (mInterstitialAdListener != null) {
            mInterstitialAdListener.onInterstitialDismissed(this);
        }
    }

    @Override
    public void onAdComplete(@Nullable final MoPubReward moPubReward) {
        MoPubLog.log(CUSTOM, "Interstitial finished.");

        if (mAdViewController != null) {
            mAdViewController.onAdComplete(null);
        }
    }

    @Override
    public void onAdImpression() { /* no-op for interstitial */ }

    @Override
    @NonNull
    public AdFormat getAdFormat() {
        return AdFormat.INTERSTITIAL;
    }

    @Override
    public AdViewController getAdViewController() {
        return mAdViewController;
    }

    @Override
    public void setAdViewController(@Nullable AdViewController adViewController) {
        mAdViewController = adViewController;
    }

    @Override
    @NonNull
    public Point resolveAdSize() {
        return DeviceUtils.getDeviceDimensions(mActivity);
    }

    @VisibleForTesting
    @Deprecated
    void setHandler(@NonNull final Handler handler) {
        mHandler = handler;
    }

    @VisibleForTesting
    @Deprecated
    void setCurrentInterstitialState(@NonNull final InterstitialState interstitialState) {
        mCurrentInterstitialState = interstitialState;
    }

    @VisibleForTesting
    @Deprecated
    @NonNull
    InterstitialState getCurrentInterstitialState() {
        return mCurrentInterstitialState;
    }

    @VisibleForTesting
    @Deprecated
    void setFullscreenAdAdapter(@NonNull final FullscreenAdAdapter
            fullscreenAdAdapter) {
        mFullscreenAdapter = fullscreenAdAdapter;
    }
}
