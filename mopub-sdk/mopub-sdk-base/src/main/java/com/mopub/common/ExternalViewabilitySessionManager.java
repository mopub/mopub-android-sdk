// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;

import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.VastVideoConfig;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;

/**
 * Encapsulates all third-party viewability session measurements.
 */
public class ExternalViewabilitySessionManager {

    public enum ViewabilityVendor {
        AVID, MOAT, ALL;

        public void disable() {
            MoPubLog.log(CUSTOM, "Disabled viewability for " + this);
        }

        /**
         * @link { AdUrlGenerator#VIEWABILITY_KEY }
         */
        @NonNull
        public static String getEnabledVendorKey() {
            return "0";
        }

        @Nullable
        public static ViewabilityVendor fromKey(@NonNull final String key) {
            Preconditions.checkNotNull(key);

            switch (key) {
                case "1":
                    return AVID;
                case "2":
                    return MOAT;
                case "3":
                    return ALL;
                default:
                    return null;
            }
        }
    }

    public ExternalViewabilitySessionManager(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        initialize(context);
    }

    /**
     * Allow the viewability session to perform any necessary initialization. Each session
     * must handle any relevant caching or lazy loading independently.
     *
     * @param context Preferably Activity Context. Currently only used to obtain a reference to the
     *                Application required by some viewability vendors.
     */
    private void initialize(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

    }

    /**
     * Perform any necessary clean-up and release of resources.
     */
    public void invalidate() {
    }

    /**
     * Registers and starts viewability tracking for the given WebView.
     * @param context Preferably an Activity Context.
     * @param webView The WebView to be tracked.
     */
    public void createDisplaySession(@NonNull final Context context,
            @NonNull final WebView webView) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(webView);

    }

    /**
     * Begins deferred impression tracking. For cached ads (i.e. interstitials) this should be
     * called separately from {@link ExternalViewabilitySessionManager#createDisplaySession(Context, WebView)}.
     * @param activity
     */
    public void startDeferredDisplaySession(@NonNull final Activity activity) {
    }

    /**
     * Unregisters and disables all viewability tracking for the given WebView.
     */
    public void endDisplaySession() {
    }

    /**
     * Registers and starts video viewability tracking for the given View.
     *
     * @param activity An Activity Context.
     * @param view The player View.
     * @param vastVideoConfig Configuration file used to store video viewability tracking tags.
     */
    public void createVideoSession(@NonNull final Activity activity, @NonNull final View view,
                                   @NonNull final VastVideoConfig vastVideoConfig) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(view);
        Preconditions.checkNotNull(vastVideoConfig);

    }

    /**
     * Prevents friendly obstructions from affecting viewability scores.
     *
     * @param view View in the same Window and a higher z-index as the video playing.
     */
    public void registerVideoObstruction(@NonNull View view) {
        Preconditions.checkNotNull(view);

    }

    public void onVideoPrepared(@NonNull final View playerView, final int duration) {
        Preconditions.checkNotNull(playerView);

    }

    /**
     * Notify pertinent video lifecycle events (e.g. MediaPlayer onPrepared, first quartile fired).
     *
     * @param event Corresponding {@link ExternalViewabilitySession.VideoEvent}.
     * @param playheadMillis Current video playhead, in milliseconds.
     */
    public void recordVideoEvent(@NonNull final ExternalViewabilitySession.VideoEvent event,
            final int playheadMillis) {
        Preconditions.checkNotNull(event);

    }

    /**
     * Unregisters and disables all viewability tracking for the given View.
     */
    public void endVideoSession() {
    }

    private void logEvent(@NonNull final String session,
            @NonNull final String event,
            @Nullable final Boolean successful,
            final boolean isVerbose) {

    }
}
