package com.mopub.common;

public class Constants {

    private Constants() {}

    public static final String HTTP = "http";
    public static final String HTTPS = "https";
    public static final String INTENT_SCHEME = "intent";

    public static final String HOST = "ads.mopub.com";

    public static final String AD_HANDLER = "/m/ad";
    public static final String CONVERSION_TRACKING_HANDLER = "/m/open";
    public static final String POSITIONING_HANDLER = "/m/pos";


    public static final int TEN_SECONDS_MILLIS = 10 * 1000;
    public static final int THIRTY_SECONDS_MILLIS = 30 * 1000;
    public static final int FIFTEEN_MINUTES_MILLIS = 15 * 60 * 1000;
    public static final int FOUR_HOURS_MILLIS = 4 * 60 * 60 * 1000;

    public static final int AD_EXPIRATION_DELAY = FOUR_HOURS_MILLIS;

    public static final int TEN_MB = 10 * 1024 * 1024;

    public static final int UNUSED_REQUEST_CODE = 255;  // Acceptable range is [0, 255]

    public static final String NATIVE_VIDEO_ID = "native_video_id";
    public static final String NATIVE_VAST_VIDEO_CONFIG = "native_vast_video_config";

    // Internal Video Tracking nouns, defined in ad server
    public static final String VIDEO_TRACKING_EVENTS_KEY = "events";
    public static final String VIDEO_TRACKING_URLS_KEY = "urls";
    public static final String VIDEO_TRACKING_URL_MACRO = "%%VIDEO_EVENT%%";
}
