package com.mopub.network;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class NetworkingTest {
    private Activity context;
    static volatile String sUserAgent;

    @Before
    public void setUp() {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @After
    public void tearDown() {
        Networking.clearForTesting();
        sUserAgent = null;
    }

    @Test
    public void getUserAgent_usesCachedUserAgent() {
        Networking.setUserAgentForTesting("some cached user agent");
        String userAgent = Networking.getUserAgent(context);

        assertThat(userAgent).isEqualTo("some cached user agent");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void getUserAgent_withSdkVersion16_shouldIncludeAndroid() {
        String userAgent = Networking.getUserAgent(context);

        assertThat(userAgent).containsIgnoringCase("android");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void getUserAgent_withSdkVersionGreaterThan16_shouldIncludeAndroid() {
        String userAgent = Networking.getUserAgent(context);

        assertThat(userAgent).containsIgnoringCase("android");
    }

    @Test
    public void getCachedUserAgent_usesCachedUserAgent() {
        Networking.setUserAgentForTesting("some cached user agent");
        String userAgent = Networking.getCachedUserAgent();

        assertThat(userAgent).isEqualTo("some cached user agent");
    }
}
