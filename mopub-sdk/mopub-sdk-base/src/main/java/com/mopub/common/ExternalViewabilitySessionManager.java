package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.webkit.WebView;

import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.VastVideoConfig;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Encapsulates all third-party viewability session measurements.
 */
public class ExternalViewabilitySessionManager {

    @NonNull private final Set<ExternalViewabilitySession> mViewabilitySessions;

    public enum ViewabilityVendor {
        AVID, MOAT, ALL;

        public void disable() {
            switch (this) {
                case AVID:
                    AvidViewabilitySession.disable();
                    break;
                case MOAT:
                    MoatViewabilitySession.disable();
                    break;
                case ALL:
                    AvidViewabilitySession.disable();
                    MoatViewabilitySession.disable();
                    break;
                default:
                    MoPubLog.d("Attempted to disable an invalid viewability vendor: " + this);
                    return;
            }
            MoPubLog.d("Disabled viewability for " + this);
        }

        /**
         * @link { AdUrlGenerator#VIEWABILITY_KEY }
         */
        @NonNull
        public static String getEnabledVendorKey() {
            final boolean avidEnabled = AvidViewabilitySession.isEnabled();
            final boolean moatEnabled = MoatViewabilitySession.isEnabled();

            String vendorKey = "0";
            if (avidEnabled && moatEnabled) {
                vendorKey = "3";
            } else if (avidEnabled) {
                vendorKey = "1";
            } else if (moatEnabled) {
                vendorKey = "2";
            }

            return vendorKey;
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

        mViewabilitySessions = new HashSet<ExternalViewabilitySession>();
        mViewabilitySessions.add(new AvidViewabilitySession());
        mViewabilitySessions.add(new MoatViewabilitySession());

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

        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.initialize(context);
            logEvent(session, "initialize", successful, false);
        }
    }

    /**
     * Perform any necessary clean-up and release of resources.
     */
    public void invalidate() {
        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.invalidate();
            logEvent(session, "invalidate", successful, false);
        }
    }

    /**
     * Registers and starts viewability tracking for the given WebView.
     * @param context Preferably an Activity Context.
     * @param webView The WebView to be tracked.
     * @param isDeferred True for cached ads (i.e. interstitials)
     */
    public void createDisplaySession(@NonNull final Context context,
            @NonNull final WebView webView, boolean isDeferred) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(webView);

        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.createDisplaySession(context, webView, isDeferred);
            logEvent(session, "start display session", successful, true);
        }
    }

    public void createDisplaySession(@NonNull final Context context,
            @NonNull final WebView webview) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(webview);

        createDisplaySession(context, webview, false);
    }

    /**
     * Begins deferred impression tracking. For cached ads (i.e. interstitials) this should be
     * called separately from {@link ExternalViewabilitySessionManager#createDisplaySession(Context, WebView)}.
     * @param activity
     */
    public void startDeferredDisplaySession(@NonNull final Activity activity) {
        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.startDeferredDisplaySession(activity);
            logEvent(session, "record deferred session", successful, true);
        }
    }

    /**
     * Unregisters and disables all viewability tracking for the given WebView.
     */
    public void endDisplaySession() {
        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.endDisplaySession();
            logEvent(session, "end display session", successful, true);
        }
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

        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Set<String> buyerResources = new HashSet<String>();
            if (session instanceof AvidViewabilitySession) {
                buyerResources.addAll(vastVideoConfig.getAvidJavascriptResources());
            } else if (session instanceof MoatViewabilitySession) {
                buyerResources.addAll(vastVideoConfig.getMoatImpressionPixels());
            }

            final Boolean successful = session.createVideoSession(activity, view, buyerResources,
                    vastVideoConfig.getExternalViewabilityTrackers());
            logEvent(session, "start video session", successful, true);
        }
    }

    /**
     * Prevents friendly obstructions from affecting viewability scores.
     *
     * @param view View in the same Window and a higher z-index as the video playing.
     */
    public void registerVideoObstruction(@NonNull View view) {
        Preconditions.checkNotNull(view);

        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.registerVideoObstruction(view);
            logEvent(session, "register friendly obstruction", successful, true);
        }
    }

    public void onVideoPrepared(@NonNull final View playerView, final int duration) {
        Preconditions.checkNotNull(playerView);

        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.onVideoPrepared(playerView, duration);
            logEvent(session, "on video prepared", successful, true);
        }
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

        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.recordVideoEvent(event, playheadMillis);
            logEvent(session, "record video event (" + event.name() + ")", successful, true);
        }
    }

    /**
     * Unregisters and disables all viewability tracking for the given View.
     */
    public void endVideoSession() {
        for (final ExternalViewabilitySession session : mViewabilitySessions) {
            final Boolean successful = session.endVideoSession();
            logEvent(session, "end video session", successful, true);
        }
    }

    private void logEvent(@NonNull final ExternalViewabilitySession session,
            @NonNull final String event,
            @Nullable final Boolean successful,
            final boolean isVerbose) {
        Preconditions.checkNotNull(session);
        Preconditions.checkNotNull(event);

        if (successful == null) {
            // Method return values are only null when the corresponding viewability vendor has been
            // disabled. Do not log in those cases.
            return;
        }

        final String failureString = successful ? "" : "failed to ";
        final String message = String.format(Locale.US, "%s viewability event: %s%s.",
                session.getName(), failureString, event);
        if (isVerbose) {
            MoPubLog.v(message);
        } else {
            MoPubLog.d(message);
        }
    }
}
