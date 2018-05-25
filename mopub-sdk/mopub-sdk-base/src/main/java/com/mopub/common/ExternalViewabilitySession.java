package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementers should use reflection to invoke external APIs as all viewability dependencies are
 * optional.
 *
 * Note that all interface methods return @Nullable Booleans. Return values are as follows:
 * null - vendor was disabled either via client or server; method calls fast fail
 * true - successfully called through via reflection
 * false - error invoking via reflection or unexpected internal session state
 */
public interface ExternalViewabilitySession {
    @NonNull String getName();

    @Nullable Boolean initialize(@NonNull final Context context);
    @Nullable Boolean invalidate();

    // Display only
    @Nullable Boolean createDisplaySession(@NonNull final Context context,
            @NonNull final WebView webView, boolean isDeferred);
    @Nullable Boolean startDeferredDisplaySession(@NonNull final Activity activity);
    @Nullable Boolean endDisplaySession();

    // Video only
    @Nullable Boolean createVideoSession(@NonNull final Activity activity, @NonNull final View view,
            @NonNull final Set<String> buyerResources,
            @NonNull final Map<String, String> videoViewabilityTrackers);
    @Nullable Boolean registerVideoObstruction(@NonNull final View view);
    @Nullable Boolean onVideoPrepared(@NonNull final View playerView, final int duration);
    @Nullable Boolean recordVideoEvent(@NonNull final VideoEvent event, final int playheadMillis);
    @Nullable Boolean endVideoSession();

    enum VideoEvent {
        AD_LOADED(null, "recordAdLoadedEvent"),
        AD_STARTED("AD_EVT_START", "recordAdStartedEvent"),
        AD_STOPPED("AD_EVT_STOPPED", "recordAdStoppedEvent"),
        AD_PAUSED("AD_EVT_PAUSED", "recordAdPausedEvent"),
        AD_PLAYING("AD_EVT_PLAYING", "recordAdPlayingEvent"),
        AD_SKIPPED("AD_EVT_SKIPPED", "recordAdSkippedEvent"),

        AD_IMPRESSED(null, "recordAdImpressionEvent"),
        AD_CLICK_THRU(null, "recordAdClickThruEvent"),

        AD_VIDEO_FIRST_QUARTILE("AD_EVT_FIRST_QUARTILE", "recordAdVideoFirstQuartileEvent"),
        AD_VIDEO_MIDPOINT("AD_EVT_MID_POINT", "recordAdVideoMidpointEvent"),
        AD_VIDEO_THIRD_QUARTILE("AD_EVT_THIRD_QUARTILE", "recordAdVideoThirdQuartileEvent"),
        AD_COMPLETE("AD_EVT_COMPLETE", "recordAdCompleteEvent"),

        RECORD_AD_ERROR(null, "recordAdError");

        // These are not yet possible with our VAST player. Unimplemented.
        // AD_EXPANDED_CHANGE,
        // AD_ENTERED_FULLSCREEN,
        // AD_EXITED_FULLSCREEN,
        // RECORD_AD_DURATION_CHANGED,
        // AD_VOLUME_CHANGE_EVENT,

        // These are not yet possible with our VAST player. Unimplemented.
        // AD_USER_MINIMIZE,
        // AD_USER_ACCEPT_INVITATION,
        // AD_USER_CLOSE,


        @Nullable private String moatEnumName;
        @NonNull private String avidMethodName;
        VideoEvent(@Nullable final String moatEnumName, @NonNull final String avidMethodName) {
            this.moatEnumName = moatEnumName;
            this.avidMethodName = avidMethodName;
        }

        @Nullable
        public String getMoatEnumName() {
            return moatEnumName;
        }

        @NonNull
        public String getAvidMethodName() {
            return avidMethodName;
        }
    }
}
