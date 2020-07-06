// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.logging;

import androidx.annotation.Nullable;
import android.util.Log;

/**
 * All logs will be printed using android.util.Log.i(). As a result, filtering these by log level
 * is not possible.
 *
 * Due to the new format of the logs, filtering can be done by class or method name, or log event
 * message.
 */
public class MoPubDefaultLogger implements MoPubLogger {

    /**
     * Logcat has a max message length of 4kB, so let's split each message by this max message
     * length. Since each message has some metadata, let's limit this to 3kB.
     */
    static int MAX_MESSAGE_LENGTH_BYTES = 3 * 1024;

    /**
     * MESSAGE_FORMAT is used to produce a log in the following format:
     * "[com.mopub.common.logging.MoPubLog][log] Ad Custom Log - Loading ad adapter."
     */
    private static final String MESSAGE_FORMAT = "[%s][%s] %s";

    /**
     * MESSAGE_WITH_ID_FORMAT is used to produce a log in the following format:
     * "[com.mopub.common.logging.MoPubLog][log][ad-unit-id-123] Adapter Custom Log - Attempting to invoke base ad: com.mopub.mobileads.MoPubInline"
     */
    private static final String MESSAGE_WITH_ID_FORMAT = "[%s][%s][%s] %s";

    public MoPubDefaultLogger() {
    }

    @Override
    public void log(@Nullable String className, @Nullable String methodName,
                    @Nullable String identifier, @Nullable String message) {
        for (final String segment : split(message)) {
            if (identifier == null) {
                Log.i(MoPubLog.LOGTAG, String.format(MESSAGE_FORMAT, className,
                        methodName, segment));
            } else {
                Log.i(MoPubLog.LOGTAG, String.format(MESSAGE_WITH_ID_FORMAT, className,
                        methodName, identifier, segment));
            }
        }
    }

    static String[] split(@Nullable final String message) {
        if (message == null) {
            return new String[1];
        }

        final int segmentCount = 1 + (message.length() / MAX_MESSAGE_LENGTH_BYTES);
        final String[] segments = new String[segmentCount];
        for (int i = 0; i < segmentCount; i++) {
            segments[i] = message.substring(i * MAX_MESSAGE_LENGTH_BYTES,
                    Math.min((i + 1) * MAX_MESSAGE_LENGTH_BYTES, message.length()));
        }
        return segments;
    }
}
