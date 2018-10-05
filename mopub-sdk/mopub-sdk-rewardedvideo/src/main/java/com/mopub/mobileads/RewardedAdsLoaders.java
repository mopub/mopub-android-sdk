// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.network.AdLoader;
import com.mopub.network.AdResponse;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import java.util.HashMap;
import java.util.Map;

class RewardedAdsLoaders {
    @NonNull private final HashMap<String, AdLoaderRewardedVideo> mAdUnitToAdLoader;
    @NonNull private final MoPubRewardedVideoManager moPubRewardedVideoManager;

    public class RewardedVideoRequestListener implements AdLoader.Listener {
        public final String adUnitId;

        RewardedVideoRequestListener(String adUnitId) {
            this.adUnitId = adUnitId;
        }

        @Override
        public void onSuccess(final AdResponse response) {
            moPubRewardedVideoManager.onAdSuccess(response);
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            moPubRewardedVideoManager.onAdError(volleyError, adUnitId);
        }
    }

    RewardedAdsLoaders(@NonNull final MoPubRewardedVideoManager rewardedVideoManager){
        moPubRewardedVideoManager  = rewardedVideoManager;
        mAdUnitToAdLoader = new HashMap<>();
    }

    @Nullable
    Request<?> loadNextAd(@NonNull Context context,
                          @NonNull String adUnitId,
                          @NonNull String adUrlString,
                          @Nullable MoPubErrorCode errorCode) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(context);

        AdLoaderRewardedVideo adLoader = mAdUnitToAdLoader.get(adUnitId);

        if (adLoader == null || !adLoader.hasMoreAds()) {
            adLoader = new AdLoaderRewardedVideo(adUrlString,
                                                 AdFormat.REWARDED_VIDEO,
                                                 adUnitId,
                                                 context,
                                                 new RewardedVideoRequestListener(adUnitId));
            mAdUnitToAdLoader.put(adUnitId, adLoader);
        }

        return adLoader.loadNextAd(errorCode);
    }

    boolean isLoading(@NonNull final String adUnitId) {
        return mAdUnitToAdLoader.containsKey(adUnitId) && mAdUnitToAdLoader.get(adUnitId).isRunning();
    }

    void markFail(@NonNull final String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        if (mAdUnitToAdLoader.containsKey(adUnitId)) {
            mAdUnitToAdLoader.remove(adUnitId);
        }
    }

    void markPlayed(@NonNull final String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        if (mAdUnitToAdLoader.containsKey(adUnitId)) {
            mAdUnitToAdLoader.remove(adUnitId);
        }
    }

    void onRewardedVideoStarted(@NonNull String adUnitId, @NonNull Context context) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(context);

        AdLoaderRewardedVideo loaderRewardedVideo = mAdUnitToAdLoader.get(adUnitId);
        if (loaderRewardedVideo == null) {
            return;
        }

        loaderRewardedVideo.trackImpression(context);
    }

    void onRewardedVideoClicked(@NonNull String adUnitId, @NonNull Context context){
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(context);

        AdLoaderRewardedVideo loaderRewardedVideo = mAdUnitToAdLoader.get(adUnitId);
        if (loaderRewardedVideo == null) {
            return;
        }

        loaderRewardedVideo.trackClick(context);
    }

    boolean canPlay(@NonNull final String adUnitId) {
        AdLoaderRewardedVideo loaderRewardedVideo = mAdUnitToAdLoader.get(adUnitId);
        if (loaderRewardedVideo == null) {
            return false;
        }

        AdResponse adResponse =  loaderRewardedVideo.getLastDeliveredResponse();
        return adResponse != null;
    }

    boolean hasMoreAds(@NonNull final String adUnitId) {
        AdLoaderRewardedVideo loaderRewardedVideo = mAdUnitToAdLoader.get(adUnitId);
        return loaderRewardedVideo != null && loaderRewardedVideo.hasMoreAds();
    }

    void creativeDownloadSuccess(@NonNull final String adUnitId){
        AdLoaderRewardedVideo loaderRewardedVideo = mAdUnitToAdLoader.get(adUnitId);
        if (loaderRewardedVideo == null) {
            return;
        }

        loaderRewardedVideo.creativeDownloadSuccess();
    }

    @Deprecated
    @VisibleForTesting
    void clearMapping() {
        mAdUnitToAdLoader.clear();
    }

    @Deprecated
    @VisibleForTesting
    Map<String, AdLoaderRewardedVideo> getLoadersMap(){
        return mAdUnitToAdLoader;
    }
}
