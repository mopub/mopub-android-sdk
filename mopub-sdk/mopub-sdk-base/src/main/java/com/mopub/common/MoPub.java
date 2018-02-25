package com.mopub.common;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.mopub.common.ExternalViewabilitySessionManager.ViewabilityVendor;

public class MoPub {
    public static final String SDK_VERSION = "4.20.0";

    public enum LocationAwareness { NORMAL, TRUNCATED, DISABLED }

    /**
     * Browser agent to handle URIs with scheme HTTP or HTTPS
     */
    public enum BrowserAgent {
        /**
         * MoPub's in-app browser
         */
        IN_APP,

        /**
         * Default browser application on device
         */
        NATIVE;

        /**
         * Maps header value from MoPub's AdServer to browser agent:
         * 0 is MoPub's in-app browser (IN_APP), and 1 is device's default browser (NATIVE).
         * For null or all other undefined values, returns default browser agent IN_APP.
         * @param browserAgent Integer header value from MoPub's AdServer.
         * @return IN_APP for 0, NATIVE for 1, and IN_APP for null or all other undefined values.
         */
        @NonNull
        public static BrowserAgent fromHeader(@Nullable final Integer browserAgent) {
            if (browserAgent == null) {
                return IN_APP;
            }

            return browserAgent == 1 ? NATIVE : IN_APP;
        }
    }

    private static final String MOPUB_REWARDED_VIDEOS =
            "com.mopub.mobileads.MoPubRewardedVideos";
    private static final String MOPUB_REWARDED_VIDEO_MANAGER =
            "com.mopub.mobileads.MoPubRewardedVideoManager";
    private static final String MOPUB_REWARDED_VIDEO_LISTENER =
            "com.mopub.mobileads.MoPubRewardedVideoListener";
    private static final String MOPUB_REWARDED_VIDEO_MANAGER_REQUEST_PARAMETERS =
            "com.mopub.mobileads.MoPubRewardedVideoManager$RequestParameters";

    private static final int DEFAULT_LOCATION_PRECISION = 6;
    private static final long DEFAULT_LOCATION_REFRESH_TIME_MILLIS = 60 * 1000;

    @NonNull private static volatile LocationAwareness sLocationAwareness = LocationAwareness.NORMAL;
    private static volatile int sLocationPrecision = DEFAULT_LOCATION_PRECISION;
    private static volatile long sMinimumLocationRefreshTimeMillis = DEFAULT_LOCATION_REFRESH_TIME_MILLIS;
    @NonNull private static volatile BrowserAgent sBrowserAgent = BrowserAgent.IN_APP;
    private static volatile boolean sIsBrowserAgentOverriddenByClient = false;
    private static boolean sSearchedForUpdateActivityMethod = false;
    @Nullable private static Method sUpdateActivityMethod;

    @NonNull
    public static LocationAwareness getLocationAwareness() {
        Preconditions.checkNotNull(sLocationAwareness);

        return sLocationAwareness;
    }

    public static void setLocationAwareness(@NonNull final LocationAwareness locationAwareness) {
        Preconditions.checkNotNull(locationAwareness);

        sLocationAwareness = locationAwareness;
    }

    public static int getLocationPrecision() {
        return sLocationPrecision;
    }

    /**
     * Sets the precision to use when the SDK's location awareness is set
     * to {@link com.mopub.common.MoPub.LocationAwareness#TRUNCATED}.
     */
    public static void setLocationPrecision(int precision) {
        sLocationPrecision = Math.min(Math.max(0, precision), DEFAULT_LOCATION_PRECISION);
    }

    public static void setMinimumLocationRefreshTimeMillis(
            final long minimumLocationRefreshTimeMillis) {
        sMinimumLocationRefreshTimeMillis = minimumLocationRefreshTimeMillis;
    }

    public static long getMinimumLocationRefreshTimeMillis() {
        return sMinimumLocationRefreshTimeMillis;
    }

    public static void setBrowserAgent(@NonNull final BrowserAgent browserAgent) {
        Preconditions.checkNotNull(browserAgent);

        sBrowserAgent = browserAgent;
        sIsBrowserAgentOverriddenByClient = true;
    }

    public static void setBrowserAgentFromAdServer(
            @NonNull final BrowserAgent adServerBrowserAgent) {
        Preconditions.checkNotNull(adServerBrowserAgent);

        if (sIsBrowserAgentOverriddenByClient) {
            MoPubLog.w("Browser agent already overridden by client with value " + sBrowserAgent);
        } else {
            sBrowserAgent = adServerBrowserAgent;
        }
    }

    @NonNull
    public static BrowserAgent getBrowserAgent() {
        Preconditions.checkNotNull(sBrowserAgent);

        return sBrowserAgent;
    }

    @VisibleForTesting
    static boolean isBrowserAgentOverriddenByClient() {
        return sIsBrowserAgentOverriddenByClient;
    }

    @VisibleForTesting
    @Deprecated
    public static void resetBrowserAgent() {
        sBrowserAgent = BrowserAgent.IN_APP;
        sIsBrowserAgentOverriddenByClient = false;
    }

    //////// MoPub LifecycleListener messages ////////

    public static void onCreate(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onCreate(activity);
        updateActivity(activity);
    }

    public static void onStart(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onStart(activity);
        updateActivity(activity);
    }

    public static void onPause(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onPause(activity);
    }

    public static void onResume(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onResume(activity);
        updateActivity(activity);
    }

    public static void onRestart(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onRestart(activity);
        updateActivity(activity);
    }

    public static void onStop(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onStop(activity);
    }

    public static void onDestroy(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onDestroy(activity);
    }

    public static void onBackPressed(@NonNull final Activity activity) {
        MoPubLifecycleManager.getInstance(activity).onBackPressed(activity);
    }

    public static void disableViewability(@NonNull final ViewabilityVendor vendor) {
        Preconditions.checkNotNull(vendor);

        vendor.disable();
    }

    ////////// MoPub RewardedVideoControl methods //////////
    // These methods have been deprecated as of release 4.9 due to SDK modularization. MoPub is
    // inside of the base module while MoPubRewardedVideos is inside of the rewarded video module.
    // MoPubRewardedVideos methods must now be called with reflection because the publisher
    // may have excluded the rewarded video module.


    /**
     * @deprecated As of release 4.9, use MoPubRewardedVideos#initializeRewardedVideo instead
     */
    @Deprecated
    public static void initializeRewardedVideo(@NonNull Activity activity, MediationSettings... mediationSettings) {
        try {
            new Reflection.MethodBuilder(null, "initializeRewardedVideo")
                    .setStatic(Class.forName(MOPUB_REWARDED_VIDEOS))
                    .addParam(Activity.class, activity)
                    .addParam(MediationSettings[].class, mediationSettings)
                    .execute();
        } catch (ClassNotFoundException e) {
            MoPubLog.w("initializeRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            MoPubLog.w("initializeRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            MoPubLog.e("Error while initializing rewarded video", e);
        }
    }

    @VisibleForTesting
    static void updateActivity(@NonNull Activity activity) {
        if (!sSearchedForUpdateActivityMethod) {
            sSearchedForUpdateActivityMethod = true;
            try {
                Class moPubRewardedVideoManagerClass = Class.forName(
                        MOPUB_REWARDED_VIDEO_MANAGER);
                sUpdateActivityMethod = Reflection.getDeclaredMethodWithTraversal(
                        moPubRewardedVideoManagerClass, "updateActivity", Activity.class);
            } catch (ClassNotFoundException e) {
                // rewarded video module not included
            } catch (NoSuchMethodException e) {
                // rewarded video module not included
            }
        }

        if (sUpdateActivityMethod != null) {
            try {
                sUpdateActivityMethod.invoke(null, activity);
            } catch (IllegalAccessException e) {
                MoPubLog.e("Error while attempting to access the update activity method - this " +
                        "should not have happened", e);
            } catch (InvocationTargetException e) {
                MoPubLog.e("Error while attempting to access the update activity method - this " +
                        "should not have happened", e);
            }
        }
    }

    /**
     * @deprecated As of release 4.9, use MoPubRewardedVideos#setRewardedVideoListener instead
     */
    @Deprecated
    public static void setRewardedVideoListener(@Nullable Object listener) {
        try {
            Class moPubRewardedVideoListenerClass = Class.forName(
                    MOPUB_REWARDED_VIDEO_LISTENER);
            new Reflection.MethodBuilder(null, "setRewardedVideoListener")
                    .setStatic(Class.forName(MOPUB_REWARDED_VIDEOS))
                    .addParam(moPubRewardedVideoListenerClass, listener)
                    .execute();
        } catch (ClassNotFoundException e) {
            MoPubLog.w("setRewardedVideoListener was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            MoPubLog.w("setRewardedVideoListener was called without the rewarded video module");
        } catch (Exception e) {
            MoPubLog.e("Error while setting rewarded video listener", e);
        }
    }

    /**
     * @deprecated As of release 4.9, use MoPubRewardedVideos#loadRewardedVideo instead
     */
    @Deprecated
    public static void loadRewardedVideo(@NonNull String adUnitId,
            @Nullable MediationSettings... mediationSettings) {
        MoPub.loadRewardedVideo(adUnitId, null, mediationSettings);
    }

    /**
     * @deprecated As of release 4.9, use MoPubRewardedVideos#loadRewardedVideo instead
     */
    @Deprecated
    public static void loadRewardedVideo(@NonNull String adUnitId,
            @Nullable Object requestParameters,
            @Nullable MediationSettings... mediationSettings) {
        try {
            Class requestParametersClass = Class.forName(
                    MOPUB_REWARDED_VIDEO_MANAGER_REQUEST_PARAMETERS);
            new Reflection.MethodBuilder(null, "loadRewardedVideo")
                    .setStatic(Class.forName(MOPUB_REWARDED_VIDEOS))
                    .addParam(String.class, adUnitId)
                    .addParam(requestParametersClass, requestParameters)
                    .addParam(MediationSettings[].class, mediationSettings)
                    .execute();
        } catch (ClassNotFoundException e) {
            MoPubLog.w("loadRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            MoPubLog.w("loadRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            MoPubLog.e("Error while loading rewarded video", e);
        }
    }

    /**
     * @deprecated As of release 4.9, use MoPubRewardedVideos#hasRewardedVideo instead
     */
    @Deprecated
    public static boolean hasRewardedVideo(@NonNull String adUnitId) {
        try {
            return (boolean) new Reflection.MethodBuilder(null, "hasRewardedVideo")
                    .setStatic(Class.forName(MOPUB_REWARDED_VIDEOS))
                    .addParam(String.class, adUnitId)
                    .execute();
        } catch (ClassNotFoundException e) {
            MoPubLog.w("hasRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            MoPubLog.w("hasRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            MoPubLog.e("Error while checking rewarded video", e);
        }
        return false;
    }

    /**
     * @deprecated As of release 4.9, use MoPubRewardedVideos#showRewardedVideo instead
     */
    @Deprecated
    public static void showRewardedVideo(@NonNull String adUnitId) {
        try {
            new Reflection.MethodBuilder(null, "showRewardedVideo")
                    .setStatic(Class.forName(MOPUB_REWARDED_VIDEOS))
                    .addParam(String.class, adUnitId)
                    .execute();
        } catch (ClassNotFoundException e) {
            MoPubLog.w("showRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            MoPubLog.w("showRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            MoPubLog.e("Error while showing rewarded video", e);
        }
    }
}
