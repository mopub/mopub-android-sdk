// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.util.Reflection;
import com.mopub.network.Networking;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static com.mopub.common.ExternalViewabilitySessionManager.ViewabilityVendor;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.INIT_STARTED;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.INIT_FINISHED;

public class MoPub {
    public static final String SDK_VERSION = "5.7.1";

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

    private static final int DEFAULT_LOCATION_PRECISION = 6;
    private static final long DEFAULT_LOCATION_REFRESH_TIME_MILLIS = 60 * 1000;

    @NonNull private static volatile LocationAwareness sLocationAwareness = LocationAwareness.NORMAL;
    private static volatile int sLocationPrecision = DEFAULT_LOCATION_PRECISION;
    private static volatile long sMinimumLocationRefreshTimeMillis = DEFAULT_LOCATION_REFRESH_TIME_MILLIS;
    @NonNull private static volatile BrowserAgent sBrowserAgent = BrowserAgent.IN_APP;
    private static volatile boolean sIsBrowserAgentOverriddenByClient = false;
    private static boolean sSearchedForUpdateActivityMethod = false;
    @Nullable private static Method sUpdateActivityMethod;
    private static boolean sSdkInitialized = false;
    private static boolean sSdkInitializing = false;
    private static AdapterConfigurationManager sAdapterConfigurationManager;
    private static PersonalInfoManager sPersonalInfoManager;

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
            MoPubLog.log(CUSTOM, "Browser agent already overridden by client with value " + sBrowserAgent);
        } else {
            sBrowserAgent = adServerBrowserAgent;
        }
    }

    @NonNull
    public static BrowserAgent getBrowserAgent() {
        Preconditions.checkNotNull(sBrowserAgent);

        return sBrowserAgent;
    }

    /**
     * Initializes the MoPub SDK. Call this before making any rewarded ads or advanced bidding
     * requests. This will do the rewarded video custom event initialization any number of times,
     * but the SDK itself can only be initialized once, and the rewarded ads module can only be
     * initialized once.
     *
     * @param context                   Recommended to be an activity context.
     *                                  Rewarded ads initialization requires an Activity.
     * @param sdkConfiguration          Configuration data to initialize the SDK.
     * @param sdkInitializationListener Callback for when SDK initialization finishes.
     */
    public static void initializeSdk(@NonNull final Context context,
            @NonNull final SdkConfiguration sdkConfiguration,
            @Nullable final SdkInitializationListener sdkInitializationListener) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(sdkConfiguration);

        MoPubLog.setLogLevel(sdkConfiguration.getLogLevel());

        MoPubLog.log(INIT_STARTED);
        MoPubLog.log(CUSTOM, "SDK initialize has been called with ad unit: " + sdkConfiguration.getAdUnitId());

        if (context instanceof Activity) {
            final Activity activity = (Activity) context;
            initializeRewardedVideo(activity, sdkConfiguration);
        }

        if (sSdkInitialized) {
            MoPubLog.log(CUSTOM, "MoPub SDK is already initialized");
            initializationFinished(sdkInitializationListener);
            return;
        }
        if (sSdkInitializing) {
            MoPubLog.log(CUSTOM, "MoPub SDK is currently initializing.");
            return;
        }

        if (Looper.getMainLooper() != Looper.myLooper()) {
            MoPubLog.log(CUSTOM, "MoPub can only be initialized on the main thread.");
            return;
        }

        sSdkInitializing = true;

        // Guarantees initialization of the request queue on the main thread.
        Networking.getRequestQueue(context);

        final InternalSdkInitializationListener internalSdkInitializationListener =
                new InternalSdkInitializationListener(sdkInitializationListener);

        final SdkInitializationListener compositeSdkInitializationListener =
                new CompositeSdkInitializationListener(internalSdkInitializationListener, 2);

        sPersonalInfoManager = new PersonalInfoManager(context, sdkConfiguration.getAdUnitId(),
                compositeSdkInitializationListener);
        sPersonalInfoManager.setAllowLegitimateInterest(sdkConfiguration.getLegitimateInterestAllowed());

        ClientMetadata.getInstance(context);

        sAdapterConfigurationManager = new AdapterConfigurationManager(compositeSdkInitializationListener);
        sAdapterConfigurationManager.initialize(context,
                sdkConfiguration.getAdapterConfigurationClasses(),
                sdkConfiguration.getMediatedNetworkConfigurations(),
                sdkConfiguration.getMoPubRequestOptions());
    }

    /**
     * @return true if SDK is initialized.
     */
    public static boolean isSdkInitialized() {
        return sSdkInitialized;
    }

    /**
     * Check this to see if you are allowed to collect personal user data.
     *
     * @return True if allowed, false otherwise.
     */
    public static boolean canCollectPersonalInformation() {
        return sPersonalInfoManager != null && sPersonalInfoManager.canCollectPersonalInformation();
    }

    /**
     * Set the allowance of legitimate interest.
     * This API can be used if you want to allow supported SDK networks to collect user information on the basis of legitimate interest.
     *
     * @param allowed should be true if legitimate interest is allowed. False if it isn't allowed.
     */
    public static void setAllowLegitimateInterest(final boolean allowed) {
        if (sPersonalInfoManager != null) {
            sPersonalInfoManager.setAllowLegitimateInterest(allowed);
        }
    }

    /**
     * Check this to see if legitimate interest is allowed.
     *
     * @return True if allowed, false otherwise.
     */
    public static boolean shouldAllowLegitimateInterest() {
        return sPersonalInfoManager != null && sPersonalInfoManager.shouldAllowLegitimateInterest();
    }

    @Nullable
    static String getAdvancedBiddingTokensJson(@NonNull final Context context) {
        Preconditions.checkNotNull(context);

        if (sAdapterConfigurationManager == null) {
            return null;
        }
        return sAdapterConfigurationManager.getTokensAsJsonString(context);
    }

    /**
     * Gets the consent manager for handling user data.
     *
     * @return A PersonalInfoManager that handles consent management.
     */
    @Nullable
    public static PersonalInfoManager getPersonalInformationManager() {
        return sPersonalInfoManager;
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

    @Nullable
    public static List<String> getAdapterConfigurationInfo() {
        final AdapterConfigurationManager configurationManager = sAdapterConfigurationManager;
        if (configurationManager != null) {
            return configurationManager.getAdapterConfigurationInfo();
        }
        return null;
    }

    private static void initializeRewardedVideo(@NonNull Activity activity, @NonNull SdkConfiguration sdkConfiguration) {
        Preconditions.checkNotNull(activity);
        Preconditions.checkNotNull(sdkConfiguration);

        try {
            new Reflection.MethodBuilder(null, "initializeRewardedVideo")
                    .setStatic(Class.forName(MOPUB_REWARDED_VIDEOS))
                    .setAccessible()
                    .addParam(Activity.class, activity)
                    .addParam(SdkConfiguration.class, sdkConfiguration).execute();
        } catch (ClassNotFoundException e) {
            MoPubLog.log(CUSTOM, "initializeRewardedVideo was called without the rewarded video module");
        } catch (NoSuchMethodException e) {
            MoPubLog.log(CUSTOM, "initializeRewardedVideo was called without the rewarded video module");
        } catch (Exception e) {
            MoPubLog.log(ERROR, "Error while initializing rewarded video", e);
        }
    }

    private static void initializationFinished(@Nullable final SdkInitializationListener sdkInitializationListener) {
        sSdkInitializing = false;
        sSdkInitialized = true;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (sdkInitializationListener != null) {
                    sdkInitializationListener.onInitializationFinished();
                }
            }
        });
    }

    private static class InternalSdkInitializationListener implements SdkInitializationListener {
        @Nullable
        private SdkInitializationListener mSdkInitializationListener;

        InternalSdkInitializationListener(@Nullable SdkInitializationListener sdkInitializationListener) {
            mSdkInitializationListener = sdkInitializationListener;
        }

        @Override
        public void onInitializationFinished() {
            final AdapterConfigurationManager adapterConfigurationManager = sAdapterConfigurationManager;
            if (adapterConfigurationManager != null) {
                MoPubLog.log(INIT_FINISHED, adapterConfigurationManager.getAdapterConfigurationInfo());
            }
            initializationFinished(mSdkInitializationListener);
            mSdkInitializationListener = null;
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
                MoPubLog.log(ERROR, "Error while attempting to access the update activity method - this " +
                        "should not have happened", e);
            } catch (InvocationTargetException e) {
                MoPubLog.log(ERROR, "Error while attempting to access the update activity method - this " +
                        "should not have happened", e);
            }
        }
    }

    @Deprecated
    @VisibleForTesting
    static void clearAdvancedBidders() {
        sAdapterConfigurationManager = null;
        sPersonalInfoManager = null;
        sSdkInitialized = false;
        sSdkInitializing = false;
    }

    @Deprecated
    @VisibleForTesting
    static void setPersonalInfoManager(@Nullable final PersonalInfoManager personalInfoManager) {
        sPersonalInfoManager = personalInfoManager;
    }
}
