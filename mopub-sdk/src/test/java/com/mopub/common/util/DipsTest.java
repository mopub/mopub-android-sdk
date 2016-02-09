package com.mopub.common.util;

import android.app.Activity;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class DipsTest {

    @Mock private Activity activity;
    @Mock private Resources resources;

    @Before
    public void setUp() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        displayMetrics.widthPixels = 480;
        displayMetrics.heightPixels = 800;
        displayMetrics.density = 2;
        when(activity.getResources()).thenReturn(resources);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);
    }

    @Test
    public void screenWidthAsIntDips_shouldReturnTheWidthAsDips() throws Exception {
        assertThat(Dips.screenWidthAsIntDips(activity)).isEqualTo(240);
    }

    @Test
    public void screenHeightAsIntDips_shouldReturnTheHeightAsDips() throws Exception {
        assertThat(Dips.screenHeightAsIntDips(activity)).isEqualTo(400);
    }
}
