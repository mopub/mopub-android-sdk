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
        GOOGLE_PLAY_SERVICES_BANNER("admob_native_banner",
                "com.mopub.mobileads.GooglePlayServicesBanner", false),
        GOOGLE_PLAY_SERVICES_INTERSTITIAL("admob_full_interstitial",
                "com.mopub.mobileads.GooglePlayServicesInterstitial", false),
        MILLENNIAL_BANNER("millennial_native_banner",
                "com.mopub.mobileads.MillennialBanner", false),
        MILLENNIAL_INTERSTITIAL("millennial_full_interstitial",
                "com.mopub.mobileads.MillennialInterstitial", false),

        // MoPub-specific custom events.
        MRAID_BANNER("mraid_banner",
                "com.mopub.mraid.MraidBanner", true),
        MRAID_INTERSTITIAL("mraid_interstitial",
                "com.mopub.mraid.MraidInterstitial", true),
        HTML_BANNER("html_banner",
                "com.mopub.mobileads.HtmlBanner", true),
        HTML_INTERSTITIAL("html_interstitial",
                "com.mopub.mobileads.HtmlInterstitial", true),
        VAST_VIDEO_INTERSTITIAL("vast_interstitial",
                "com.mopub.mobileads.VastVideoInterstitial", true),
        MOPUB_NATIVE("mopub_native",
                "com.mopub.nativeads.MoPubCustomEventNative", true),
        MOPUB_VIDEO_NATIVE("mopub_video_native",
                "com.mopub.nativeads.MoPubCustomEventVideoNative", true),
        MOPUB_REWARDED_VIDEO("rewarded_video",
                "com.mopub.mobileads.MoPubRewardedVideo", true),
        MOPUB_REWARDED_PLAYABLE("rewarded_playable",
                "com.mopub.mobileads.MoPubRewardedPlayable", true),

        UNSPECIFIED("", null, false);

        @NonNull
        private final String mKey;
        @Nullable
        private final String mClassName;
        private final boolean mIsMoPubSpecific;

        private CustomEventType(String key, String className, boolean isMoPubSpecific) {
            mKey = key;
            mClassName = className;
            mIsMoPubSpecific = isMoPubSpecific;
        }

        private static CustomEventType fromString(@Nullable final String key) {
            for (CustomEventType customEventType : values()) {
                if (customEventType.mKey.equals(key)) {
                    return customEventType;
                }
            }

            return UNSPECIFIED;
        }

        private static CustomEventType fromClassName(@Nullable final String className) {
            for (CustomEventType customEventType : values()) {
                if (customEventType.mClassName != null
                        && customEventType.mClassName.equals(className)) {
                    return customEventType;
                }
            }

            return UNSPECIFIED;
        }

        @Override
        public String toString() {
            return mClassName;
        }

        public static boolean isMoPubSpecific(@Nullable final String className) {
            return fromClassName(className).mIsMoPubSpecific;
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
        } else if (AdType.STATIC_NATIVE.equalsIgnoreCase(adType)) {
            return CustomEventType.MOPUB_NATIVE.toString();
        } else if (AdType.VIDEO_NATIVE.equalsIgnoreCase(adType)) {
            return CustomEventType.MOPUB_VIDEO_NATIVE.toString();
        } else if (AdType.REWARDED_VIDEO.equalsIgnoreCase(adType)) {
            return CustomEventType.MOPUB_REWARDED_VIDEO.toString();
        } else if (AdType.REWARDED_PLAYABLE.equalsIgnoreCase(adType)) {
            return CustomEventType.MOPUB_REWARDED_PLAYABLE.toString();
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
