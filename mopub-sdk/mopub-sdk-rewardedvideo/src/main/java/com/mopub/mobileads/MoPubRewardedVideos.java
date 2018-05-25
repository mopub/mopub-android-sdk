package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.common.Preconditions;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.ReflectionTarget;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * MoPubRewardedVideos is a utility class that holds controller methods for other MoPub rewarded
 * video classes.
 */
public class MoPubRewardedVideos {

    @ReflectionTarget
    private static void initializeRewardedVideo(@NonNull Activity activity,
            MediationSettings... mediationSettings) {
        Preconditions.checkNotNull(activity);

        MoPubRewardedVideoManager.init(activity, mediationSettings);
    }

    @ReflectionTarget
    private static void initializeRewardedVideo(@NonNull Activity activity,
            @NonNull List<Class<? extends CustomEventRewardedVideo>> networksToInit,
            MediationSettings... mediationSettings) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(networksToInit);

        MoPubRewardedVideoManager.init(activity, mediationSettings);
        MoPubRewardedVideoManager.initNetworks(activity, networksToInit);
    }

    /**
     * Use MoPub#initializeSdk instead.
     */
    @ReflectionTarget
    @SuppressWarnings("unchecked")
    private static void initializeRewardedVideo(@NonNull final Activity activity,
            @NonNull final SdkConfiguration sdkConfiguration) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(sdkConfiguration);

        final List<String> networksToInit = sdkConfiguration.getNetworksToInit();
        final List<Class<? extends CustomEventRewardedVideo>> classList = new ArrayList<>();
        if (networksToInit != null) {
            for (final String networkClassName : networksToInit) {
                if (TextUtils.isEmpty(networkClassName)) {
                    continue;
                }
                try {
                    final Class networkClass = Class.forName(networkClassName);
                    classList.add(networkClass.asSubclass(CustomEventRewardedVideo.class));
                } catch (ClassNotFoundException e) {
                    MoPubLog.w("Ignoring unknown class name " + networkClassName);
                } catch (ClassCastException e) {
                    MoPubLog.w(
                            "Unable to cast " + networkClassName +
                                    " to Class<? extends CustomEventRewardedVideo>.");
                }
            }
        }

        if (!classList.isEmpty()) {
            initializeRewardedVideo(activity, classList, sdkConfiguration.getMediationSettings());
        } else {
            initializeRewardedVideo(activity, sdkConfiguration.getMediationSettings());
        }
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

    @ReflectionTarget
    public static void showRewardedVideo(@NonNull String adUnitId, @Nullable String customData) {
        Preconditions.checkNotNull(adUnitId);

        MoPubRewardedVideoManager.showVideo(adUnitId, customData);
    }

    @ReflectionTarget
    public static Set<MoPubReward> getAvailableRewards(@NonNull String adUnitId) {
        Preconditions.checkNotNull(adUnitId);

        return MoPubRewardedVideoManager.getAvailableRewards(adUnitId);
    }

    @ReflectionTarget
    public static void selectReward(@NonNull String adUnitId, @NonNull MoPubReward selectedReward) {
        Preconditions.checkNotNull(adUnitId);
        Preconditions.checkNotNull(selectedReward);

        MoPubRewardedVideoManager.selectReward(adUnitId, selectedReward);
    }
}
