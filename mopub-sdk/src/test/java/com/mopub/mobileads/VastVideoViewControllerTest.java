package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.mopub.common.util.DeviceUtils.ForceOrientation;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Drawables;
import com.mopub.mobileads.resource.CloseButtonDrawable;
import com.mopub.mobileads.test.support.GestureUtils;
import com.mopub.mobileads.util.vast.VastCompanionAd;
import com.mopub.mobileads.util.vast.VastVideoConfiguration;
import com.mopub.network.MaxWidthImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.apache.http.HttpRequest;
import org.apache.maven.artifact.ant.shaded.ReflectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocalBroadcastManager;
import org.robolectric.shadows.ShadowVideoView;
import org.robolectric.tester.org.apache.http.RequestMatcher;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.mopub.common.MoPubBrowser.DESTINATION_URL_KEY;
import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static com.mopub.common.util.ResponseHeader.USER_AGENT;
import static com.mopub.mobileads.BaseVideoViewController.BaseVideoViewControllerListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.getHtmlInterstitialIntentFilter;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.VastVideoViewController.DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;
import static com.mopub.mobileads.VastVideoViewController.MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON;
import static com.mopub.mobileads.VastVideoViewController.VAST_VIDEO_CONFIGURATION;
import static com.mopub.volley.toolbox.ImageLoader.ImageListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class VastVideoViewControllerTest {
    public static final int NETWORK_DELAY = 100;

    private static final String COMPANION_IMAGE_URL = "companion_image_url";
    private static final String COMPANION_CLICK_TRACKING_URL_1 = "companion_click_tracking_url_1";
    private static final String COMPANION_CLICK_TRACKING_URL_2 = "companion_click_tracking_url_2";
    private static final String COMPANION_CLICK_DESTINATION_URL = "http://companion_click_destination_url";
    private static final String CLICKTHROUGH_URL = "http://clickthrough_url";

    private Context context;
    private Bundle bundle;
    private long testBroadcastIdentifier;
    private VastVideoViewController subject;
    private BaseVideoViewControllerListener baseVideoViewControllerListener;
    private EventForwardingBroadcastReceiver broadcastReceiver;
    private int expectedBrowserRequestCode;
    private String expectedUserAgent;

    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Mock
    MaxWidthImageLoader mockImageLoader;
    @Mock
    private VideoView mockVideoView;

    private VastVideoViewCountdownRunnable spyCountdownRunnable;
    private VastVideoViewProgressRunnable spyProgressRunnable;
    private VideoView spyVideoView;


    @Before
    public void setUp() throws Exception {
        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        context = Robolectric.buildActivity(Activity.class).create().get();
        bundle = new Bundle();
        testBroadcastIdentifier = 1111;
        broadcastReceiver = mock(EventForwardingBroadcastReceiver.class);
        baseVideoViewControllerListener = mock(BaseVideoViewControllerListener.class);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setNetworkMediaFileUrl("video_url");
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(new VastAbsoluteProgressTracker("start", 2000)));
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f),
                new VastFractionalProgressTracker("mid", 0.5f), new VastFractionalProgressTracker("third", 0.75f)));
        vastVideoConfiguration.addCompleteTrackers(Arrays.asList("complete"));
        vastVideoConfiguration.addCloseTrackers(Arrays.asList("close"));
        vastVideoConfiguration.addSkipTrackers(Arrays.asList("skip"));
        vastVideoConfiguration.addImpressionTrackers(Arrays.asList("imp"));
        vastVideoConfiguration.setClickThroughUrl(CLICKTHROUGH_URL);
        vastVideoConfiguration.addClickTrackers(Arrays.asList("click_1", "click_2"));

        VastCompanionAd vastCompanionAd = new VastCompanionAd(
                300,
                250,
                COMPANION_IMAGE_URL,
                COMPANION_CLICK_DESTINATION_URL,
                new ArrayList<String>(Arrays.asList(COMPANION_CLICK_TRACKING_URL_1, COMPANION_CLICK_TRACKING_URL_2))
        );
        vastVideoConfiguration.setVastCompanionAd(vastCompanionAd);

        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        expectedBrowserRequestCode = 1;

        Robolectric.getUiThreadScheduler().pause();
        Robolectric.getBackgroundScheduler().pause();
        Robolectric.clearPendingHttpResponses();

        // Used to give responses to Vast Download Tasks.
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

        ShadowLocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
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
        assertThat(layerDrawable.getDrawable(0)).isEqualTo(Drawables.THATCHED_BACKGROUND.createDrawable(
                context));
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
    public void constructor_whenCustomCtaTextNotSpecified_shouldUseDefaultCtaText() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        assertThat(getVastVideoToolbar().getLearnMoreWidget().getTextViewText()).isEqualTo("Learn More");
    }

    @Test
    public void constructor_whenCustomCtaTextSpecified_shouldUseCustomCtaText() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setCustomCtaText("custom CTA text");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        assertThat(getVastVideoToolbar().getLearnMoreWidget().getTextViewText()).isEqualTo("custom CTA text");
    }

    @Test
    public void constructor_whenCustomSkipTextNotSpecified_shouldUseDefaultSkipText() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        assertThat(getVastVideoToolbar().getCloseButtonWidget().getTextViewText()).isEqualTo("Close");
    }

    @Test
    public void constructor_whenCustomSkipTextSpecified_shouldUseCustomSkipText() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setCustomSkipText("custom skip text");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        assertThat(getVastVideoToolbar().getCloseButtonWidget().getTextViewText()).isEqualTo("custom skip text");
    }

    @Test
    public void constructor_whenCustomCloseIconNotSpecified_shouldUseDefaultCloseIcon() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        Drawable imageViewDrawable = getVastVideoToolbar().getCloseButtonWidget().getImageViewDrawable();

        // Default close icon is an instance of CloseButtonDrawable
        assertThat(imageViewDrawable).isInstanceOf(CloseButtonDrawable.class);
    }

    @Test
    public void constructor_whenCustomCloseIconSpecified_shouldUseCustomCloseIcon() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setCustomCloseIconUrl("http://ton.twitter.com/exchange-media/images/v4/star_icon_3x_1.png");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        verify(mockImageLoader).get(eq("http://ton.twitter.com/exchange-media/images/v4/star_icon_3x_1.png"), any(ImageListener.class));
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
    public void onCreate_shouldFireImpressionTracker() throws Exception {
        initializeSubject();

        subject.onCreate();
        verify(mockRequestQueue).add(argThat(isUrl("imp")));
    }

    @Test
    public void onCreate_shouldBroadcastInterstitialShow() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, testBroadcastIdentifier);

        initializeSubject();

        Robolectric.getUiThreadScheduler().unPause();
        subject.onCreate();
        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    @Test
    public void onCreate_whenCustomForceOrientationNotSpecified_shouldForceLandscapeOrientation() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onCreate();

        verify(baseVideoViewControllerListener).onSetRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void onCreate_whenCustomForceOrientationIsDeviceOrientation_shouldNotForceLandscapeOrientation() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setCustomForceOrientation(ForceOrientation.DEVICE_ORIENTATION);
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onCreate();

        verify(baseVideoViewControllerListener, never()).onSetRequestedOrientation(anyInt());
    }

    @Test
    public void onCreate_whenCustomForceOrientationIsPortraitOrientation_shouldForcePortraitOrientation() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setCustomForceOrientation(ForceOrientation.FORCE_PORTRAIT);
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onCreate();

        verify(baseVideoViewControllerListener).onSetRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void onCreate_whenCustomForceOrientationIsLandscapeOrientation_shouldForceLandscapeOrientation() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setCustomForceOrientation(ForceOrientation.FORCE_LANDSCAPE);
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.onCreate();

        verify(baseVideoViewControllerListener).onSetRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
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

        // This request is still made by the older http stack.
        assertHttpRequestsMade(expectedUserAgent, COMPANION_IMAGE_URL);

        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CLICK_TRACKING_URL_1)));
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CLICK_TRACKING_URL_2)));

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(baseVideoViewControllerListener).onStartActivityForResult(
                eq(MoPubBrowser.class),
                eq(expectedBrowserRequestCode),
                bundleCaptor.capture()
        );

        assertThat(bundleCaptor.getValue().get(DESTINATION_URL_KEY)).isEqualTo(COMPANION_CLICK_DESTINATION_URL);
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
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(15990, 15999);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getUiThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Intent nextStartedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLessThan16Seconds_andClickAfterEnd_shouldStartMoPubBrowser() throws Exception {
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(15999, 15999);
        subject.onResume();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getUiThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(baseVideoViewControllerListener).onStartActivityForResult(
                eq(MoPubBrowser.class),
                eq(expectedBrowserRequestCode),
                bundleCaptor.capture()
        );

        assertThat(bundleCaptor.getValue().get(DESTINATION_URL_KEY)).isEqualTo(CLICKTHROUGH_URL);
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLongerThan16Seconds_andClickBefore5Seconds_shouldDoNothing() throws Exception {
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(4999, 100000);
        subject.onResume();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getUiThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Intent nextStartedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLongerThan16Seconds_andClickAfter5Seconds_shouldStartMoPubBrowser() throws Exception {
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(5001, 100000);
        subject.onResume();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getUiThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(baseVideoViewControllerListener).onStartActivityForResult(
                eq(MoPubBrowser.class),
                eq(expectedBrowserRequestCode),
                bundleCaptor.capture()
        );

        assertThat(bundleCaptor.getValue().get(DESTINATION_URL_KEY)).isEqualTo(CLICKTHROUGH_URL);
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
        verify(mockRequestQueue).add(argThat(isUrl("click_1")));
        verify(mockRequestQueue).add(argThat(isUrl("click_2")));
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
        spyOnVideoView();
        setVideoViewParams(0, 1000);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(1000);
    }

    @Test
    public void onPrepared_whenDurationIsGreaterThanMaxVideoDurationForCloseButton_shouldNotSetShowCloseButtonDelay() throws Exception {
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onPrepared_whenPercentSkipOffsetSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("25%");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(2500);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenAbsoluteSkipOffsetSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("00:00:03");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3000);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenAbsoluteSkipOffsetWithMillisecondsSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("00:00:03.141");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3141);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsNull_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset(null);
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidAbsoluteFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("123:4:56.7");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidPercentFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("101%");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidFractionalPercentFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("3.14%");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsNegative_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("-00:00:03");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsZero_shouldSetShowCloseButtonDelayToZero() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("00:00:00");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(0);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsLongerThanDurationForShortVideo_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("00:00:11");   // 11s
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);    // 10s: short video

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(10 * 1000);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsLongerThanDurationForLongVideo_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("00:00:21");   // 21s
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 20000);    // 20s: long video

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onCompletion_shouldMarkVideoAsFinished() throws Exception {
        initializeSubject();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.isVideoFinishedPlaying()).isTrue();
    }

    @Test
    public void onCompletion_whenAllTrackersTracked_whenNoPlaybackErrors_shouldPingCompletionTrackersOnlyOnce() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        VastAbsoluteProgressTracker testTracker = new VastAbsoluteProgressTracker("testUrl", 123);
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(testTracker));
        vastVideoConfiguration.addCompleteTrackers(Arrays.asList("complete_1", "complete_2"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        testTracker.setTracked();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);
        verify(mockRequestQueue).add(argThat(isUrl("complete_1")));
        verify(mockRequestQueue).add(argThat(isUrl("complete_2")));

        // Completion trackers should still only be hit once
        getShadowVideoView().getOnCompletionListener().onCompletion(null);
        verify(mockRequestQueue).add(argThat(isUrl("complete_1")));
        verify(mockRequestQueue).add(argThat(isUrl("complete_2")));
    }

    @Test
    public void onCompletion_whenSomeTrackersRemain_shouldNotPingCompletionTrackers() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addCompleteTrackers(Arrays.asList("complete_1", "complete_2"));
        VastAbsoluteProgressTracker testTracker = new VastAbsoluteProgressTracker("testUrl", 123);
        // Never track the testTracker, so completion trackers should not be fired.
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(testTracker));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_1")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_2")));
    }

    @Test
    public void onCompletion_whenPlaybackError_shouldNotPingCompletionTrackers() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addCompleteTrackers(Arrays.asList("complete_1", "complete_2"));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        subject.setVideoError();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_1")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_2")));
    }

    @Test
    public void onCompletion_shouldPreventOnResumeFromStartingVideo() throws Exception {
        initializeSubject();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        subject.onResume();

        assertThat(getShadowVideoView().isPlaying()).isFalse();
    }

    @Test
    public void onCompletion_shouldStopProgressCheckerAndCountdown() throws Exception {
        initializeSubject();
        subject.onResume();

        reset(spyCountdownRunnable, spyCountdownRunnable);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        verify(spyCountdownRunnable).stop();
        verify(spyProgressRunnable).stop();
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
        assertThat(subject.getVideoError()).isTrue();
    }

    @Test
    public void onError_shouldStopProgressChecker() throws Exception {
        initializeSubject();
        subject.onResume();

        verify(spyProgressRunnable).startRepeating(anyLong());
        verify(spyCountdownRunnable).startRepeating(anyLong());
        reset(spyProgressRunnable, spyCountdownRunnable);
        getShadowVideoView().getOnErrorListener().onError(null, 0, 0);

        verify(spyProgressRunnable).stop();
        verify(spyCountdownRunnable).stop();
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
        if (file.exists()) {
            assertThat(file.delete()).isTrue();
        }

        initializeSubject();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE)).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(1);
    }

    @Test
    public void handleClick_shouldMakeRequestsToClickTrackingUrls() throws Exception {
        initializeSubject();
        subject.handleClick(Arrays.asList("clicktracker1", "clicktracker2"), CLICKTHROUGH_URL);

        verify(mockRequestQueue).add(argThat(isUrl("clicktracker1")));
        verify(mockRequestQueue).add(argThat(isUrl("clicktracker2")));
    }

    @Test
    public void handleClick_withNullClickTrackers_shouldNotThrowAnException() throws Exception {
        initializeSubject();
        subject.handleClick(null, CLICKTHROUGH_URL);

        // pass
    }

    @Test
    public void handleClick_withNullClickThroughUrl_shouldNotBroadcastClickOrOpenNewActivity() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, testBroadcastIdentifier);

        initializeSubject();
        subject.handleClick(Arrays.asList("clicktracker"), null);

        Robolectric.getUiThreadScheduler().unPause();
        verify(broadcastReceiver, never()).onReceive(any(Context.class), eq(expectedIntent));
        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void handleClick_withMoPubNativeBrowserClickThroughUrl_shouldOpenExternalBrowser() throws Exception {
        initializeSubject();

        subject.handleClick(Arrays.asList("clicktracker"),
                "mopubnativebrowser://navigate?url=http%3A%2F%2Fwww.mopub.com");

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getDataString()).isEqualTo("http://www.mopub.com");
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void handleClick_withMalformedMoPubNativeBrowserClickThroughUrl_shouldNotOpenANewActivity() throws Exception {
        initializeSubject();

        // url2 is an invalid query parameter
        subject.handleClick(Arrays.asList("clicktracker"),
                "mopubnativebrowser://navigate?url2=http%3A%2F%2Fwww.mopub.com");

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void handleClick_withAboutBlankClickThroughUrl_shouldFailSilently() throws Exception {
        initializeSubject();

        subject.handleClick(Arrays.asList("clicktracker"), "about:blank");

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void videoRunnablesRun_shouldFireOffAllProgressTrackers() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f),
                new VastFractionalProgressTracker("second", 0.5f),
                new VastFractionalProgressTracker("third", 0.75f)));

        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(9002, 9002);
        subject.onResume();

        // this runs the videoProgressChecker and countdown runnable
        Robolectric.getUiThreadScheduler().unPause();

        verify(mockRequestQueue).add(argThat(isUrl("first")));
        verify(mockRequestQueue).add(argThat(isUrl("second")));
        verify(mockRequestQueue).add(argThat(isUrl("third")));
    }

    @Test
    public void getUntriggeredTrackersBefore_endOfVideo_shouldReturnAllTrackers() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f),
                new VastFractionalProgressTracker("second", 0.5f),
                new VastFractionalProgressTracker("third", 0.75f)));
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(new VastAbsoluteProgressTracker("1secs", 1000), new VastAbsoluteProgressTracker("10secs", 10000)));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(11000, 11000);

        final List<VastTracker> untriggeredTrackers = subject.getUntriggeredTrackersBefore(11000, 11000);
        assertThat(untriggeredTrackers).hasSize(5);

        // Sorted absolute trackers, followed by sorted fractional trackers
        final VastTracker tracker0 = untriggeredTrackers.get(0);
        assertThat(tracker0).isExactlyInstanceOf(VastAbsoluteProgressTracker.class);
        assertThat(((VastAbsoluteProgressTracker) tracker0).getTrackingMilliseconds()).isEqualTo(1000);

        final VastTracker tracker1 = untriggeredTrackers.get(1);
        assertThat(tracker1).isExactlyInstanceOf(VastAbsoluteProgressTracker.class);
        assertThat(((VastAbsoluteProgressTracker) tracker1).getTrackingMilliseconds()).isEqualTo(10000);


        final VastTracker tracker2 = untriggeredTrackers.get(2);
        assertThat(tracker2).isExactlyInstanceOf(VastFractionalProgressTracker.class);
        assertThat(((VastFractionalProgressTracker) tracker2).trackingFraction()).isEqualTo(0.25f);

        final VastTracker tracker3 = untriggeredTrackers.get(3);
        assertThat(tracker3).isExactlyInstanceOf(VastFractionalProgressTracker.class);
        assertThat(((VastFractionalProgressTracker) tracker3).trackingFraction()).isEqualTo(0.5f);

        final VastTracker tracker4 = untriggeredTrackers.get(4);
        assertThat(tracker4).isExactlyInstanceOf(VastFractionalProgressTracker.class);
        assertThat(((VastFractionalProgressTracker) tracker4).trackingFraction()).isEqualTo(0.75f);
    }

    @Test
    public void getUntriggeredTrackersBefore_withTriggeredTrackers_shouldNotReturnTriggered() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f),
                new VastFractionalProgressTracker("second", 0.5f),
                new VastFractionalProgressTracker("third", 0.75f)));
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(new VastAbsoluteProgressTracker("5secs", 5000), new VastAbsoluteProgressTracker("10secs", 10000)));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(11000, 11000);

        final List<VastTracker> untriggeredTrackers = subject.getUntriggeredTrackersBefore(11000, 11000);
        assertThat(untriggeredTrackers).hasSize(5);
        untriggeredTrackers.get(0).setTracked();

        final List<VastTracker> secondTrackersList = subject.getUntriggeredTrackersBefore(11000, 11000);
        assertThat(secondTrackersList).hasSize(4);
    }

    @Test
    public void videoRunnablesRun_whenDurationIsInvalid_shouldNotMakeAnyNetworkCalls() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 100);

        subject.onResume();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().runTasks(2);
        // make sure the repeated task hasn't run yet
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenCurrentTimeLessThanTwoSeconds_shouldNotFireStartTracker() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(new VastAbsoluteProgressTracker("start", 2000)));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(1999, 100000);
        subject.onResume();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().runTasks(2);
        // make sure the repeated task hasn't run yet
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        // Since it has not yet been a second, we expect that the start tracker has not been fired
        verifyZeroInteractions(mockRequestQueue);

        // run checker another time
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);
        Robolectric.getUiThreadScheduler().runTasks(2);

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenCurrentTimeGreaterThanTwoSeconds_shouldFireStartTracker() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(new VastAbsoluteProgressTracker("start", 2000)));
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(new VastAbsoluteProgressTracker("later", 3000)));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(2000, 100000);
        subject.onResume();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().unPause();

        verify(mockRequestQueue).add(argThat(isUrl("start")));

        // run checker another time
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);
        Robolectric.getUiThreadScheduler().runTasks(2);

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenProgressIsPastFirstQuartile_shouldOnlyPingFirstQuartileTrackersOnce() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f)));
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("don't call", 0.28f)));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(26, 100);
        subject.onResume();

        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().unPause();

        verify(mockRequestQueue).add(argThat(isUrl("first")));

        // run checker another time
        Robolectric.getUiThreadScheduler().runTasks(2);

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenProgressIsPastMidQuartile_shouldPingFirstQuartileTrackers_andMidQuartileTrackersBothOnlyOnce() throws Exception {

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f)));
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("second", 0.5f)));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(51, 100);

        subject.onResume();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().unPause();

        verify(mockRequestQueue).add(argThat(isUrl("first")));
        verify(mockRequestQueue).add(argThat(isUrl("second")));

        Robolectric.getUiThreadScheduler().runTasks(2);

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenProgressIsPastThirdQuartile_shouldPingFirstQuartileTrackers_andMidQuartileTrackers_andThirdQuartileTrackersAllOnlyOnce() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f)));
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("second", 0.5f)));
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("third", 0.75f)));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(76, 100);

        subject.onResume();
        assertThat(Robolectric.getUiThreadScheduler().enqueuedTaskCount()).isEqualTo(2);

        Robolectric.getUiThreadScheduler().unPause();

        verify(mockRequestQueue).add(argThat(isUrl("first")));
        verify(mockRequestQueue).add(argThat(isUrl("second")));
        verify(mockRequestQueue).add(argThat(isUrl("third")));

        Robolectric.getUiThreadScheduler().runTasks(2);

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_asVideoPlays_shouldPingAllThreeTrackersIndividuallyOnce() throws Exception {
        //stub(mockMediaPlayer.getDuration()).toReturn(100);

        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f)));
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("second", 0.5f)));
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("third", 0.75f)));
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        when(spyVideoView.getDuration()).thenReturn(100);
        subject.onResume();

        // before any trackers are fired
        seekToAndAssertRequestsMade(1);

        seekToAndAssertRequestsMade(24);

        // after it hits first tracker
        seekToAndAssertRequestsMade(26, "first");

        // before mid quartile is hit
        seekToAndAssertRequestsMade(49);

        // after it hits mid trackers
        seekToAndAssertRequestsMade(51, "second");

        // before third quartile is hit
        seekToAndAssertRequestsMade(74);

        // after third quartile is hit
        seekToAndAssertRequestsMade(76, "third");

        // way after third quartile is hit
        seekToAndAssertRequestsMade(99);
    }

    private void seekToAndAssertRequestsMade(int position, String... trackingUrls) {
        when(spyVideoView.getCurrentPosition()).thenReturn(position);

        for (String url : trackingUrls) {
            Robolectric.getUiThreadScheduler().unPause();
            verify(mockRequestQueue).add(argThat(isUrl(url)));
        }
    }

    @Test
    public void videoRunnablesRun_whenCurrentPositionIsGreaterThanShowCloseButtonDelay_shouldShowCloseButton() throws Exception {

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(5001, 5002);
        subject.onResume();

        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        Robolectric.getUiThreadScheduler().unPause();

        assertThat(subject.isShowCloseButtonEventFired()).isTrue();
    }

    @Test
    public void videoRunnablesRun_whenCurrentPositionIsGreaterThanSkipOffset_shouldShowCloseButton() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("25%");    // skipoffset is at 2.5s
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(2501, 10000); // duration is 10s, current position is 1ms after skipoffset
        subject.onResume();


        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(2500);
        assertThat(subject.getHasSkipOffset()).isTrue();

        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        Robolectric.getUiThreadScheduler().unPause();

        assertThat(subject.isShowCloseButtonEventFired()).isTrue();
    }

    @Test
    public void videoRunnablesRun_whenCurrentPositionIsLessThanSkipOffset_shouldNotShowCloseButton() throws Exception {
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfiguration.setSkipOffset("00:00:03");   // skipoffset is at 3s
        bundle.putSerializable(VAST_VIDEO_CONFIGURATION, vastVideoConfiguration);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(2999, 10000); // duration is 10s, current position is 1ms before skipoffset
        subject.onResume();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3000);
        assertThat(subject.getHasSkipOffset()).isTrue();

        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        Robolectric.getUiThreadScheduler().unPause();

        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
    }

    @Test
    public void onPause_shouldStopRunnables() throws Exception {
        initializeSubject();

        subject.onResume();
        verify(spyCountdownRunnable).startRepeating(anyLong());
        verify(spyProgressRunnable).startRepeating(anyLong());

        subject.onPause();
        verify(spyCountdownRunnable).stop();
        verify(spyProgressRunnable).stop();
    }

    @Test
    public void onResume_shouldStartRunnables() throws Exception {
        initializeSubject();

        subject.onPause();
        verify(spyCountdownRunnable).stop();
        verify(spyProgressRunnable).stop();

        subject.onResume();
        verify(spyCountdownRunnable).startRepeating(anyLong());
        verify(spyProgressRunnable).startRepeating(anyLong());
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

    @Test
    public void onResume_shouldSeekToPrePausedPosition() throws Exception {
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(7000, 10000);

        subject.onPause();

        setVideoViewParams(1000, 10000);

        subject.onResume();
        verify(spyVideoView).seekTo(eq(7000));
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

    @Test
    public void onClickCloseButton_whenCloseButtonIsVisible_shouldFireCloseTrackers() throws Exception {
        initializeSubject();

        subject.setCloseButtonVisible(true);

        // We don't have direct access to the CloseButtonWidget's close event, so we manually
        // invoke its onTouchListener's onTouch callback with a fake MotionEvent.ACTION_UP action.
        View.OnTouchListener closeButtonOnTouchListener =
                shadowOf(getVastVideoToolbar().getCloseButtonWidget()).getOnTouchListener();
        closeButtonOnTouchListener.onTouch(null, GestureUtils.createActionUp(0, 0));

        verify(mockRequestQueue).add(argThat(isUrl("close")));
        verify(mockRequestQueue).add(argThat(isUrl("skip")));
    }

    private void initializeSubject() throws IllegalAccessException {
        subject = new VastVideoViewController(context, bundle, testBroadcastIdentifier, baseVideoViewControllerListener);
        spyOnRunnables();
    }

    private void spyOnVideoView() throws IllegalAccessException {
        spyVideoView = spy(subject.getVideoView());
        ReflectionUtils.setVariableValueInObject(subject, "mVideoView", spyVideoView);
    }

    private void spyOnRunnables() throws IllegalAccessException {
        final VastVideoViewProgressRunnable progressCheckerRunnable = (VastVideoViewProgressRunnable) ReflectionUtils.getValueIncludingSuperclasses("mProgressCheckerRunnable", subject);
        spyProgressRunnable = spy(progressCheckerRunnable);

        final VastVideoViewCountdownRunnable countdownRunnable = (VastVideoViewCountdownRunnable) ReflectionUtils.getValueIncludingSuperclasses("mCountdownRunnable", subject);
        spyCountdownRunnable = spy(countdownRunnable);

        ReflectionUtils.setVariableValueInObject(subject, "mProgressCheckerRunnable", spyProgressRunnable);
        ReflectionUtils.setVariableValueInObject(subject, "mCountdownRunnable", spyCountdownRunnable);
    }

    private void setVideoViewParams(int currentPosition, int duration) throws IllegalAccessException {
        when(spyVideoView.getCurrentPosition()).thenReturn(currentPosition);
        when(spyVideoView.getDuration()).thenReturn(duration);
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

    public static void assertHttpRequestsMade(final String userAgent, final String... urls) {
        final int numberOfReceivedHttpRequests = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos().size();
        assertThat(numberOfReceivedHttpRequests).isEqualTo(urls.length);

        for (final String url : urls) {
            assertThat(Robolectric.httpRequestWasMade(url)).isTrue();
        }

        if (userAgent != null) {
            while (true) {
                final HttpRequest httpRequest = Robolectric.getNextSentHttpRequest();
                if (httpRequest == null) {
                    break;
                }

                assertThat(httpRequest.getFirstHeader(USER_AGENT.getKey()).getValue())
                        .isEqualTo(userAgent);
            }
        }
    }
}
