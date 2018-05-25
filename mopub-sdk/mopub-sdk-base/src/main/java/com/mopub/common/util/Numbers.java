package com.mopub.common.util;

public class Numbers {
    private Numbers() {}

    /**
     * Tries to parse the double value from a Number or String.
     * @param value the object to parse.
     * @return a {@code Double} instance containing the parsed double value.
     * @throws ClassCastException if {@code value} cannot be parsed as a double value.
     */
    public static Double parseDouble(final Object value) throws ClassCastException {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.valueOf((String) value);
            } catch (NumberFormatException e) {
                throw new ClassCastException("Unable to parse " + value + " as double.");
            }
        } else {
            throw new ClassCastException("Unable to parse " + value + " as double.");
        }
    }

    /**
     * Rounds up to the nearest full second. Formally, this is the long
     * closest to negative infinity above or equal to millis, in milliseconds,
     * converted to seconds.
     *
     * @param millis Time in milliseconds
     * @return Time in seconds, rounded up.
     */
    public static long convertMillisecondsToSecondsRoundedUp(final long millis) {
        return Math.round(Math.ceil(millis / 1000f));
    }
}
