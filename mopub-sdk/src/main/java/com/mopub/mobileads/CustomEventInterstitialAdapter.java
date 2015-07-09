package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.AdReport;
import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import com.mopub.mobileads.factories.CustomEventInterstitialFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

public class CustomEventInterstitialAdapter implements CustomEventInterstitialListener {
    public static final int DEFAULT_INTERSTITIAL_TIMEOUT_DELAY = Constants.THIRTY_SECONDS_MILLIS;

    private final MoPubInterstitial mMoPubInterstitial;
    private boolean mInvalidated;
    private CustomEventInterstitialAdapterListener mCustomEventInterstitialAdapterListener;
    private CustomEventInterstitial mCustomEventInterstitial;
    private Context mContext;
    private Map<String, Object> mLocalExtras;
    private Map<String, String> mServerExtras;
    private final Handler mHandler;
    private final Runnable mTimeout;

    public CustomEventInterstitialAdapter(@NonNull final MoPubInterstitial moPubInterstitial,
            @NonNull final String className,
            @NonNull final Map<String, String> serverExtras,
            long broadcastIdentifier,
            @Nullable AdReport adReport) {
        Preconditions.checkNotNull(serverExtras);
        mHandler = new Handler();
        mMoPubInterstitial = moPubInterstitial;
        mContext = mMoPubInterstitial.getActivity();
        mTimeout = new Runnable() {
            @Override
            public void run() {
                MoPubLog.d("Third-party network timed out.");
                onInterstitialFailed(NETWORK_TIMEOUT);
                invalidate();
            }
        };

        MoPubLog.d("Attempting to invoke custom event: " + className);
        try {
            mCustomEventInterstitial = CustomEventInterstitialFactory.create(className);
        } catch (Exception exception) {
            MoPubLog.d("Couldn't locate or instantiate custom event: " + className + ".");
            mMoPubInterstitial.onCustomEventInterstitialFailed(ADAPTER_NOT_FOUND);
            return;
        }

        mServerExtras = new TreeMap<String, String>(serverExtras);
        mLocalExtras = mMoPubInterstitial.getLocalExtras();
        if (mMoPubInterstitial.getLocation() != null) {
            mLocalExtras.put("location", mMoPubInterstitial.getLocation());
        }
        mLocalExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        mLocalExtras.put(AD_REPORT_KEY, adReport);
    }

    void loadInterstitial() {
        if (isInvalidated() || mCustomEventInterstitial == null) {
            return;
        }

        if (getTimeoutDelayMilliseconds() > 0) {
            mHandler.postDelayed(mTimeout, getTimeoutDelayMilliseconds());
        }

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mCustomEventInterstitial.loadInterstitial(mContext, this, mLocalExtras, mServerExtras);
        } catch (Exception e) {
            MoPubLog.d("Loading a custom event interstitial threw an exception.", e);
            onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    void showInterstitial() {
        if (isInvalidated() || mCustomEventInterstitial == null) {
            return;
        }

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mCustomEventInterstitial.showInterstitial();
        } catch (Exception e) {
            MoPubLog.d("Showing a custom event interstitial threw an exception.", e);
            onInterstitialFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    void invalidate() {
        if (mCustomEventInterstitial != null) {

            // Custom event classes can be developed by any third party and may not be tested.
            // We catch all exceptions here to prevent crashes from untested code.
            try {
                mCustomEventInterstitial.onInvalidate();
            } catch (Exception e) {
                MoPubLog.d("Invalidating a custom event interstitial threw an exception.", e);
            }
        }
        mCustomEventInterstitial = null;
        mContext = null;
        mServerExtras = null;
        mLocalExtras = null;
        mCustomEventInterstitialAdapterListener = null;
        mInvalidated = true;
    }

    boolean isInvalidated() {
        return mInvalidated;
    }

    void setAdapterListener(CustomEventInterstitialAdapterListener listener) {
        mCustomEventInterstitialAdapterListener = listener;
    }

    private void cancelTimeout() {
        mHandler.removeCallbacks(mTimeout);
    }

    private int getTimeoutDelayMilliseconds() {
        if (mMoPubInterstitial == null
                || mMoPubInterstitial.getAdTimeoutDelay() == null
                || mMoPubInterstitial.getAdTimeoutDelay() < 0) {
            return DEFAULT_INTERSTITIAL_TIMEOUT_DELAY;
        }

        return mMoPubInterstitial.getAdTimeoutDelay() * 1000;
    }

    interface CustomEventInterstitialAdapterListener {
        void onCustomEventInterstitialLoaded();
        void onCustomEventInterstitialFailed(MoPubErrorCode errorCode);
        void onCustomEventInterstitialShown();
        void onCustomEventInterstitialClicked();
        void onCustomEventInterstitialDismissed();
    }

    /*
     * CustomEventInterstitial.Listener implementation
     */
    @Override
    public void onInterstitialLoaded() {
        if (isInvalidated()) {
            return;
        }

        cancelTimeout();

        if (mCustomEventInterstitialAdapterListener != null) {
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialLoaded();
        }
    }

    @Override
    public void onInterstitialFailed(MoPubErrorCode errorCode) {
        if (isInvalidated()) {
            return;
        }

        if (mCustomEventInterstitialAdapterListener != null) {
            if (errorCode == null) {
                errorCode = UNSPECIFIED;
            }
            cancelTimeout();
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialFailed(errorCode);
        }
    }

    @Override
    public void onInterstitialShown() {
        if (isInvalidated()) {
            return;
        }

        if (mCustomEventInterstitialAdapterListener != null) {
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialShown();
        }
    }

    @Override
    public void onInterstitialClicked() {
        if (isInvalidated()) {
            return;
        }

        if (mCustomEventInterstitialAdapterListener != null) {
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialClicked();
        }
    }

    @Override
    public void onLeaveApplication() {
        onInterstitialClicked();
    }

    @Override
    public void onInterstitialDismissed() {
        if (isInvalidated()) {
            return;
        }

        if (mCustomEventInterstitialAdapterListener != null) {
            mCustomEventInterstitialAdapterListener.onCustomEventInterstitialDismissed();
        }
    }

    @Deprecated
    void setCustomEventInterstitial(CustomEventInterstitial interstitial) {
        mCustomEventInterstitial = interstitial;
    }
}
