package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Reflection;

import java.util.List;
import java.util.Map;
import java.util.Set;

// API documentation: https://drive.google.com/drive/folders/0B8U0thTyT1GGZTdEWm03VTlfbm8
class AvidViewabilitySession implements ExternalViewabilitySession {
    private static final String AVID_MANAGER_PATH =
            "com.integralads.avid.library.mopub.AvidManager";
    private static final String EXTERNAL_AVID_AD_SESSION_CONTEXT_PATH =
            "com.integralads.avid.library.mopub.session.ExternalAvidAdSessionContext";
    private static final String AVID_AD_SESSION_MANAGER_PATH =
            "com.integralads.avid.library.mopub.session.AvidAdSessionManager";
    private static final String AVID_KEY = "avid";

    private static Object sAvidAdSessionContextDeferred;
    private static Object sAvidAdSessionContextNonDeferred;
    private static Boolean sIsViewabilityEnabledViaReflection;
    private static boolean sIsVendorDisabled;

    @Nullable private Object mAvidDisplayAdSession;
    @Nullable private Object mAvidVideoAdSession;

    static boolean isEnabled() {
        return !sIsVendorDisabled && isViewabilityEnabledViaReflection();
    }

    static void disable() {
        sIsVendorDisabled = true;
    }

    private static boolean isViewabilityEnabledViaReflection() {
        if (sIsViewabilityEnabledViaReflection == null) {
            sIsViewabilityEnabledViaReflection = Reflection.classFound(AVID_AD_SESSION_MANAGER_PATH);
            MoPubLog.d("Avid is "
                    + (sIsViewabilityEnabledViaReflection ? "" : "un")
                    + "available via reflection.");
        }

        return sIsViewabilityEnabledViaReflection;
    }

    @Nullable
    private static Object getAvidAdSessionContextDeferred() {
        if (sAvidAdSessionContextDeferred == null) {
            try {
                // Pre-reflection code:
                // sAvidAdSessionContextDeferred = new ExternalAvidAdSessionContext(
                //         MoPub.SDK_VERSION, true);

                sAvidAdSessionContextDeferred = Reflection.instantiateClassWithConstructor(
                        EXTERNAL_AVID_AD_SESSION_CONTEXT_PATH, Object.class,
                        new Class[] {String.class, boolean.class},
                        new Object[] {MoPub.SDK_VERSION, true});
            } catch (Exception e) {
                MoPubLog.d("Unable to generate Avid deferred ad session context: "
                        + e.getMessage());
            }
        }

        return sAvidAdSessionContextDeferred;
    }

    @Nullable
    private static Object getAvidAdSessionContextNonDeferred() {
        if (sAvidAdSessionContextNonDeferred == null) {
            try {
                // Pre-reflection code:
                // sAvidAdSessionContextNonDeferred = new ExternalAvidAdSessionContext(
                //         MoPub.SDK_VERSION);

                sAvidAdSessionContextNonDeferred = Reflection.instantiateClassWithConstructor(
                        EXTERNAL_AVID_AD_SESSION_CONTEXT_PATH, Object.class,
                        new Class[] {String.class},
                        new Object[] {MoPub.SDK_VERSION});
            } catch (Exception e) {
                MoPubLog.d("Unable to generate Avid ad session context: "
                        + e.getMessage());
            }
        }

        return sAvidAdSessionContextNonDeferred;
    }

    @Override
    @NonNull
    public String getName() {
        return "AVID";
    }

    @Override
    @Nullable
    public Boolean initialize(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        if (!isEnabled()) {
            return null;
        }

        // unimplemented by Avid
        return true;
    }

    @Override
    @Nullable
    public Boolean invalidate() {
        if (!isEnabled()) {
            return null;
        }

        mAvidDisplayAdSession = null;
        mAvidVideoAdSession = null;

        return true;
    }

    @Override
    @Nullable
    public Boolean createDisplaySession(@NonNull final Context context,
            @NonNull final WebView webView, boolean isDeferred) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(webView);

        if (!isEnabled()) {
            return null;
        }

        final Object avidAdSessionContext =
                isDeferred
                ? getAvidAdSessionContextDeferred()
                : getAvidAdSessionContextNonDeferred();
        final Activity activity = (context instanceof Activity) ? (Activity) context : null;

        // Pre-reflection code:
        // mAvidDisplayAdSession = AvidAdSessionManager.startAvidDisplayAdSession(activity,
        //         avidAdSessionContext);
        // mAvidDisplayAdSession.registerAdView(webView, activity);

        try {
            mAvidDisplayAdSession = new Reflection.MethodBuilder(null, "startAvidDisplayAdSession")
                    .setStatic(AVID_AD_SESSION_MANAGER_PATH)
                    .addParam(Context.class, context)
                    .addParam(EXTERNAL_AVID_AD_SESSION_CONTEXT_PATH, avidAdSessionContext)
                    .execute();

            new Reflection.MethodBuilder(mAvidDisplayAdSession, "registerAdView")
                    .addParam(View.class, webView)
                    .addParam(Activity.class, activity)
                    .execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Avid start display session: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean startDeferredDisplaySession(@NonNull final Activity activity) {
        if (!isEnabled()) {
            return null;
        }

        if (mAvidDisplayAdSession == null) {
            MoPubLog.d("Avid DisplayAdSession unexpectedly null.");
            return false;
        }

        // Pre-reflection code:
        // AvidManager.getInstance().registerActivity(activity);
        // if (mAvidDisplayAdSession.getAvidDeferredAdSessionListener() != null) {
        //     mAvidDisplayAdSession.getAvidDeferredAdSessionListener().recordReadyEvent();
        // }

        try {
            final Object avidManager = new Reflection.MethodBuilder(null, "getInstance")
                    .setStatic(AVID_MANAGER_PATH)
                    .execute();

            new Reflection.MethodBuilder(avidManager, "registerActivity")
                    .addParam(Activity.class, activity)
                    .execute();

            final Object deferredAdSessionListener =
                    new Reflection.MethodBuilder(mAvidDisplayAdSession,
                            "getAvidDeferredAdSessionListener").execute();

            if (deferredAdSessionListener == null) {
                MoPubLog.d("Avid AdSessionListener unexpectedly null.");
                return false;
            }

            new Reflection.MethodBuilder(deferredAdSessionListener, "recordReadyEvent")
                    .execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Avid record deferred session: "
                    + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean endDisplaySession() {
        if (!isEnabled()) {
            return null;
        }

        if (mAvidDisplayAdSession == null) {
            MoPubLog.d("Avid DisplayAdSession unexpectedly null.");
            return false;
        }

        // Pre-reflection code:
        // mAvidDisplayAdSession.endSession();

        try {
            new Reflection.MethodBuilder(mAvidDisplayAdSession, "endSession").execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Avid end session: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean createVideoSession(@NonNull final Activity activity, @NonNull final View view,
            @NonNull final Set<String> buyerResources,
            @NonNull final Map<String, String> videoViewabilityTrackers) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(view);
        Preconditions.checkNotNull(buyerResources);
        Preconditions.checkNotNull(videoViewabilityTrackers);

        if (!isEnabled()) {
            return null;
        }

        // Pre-reflection code:
        // mAvidVideoAdSession = AvidAdSessionManager.startAvidManagedVideoAdSession(activity,
        //         (ExternalAvidAdSessionContext) getAvidAdSessionContextNonDeferred());
        // mAvidVideoAdSession.registerAdView(view, activity);
        // if (!TextUtils.isEmpty(videoViewabilityTrackers.get(AVID_KEY))) {
        //     mAvidVideoAdSession.injectJavaScriptResource(videoViewabilityTrackers.get(AVID_KEY));
        // }
        // for (final String buyerResource : buyerResources) {
        //     if (buyerResource != null) {
        //         mAvidVideoAdSession.injectJavaScriptResource(buyerResource);
        //     }
        // }

        try {
            mAvidVideoAdSession = new Reflection.MethodBuilder(null, "startAvidManagedVideoAdSession")
                    .setStatic(AVID_AD_SESSION_MANAGER_PATH)
                    .addParam(Context.class, activity)
                    .addParam(EXTERNAL_AVID_AD_SESSION_CONTEXT_PATH, getAvidAdSessionContextNonDeferred())
                    .execute();

            new Reflection.MethodBuilder(mAvidVideoAdSession, "registerAdView")
                    .addParam(View.class, view)
                    .addParam(Activity.class, activity)
                    .execute();

            if (!TextUtils.isEmpty(videoViewabilityTrackers.get(AVID_KEY))) {
                new Reflection.MethodBuilder(mAvidVideoAdSession, "injectJavaScriptResource")
                        .addParam(String.class, videoViewabilityTrackers.get(AVID_KEY))
                        .execute();
            }

            for (final String buyerResource : buyerResources) {
                if (!TextUtils.isEmpty(buyerResource)) {
                    new Reflection.MethodBuilder(mAvidVideoAdSession, "injectJavaScriptResource")
                            .addParam(String.class, buyerResource)
                            .execute();
                }
            }

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Avid start video session: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean registerVideoObstruction(@NonNull final View view) {
        Preconditions.checkNotNull(view);

        if (!isEnabled()) {
            return null;
        }

        if (mAvidVideoAdSession == null) {
            MoPubLog.d("Avid VideoAdSession unexpectedly null.");
            return false;
        }

        try {
            // Pre-reflection code:
            // mAvidVideoAdSession.registerFriendlyObstruction(view);

            new Reflection.MethodBuilder(mAvidVideoAdSession, "registerFriendlyObstruction")
                    .addParam(View.class, view)
                    .execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to register Avid video obstructions: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean onVideoPrepared(@NonNull final View playerView, final int duration) {
        Preconditions.checkNotNull(playerView);

        if (!isEnabled()) {
            return null;
        }

        // unimplemented by Avid
        return true;
    }

    @Override
    @Nullable
    public Boolean recordVideoEvent(@NonNull final VideoEvent event, final int playheadMillis) {
        Preconditions.checkNotNull(event);

        if (!isEnabled()) {
            return null;
        }

        if (mAvidVideoAdSession == null) {
            MoPubLog.d("Avid VideoAdSession unexpectedly null.");
            return false;
        }

        try {
            switch (event) {
                case AD_LOADED:
                case AD_STARTED:
                case AD_STOPPED:
                case AD_PAUSED:
                case AD_PLAYING:
                case AD_SKIPPED:
                case AD_IMPRESSED:
                case AD_CLICK_THRU:
                case AD_VIDEO_FIRST_QUARTILE:
                case AD_VIDEO_MIDPOINT:
                case AD_VIDEO_THIRD_QUARTILE:
                case AD_COMPLETE:
                    handleVideoEventReflection(event);
                    return true;

                case RECORD_AD_ERROR:
                    handleVideoEventReflection(event, "error");
                    return true;

                default:
                    MoPubLog.d("Unexpected video event type: " + event);
                    return false;
            }
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Avid video event for "
                    + event.getAvidMethodName() + ": " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean endVideoSession() {
        if (!isEnabled()) {
            return null;
        }

        if (mAvidVideoAdSession == null) {
            MoPubLog.d("Avid VideoAdSession unexpectedly null.");
            return false;
        }

        // Pre-reflection code:
        // mAvidVideoAdSession.endSession();

        try {
            new Reflection.MethodBuilder(mAvidVideoAdSession, "endSession").execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Avid end video session: " + e.getMessage());
            return false;
        }
    }

    private void handleVideoEventReflection(@NonNull final VideoEvent videoEvent) throws Exception {
        handleVideoEventReflection(videoEvent, null);
    }

    private void handleVideoEventReflection(@NonNull final VideoEvent videoEvent,
            @Nullable final String message) throws Exception {
        // Pre-reflection code:
        // mAvidVideoAdSession.getAvidVideoPlaybackListener().<videoEventMethodName>();

        final Object playbackListener =
                new Reflection.MethodBuilder(mAvidVideoAdSession, "getAvidVideoPlaybackListener")
                .execute();

        Reflection.MethodBuilder methodBuilder =
                new Reflection.MethodBuilder(playbackListener, videoEvent.getAvidMethodName());

        if (!TextUtils.isEmpty(message)) {
            methodBuilder.addParam(String.class, message);
        }

        methodBuilder.execute();
    }
}
