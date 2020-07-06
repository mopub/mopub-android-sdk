// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

/**
 * Keys used in localExtras and serverExtras maps for MoPub base ads.
 */
public class DataKeys {
    public static final String AD_REPORT_KEY = "mopub-intent-ad-report";
    public static final String HTML_RESPONSE_BODY_KEY = "html-response-body";
    public static final String CLICKTHROUGH_URL_KEY = "clickthrough-url";
    public static final String CLICK_TRACKING_URL_KEY = "click-tracking-url";
    public static final String CREATIVE_ORIENTATION_KEY = "com_mopub_orientation";
    public static final String VAST_CLICK_EXP_ENABLED_KEY = "com_mopub_vast_click_exp_enabled";
    public static final String JSON_BODY_KEY = "com_mopub_native_json";
    public static final String BROADCAST_IDENTIFIER_KEY = "broadcastIdentifier";
    public static final String AD_UNIT_ID_KEY = "com_mopub_ad_unit_id";
    public static final String AD_WIDTH = "com_mopub_ad_width";
    public static final String AD_HEIGHT = "com_mopub_ad_height";
    public static final String ADUNIT_FORMAT = "adunit_format";
    public static final String AD_DATA_KEY = "com_mopub_ad_data";

    // Banner imp tracking fields
    public static final String BANNER_IMPRESSION_MIN_VISIBLE_DIPS = "banner-impression-min-pixels";
    public static final String BANNER_IMPRESSION_MIN_VISIBLE_MS = "banner-impression-min-ms";

    // Native fields
    public static final String IMPRESSION_MIN_VISIBLE_PERCENT = "impression-min-visible-percent";
    public static final String IMPRESSION_VISIBLE_MS = "impression-visible-ms";
    public static final String IMPRESSION_MIN_VISIBLE_PX = "impression-min-visible-px";

    // Native Video fields
    public static final String PLAY_VISIBLE_PERCENT = "play-visible-percent";
    public static final String PAUSE_VISIBLE_PERCENT = "pause-visible-percent";
    public static final String MAX_BUFFER_MS = "max-buffer-ms";
    public static final String EVENT_DETAILS = "event-details";

    // Rewarded Ad fields
    public static final String REWARDED_AD_CURRENCY_NAME_KEY = "rewarded-ad-currency-name";
    public static final String REWARDED_AD_CURRENCY_AMOUNT_STRING_KEY = "rewarded-ad-currency-value-string";
    public static final String REWARDED_AD_CUSTOMER_ID_KEY = "rewarded-ad-customer-id";
    public static final String REWARDED_AD_DURATION_KEY = "rewarded-ad-duration";
    public static final String SHOULD_REWARD_ON_CLICK_KEY = "should-reward-on-click";

    // Viewability fields
    public static final String EXTERNAL_VIDEO_VIEWABILITY_TRACKERS_KEY = "external-video-viewability-trackers";

    // Advanced bidding fields
    public static final String ADM_KEY = "adm";

    // Video tracking fields
    public static final String VIDEO_TRACKERS_KEY = "video-trackers";

    /**
     * @deprecated as of 4.12, replaced by {@link #REWARDED_AD_CUSTOMER_ID_KEY}
     */
    @Deprecated
    public static final String REWARDED_VIDEO_CUSTOMER_ID = "rewarded-ad-customer-id";

    /**
     * @deprecated as of 5.4 since this is no longer used.
     */
    @Deprecated
    public static final String REDIRECT_URL_KEY = "redirect-url";

}
