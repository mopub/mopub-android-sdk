package com.mopub.mobileads.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.mopub.common.logging.MoPubLog;

public class Utils {
    private Utils() {}

    public static boolean executeIntent(Context context, Intent intent, String errorMessage) {
        try {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            MoPubLog.d((errorMessage != null)
                    ? errorMessage
                    : "Unable to start intent.");
            return false;
        }
        return true;
    }
}