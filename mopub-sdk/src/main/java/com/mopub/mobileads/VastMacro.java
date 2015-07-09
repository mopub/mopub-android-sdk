package com.mopub.mobileads;

/**
 * Macros that MoPub supports. This is in the format of [name]. For example, an errorcode macro
 * would show up as "http://www.somesite.com/somemethod?error=[ERRORCODE]"
 */
enum VastMacro {
    /**
     * The VAST error number that represents type of error. See {@link VastErrorCode} for possible
     * values.
     */
    ERRORCODE,

    /**
     * Current progress of the video in the format HH:MM:SS.mmm. For example, 4.5 seconds into the
     * video would be represented as 00:00:04.500.
     */
    CONTENTPLAYHEAD,

    /**
     * A random 8-digit number intended for cachebusting.
     */
    CACHEBUSTING,

    /**
     * The url of the current video.
     */
    ASSETURI
}
