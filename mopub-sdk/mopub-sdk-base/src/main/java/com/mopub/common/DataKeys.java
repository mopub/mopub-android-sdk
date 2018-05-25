package com.mopub.common;

/**
 * Keys used in localExtras and serverExtras maps for MoPub custom events.
 */
public class DataKeys {
    public static final String AD_REPORT_KEY = "mopub-intent-ad-report";
    public static final String HTML_RESPONSE_BODY_KEY = "Html-Response-Body";
    public static final String REDIRECT_URL_KEY = "Redirect-Url";
    public static final String CLICKTHROUGH_URL_KEY = "Clickthrough-Url";
    public static final String CLICK_TRACKING_URL_KEY = "Click-Tracking-Url";
    public static final String SCROLLABLE_KEY = "Scrollable";
    public static final String CREATIVE_ORIENTATION_KEY = "com_mopub_orientation";
    public static final String JSON_BODY_KEY = "com_mopub_native_json";
    public static final String BROADCAST_IDENTIFIER_KEY = "broadcastIdentifier";
    public static final String AD_UNIT_ID_KEY = "com_mopub_ad_unit_id";
    public static final String AD_WIDTH = "com_mopub_ad_width";
    public static final String AD_HEIGHT = "com_mopub_ad_height";

    // Banner imp tracking fields
    public static final String BANNER_IMPRESSION_MIN_VISIBLE_DIPS = "Banner-Impression-Min-Pixels";
    public static final String BANNER_IMPRESSION_MIN_VISIBLE_MS = "Banner-Impression-Min-Ms";
    public static final String BANNER_IMPRESSION_PIXEL_COUNT_ENABLED = "Banner-Impression-Pixel-Count-Enabled";

    // Native fields
    public static final String IMPRESSION_MIN_VISIBLE_PERCENT = "Impression-Min-Visible-Percent";
    public static final String IMPRESSION_VISIBLE_MS = "Impression-Visible-Ms";
    public static final String IMPRESSION_MIN_VISIBLE_PX = "Impression-Min-Visible-Px";

    // Native Video fields
    public static final String PLAY_VISIBLE_PERCENT = "Play-Visible-Percent";
    public static final String PAUSE_VISIBLE_PERCENT = "Pause-Visible-Percent";
    public static final String MAX_BUFFER_MS = "Max-Buffer-Ms";

    // Rewarded Ad fields
    public static final String REWARDED_AD_CURRENCY_NAME_KEY = "Rewarded-Ad-Currency-Name";
    public static final String REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY = "Rewarded-Ad-Currency-Value-String";
    public static final String REWARDED_AD_CUSTOMER_ID_KEY = "Rewarded-Ad-Customer-Id";
    public static final String REWARDED_AD_DURATION_KEY = "Rewarded-Ad-Duration";
    public static final String SHOULD_REWARD_ON_CLICK_KEY = "Should-Reward-On-Click";

    // Viewability fields
    public static final String EXTERNAL_VIDEO_VIEWABILITY_TRACKERS_KEY = "External-Video-Viewability-Trackers";

    // Advanced bidding fields
    public static final String ADM_KEY = "Adm";

    /**
     * @deprecated as of 4.12, replaced by {@link #REWARDED_AD_CUSTOMER_ID_KEY}
     */
    @Deprecated
    public static final String REWARDED_VIDEO_CUSTOMER_ID = "Rewarded-Ad-Customer-Id";

    // Video tracking fields
    public static final String VIDEO_TRACKERS_KEY = "Video-Trackers";
}
