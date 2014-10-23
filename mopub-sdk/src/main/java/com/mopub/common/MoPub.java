package com.mopub.common;

public class MoPub {
    public static final String SDK_VERSION = "3.2.1";

    public static enum LocationAwareness { NORMAL, TRUNCATED, DISABLED }

    private static final int DEFAULT_LOCATION_PRECISION = 6;
    private static volatile LocationAwareness sLocationLocationAwareness = LocationAwareness.NORMAL;
    private static volatile int sLocationPrecision = DEFAULT_LOCATION_PRECISION;

    public static LocationAwareness getLocationAwareness() {
        return sLocationLocationAwareness;
    }

    public static void setLocationAwareness(LocationAwareness locationAwareness) {
        sLocationLocationAwareness = locationAwareness;
    }

    public static int getLocationPrecision() {
        return sLocationPrecision;
    }

    /**
     * Sets the precision to use when the SDK's location awareness is set
     * to {@link com.mopub.common.MoPub.LocationAwareness#TRUNCATED}.
     */
    public static void setLocationPrecision(int precision) {
        sLocationPrecision = Math.min(Math.max(0, precision), DEFAULT_LOCATION_PRECISION);
    }
}
