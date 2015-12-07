package com.mopub.common.util;

import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VisibilityTest {
    @Test
    public void isScreenVisible_shouldOnlyReturnTrueForViewVisible() throws Exception {
        assertThat(Visibility.isScreenVisible(View.VISIBLE)).isTrue();
        assertThat(Visibility.isScreenVisible(View.INVISIBLE)).isFalse();
        assertThat(Visibility.isScreenVisible(View.GONE)).isFalse();
    }
    
    @Test
    public void hasScreenVisibilityChanged_withIdenticalArguments_shouldReturnFalse() throws Exception {
        assertThat(Visibility.hasScreenVisibilityChanged(View.VISIBLE, View.VISIBLE)).isFalse();
        assertThat(Visibility.hasScreenVisibilityChanged(View.INVISIBLE, View.INVISIBLE)).isFalse();
        assertThat(Visibility.hasScreenVisibilityChanged(View.GONE, View.GONE)).isFalse();
    }

    @Test
    public void hasScreenVisibilityChanged_withTwoNonVisibleArguments_shouldReturnFalse() throws Exception {
        assertThat(Visibility.hasScreenVisibilityChanged(View.INVISIBLE, View.GONE)).isFalse();
        assertThat(Visibility.hasScreenVisibilityChanged(View.GONE, View.INVISIBLE)).isFalse();
    }

    @Test
    public void hasScreenVisibilityChanged_withDifferentVisibilities_shouldReturnTrue() throws Exception {
        assertThat(Visibility.hasScreenVisibilityChanged(View.VISIBLE, View.GONE)).isTrue();
        assertThat(Visibility.hasScreenVisibilityChanged(View.GONE, View.VISIBLE)).isTrue();
        assertThat(Visibility.hasScreenVisibilityChanged(View.VISIBLE, View.INVISIBLE)).isTrue();
        assertThat(Visibility.hasScreenVisibilityChanged(View.INVISIBLE, View.VISIBLE)).isTrue();
    }
}