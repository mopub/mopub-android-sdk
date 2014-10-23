package com.mopub.common.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.view.Gravity;
import android.widget.Toast;

import com.mopub.common.logging.MoPubLog;

import java.util.ArrayList;
import java.util.List;

public class ManifestUtils {
    private ManifestUtils() {}

    private static final List<Class<? extends Activity>> REQUIRED_WEB_VIEW_SDK_ACTIVITIES;
    static {
        REQUIRED_WEB_VIEW_SDK_ACTIVITIES = new ArrayList<Class<? extends Activity>>(4);
        // As a convenience, full class paths are provided here, in case the MoPub SDK was imported
        // incorrectly and these files were left out.
        REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(com.mopub.mobileads.MoPubActivity.class);
        REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(com.mopub.mobileads.MraidActivity.class);
        REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(com.mopub.mobileads.MraidVideoPlayerActivity.class);
        REQUIRED_WEB_VIEW_SDK_ACTIVITIES.add(com.mopub.common.MoPubBrowser.class);
    }

    private static final List<Class<? extends Activity>> REQUIRED_NATIVE_SDK_ACTIVITIES;
    static {
        REQUIRED_NATIVE_SDK_ACTIVITIES = new ArrayList<Class<? extends Activity>>(1);
        REQUIRED_NATIVE_SDK_ACTIVITIES.add(com.mopub.common.MoPubBrowser.class);
    }

    public static void checkWebViewActivitiesDeclared(final Context context) {
        displayWarningForMissingActivities(context, REQUIRED_WEB_VIEW_SDK_ACTIVITIES);
    }

    public static void checkNativeActivitiesDeclared(final Context context) {
        displayWarningForMissingActivities(context, REQUIRED_NATIVE_SDK_ACTIVITIES);
    }

    /**
     * This method is intended to display a warning to developers when they have accidentally
     * omitted Activity declarations in their application's AndroidManifest. This class maintains
     * two different lists of required Activity permissions, for the WebView and Native SDKs.
     * Calling this when there are inadequate permissions will always Log a warning to the
     * developer, and if the the application is debuggable, it will also display a Toast.
     */
    static void displayWarningForMissingActivities(final Context context,
            final List<Class<? extends Activity>> requiredActivities) {
        if (context == null) {
            return;
        }

        final List<String> undeclaredActivities = getUndeclaredActivities(context, requiredActivities);

        if (undeclaredActivities.isEmpty()) {
            return;
        }

        // If the application is debuggable, display a loud toast
        if (isDebuggable(context)) {
            final String message =  "ERROR: YOUR MOPUB INTEGRATION IS INCOMPLETE.\n" +
                    "Check logcat and update your AndroidManifest.xml with the correct activities.";
            final Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.FILL_HORIZONTAL, 0, 0);
            toast.show();
        }

        // Regardless, log a warning
        logMissingActivities(undeclaredActivities);
    }

    static boolean isDebuggable(final Context context) {
        if (context == null || context.getApplicationInfo() == null) {
            return false;
        }

        final int applicationFlags = context.getApplicationInfo().flags;

        return Utils.bitMaskContainsFlag(applicationFlags, ApplicationInfo.FLAG_DEBUGGABLE);
    }

    private static List<String> getUndeclaredActivities(final Context context,
            List<Class<? extends Activity>> requiredActivities) {
        final List<String> undeclaredActivities = new ArrayList<String>();

        for (final Class<? extends Activity> activityClass : requiredActivities) {
            final Intent intent = new Intent(context, activityClass);

            if (!IntentUtils.deviceCanHandleIntent(context, intent)) {
                undeclaredActivities.add(activityClass.getName());
            }
        }

        return undeclaredActivities;
    }

    private static void logMissingActivities(final List<String> undeclaredActivities) {
        final StringBuilder stringBuilder =
                new StringBuilder("AndroidManifest permissions for the following required MoPub activities are missing:\n");

        for (final String activity : undeclaredActivities) {
            stringBuilder.append("\n\t").append(activity);
        }
        stringBuilder.append("\n\nPlease update your manifest to include them.");

        MoPubLog.w(stringBuilder.toString());
    }

    @Deprecated // for testing
    static List<Class<? extends Activity>> getRequiredWebViewSdkActivities() {
        return REQUIRED_WEB_VIEW_SDK_ACTIVITIES;
    }

    @Deprecated // for testing
    static List<Class<? extends Activity>> getRequiredNativeSdkActivities() {
        return REQUIRED_NATIVE_SDK_ACTIVITIES;
    }
}
