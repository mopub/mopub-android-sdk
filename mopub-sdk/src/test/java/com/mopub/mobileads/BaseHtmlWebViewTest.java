// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowWebView;

import static android.webkit.WebSettings.PluginState;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class BaseHtmlWebViewTest {

    private BaseHtmlWebView subject;
    private MotionEvent touchDown;
    private MotionEvent touchUp;
    private Activity testActivity;

    @Before
    public void setUp() throws Exception {
        testActivity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new BaseHtmlWebView(testActivity);

        touchDown = createMotionEvent(MotionEvent.ACTION_DOWN);
        touchUp = createMotionEvent(MotionEvent.ACTION_UP);
    }

    @Test
    public void pluginState_shouldDefaultToOff_shouldNeverBeEnabled()  {
        assertThat(subject.getSettings().getPluginState()).isEqualTo(PluginState.OFF);
    }

    @Test
    public void init_shouldSetWebViewScrollability() {
        subject.init();
        assertThat(Shadows.shadowOf(subject).getOnTouchListener()).isNotNull();

        subject.init();
        assertThat(Shadows.shadowOf(subject).getOnTouchListener()).isNotNull();
    }

    @Test
    public void loadUrl_shouldAcceptNullParameter() {
        subject.loadUrl(null);
        // pass
    }

    @Test
    public void loadUrl_whenUrlIsJavascript_shouldCallSuperLoadUrl() {
        String javascriptUrl = "javascript:function() {alert(\"guy\")};";
        subject.loadUrl(javascriptUrl);

        assertThat(Shadows.shadowOf(subject).getLastLoadedUrl()).isEqualTo(javascriptUrl);
    }

    @Test
    public void loadHtmlResponse_shouldCallLoadDataWithBaseURL() {
        String htmlResponse = "some random html response";
        subject.loadHtmlResponse(htmlResponse);

        ShadowWebView.LoadDataWithBaseURL lastLoadData = Shadows.shadowOf(subject).getLastLoadDataWithBaseURL();
        assertThat(lastLoadData.baseUrl).isEqualTo("http://" + Constants.HOST + "/");
        assertThat(lastLoadData.data).isEqualTo(htmlResponse);
        assertThat(lastLoadData.mimeType).isEqualTo("text/html");
        assertThat(lastLoadData.encoding).isEqualTo("utf-8");
        assertThat(lastLoadData.historyUrl).isNull();
    }

    @Test
    public void sendTouchEvent_shouldSetUserClicked() {
        assertThat(subject.wasClicked()).isFalse();

        subject.initializeOnTouchListener();
        View.OnTouchListener onTouchListener = Shadows.shadowOf(subject).getOnTouchListener();

        onTouchListener.onTouch(subject, touchDown);
        onTouchListener.onTouch(subject, touchUp);
        assertThat(subject.wasClicked()).isTrue();
    }

    @Test
    public void sendTouchEvent_withTouchDownAndTouchUp_shouldEventuallySetUserClicked() {
        subject.initializeOnTouchListener();
        View.OnTouchListener onTouchListener = Shadows.shadowOf(subject).getOnTouchListener();

        onTouchListener.onTouch(subject, touchDown);
        assertThat(subject.wasClicked()).isFalse();
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));
        onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));
        assertThat(subject.wasClicked()).isFalse();

        onTouchListener.onTouch(subject, touchUp);
        assertThat(subject.wasClicked()).isTrue();
    }

    @Test
    public void onResetClicked_shouldonResetClicked() {
        subject.initializeOnTouchListener();
        View.OnTouchListener onTouchListener = Shadows.shadowOf(subject).getOnTouchListener();

        onTouchListener.onTouch(subject, touchDown);
        onTouchListener.onTouch(subject, touchUp);
        assertThat(subject.wasClicked()).isTrue();

        subject.onResetUserClick();
        assertThat(subject.wasClicked()).isFalse();
    }

    @Test
    public void onResetClicked_whenTouchStateIsUnset_shouldKeepTouchStateUnset() {
        subject.initializeOnTouchListener();
        assertThat(subject.wasClicked()).isFalse();

        subject.onResetUserClick();
        assertThat(subject.wasClicked()).isFalse();
    }

    @Test
    public void setWebView_whenActionMove_onTouchListenerShouldReturnTrue() {
        subject.initializeOnTouchListener();

        View.OnTouchListener onTouchListener = Shadows.shadowOf(subject).getOnTouchListener();
        boolean shouldConsumeTouch = onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_MOVE));

        assertThat(shouldConsumeTouch).isTrue();
    }

    @Test
    public void setWebView_whenMotionEventIsNotActionMove_onTouchListenerShouldReturnFalse() {
        subject.initializeOnTouchListener();

        View.OnTouchListener onTouchListener = Shadows.shadowOf(subject).getOnTouchListener();

        boolean shouldConsumeTouch = onTouchListener.onTouch(subject, touchUp);
        assertThat(shouldConsumeTouch).isFalse();

        shouldConsumeTouch = onTouchListener.onTouch(subject, touchDown);
        assertThat(shouldConsumeTouch).isFalse();

        shouldConsumeTouch = onTouchListener.onTouch(subject, createMotionEvent(MotionEvent.ACTION_CANCEL));
        assertThat(shouldConsumeTouch).isFalse();
    }

    @Test
    public void destroy_shouldRemoveSelfFromParent() {
        ViewGroup parentView = mock(ViewGroup.class);
        ShadowWebView shadow = Shadows.shadowOf(subject);
        shadow.setMyParent(parentView);

        subject.destroy();

        verify(parentView).removeView(eq(subject));
        assertThat(shadow.wasDestroyCalled());
    }
    
    private static MotionEvent createMotionEvent(int action) {
        return MotionEvent.obtain(0, 0, action, 0, 0, 0);
    }
}
