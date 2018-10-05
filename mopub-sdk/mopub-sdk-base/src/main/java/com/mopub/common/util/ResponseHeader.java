// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

public enum ResponseHeader {
    AD_TIMEOUT("x-ad-timeout-ms"),
    AD_TYPE("x-adtype"),
    CLICK_TRACKING_URL("x-clickthrough"),
    CUSTOM_EVENT_DATA("x-custom-event-class-data"),
    CUSTOM_EVENT_NAME("x-custom-event-class-name"),
    CREATIVE_ID("x-creativeid"),
    DSP_CREATIVE_ID("x-dspcreativeid"),
    FAIL_URL("x-next-url"),
    FULL_AD_TYPE("x-fulladtype"),
    HEIGHT("x-height"),
    IMPRESSION_URL("x-imptracker"),
    IMPRESSION_URLS("imptrackers"),
    NATIVE_PARAMS("x-nativeparams"),
    NETWORK_TYPE("x-networktype"),
    ORIENTATION("x-orientation"),
    REFRESH_TIME("x-refreshtime"),
    WARMUP("x-warmup"),
    WIDTH("x-width"),
    BACKFILL("x-backfill"),
    REQUEST_ID("x-request-id"),

    // HTTP headers
    CONTENT_TYPE("content-type"),
    LOCATION("location"),
    USER_AGENT("user-agent"),
    ACCEPT_LANGUAGE("accept-language"),

    BROWSER_AGENT("x-browser-agent"),

    // Banner impression tracking fields
    BANNER_IMPRESSION_MIN_VISIBLE_DIPS("x-banner-impression-min-pixels"),
    BANNER_IMPRESSION_MIN_VISIBLE_MS("x-banner-impression-min-ms"),

    // Native fields
    IMPRESSION_MIN_VISIBLE_PERCENT("x-impression-min-visible-percent"),
    IMPRESSION_VISIBLE_MS("x-impression-visible-ms"),
    IMPRESSION_MIN_VISIBLE_PX("x-native-impression-min-px"),

    // Native Video fields
    PLAY_VISIBLE_PERCENT("x-play-visible-percent"),
    PAUSE_VISIBLE_PERCENT("x-pause-visible-percent"),
    MAX_BUFFER_MS("x-max-buffer-ms"),

    // Rewarded Ad fields
    REWARDED_VIDEO_CURRENCY_NAME("x-rewarded-video-currency-name"),
    REWARDED_VIDEO_CURRENCY_AMOUNT("x-rewarded-video-currency-amount"),
    REWARDED_CURRENCIES("x-rewarded-currencies"),
    REWARDED_VIDEO_COMPLETION_URL("x-rewarded-video-completion-url"),
    REWARDED_DURATION("x-rewarded-duration"),
    SHOULD_REWARD_ON_CLICK("x-should-reward-on-click"),

    // Internal Video Trackers
    VIDEO_TRACKERS("x-video-trackers"),

    // Viewability fields
    VIDEO_VIEWABILITY_TRACKERS("x-video-viewability-trackers"),
    DISABLE_VIEWABILITY("x-disable-viewability"),

    // Client-side Waterfall
    AD_RESPONSES("ad-responses"),
    CONTENT("content"),
    METADATA("metadata"),

    BEFORE_LOAD_URL("x-before-load-url"),
    AFTER_LOAD_URL("x-after-load-url"),
    AFTER_LOAD_SUCCESS_URL("x-after-load-success-url"),
    AFTER_LOAD_FAIL_URL("x-after-load-fail-url"),

    // Consent fields
    INVALIDATE_CONSENT("invalidate_consent"),
    FORCE_EXPLICIT_NO("force_explicit_no"),
    REACQUIRE_CONSENT("reacquire_consent"),
    CONSENT_CHANGE_REASON("consent_change_reason"),
    FORCE_GDPR_APPLIES("force_gdpr_applies");

    private final String key;
    ResponseHeader(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}

