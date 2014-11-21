package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.view.View;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;
import com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import com.mopub.mobileads.factories.CustomEventBannerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

public class CustomEventBannerAdapter implements CustomEventBannerListener {
    public static final int DEFAULT_BANNER_TIMEOUT_DELAY = 10000;
    private boolean mInvalidated;
    private MoPubView mMoPubView;
    private Context mContext;
    private CustomEventBanner mCustomEventBanner;
    private Map<String, Object> mLocalExtras;
    private Map<String, String> mServerExtras;

    private final Handler mHandler;
    private final Runnable mTimeout;
    private boolean mStoredAutorefresh;

    public CustomEventBannerAdapter(MoPubView moPubView, String className, String classData) {
        mHandler = new Handler();
        mMoPubView = moPubView;
        mContext = moPubView.getContext();
        mLocalExtras = new HashMap<String, Object>();
        mServerExtras = new HashMap<String, String>();
        mTimeout = new Runnable() {
            @Override
            public void run() {
                MoPubLog.d("Third-party network timed out.");
                onBannerFailed(NETWORK_TIMEOUT);
                invalidate();
            }
        };

        MoPubLog.d("Attempting to invoke custom event: " + className);
        try {
            mCustomEventBanner = CustomEventBannerFactory.create(className);
        } catch (Exception exception) {
            MoPubLog.d("Couldn't locate or instantiate custom event: " + className + ".");
            mMoPubView.loadFailUrl(ADAPTER_NOT_FOUND);
            return;
        }

        // Attempt to load the JSON extras into mServerExtras.
        try {
            mServerExtras = Json.jsonStringToMap(classData);
        } catch (Exception exception) {
            MoPubLog.d("Failed to create Map from JSON: " + classData + exception.toString());
        }

        mLocalExtras = mMoPubView.getLocalExtras();
        if (mMoPubView.getLocation() != null) {
            mLocalExtras.put("location", mMoPubView.getLocation());
        }
        if (mMoPubView.getAdViewController() != null) {
            mLocalExtras.put(AD_CONFIGURATION_KEY, mMoPubView.getAdViewController().getAdConfiguration());
        }
    }

    void loadAd() {
        if (isInvalidated() || mCustomEventBanner == null) {
            return;
        }

        if (getTimeoutDelayMilliseconds() > 0) {
            mHandler.postDelayed(mTimeout, getTimeoutDelayMilliseconds());
        }

        mCustomEventBanner.loadBanner(mContext, this, mLocalExtras, mServerExtras);
    }

    void invalidate() {
        if (mCustomEventBanner != null) mCustomEventBanner.onInvalidate();
        mContext = null;
        mCustomEventBanner = null;
        mLocalExtras = null;
        mServerExtras = null;
        mInvalidated = true;
    }

    boolean isInvalidated() {
        return mInvalidated;
    }

    private void cancelTimeout() {
        mHandler.removeCallbacks(mTimeout);
    }

    private int getTimeoutDelayMilliseconds() {
        if (mMoPubView == null
                || mMoPubView.getAdTimeoutDelay() == null
                || mMoPubView.getAdTimeoutDelay() < 0) {
            return DEFAULT_BANNER_TIMEOUT_DELAY;
        }

        return mMoPubView.getAdTimeoutDelay() * 1000;
    }

    /*
     * CustomEventBanner.Listener implementation
     */
    @Override
    public void onBannerLoaded(View bannerView) {
        if (isInvalidated()) {
            return;
        }

        cancelTimeout();

        if (mMoPubView != null) {
            mMoPubView.nativeAdLoaded();
            mMoPubView.setAdContentView(bannerView);
            if (!(bannerView instanceof HtmlBannerWebView)) {
                mMoPubView.trackNativeImpression();
            }
        }
    }

    @Override
    public void onBannerFailed(MoPubErrorCode errorCode) {
        if (isInvalidated()) return;
        
        if (mMoPubView != null) {
            if (errorCode == null) {
                errorCode = UNSPECIFIED;
            }
            cancelTimeout();
            mMoPubView.loadFailUrl(errorCode);
        }
    }

    @Override
    public void onBannerExpanded() {
        if (isInvalidated()) return;

        mStoredAutorefresh = mMoPubView.getAutorefreshEnabled();
        mMoPubView.setAutorefreshEnabled(false);
        mMoPubView.adPresentedOverlay();
    }

    @Override
    public void onBannerCollapsed() {
        if (isInvalidated()) return;

        mMoPubView.setAutorefreshEnabled(mStoredAutorefresh);
        mMoPubView.adClosed();
    }

    @Override
    public void onBannerClicked() {
        if (isInvalidated()) return;
        
        if (mMoPubView != null) mMoPubView.registerClick();
    }
    
    @Override
    public void onLeaveApplication() {
        onBannerClicked();
    }
}
