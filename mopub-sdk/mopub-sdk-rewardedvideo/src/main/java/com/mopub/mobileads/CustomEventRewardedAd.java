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
 * The base class when dealing with rewarded formats.
 */
public abstract class CustomEventRewardedAd {

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
     * The MoPub ad loading system calls this after MoPub indicates that this custom event should
     * be loaded.
     *
     * @param launcherActivity the "main activity" of the app. Useful for initializing sdks.
     * @param localExtras      a map containing additional custom data set in app
     * @param serverExtras     a map containing additional custom data configurable on the mopub website
     */
    final void loadCustomEvent(@NonNull Activity launcherActivity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras) {
        try {
            if (checkAndInitializeSdk(launcherActivity, localExtras, serverExtras)) {
                MoPubLifecycleManager.getInstance(launcherActivity).addLifecycleListener(
                        getLifecycleListener());
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
     * <p/>
     * Implementers should also use this method (or checkAndInitializeSdk)
     * to register a listener for their SDK.
     * <p/>
     * This method should not call any {@link MoPubRewardedVideoManager} event methods directly
     * (onAdLoadSuccess, etc). Instead the SDK delegate/listener should call these methods.
     *
     * @param activity     the "main activity" of the app. Useful for initializing sdks.
     * @param localExtras  a map containing additional custom data set in app
     * @param serverExtras a map containing additional custom data configurable on the mopub website
     */
    protected abstract void loadWithSdkInitialized(@NonNull Activity activity,
            @NonNull Map<String, Object> localExtras,
            @NonNull Map<String, String> serverExtras)
            throws Exception;

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
     * Implementers should query the 3rd party SDK for whether the 3rd party SDK & ID represented
     * by the custom event is ready to be shown.
     *
     * @return true iff a video is available to play.
     */
    protected abstract boolean isReady();

    /**
     * Implementers should now play the rewarded item for this custom event.
     */
    protected abstract void show();
}
