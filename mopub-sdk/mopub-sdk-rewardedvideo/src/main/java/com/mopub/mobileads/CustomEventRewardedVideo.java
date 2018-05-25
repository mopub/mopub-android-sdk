package com.mopub.mobileads;

import android.support.annotation.Nullable;

/**
 * Extend this class to mediate 3rd party rewarded videos.
 */
public abstract class CustomEventRewardedVideo extends CustomEventRewardedAd {

    @Deprecated
    protected interface CustomEventRewardedVideoListener {
    }

    @Deprecated
    @Nullable
    /**
     * This is no longer used. Implementing this does not do anything.
     */
    protected CustomEventRewardedVideoListener getVideoListenerForSdk() {
        return null;
    }

    /**
     * Legacy proxy for {@link CustomEventRewardedAd#isReady}. Implementers should query the 3rd
     * party SDK for whether there is a video available for the 3rd party SDK & ID represented by
     * the custom event. This has been deprecated in favor of isReady(), but isReady() will call
     * hasVideoAvailable() if not overridden.
     */
    @Deprecated
    protected abstract boolean hasVideoAvailable();

    /**
     * Legacy proxy for {@link CustomEventRewardedAd#show}. Implementers should now play the
     * rewarded video for this custom event. This has been deprecated in favor of show(), but
     * show() will call showVideo() if not overridden.
     */
    @Deprecated
    protected abstract void showVideo();

    @Override
    protected boolean isReady() {
        return hasVideoAvailable();
    }

    @Override
    protected void show() {
        showVideo();
    }
}
