package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.util.ResponseHeader;

import java.util.Map;

import static com.mopub.network.HeaderUtils.extractHeader;

public class AdTypeTranslator {
    public enum CustomEventType {
        // "Special" custom events that we let people choose in the UI.
        GOOGLE_PLAY_SERVICES_BANNER("admob_native_banner", "com.mopub.mobileads.GooglePlayServicesBanner"),
        GOOGLE_PLAY_SERVICES_INTERSTITIAL("admob_full_interstitial", "com.mopub.mobileads.GooglePlayServicesInterstitial"),
        MILLENNIAL_BANNER("millennial_native_banner", "com.mopub.mobileads.MillennialBanner"),
        MILLENNIAL_INTERSTITIAL("millennial_full_interstitial", "com.mopub.mobileads.MillennialInterstitial"),

        // MoPub-specific custom events.
        MRAID_BANNER("mraid_banner", "com.mopub.mraid.MraidBanner"),
        MRAID_INTERSTITIAL("mraid_interstitial", "com.mopub.mraid.MraidInterstitial"),
        HTML_BANNER("html_banner", "com.mopub.mobileads.HtmlBanner"),
        HTML_INTERSTITIAL("html_interstitial", "com.mopub.mobileads.HtmlInterstitial"),
        VAST_VIDEO_INTERSTITIAL("vast_interstitial", "com.mopub.mobileads.VastVideoInterstitial"),
        MOPUB_NATIVE("mopub_native", "com.mopub.nativeads.MoPubCustomEventNative"),
        MOPUB_VIDEO_NATIVE("mopub_video_native", "com.mopub.nativeads.MoPubCustomEventVideoNative"),
        MOPUB_REWARDED_VIDEO("rewarded_video", "com.mopub.mobileads.MoPubRewardedVideo"),

        UNSPECIFIED("", null);

        private final String mKey;
        private final String mClassName;

        private CustomEventType(String key, String className) {
            mKey = key;
            mClassName = className;
        }

        private static CustomEventType fromString(String key) {
            for (CustomEventType customEventType : values()) {
                if (customEventType.mKey.equals(key)) {
                    return customEventType;
                }
            }

            return UNSPECIFIED;
        }

        @Override
        public String toString() {
            return mClassName;
        }
    }

    public static final String BANNER_SUFFIX = "_banner";
    public static final String INTERSTITIAL_SUFFIX = "_interstitial";

    static String getAdNetworkType(String adType, String fullAdType) {
        String adNetworkType = AdType.INTERSTITIAL.equals(adType) ? fullAdType : adType;
        return adNetworkType != null ? adNetworkType : "unknown";
    }

    public static String getCustomEventName(@NonNull AdFormat adFormat,
            @NonNull String adType,
            @Nullable String fullAdType,
            @NonNull Map<String, String> headers) {
        if (AdType.CUSTOM.equalsIgnoreCase(adType)) {
            return extractHeader(headers, ResponseHeader.CUSTOM_EVENT_NAME);
        } else if (AdType.STATIC_NATIVE.equalsIgnoreCase(adType)){
            return CustomEventType.MOPUB_NATIVE.toString();
        } else if (AdType.VIDEO_NATIVE.equalsIgnoreCase(adType)) {
            return CustomEventType.MOPUB_VIDEO_NATIVE.toString();
        } else if (AdType.REWARDED_VIDEO.equalsIgnoreCase(adType)) {
            return CustomEventType.MOPUB_REWARDED_VIDEO.toString();
        } else if (AdType.HTML.equalsIgnoreCase(adType) || AdType.MRAID.equalsIgnoreCase(adType)) {
            return (AdFormat.INTERSTITIAL.equals(adFormat)
                    ? CustomEventType.fromString(adType + INTERSTITIAL_SUFFIX)
                    : CustomEventType.fromString(adType + BANNER_SUFFIX)).toString();
        } else if (AdType.INTERSTITIAL.equalsIgnoreCase(adType)) {
            return CustomEventType.fromString(fullAdType + INTERSTITIAL_SUFFIX).toString();
        } else {
            return CustomEventType.fromString(adType + BANNER_SUFFIX).toString();
        }
    }
}
