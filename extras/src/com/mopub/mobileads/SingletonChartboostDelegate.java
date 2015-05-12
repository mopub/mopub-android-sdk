package com.mopub.mobileads;

import android.util.Log;

import com.chartboost.sdk.ChartboostDelegate;
import com.chartboost.sdk.Model.CBError;
import com.mopub.common.MoPubReward;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventRewardedVideo.CustomEventRewardedVideoListener;
import com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import java.util.HashMap;
import java.util.Map;

import static com.chartboost.sdk.Model.CBError.CBImpressionError;
import static com.mopub.mobileads.MoPubErrorCode.VIDEO_DOWNLOAD_ERROR;

@VisibleForTesting
class SingletonChartboostDelegate extends ChartboostDelegate
        implements CustomEventRewardedVideoListener {

    private static final CustomEventInterstitialListener NULL_LISTENER = new CustomEventInterstitialListener() {
        @Override public void onInterstitialLoaded() { }
        @Override public void onInterstitialFailed(MoPubErrorCode errorCode) { }
        @Override public void onInterstitialShown() { }
        @Override public void onInterstitialClicked() { }
        @Override public void onLeaveApplication() { }
        @Override public void onInterstitialDismissed() { }
    };

    static SingletonChartboostDelegate instance = new SingletonChartboostDelegate();

    private Map<String, CustomEventInterstitialListener> mInterstitialLocationsToLoad =
            new HashMap<String, CustomEventInterstitialListener>();

    private Set<String> mRewardedLocationsToLoad = Collections.synchronizedSet(new TreeSet<String>());

    public Map<String, CustomEventInterstitialListener> getInterstitialLocationsToLoad() {
        return mInterstitialLocationsToLoad;
    }

    public Set<String> getRewardedLocationsToLoad() {
        return mRewardedLocationsToLoad;
    }

    public void registerListener(String location, CustomEventInterstitialListener interstitialListener) {
        getInterstitialLocationsToLoad().put(location, interstitialListener);
    }

    public void unregisterListener(String location) {
        getInterstitialLocationsToLoad().remove(location);
    }

    @SuppressWarnings("unused")
    public boolean hasLocation(String location) {
        return getInterstitialLocationsToLoad().containsKey(location);
    }

    CustomEventInterstitialListener getListener(String location) {
        CustomEventInterstitialListener listener = getInterstitialLocationsToLoad().get(location);
        return listener != null ? listener : NULL_LISTENER;
    }

    /*
     * Interstitial delegate methods
     */
    @Override
    public boolean shouldDisplayInterstitial(String location) {
        return true;
    }

    @Override
    public boolean shouldRequestInterstitial(String location) {
        return true;
    }

    @Override
    public void didCacheInterstitial(String location) {
        Log.d("MoPub", "Chartboost interstitial loaded successfully.");
        getListener(location).onInterstitialLoaded();
    }

    @Override
    public void didFailToLoadInterstitial(String location, CBImpressionError error) {
        Log.d("MoPub", "Chartboost interstitial ad failed to load. Error: " + error.name());
        getListener(location).onInterstitialFailed(MoPubErrorCode.NETWORK_NO_FILL);
    }

    @Override
    public void didDismissInterstitial(String location) {
        // Note that this method is fired before didCloseInterstitial and didClickInterstitial.
        Log.d("MoPub", "Chartboost interstitial ad dismissed.");
        getListener(location).onInterstitialDismissed();
    }

    @Override
    public void didCloseInterstitial(String location) {
    }

    @Override
    public void didClickInterstitial(String location) {
        Log.d("MoPub", "Chartboost interstitial ad clicked.");
        getListener(location).onInterstitialClicked();
    }

    @Override
    public void didDisplayInterstitial(String location) {
        Log.d("MoPub", "Chartboost interstitial ad shown.");
        getListener(location).onInterstitialShown();
    }

    /*
     * More Apps delegate methods
     */
    @Override
    public boolean shouldRequestMoreApps(String location) {
        return false;
    }

    @Override
    public boolean shouldDisplayMoreApps(String location) {
        return false;
    }

    @Override
    public void didFailToLoadMoreApps(String location, CBImpressionError error) {
    }

    @Override
    public void didCacheMoreApps(String location) {
    }

    @Override
    public void didDismissMoreApps(String location) {
    }

    @Override
    public void didCloseMoreApps(String location) {
    }

    @Override
    public void didClickMoreApps(String location) {
    }


    /*
     * Rewarded video delegate methods
     */
    @Override
    public boolean shouldDisplayRewardedVideo(String location) {
        return super.shouldDisplayRewardedVideo(location);
    }

    @Override
    public void didCacheRewardedVideo(String location) {
        super.didCacheRewardedVideo(location);

        if (getRewardedLocationsToLoad().contains(location)) {
            MoPubLog.d("Chartboost rewarded video cached for location " + location + ".");
            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(ChartboostRewardedVideo.class, location);
            getRewardedLocationsToLoad().remove(location);
        }
    }

    @Override
    public void didFailToLoadRewardedVideo(String location, CBError.CBImpressionError error) {
        super.didFailToLoadRewardedVideo(location, error);

        if (getRewardedLocationsToLoad().contains(location)) {
            MoPubLog.d("Chartboost rewarded video cache failed for location " + location + ".");
            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(ChartboostRewardedVideo.class, location, VIDEO_DOWNLOAD_ERROR);
            getRewardedLocationsToLoad().remove(location);
        }
    }

    @Override
    public void didDismissRewardedVideo(String location) {
        // This is called before didCloseRewardedVideo and didClickRewardedVideo
        super.didDismissRewardedVideo(location);
        MoPubRewardedVideoManager.onRewardedVideoClosed(ChartboostRewardedVideo.class, location);
        MoPubLog.d("Chartboost rewarded video dismissed for location " + location + ".");
    }

    @Override
    public void didCloseRewardedVideo(String location) {
        super.didCloseRewardedVideo(location);
        MoPubLog.d("Chartboost rewarded video closed for location " + location + ".");
    }

    @Override
    public void didClickRewardedVideo(String location) {
        super.didClickRewardedVideo(location);
        MoPubRewardedVideoManager.onRewardedVideoClicked(ChartboostRewardedVideo.class, location);
        MoPubLog.d("Chartboost rewarded video clicked for location " + location + ".");
    }

    @Override
    public void didCompleteRewardedVideo(String location, int reward) {
        super.didCompleteRewardedVideo(location, reward);
        MoPubLog.d("Chartboost rewarded video completed for location " + location + " with "
                + "reward amount " + reward);
        MoPubRewardedVideoManager.onRewardedVideoCompleted(
                ChartboostRewardedVideo.class,
                location,
                MoPubReward.success(MoPubReward.NO_REWARD_LABEL, reward));
    }

    @Override
    public void didDisplayRewardedVideo(String location) {
        super.didDisplayRewardedVideo(location);
        MoPubLog.d("Chartboost rewarded video displayed for location " + location + ".");
        MoPubRewardedVideoManager.onRewardedVideoStarted(ChartboostRewardedVideo.class, location);
    }
}