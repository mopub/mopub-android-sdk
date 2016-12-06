package com.mopub.common.logging;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.util.Log;

import com.mopub.common.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MoPubLog {
    public static final String LOGGER_NAMESPACE = "com.mopub";

    private static final String LOGTAG = "MoPub";
    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAMESPACE);
    private static final MoPubLogHandler LOG_HANDLER = new MoPubLogHandler();

    /**
     * Sets up the {@link Logger}, {@link Handler}, and prevents any parent Handlers from being
     * notified to avoid duplicated log messages.
     */
    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);
        LOG_HANDLER.setLevel(Level.FINE);

        LogManager.getLogManager().addLogger(LOGGER);
        addHandler(LOGGER, LOG_HANDLER);
    }

    private MoPubLog() {}

    public static void c(final String message) {
        MoPubLog.c(message, null);
    }

    public static void v(final String message) {
        MoPubLog.v(message, null);
    }

    public static void d(final String message) {
        MoPubLog.d(message, null);
    }

    public static void i(final String message) {
        MoPubLog.i(message, null);
    }

    public static void w(final String message) {
        MoPubLog.w(message, null);
    }

    public static void e(final String message) {
        MoPubLog.e(message, null);
    }

    public static void c(final String message, final Throwable throwable) {
        LOGGER.log(Level.FINEST, message, throwable);
    }

    public static void v(final String message, final Throwable throwable) {
        LOGGER.log(Level.FINE, message, throwable);
    }

    public static void d(final String message, final Throwable throwable) {
        LOGGER.log(Level.CONFIG, message, throwable);
    }

    public static void i(final String message, final Throwable throwable) {
        LOGGER.log(Level.INFO, message, throwable);
    }

    public static void w(final String message, final Throwable throwable) {
        LOGGER.log(Level.WARNING, message, throwable);
    }

    public static void e(final String message, final Throwable throwable) {
        LOGGER.log(Level.SEVERE, message, throwable);
    }

    @VisibleForTesting
    public static void setSdkHandlerLevel(@NonNull final Level level) {
        LOG_HANDLER.setLevel(level);
    }

    /**
     * Adds a {@link Handler} to a {@link Logger} if they are not already associated.
     */
    private static void addHandler(@NonNull final Logger logger,
            @NonNull final Handler handler) {
        final Handler[] currentHandlers = logger.getHandlers();
        for (final Handler currentHandler : currentHandlers) {
            if (currentHandler.equals(handler)) {
                return;
            }
        }
        logger.addHandler(handler);
    }

    private static final class MoPubLogHandler extends Handler {
        private static final Map<Level, Integer> LEVEL_TO_LOG = new HashMap<Level, Integer>(7);

        /*
         * Mapping between Level.* and Log.*:
         * Level.FINEST  => Log.v
         * Level.FINER   => Log.v
         * Level.FINE    => Log.v
         * Level.CONFIG  => Log.d
         * Level.INFO    => Log.i
         * Level.WARNING => Log.w
         * Level.SEVERE  => Log.e
         */
        static {
            LEVEL_TO_LOG.put(Level.FINEST, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.FINER, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.FINE, Log.VERBOSE);
            LEVEL_TO_LOG.put(Level.CONFIG, Log.DEBUG);
            LEVEL_TO_LOG.put(Level.INFO, Log.INFO);
            LEVEL_TO_LOG.put(Level.WARNING, Log.WARN);
            LEVEL_TO_LOG.put(Level.SEVERE, Log.ERROR);
        }

        @Override
        @SuppressLint("LogTagMismatch")
        public void publish(final LogRecord logRecord) {
            if (isLoggable(logRecord)) {
                final int priority;
                if (LEVEL_TO_LOG.containsKey(logRecord.getLevel())) {
                    priority = LEVEL_TO_LOG.get(logRecord.getLevel());
                } else {
                    priority = Log.VERBOSE;
                }

                String message = logRecord.getMessage() + "\n";

                final Throwable error = logRecord.getThrown();
                if (error != null) {
                    message += Log.getStackTraceString(error);
                }

                Log.println(priority, LOGTAG, message);
            }
        }

        @Override public void close() {}

        @Override public void flush() {}
    }
}
