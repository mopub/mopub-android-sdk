package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.mopub.TestSdkHelper;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.GestureUtils;
import com.mopub.mobileads.test.support.VastUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowWebView;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastWebViewTest {

    private VastWebView subject;
    private Context context;
    @Mock VastWebView.VastWebViewClickListener mockVastWebViewClickListener;
    @Mock private VastResource mockResource;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        subject = new VastWebView(context);
        subject.setVastWebViewClickListener(mockVastWebViewClickListener);
    }

    @Test
    public void constructor_shouldSetOnTouchListener() throws Exception {
        assertThat(Shadows.shadowOf(subject).getOnTouchListener())
                .isInstanceOf(VastWebView.VastWebViewOnTouchListener.class);
    }

    @Config(sdk = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void pluginState_atLeastJellybeanMr2_shouldDefaultToOff_shouldNeverBeEnabled()  {
        subject = new VastWebView(Robolectric.buildActivity(Activity.class).create().get());
        assertThat(subject.getSettings().getPluginState()).isEqualTo(WebSettings.PluginState.OFF);

        subject.enablePlugins(true);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(WebSettings.PluginState.OFF);
    }

    @Test
    public void pluginState_BelowJellybeanMr2_shouldDefaultToOn_shouldAllowToggling() {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.JELLY_BEAN);
        subject = new VastWebView(context);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(WebSettings.PluginState.ON);

        subject.enablePlugins(false);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(WebSettings.PluginState.OFF);

        subject.enablePlugins(true);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(WebSettings.PluginState.ON);
    }

    @Test
    public void loadData_shouldCallLoadDataWithBaseURL() throws Exception {
        String data = "some random html response";
        subject.loadData(data);

        ShadowWebView.LoadDataWithBaseURL lastLoadData
                = Shadows.shadowOf(subject).getLastLoadDataWithBaseURL();
        assertThat(lastLoadData.baseUrl).isEqualTo("http://ads.mopub.com/");
        assertThat(lastLoadData.data).isEqualTo(data);
        assertThat(lastLoadData.mimeType).isEqualTo("text/html");
        assertThat(lastLoadData.encoding).isEqualTo("utf-8");
        assertThat(lastLoadData.historyUrl).isNull();
    }

    @Test
    public void VastWebViewOnTouchListener_withActionDown_withActionUp_shouldCallOnVastWebViewClick() throws Exception {
        View.OnTouchListener onTouchListener = Shadows.shadowOf(subject).getOnTouchListener();
        onTouchListener.onTouch(subject, GestureUtils.createActionDown(0, 0));
        onTouchListener.onTouch(subject, GestureUtils.createActionUp(0, 0));

        verify(mockVastWebViewClickListener).onVastWebViewClick();
    }

    @Test
    public void createView_shouldInitializeAndReturnView() throws Exception {
        VastIconConfig vastIconConfig = new VastIconConfig(123, 456, 789, 101,
                mockResource,
                VastUtils.stringsToVastTrackers("clickTrackerOne", "clickTrackerTwo"),
                "clickThroughUri",
                VastUtils.stringsToVastTrackers("viewTrackerOne", "viewTrackerTwo")
        );

        WebView webView = subject.createView(context, vastIconConfig.getVastResource());
        assertThat(webView).isNotNull();
        verify(mockResource).initializeWebView(any(VastWebView.class));
    }
}
