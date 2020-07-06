// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.Preconditions;
import com.mopub.network.AdLoader;
import com.mopub.network.AdResponse;
import com.mopub.network.SingleImpression;
import com.mopub.network.TrackingRequest;

import java.util.Collections;
import java.util.List;

class AdLoaderRewardedVideo extends AdLoader {
    private boolean mImpressionTrackerFired;
    private boolean mClickTrackerFired;

    AdLoaderRewardedVideo(@NonNull String url,
                          @NonNull AdFormat adFormat,
                          @NonNull String adUnitId,
                          @NonNull Context context,
                          @NonNull Listener listener) {
        super(url, adFormat, adUnitId, context, listener);

        mImpressionTrackerFired = false;
        mClickTrackerFired = false;
    }

    @Nullable
    String getFailurl() {
        if (mMultiAdResponse != null) {
            return mMultiAdResponse.getFailURL();
        }
        return null;
    }

    @NonNull
    List<String> getImpressionUrls() {
        if (mLastDeliveredResponse != null) {
            return mLastDeliveredResponse.getImpressionTrackingUrls();
        }
        return Collections.emptyList();
    }

    @Nullable
    String getClickUrl() {
        if (mLastDeliveredResponse != null) {
            return mLastDeliveredResponse.getClickTrackingUrl();
        }
        return null;
    }

    @Nullable
    AdResponse getLastDeliveredResponse() {
        return mLastDeliveredResponse;
    }

    void trackImpression(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (mLastDeliveredResponse == null || mImpressionTrackerFired) {
            return;
        }

        mImpressionTrackerFired = true;
        TrackingRequest.makeTrackingHttpRequest(
                getImpressionUrls(),
                context);

        final String adUnitId = mLastDeliveredResponse.getAdUnitId();
        new SingleImpression(adUnitId, mLastDeliveredResponse.getImpressionData()).sendImpression();
    }

    void trackClick(@NonNull Context context) {
        Preconditions.checkNotNull(context);

        if (mLastDeliveredResponse == null || mClickTrackerFired) {
            return;
        }

        mClickTrackerFired = true;
        TrackingRequest.makeTrackingHttpRequest(
                getClickUrl(),
                context);
    }
}
