// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AdFormat;
import com.mopub.common.AdType;
import com.mopub.common.Preconditions;
import com.mopub.common.util.ResponseHeader;

import org.json.JSONObject;

import static com.mopub.network.HeaderUtils.extractHeader;

public class AdTypeTranslator {
    public enum BaseAdType {
        // "Special" base ads that we let people choose in the UI.
        GOOGLE_PLAY_SERVICES_BANNER("admob_native_banner",
                "com.mopub.mobileads.GooglePlayServicesBanner", false),
        GOOGLE_PLAY_SERVICES_INTERSTITIAL("admob_full_interstitial",
                "com.mopub.mobileads.GooglePlayServicesInterstitial", false),

        // MoPub-specific base ads/custom events.
        MOPUB_NATIVE("mopub_native",
                "com.mopub.nativeads.MoPubCustomEventNative", true),
        MOPUB_VIDEO_NATIVE("mopub_video_native",
                "com.mopub.nativeads.MoPubCustomEventVideoNative", true),
        MOPUB_INLINE("mopub_inline", "com.mopub.mobileads.MoPubInline",
                true),
        MOPUB_FULLSCREEN("fullscreen", "com.mopub.mobileads.MoPubFullscreen", true),

        UNSPECIFIED("", null, false);

        @NonNull
        private final String mKey;
        @Nullable
        private final String mClassName;
        private final boolean mIsMoPubSpecific;

        private BaseAdType(String key, String className, boolean isMoPubSpecific) {
            mKey = key;
            mClassName = className;
            mIsMoPubSpecific = isMoPubSpecific;
        }

        private static BaseAdType fromString(@Nullable final String key) {
            for (BaseAdType baseAdType : values()) {
                if (baseAdType.mKey.equals(key)) {
                    return baseAdType;
                }
            }

            return UNSPECIFIED;
        }

        private static BaseAdType fromClassName(@Nullable final String className) {
            for (BaseAdType baseAdType : values()) {
                if (baseAdType.mClassName != null
                        && baseAdType.mClassName.equals(className)) {
                    return baseAdType;
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

    public static String getBaseAdClassName(@NonNull AdFormat adFormat,
                                            @NonNull String adType,
                                            @Nullable String fullAdType,
                                            @Nullable JSONObject headers) {
        Preconditions.checkNotNull(adFormat);
        Preconditions.checkNotNull(adType);

        switch (adType.toLowerCase()) {
            case AdType.CUSTOM:
                return extractHeader(headers, ResponseHeader.CUSTOM_EVENT_NAME);
            case AdType.STATIC_NATIVE:
                return BaseAdType.MOPUB_NATIVE.toString();
            case AdType.VIDEO_NATIVE:
                return BaseAdType.MOPUB_VIDEO_NATIVE.toString();
            case AdType.INTERSTITIAL:
                if ("admob_full".equals(fullAdType)) {
                    return BaseAdType.fromString(fullAdType + INTERSTITIAL_SUFFIX).toString();
                }
                // Deliberate fallthrough
            case AdType.REWARDED_VIDEO:
            case AdType.REWARDED_PLAYABLE:
            case AdType.FULLSCREEN:
                return BaseAdType.MOPUB_FULLSCREEN.toString();
            case AdType.HTML:
            case AdType.MRAID:
                return (AdFormat.BANNER.equals(adFormat)
                        ? BaseAdType.MOPUB_INLINE
                        : BaseAdType.MOPUB_FULLSCREEN).toString();
            default:
                return BaseAdType.fromString(adType + BANNER_SUFFIX).toString();
        }
    }
}
