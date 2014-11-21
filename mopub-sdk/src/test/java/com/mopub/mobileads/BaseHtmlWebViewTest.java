package com.mopub.mobileads;

import android.app.Activity;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowWebView;

import static android.webkit.WebSettings.PluginState;
import static com.mopub.common.util.VersionCode.HONEYCOMB_MR2;
import static com.mopub.common.util.VersionCode.ICE_CREAM_SANDWICH;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class BaseHtmlWebViewTest {

    private BaseHtmlWebView subject;
    private MotionEvent touchDown;
    private MotionEvent touchUp;
    private AdConfiguration adConfiguration;

    @Before
    public void setUp() throws Exception {
        adConfiguration = mock(AdConfiguration.class);
        subject = new BaseHtmlWebView(new Activity(), adConfiguration);

        touchDown = createMotionEvent(MotionEvent.ACTION_DOWN);
        touchUp = createMotionEvent(MotionEvent.ACTION_UP);
    }

    @Config(reportSdk = VERSION_CODES.JELLY_BEAN_MR2)
    @Test
    public void pluginState_atLeastJellybeanMr2_shouldDefaultToOff_shouldNeverBeEnabled()  {
        subject = new BaseHtmlWebView(new Activity(), adConfiguration);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.OFF);

        subject.enablePlugins(true);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.OFF);
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void pluginState_atLeastIcsButBelowJellybeanMr2_shouldDefaultToOn_shouldAllowToggling() {
        subject = new BaseHtmlWebView(new Activity(), adConfiguration);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.ON);

        subject.enablePlugins(false);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.OFF);

        subject.enablePlugins(true);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.ON);
    }

    @Config(reportSdk = VERSION_CODES.GINGERBREAD_MR1)
    @Test
    public void pluginState_beforeIcs_shouldDefaultToOff_shouldAllowToggling() {
        subject = new BaseHtmlWebView(new Activity(), adConfiguration);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.OFF);

        subject.enablePlugins(true);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.ON);

        subject.enablePlugins(false);
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.OFF);
    }

    @Test
    public void init_shouldSetWebViewScrollability() throws Exception {
        subject.init(false);
        assertThat(shadowOf(subject).getOnTouchListener()).isNotNull();

        subject.init(true);
        assertThat(shadowOf(subject).getOnTouchListener()).isNotNull();
    }

    @Test
    public void loadUrl_shouldAcceptNullParameter() throws Exception {
        subject.loadUrl(null);
        // pass
    }

    @Test
    public void loadUrl_whenUrlIsJavascript_shouldCallSuperLoadUrl() throws Exception {
        String javascriptUrl = "javascript:function() {alert(\"guy\")};";
        subject.loadUrl(javascriptUrl);

        assertThat(shadowOf(subject).getLastLoadedUrl()).isEqualTo(javascriptUrl);
    }

    @Test
    public void loadHtmlResponse_shouldCallLoadDataWithBaseURL() throws Exception {
        String htmlResponse = "some random html response";
        subject.loadHtmlResponse(htmlResponse);

        ShadowWebView.LoadDataWithBaseURL lastLoadData = shadowOf(subject).getLastLoadDataWithBaseURL();
        assertThat(lastLoadData.baseUrl).isEqualTo("http://ads.mopub.com/");
        assertThat(lastLoadData.data).isEqualTo(htmlResponse);
        assertThat(lastLoadData.mimeType).isEqualTo("text/html");
        assertThat(lastLoadData.encoding).isEqualTo("utf-8");
        assertThat(lastLoadData.historyUrl).isNull();
    }

    @Test
    public void sendTouchEvent_withScrollingDisabled_shouldSetUserClicked() throws Exception {
        assertThat(subject.wasClicked()).isFalse();

        subject.initializeOnTouchListener(false);
        View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();

        onTouchListener.onTouch(subject, touchUp);
        assertThat(subject.wasClicked()).isTrue();
    }

    @Test
    public void sendTouchEvent_withScrollingEnabled_shouldSetUserClicked() throws Exception {
        assertThat(subject.wasClicked()).isFalse();

        subject.initializeOnTouchListener(true);
        View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();

        onTouchListener.onTouch(subject, touchUp);
        assertThat(subject.wasClicked()).isTrue();
    }

    @Test
    public void sendTouchEvent_withScrollingDisabled_withLotsOfRandomMotionEvents_shouldEventuallySetUserClicked() throws Exception {
        subject.initializeOnTouchListener(false);
        View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();

        onTouchListener.onTouch(subject, touchDown);
        assertThat(subject.wasClicked()).isFalse();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_CANCEL));
        assertThat(subject.wasClicked()).isFalse();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));
        assertThat(subject.wasClicked()).isFalse();

        onTouchListener.onTouch(subject, touchUp);
        assertThat(subject.wasClicked()).isTrue();

        onTouchListener.onTouch(subject, touchDown);
        assertThat(subject.wasClicked()).isTrue();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_CANCEL));
        assertThat(subject.wasClicked()).isTrue();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));
        assertThat(subject.wasClicked()).isTrue();
    }

    @Test
    public void sendTouchEvent_withScrollingEnabled_withLotsOfRandomMotionEvents_shouldEventuallySetUserClicked() throws Exception {
        subject.initializeOnTouchListener(true);
        View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();

        onTouchListener.onTouch(subject, touchDown);
        assertThat(subject.wasClicked()).isFalse();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_CANCEL));
        assertThat(subject.wasClicked()).isFalse();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));
        assertThat(subject.wasClicked()).isFalse();

        onTouchListener.onTouch(subject, touchUp);
        assertThat(subject.wasClicked()).isTrue();

        onTouchListener.onTouch(subject, touchDown);
        assertThat(subject.wasClicked()).isTrue();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_CANCEL));
        assertThat(subject.wasClicked()).isTrue();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));
        assertThat(subject.wasClicked()).isTrue();
    }

    @Test
    public void onResetClicked_shouldonResetClicked() throws Exception {
        subject.initializeOnTouchListener(false);
        View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();

        onTouchListener.onTouch(subject, touchDown);
        onTouchListener.onTouch(subject, touchUp);
        assertThat(subject.wasClicked()).isTrue();

        subject.onResetUserClick();
        assertThat(subject.wasClicked()).isFalse();
    }

    @Test
    public void onResetClicked_whenTouchStateIsUnset_shouldKeepTouchStateUnset() throws Exception {
        subject.initializeOnTouchListener(false);
        assertThat(subject.wasClicked()).isFalse();

        subject.onResetUserClick();
        assertThat(subject.wasClicked()).isFalse();
    }

    @Test
    public void setWebViewScrollingEnabled_whenScrollableIsTrue_onTouchListenerShouldAlwaysReturnFalse() throws Exception {
        subject.initializeOnTouchListener(true);

        View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();
        // ACTION_DOWN is guaranteed to be run before ACTION_MOVE
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_DOWN));
        boolean shouldConsumeTouch = onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));

        assertThat(shouldConsumeTouch).isFalse();
    }

    @Test
    public void setWebViewScrollingEnabled_whenScrollableIsFalse_whenActionMove_onTouchListenerShouldReturnTrue() throws Exception {
        subject.initializeOnTouchListener(false);

        View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();
        boolean shouldConsumeTouch = onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));

        assertThat(shouldConsumeTouch).isTrue();
    }

    @Test
    public void setWebViewScrollingEnabled_whenScrollableIsFalse_whenMotionEventIsNotActionMove_onTouchListenerShouldReturnFalse() throws Exception {
        subject.initializeOnTouchListener(false);

        View.OnTouchListener onTouchListener = shadowOf(subject).getOnTouchListener();

        boolean shouldConsumeTouch = onTouchListener.onTouch(subject, touchUp);
        assertThat(shouldConsumeTouch).isFalse();

        shouldConsumeTouch = onTouchListener.onTouch(subject, touchDown);
        assertThat(shouldConsumeTouch).isFalse();

        shouldConsumeTouch = onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_CANCEL));
        assertThat(shouldConsumeTouch).isFalse();
    }

    @Test
    public void destroy_shouldRemoveSelfFromParent() throws Exception {
        ViewGroup parentView = mock(ViewGroup.class);
        ShadowWebView shadow = shadowOf(subject);
        shadow.setMyParent(parentView);

        subject.destroy();

        verify(parentView).removeView(eq(subject));
        assertThat(shadow.wasDestroyCalled());
    }
    
    private static MotionEvent createMotionEvent(int action) {
        return MotionEvent.obtain(0, 0, action, 0, 0, 0);
    }
}
