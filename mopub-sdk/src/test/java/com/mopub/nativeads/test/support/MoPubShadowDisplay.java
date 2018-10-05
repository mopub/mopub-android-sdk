// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads.test.support;

import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowDisplay;
import org.robolectric.shadows.ShadowDisplayManager;

/* Our old version of Robolectric doesn't have the newer Display.class methods implemented. */
@Implements(Display.class)
public class MoPubShadowDisplay extends ShadowDisplay {
    public void getSize(Point size) {
        Display display = getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        size.set(metrics.widthPixels, metrics.heightPixels);
    }

    private static int sRotation = Surface.ROTATION_0;

    @Implementation
    public int getRotation() {
        return sRotation;
    }

    public static void setStaticRotation(int rotation) {
        sRotation = rotation;
    }
}
