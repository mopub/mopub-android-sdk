// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.logging;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.mopub.common.Preconditions;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.util.Strings.getDelimitedString;
import static java.text.MessageFormat.format;

public class MoPubLog {

    public static final String LOGTAG = "MoPub";

    /**
     * STACK_TRACE_LEVEL is a magic number used to determine the offset on the call stack of the
     * calling class and method so the names can be used in log messages.
     */
    private static final int STACK_TRACE_LEVEL = 4;

    /**
     * LogLevelInt values set for parity with iOS
     */
    public interface LogLevelInt {
        int DEBUG_INT = 20;
        int INFO_INT = 30;
        int NONE_INT = 70;
    }

    public enum LogLevel implements LogLevelInt {

        DEBUG(DEBUG_INT, "DEBUG"),
        INFO(INFO_INT, "INFO"),
        NONE(NONE_INT, "NONE");

        private int mLevel;
        private String mLevelString;

        LogLevel(int level, String levelString) {
            this.mLevel = level;
            this.mLevelString = levelString;
        }

        @NonNull
        public String toString() {
            return mLevelString;
        }

        @NonNull
        public int intValue() {
            return mLevel;
        }

        /**
         * This valueOf overload is used to get the associated LogLevel enum from an int.
         *
         * @param level The int value for which the LogLevel is needed.
         * @return The LogLevel associated with the level. Will return NONE by default.
         */
        @NonNull
        public static LogLevel valueOf(final int level) {
            switch (level) {
                case DEBUG_INT:
                    return DEBUG;
                case INFO_INT:
                    return INFO;
                case NONE_INT:
                default:
                    return NONE;
            }
        }
    }

    @NonNull private static final MoPubLog sInstance = new MoPubLog();
    @NonNull private LogLevel sLogLevel = LogLevel.INFO;
    @NonNull private Map<MoPubLogger, LogLevel> mLoggers = new HashMap<>();
    @NonNull private MoPubLogger mDefaultLogger = new MoPubDefaultLogger();

    private MoPubLog() {
    }

    private static void logDeprecated(@Nullable final String message, @Nullable final Throwable throwable) {
        MoPubLog.log(SdkLogEvent.CUSTOM_WITH_THROWABLE, message, (throwable != null)
                ? throwable.getMessage() : "");
    }

    private static void removeLogger(@Nullable MoPubLogger logger) {
        sInstance.mLoggers.remove(logger);
    }

    public static void addLogger(@Nullable MoPubLogger logger) {
        addLogger(logger, sInstance.sLogLevel);
    }

    public static void addLogger(@Nullable MoPubLogger logger, @Nullable LogLevel logLevel) {
        sInstance.mLoggers.put(logger, logLevel);
    }

    public static void setLogLevel(@NonNull LogLevel logLevel) {
        Preconditions.checkNotNull(logLevel);

        sInstance.sLogLevel = logLevel;
        addLogger(sInstance.mDefaultLogger, sInstance.sLogLevel);
    }

    @NonNull
    public static LogLevel getLogLevel() {
        return sInstance.sLogLevel;
    }

    public static void log(@Nullable final MPLogEventType logEventType, @Nullable final Object... args) {
        Pair<String, String> classAndMethodNames = getClassAndMethod();
        log(classAndMethodNames, null, logEventType, args);  // null identifiers are omitted
    }

    public static void log(@Nullable final String identifier, @Nullable final MPLogEventType logEventType,
                           @Nullable final Object... args) {
        Pair<String, String> classAndMethodNames = getClassAndMethod();
        log(classAndMethodNames, identifier, logEventType, args);  // null identifiers are omitted
    }

    private static void log(@NonNull Pair<String, String> classAndMethodNames, @Nullable String identifier,
                            @Nullable  MPLogEventType logEventType, @Nullable Object... args) {
        Preconditions.checkNotNull(classAndMethodNames);

        if (logEventType == null) {
            return;
        }

        for (MoPubLogger logger : sInstance.mLoggers.keySet()) {
            if (sInstance.mLoggers.get(logger) != null
                    && sInstance.mLoggers.get(logger).intValue() <= logEventType.getLogLevel().intValue()) {
                logger.log(classAndMethodNames.first, classAndMethodNames.second, identifier,
                        logEventType.getMessage(args));
            }
        }
    }

    private static Pair<String, String> getClassAndMethod() {
        final StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        return new Pair<>(stackTraceElements[STACK_TRACE_LEVEL].getClassName(),
                stackTraceElements[STACK_TRACE_LEVEL].getMethodName());
    }

    @Deprecated
    public static void c(final String message) {
        MoPubLog.c(message, null);
    }

    @Deprecated
    public static void v(final String message) {
        MoPubLog.v(message, null);
    }

    @Deprecated
    public static void d(final String message) {
        MoPubLog.d(message, null);
    }

    @Deprecated
    public static void i(final String message) {
        MoPubLog.i(message, null);
    }

    @Deprecated
    public static void w(final String message) {
        MoPubLog.w(message, null);
    }

    @Deprecated
    public static void e(final String message) {
        MoPubLog.e(message, null);
    }

    @Deprecated
    public static void c(final String message, final Throwable throwable) {
        logDeprecated(message, throwable);
    }

    @Deprecated
    public static void v(final String message, final Throwable throwable) {
        logDeprecated(message, throwable);
    }

    @Deprecated
    public static void d(final String message, final Throwable throwable) {
        logDeprecated(message, throwable);
    }

    @Deprecated
    public static void i(final String message, final Throwable throwable) {
        logDeprecated(message, throwable);
    }

    @Deprecated
    public static void w(final String message, final Throwable throwable) {
        logDeprecated(message, throwable);
    }

    @Deprecated
    public static void e(final String message, final Throwable throwable) {
        logDeprecated(message, throwable);
    }

    protected interface MPLogEventType {
        @NonNull
        String getMessage(@Nullable final Object... args);

        @NonNull
        LogLevel getLogLevel();
    }

    public enum AdLogEvent implements MPLogEventType {

        // Params:
        // Ad request URL
        // POST body of request
        REQUESTED(LogLevel.DEBUG, "Ad requesting from AdServer: {0}\n{1}"),

        // Params:
        // Response JSON
        RESPONSE_RECEIVED(LogLevel.DEBUG, "Ad server responded with:\n{0}"),

        // Params:
        // <none>
        LOAD_ATTEMPTED(LogLevel.INFO, "Ad attempting to load"),

        // Params:
        // <none>
        LOAD_SUCCESS(LogLevel.INFO, "Ad loaded"),

        // Params:
        // Error Code
        // Error Message
        LOAD_FAILED(LogLevel.INFO, "Ad failed to load: ({0}) {1}"),

        // Params:
        // <none>
        SHOW_ATTEMPTED(LogLevel.INFO, "Attempting to show ad"),

        // Params:
        // <none>
        SHOW_SUCCESS(LogLevel.INFO, "Ad shown"),

        // Params:
        // Error Code
        // Error Message
        SHOW_FAILED(LogLevel.INFO, "Ad failed to show: ({0}) {1}"),

        // Params:
        // Expiration time interval in seconds
        EXPIRED(LogLevel.DEBUG, "Ad expired since it was not shown within {0} seconds of it being loaded"),

        // Params:
        // <none>
        CLICKED(LogLevel.DEBUG, "Ad clicked"),

        // Params:
        // <none>
        WILL_APPEAR(LogLevel.DEBUG, "Ad will appear"),

        // Params:
        // <none>
        DID_APPEAR(LogLevel.DEBUG, "Ad did appear"),

        // Params:
        // <none>
        WILL_DISAPPEAR(LogLevel.DEBUG, "Ad will disappear"),

        // Params:
        // <none>
        DID_DISAPPEAR(LogLevel.DEBUG, "Ad did disappear"),

        // Params:
        // Reward amount
        // Reward currency name
        SHOULD_REWARD(LogLevel.DEBUG, "Ad should reward user with {0} {1}"),

        // Params:
        // <none>
        WILL_LEAVE_APPLICATION(LogLevel.DEBUG, "Ad will leave application"),

        // Params:
        // Custom message string
        CUSTOM(LogLevel.DEBUG, "Ad Log - {0}"),

        // Params:
        // Custom message string
        // Throwable message string
        CUSTOM_WITH_THROWABLE(LogLevel.DEBUG, "Ad Log With Throwable - {0}, {1}");

        private LogLevel mLogLevel;
        private String mMessageFormat;

        AdLogEvent(@NonNull final LogLevel logLevel, @NonNull final String messageFormat) {
            Preconditions.checkNotNull(logLevel);
            Preconditions.checkNotNull(messageFormat);

            mLogLevel = logLevel;
            mMessageFormat = messageFormat;
        }

        @Override
        @NonNull
        public String getMessage(@Nullable final Object... args) {
            return format(mMessageFormat, args);
        }

        @Override
        @NonNull
        public LogLevel getLogLevel() {
            return mLogLevel;
        }
    }

    public enum AdapterLogEvent implements MPLogEventType {

        // Params:
        // Base ad name
        // Creative ID
        // DSP Name
        LOAD_ATTEMPTED(LogLevel.DEBUG, "Adapter {0} attempting to load ad{1}{2}"),

        // Params:
        // Base ad name
        LOAD_SUCCESS(LogLevel.DEBUG, "Adapter {0} successfully loaded ad"),

        // Params:
        // Base ad name
        // Error Code
        // Error Message
        LOAD_FAILED(LogLevel.DEBUG, "Adapter {0} failed to load ad: ({1}) {2}"),

        // Params:
        // Base ad name
        SHOW_ATTEMPTED(LogLevel.DEBUG, "Adapter {0} attempting to show ad"),

        // Params:
        // Base ad name
        SHOW_SUCCESS(LogLevel.DEBUG, "Adapter {0} successfully showed ad"),

        // Params:
        // Base ad name
        // Error Code
        // Error Message
        SHOW_FAILED(LogLevel.DEBUG, "Adapter {0} failed to show ad: ({1}) {2}"),

        // Params:
        // Base Ad Name
        // Expiration time interval in seconds
        EXPIRED(LogLevel.DEBUG, "Adapter {0} expired since it was not shown within {1} seconds of it being loaded"),

        // Params:
        // Base Ad Name
        CLICKED(LogLevel.DEBUG, "Adapter {0} clicked"),

        // Params:
        // Base Ad Name
        WILL_APPEAR(LogLevel.DEBUG, "Adapter {0} will appear"),

        // Params:
        // Base Ad Name
        DID_APPEAR(LogLevel.DEBUG, "Adapter {0} did appear"),

        // Params:
        // Base Ad Name
        WILL_DISAPPEAR(LogLevel.DEBUG, "Adapter {0} will disappear"),

        // Params:
        // Base Ad Name
        // <none>
        DID_DISAPPEAR(LogLevel.DEBUG, "Adapter {0} did disappear"),

        // Params:
        // Base Ad Name
        // Reward amount
        // Reward currency name
        SHOULD_REWARD(LogLevel.DEBUG, "Adapter {0} should reward user with {1} {2}"),

        // Params:
        // Base Ad Name
        WILL_LEAVE_APPLICATION(LogLevel.DEBUG, "Adapter {0} will leave application"),

        // Params:
        // Base Ad Name
        // Custom message string
        CUSTOM(LogLevel.DEBUG, "Adapter {0} Log - {1}"),

        // Params:
        // Base Ad Name
        // Custom message string
        // Throwable message string
        CUSTOM_WITH_THROWABLE(LogLevel.DEBUG, "Adapter {0} Log With Throwable - {1}, {2}");

        private LogLevel mLogLevel;
        private String mMessageFormat;

        AdapterLogEvent(@NonNull final LogLevel logLevel, @NonNull final String messageFormat) {
            Preconditions.checkNotNull(logLevel);
            Preconditions.checkNotNull(messageFormat);

            mLogLevel = logLevel;
            mMessageFormat = messageFormat;
        }

        @Override
        @NonNull
        public String getMessage(@Nullable final Object... args) {
            final MessageFormat mf = new MessageFormat(mMessageFormat);
            final Object[] params = Arrays.copyOf(args, mf.getFormats().length);

            if (this == LOAD_ATTEMPTED) {
                if (args.length > 1 && args[1] != null && !TextUtils.isEmpty(args[1].toString())) {
                    params[1] = format(" with DSP creative ID {0}", args[1].toString());
                } else {
                    params[1] = "";
                }

                if (args.length > 2 && args[2] != null && !TextUtils.isEmpty(args[2].toString())) {
                    params[2] = format(" with DSP name {0}", args[2].toString());
                } else {
                    params[2] = "";
                }
            }

            return mf.format(params);
        }

        @Override
        @NonNull
        public LogLevel getLogLevel() {
            return mLogLevel;
        }
    }

    public enum ConsentLogEvent implements MPLogEventType {

        // Params:
        // <none>
        SYNC_ATTEMPTED(LogLevel.DEBUG, "Consent attempting to synchronize state"),

        // Params:
        // Optional message
        SYNC_COMPLETED(LogLevel.DEBUG, "Consent synchronization completed: {0}"),

        // Params:
        // Error Code
        // Error Message
        SYNC_FAILED(LogLevel.DEBUG, "Consent synchronization failed: ({0}) {1}"),

        // Params:
        // Current consent state
        // Old consent state
        // Can collect personal info
        // Reason
        UPDATED(LogLevel.DEBUG, "Consent changed from {0} to {1}: PII {2} be collected. Reason: {3}"),

        // Params:
        // <none>
        SHOULD_SHOW_DIALOG(LogLevel.DEBUG, "Consent dialog should be shown"),

        // Params:
        // <none>
        LOAD_ATTEMPTED(LogLevel.DEBUG, "Consent attempting to load dialog"),

        // Params:
        // <none>
        LOAD_SUCCESS(LogLevel.DEBUG, "Consent dialog loaded"),

        // Params:
        // Error Code
        // Error Message
        LOAD_FAILED(LogLevel.DEBUG, "Consent dialog failed: ({0}) {1}"),

        // Params:
        // <none>
        SHOW_ATTEMPTED(LogLevel.DEBUG, "Consent dialog attempting to show"),

        // Params:
        // <none>
        SHOW_SUCCESS(LogLevel.DEBUG, "Consent successfully showed dialog"),

        // Params:
        // Error Code
        // Error Message
        SHOW_FAILED(LogLevel.DEBUG, "Consent dialog failed to show: ({0}) {1}"),

        // Params:
        // Custom message string
        CUSTOM(LogLevel.DEBUG, "Consent Log - {0}"),

        // Params:
        // Custom message string
        // Throwable message string
        CUSTOM_WITH_THROWABLE(LogLevel.DEBUG, "Consent Log With Throwable - {0}, {1}");

        private LogLevel mLogLevel;
        private String mMessageFormat;

        ConsentLogEvent(@NonNull final LogLevel logLevel, @NonNull final String messageFormat) {
            Preconditions.checkNotNull(logLevel);
            Preconditions.checkNotNull(messageFormat);

            mLogLevel = logLevel;
            mMessageFormat = messageFormat;
        }

        @Override
        @NonNull
        public String getMessage(@Nullable final Object... args) {
            if (this == UPDATED && args != null && args.length > 2) {
                args[2] = (args[2] instanceof Boolean && (Boolean) args[2])
                        ? "can"
                        : "cannot";
            }

            return format(mMessageFormat, args);
        }

        @Override
        @NonNull
        public LogLevel getLogLevel() {
            return mLogLevel;
        }
    }

    public enum SdkLogEvent implements MPLogEventType {

        // Params:
        // <none>
        INIT_STARTED(LogLevel.DEBUG, "SDK initialization started"),

        // Params:
        // Comma delimited string of networks that were initialized, or "No networks initialized."
        // Comma delimited string of advanced bidder that were initialized, or "No bidders initialized."
        INIT_FINISHED(LogLevel.INFO, "SDK initialized and ready to display ads.\nInitialized adapters:\n{0}"),

        INIT_FAILED(LogLevel.INFO, "SDK initialization failed - {0}\n{1}"),

        // Params:
        // Custom message string
        CUSTOM(LogLevel.DEBUG, "SDK Log - {0}"),

        // Params:
        // Custom message string
        // Throwable message string
        CUSTOM_WITH_THROWABLE(LogLevel.DEBUG, "SDK Log With Throwable - {0}, {1}"),

        // Params:
        // Custom message string
        ERROR(LogLevel.DEBUG, "SDK Error Log - {0}"),

        // Params:
        // Custom message string
        // Throwable message string
        ERROR_WITH_THROWABLE(LogLevel.DEBUG, "SDK Error Log - {0}, {1}");

        private LogLevel mLogLevel;
        private String mMessageFormat;

        SdkLogEvent(@NonNull final LogLevel logLevel, @NonNull final String messageFormat) {
            Preconditions.checkNotNull(logLevel);
            Preconditions.checkNotNull(messageFormat);

            mLogLevel = logLevel;
            mMessageFormat = messageFormat;
        }

        @Override
        @NonNull
        public String getMessage(@Nullable final Object... args) {
            if (this == INIT_FINISHED && args.length > 0) {
                final String adapters = getDelimitedString(args[0], "\n");

                if (TextUtils.isEmpty(adapters)) {
                    args[0] = "No adapters initialized.";
                } else {
                    args[0] = adapters;
                }
            }

            return format(mMessageFormat, args);
        }

        @Override
        @NonNull
        public LogLevel getLogLevel() {
            return mLogLevel;
        }
    }

}
