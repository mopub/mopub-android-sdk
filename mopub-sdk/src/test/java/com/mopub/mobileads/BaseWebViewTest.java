package com.mopub.mobileads;


import android.app.Activity;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.view.ViewGroup;
import android.webkit.WebSettings;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowWebView;

import static com.mopub.common.util.VersionCode.JELLY_BEAN_MR1;
import static com.mopub.common.util.VersionCode.JELLY_BEAN_MR2;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class BaseWebViewTest {
    private Activity context;
    private BaseWebView subject;

    @Before
    public void setup() {
        context = new Activity();
    }

    @Config(reportSdk = VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void beforeJellyBeanMr1_shouldDisablePluginsByDefault() throws Exception {
        subject = new BaseWebView(context);

        WebSettings webSettings = subject.getSettings();
        assertThat(webSettings.getPluginState()).isEqualTo(WebSettings.PluginState.OFF);

        subject.enablePlugins(true);
        assertThat(webSettings.getPluginState()).isEqualTo(WebSettings.PluginState.ON);
    }

    @Config(reportSdk = VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void atLeastJellybeanMr2_shouldPass() throws Exception {
        subject = new BaseWebView(context);

        subject.enablePlugins(true);

        // pass
    }

    @Test
    public void destroy_shouldRemoveSelfFromParent_beforeCallingDestroy() throws Exception {
        subject = new BaseWebView(context);
        ViewGroup parent = mock(ViewGroup.class);
        ShadowWebView shadow = shadowOf(subject);
        shadow.setMyParent(parent);

        subject.destroy();

        verify(parent).removeView(eq(subject));
        assertThat(shadow.wasDestroyCalled()).isTrue();
    }

    @Test
    public void destroy_shouldSetTheCorrectStateVariable() {
        subject = new BaseWebView(context);

        assertThat(subject.mIsDestroyed).isFalse();

        subject.destroy();

        assertThat(subject.mIsDestroyed).isTrue();
    }
}
