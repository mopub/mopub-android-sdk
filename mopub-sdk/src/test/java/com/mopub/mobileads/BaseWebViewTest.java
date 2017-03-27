package com.mopub.mobileads;


import android.app.Activity;
import android.os.Build.VERSION_CODES;
import android.view.ViewGroup;
import android.webkit.WebSettings;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.fakes.RoboWebSettings;
import org.robolectric.shadows.ShadowWebView;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class BaseWebViewTest {
    private Activity context;
    private BaseWebView subject;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
    }

    @Config(sdk = VERSION_CODES.JELLY_BEAN)
    @Test
    public void constructor_beforeJellyBeanMr1_shouldDisablePluginsByDefault() throws Exception {
        subject = new BaseWebView(context);

        WebSettings webSettings = subject.getSettings();
        assertThat(webSettings.getPluginState()).isEqualTo(WebSettings.PluginState.OFF);

        subject.enablePlugins(true);
        assertThat(webSettings.getPluginState()).isEqualTo(WebSettings.PluginState.ON);
    }

    @Test
    public void constructor_shouldDisableFileAccess() {
        subject = new BaseWebView(context);

        final WebSettings webSettings = subject.getSettings();
        assertThat(webSettings.getAllowFileAccess()).isEqualTo(false);
    }

    @Test
    public void constructor_shouldDisableContentAccess() {
        subject = new BaseWebView(context);

        final WebSettings webSettings = subject.getSettings();
        assertThat(webSettings.getAllowContentAccess()).isEqualTo(false);
    }

    @Test
    public void constructor_shouldDisableAccessFromFileUrls() {
        subject = new BaseWebView(context);

        final WebSettings webSettings = subject.getSettings();
        assertThat(webSettings.getAllowFileAccessFromFileURLs()).isEqualTo(false);
        assertThat(webSettings.getAllowUniversalAccessFromFileURLs()).isEqualTo(false);
    }

    @Config(sdk = VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void constructor_atLeastJellybeanMr2_shouldPass() throws Exception {
        subject = new BaseWebView(context);

        subject.enablePlugins(true);

        // pass
    }

    @Test
    public void enableJavascriptCaching_enablesJavascriptDomStorageAndAppCache() {
        subject = new BaseWebView(context);
        final RoboWebSettings settings = (RoboWebSettings) subject.getSettings();

        subject.enableJavascriptCaching();

        assertThat(settings.getJavaScriptEnabled()).isTrue();
        assertThat(settings.getDomStorageEnabled()).isTrue();
        assertThat(settings.getAppCacheEnabled()).isTrue();
        assertThat(settings.getAppCachePath()).isEqualTo(context.getCacheDir().getAbsolutePath());
    }

    @Test
    public void destroy_shouldRemoveSelfFromParent_beforeCallingDestroy() throws Exception {
        subject = new BaseWebView(context);
        ViewGroup parent = mock(ViewGroup.class);
        ShadowWebView shadow = Shadows.shadowOf(subject);
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
