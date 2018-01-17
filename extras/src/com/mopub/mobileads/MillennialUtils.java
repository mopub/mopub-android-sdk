package com.mopub.mobileads;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.millennialmedia.MMException;
import com.millennialmedia.MMSDK;
import com.mopub.common.Preconditions;


final public class MillennialUtils {
    private static final String LOG_TAG = MillennialUtils.class.getSimpleName();

    private static final Handler handler = new Handler(Looper.getMainLooper());

    public static final String VERSION = "1.2.0";

    private static volatile boolean sIsInitialized = false;

    public static void postOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    public static boolean isEmpty(String s) {
        return (s == null || s.trim().isEmpty());
    }

    /**
     * @param context - application or activity context
     * @return {@code true} successfully initialized
     */
    public static boolean initSdk(@NonNull Context context) {
        if (sIsInitialized) {
            return true;
        }

        synchronized (MillennialUtils.class) {
            if (sIsInitialized) {
                return true;
            }
            Preconditions.checkNotNull(context);

            final Application application;
            if (context instanceof Application) {
                application = (Application) context;
            } else {
                application = (Application) context.getApplicationContext();
            }

            try {
                MMSDK.initialize(application);
            } catch (MMException e) {
                Log.e(LOG_TAG, "Exception occurred initializing the MM SDK.", e);
                return false;
            }

            sIsInitialized = true;
        }
        return true;
    }
}
