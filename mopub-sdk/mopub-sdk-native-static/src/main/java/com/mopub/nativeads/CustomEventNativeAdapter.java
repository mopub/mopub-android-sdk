// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Constants;
import com.mopub.common.DataKeys;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.nativeads.factories.CustomEventNativeFactory;
import com.mopub.network.AdResponse;

import java.util.Map;

final class CustomEventNativeAdapter {

    @NonNull
    private final Handler mHandler;
    @NonNull
    private final Runnable mTimeout;
    @Nullable
    private CustomEventNative customEventNative;
    @NonNull
    private CustomEventNative.CustomEventNativeListener mExternalListener;

    private volatile boolean mCompleted;

    CustomEventNativeAdapter(@NonNull final CustomEventNative.CustomEventNativeListener customEventNativeListener) {
        Preconditions.checkNotNull(customEventNativeListener);

        mExternalListener = customEventNativeListener;

        mCompleted = false;
        mHandler = new Handler();
        mTimeout = new Runnable() {
            @Override
            public void run() {
                if (mCompleted) {
                    return;
                }
                MoPubLog.d("Timeout loading native ad content. " + CustomEventNativeAdapter.this.toString());
                stopLoading();
                mExternalListener.onNativeAdFailed(NativeErrorCode.NETWORK_TIMEOUT);
            }
        };
    }

    public void loadNativeAd(@NonNull final Context context,
                             @NonNull final Map<String, Object> localExtras,
                             @NonNull final AdResponse adResponse) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(localExtras);
        Preconditions.checkNotNull(adResponse);

        String customEventNativeClassName = adResponse.getCustomEventClassName();

        MoPubLog.d("Attempting to invoke custom event: " + customEventNativeClassName);
        try {
            customEventNative = CustomEventNativeFactory.create(customEventNativeClassName);
        } catch (Exception e) {
            MoPubLog.e("Failed to load Custom Event Native class: " + customEventNativeClassName);
            mExternalListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
            return;
        }
        if (adResponse.hasJson()) {
            localExtras.put(DataKeys.JSON_BODY_KEY, adResponse.getJsonBody());
        }

        localExtras.put(DataKeys.CLICK_TRACKING_URL_KEY, adResponse.getClickTrackingUrl());

        // Custom event classes can be developed by any third party and may not be tested.
        // We catch all exceptions here to prevent crashes from untested code.
        try {
            customEventNative.loadNativeAd(
                    context,
                    createListener(),
                    localExtras,
                    adResponse.getServerExtras());

            long timeoutMS = adResponse.getAdTimeoutMillis(Constants.THIRTY_SECONDS_MILLIS);
            mHandler.postDelayed(mTimeout, timeoutMS);
        } catch (Exception e) {
            MoPubLog.w("Loading custom event native threw an error.", e);
            mExternalListener.onNativeAdFailed(NativeErrorCode.NATIVE_ADAPTER_NOT_FOUND);
        }
    }

    @NonNull
    private CustomEventNative.CustomEventNativeListener createListener() {
        return new CustomEventNative.CustomEventNativeListener() {
            @Override
            public void onNativeAdLoaded(BaseNativeAd nativeAd) {
                if (mCompleted) {
                    return;
                }
                invalidate();
                mExternalListener.onNativeAdLoaded(nativeAd);
            }

            @Override
            public void onNativeAdFailed(NativeErrorCode errorCode) {
                if (mCompleted) {
                    return;
                }
                invalidate();
                mExternalListener.onNativeAdFailed(errorCode);
            }
        };
    }

    void stopLoading() {
        try {
            if (customEventNative != null)
                customEventNative.onInvalidate();
        } catch (Exception e) {
            MoPubLog.e(e.toString());
        }
        invalidate();
    }

    private synchronized void invalidate() {
        if (!mCompleted) {
            mCompleted = true;
            mHandler.removeCallbacks(mTimeout);
            customEventNative = null;
        }
    }
}
