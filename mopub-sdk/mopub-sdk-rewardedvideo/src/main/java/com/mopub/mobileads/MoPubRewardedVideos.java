package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.MediationSettings;
import com.mopub.common.Preconditions;
import com.mopub.common.util.ReflectionTarget;

/**
 * MoPubRewardedVideos is a utility class that holds controller methods for other MoPub rewarded
 * video classes.
 */
public class MoPubRewardedVideos {

    @ReflectionTarget
    public static void initializeRewardedVideo(@NonNull Activity activity,
            MediationSettings... mediationSettings) {
        Preconditions.checkNotNull(activity);

        MoPubRewardedVideoManager.init(activity, mediationSettings);
    }

    @ReflectionTarget
    public static void setRewardedVideoListener(@Nullable MoPubRewardedVideoListener listener) {
        MoPubRewardedVideoManager.setVideoListener(listener);
    }

    @ReflectionTarget
    public static void loadRewardedVideo(@NonNull String adUnitId,
            @Nullable MediationSettings... mediationSettings) {
        Preconditions.checkNotNull(adUnitId);

        MoPubRewardedVideoManager.loadVideo(adUnitId, null, mediationSettings);
    }

    @ReflectionTarget
    public static void loadRewardedVideo(@NonNull String adUnitId,
            @Nullable MoPubRewardedVideoManager.RequestParameters requestParameters,
            @Nullable MediationSettings... mediationSettings) {
        Preconditions.checkNotNull(adUnitId);

        MoPubRewardedVideoManager.loadVideo(adUnitId, requestParameters, mediationSettings);
    }

    @ReflectionTarget
    public static boolean hasRewardedVideo(@NonNull String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        return MoPubRewardedVideoManager.hasVideo(adUnitId);
    }

    @ReflectionTarget
    public static void showRewardedVideo(@NonNull String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        MoPubRewardedVideoManager.showVideo(adUnitId);
    }
}
