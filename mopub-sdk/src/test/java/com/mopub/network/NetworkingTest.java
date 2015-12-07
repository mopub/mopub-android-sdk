package com.mopub.network;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.concurrent.Semaphore;

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

    @Test
    public void getUserAgent_fromMainThread_shouldIncludeAndroid() throws InterruptedException {
        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String userAgent = Networking.getUserAgent(context);
                assertThat(userAgent).containsIgnoringCase("android");
            }
        });
    }

    @Ignore("Flaky - setProperty + threading is unreliable in the test environment.")
    @Test
    public void getUserAgent_fromBackgroundThread_shouldIncludeAndroid() throws InterruptedException {
        System.setProperty("http.agent", "system level user agent");

        final Semaphore semaphore = new Semaphore(0);

        new Thread(new Runnable() {
            @Override
            public void run() {
                sUserAgent = Networking.getUserAgent(context);
                semaphore.release();
            }
        }).start();

        semaphore.acquire();
        assertThat(sUserAgent).isEqualTo("system level user agent");
    }

    public void getCachedUserAgent_usesCachedUserAgent() {
        Networking.setUserAgentForTesting("some cached user agent");
        String userAgent = Networking.getCachedUserAgent();

        assertThat(userAgent).isEqualTo("some cached user agent");
    }
}
