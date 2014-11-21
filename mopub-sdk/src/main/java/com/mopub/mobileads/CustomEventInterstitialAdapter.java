package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;
import com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import com.mopub.mobileads.factories.CustomEventInterstitialFactory;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

public class CustomEventInterstitialAdapter implements CustomEventInterstitialListener {
    public static final int DEFAULT_INTERSTITIAL_TIMEOUT_DELAY = 30000;

    private final MoPubInterstitial mMoPubInterstitial;
    private boolean mInvalidated;
    private CustomEventInterstitialAdapterListener mCustomEventInterstitialAdapterListener;
    private CustomEventInterstitial mCustomEventInterstitial;
    private Context mContext;
    private Map<String, Object> mLocalExtras;
    private Map<String, String> mServerExtras;
    private final Handler mHandler;
    private final Runnable mTimeout;

    public CustomEventInterstitialAdapter(final MoPubInterstitial moPubInterstitial, final String className, final String jsonParams) {
        mHandler = new Handler();
        mMoPubInterstitial = moPubInterstitial;
        mServerExtras = new HashMap<String, String>();
        mLocalExtras = new HashMap<String, Object>();
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

        // Attempt to load the JSON extras into mServerExtras.
        try {
            mServerExtras = Json.jsonStringToMap(jsonParams);
        } catch (Exception exception) {
            MoPubLog.d("Failed to create Map from JSON: " + jsonParams);
        }

        mLocalExtras = mMoPubInterstitial.getLocalExtras();
        if (mMoPubInterstitial.getLocation() != null) {
            mLocalExtras.put("location", mMoPubInterstitial.getLocation());
        }

        final AdViewController adViewController = mMoPubInterstitial.getMoPubInterstitialView().getAdViewController();
        if (adViewController != null) {
            mLocalExtras.put(AD_CONFIGURATION_KEY, adViewController.getAdConfiguration());
        }
    }
    
    void loadInterstitial() {
        if (isInvalidated() || mCustomEventInterstitial == null) {
            return;
        }

        if (getTimeoutDelayMilliseconds() > 0) {
            mHandler.postDelayed(mTimeout, getTimeoutDelayMilliseconds());
        }

        mCustomEventInterstitial.loadInterstitial(mContext, this, mLocalExtras, mServerExtras);
    }
    
    void showInterstitial() {
        if (isInvalidated() || mCustomEventInterstitial == null) return;
        
        mCustomEventInterstitial.showInterstitial();
    }

    void invalidate() {
        if (mCustomEventInterstitial != null) mCustomEventInterstitial.onInvalidate();
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
        if (isInvalidated()) return;

        if (mCustomEventInterstitialAdapterListener != null) mCustomEventInterstitialAdapterListener.onCustomEventInterstitialDismissed();
    }

    @Deprecated
    void setCustomEventInterstitial(CustomEventInterstitial interstitial) {
        mCustomEventInterstitial = interstitial;
    }
}
