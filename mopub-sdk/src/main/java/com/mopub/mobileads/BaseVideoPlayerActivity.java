package com.mopub.mobileads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;

import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.util.vast.VastVideoConfiguration;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.VastVideoViewController.VAST_VIDEO_CONFIGURATION;

class BaseVideoPlayerActivity extends Activity {
    static final String VIDEO_CLASS_EXTRAS_KEY = "video_view_class_name";
    static final String VIDEO_URL = "video_url";

    static void startMraid(final Context context, final String videoUrl, final AdConfiguration adConfiguration) {
        final Intent intentVideoPlayerActivity = createIntentMraid(context, videoUrl, adConfiguration);
        try {
            context.startActivity(intentVideoPlayerActivity);
        } catch (ActivityNotFoundException e) {
            MoPubLog.d("Activity MraidVideoPlayerActivity not found. Did you declare it in your AndroidManifest.xml?");
        }
    }

    static Intent createIntentMraid(final Context context,
            final String videoUrl,
            final AdConfiguration adConfiguration) {
        final Intent intentVideoPlayerActivity = new Intent(context, MraidVideoPlayerActivity.class);
        intentVideoPlayerActivity.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intentVideoPlayerActivity.putExtra(VIDEO_CLASS_EXTRAS_KEY, "mraid");
        intentVideoPlayerActivity.putExtra(VIDEO_URL, videoUrl);
        intentVideoPlayerActivity.putExtra(AD_CONFIGURATION_KEY, adConfiguration);
        return intentVideoPlayerActivity;
    }

    static void startVast(final Context context,
            final VastVideoConfiguration vastVideoConfiguration,
            final AdConfiguration adConfiguration) {
        final Intent intentVideoPlayerActivity = createIntentVast(context, vastVideoConfiguration, adConfiguration);
        try {
            context.startActivity(intentVideoPlayerActivity);
        } catch (ActivityNotFoundException e) {
            MoPubLog.d("Activity MraidVideoPlayerActivity not found. Did you declare it in your AndroidManifest.xml?");
        }
    }

    static Intent createIntentVast(final Context context,
            final VastVideoConfiguration vastVideoConfiguration,
            final AdConfiguration adConfiguration) {
        final Intent intentVideoPlayerActivity = new Intent(context, MraidVideoPlayerActivity.class);
        intentVideoPlayerActivity.setFlags(FLAG_ACTIVITY_NEW_TASK);
        intentVideoPlayerActivity.putExtra(VIDEO_CLASS_EXTRAS_KEY, "vast");
        intentVideoPlayerActivity.putExtra(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);
        intentVideoPlayerActivity.putExtra(AD_CONFIGURATION_KEY, adConfiguration);
        return intentVideoPlayerActivity;
    }
}

