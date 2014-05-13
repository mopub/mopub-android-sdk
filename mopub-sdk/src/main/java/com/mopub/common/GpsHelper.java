package com.mopub.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.mopub.common.factories.MethodBuilderFactory;

import static com.mopub.common.util.Reflection.MethodBuilder;
import static com.mopub.common.util.Reflection.classFound;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.ADMOB_BANNER;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.ADMOB_INTERSTITIAL;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.GOOGLE_PLAY_BANNER;
import static com.mopub.mobileads.AdTypeTranslator.CustomEventType.GOOGLE_PLAY_INTERSTITIAL;

public class GpsHelper {
    static public final int GOOGLE_PLAY_SUCCESS_CODE = 0;
    static public final String ADVERTISING_ID_KEY = "advertisingId";
    static public final String IS_LIMIT_AD_TRACKING_ENABLED_KEY = "isLimitAdTrackingEnabled";
    private static String sPlayServicesUtilClassName = "com.google.android.gms.common.GooglePlayServicesUtil";
    private static String sAdvertisingIdClientClassName = "com.google.android.gms.ads.identifier.AdvertisingIdClient";

    public interface GpsHelperListener {
        public void onFetchAdInfoCompleted();
    }

    static public CustomEventType convertAdMobToGooglePlayServices(final Context context, final CustomEventType customEventType) {
        // In both cases, only check if GooglePlayServices is available if absolutely necessary
        if (customEventType == ADMOB_BANNER &&
                classFound(GOOGLE_PLAY_BANNER.toString()) &&
                isGpsAvailable(context)) {
            return GOOGLE_PLAY_BANNER;
        } else if (customEventType == ADMOB_INTERSTITIAL &&
                classFound(GOOGLE_PLAY_INTERSTITIAL.toString()) &&
                isGpsAvailable(context)) {
            return GOOGLE_PLAY_INTERSTITIAL;
        }

        return customEventType;
    }

    static boolean isGpsAvailable(final Context context) {
        try {
            MethodBuilder methodBuilder = MethodBuilderFactory.create(null, "isGooglePlayServicesAvailable")
                    .setStatic(Class.forName(sPlayServicesUtilClassName))
                    .addParam(Context.class, context);

            Object result = methodBuilder.execute();

            return (result != null);
        } catch (Exception exception) {
            return false;
        }
    }

    static String getAdvertisingId(final Context context) {
        final String defaultValue = null;
        if (isGpsAvailable(context)) {
            return SharedPreferencesHelper.getSharedPreferences(context)
                    .getString(ADVERTISING_ID_KEY, defaultValue);
        } else {
            return defaultValue;
        }
    }

    static public boolean isLimitAdTrackingEnabled(Context context) {
        final boolean defaultValue = false;
        if (isGpsAvailable(context)) {
            return SharedPreferencesHelper.getSharedPreferences(context)
                    .getBoolean(IS_LIMIT_AD_TRACKING_ENABLED_KEY, defaultValue);
        } else {
            return defaultValue;
        }
    }

    static boolean isSharedPreferencesPopluated(final Context context) {
        SharedPreferences sharedPreferences = SharedPreferencesHelper.getSharedPreferences(context);
        return sharedPreferences.contains(ADVERTISING_ID_KEY) &&
                sharedPreferences.contains(IS_LIMIT_AD_TRACKING_ENABLED_KEY);
    }

    static public void asyncFetchAdvertisingInfoIfNotCached(final Context context, final GpsHelperListener gpsHelperListener) {
        // This method guarantees that the Google Play Services (GPS) advertising info will
        // be populated if GPS is available and the ad info is not already cached
        // The above will happen before the callback is run
        if (isGpsAvailable(context) && !isSharedPreferencesPopluated(context)) {
            asyncFetchAdvertisingInfo(context, gpsHelperListener);
        } else {
            gpsHelperListener.onFetchAdInfoCompleted();
        }
    }

    static public void asyncFetchAdvertisingInfo(final Context context) {
        asyncFetchAdvertisingInfo(context, null);
    }

    static public void asyncFetchAdvertisingInfo(final Context context, final GpsHelperListener gpsHelperListener) {
        if (!classFound(sAdvertisingIdClientClassName)) {
            if (gpsHelperListener != null) {
                gpsHelperListener.onFetchAdInfoCompleted();
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MethodBuilder methodBuilder = MethodBuilderFactory.create(null, "getAdvertisingIdInfo")
                            .setStatic(Class.forName(sAdvertisingIdClientClassName))
                            .addParam(Context.class, context);

                    Object adInfo = methodBuilder.execute();

                    if (adInfo != null) {
                        updateSharedPreferences(context, adInfo);
                    }
                } catch (Exception exception) {
                    Log.d("MoPub", "Unable to obtain AdvertisingIdClient.getAdvertisingIdInfo()");
                } finally {
                    if (gpsHelperListener != null) {
                        gpsHelperListener.onFetchAdInfoCompleted();
                    }
                }
            }
        }).start();
    }

    static void updateSharedPreferences(final Context context, final Object adInfo) {
        String advertisingId = reflectedGetAdvertisingId(adInfo, null);
        boolean isLimitAdTrackingEnabled = reflectedIsLimitAdTrackingEnabled(adInfo, false);

        /*
         * Committing using the editor is atomic; a single editor must always commit
         * to ensure that the state of the GPS variables are in sync.
         */

        SharedPreferencesHelper.getSharedPreferences(context)
                .edit()
                .putString(ADVERTISING_ID_KEY, advertisingId)
                .putBoolean(IS_LIMIT_AD_TRACKING_ENABLED_KEY, isLimitAdTrackingEnabled)
                .commit();
    }

    static String reflectedGetAdvertisingId(final Object adInfo, final String defaultValue) {
        try {
            return (String) MethodBuilderFactory.create(adInfo, "getId").execute();
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    static boolean reflectedIsLimitAdTrackingEnabled(final Object adInfo, final boolean defaultValue) {
        try {
            Boolean result = (Boolean) MethodBuilderFactory.create(adInfo, "isLimitAdTrackingEnabled").execute();
            return (result != null) ? result : defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    @Deprecated
    static public void setClassNamesForTesting() {
        // This method is used for testing only to help alleviate pain with testing
        // unlinked libraries via reflection
        // Set class names to something that is linked so Class.forName method doesn't throw
        String className = "java.lang.Class";
        sPlayServicesUtilClassName = className;
        sAdvertisingIdClientClassName = className;
    }
}

