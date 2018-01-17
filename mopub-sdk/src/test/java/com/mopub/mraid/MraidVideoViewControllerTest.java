package com.mopub.mraid;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.widget.ImageButton;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.EventForwardingBroadcastReceiver;

import org.apache.http.HttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowVideoView;
import org.robolectric.shadows.httpclient.FakeHttp;
import org.robolectric.shadows.httpclient.RequestMatcher;
import org.robolectric.shadows.httpclient.TestHttpResponse;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mopub.mobileads.BaseVideoPlayerActivity.VIDEO_URL;
import static com.mopub.mobileads.BaseVideoViewController.BaseVideoViewControllerListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MraidVideoViewControllerTest {
    private Context context;
    private Bundle bundle;
    private MraidVideoViewController subject;
    private BaseVideoViewControllerListener baseVideoViewControllerListener;
    private EventForwardingBroadcastReceiver broadcastReceiver;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        bundle = new Bundle();
        baseVideoViewControllerListener = mock(BaseVideoViewControllerListener.class);

        bundle.putString(VIDEO_URL, "https://video_url");

        Robolectric.getForegroundThreadScheduler().pause();
        Robolectric.getBackgroundThreadScheduler().pause();

        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, "body"));

        ShadowLocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null, 0).getIntentFilter());
    }

    @After
    public void tearDown() throws Exception {
        Robolectric.getForegroundThreadScheduler().reset();
        Robolectric.getBackgroundThreadScheduler().reset();
        FakeHttp.clearPendingHttpResponses();

        ShadowLocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
    }

    @Test
    public void constructor_shouldSetListenersAndVideoPath() throws Exception {
        initializeSubject();
        ShadowVideoView shadowSubject = Shadows.shadowOf(subject.getVideoView());

        assertThat(shadowSubject.getOnCompletionListener()).isNotNull();
        assertThat(shadowSubject.getOnErrorListener()).isNotNull();

        assertThat(shadowSubject.getVideoPath()).isEqualTo("https://video_url");
        assertThat(subject.getVideoView().hasFocus()).isTrue();
    }
    
    @Test
    public void onCreate_shouldCreateAndHideCloseButton() throws Exception {
        initializeSubject();
        subject.onCreate();

        ImageButton closeButton = getCloseButton();

        assertThat(closeButton).isNotNull();
        assertThat(Shadows.shadowOf(closeButton).getOnClickListener()).isNotNull();
        assertThat(closeButton.getVisibility()).isEqualTo(GONE);
    }

    @Test
    public void backButtonEnabled_shouldReturnTrue() throws Exception {
        initializeSubject();

        assertThat(subject.backButtonEnabled()).isTrue();
    }

    @Test
    public void closeButton_onClick_shouldCallBaseVideoControllerListenerOnFinish() throws Exception {
        initializeSubject();
        subject.onCreate();

        getCloseButton().performClick();
        verify(baseVideoViewControllerListener).onFinish();
    }

    @Test
    public void onCompletionListener_shouldCallBaseVideoViewControllerListenerOnFinish() throws Exception {
        initializeSubject();
        subject.onCreate();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        verify(baseVideoViewControllerListener).onFinish();
    }

    @Test
    public void onCompletionListener_shouldShowCloseButton() throws Exception {
        initializeSubject();
        subject.onCreate();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(getCloseButton().getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void onErrorListener_shouldReturnFalseAndNotCallBaseVideoControllerListenerOnFinish() throws Exception {
        initializeSubject();
        subject.onCreate();

        assertThat(getShadowVideoView().getOnErrorListener().onError(null, 0, 0)).isEqualTo(false);

        verify(baseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void onErrorListener_shouldShowCloseButton() throws Exception {
        initializeSubject();
        subject.onCreate();

        assertThat(getShadowVideoView().getOnErrorListener().onError(null, 0, 0)).isEqualTo(false);

        assertThat(getCloseButton().getVisibility()).isEqualTo(VISIBLE);
    }

    private void initializeSubject() {
        subject = new MraidVideoViewController(context, bundle, null, baseVideoViewControllerListener);
    }

    private ShadowVideoView getShadowVideoView() {
        return Shadows.shadowOf(subject.getVideoView());
    }

    ImageButton getCloseButton() {
        return (ImageButton) subject.getLayout().getChildAt(1);
    }
}
