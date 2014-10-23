package com.mopub.mobileads.util;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;

import com.mopub.common.util.IntentUtils;
import com.mopub.common.util.VersionCode;
import com.mopub.mobileads.MraidVideoPlayerActivity;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Environment.MEDIA_MOUNTED;

public class Mraids {
    public static final String ANDROID_CALENDAR_CONTENT_TYPE = "vnd.android.cursor.item/event";

    public static boolean isTelAvailable(Context context) {
        Intent telIntent = new Intent(Intent.ACTION_DIAL);
        telIntent.setData(Uri.parse("tel:"));

        return IntentUtils.deviceCanHandleIntent(context, telIntent);
    }

    public static boolean isSmsAvailable(Context context) {
        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setData(Uri.parse("sms:"));

        return IntentUtils.deviceCanHandleIntent(context, smsIntent);
    }

    public static boolean isStorePictureSupported(Context context) {
        return MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                && context.checkCallingOrSelfPermission(WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isCalendarAvailable(Context context) {
        Intent calendarIntent = new Intent(Intent.ACTION_INSERT).setType(ANDROID_CALENDAR_CONTENT_TYPE);

        return VersionCode.currentApiLevel().isAtLeast(VersionCode.ICE_CREAM_SANDWICH)
                && IntentUtils.deviceCanHandleIntent(context, calendarIntent);
    }

    public static boolean isInlineVideoAvailable(Context context) {
        Intent mraidVideoIntent = new Intent(context, MraidVideoPlayerActivity.class);

        return IntentUtils.deviceCanHandleIntent(context, mraidVideoIntent);
    }
}
