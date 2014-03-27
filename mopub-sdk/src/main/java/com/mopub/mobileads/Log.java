package com.mopub.mobileads;

import java.util.HashMap;
import java.util.Map;

/**
 * Logging mechanism which supports reentrant registration of
 * {@link com.mopub.mobileads.Logger} implementors.
 * Part of the Scopely™ Platform
 * © 2013 Scopely, Inc.
 */
public class Log {
    @SuppressWarnings("UnusedDeclaration")
    private static final String TAG = Log.class.getCanonicalName();

    private static boolean logsEnabled = true;

    private static Map<Logger, Integer> loggerIntegerMap;

    private static Map<Logger, Integer> getLoggerIntegerMap() {
        if(loggerIntegerMap == null) {
            loggerIntegerMap = new HashMap<Logger, Integer>();
        }
        return loggerIntegerMap;
    }

    private static interface LoggerSideEffect {
        void performSideEffect(Logger logger);
    }

    public static void setLogsEnabled(boolean logsEnabled) {
        Log.logsEnabled = logsEnabled;
    }

    public static void registerLogger(Logger logger) {
        Map<Logger, Integer> loggerIntegerMap = getLoggerIntegerMap();
        if(!loggerIntegerMap.containsKey(logger)) {
            loggerIntegerMap.put(logger, 0);
        }
        loggerIntegerMap.put(logger, loggerIntegerMap.get(logger) + 1);
    }

    public static void unregisterLogger(Logger logger) {
        Map<Logger, Integer> loggerIntegerMap = getLoggerIntegerMap();
        if(!loggerIntegerMap.containsKey(logger)) {
            throw new RuntimeException("trying to unregister an unregistered logger");
        }
        loggerIntegerMap.put(logger, loggerIntegerMap.get(logger) - 1);
        if(loggerIntegerMap.get(logger) == 0) {
            loggerIntegerMap.remove(logger);
        }
    }

    public static int v(final String tag, final String msg) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.v(tag, msg);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.v(tag, msg) : 0;
    }

    public static int v(final String tag, final String msg, final Throwable tr) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.v(tag, msg, tr);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.v(tag, msg, tr) : 0;
    }

    public static int d(final String tag, final String msg) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.d(tag, msg);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.d(tag, msg) : 0;
    }

    public static int d(final String tag, final String msg, final Throwable tr) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.d(tag, msg, tr);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.d(tag, msg, tr) : 0;
    }

    public static int i(final String tag, final String msg) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.i(tag, msg);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.i(tag, msg) : 0;
    }

    public static int i(final String tag, final String msg, final Throwable tr) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.i(tag, msg, tr);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.i(tag, msg, tr) : 0;
    }

    public static int w(final String tag, final String msg) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.w(tag, msg);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.w(tag, msg) : 0;
    }

    public static int w(final String tag, final String msg, final Throwable tr) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.w(tag, msg, tr);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.w(tag, msg, tr) : 0;
    }

    public static int w(final String tag, final Throwable tr) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.w(tag, tr);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.w(tag, tr) : 0;
    }

    public static int e(final String tag, final String msg) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.e(tag, msg);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.e(tag, msg) : 0;
    }

    public static int e(final String tag, final String msg, final Throwable tr) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.e(tag, msg, tr);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.e(tag, msg, tr) : 0;
    }

    public static int wtf(final String tag, final String msg) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.wtf(tag, msg);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.wtf(tag, msg) : 0;
    }

    public static int wtf(final String tag, final Throwable tr) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.wtf(tag, tr);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.wtf(tag, tr) : 0;
    }

    public static int wtf(final String tag, final String msg, final Throwable tr) {
        if(!logsEnabled) {
            return 0;
        }
        broadcastLoggerSideEffect(
                new LoggerSideEffect() {
                    @Override
                    public void performSideEffect(Logger logger) {
                        logger.wtf(tag, msg, tr);
                    }
                }
        );
        return shouldLogNormally() ? android.util.Log.wtf(tag, msg, tr) : 0;
    }

    private static void broadcastLoggerSideEffect(LoggerSideEffect loggerSideEffect) {
        if(loggerIntegerMap != null) for(Logger logger : loggerIntegerMap.keySet()) {
            loggerSideEffect.performSideEffect(logger);
        }
    }

    /**
     * We only use {@link android.util.Log} methods when no loggers are registered.
     * @return whether we should be logging normally.
     */
    private static boolean shouldLogNormally() {
        return loggerIntegerMap == null || loggerIntegerMap.size() == 0;
    }
}
