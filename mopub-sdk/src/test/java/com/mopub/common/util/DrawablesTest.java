package com.mopub.common.util;

import android.app.Activity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class DrawablesTest {
    @Test
    public void decodeImage_shouldCacheDrawables() throws Exception {
        assertThat(Drawables.BACKGROUND.decodeImage(new Activity()))
                .isSameAs(Drawables.BACKGROUND.decodeImage(new Activity()));
    }
}
