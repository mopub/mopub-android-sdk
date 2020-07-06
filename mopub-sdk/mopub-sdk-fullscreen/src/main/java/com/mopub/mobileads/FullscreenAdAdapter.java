// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.BaseAdFactory;

import java.util.Map;

import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_FAILED;
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_SHOW_ERROR;

public class FullscreenAdAdapter extends AdAdapter {
    private Map<String, String> mExtras;
    private long mBroadcastIdentifier;
    private String mDspCreativeId;

    public FullscreenAdAdapter(@NonNull Context context,
                               @NonNull final String className,
                               @NonNull final AdData adData) throws BaseAdNotFoundException {
        super(context, className, adData);
        mBroadcastIdentifier = adData.getBroadcastIdentifier();
        mExtras = adData.getExtras();
        mDspCreativeId = adData.getDspCreativeId();

        if (!(context instanceof Activity)) {
            throw new IllegalArgumentException("An Activity Context is required.");
        }

        MoPubLog.log(CUSTOM, "Attempting to invoke base ad: " + className);
        try {
            mBaseAd = BaseAdFactory.create(className);
        } catch (Exception exception) {
            throw new BaseAdNotFoundException(exception);
        }
    }

    @Override
    void show(@Nullable MoPubAd moPubAd) {
        MoPubLog.log(SHOW_ATTEMPTED);
        if (isInvalidated() || mBaseAd == null) {
            return;
        }

        // Base ad classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            mBaseAd.internalShow(this);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM_WITH_THROWABLE,
                    "Calling show on base ad threw an exception.", e);

            MoPubLog.log(SHOW_FAILED,
                    FULLSCREEN_SHOW_ERROR,
                    FULLSCREEN_SHOW_ERROR.getIntCode());
            onAdFailed(MoPubErrorCode.FULLSCREEN_SHOW_ERROR);
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
                        "Invalidating a base ad interstitial threw an exception.", e);
            }
        }

        final WebViewCacheService.Config config =
                WebViewCacheService.popWebViewConfig(mBroadcastIdentifier);
        if (config != null) {
            config.getWebView().destroy();
        }
    }

    /*
     * AdLifecycleListener.LoadListener and AdLifecycleListener.InteractionListener implementations
     */

    @Override
    public void onAdPauseAutoRefresh() { /* NO-OP */ }

    @Override
    public void onAdResumeAutoRefresh() { /* NO-OP */ }

    @Deprecated
    void setBaseAd(BaseAd baseAd) {
        mBaseAd = baseAd;
    }

    @Deprecated
    @VisibleForTesting
    void setBroadcastIdentifier(long broadcastIdentifier) {
        mBroadcastIdentifier = broadcastIdentifier;
    }

    @VisibleForTesting
    long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }
}
