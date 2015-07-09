package com.mopub.mobileads;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

/**
 * The various possible error codes for VAST that MoPub supports. See
 * http://www.iab.net/media/file/VASTv3.0.pdf for more information about the error codes.
 */
public enum VastErrorCode {
    /**
     * For any type of XML issue in the VAST document. e.g. missing a matching tag, missing a square
     * bracket, not using CDATA correctly, etc.
     */
    XML_PARSING_ERROR("100"),

    /**
     * When following a wrapper redirect and the URI was either unavailable or reached a timeout
     * as defined by the video player.
     */
    WRAPPER_TIMEOUT("301"),

    /**
     * When receiving a no ads VAST response after one or more Wrappers. See section 2.4.2.4 in the
     * Vast 3.0 spec for details.
     */
    NO_ADS_VAST_RESPONSE("303"),

    /**
     * For any reason the linear video ad failed to download or play (or for errors during
     * playback).
     */
    GENERAL_LINEAR_AD_ERROR("400"),

    /**
     * For any reason the companion ad failed to download or show. MoPub still tries to show the
     * linear ad regardless of the status of the companion ad.
     */
    GENERAL_COMPANION_AD_ERROR("600"),

    /**
     * Any other error, or an unexpected error.
     */
    UNDEFINED_ERROR("900");

    @NonNull private final String mErrorCode;

    VastErrorCode(@NonNull String errorCode) {
        Preconditions.checkNotNull(errorCode, "errorCode cannot be null");
        mErrorCode = errorCode;
    }

    /**
     * Gets the code for the error
     *
     * @return String of the code representing that error
     */
    @NonNull
    String getErrorCode() {
        return mErrorCode;
    }
}
