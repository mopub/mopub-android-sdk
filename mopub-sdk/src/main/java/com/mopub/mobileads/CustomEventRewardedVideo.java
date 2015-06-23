package com.mopub.mobileads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubLifecycleManager;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.Map;

/**
 *
 */
public abstract class CustomEventRewardedVideo {
    /**
     * This marker interface is used to indicate that an object is a listener for a 3rd party SDKs
     * rewarded video system.
     */
    @VisibleForTesting
    protected static interface CustomEventRewardedVideoListener {}

    /**
     * Called by the {@link MoPubRewardedVideoManager} after loading the custom event.
     */
    @Nullable
    @VisibleForTesting
    protected abstract CustomEventRewardedVideoListener getVideoListenerForSdk();

    /**
     * Provides a {@link LifecycleListener} if the custom event's ad network wishes to be notified of
     * activity lifecycle events in the application.
     *
     * @return a LifecycleListener. May be null.
     */
    @Nullable
    @VisibleForTesting
    protected abstract LifecycleListener getLifecycleListener();

    /**
     * Called by the {@link MoPubRewardedVideoManager} after loading the custom event.
     * This should return the "ad unit id", "zone id" or similar identifier for the network.
     * May be empty if the network does not have anything more specific than an application ID.
     *
     * @return the id string for this ad unit with the ad network.
     */
    @NonNull
    protected abstract String getAdNetworkId();

    /**
     * Called to when the custom event is no longer used. Implementers should cancel any
     * pending requests. The initialized SDK may be reused by another CustomEvent instance
     * and should not be shut down or cleaned up.
     */
    protected abstract void onInvalidate();

    /**
     * The MoPub ad loading system calls this after MoPub indicates that this custom event should
     * be loaded.
     *
     * @param launcherActivity the "main activity" of the app. Useful for initializing sdks.
     * @param localExtras
     * @param serverExtras
     */
    final void loadCustomEvent(@NonNull Activity launcherActivity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras) {
        try {
            if (checkAndInitializeSdk(launcherActivity, localExtras, serverExtras)) {
                MoPubLifecycleManager.getInstance(launcherActivity).addLifecycleListener(getLifecycleListener());
            }
            loadWithSdkInitialized(launcherActivity, localExtras, serverExtras);
        } catch (Exception e) {
            MoPubLog.e(e.getMessage());
        }
    }

    /**
     * Sets up the 3rd party ads SDK if it needs configuration. Extenders should use this
     * to do any static initialization the first time this method is run by any class instance.
     * From then on, the SDK should be reused without initialization.
     *
     * @return true if the SDK performed initialization, false if the SDK was already initialized.
     */
    protected abstract boolean checkAndInitializeSdk(@NonNull Activity launcherActivity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception;

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
    protected abstract void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception;

    /**
     * Implementers should query the 3rd party SDK for whether there is a video available for the
     * 3rd party SDK & ID represented by the custom event.
     *
     * @return true iff a video is available to play.
     */
    protected abstract boolean hasVideoAvailable();

    /**
     * Implementers should now play the rewarded video for this custom event.
     */
    protected abstract void showVideo();
}
