// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.LifecycleListener;
import com.mopub.common.MoPubLifecycleManager;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;

/*
 * BaseAd is a base class for network adapters. By
 * implementing subclasses of BaseAd, you can enable the MoPub SDK to natively
 * support a wider variety of third-party ad networks, or execute any of your application code on
 * demand.
 *
 * At runtime, the MoPub SDK will find and instantiate a BaseAd subclass as needed
 * and invoke its checkAndInitializeSdk() and load() methods.
 */
public abstract class BaseAd {

    private boolean mAutomaticImpressionAndClickTracking = true;

    protected AdLifecycleListener.LoadListener mLoadListener;
    protected AdLifecycleListener.InteractionListener mInteractionListener;

    /*
     * Called when a BaseAd is being invalidated or destroyed. Perform any final cleanup here.
     */
    protected abstract void onInvalidate();

    /*
     * Fire MPX impression trackers and 3rd-party impression trackers from JS.
     */
    protected void trackMpxAndThirdPartyImpressions() {
    }

    /**
     * Enables or disables automatic impression and click tracking. This is enabled by default.
     *
     * @param autoTrack True to use automatic impression and click tracking. False to use manual
     *                  impression and click tracking.
     */
    protected void setAutomaticImpressionAndClickTracking(final boolean autoTrack) {
        mAutomaticImpressionAndClickTracking = autoTrack;
    }

    boolean isAutomaticImpressionAndClickTrackingEnabled() {
        return mAutomaticImpressionAndClickTracking;
    }

    /**
     * Provides a {@link LifecycleListener} if the base ad's ad network wishes to be notified of
     * activity lifecycle events in the application.
     *
     * @return a LifecycleListener. May be null.
     */
    @Nullable
    @VisibleForTesting
    protected abstract LifecycleListener getLifecycleListener();

    /**
     * Called by the MoPubRewardedVideoManager after loading the base ad.
     * This should return the "ad unit id", "zone id" or similar identifier for the network.
     * May be empty if the network does not have anything more specific than an application ID.
     *
     * @return the id string for this ad unit with the ad network.
     */
    @NonNull
    protected abstract String getAdNetworkId();

    /**
     * The MoPub ad loading system calls this after MoPub indicates that this base ad should
     * be loaded.
     *
     * @param context      a context from the calling application.
     * @param loadListener the listener to notify of lifecycle events.
     * @param adData       a collection of ad data.
     */
    @VisibleForTesting
    final void internalLoad(@NonNull final Context context,
                            @NonNull final AdLifecycleListener.LoadListener loadListener,
                            @NonNull final AdData adData) throws Exception {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(loadListener);
        Preconditions.checkNotNull(adData);

        mLoadListener = loadListener;

        if (context instanceof Activity && checkAndInitializeSdk((Activity) context, adData)) {
            MoPubLifecycleManager.getInstance((Activity) context).addLifecycleListener(
                    getLifecycleListener());
        }
        load(context, adData);
    }

    /**
     * Sets up the 3rd party ads SDK if it needs configuration. Extenders should use this
     * to do any static initialization the first time this method is run by any class instance.
     * From then on, the SDK should be reused without initialization.
     *
     * @return true if the SDK performed initialization, false if the SDK was already initialized.
     */
    protected abstract boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                                     @NonNull final AdData adData)
            throws Exception;

    /**
     * When the MoPub SDK receives a response indicating it should load a base ad, it will send
     * this message to your base ad class. Your implementation of this method can either load
     * an interstitial ad from a third-party ad network, or execute any application code.
     * It must also notify the provided FullscreenAdListener of certain lifecycle
     * events.
     * <p>
     * The adData parameter is an object containing additional data required by the subclass as well
     * as data configurable on the MoPub website that you want to associate with a given base
     * ad request. This data may be used to pass dynamic information, such as publisher IDs, without
     * changes in application code.
     * <p/>
     * Implementers should also use this method (or checkAndInitializeSdk)
     * to register a listener for their SDK.
     * <p/>
     * This method should not call any MoPubRewardedVideoManager event methods directly
     * (onAdLoadSuccess, etc). Instead the SDK delegate/listener should call these methods.
     *
     * @param context a context from the calling application.
     * @param adData  a collection of ad data.
     */
    protected abstract void load(@NonNull final Context context,
                                 @NonNull final AdData adData)
            throws Exception;

    /**
     * The MoPub ad showing system calls this after MoPub indicates that this base ad should
     * be shown.
     *
     * @param interactionListener the listener to notify of interaction events.
     */
    final void internalShow(@NonNull final AdLifecycleListener.InteractionListener interactionListener) {
        Preconditions.checkNotNull(interactionListener);
        mInteractionListener = interactionListener;
        show();
    }

    /**
     * Implementers should now show the ad for this base ad. Optional for inline ads that correctly
     * return a view from getAdView
     */
    protected void show() { /* no-op */ }

    /**
     * Provides the {@link View} of the base ad's ad network. This is required for Inline ads to
     * show correctly, but is otherwise optional.
     *
     * @return a View. Default implementation returns null.
     */
    @Nullable
    protected View getAdView() {
        return null;
    }
}
