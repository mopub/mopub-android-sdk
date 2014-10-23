package com.mopub.mobileads;

public class AdTypeTranslator {
    public enum CustomEventType {
        // With the deprecation of a standalone AdMob SDK, these now point to Google Play Services
        GOOGLE_PLAY_SERVICES_BANNER("admob_native_banner", "com.mopub.mobileads.GooglePlayServicesBanner"),
        GOOGLE_PLAY_SERVICES_INTERSTITIAL("admob_full_interstitial", "com.mopub.mobileads.GooglePlayServicesInterstitial"),

        MILLENNIAL_BANNER("millennial_native_banner", "com.mopub.mobileads.MillennialBanner"),
        MILLENNIAL_INTERSTITIAL("millennial_full_interstitial", "com.mopub.mobileads.MillennialInterstitial"),
        MRAID_BANNER("mraid_banner", "com.mopub.mobileads.MraidBanner"),
        MRAID_INTERSTITIAL("mraid_interstitial", "com.mopub.mobileads.MraidInterstitial"),
        HTML_BANNER("html_banner", "com.mopub.mobileads.HtmlBanner"),
        HTML_INTERSTITIAL("html_interstitial", "com.mopub.mobileads.HtmlInterstitial"),
        VAST_VIDEO_INTERSTITIAL("vast_interstitial", "com.mopub.mobileads.VastVideoInterstitial"),

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

    static String getAdNetworkType(String adType, String fullAdType) {
        String adNetworkType = "interstitial".equals(adType) ? fullAdType : adType;
        return adNetworkType != null ? adNetworkType : "unknown";
    }

    static String getCustomEventNameForAdType(MoPubView moPubView, String adType, String fullAdType) {
        CustomEventType customEventType;

        if ("html".equals(adType) || "mraid".equals(adType)) {
            customEventType = (isInterstitial(moPubView))
                    ? CustomEventType.fromString(adType + "_interstitial")
                    : CustomEventType.fromString(adType + "_banner");
        } else {
            customEventType = ("interstitial".equals(adType))
                    ? CustomEventType.fromString(fullAdType + "_interstitial")
                    : CustomEventType.fromString(adType + "_banner");
        }

        return customEventType.toString();
    }

    private static boolean isInterstitial(MoPubView moPubView) {
        return moPubView instanceof MoPubInterstitial.MoPubInterstitialView;
    }
}
