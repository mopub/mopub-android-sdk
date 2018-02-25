package com.mopub.common;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.webkit.WebView;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Reflection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// API documentation: https://drive.google.com/drive/folders/0B8U0thTyT1GGLUlweWRVMXk1Qlk
class MoatViewabilitySession implements ExternalViewabilitySession {
    private static final String MOAT_FACTORY_PATH = "com.moat.analytics.mobile.mpub.MoatFactory";
    private static final String MOAT_OPTIONS_PATH = "com.moat.analytics.mobile.mpub.MoatOptions";
    private static final String MOAT_ANALYTICS_PATH = "com.moat.analytics.mobile.mpub.MoatAnalytics";
    private static final String MOAT_AD_EVENT_PATH = "com.moat.analytics.mobile.mpub.MoatAdEvent";
    private static final String MOAT_AD_EVENT_TYPE_PATH = "com.moat.analytics.mobile.mpub.MoatAdEventType";
    private static final String MOAT_REACTIVE_VIDEO_TRACKER_PLUGIN_PATH = "com.moat.analytics.mobile.mpub.ReactiveVideoTrackerPlugin";
    private static final String MOAT_PLUGIN_PATH = "com.moat.analytics.mobile.mpub.MoatPlugin";

    private static final String PARTNER_CODE_KEY = "partnerCode";
    // MoPub's partner identifier with Moat. Partner code is normally parsed from the video
    // viewability tracking URL, but in case of error, this default value is used instead.
    private static final String DEFAULT_PARTNER_CODE = "mopubinapphtmvideo468906546585";
    private static final String MOAT_KEY = "moat";
    private static final String MOAT_VAST_IDS_KEY = "zMoatVASTIDs";

    private static Boolean sIsViewabilityEnabledViaReflection;
    private static boolean sIsVendorDisabled;
    private static boolean sWasInitialized = false;

    private static final Map<String, String> QUERY_PARAM_MAPPING = new HashMap<String, String>();
    static {
        QUERY_PARAM_MAPPING.put("moatClientLevel1", "level1");
        QUERY_PARAM_MAPPING.put("moatClientLevel2", "level2");
        QUERY_PARAM_MAPPING.put("moatClientLevel3", "level3");
        QUERY_PARAM_MAPPING.put("moatClientLevel4", "level4");
        QUERY_PARAM_MAPPING.put("moatClientSlicer1", "slicer1");
        QUERY_PARAM_MAPPING.put("moatClientSlicer2", "slicer2");
    }

    @Nullable private Object mMoatWebAdTracker;
    @Nullable private Object mMoatVideoTracker;
    @NonNull private Map<String, String> mAdIds = new HashMap<String, String>();
    private boolean mWasVideoPrepared;

    static boolean isEnabled() {
        return !sIsVendorDisabled && isViewabilityEnabledViaReflection();
    }

    static void disable() {
        sIsVendorDisabled = true;
    }

    private static boolean isViewabilityEnabledViaReflection() {
        if (sIsViewabilityEnabledViaReflection == null) {
            sIsViewabilityEnabledViaReflection = Reflection.classFound(MOAT_FACTORY_PATH);
            MoPubLog.d("Moat is "
                    + (sIsViewabilityEnabledViaReflection ? "" : "un")
                    + "available via reflection.");
        }

        return sIsViewabilityEnabledViaReflection;
    }

    @Override
    @NonNull
    public String getName() {
        return "Moat";
    }

    @Override
    @Nullable
    public Boolean initialize(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        if (!isEnabled()) {
            return null;
        }

        if (sWasInitialized) {
            return true;
        }

        final Application application;
        if (context instanceof Activity) {
            application = ((Activity) context).getApplication();
        } else {
            try {
                application = (Application) context.getApplicationContext();
            } catch (ClassCastException e) {
                MoPubLog.d("Unable to initialize Moat, error obtaining application context.");
                return false;
            }
        }

        // Pre-reflection code:
        // final MoatOptions options = new MoatOptions();
        // options.disableAdIdCollection = true;
        // options.disableLocationServices = true;
        // MoatAnalytics.getInstance().start(options, application);

        try {
            Object moatOptions = Reflection.instantiateClassWithEmptyConstructor(MOAT_OPTIONS_PATH,
                    Object.class);

            moatOptions.getClass().getField("disableAdIdCollection")
                    .setBoolean(moatOptions, true);

            moatOptions.getClass().getField("disableLocationServices")
                    .setBoolean(moatOptions, true);

            Object moatAnalytics = new Reflection.MethodBuilder(null, "getInstance")
                    .setStatic(MOAT_ANALYTICS_PATH)
                    .execute();

            new Reflection.MethodBuilder(moatAnalytics, "start")
                    .addParam(MOAT_OPTIONS_PATH, moatOptions)
                    .addParam(Application.class, application)
                    .execute();

            sWasInitialized = true;
            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to initialize Moat: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean invalidate() {
        if (!isEnabled()) {
            return null;
        }

        mMoatWebAdTracker = null;
        mMoatVideoTracker = null;
        mAdIds.clear();

        return true;
    }

    @Override
    @Nullable
    public Boolean createDisplaySession(@NonNull final Context context,
            @NonNull final WebView webView, boolean isDeferred) {
        Preconditions.checkNotNull(context);

        if (!isEnabled()) {
            return null;
        }

        // Pre-reflection code:
        // mMoatWebAdTracker = MoatFactory.create().createWebAdTracker(webView);
        // if (!isDeferred) {
        //     mMoatWebAdTracker.startTracking();
        // }

        try {
            Object moatFactory = new Reflection.MethodBuilder(null, "create")
                    .setStatic(MOAT_FACTORY_PATH)
                    .execute();

            mMoatWebAdTracker = new Reflection.MethodBuilder(moatFactory, "createWebAdTracker")
                    .addParam(WebView.class, webView)
                    .execute();

            // If we're not dealing with a deferred session, start tracking now
            if (!isDeferred) {
                new Reflection.MethodBuilder(mMoatWebAdTracker, "startTracking").execute();
            }

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Moat start display session: "
                    + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean startDeferredDisplaySession(@NonNull final Activity activity) {
        if (!isEnabled()) {
            return null;
        }

        if (mMoatWebAdTracker == null) {
            MoPubLog.d("MoatWebAdTracker unexpectedly null.");
            return false;
        }

        // Pre-reflection code:
        // mMoatWebAdTracker.startTracking();

        try {
            new Reflection.MethodBuilder(mMoatWebAdTracker, "startTracking").execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to record deferred display session for Moat: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean endDisplaySession() {
        if (!isEnabled()) {
            return null;
        }

        if (mMoatWebAdTracker == null) {
            MoPubLog.d("Moat WebAdTracker unexpectedly null.");
            return false;
        }

        // Pre-reflection code:
        // mMoatWebAdTracker.stopTracking();

        try {
            new Reflection.MethodBuilder(mMoatWebAdTracker, "stopTracking").execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Moat end session: " + e.getMessage());
        }

        return false;
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

        updateAdIdsFromUrlStringAndBuyerResources(videoViewabilityTrackers.get(MOAT_KEY),
                buyerResources);

        String partnerCode = mAdIds.get(PARTNER_CODE_KEY);
        if (TextUtils.isEmpty(partnerCode)) {
            MoPubLog.d("partnerCode was empty when starting Moat video session");
            return false;
        }

        // Pre-reflection code:
        // MoatPlugin moatPlugin = new ReactiveVideoTrackerPlugin(partnerCode);
        // mMoatVideoTracker = MoatFactory.create().createCustomTracker(moatPlugin);

        try {
            final Object moatPlugin = Reflection.instantiateClassWithConstructor(
                    MOAT_REACTIVE_VIDEO_TRACKER_PLUGIN_PATH, Object.class,
                    new Class[]{String.class}, new Object[]{partnerCode});

            final Object moatFactory = new Reflection.MethodBuilder(null, "create")
                    .setStatic(MOAT_FACTORY_PATH)
                    .execute();

            mMoatVideoTracker = new Reflection.MethodBuilder(moatFactory, "createCustomTracker")
                    .addParam(MOAT_PLUGIN_PATH, moatPlugin)
                    .execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Moat start video session: " + e.getMessage());
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

        // unimplemented by Moat
        return true;
    }

    @Override
    @Nullable
    public Boolean onVideoPrepared(@NonNull final View playerView, final int duration) {
        Preconditions.checkNotNull(playerView);

        if (!isEnabled()) {
            return null;
        }

        if (mMoatVideoTracker == null) {
            MoPubLog.d("Moat VideoAdTracker unexpectedly null.");
            return false;
        }

        if (mWasVideoPrepared) {
            return false;
        }

        // Pre-reflection code:
        // mMoatVideoTracker.trackVideoAd(mAdIds, duration, playerView);

        try {
            new Reflection.MethodBuilder(mMoatVideoTracker, "trackVideoAd")
                    .addParam(Map.class, mAdIds)
                    .addParam(Integer.class, duration)
                    .addParam(View.class, playerView)
                    .execute();
            mWasVideoPrepared = true;
            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Moat onVideoPrepared: " + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean recordVideoEvent(@NonNull final VideoEvent event, final int playheadMillis) {
        Preconditions.checkNotNull(event);

        if (!isEnabled()) {
            return null;
        }

        if (mMoatVideoTracker == null) {
            MoPubLog.d("Moat VideoAdTracker unexpectedly null.");
            return false;
        }

        try {
            switch (event) {
                case AD_STARTED:
                case AD_STOPPED:
                case AD_PAUSED:
                case AD_PLAYING:
                case AD_SKIPPED:
                case AD_VIDEO_FIRST_QUARTILE:
                case AD_VIDEO_MIDPOINT:
                case AD_VIDEO_THIRD_QUARTILE:
                case AD_COMPLETE:
                    handleVideoEventReflection(event, playheadMillis);
                    return true;

                case AD_LOADED:
                case AD_IMPRESSED:
                case AD_CLICK_THRU:
                case RECORD_AD_ERROR:
                    // unimplemented
                    return null;

                default:
                    MoPubLog.d("Unexpected video event: " + event.getMoatEnumName());
                    return false;
            }
        } catch (Exception e) {
            MoPubLog.d("Video event " + event.getMoatEnumName() + " failed. "
                    + e.getMessage());
            return false;
        }
    }

    @Override
    @Nullable
    public Boolean endVideoSession() {
        if (!isEnabled()) {
            return null;
        }

        if (mMoatVideoTracker == null) {
            MoPubLog.d("Moat VideoAdTracker unexpectedly null.");
            return false;
        }

        // Pre-reflection code:
        // mMoatVideoTracker.stopTracking();

        try {
            new Reflection.MethodBuilder(mMoatVideoTracker, "stopTracking").execute();

            return true;
        } catch (Exception e) {
            MoPubLog.d("Unable to execute Moat end video session: " + e.getMessage());
            return false;
        }
    }

    /**
     * Generates the adIds map from the video viewability tracking URL and any additional buyer tag
     * resources.
     *
     * @param urlString Used to gather partnerCode and relevant level/slicer information.
     * Example: https://z.moatads.com/mopubappdisplay698212075271/moatad.js#moatClientLevel1=appname&moatClientLevel2=adunit&moatClientLevel3=creativetype&moatClientSlicer1=adformat
     *
     * @param buyerResources Moat buyer-tag impression pixels.
     *
     * Example output adIds map:
     * {
     *     "level1": “appname",
     *     "level2": "adunit",
     *     "level3": "creativetype",
     *     "slicer1": "adformat",
     *     "partnerCode": "mopubappdisplay698212075271",
     *     "zMoatVASTIDs": "<ViewableImpression id="${BUYER_AD_SERVER_MACRO[S]}"><![CDATA[https://px.moatads.com/pixel.gif?moatPartnerCode=${MOAT_PARTNER_CODE}]]</ViewableImpression>"
     * }
     */
    private void updateAdIdsFromUrlStringAndBuyerResources(@Nullable final String urlString,
            @Nullable final Set<String> buyerResources) {
        mAdIds.clear();
        mAdIds.put(PARTNER_CODE_KEY, DEFAULT_PARTNER_CODE);
        mAdIds.put(MOAT_VAST_IDS_KEY, TextUtils.join(";", buyerResources));

        if (TextUtils.isEmpty(urlString)) {
            return;
        }

        final Uri uri = Uri.parse(urlString);

        final List<String> pathSegments = uri.getPathSegments();
        // If a partnerCode is parsed from the viewability tracking URL, prefer to use that.
        // Otherwise fallback to the MoPub default that was already added to the map.
        if (pathSegments.size() > 0 && !TextUtils.isEmpty(pathSegments.get(0))) {
            mAdIds.put(PARTNER_CODE_KEY, pathSegments.get(0));
        }

        final String fragment = uri.getFragment();
        if (!TextUtils.isEmpty(fragment)) {
            for (final String fragmentPairs : fragment.split("&")) {
                final String[] fragmentPair = fragmentPairs.split("=");
                if (fragmentPair.length < 2) {
                    continue;
                }

                final String fragmentKey = fragmentPair[0];
                final String fragmentValue = fragmentPair[1];
                if (TextUtils.isEmpty(fragmentKey) || TextUtils.isEmpty(fragmentValue)) {
                    continue;
                }

                if (QUERY_PARAM_MAPPING.containsKey(fragmentKey)) {
                    mAdIds.put(QUERY_PARAM_MAPPING.get(fragmentKey), fragmentValue);
                }
            }
        }
    }

    private boolean handleVideoEventReflection(@NonNull VideoEvent videoEvent,
            final int playheadMillis) throws Exception {
        if (videoEvent.getMoatEnumName() == null) {
            return false;
        }

        // Pre-reflection code:
        // MoatAdEvent event = new MoatAdEventType(<moatAdEventType>, playhead);
        // mMoatVideoTracker.dispatchEvent(event);

        final Class<?> clazz = Class.forName(MOAT_AD_EVENT_TYPE_PATH);
        final Enum<?> adEventTypeEnum = Enum.valueOf(clazz.asSubclass(Enum.class),
                videoEvent.getMoatEnumName());

        final Object moatAdEvent = Reflection.instantiateClassWithConstructor(
                MOAT_AD_EVENT_PATH, Object.class, new Class[]{clazz, Integer.class},
                new Object[]{adEventTypeEnum, playheadMillis});

        new Reflection.MethodBuilder(mMoatVideoTracker, "dispatchEvent")
                .addParam(MOAT_AD_EVENT_PATH, moatAdEvent)
                .execute();

        return true;
    }
}
