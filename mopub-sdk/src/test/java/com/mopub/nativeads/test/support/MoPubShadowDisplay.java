package com.mopub.nativeads.test.support;

import android.graphics.Point;
import android.view.Display;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowDisplay;

/* Our old version of Robolectric doesn't have the newer Display.class methods implemented. */
@Implements(Display.class)
public class MoPubShadowDisplay extends ShadowDisplay {

    public void getSize(Point size) {
        size.set(getWidth(), getHeight());
    }
}
