package com.mopub.common;


import android.os.Looper;

import com.mopub.common.logging.MoPubLog;

import java.util.IllegalFormatException;

/**
 * Simple static methods to be called at the start of your own methods to verify correct arguments
 * and state.
 *
 * Each method supports 2 flavors - one that will always throw a runtime exception, and a NoThrow
 * version that will only throw an exception when in strict mode. We use the former
 * for internal state checks, and the later to validate arguments passed to the SDK.
 */
public final class Preconditions {

    public static final String EMPTY_ARGUMENTS = "";

    private Preconditions() {
        // Non-instantiable.
    }

    /**
     * Ensures the truth of an expression.
     */
    public static void checkArgument(boolean expression) {
        checkArgumentInternal(expression, true, "Illegal argument.", EMPTY_ARGUMENTS);
    }

    /**
     * Ensures the truth of an expression, with an error message.
     */
    public static void checkArgument(boolean expression, String errorMessage) {
        checkArgumentInternal(expression, true, errorMessage, EMPTY_ARGUMENTS);
    }

    /**
     * Ensures the truth of an expression, with an error message that takes arguments.
     */
    public static void checkArgument(boolean expression,
            String errorMessageTemplate, Object... errorMessageArgs) {
        checkArgumentInternal(expression, true, errorMessageTemplate, errorMessageArgs);
    }

    /**
     * Ensures the truth of an expression involving the state of the caller.
     */
    public static void checkState(boolean expression) {
        checkStateInternal(expression, true, "Illegal state.", EMPTY_ARGUMENTS);
    }

    /**
     * Ensures the truth of an expression involving the state of the caller, with an error message.
     */
    public static void checkState(boolean expression, String errorMessage) {
        checkStateInternal(expression, true, errorMessage, EMPTY_ARGUMENTS);
    }

    /**
     * Ensures the truth of an expression involving the state of the caller, with an error message
     * that takes arguments.
     */
    public static void checkState(boolean expression,
            String errorMessageTemplate, Object... errorMessageArgs) {
        checkStateInternal(expression, true, errorMessageTemplate, errorMessageArgs);
    }

    /**
     * Ensures that an object reference is not null.
     */
    public static void checkNotNull(Object reference) {
        checkNotNullInternal(reference, true, "Object can not be null.", EMPTY_ARGUMENTS);
    }

    /**
     * Ensures that an object reference is not null, with an error message.
     */
    public static void checkNotNull(Object reference, String errorMessage) {
        checkNotNullInternal(reference, true, errorMessage, EMPTY_ARGUMENTS);
    }

    /**
     * Ensures that an object reference is not null, with an error message that takes arguments.
     */
    public static void checkNotNull(Object reference,
            String errorMessageTemplate, Object... errorMessageArgs) {
        checkNotNullInternal(reference, true, errorMessageTemplate, errorMessageArgs);
    }

    /**
     * Ensures that the current thread is the UI thread.
     */
    public static void checkUiThread() {
        checkUiThreadInternal(true, "This method must be called from the UI thread.",
                EMPTY_ARGUMENTS);
    }

    /**
     * Ensures that the current thread is the UI thread, with an error message.
     */
    public static void checkUiThread(String errorMessage) {
        checkUiThreadInternal(true, errorMessage, EMPTY_ARGUMENTS);
    }

    /**
     * Ensures that the current thread is the UI thread, with an error message that takes
     * arguments.
     */
    public static void checkUiThread(String errorMessageTemplate, Object... errorMessageArgs) {
        checkUiThreadInternal(true, errorMessageTemplate, errorMessageArgs);
    }

    /**
     * Preconditions checks that avoid throwing and exception in release mode. These versions return
     * a boolean which the caller should check.
     */
    public final static class NoThrow {
        private static volatile boolean sStrictMode = false;

        /**
         * Enables or disables strict mode.
         *
         * In strict mode, this class will throw anyway. For example, you could set strict mode to
         * BuildConfig.DEBUG to always get exceptions when in the IDE.
         *
         * @param strictMode Whether to use strict mode.
         */
        public static void setStrictMode(boolean strictMode) {
            sStrictMode = strictMode;
        }

        /**
         * Ensures the truth of an expression.
         */
        public static boolean checkArgument(boolean expression) {
            return checkArgumentInternal(expression, sStrictMode, "Illegal argument",
                    EMPTY_ARGUMENTS);
        }

        /**
         * Ensures the truth of an expression, with an error message.
         */
        public static boolean checkArgument(boolean expression, String errorMessage) {
            return checkArgumentInternal(expression, sStrictMode, errorMessage, EMPTY_ARGUMENTS);
        }

        /**
         * Ensures the truth of an expression, with an error message that takes arguments.
         */
        public static boolean checkArgument(boolean expression,
                String errorMessageTemplate, Object... errorMessageArgs) {
            return checkArgumentInternal(expression, sStrictMode, errorMessageTemplate,
                    errorMessageArgs);
        }


        /**
         * Ensures the truth of an expression involving the state of the caller.
         */
        public static boolean checkState(boolean expression) {
            return checkStateInternal(expression, sStrictMode, "Illegal state.", EMPTY_ARGUMENTS);
        }

        /**
         * Ensures the truth of an expression involving the state of the caller, with an error
         * message.
         */
        public static boolean checkState(boolean expression, String errorMessage) {
            return checkStateInternal(expression, sStrictMode, errorMessage, EMPTY_ARGUMENTS);
        }

        /**
         * Ensures the truth of an expression involving the state of the caller, with an error
         * message that takes arguments.
         */
        public static boolean checkState(boolean expression,
                String errorMessageTemplate, Object... errorMessageArgs) {
            return checkStateInternal(expression, sStrictMode, errorMessageTemplate,
                    errorMessageArgs);
        }

        /**
         * Ensures that an object reference is not null.
         */
        public static boolean checkNotNull(Object reference) {
            return checkNotNullInternal(reference, sStrictMode, "Object can not be null.",
                    EMPTY_ARGUMENTS);
        }

        /**
         * Ensures that an object reference is not null, with an error message.
         */
        public static boolean checkNotNull(Object reference, String errorMessage) {
            return checkNotNullInternal(reference, sStrictMode, errorMessage, EMPTY_ARGUMENTS);
        }

        /**
         * Ensures that an object reference is not null, with an error message that takes
         * arguments.
         */
        public static boolean checkNotNull(Object reference,
                String errorMessageTemplate, Object... errorMessageArgs) {
            return checkNotNullInternal(reference, sStrictMode, errorMessageTemplate,
                    errorMessageArgs);
        }

        /**
         * Ensures that the current thread is the UI thread.
         */
        public static boolean checkUiThread() {
            return checkUiThreadInternal(sStrictMode,
                    "This method must be called from the UI thread.", EMPTY_ARGUMENTS);
        }

        /**
         * Ensures that the current thread is the UI thread, with an error message.
         */
        public static boolean checkUiThread(String errorMessage) {
            return checkUiThreadInternal(sStrictMode, errorMessage, EMPTY_ARGUMENTS);
        }

        /**
         * Ensures that the current thread is the UI thread, with an error message that takes
         * arguments.
         */
        public static boolean checkUiThread(String errorMessageTemplate,
                Object... errorMessageArgs) {
            return checkUiThreadInternal(false, errorMessageTemplate, errorMessageArgs);
        }
    }

    private static boolean checkArgumentInternal(boolean expression, boolean allowThrow,
            String errorMessageTemplate, Object... errorMessageArgs) {
        if (expression) {
            return true;
        }
        String errorMessage = format(errorMessageTemplate, errorMessageArgs);
        if (allowThrow) {
            throw new IllegalArgumentException(errorMessage);
        }
        MoPubLog.e(errorMessage);
        return false;
    }

    private static boolean checkStateInternal(boolean expression, boolean allowThrow,
            String errorMessageTemplate, Object... errorMessageArgs) {
        if (expression) {
            return true;
        }
        String errorMessage = format(errorMessageTemplate, errorMessageArgs);
        if (allowThrow) {
            throw new IllegalStateException(errorMessage);
        }
        MoPubLog.e(errorMessage);
        return false;
    }

    private static boolean checkNotNullInternal(Object reference, boolean allowThrow,
            String errorMessageTemplate, Object... errorMessageArgs) {
        if (reference != null) {
            return true;
        }
        String errorMessage = format(errorMessageTemplate, errorMessageArgs);
        if (allowThrow) {
            throw new NullPointerException(errorMessage);
        }
        MoPubLog.e(errorMessage);
        return false;
    }

    private static boolean checkUiThreadInternal(boolean allowThrow,
            String errorMessageTemplate, Object... errorMessageArgs) {
        // Check that the main looper is the current looper.
        if (Looper.getMainLooper().equals(Looper.myLooper())) {
            return true;
        }
        String errorMessage = format(errorMessageTemplate, errorMessageArgs);
        if (allowThrow) {
            throw new IllegalStateException(errorMessage);
        }
        MoPubLog.e(errorMessage);
        return false;
    }

    /**
     * Substitutes each {@code %s} in {@code template} with an argument. These are matched by
     * position - the first {@code %s} gets {@code args[0]}, etc.
     */
    private static String format(String template, Object... args) {
        template = String.valueOf(template);  // null -> "null"

        try {
            return String.format(template, args);
        } catch (IllegalFormatException exception) {
            MoPubLog.e("MoPub preconditions had a format exception: " + exception.getMessage());
            return template;
        }
    }
}
