package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaPlayer;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.VideoView;
import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Drawables;
import com.mopub.mobileads.test.support.GestureUtils;
import com.mopub.mobileads.util.vast.VastCompanionAd;
import com.mopub.mobileads.util.vast.VastVideoConfiguration;
import org.apache.http.HttpRequest;
import org.apache.maven.artifact.ant.shaded.ReflectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocalBroadcastManager;
import org.robolectric.shadows.ShadowVideoView;
import org.robolectric.tester.org.apache.http.RequestMatcher;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.io.*;
import java.util.*;

import static android.media.MediaPlayer.OnPreparedListener;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.mopub.common.MoPubBrowser.DESTINATION_URL_KEY;
import static com.mopub.common.util.test.support.CommonUtils.assertHttpRequestsMade;
import static com.mopub.mobileads.BaseVideoViewController.BaseVideoViewControllerListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.getHtmlInterstitialIntentFilter;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.VastVideoViewController.DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;
import static com.mopub.mobileads.VastVideoViewController.MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON;
import static com.mopub.mobileads.VastVideoViewController.VAST_VIDEO_CONFIGURATION;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class VastVideoViewControllerTest {
    public static final int NETWORK_DELAY = 500;
    private MediaPlayer mediaPlayer;
    private Context context;
    private Bundle bundle;
    private long testBroadcastIdentifier;
    private VastVideoViewController subject;
    private BaseVideoViewControllerListener baseVideoViewControllerListener;
    private EventForwardingBroadcastReceiver broadcastReceiver;
    private int expectedBrowserRequestCode;
    private String expectedUserAgent;

    @Before
    public void setUp() throws Exception {
        mediaPlayer = mock(MediaPlayer.class);
        context = new Activity();
        bundle = new Bundle();
        testBroadcastIdentifier = 1111;
        broadcastReceiver = mock(EventForwardingBroadcastReceiver.class);
        baseVideoViewControllerListener = mock(BaseVideoViewControllerListener.class);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setNetworkMediaFileUrl("video_url");
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addStartTrackers(Arrays.asList("start"));
        vastVideoConfiguration.addFirstQuartileTrackers(Arrays.asList("first"));
        vastVideoConfiguration.addMidpointTrackers(Arrays.asList("mid"));
        vastVideoConfiguration.addThirdQuartileTrackers(Arrays.asList("third"));
        vastVideoConfiguration.addCompleteTrackers(Arrays.asList("complete"));
        vastVideoConfiguration.addImpressionTrackers(Arrays.asList("imp"));
        vastVideoConfiguration.setClickThroughUrl("clickThrough");
        vastVideoConfiguration.addClickTrackers(Arrays.asList("click_1", "click_2"));

        VastCompanionAd vastCompanionAd = new VastCompanionAd(
                300,
                250,
                "companion_image_url",
                "companion_click_destination_url",
                new ArrayList<String>(Arrays.asList("companion_click_tracking_url_1", "companion_click_tracking_url_2"))
        );
        vastVideoConfiguration.setVastCompanionAd(vastCompanionAd);

        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        expectedBrowserRequestCode = 1;

        Robolectric.getUiThreadScheduler().pause();
        Robolectric.getBackgroundScheduler().pause();
        Robolectric.clearPendingHttpResponses();

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, "body"));

        ShadowLocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        expectedUserAgent = new WebView(context).getSettings().getUserAgentString();
    }

    @After
    public void tearDown() throws Exception {
        Robolectric.getUiThreadScheduler().reset();
        Robolectric.getBackgroundScheduler().reset();
        Robolectric.clearPendingHttpResponses();

        ShadowLocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
    }

    @Test
    public void constructor_shouldPingImpressionTrackers() throws Exception {
        // XXX this test needs to be at the top of the constructor tests since it checks for async
        // http requests. If it's below any other constructor tests, there is a chance outstanding
        // async requests will not run until this tests starts, thus polluting the http requests

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addStartTrackers(Arrays.asList("start"));
        vastVideoConfiguration.addImpressionTrackers(Arrays.asList("imp"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(expectedUserAgent, "imp");
    }

    @Test
    public void constructor_shouldAddVastVideoToolbarToLayout() throws Exception {
        initializeSubject();

        VastVideoToolbar vastVideoToolbar = getVastVideoToolbar();
        final ViewGroup.LayoutParams layoutParams = vastVideoToolbar.getLayoutParams();

        assertThat(vastVideoToolbar.getParent()).isEqualTo(subject.getLayout());
        assertThat(vastVideoToolbar.getVisibility()).isEqualTo(View.VISIBLE);

        assertThat(layoutParams.width).isEqualTo(MATCH_PARENT);
        assertThat(layoutParams.height).isEqualTo(Dips.dipsToIntPixels(44, context));
    }

    @Test
    public void constructor_shouldSetVideoListenersAndVideoPath() throws Exception {
        initializeSubject();
        ShadowVideoView videoView = shadowOf(subject.getVideoView());

        assertThat(videoView.getOnCompletionListener()).isNotNull();
        assertThat(videoView.getOnErrorListener()).isNotNull();
        assertThat(videoView.getOnTouchListener()).isNotNull();
        assertThat(videoView.getOnPreparedListener()).isNotNull();

        assertThat(videoView.getVideoPath()).isEqualTo("disk_video_path");
        assertThat(subject.getVideoView().hasFocus()).isTrue();
    }

    @Test
    public void constructor_shouldNotChangeCloseButtonDelay() throws Exception {
        initializeSubject();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void constructor_shouldAddThatchedBackgroundWithGradientToLayout() throws Exception {
        initializeSubject();
        ViewGroup viewGroup = subject.getLayout();
        LayerDrawable layerDrawable = (LayerDrawable) viewGroup.getBackground();
        assertThat(layerDrawable.getDrawable(0)).isEqualTo(Drawables.THATCHED_BACKGROUND.decodeImage(context));
        assertThat(layerDrawable.getDrawable(1)).isEqualTo(
                new GradientDrawable(
                        GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{Color.argb(0, 0, 0, 0), Color.argb(255, 0, 0, 0)})
        );
    }
    
    @Test
    public void constructor_withMissingVastVideoConfiguration_shouldThrowIllegalStateException() throws Exception {
        bundle.clear();
        try {
            initializeSubject();
            fail("VastVideoViewController didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
            // pass
        }
    }

    @Test
    public void constructor_withNullVastVideoConfigurationDiskMediaFileUrl_shouldThrowIllegalStateException() throws Exception {
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, new VastVideoConfiguration());
        try {
            initializeSubject();
            fail("VastVideoViewController didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
            // pass
        }
    }

    @Test
    public void onCreate_shouldBroadcastInterstitialShow() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, testBroadcastIdentifier);

        initializeSubject();

        subject.onCreate();
        Robolectric.getUiThreadScheduler().unPause();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Test
    public void onCreate_withCompanionAd_shouldDownloadCompanionAd() throws Exception {
        initializeSubject();

        final ImageView imageView = subject.getCompanionAdImageView();
        assertThat(imageView.getDrawable()).isNull();

        subject.onCreate();
        Robolectric.getBackgroundScheduler().unPause();
        Robolectric.getUiThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertThat(shadowOf(((BitmapDrawable) imageView.getDrawable()).getBitmap()).getCreatedFromBytes()).isEqualTo("body".getBytes());
    }

    @Test
    public void onComplete_withNullDownloadResponse_shouldNotSetCompanionAdImageBitmap() throws Exception {
        initializeSubject();

        final ImageView imageView = subject.getCompanionAdImageView();
        assertThat(imageView.getDrawable()).isNull();

        subject.onComplete("url", null);

        assertThat(imageView.getDrawable()).isNull();
    }

    @Test
    public void onClick_withCompanionAd_shouldFireCompanionAdClickTrackersAndStartMoPubBrowser() throws Exception {
        initializeSubject();

        final ImageView imageView = subject.getCompanionAdImageView();
        assertThat(imageView.performClick()).isFalse();
        subject.onCreate();

        Robolectric.getBackgroundScheduler().unPause();
        Robolectric.getUiThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertThat(imageView.performClick()).isTrue();
        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(
                expectedUserAgent,
                "companion_image_url",
                "imp",
                "companion_click_tracking_url_1",
                "companion_click_tracking_url_2"
        );

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(baseVideoViewControllerListener).onStartActivityForResult(
                eq(MoPubBrowser.class),
                eq(expectedBrowserRequestCode),
                bundleCaptor.capture()
        );

        assertThat(bundleCaptor.getValue().get(DESTINATION_URL_KEY)).isEqualTo("companion_click_destination_url");
    }

    @Test
    public void onDestroy_shouldBroadcastInterstitialDismiss() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, testBroadcastIdentifier);

        initializeSubject();

        subject.onDestroy();
        Robolectric.getUiThreadScheduler().unPause();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Test
    public void onActivityResult_shouldCallFinish() throws Exception {
        final int expectedResultCode = Activity.RESULT_OK;

        initializeSubject();

        subject.onActivityResult(expectedBrowserRequestCode, expectedResultCode, null);

        verify(baseVideoViewControllerListener).onFinish();
    }

    @Test
    public void onActivityResult_withIncorrectRequestCode_shouldNotCallFinish() throws Exception {
        final int incorrectRequestCode = 1000;
        final int expectedResultCode = Activity.RESULT_OK;

        initializeSubject();

        subject.onActivityResult(incorrectRequestCode, expectedResultCode, null);

        verify(baseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void onActivityResult_withIncorrectResultCode_shouldNotCallFinish() throws Exception {
        final int incorrectResultCode = Activity.RESULT_CANCELED;

        initializeSubject();

        subject.onActivityResult(expectedBrowserRequestCode, incorrectResultCode, null);

        verify(baseVideoViewControllerListener, never()).onFinish();
    }


    @Test
    public void onTouch_withTouchUp_whenVideoLessThan16Seconds_andClickBeforeEnd_shouldDoNothing() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(15999);
        stub(mediaPlayer.getCurrentPosition()).toReturn(15990);

        initializeSubject();
        setMediaPlayer(mediaPlayer);
        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getUiThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Intent nextStartedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLessThan16Seconds_andClickAfterEnd_shouldStartMoPubBrowser() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(15999);
        stub(mediaPlayer.getCurrentPosition()).toReturn(16000);

        initializeSubject();
        subject.onResume();

        setMediaPlayer(mediaPlayer);
        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getUiThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(baseVideoViewControllerListener).onStartActivityForResult(
                eq(MoPubBrowser.class),
                eq(expectedBrowserRequestCode),
                bundleCaptor.capture()
        );

        assertThat(bundleCaptor.getValue().get(DESTINATION_URL_KEY)).isEqualTo("clickThrough");
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLongerThan16Seconds_andClickBefore5Seconds_shouldDoNothing() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(100000);
        stub(mediaPlayer.getCurrentPosition()).toReturn(4999);

        initializeSubject();
        subject.onResume();

        setMediaPlayer(mediaPlayer);
        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getUiThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Intent nextStartedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLongerThan16Seconds_andClickAfter5Seconds_shouldStartMoPubBrowser() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(100000);
        stub(mediaPlayer.getCurrentPosition()).toReturn(5001);

        initializeSubject();
        subject.onResume();

        setMediaPlayer(mediaPlayer);
        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getUiThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(baseVideoViewControllerListener).onStartActivityForResult(
                eq(MoPubBrowser.class),
                eq(expectedBrowserRequestCode),
                bundleCaptor.capture()
        );

        assertThat(bundleCaptor.getValue().get(DESTINATION_URL_KEY)).isEqualTo("clickThrough");
    }

    @Test
    public void onTouch_whenCloseButtonVisible_shouldPingClickThroughTrackers() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addClickTrackers(Arrays.asList("click_1", "click_2"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        subject.setCloseButtonVisible(true);

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));
        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(expectedUserAgent, "click_1", "click_2");
    }

    @Test
    public void onTouch_whenCloseButtonNotVisible_shouldNotPingClickThroughTrackers() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addClickTrackers(Arrays.asList("click_1", "click_2"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        subject.setCloseButtonVisible(false);

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertThat(Robolectric.httpRequestWasMade()).isFalse();
    }

    @Test
    public void onTouch_withNullBaseVideoViewListener_andActionTouchUp_shouldReturnTrueAndNotBlowUp() throws Exception {
        subject = new VastVideoViewController(context, bundle, testBroadcastIdentifier, null);

        boolean result = getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        // pass

        assertThat(result).isTrue();
    }

    @Test
    public void onTouch_withActionTouchDown_shouldConsumeMotionEvent() throws Exception {
        initializeSubject();

        boolean result = getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionDown(0, 0));

        assertThat(result).isTrue();
    }

    @Test
    public void onPrepared_whenDurationIsLessThanMaxVideoDurationForCloseButton_shouldSetShowCloseButtonDelayToDuration() throws Exception {
        initializeSubject();

        stub(mediaPlayer.getDuration()).toReturn(1000);
        setMediaPlayer(mediaPlayer);

        // this callback is typically called when the media filed is loaded and ready to play
        // invoke this manually since we are using a mock url and don't want to wait
        final OnPreparedListener onPreparedListener = getShadowVideoView().getOnPreparedListener();
        onPreparedListener.onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(1000);
    }

    @Test
    public void onPrepared_whenDurationIsGreaterThanMaxVideoDurationForCloseButton_shouldNotSetShowCloseButtonDelay() throws Exception {
        initializeSubject();

        stub(mediaPlayer.getDuration()).toReturn(MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);
        setMediaPlayer(mediaPlayer);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onCompletion_shouldMarkVideoAsFinished() throws Exception {
        initializeSubject();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.isVideoFinishedPlaying()).isTrue();
    }

    @Test
    public void onCompletion_shouldPingCompletionTrackers() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addCompleteTrackers(Arrays.asList("complete_1", "complete_2"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(expectedUserAgent, "complete_1", "complete_2");
    }

    @Test
    public void onCompletion_shouldPreventOnResumeFromStartingVideo() throws Exception {
        initializeSubject();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        subject.onResume();

        assertThat(getShadowVideoView().isPlaying()).isFalse();
    }

    @Test
    public void onCompletion_shouldStopProgressChecker() throws Exception {
        initializeSubject();
        subject.onResume();

        assertThat(subject.getIsVideoProgressShouldBeChecked()).isTrue();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.getIsVideoProgressShouldBeChecked()).isFalse();
    }

    @Test
    public void onCompletion_shouldDisplayCompanionAdIfAvailable() throws Exception {
        initializeSubject();
        subject.onCreate();

        Robolectric.getBackgroundScheduler().unPause();
        Robolectric.getUiThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        final ImageView imageView = subject.getCompanionAdImageView();

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(imageView.getVisibility()).isEqualTo(View.INVISIBLE);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.GONE);
        assertThat(imageView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(shadowOf(((BitmapDrawable) imageView.getDrawable()).getBitmap()).getCreatedFromBytes()).isEqualTo("body".getBytes());
    }

    @Test
    public void onCompletion_shouldShowThatchedBackground() throws Exception {
        initializeSubject();

        final ImageView imageView = subject.getCompanionAdImageView();

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(imageView.getVisibility()).isEqualTo(View.INVISIBLE);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.GONE);
        assertThat(imageView.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onError_shouldFireVideoErrorAndReturnFalse() throws Exception {
        initializeSubject();

        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_FAIL, testBroadcastIdentifier);

        boolean result = getShadowVideoView().getOnErrorListener().onError(null, 0, 0);
        Robolectric.getUiThreadScheduler().unPause();

        assertThat(result).isFalse();
        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Test
    public void onError_shouldStopProgressChecker() throws Exception {
        initializeSubject();
        subject.onResume();

        assertThat(subject.getIsVideoProgressShouldBeChecked()).isTrue();

        getShadowVideoView().getOnErrorListener().onError(null, 0, 0);

        assertThat(subject.getIsVideoProgressShouldBeChecked()).isFalse();
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Test
    public void onError_withVideoFilePermissionErrorBelowJellyBean_shouldRetryPlayingTheVideo() throws Exception {
        File file = new File("disk_video_path");
        file.createNewFile();

        initializeSubject();

        assertThat(getShadowVideoView().getCurrentVideoState()).isEqualTo(-1);

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        getShadowVideoView().getOnErrorListener().onError(new MediaPlayer(), 1, Integer.MIN_VALUE);

        assertThat(getShadowVideoView().isPlaying()).isTrue();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        file.delete();
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Test
    public void retryMediaPlayer_withVideoFilePermissionErrorAndBelowJellyBean_shouldReturnTrue() throws Exception {
        File file = new File("disk_video_path");
        file.createNewFile();

        initializeSubject();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE)).isTrue();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        file.delete();
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Test
    public void retryMediaPlayer_shouldNotRunMoreThanOnce() throws Exception {
        File file = new File("disk_video_path");
        file.createNewFile();

        initializeSubject();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE)).isTrue();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE)).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        file.delete();
    }

    @Config(reportSdk = VERSION_CODES.JELLY_BEAN)
    @Test
    public void retryMediaPlayer_withAndroidVersionAboveJellyBean_shouldReturnFalse() throws Exception {
        File file = new File("disk_video_path");
        file.createNewFile();

        initializeSubject();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE)).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(0);

        file.delete();
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void retryMediaPlayer_withOtherVideoError_shouldReturnFalse() throws Exception {
        File file = new File("disk_video_path");
        file.createNewFile();

        initializeSubject();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 2, Integer.MIN_VALUE)).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(0);

        file.delete();
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void retryMediaPlayer_withExceptionThrown_shouldReturnFalseAndIncrementRetryCount() throws Exception {
        File file = new File("disk_video_path");
        if (file.exists()){
            assertThat(file.delete()).isTrue();
        }

        initializeSubject();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE)).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(1);
    }

    @Test
    public void videoProgressCheckerRunnableRun_shouldFireOffAllProgressTrackers() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(9001);
        stub(mediaPlayer.getCurrentPosition()).toReturn(9002);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFirstQuartileTrackers(Arrays.asList("first"));
        vastVideoConfiguration.addMidpointTrackers(Arrays.asList("second"));
        vastVideoConfiguration.addThirdQuartileTrackers(Arrays.asList("third"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onResume();
        setMediaPlayer(mediaPlayer);

        // this runs the videoProgressChecker
        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(expectedUserAgent, "first", "second", "third");
    }

    @Test
    public void videoProgressCheckerRunnableRun_whenDurationIsInvalid_shouldNotMakeAnyNetworkCalls() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(0);
        stub(mediaPlayer.getCurrentPosition()).toReturn(100);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        setMediaPlayer(mediaPlayer);
        subject.onResume();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().runOneTask();
        // make sure the repeated task hasn't run yet
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        Thread.sleep(NETWORK_DELAY);

        assertThat(Robolectric.httpRequestWasMade()).isFalse();
    }

    @Test
    public void videoProgressCheckerRunnableRun_whenCurrentTimeLessThanOneSecond_shouldNotFireStartTracker() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addStartTrackers(Arrays.asList("start"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        stub(mediaPlayer.getDuration()).toReturn(100000);
        stub(mediaPlayer.getCurrentPosition()).toReturn(999);

        initializeSubject();
        subject.onResume();
        setMediaPlayer(mediaPlayer);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().runOneTask();
        // make sure the repeated task hasn't run yet
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);

        Thread.sleep(NETWORK_DELAY);

        // Since it has not yet been a second, we expect that the start tracker has not been fired
        assertHttpRequestsMade(expectedUserAgent);
        Robolectric.getFakeHttpLayer().clearRequestInfos();

        // run checker another time
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);
        Robolectric.getUiThreadScheduler().runOneTask();

        Thread.sleep(NETWORK_DELAY);

        assertThat(Robolectric.httpRequestWasMade()).isFalse();
    }

    @Test
    public void videoProgressCheckerRunnableRun_whenCurrentTimeGreaterThanOneSecond_shouldFireStartTracker() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addStartTrackers(Arrays.asList("start"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        stub(mediaPlayer.getDuration()).toReturn(100000);
        stub(mediaPlayer.getCurrentPosition()).toReturn(1000);

        initializeSubject();
        subject.onResume();
        setMediaPlayer(mediaPlayer);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();

        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(expectedUserAgent, "start");
        Robolectric.getFakeHttpLayer().clearRequestInfos();

        // run checker another time
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(1);
        Robolectric.getUiThreadScheduler().runOneTask();

        Thread.sleep(NETWORK_DELAY);

        assertThat(Robolectric.httpRequestWasMade()).isFalse();
    }

    @Test
    public void videoProgressCheckerRunnableRun_whenProgressIsPastFirstQuartile_shouldOnlyPingFirstQuartileTrackersOnce() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(100);
        stub(mediaPlayer.getCurrentPosition()).toReturn(26);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFirstQuartileTrackers(Arrays.asList("first"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onResume();
        setMediaPlayer(mediaPlayer);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(expectedUserAgent, "first");
        Robolectric.getFakeHttpLayer().clearRequestInfos();

        // run checker another time
        Robolectric.getUiThreadScheduler().runOneTask();
        Thread.sleep(NETWORK_DELAY);

        assertThat(Robolectric.httpRequestWasMade()).isFalse();
    }

    @Test
    public void videoProgressCheckerRunnableRun_whenProgressIsPastMidQuartile_shouldPingFirstQuartileTrackers_andMidQuartileTrackersBothOnlyOnce() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(100);
        stub(mediaPlayer.getCurrentPosition()).toReturn(51);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFirstQuartileTrackers(Arrays.asList("first"));
        vastVideoConfiguration.addMidpointTrackers(Arrays.asList("second"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onResume();
        setMediaPlayer(mediaPlayer);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(expectedUserAgent, "first", "second");
        Robolectric.getFakeHttpLayer().clearRequestInfos();

        Robolectric.getUiThreadScheduler().runOneTask();
        Thread.sleep(NETWORK_DELAY);

        assertThat(Robolectric.httpRequestWasMade()).isFalse();
    }

    @Test
    public void videoProgressCheckerRunnableRun_whenProgressIsPastThirdQuartile_shouldPingFirstQuartileTrackers_andMidQuartileTrackers_andThirdQuartileTrackersAllOnlyOnce() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(100);
        stub(mediaPlayer.getCurrentPosition()).toReturn(76);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFirstQuartileTrackers(Arrays.asList("first"));
        vastVideoConfiguration.addMidpointTrackers(Arrays.asList("second"));
        vastVideoConfiguration.addThirdQuartileTrackers(Arrays.asList("third"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onResume();
        setMediaPlayer(mediaPlayer);
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        assertHttpRequestsMade(expectedUserAgent, "first", "second", "third");
        Robolectric.getFakeHttpLayer().clearRequestInfos();

        Robolectric.getUiThreadScheduler().runOneTask();
        Thread.sleep(NETWORK_DELAY);

        assertThat(Robolectric.httpRequestWasMade()).isFalse();
    }

    @Test
    public void videoProgressCheckerRunnableRun_asVideoPlays_shouldPingAllThreeTrackersIndividuallyOnce() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(100);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFirstQuartileTrackers(Arrays.asList("first"));
        vastVideoConfiguration.addMidpointTrackers(Arrays.asList("second"));
        vastVideoConfiguration.addThirdQuartileTrackers(Arrays.asList("third"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onResume();
        setMediaPlayer(mediaPlayer);

        // before any trackers are fired
        fastForwardMediaPlayerAndAssertRequestMade(1);

        fastForwardMediaPlayerAndAssertRequestMade(24);

        // after it hits first tracker
        fastForwardMediaPlayerAndAssertRequestMade(26, "first");

        // before mid quartile is hit
        fastForwardMediaPlayerAndAssertRequestMade(49);

        // after it hits mid trackers
        fastForwardMediaPlayerAndAssertRequestMade(51, "second");

        // before third quartile is hit
        fastForwardMediaPlayerAndAssertRequestMade(74);

        // after third quartile is hit
        fastForwardMediaPlayerAndAssertRequestMade(76, "third");

        // way after third quartile is hit
        fastForwardMediaPlayerAndAssertRequestMade(99);
    }

    @Test
    public void videoProgressCheckerRunnableRun_whenCurrentPositionIsGreaterThanShowCloseButtonDelay_shouldShowCloseButton() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(5002);
        stub(mediaPlayer.getCurrentPosition()).toReturn(5001);

        initializeSubject();
        subject.onResume();
        setMediaPlayer(mediaPlayer);

        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        Robolectric.getUiThreadScheduler().unPause();

        assertThat(subject.isShowCloseButtonEventFired()).isTrue();
    }

    @Test
    public void onPause_shouldStopProgressChecker() throws Exception {
        initializeSubject();

        subject.onResume();
        assertThat(subject.getIsVideoProgressShouldBeChecked()).isTrue();

        subject.onPause();
        assertThat(subject.getIsVideoProgressShouldBeChecked()).isFalse();

        subject.onPause();
        assertThat(subject.getIsVideoProgressShouldBeChecked()).isFalse();
    }

    @Test
    public void onResume_shouldStartVideoProgressCheckerOnce() throws Exception {
        initializeSubject();

        subject.onResume();
        assertThat(subject.getIsVideoProgressShouldBeChecked()).isTrue();

        subject.onPause();
        assertThat(subject.getIsVideoProgressShouldBeChecked()).isFalse();

        subject.onResume();
        assertThat(subject.getIsVideoProgressShouldBeChecked()).isTrue();

        subject.onResume();
        assertThat(subject.getIsVideoProgressShouldBeChecked()).isTrue();
    }

    @Test
    public void onResume_shouldSetVideoViewStateToStarted() throws Exception {
        initializeSubject();

        subject.onResume();

        assertThat(getShadowVideoView().getCurrentVideoState()).isEqualTo(ShadowVideoView.START);
        assertThat(getShadowVideoView().getPrevVideoState()).isNotEqualTo(ShadowVideoView.START);
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Test
    public void onResume_shouldResetVideoRetryCountToZero() throws Exception {
        File file = new File("disk_video_path");
        file.createNewFile();

        initializeSubject();

        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE)).isTrue();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        subject.onResume();
        assertThat(subject.getVideoRetries()).isEqualTo(0);

        file.delete();
    }

    @Ignore("pending")
    @Test
    public void onResume_shouldSeekToPrePausedPosition() throws Exception {
        stub(mediaPlayer.getDuration()).toReturn(10000);
        stub(mediaPlayer.getCurrentPosition()).toReturn(7000);

        initializeSubject();
        setMediaPlayer(mediaPlayer);
        final VideoView videoView = spy(subject.getVideoView());

        subject.onPause();

        stub(mediaPlayer.getCurrentPosition()).toReturn(1000);

        subject.onResume();
        verify(videoView).seekTo(eq(7000));
    }

    @Test
    public void backButtonEnabled_shouldDefaultToFalse() throws Exception {
        initializeSubject();

        assertThat(subject.backButtonEnabled()).isFalse();
    }

    @Test
    public void backButtonEnabled_whenCloseButtonIsVisible_shouldReturnTrue() throws Exception {
        initializeSubject();

        subject.setCloseButtonVisible(true);

        assertThat(subject.backButtonEnabled()).isTrue();
    }

    private void initializeSubject() {
        subject = new VastVideoViewController(context, bundle, testBroadcastIdentifier, baseVideoViewControllerListener);
    }

    private void setMediaPlayer(final MediaPlayer mediaPlayer) throws IllegalAccessException {
        final VideoView videoView = subject.getVideoView();
        ReflectionUtils.setVariableValueInObject(videoView, "mMediaPlayer", mediaPlayer);

        int state = (Integer) ReflectionUtils.getValueIncludingSuperclasses("STATE_PLAYING", videoView);

        ReflectionUtils.setVariableValueInObject(videoView, "mCurrentState", state);
    }

    private void fastForwardMediaPlayerAndAssertRequestMade(int time, String... urls) throws Exception {
        stub(mediaPlayer.getCurrentPosition()).toReturn(time);
        Robolectric.getUiThreadScheduler().unPause();
        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        if (urls == null) {
            assertThat(Robolectric.getNextSentHttpRequest()).isNull();
        } else {
            assertHttpRequestsMade(expectedUserAgent, urls);
        }

        Robolectric.getFakeHttpLayer().clearRequestInfos();
    }

    private VastVideoToolbar getVastVideoToolbar() {
        final ViewGroup layout = subject.getLayout();

        for (int i = 0; i < layout.getChildCount(); i++) {
            final View child = layout.getChildAt(i);
            if (child instanceof VastVideoToolbar) {
                return (VastVideoToolbar) child;
            }
        }

        fail("Unable to find VastVideoToolbar in view hierarchy.");
        return null;
    }

    private ShadowVideoView getShadowVideoView() {
        return shadowOf(subject.getVideoView());
    }
}
