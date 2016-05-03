package com.mopub.nativeads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.flurry.android.ads.FlurryAdNative;
import com.mopub.mobileads.FlurryAgentWrapper;

import java.util.Map;

public class FlurryCustomEventNative extends CustomEventNative {

    private static final String LOG_TAG = FlurryCustomEventNative.class.getSimpleName();

    @Override
    protected void loadNativeAd(@NonNull final Activity activity,
                                @NonNull final CustomEventNativeListener customEventNativeListener,
                                @NonNull final Map<String, Object> localExtras,
                                @NonNull final Map<String, String> serverExtras) {

        final String flurryApiKey;
        final String flurryAdSpace;

        //Get the FLURRY_APIKEY and FLURRY_ADSPACE from the server.
        if (validateExtras(serverExtras)) {
            flurryApiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
            flurryAdSpace = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);

            FlurryAgentWrapper.getInstance().startSession(activity, flurryApiKey);
        } else {
            customEventNativeListener.onNativeAdFailed(
                    NativeErrorCode.NATIVE_ADAPTER_CONFIGURATION_ERROR);
            Log.i(LOG_TAG, "Failed Native AdFetch: Missing required server extras" +
                    " [FLURRY_APIKEY and/or FLURRY_ADSPACE].");
            return;
        }

        final FlurryStaticNativeAd mflurryStaticNativeAd =
                new FlurryStaticNativeAd(activity,
                        new FlurryAdNative(activity, flurryAdSpace), customEventNativeListener);
        mflurryStaticNativeAd.fetchAd();
    }

    private boolean validateExtras(final Map<String, String> serverExtras) {
        final String flurryApiKey = serverExtras.get(FlurryAgentWrapper.PARAM_API_KEY);
        final String flurryAdSpace = serverExtras.get(FlurryAgentWrapper.PARAM_AD_SPACE_NAME);
        Log.i(LOG_TAG, "ServerInfo fetched from Mopub " + FlurryAgentWrapper.PARAM_API_KEY + " : "
                + flurryApiKey + " and " + FlurryAgentWrapper.PARAM_AD_SPACE_NAME + " :" +
                flurryAdSpace);
        return (!TextUtils.isEmpty(flurryApiKey) && !TextUtils.isEmpty(flurryAdSpace));
    }

}
