package com.mopub.nativeads.test.support;

import android.graphics.Bitmap;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowBitmap;

// XXX the config for a newly created ShadowBitmap is never set so we need to set it ourselves
// https://github.com/robolectric/robolectric/issues/876
@Implements(Bitmap.class)
public class MoPubShadowBitmap extends ShadowBitmap {

    public MoPubShadowBitmap() {
        // can also be some other config value
        setConfig(Bitmap.Config.ARGB_8888);
    }
}

