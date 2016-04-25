/*

    SpotX - MoPub Advertising Plugin Integration

    To use the the SpotX Advertising Pluging, copy the SpotX* source files into your application’s
    com.mopub.mobileads package, and add a build dependency to your application's build.gradle
    file (e.g. compile 'com.spotxchange:spotx-sdk-android:2.2.+').

 */

package com.mopub.mobileads;

import com.mopub.mobileads.CustomEventRewardedVideo;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.mopub.common.DataKeys;
import com.mopub.common.LifecycleListener;
import com.mopub.common.BaseLifecycleListener;
import com.mopub.common.MediationSettings;
import com.mopub.common.MoPubReward;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubRewardedVideoManager;

import java.util.*;

import com.spotxchange.sdk.android.SpotxAdListener;
import com.spotxchange.sdk.android.SpotxAdSettings;
import com.spotxchange.sdk.android.SpotxAdView;

/**
 *
 */
public class SpotXRewardedVideo extends CustomEventRewardedVideo {

    /*
     * Constants inded for internal MoPub use. Do not modify.
     */
    public static final String SPOTX_AD_NETWORK_CONSTANT = "spotx";

    private static SpotxAdView _spotxAdView;
    private static SpotxRewardedVideoListener _spotxListener;
    private static boolean _initialized;
    private static boolean _isAdAvailable;
    private String _adUnitId;

    private static LifecycleListener _lifecycleListener = new BaseLifecycleListener(){

        @Override
        public void onBackPressed(@NonNull final Activity activity){
            _spotxAdView.setVisibility(View.INVISIBLE);
            _isAdAvailable = false;
        }

        @Override
        public void onDestroy(@NonNull final Activity activity){
            _spotxAdView.setVisibility(View.INVISIBLE);
            _spotxAdView.unsetAdListener();
            _spotxAdView = null;
        }
    };

    public SpotXRewardedVideo(){

        _initialized = _spotxAdView != null;
        _spotxListener = _spotxListener == null ? new SpotxRewardedVideoListener() : _spotxListener;
        _isAdAvailable = false;
        _adUnitId = null;
    }

    @Nullable
    @Override
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return _spotxListener;
    }

    /**
     * Provides a {@link LifecycleListener} if the custom event's ad network wishes to be notified of
     * activity lifecycle events in the application.
     *
     * @return a LifecycleListener. May be null.
     */
    @Nullable
    // @VisibleForTesting
    protected LifecycleListener getLifecycleListener(){
        return _lifecycleListener;
    }

    /**
     * Called by the {@link MoPubRewardedVideoManager} after loading the custom event.
     * This should return the "ad unit id", "zone id" or similar identifier for the network.
     * May be empty if the network does not have anything more specific than an application ID.
     *
     * @return the id string for this ad unit with the ad network.
     */
    @NonNull
    protected String getAdNetworkId(){
        return SPOTX_AD_NETWORK_CONSTANT;
    };

    /**
     * Called to when the custom event is no longer used. Implementers should cancel any
     * pending requests. The initialized SDK may be reused by another CustomEvent instance
     * and should not be shut down or cleaned up.
     */
    protected void onInvalidate(){
        _isAdAvailable = false;
        _spotxAdView.unsetAdListener();
        _spotxAdView = null;
            }

    /**
     * Sets up the 3rd party ads SDK if it needs configuration. Extenders should use this
     * to do any static initialization the first time this method is run by any class instance.
     * From then on, the SDK should be reused without initialization.
     *
     * @return true if the SDK performed initialization, false if the SDK was already initialized.
     */
    @NonNull
    protected  boolean checkAndInitializeSdk(Activity launcherActivity, @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras) throws Exception{

        synchronized (SpotXRewardedVideo.class){

            if(!_initialized){
                SpotxAdSettings adSettings = SpotXCommon.constructAdSettings(localExtras, serverExtras, false);
                _spotxAdView = new SpotxAdView(launcherActivity, adSettings);
                _spotxAdView.setAdListener(_spotxListener);
                launcherActivity.addContentView(_spotxAdView, new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                _initialized = true;
                return true;
            }

            return false;
        }
    }

    /**
     * Runs the ad-loading logic for the 3rd party SDK. localExtras & serverExtras should together
     * contain all the data needed to load an ad.
     *
     * Implementers should also use this method (or checkAndInitializeSdk)
     * to register a listener for their SDK, wrap it in a
     * {@link com.mopub.mobileads.CustomEventRewardedVideo.CustomEventRewardedVideoListener}
     *
     * This method should not call any {@link MoPubRewardedVideoManager} event methods directly
     * (onAdLoadSuccess, etc). Instead the SDK delegate/listener should call these methods.
     *
     * @param activity the "main activity" of the app. Useful for initializing sdks.
     * @param localExtras
     * @param serverExtras
     */
    @NonNull
    protected void loadWithSdkInitialized(Activity activity, Map<String, Object> localExtras,
            Map<String, String> serverExtras) throws Exception{

        this._adUnitId = (String)localExtras.get(DataKeys.AD_UNIT_ID_KEY);
        SpotxMediationSettings ms = MoPubRewardedVideoManager.getInstanceMediationSettings(SpotxMediationSettings.class, this._adUnitId);
        String channel_id = (ms != null && ms.channel_id != null) ? ms.channel_id : "";
        if(!channel_id.isEmpty()){
            localExtras.put(SpotXCommon.CHANNEL_ID_KEY, channel_id);
        }

        SpotxAdSettings adSettings = SpotXCommon.constructAdSettings(localExtras, serverExtras, false);
        _spotxAdView.setAdSettings(adSettings);
        _spotxAdView.setVisibility(View.INVISIBLE);
        _spotxAdView.init();
    }

    /**
     * Implementers should query the 3rd party SDK for whether there is a video available for the
     * 3rd party SDK & ID represented by the custom event.
     *
     * @return true iff a video is available to play.
     */
    @Override
    protected boolean hasVideoAvailable(){
        return _isAdAvailable;
    }

    /**
     * Implementers should now play the rewarded video for this custom event.
     */
    protected void showVideo(){
        _spotxAdView.setVisibility(View.VISIBLE);
    }

    private class SpotxRewardedVideoListener implements CustomEventRewardedVideoListener, SpotxAdListener {

        @Override
        public void adLoaded() {

            MoPubRewardedVideoManager.onRewardedVideoLoadSuccess(SpotXRewardedVideo.class, SPOTX_AD_NETWORK_CONSTANT);
            _isAdAvailable = true;
        }

        @Override
        public void adStarted() {

            MoPubRewardedVideoManager.onRewardedVideoStarted(SpotXRewardedVideo.class, SPOTX_AD_NETWORK_CONSTANT);
        }

        @Override
        public void adCompleted() {

            _spotxAdView.setVisibility(View.INVISIBLE);
            MoPubRewardedVideoManager.onRewardedVideoCompleted(SpotXRewardedVideo.class, SPOTX_AD_NETWORK_CONSTANT,
                    MoPubReward.success(MoPubReward.NO_REWARD_LABEL, MoPubReward.NO_REWARD_AMOUNT));
            _isAdAvailable = false;
        }

        @Override
        public void adError() {

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(
                    SpotXRewardedVideo.class,
                    SPOTX_AD_NETWORK_CONSTANT,
                    MoPubErrorCode.UNSPECIFIED);
            _isAdAvailable = false;
        }

        @Override
        public void adExpired() {

            MoPubRewardedVideoManager.onRewardedVideoLoadFailure(SpotXRewardedVideo.class, SPOTX_AD_NETWORK_CONSTANT,
                    MoPubErrorCode.UNSPECIFIED);
            _isAdAvailable = false;
        }

        @Override
        public void adClicked() {
            MoPubRewardedVideoManager.onRewardedVideoClicked(SpotXRewardedVideo.class, SPOTX_AD_NETWORK_CONSTANT);
        }
    }

    public static class SpotxMediationSettings implements MediationSettings
    {
        @Nullable private final String channel_id;

        public static class Builder {
            @Nullable private String channel_id;

            public Builder withChannelId(@NonNull final String channel_id) {
                this.channel_id = channel_id;
                return this;
    }

            public SpotxMediationSettings build() {
                return new SpotxMediationSettings(this);
}
        }

        private SpotxMediationSettings(@NonNull final Builder builder) {
            this.channel_id = builder.channel_id;
        }
    }
}