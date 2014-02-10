package com.mopub.mobileads;

import java.util.HashMap;
import java.util.Map;

/**
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
        return android.util.Log.v(tag, msg);
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
        return android.util.Log.v(tag, msg, tr);
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
        return android.util.Log.d(tag, msg);
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
        return android.util.Log.d(tag, msg, tr);
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
        return android.util.Log.i(tag, msg);
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
        return android.util.Log.i(tag, msg, tr);
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
        return android.util.Log.w(tag, msg);
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
        return android.util.Log.w(tag, msg, tr);
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
        return android.util.Log.w(tag, tr);
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
        return android.util.Log.e(tag, msg);
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
        return android.util.Log.e(tag, msg, tr);
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
        return android.util.Log.wtf(tag, msg);
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
        return android.util.Log.wtf(tag, tr);
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
        return android.util.Log.wtf(tag, msg, tr);
    }

    private static void broadcastLoggerSideEffect(LoggerSideEffect loggerSideEffect) {
        if(loggerIntegerMap != null) for(Logger logger : loggerIntegerMap.keySet()) {
            loggerSideEffect.performSideEffect(logger);
        }
    }
}
