package com.mopub.mobileads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import com.mopub.mobileads.factories.CustomEventBannerFactory;

import java.util.Map;
import java.util.TreeMap;

import static com.mopub.common.DataKeys.AD_HEIGHT;
import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.AD_WIDTH;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_TIMEOUT;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;

public class CustomEventBannerAdapter implements CustomEventBannerListener {
    public static final int DEFAULT_BANNER_TIMEOUT_DELAY = Constants.TEN_SECONDS_MILLIS;
    private boolean mInvalidated;
    private MoPubView mMoPubView;
    private Context mContext;
    private CustomEventBanner mCustomEventBanner;
    private Map<String, Object> mLocalExtras;
    private Map<String, String> mServerExtras;

    private final Handler mHandler;
    private final Runnable mTimeout;
    private boolean mStoredAutorefresh;

    public CustomEventBannerAdapter(@NonNull MoPubView moPubView,
            @NonNull String className,
            @NonNull Map<String, String> serverExtras,
            long broadcastIdentifier,
            @Nullable AdReport adReport) {
        Preconditions.checkNotNull(serverExtras);
        mHandler = new Handler();
        mMoPubView = moPubView;
        mContext = moPubView.getContext();
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
        mServerExtras = new TreeMap<String, String>(serverExtras);

        mLocalExtras = mMoPubView.getLocalExtras();
        if (mMoPubView.getLocation() != null) {
            mLocalExtras.put("location", mMoPubView.getLocation());
        }
        mLocalExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        mLocalExtras.put(AD_REPORT_KEY, adReport);
        mLocalExtras.put(AD_WIDTH, mMoPubView.getAdWidth());
        mLocalExtras.put(AD_HEIGHT, mMoPubView.getAdHeight());
    }

    void loadAd() {
        if (isInvalidated() || mCustomEventBanner == null) {
            return;
        }

        if (getTimeoutDelayMilliseconds() > 0) {
            mHandler.postDelayed(mTimeout, getTimeoutDelayMilliseconds());
        }

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mCustomEventBanner.loadBanner(mContext, this, mLocalExtras, mServerExtras);
        } catch (Exception e) {
            MoPubLog.d("Loading a custom event banner threw an exception.", e);
            onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }

    void invalidate() {
        if (mCustomEventBanner != null) {
            // Custom event classes can be developed by any third party and may not be tested.
            // We catch all exceptions here to prevent crashes from untested code.
            try {
                mCustomEventBanner.onInvalidate();
            } catch (Exception e) {
                MoPubLog.d("Invalidating a custom event banner threw an exception", e);
            }
        }
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
        if (isInvalidated()) {
            return;
        }

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
        if (isInvalidated()) {
            return;
        }

        mStoredAutorefresh = mMoPubView.getAutorefreshEnabled();
        mMoPubView.setAutorefreshEnabled(false);
        mMoPubView.adPresentedOverlay();
    }

    @Override
    public void onBannerCollapsed() {
        if (isInvalidated()) {
            return;
        }

        mMoPubView.setAutorefreshEnabled(mStoredAutorefresh);
        mMoPubView.adClosed();
    }

    @Override
    public void onBannerClicked() {
        if (isInvalidated()) {
            return;
        }

        if (mMoPubView != null) {
            mMoPubView.registerClick();
        }
    }

    @Override
    public void onLeaveApplication() {
        onBannerClicked();
    }
}
