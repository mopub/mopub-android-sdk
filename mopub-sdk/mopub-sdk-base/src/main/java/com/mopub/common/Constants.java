// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

public class Constants {

    private Constants() {}

    static {
        HOST = "ads.mopub.com";
    }

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String INTENT_SCHEME = "intent";

    public static final String HOST;

    public static final String AD_HANDLER = "/m/ad";
    public static final String CONVERSION_TRACKING_HANDLER = "/m/open";
    public static final String POSITIONING_HANDLER = "/m/pos";
    public static final String GDPR_SYNC_HANDLER = "/m/gdpr_sync";
    public static final String GDPR_CONSENT_HANDLER = "/m/gdpr_consent_dialog";


    public static final int TEN_SECONDS_MILLIS = 10 * 1000;
    public static final int THIRTY_SECONDS_MILLIS = 30 * 1000;
    public static final int FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1000;
    public static final int FOUR_HOURS_MILLIS = 4 * 60 * 60 * 1000;

    public static final int AD_EXPIRATION_DELAY = FOUR_HOURS_MILLIS;

    public static final int TEN_MB = 10 * 1024 * 1024;

    public static final int UNUSED_REQUEST_CODE = 255;  // Acceptable range is [0, 255]

    public static final String NATIVE_VIDEO_ID = "native_video_id";
    public static final String NATIVE_VAST_VIDEO_CONFIG = "native_vast_video_config";

    // Internal Video Controller Versions
    public static final int VIDEO_CONTROLLER_ONE = 1;
    public static final int VIDEO_CONTROLLER_TWO = 2;

    // Internal Video Tracking nouns, defined in ad server
    public static final String VIDEO_TRACKING_EVENTS_KEY = "events";
    public static final String VIDEO_TRACKING_URLS_KEY = "urls";
    public static final String VIDEO_TRACKING_URL_MACRO = "%%VIDEO_EVENT%%";

    // VAST JSON Field names
    public static final String VAST_WIDTH = "width";
    public static final String VAST_HEIGHT = "height";
    public static final String VAST_RESOURCE = "resource";
    public static final String VAST_TYPE = "type";
    public static final String VAST_CREATIVE_TYPE = "creative_type";

    public static final String VAST_TRACKER_CONTENT = "content";
    public static final String VAST_TRACKER_MESSAGE_TYPE = "message_type";
    public static final String VAST_TRACKER_REPEATABLE = "is_repeatable";
    public static final String VAST_TRACKER_TRACKING_MS = "tracking_ms";
    public static final String VAST_TRACKER_TRACKING_FRACTION = "tracking_fraction";
    public static final String VAST_TRACKER_PLAYTIME_MS = "playtime_ms";
    public static final String VAST_TRACKER_PERCENT_VIEWABLE = "percent_viewable";

    public static final String VAST_TRACKERS_IMPRESSION = "impression_trackers";
    public static final String VAST_TRACKERS_FRACTIONAL = "fractional_trackers";
    public static final String VAST_TRACKERS_ABSOLUTE = "absolute_trackers";
    public static final String VAST_TRACKERS_PAUSE = "pause_trackers";
    public static final String VAST_TRACKERS_RESUME = "resume_trackers";
    public static final String VAST_TRACKERS_COMPLETE = "complete_trackers";
    public static final String VAST_TRACKERS_CLOSE = "close_trackers";
    public static final String VAST_TRACKERS_SKIP = "skip_trackers";
    public static final String VAST_TRACKERS_CLICK = "click_trackers";
    public static final String VAST_TRACKERS_ERROR = "error_trackers";

    public static final String VAST_URL_CLICKTHROUGH = "clickthrough_url";
    public static final String VAST_URL_NETWORK_MEDIA_FILE = "network_media_file_url";
    public static final String VAST_URL_DISK_MEDIA_FILE = "disk_media_file_url";
    public static final String VAST_SKIP_OFFSET = "skip_offset";
    public static final String VAST_SKIP_OFFSET_MS = "skip_offset_ms";
    public static final String VAST_DURATION_MS = "duration_ms";
    public static final String VAST_COMPANION_AD_LANDSCAPE = "landscape_companion_ad";
    public static final String VAST_COMPANION_AD_PORTRAIT = "portrait_companion_ad";
    public static final String VAST_ICON_CONFIG = "icon_config";
    public static final String VAST_IS_SKIPPABLE = "is_skippable";
    public static final String VAST_IS_REWARDED = "is_rewarded";
    public static final String VAST_ENABLE_CLICK_EXP = "enable_click_exp";

    public static final String VAST_CUSTOM_TEXT_CTA = "custom_cta_text";
    public static final String VAST_CUSTOM_TEXT_SKIP = "custom_skip_text";
    public static final String VAST_CUSTOM_CLOSE_ICON_URL = "custom_close_icon_url";
    public static final String VAST_VIDEO_VIEWABILITY_TRACKER = "video_viewability_tracker";

    public static final String VAST_EXTERNAL_VIEWABILITY_TRACKERS = "external_viewability_trackers";
    public static final String VAST_AVID_JAVASCRIPT_RESOURCES = "avid_javascript_resources";
    public static final String VAST_MOAT_IMPRESSION_PIXELS = "moat_impression_pixels";

    public static final String VAST_DSP_CREATIVE_ID = "dsp_creative_id";
    public static final String VAST_PRIVACY_ICON_IMAGE_URL = "privacy_icon_image_url";
    public static final String VAST_PRIVACY_ICON_CLICK_URL = "privacy_icon_click_url";

}
