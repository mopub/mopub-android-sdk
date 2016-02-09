package com.mopub;

import android.os.Build;

import org.robolectric.util.ReflectionHelpers;

public class TestSdkHelper {

    /**
     * Sets the SDK level using reflection. Must only be used in tests, in situations where
     * Robolectric does not support a given level. Only sets SDK level for one test.
     * If you need this value to apply to the whole class, take care to set this in {@code #setUp}.
     *
     * Be careful when setting this value before calling Robolectric code, as it can interfere with
     * some Robolectric behaviors (like attempting to call older API methods that do not
     * exist in the android.jar you are executing against.)
     *
     */
    public static void setReportedSdkLevel(final int sdkLevel) {
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", sdkLevel);
    }

}
