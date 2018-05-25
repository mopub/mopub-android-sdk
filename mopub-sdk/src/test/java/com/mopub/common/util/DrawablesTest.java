package com.mopub.common.util;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class DrawablesTest {
    @Test
    public void createDrawable_shouldNotCacheDrawables() throws Exception {
        assertThat(Drawables.BACKGROUND.createDrawable(
                Robolectric.buildActivity(Activity.class).create().get())).isNotSameAs(
                Drawables.BACKGROUND.createDrawable(
                        Robolectric.buildActivity(Activity.class).create().get()));
    }

    @Test
    public void getBitmap_shouldCacheBitmap() throws Exception {
        assertThat(Drawables.BACKGROUND.getBitmap())
                .isSameAs(Drawables.BACKGROUND.getBitmap());
    }
}
