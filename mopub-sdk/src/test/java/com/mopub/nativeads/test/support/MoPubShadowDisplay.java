package com.mopub.nativeads.test.support;

import android.graphics.Point;
import android.view.Display;
import android.view.Surface;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowDisplay;

/* Our old version of Robolectric doesn't have the newer Display.class methods implemented. */
@Implements(Display.class)
public class MoPubShadowDisplay extends ShadowDisplay {

    public void getSize(Point size) {
        size.set(getWidth(), getHeight());
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
