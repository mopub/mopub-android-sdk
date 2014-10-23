package com.mopub.common.util;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static com.mopub.common.util.VersionCode.BASE;
import static com.mopub.common.util.VersionCode.CUR_DEVELOPMENT;
import static com.mopub.common.util.VersionCode.DONUT;
import static com.mopub.common.util.VersionCode.FROYO;
import static com.mopub.common.util.VersionCode.ICE_CREAM_SANDWICH;
import static com.mopub.common.util.VersionCode.JELLY_BEAN;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class VersionCodeTest {
    @Test
    public void currentApiLevel_shouldReflectActualApiLevel() throws Exception {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 4);
        assertThat(VersionCode.currentApiLevel()).isEqualTo(DONUT);

        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 8);
        assertThat(VersionCode.currentApiLevel()).isEqualTo(FROYO);

        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 14);
        assertThat(VersionCode.currentApiLevel()).isEqualTo(ICE_CREAM_SANDWICH);
    }

    @Test
    public void currentApiLevel_whenUnknownApiLevel_shouldReturnCurDevelopment() throws Exception {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", 900);
        assertThat(VersionCode.currentApiLevel()).isEqualTo(CUR_DEVELOPMENT);
    }

    @Test
    public void isAtMost_shouldCompareVersions() throws Exception {
        assertThat(ICE_CREAM_SANDWICH.isAtMost(BASE)).isFalse();
        assertThat(ICE_CREAM_SANDWICH.isAtMost(JELLY_BEAN)).isTrue();
        assertThat(ICE_CREAM_SANDWICH.isAtMost(ICE_CREAM_SANDWICH)).isTrue();
    }

    @Test
    public void isAtLeast_shouldCompareVersions() throws Exception {
        assertThat(ICE_CREAM_SANDWICH.isAtLeast(BASE)).isTrue();
        assertThat(ICE_CREAM_SANDWICH.isAtLeast(JELLY_BEAN)).isFalse();
        assertThat(ICE_CREAM_SANDWICH.isAtLeast(ICE_CREAM_SANDWICH)).isTrue();
    }

    @Test
    public void isBelow_shouldCompareVersions() throws Exception {
        assertThat(ICE_CREAM_SANDWICH.isBelow(BASE)).isFalse();
        assertThat(ICE_CREAM_SANDWICH.isBelow(JELLY_BEAN)).isTrue();
        assertThat(ICE_CREAM_SANDWICH.isBelow(ICE_CREAM_SANDWICH)).isFalse();
    }
}
