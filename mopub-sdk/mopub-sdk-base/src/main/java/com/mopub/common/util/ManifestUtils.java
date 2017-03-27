package com.mopub.common.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.widget.Toast;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import java.util.ArrayList;
import java.util.List;

public class ManifestUtils {
    private ManifestUtils() {}

    private static final String MOPUB_ACTIVITY = "com.mopub.mobileads.MoPubActivity";
    private static final String MRAID_ACTIVITY = "com.mopub.mobileads.MraidActivity";
    private static final String REWARDED_MRAID_ACTIVITY =
            "com.mopub.mobileads.RewardedMraidActivity";
    private static final List<Class<? extends Activity>> REQUIRED_WEB_VIEW_SDK_ACTIVITIES;
    private static FlagCheckUtil sFlagCheckUtil = new FlagCheckUtil();

    /**
     * This class maintains two different lists of required Activity permissions,
     * for the WebView and Native SDKs.
     */
    static {
        REQUIRED_WEB_VIEW_SDK_ACTIVITIES = new ArrayList<Class<? extends Activity>>(4);
        // As a convenience, full class paths are provided here, in case the MoPub SDK was imported
        // incorrectly and these files were left out.
        try {
            final Class moPubActivityClass = Class.forName(MOPUB_ACTIVITY);
            final Class mraidActivityClass = Class.forName(MRAID_ACTIVITY);
            final Class rewardedMraidActivityClass = Class.forName(REWARDED_MRAID_ACTIVITY);
            REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(moPubActivityClass);
            REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(mraidActivityClass);
            REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(rewardedMraidActivityClass);
        } catch (ClassNotFoundException e) {
            MoPubLog.i("ManifestUtils running without interstitial module");
        }

        REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(com.mopub.mobileads.MraidVideoPlayerActivity.class);
        REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(com.mopub.common.MoPubBrowser.class);
    }

    private static final List<Class<? extends Activity>> REQUIRED_NATIVE_SDK_ACTIVITIES;
    static {
        REQUIRED_NATIVE_SDK_ACTIVITIES = new ArrayList<Class<? extends Activity>>(1);
        REQUIRED_NATIVE_SDK_ACTIVITIES.add(com.mopub.common.MoPubBrowser.class);
    }

    public static void checkWebViewActivitiesDeclared(@NonNull final Context context) {
        if (!Preconditions.NoThrow.checkNotNull(context, "context is not allowed to be null")) {
            return;
        }

        displayWarningForMissingActivities(context, REQUIRED_WEB_VIEW_SDK_ACTIVITIES);
        displayWarningForMisconfiguredActivities(context, REQUIRED_WEB_VIEW_SDK_ACTIVITIES);
    }

    public static void checkNativeActivitiesDeclared(@NonNull final Context context) {
        if (!Preconditions.NoThrow.checkNotNull(context, "context is not allowed to be null")) {
            return;
        }

        displayWarningForMissingActivities(context, REQUIRED_NATIVE_SDK_ACTIVITIES);
        displayWarningForMisconfiguredActivities(context, REQUIRED_NATIVE_SDK_ACTIVITIES);
    }

    /**
     * This method is intended to display a warning to developers when they have accidentally
     * omitted Activity declarations in their application's AndroidManifest.
     * Calling this when there are inadequate permissions will always Log a warning to the
     * developer, and if the the application is debuggable, it will also display a Toast.
     */
    @VisibleForTesting
    static void displayWarningForMissingActivities(@NonNull final Context context,
            @NonNull final List<Class<? extends Activity>> requiredActivities) {

        final List<Class<? extends Activity>> undeclaredActivities =
                filterDeclaredActivities(context, requiredActivities, false);

        if (undeclaredActivities.isEmpty()) {
            return;
        }

        logWarningToast(context);

        // Regardless, log a warning
        logMissingActivities(undeclaredActivities);
    }

    /**
     * This method is intended to display a warning to developers when they have accidentally
     * omitted configChanges values from Activity declarations in their application's AndroidManifest.
     * Calling this when there are inadequate permissions will always Log a warning to the
     * developer, and if the the application is debuggable, it will also display a Toast.
     */
    @VisibleForTesting
    static void displayWarningForMisconfiguredActivities(@NonNull final Context context,
            @NonNull final List<Class<? extends Activity>> requiredActivities) {

        final List<Class<? extends Activity>> declaredActivities =
                filterDeclaredActivities(context, requiredActivities, true);
        final List<Class<? extends Activity>> misconfiguredActivities =
                getMisconfiguredActivities(context, declaredActivities);

        if (misconfiguredActivities.isEmpty()) {
            return;
        }

        logWarningToast(context);

        // Regardless, log a warning
        logMisconfiguredActivities(context, misconfiguredActivities);
    }

    public static boolean isDebuggable(@NonNull final Context context) {
        final int applicationFlags = context.getApplicationInfo().flags;
        return Utils.bitMaskContainsFlag(applicationFlags, ApplicationInfo.FLAG_DEBUGGABLE);
    }

    /**
     * Filters in activities to be returned based on matching their declaration state
     * in the Android Manifest with the isDeclared param.
     *
     * @param context the context
     * @param requiredActivities activities to filter against
     * @param isDeclared desired declaration state of activities in Android Manifest to be returned
     * @return the list of filtered in activities
     */
    private static List<Class<? extends Activity>> filterDeclaredActivities(@NonNull final Context context,
            @NonNull final List<Class<? extends Activity>> requiredActivities,
            final boolean isDeclared) {
        final List<Class<? extends Activity>> activities =
                new ArrayList<Class<? extends Activity>>();

        for (final Class<? extends Activity> activityClass : requiredActivities) {
            final Intent intent = new Intent(context, activityClass);

            if (Intents.deviceCanHandleIntent(context, intent) == isDeclared) {
                activities.add(activityClass);
            }
        }

        return activities;
    }

    @TargetApi(13)
    private static List<Class<? extends Activity>> getMisconfiguredActivities(@NonNull final Context context,
            @NonNull final List<Class<? extends Activity>> activities) {
        final List<Class<? extends Activity>> misconfiguredActivities =
                new ArrayList<Class<? extends Activity>>();

        for (final Class<? extends Activity> activity : activities) {
            ActivityConfigChanges activityConfigChanges;
            try {
                activityConfigChanges = getActivityConfigChanges(context, activity);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }

            if (!activityConfigChanges.hasKeyboardHidden || !activityConfigChanges.hasOrientation || !activityConfigChanges.hasScreenSize) {
                misconfiguredActivities.add(activity);
            }
        }

        return misconfiguredActivities;
    }

    private static void logMissingActivities(@NonNull final List<Class<? extends Activity>> undeclaredActivities) {
        final StringBuilder stringBuilder =
                new StringBuilder("AndroidManifest permissions for the following required MoPub activities are missing:\n");

        for (final Class<? extends Activity> activity : undeclaredActivities) {
            stringBuilder.append("\n\t").append(activity.getName());
        }
        stringBuilder.append("\n\nPlease update your manifest to include them.");

        MoPubLog.w(stringBuilder.toString());
    }

    private static void logMisconfiguredActivities(@NonNull Context context,
            @NonNull final List<Class<? extends Activity>> misconfiguredActivities) {
        final StringBuilder stringBuilder =
                new StringBuilder("In AndroidManifest, the android:configChanges param is missing values for the following MoPub activities:\n");

        for (final Class<? extends Activity> activity: misconfiguredActivities) {

            ActivityConfigChanges activityConfigChanges;
            try {
                activityConfigChanges = getActivityConfigChanges(context, activity);
            } catch (PackageManager.NameNotFoundException e) {
                continue;
            }

            if (!activityConfigChanges.hasKeyboardHidden) {
                stringBuilder.append("\n\tThe android:configChanges param for activity " + activity.getName() + " must include keyboardHidden.");
            }
            if (!activityConfigChanges.hasOrientation) {
                stringBuilder.append("\n\tThe android:configChanges param for activity " + activity.getName() + " must include orientation.");
            }
            if (!activityConfigChanges.hasScreenSize) {
                stringBuilder.append("\n\tThe android:configChanges param for activity " + activity.getName() + " must include screenSize.");
            }
        }

        stringBuilder.append("\n\nPlease update your manifest to include them.");

        MoPubLog.w(stringBuilder.toString());
    }

    private static ActivityConfigChanges getActivityConfigChanges(@NonNull Context context,
            @NonNull Class<? extends Activity> activity) throws PackageManager.NameNotFoundException {
        ActivityInfo activityInfo;

        // This line can throw NameNotFoundException but we don't expect it to happen since we
        // should only be operating on declared activities
        activityInfo = context.getPackageManager()
                .getActivityInfo(new ComponentName(context, activity.getName()), 0);

        ActivityConfigChanges activityConfigChanges = new ActivityConfigChanges();
        activityConfigChanges.hasKeyboardHidden = sFlagCheckUtil.hasFlag(activity, activityInfo.configChanges, ActivityInfo.CONFIG_KEYBOARD_HIDDEN);
        activityConfigChanges.hasOrientation = sFlagCheckUtil.hasFlag(activity, activityInfo.configChanges, ActivityInfo.CONFIG_ORIENTATION);
        activityConfigChanges.hasScreenSize = true;

        activityConfigChanges.hasScreenSize = sFlagCheckUtil.hasFlag(activity, activityInfo.configChanges, ActivityInfo.CONFIG_SCREEN_SIZE);

        return activityConfigChanges;
    }

    private static void logWarningToast(@NonNull final Context context) {
        // If the application is debuggable, display a loud toast
        if (isDebuggable(context)) {
            final String message = "ERROR: YOUR MOPUB INTEGRATION IS INCOMPLETE.\n" +
                    "Check logcat and update your AndroidManifest.xml with the correct activities and configuration.";
            final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.FILL_HORIZONTAL, 0, 0);
            toast.show();
        }
    }

    private static class ActivityConfigChanges {
        public boolean hasKeyboardHidden;
        public boolean hasOrientation;
        public boolean hasScreenSize;
    }

    @VisibleForTesting
    @Deprecated // for testing
    static List<Class<? extends Activity>> getRequiredWebViewSdkActivities() {
        return REQUIRED_WEB_VIEW_SDK_ACTIVITIES;
    }

    @VisibleForTesting
    @Deprecated // for testing
    static List<Class<? extends Activity>> getRequiredNativeSdkActivities() {
        return REQUIRED_NATIVE_SDK_ACTIVITIES;
    }

    @VisibleForTesting
    @Deprecated // for testing
    static void setFlagCheckUtil(final FlagCheckUtil flagCheckUtil) {
        sFlagCheckUtil = flagCheckUtil;
    }

    static class FlagCheckUtil {
        // We're only passing in the Class param here to ease testing and
        // allow mocks to match on it
        public boolean hasFlag(@SuppressWarnings("unused") Class clazz,
                int bitMask,
                int flag) {
            return Utils.bitMaskContainsFlag(bitMask, flag);
        }
    }
}
