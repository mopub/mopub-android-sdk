// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/
// 2020.3.24 change method reflectedGetAdvertisingId、reflectedIsLimitAdTrackingEnabled to public
// Huawei Technologies Co., Ltd.

package com.mopub.common;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.Nullable;

import com.mopub.common.factories.MethodBuilderFactory;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;

import java.lang.ref.WeakReference;

import static com.mopub.common.logging.MoPubLog.SdkLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.SdkLogEvent.ERROR_WITH_THROWABLE;
import static com.mopub.common.util.Reflection.MethodBuilder;
import static com.mopub.common.util.Reflection.classFound;

public class GpsHelper {
    static public final int GOOGLE_PLAY_SUCCESS_CODE = 0;
    static public final String IS_LIMIT_AD_TRACKING_ENABLED_KEY = "isLimitAdTrackingEnabled";
    private static String sAdvertisingIdClientClassName = "com.google.android.gms.ads.identifier.AdvertisingIdClient";

    public static class AdvertisingInfo {
        public final String advertisingId;
        public final boolean limitAdTracking;

        public AdvertisingInfo(String adId, boolean limitAdTrackingEnabled) {
            advertisingId = adId;
            limitAdTracking = limitAdTrackingEnabled;
        }
    }

    public interface GpsHelperListener {
        void onFetchAdInfoCompleted();
    }

    static public boolean isLimitAdTrackingEnabled(Context context) {
        return SharedPreferencesHelper.getSharedPreferences(context)
                .getBoolean(IS_LIMIT_AD_TRACKING_ENABLED_KEY, false); // default to disabled
    }

    static public void fetchAdvertisingInfoAsync(final Context context, final GpsHelperListener gpsHelperListener) {
        internalFetchAdvertisingInfoAsync(context, gpsHelperListener);
    }

    @Nullable
    static public AdvertisingInfo fetchAdvertisingInfoSync(final Context context) {
        if (context == null) {
            return null;
        }
        Object adInfo = null;
        String advertisingId = null;
        boolean isLimitAdTrackingEnabled = false;

        try {
            MethodBuilder methodBuilder = MethodBuilderFactory.create(null, "getAdvertisingIdInfo")
                    .setStatic(Class.forName(sAdvertisingIdClientClassName))
                    .addParam(Context.class, context);

            adInfo = methodBuilder.execute();
            advertisingId = reflectedGetAdvertisingId(adInfo, advertisingId);
            isLimitAdTrackingEnabled = reflectedIsLimitAdTrackingEnabled(adInfo, isLimitAdTrackingEnabled);
        } catch (Exception e) {
            MoPubLog.log(CUSTOM, "Unable to obtain Google AdvertisingIdClient.Info via reflection.");
            return null;
        }

        return new AdvertisingInfo(advertisingId, isLimitAdTrackingEnabled);
    }

    static private void internalFetchAdvertisingInfoAsync(final Context context, final GpsHelperListener gpsHelperListener) {
        if (!classFound(sAdvertisingIdClientClassName)) {
            if (gpsHelperListener != null) {
                gpsHelperListener.onFetchAdInfoCompleted();
            }
            return;
        }

        try {
            AsyncTasks.safeExecuteOnExecutor(new FetchAdvertisingInfoTask(context, gpsHelperListener));
        } catch (Exception exception) {
            MoPubLog.log(ERROR_WITH_THROWABLE, "Error executing FetchAdvertisingInfoTask", exception);

            if (gpsHelperListener != null) {
                gpsHelperListener.onFetchAdInfoCompleted();
            }
        }
    }

    static private class FetchAdvertisingInfoTask extends AsyncTask<Void, Void, Void> {
        private WeakReference<Context> mContextWeakReference;
        private WeakReference<GpsHelperListener> mGpsHelperListenerWeakReference;
        private AdvertisingInfo info;
        public FetchAdvertisingInfoTask(Context context, GpsHelperListener gpsHelperListener) {
            mContextWeakReference = new WeakReference<Context>(context);
            mGpsHelperListenerWeakReference = new WeakReference<GpsHelperListener>(gpsHelperListener);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                Context context = mContextWeakReference.get();
                if (context == null) {
                    return null;
                }

                MethodBuilder methodBuilder = MethodBuilderFactory.create(null, "getAdvertisingIdInfo")
                        .setStatic(Class.forName(sAdvertisingIdClientClassName))
                        .addParam(Context.class, context);

                Object adInfo = methodBuilder.execute();

                if (adInfo != null) {
                    // updateClientMetadata(context, adInfo);
                }
            } catch (Exception exception) {
                MoPubLog.log(CUSTOM, "Unable to obtain Google AdvertisingIdClient.Info via reflection.");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            GpsHelperListener gpsHelperListener = mGpsHelperListenerWeakReference.get();
            if (gpsHelperListener != null) {
                gpsHelperListener.onFetchAdInfoCompleted();
            }
        }
    }

    public static String reflectedGetAdvertisingId(final Object adInfo, final String defaultValue) {
        try {
            return (String) MethodBuilderFactory.create(adInfo, "getId").execute();
        } catch (Exception exception) {
            return defaultValue;
        }
    }

    public static boolean reflectedIsLimitAdTrackingEnabled(final Object adInfo, final boolean defaultValue) {
        try {
            Boolean result = (Boolean) MethodBuilderFactory.create(adInfo, "isLimitAdTrackingEnabled").execute();
            return (result != null) ? result : defaultValue;
        } catch (Exception exception) {
            return defaultValue;
        }
    }
}

