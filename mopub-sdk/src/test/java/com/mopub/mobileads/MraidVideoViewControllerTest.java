package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import com.mopub.common.test.support.SdkTestRunner;

import org.apache.http.HttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowImageButton;
import org.robolectric.shadows.ShadowLocalBroadcastManager;
import org.robolectric.shadows.ShadowVideoView;
import org.robolectric.tester.org.apache.http.RequestMatcher;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mopub.mobileads.BaseVideoPlayerActivity.VIDEO_URL;
import static com.mopub.mobileads.BaseVideoViewController.BaseVideoViewControllerListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.getHtmlInterstitialIntentFilter;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class MraidVideoViewControllerTest {
    private Context context;
    private Bundle bundle;
    private long testBroadcastIdentifier;
    private MraidVideoViewController subject;
    private BaseVideoViewControllerListener baseVideoViewControllerListener;
    private EventForwardingBroadcastReceiver broadcastReceiver;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        bundle = new Bundle();
        testBroadcastIdentifier = 1111;
        broadcastReceiver = mock(EventForwardingBroadcastReceiver.class);
        baseVideoViewControllerListener = mock(BaseVideoViewControllerListener.class);

        bundle.putString(VIDEO_URL, "http://video_url");

        Robolectric.getUiThreadScheduler().pause();
        Robolectric.getBackgroundScheduler().pause();

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, "body"));

        ShadowLocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());
    }

    @After
    public void tearDown() throws Exception {
        Robolectric.getUiThreadScheduler().reset();
        Robolectric.getBackgroundScheduler().reset();
        Robolectric.clearPendingHttpResponses();

        ShadowLocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
    }

    @Test
    public void constructor_shouldSetListenersAndVideoPath() throws Exception {
        initializeSubject();
        ShadowVideoView shadowSubject = shadowOf(subject.getVideoView());

        assertThat(shadowSubject.getOnCompletionListener()).isNotNull();
        assertThat(shadowSubject.getOnErrorListener()).isNotNull();

        assertThat(shadowSubject.getVideoPath()).isEqualTo("http://video_url");
        assertThat(subject.getVideoView().hasFocus()).isTrue();
    }
    
    @Test
    public void onCreate_shouldCreateAndHideCloseButton() throws Exception {
        initializeSubject();
        subject.onCreate();

        ImageButton closeButton = getCloseButton();

        assertThat(closeButton).isNotNull();
        assertThat(getShadowImageButton(closeButton).getOnClickListener()).isNotNull();
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

        ImageButton closeButton = getCloseButton();

        getShadowImageButton(closeButton).getOnClickListener().onClick(null);
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
    public void onCompletionListener_withNullBaseVideoViewControllerListener_shouldNotCallOnFinish() throws Exception {
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

    @Test
    public void onErrorListener_shouldBroadcastInterstitialError() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_FAIL, testBroadcastIdentifier);

        initializeSubject();
        subject.onCreate();

        assertThat(getShadowVideoView().getOnErrorListener().onError(null, 0, 0)).isEqualTo(false);
        Robolectric.getUiThreadScheduler().unPause();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    private void initializeSubject() {
        subject = new MraidVideoViewController(context, bundle, testBroadcastIdentifier, baseVideoViewControllerListener);
    }

    private ShadowVideoView getShadowVideoView() {
        return shadowOf(subject.getVideoView());
    }

    protected ImageButton getCloseButton() {
        return (ImageButton) subject.getLayout().getChildAt(1);
    }

    private ShadowImageButton getShadowImageButton(ImageButton imageButton) {
        return (ShadowImageButton) shadowOf(imageButton);
    }
}
