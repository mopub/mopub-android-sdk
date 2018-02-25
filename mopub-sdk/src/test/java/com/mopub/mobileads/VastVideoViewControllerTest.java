package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.VideoView;

import com.mopub.common.ExternalViewabilitySession;
import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.DeviceUtils.ForceOrientation;
import com.mopub.mobileads.resource.CloseButtonDrawable;
import com.mopub.mobileads.test.support.GestureUtils;
import com.mopub.mobileads.test.support.ShadowVastVideoView;
import com.mopub.mobileads.test.support.VastUtils;
import com.mopub.network.MaxWidthImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;

import org.apache.http.HttpRequest;
import org.apache.maven.artifact.ant.shaded.ReflectionUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowRelativeLayout;
import org.robolectric.shadows.ShadowTextView;
import org.robolectric.shadows.ShadowVideoView;
import org.robolectric.shadows.ShadowView;
import org.robolectric.shadows.httpclient.FakeHttp;
import org.robolectric.shadows.httpclient.RequestMatcher;
import org.robolectric.shadows.httpclient.TestHttpResponse;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static com.mopub.mobileads.BaseVideoViewController.BaseVideoViewControllerListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.VastVideoViewController.CURRENT_POSITION;
import static com.mopub.mobileads.VastVideoViewController.DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;
import static com.mopub.mobileads.VastVideoViewController.MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON;
import static com.mopub.mobileads.VastVideoViewController.RESUMED_VAST_CONFIG;
import static com.mopub.mobileads.VastVideoViewController.VAST_VIDEO_CONFIG;
import static com.mopub.mobileads.VastXmlManagerAggregator.ADS_BY_AD_SLOT_ID;
import static com.mopub.mobileads.VastXmlManagerAggregator.SOCIAL_ACTIONS_AD_SLOT_ID;
import static com.mopub.volley.toolbox.ImageLoader.ImageListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
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
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowVastVideoView.class})
public class VastVideoViewControllerTest {
    public static final int NETWORK_DELAY = 100;

    private static final String COMPANION_IMAGE_URL = "companion_image_url";
    private static final String COMPANION_CLICK_TRACKING_URL_1 = "companion_click_tracking_url_1";
    private static final String COMPANION_CLICK_TRACKING_URL_2 = "companion_click_tracking_url_2";
    private static final String COMPANION_CLICK_TRACKING_URL_3 = "companion_click_tracking_url_3";
    private static final String COMPANION_CLICK_DESTINATION_URL = "https://companion_click_destination_url";
    private static final String COMPANION_CREATIVE_VIEW_URL_1 = "companion_creative_view_url_1";
    private static final String COMPANION_CREATIVE_VIEW_URL_2 = "companion_creative_view_url_2";
    private static final String COMPANION_CREATIVE_VIEW_URL_3 = "companion_creative_view_url_3";
    private static final String RESOLVED_CLICKTHROUGH_URL = "https://www.mopub.com/";
    private static final String CLICKTHROUGH_URL = "deeplink+://navigate?" +
            "&primaryUrl=bogus%3A%2F%2Furl" +
            "&fallbackUrl=" + Uri.encode(RESOLVED_CLICKTHROUGH_URL);

    /**
     * A list of macros to include in all trackers
     */
    private static final String MACRO_TAGS = "?errorcode=[ERRORCODE]&asseturi=[ASSETURI]&contentplayhead=[CONTENTPLAYHEAD]";

    private Context context;
    private Bundle bundle;
    private Bundle savedInstanceState;
    private long testBroadcastIdentifier;
    private VastVideoViewController subject;
    private int expectedBrowserRequestCode;
    private String expectedUserAgent;

    @Mock private BaseVideoViewControllerListener baseVideoViewControllerListener;
    @Mock private EventForwardingBroadcastReceiver broadcastReceiver;
    @Mock MoPubRequestQueue mockRequestQueue;
    @Mock MaxWidthImageLoader mockImageLoader;
    @Mock private VastIconConfig mMockVastIconConfig;
    @Mock private MediaMetadataRetriever mockMediaMetadataRetriever;
    @Mock private Bitmap mockBitmap;

    private VastVideoViewCountdownRunnable spyCountdownRunnable;
    private VastVideoViewProgressRunnable spyProgressRunnable;
    private VideoView spyVideoView;

    @Before
    public void setUp() throws Exception {
        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        context = spy(Robolectric.buildActivity(Activity.class).create().get());
        bundle = new Bundle();
        savedInstanceState = new Bundle();
        testBroadcastIdentifier = 1111;

        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setDspCreativeId("dsp_creative_id");
        vastVideoConfig.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker("start" + MACRO_TAGS, 2000)));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("first" + MACRO_TAGS, 0.25f),
                        new VastFractionalProgressTracker("mid" + MACRO_TAGS, 0.5f),
                        new VastFractionalProgressTracker("third" + MACRO_TAGS, 0.75f)));
        vastVideoConfig.addPauseTrackers(
                Arrays.asList(new VastTracker("pause" + MACRO_TAGS, true)));
        vastVideoConfig.addResumeTrackers(
                Arrays.asList(new VastTracker("resume" + MACRO_TAGS, true)));
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete" + MACRO_TAGS));
        vastVideoConfig.addCloseTrackers(
                VastUtils.stringsToVastTrackers("close" + MACRO_TAGS));
        vastVideoConfig.addSkipTrackers(VastUtils.stringsToVastTrackers("skip" + MACRO_TAGS));
        vastVideoConfig.addImpressionTrackers(
                VastUtils.stringsToVastTrackers("imp" + MACRO_TAGS));
        vastVideoConfig.addErrorTrackers(
                Collections.singletonList(new VastTracker("error" + MACRO_TAGS)));
        vastVideoConfig.setClickThroughUrl(CLICKTHROUGH_URL);
        vastVideoConfig.addClickTrackers(
                VastUtils.stringsToVastTrackers("click_1" + MACRO_TAGS, "click_2" + MACRO_TAGS));

        VastCompanionAdConfig landscapeVastCompanionAdConfig = new VastCompanionAdConfig(
                300,
                250,
                new VastResource(COMPANION_IMAGE_URL,
                        VastResource.Type.STATIC_RESOURCE,
                        VastResource.CreativeType.IMAGE, 300, 250),
                COMPANION_CLICK_DESTINATION_URL,
                VastUtils.stringsToVastTrackers(COMPANION_CLICK_TRACKING_URL_1, COMPANION_CLICK_TRACKING_URL_2),
                VastUtils.stringsToVastTrackers(COMPANION_CREATIVE_VIEW_URL_1, COMPANION_CREATIVE_VIEW_URL_2)
        );
        VastCompanionAdConfig portraitVastCompanionAdConfig = new VastCompanionAdConfig(
                250,
                300,
                new VastResource(COMPANION_IMAGE_URL,
                        VastResource.Type.STATIC_RESOURCE,
                        VastResource.CreativeType.IMAGE, 250, 300),
                COMPANION_CLICK_DESTINATION_URL,
                VastUtils.stringsToVastTrackers(COMPANION_CLICK_TRACKING_URL_3),
                VastUtils.stringsToVastTrackers(COMPANION_CREATIVE_VIEW_URL_3)
        );
        vastVideoConfig.setVastCompanionAd(landscapeVastCompanionAdConfig,
                portraitVastCompanionAdConfig);

        when(mMockVastIconConfig.getWidth()).thenReturn(40);
        when(mMockVastIconConfig.getHeight()).thenReturn(40);
        VastResource vastResource = mock(VastResource.class);
        when(vastResource.getType()).thenReturn(VastResource.Type.STATIC_RESOURCE);
        when(vastResource.getResource()).thenReturn("static");
        when(vastResource.getCreativeType()).thenReturn(VastResource.CreativeType.IMAGE);
        when(mMockVastIconConfig.getVastResource()).thenReturn(vastResource);
        vastVideoConfig.setVastIconConfig(mMockVastIconConfig);


        final ArrayList<VastTracker> vastTrackers = new ArrayList<>();
        VastCompanionAdConfig socialActionsCompanionAd =
                new VastCompanionAdConfig(65, 20, vastResource, "", vastTrackers, vastTrackers);
        Map<String, VastCompanionAdConfig> socialActionsCompanionAds =
                new HashMap<String, VastCompanionAdConfig>();
        socialActionsCompanionAds.put(ADS_BY_AD_SLOT_ID, socialActionsCompanionAd);
        socialActionsCompanionAds.put(SOCIAL_ACTIONS_AD_SLOT_ID, socialActionsCompanionAd);
        vastVideoConfig.setSocialActionsCompanionAds(socialActionsCompanionAds);

        when(mockMediaMetadataRetriever.getFrameAtTime(anyLong(), anyInt())).thenReturn(mockBitmap);

        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        expectedBrowserRequestCode = 1;

        Robolectric.getForegroundThreadScheduler().pause();
        Robolectric.getBackgroundThreadScheduler().pause();
        FakeHttp.clearPendingHttpResponses();

        // Used to give responses to Vast Download Tasks.
        FakeHttp.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, "body"));

        ShadowLocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null,
                testBroadcastIdentifier).getIntentFilter());

        expectedUserAgent = new WebView(context).getSettings().getUserAgentString();
    }

    @After
    public void tearDown() throws Exception {
        Robolectric.getForegroundThreadScheduler().reset();
        Robolectric.getBackgroundThreadScheduler().reset();

        ShadowLocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
    }

    @Test
    public void constructor_shouldAddCtaButtonWidgetToLayoutAndSetInvisibleWithOnTouchListeners() throws Exception {
        initializeSubject();

        VastVideoCtaButtonWidget ctaButtonWidget = subject.getCtaButtonWidget();
        assertThat(ctaButtonWidget.getParent()).isEqualTo(subject.getLayout());
        assertThat(ctaButtonWidget.getVisibility()).isEqualTo(View.INVISIBLE);
        ShadowView ctaButtonWidgetShadow = shadowOf(ctaButtonWidget);
        assertThat(ctaButtonWidgetShadow.getOnTouchListener()).isNotNull();
        assertThat(ctaButtonWidgetShadow.getOnTouchListener()).isEqualTo(
                getShadowVideoView().getOnTouchListener());
    }

    @Test
    public void constructor_shouldAddProgressBarWidgetToLayoutAndSetInvisibleWithNoListeners() throws Exception {
        initializeSubject();

        VastVideoProgressBarWidget progressBarWidget = subject.getProgressBarWidget();
        assertThat(progressBarWidget.getParent()).isEqualTo(subject.getLayout());
        assertThat(progressBarWidget.getVisibility()).isEqualTo(View.INVISIBLE);
        ShadowView progressBarWidgetShadow = shadowOf(progressBarWidget);
        assertThat(progressBarWidgetShadow.getOnTouchListener()).isNull();
    }

    @Test
    public void constructor_shouldAddRadialCountdownWidgetToLayoutAndSetInvisibleWithNoListeners() throws Exception {
        initializeSubject();

        VastVideoRadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
        assertThat(radialCountdownWidget.getParent()).isEqualTo(subject.getLayout());
        assertThat(radialCountdownWidget.getVisibility()).isEqualTo(View.INVISIBLE);
        ShadowView radialCountdownWidgetShadow = shadowOf(radialCountdownWidget);
        assertThat(radialCountdownWidgetShadow.getOnTouchListener()).isNull();
    }

    @Test
    public void constructor_shouldAddIconViewToLayoutAndSetInvisibleWithWebViewClickListener() throws Exception {
        initializeSubject();

        View iconView = subject.getIconView();
        assertThat(iconView.getParent()).isEqualTo(subject.getLayout());
        assertThat(iconView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(((VastWebView)iconView).getVastWebViewClickListener()).isNotNull();
    }

    @Test
    public void constructor_withAdsByCompanion_shouldAddAdsByViewToLayout() throws Exception {
        initializeSubject();

        View adsByView = subject.createAdsByView((Activity) context);
        assertThat(adsByView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.getHasSocialActions()).isTrue();
        assertThat(subject.getCtaButtonWidget().getHasSocialActions()).isTrue();
        assertThat(((VastWebView) adsByView).getVastWebViewClickListener()).isNotNull();
    }

    @Test
    public void constructor_withSocialActionsCompanion_shouldAddSocialActionsViewToLayout() throws Exception {
        initializeSubject();

        View socialActionsView = subject.getSocialActionsView();
        assertThat(socialActionsView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getHasSocialActions()).isTrue();
        assertThat(subject.getCtaButtonWidget().getHasSocialActions()).isTrue();
        assertThat(((VastWebView) socialActionsView).getVastWebViewClickListener()).isNotNull();
    }

    @Test
    public void constructor_shouldAddCloseButtonWidgetToLayoutAndSetToGoneWithOnTouchListeners() throws Exception {
        initializeSubject();

        VastVideoCloseButtonWidget closeButtonWidget = subject.getCloseButtonWidget();
        assertThat(closeButtonWidget.getParent()).isEqualTo(subject.getLayout());
        assertThat(closeButtonWidget.getVisibility()).isEqualTo(View.GONE);

        ShadowRelativeLayout closeButtonWidgetShadow = (ShadowRelativeLayout) shadowOf(closeButtonWidget);
        assertThat(closeButtonWidgetShadow.getOnTouchListener()).isNull();

        ShadowView closeButtonImageViewShadow = shadowOf(closeButtonWidget.getImageView());
        assertThat(closeButtonImageViewShadow.getOnTouchListener()).isNotNull();

        ShadowTextView closeButtonTextViewShadow = shadowOf(closeButtonWidget.getTextView());
        assertThat(closeButtonTextViewShadow.getOnTouchListener()).isNotNull();
    }

    @Test
    public void constructor_shouldAddTopGradientStripWidgetToLayoutWithNoListeners() throws Exception {
        initializeSubject();

        VastVideoGradientStripWidget topGradientStripWidget = subject.getTopGradientStripWidget();
        assertThat(topGradientStripWidget.getParent()).isEqualTo(subject.getLayout());

        ShadowView topGradientStripWidgetShadow = shadowOf(topGradientStripWidget);
        assertThat(topGradientStripWidgetShadow.getOnTouchListener()).isNull();
    }

    @Test
    public void constructor_shouldAddBottomGradientStripWidgetToLayoutWithNoListeners() throws Exception {
        initializeSubject();

        VastVideoGradientStripWidget bottomGradientStripWidget = subject.getBottomGradientStripWidget();
        assertThat(bottomGradientStripWidget.getParent()).isEqualTo(subject.getLayout());

        ShadowView bottomGradientStripWidgetShadow = shadowOf(bottomGradientStripWidget);
        assertThat(bottomGradientStripWidgetShadow.getOnTouchListener()).isNull();
    }

    @Test
    public void constructor_shouldAddBlurredLastVideoFrameWidgetToLayoutAndSetInvisibleWithNoListeners() throws Exception {
        initializeSubject();

        ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();
        assertThat(blurredLastVideoFrameImageView.getParent()).isEqualTo(subject.getLayout());
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);
        ShadowView blurredLastVideoFrameImageViewShadow = shadowOf(blurredLastVideoFrameImageView);
        assertThat(blurredLastVideoFrameImageViewShadow.getOnTouchListener()).isNull();
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

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void constructor_shouldAddBlackBackgroundToLayout() throws Exception {
        initializeSubject();
        Drawable background = subject.getLayout().getBackground();
        assertThat(background).isInstanceOf(ColorDrawable.class);
        assertThat(((ColorDrawable) background).getColor()).isEqualTo(Color.BLACK);
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
        bundle.putSerializable(VAST_VIDEO_CONFIG, new VastVideoConfig());
        try {
            initializeSubject();
            fail("VastVideoViewController didn't throw IllegalStateException");
        } catch (IllegalStateException e) {
            // pass
        }
    }

    @Test
    public void constructor_whenCustomCtaTextNotSpecified_shouldUseDefaultCtaText() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        assertThat(subject.getCtaButtonWidget().getCtaText()).isEqualTo(
                "Learn More");
    }

    @Test
    public void constructor_whenCustomCtaTextSpecified_shouldUseCustomCtaText() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setCustomCtaText("custom CTA text");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        assertThat(subject.getCtaButtonWidget().getCtaText()).isEqualTo(
                "custom CTA text");
    }

    @Test
    public void constructor_whenCustomSkipTextNotSpecified_shouldUseDefaultSkipText() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        assertThat(subject.getCloseButtonWidget().getTextView().getText().toString()).isEqualTo(
                "");
    }

    @Test
    public void constructor_whenCustomSkipTextSpecified_shouldUseCustomSkipText() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setCustomSkipText("custom skip text");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        assertThat(subject.getCloseButtonWidget().getTextView().getText().toString()).isEqualTo(
                "custom skip text");
    }

    @Test
    public void constructor_whenCustomCloseIconNotSpecified_shouldUseDefaultCloseIcon() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        Drawable imageViewDrawable = subject.getCloseButtonWidget().getImageView().getDrawable();

        // Default close icon is an instance of CloseButtonDrawable
        assertThat(imageViewDrawable).isInstanceOf(CloseButtonDrawable.class);
    }

    @Test
    public void constructor_whenCustomCloseIconSpecified_shouldUseCustomCloseIcon() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setCustomCloseIconUrl(
                "https://ton.twitter.com/exchange-media/images/v4/star_icon_3x_1.png");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        verify(mockImageLoader).get(
                eq("https://ton.twitter.com/exchange-media/images/v4/star_icon_3x_1.png"),
                any(ImageListener.class));
    }

    @Test
    public void constructor_withVastConfigurationInSavedInstanceState_shouldUseThatVastConfiguration() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setNetworkMediaFileUrl("resumed_network_media_url");
        savedInstanceState.putSerializable(RESUMED_VAST_CONFIG, vastVideoConfig);

        initializeSubject();

        assertThat(subject.getNetworkMediaFileUrl()).isEqualTo("resumed_network_media_url");
    }

    @Test
    public void constructor_withSavedVastConfiguration_shouldUseThatVastConfiguration() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setNetworkMediaFileUrl("resumed_network_media_url");
        savedInstanceState.putSerializable(RESUMED_VAST_CONFIG, vastVideoConfig);

        initializeSubject();

        assertThat(subject.getNetworkMediaFileUrl()).isEqualTo("resumed_network_media_url");
    }

    @Test
    public void constructor_withSavedVastConfiguration_withCurrentPositionSet_shouldResumeVideoFromCurrentPosition() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setNetworkMediaFileUrl("resumed_network_media_url");
        savedInstanceState.putSerializable(RESUMED_VAST_CONFIG, vastVideoConfig);
        savedInstanceState.putInt(CURRENT_POSITION, 123);

        initializeSubject();
        spyOnVideoView();

        subject.onResume();

        verify(spyVideoView).seekTo(eq(123));
    }

    @Test
    public void onCreate_shouldFireImpressionTracker() throws Exception {
        initializeSubject();

        subject.onCreate();
        verify(mockRequestQueue).add(
                argThat(isUrl("imp?errorcode=&asseturi=video_url&contentplayhead=00:00:00.000")));
    }

    @Test
    public void onCreate_shouldBroadcastInterstitialShow() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, testBroadcastIdentifier);

        initializeSubject();

        Robolectric.getForegroundThreadScheduler().unPause();
        subject.onCreate();
        verify(broadcastReceiver).onReceive(any(Context.class),
                argThat(new IntentIsEqual(expectedIntent)));
    }

    @Test
    public void onCreate_whenCustomForceOrientationNotSpecified_shouldForceLandscapeOrientation() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.onCreate();

        verify(baseVideoViewControllerListener).onSetRequestedOrientation(
                SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @Test
    public void onCreate_whenCustomForceOrientationIsDeviceOrientation_shouldNotForceLandscapeOrientation() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setCustomForceOrientation(ForceOrientation.DEVICE_ORIENTATION);
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.onCreate();

        verify(baseVideoViewControllerListener, never()).onSetRequestedOrientation(anyInt());
    }

    @Test
    public void onCreate_whenCustomForceOrientationIsPortraitOrientation_shouldForcePortraitOrientation() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setCustomForceOrientation(ForceOrientation.FORCE_PORTRAIT);
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.onCreate();

        verify(baseVideoViewControllerListener).onSetRequestedOrientation(
                SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void onCreate_whenCustomForceOrientationIsLandscapeOrientation_shouldForceLandscapeOrientation() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setCustomForceOrientation(ForceOrientation.FORCE_LANDSCAPE);
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.onCreate();

        verify(baseVideoViewControllerListener).onSetRequestedOrientation(
                SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @Test
    public void VastWebView_onVastWebViewClick_shouldCallVastCompanionAdHandleClick() throws Exception {
        initializeSubject();

        VastCompanionAdConfig vastCompanionAdConfig = mock(VastCompanionAdConfig.class);
        when(vastCompanionAdConfig.getWidth()).thenReturn(300);
        when(vastCompanionAdConfig.getHeight()).thenReturn(240);
        VastResource vastResource = mock(VastResource.class);
        when(vastResource.getType()).thenReturn(VastResource.Type.STATIC_RESOURCE);
        when(vastResource.getResource()).thenReturn("static");
        when(vastCompanionAdConfig.getVastResource()).thenReturn(vastResource);

        VastWebView view = (VastWebView) subject.createCompanionAdView(context,
                vastCompanionAdConfig, View.INVISIBLE);

        view.getVastWebViewClickListener().onVastWebViewClick();
        verify(vastCompanionAdConfig).handleClick(any(Context.class), eq(1), anyString(), eq("dsp_creative_id"));
    }

    @Test
    public void createCompanionAdView_shouldLayoutAndReturnInvisibleVastIconView() throws Exception {
        initializeSubject();

        VastCompanionAdConfig vastCompanionAdConfig = mock(VastCompanionAdConfig.class);
        when(vastCompanionAdConfig.getWidth()).thenReturn(300);
        when(vastCompanionAdConfig.getHeight()).thenReturn(240);
        VastResource vastResource = mock(VastResource.class);
        when(vastResource.getType()).thenReturn(VastResource.Type.STATIC_RESOURCE);
        when(vastResource.getResource()).thenReturn("static");
        when(vastCompanionAdConfig.getVastResource()).thenReturn(vastResource);

        VastWebView view = (VastWebView) subject.createCompanionAdView(context,
                vastCompanionAdConfig, View.INVISIBLE);

        assertThat(view).isNotNull();
        assertThat(view.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(view.getVastWebViewClickListener()).isNotNull();
        assertThat(subject.getLayout().findViewById(view.getId())).isEqualTo(view);
    }

    @Test
    public void createCompanionAdView_withNullCompanionAd_shouldReturnEmptyView() throws Exception {
        initializeSubject();

        assertThat(subject.createCompanionAdView(context, null, View.INVISIBLE)).isNotNull();
    }

    @Test
    public void onDestroy_shouldBroadcastInterstitialDismiss() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS,
                testBroadcastIdentifier);

        initializeSubject();

        subject.onDestroy();
        Robolectric.getForegroundThreadScheduler().unPause();

        verify(broadcastReceiver).onReceive(any(Context.class),
                argThat(new IntentIsEqual(expectedIntent)));
    }

    @Test
    public void onDestroy_withBlurLastVideoFrameTaskStillRunning_shouldCancelTask() throws Exception {
        initializeSubject();

        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(
                VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.RUNNING);
        subject.getVastVideoView().setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.onDestroy();

        verify(mockBlurLastVideoFrameTask).cancel(true);
    }

    @Test
    public void onDestroy_withBlurLastVideoFrameTaskStillPending_shouldCancelTask() throws Exception {
        initializeSubject();

        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.PENDING);
        subject.getVastVideoView().setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.onDestroy();

        verify(mockBlurLastVideoFrameTask).cancel(true);
    }

    @Test
    public void onDestroy_withBlurLastVideoFrameTaskFinished_shouldNotCancelTask() throws Exception {
        initializeSubject();

        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.FINISHED);
        subject.getVastVideoView().setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.onDestroy();

        verify(mockBlurLastVideoFrameTask, never()).cancel(anyBoolean());
    }

    @Test
    public void onSaveInstanceState_shouldSetCurrentPosition_shouldSetVastConfiguration() throws Exception {
        initializeSubject();

        Bundle bundle = mock(Bundle.class);
        subject.onSaveInstanceState(bundle);

        verify(bundle).putInt(eq(CURRENT_POSITION), anyInt());
        verify(bundle).putSerializable(eq(RESUMED_VAST_CONFIG), any(VastVideoConfig
                .class));
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

        Robolectric.getForegroundThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Intent nextStartedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLessThan16Seconds_andClickAfterEnd_shouldTrackClick_shouldStartMoPubBrowser() throws Exception {
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(15999, 15999);
        subject.onResume();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getForegroundThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Robolectric.getBackgroundThreadScheduler().advanceBy(0);
        final Intent startedActivity = shadowOf((Activity) context).peekNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo(MoPubBrowser.class.getName());
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(
                RESOLVED_CLICKTHROUGH_URL);
        verify((Activity) context).startActivityForResult(any(Intent.class),
                eq(expectedBrowserRequestCode));
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLongerThan16Seconds_andClickBefore5Seconds_shouldDoNothing() throws Exception {
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(4999, 100000);
        subject.onResume();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getForegroundThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Intent nextStartedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLongerThan16Seconds_andClickAfter5Seconds_shouldStartMoPubBrowser() throws Exception {
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(5001, 100000);
        subject.onResume();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        Robolectric.getForegroundThreadScheduler().unPause();

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Robolectric.getBackgroundThreadScheduler().advanceBy(0);
        final Intent startedActivity = shadowOf((Activity) context).peekNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo(MoPubBrowser.class.getName());
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(
                RESOLVED_CLICKTHROUGH_URL);
        verify((Activity) context).startActivityForResult(any(Intent.class),
                eq(expectedBrowserRequestCode));
    }

    @Test
    public void onTouch_whenCloseButtonVisible_shouldPingClickThroughTrackers() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addClickTrackers(
                VastUtils.stringsToVastTrackers("click_1" + MACRO_TAGS, "click_2" + MACRO_TAGS));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        // Because it's almost never exactly 15 seconds
        when(spyVideoView.getDuration()).thenReturn(15142);
        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        subject.setCloseButtonVisible(true);

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));
        verify(mockRequestQueue).add(argThat(isUrl(
                "click_1?errorcode=&asseturi=video_url&contentplayhead=00:00:15.142")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "click_2?errorcode=&asseturi=video_url&contentplayhead=00:00:15.142")));
    }

    @Test
    public void onTouch_whenCloseButtonNotVisible_shouldNotPingClickThroughTrackers() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addClickTrackers(VastUtils.stringsToVastTrackers("click_1",
                "click_2"));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        subject.setCloseButtonVisible(false);

        getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionUp(0, 0));
        assertThat(FakeHttp.httpRequestWasMade()).isFalse();
    }

    @Test
    public void onTouch_withActionTouchDown_shouldConsumeMotionEvent() throws Exception {
        initializeSubject();

        boolean result = getShadowVideoView().getOnTouchListener().onTouch(null, GestureUtils.createActionDown(
                0, 0));

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

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onPrepared_whenPercentSkipOffsetSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("25%");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(2500);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenAbsoluteSkipOffsetSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:03");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3000);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenAbsoluteSkipOffsetWithMillisecondsSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:03.141");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3141);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsNull_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset(null);
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidAbsoluteFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("123:4:56.7");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidPercentFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("101%");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidFractionalPercentFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("3.14%");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsNegative_shouldNotSetShowCloseButtonDelay() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("-00:00:03");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsZero_shouldSetShowCloseButtonDelayToZero() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:00");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(0);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsLongerThanDurationForShortVideo_shouldSetShowCloseButtonDelay() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:11");   // 11s
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);    // 10s: short video

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(10 * 1000);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsLongerThanDurationForLongVideo_shouldSetShowCloseButtonDelay() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:21");   // 21s
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 20000);    // 20s: long video

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(20 * 1000);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenSkipOffset100Percent_shouldSetShowCloseButtonDelayToVideoDuration() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("100%");   // 20000 ms
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 20000);    // 20s: long video

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(20000);
        assertThat(subject.getHasSkipOffset()).isTrue();
    }

    @Test
    public void onPrepared_whenSkipOffsetGreaterThan100Percent_shouldSetShowCloseButtonDelayToDefault() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("101%");   // 20200 ms
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 20000);    // 20s: long video

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getHasSkipOffset()).isFalse();
    }

    @Test
    public void onPrepared_shouldCalibrateAndMakeVisibleRadialCountdownWidget() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:05");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);

        final VastVideoRadialCountdownWidget radialCountdownWidgetSpy = spy(subject.getRadialCountdownWidget());
        subject.setRadialCountdownWidget(radialCountdownWidgetSpy);

        assertThat(subject.isCalibrationDone()).isFalse();
        assertThat(radialCountdownWidgetSpy.getVisibility()).isEqualTo(View.INVISIBLE);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.isCalibrationDone()).isTrue();
        assertThat(radialCountdownWidgetSpy.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownWidgetSpy).calibrateAndMakeVisible(5000);
    }

    @Test
    public void onPrepared_shouldCalibrateAndMakeVisibleProgressBarWidget() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:05");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);
        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 10000);

        final VastVideoProgressBarWidget progressBarWidgetSpy = spy(subject.getProgressBarWidget());
        subject.setProgressBarWidget(progressBarWidgetSpy);

        assertThat(subject.isCalibrationDone()).isFalse();
        assertThat(progressBarWidgetSpy.getVisibility()).isEqualTo(View.INVISIBLE);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.isCalibrationDone()).isTrue();
        assertThat(progressBarWidgetSpy.getVisibility()).isEqualTo(View.VISIBLE);
        verify(progressBarWidgetSpy).calibrateAndMakeVisible(10000, 5000);
    }

    @Test
    public void onPrepared_shouldSetBlurredLastVideoFrame() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();
        assertThat(blurredLastVideoFrameImageView.getDrawable()).isInstanceOf(BitmapDrawable.class);
        assertThat(
                ((BitmapDrawable) blurredLastVideoFrameImageView.getDrawable()).getBitmap()).isNotNull();

        ShadowView imageView = shadowOf(subject.getBlurredLastVideoFrameImageView());
        assertThat(imageView.getOnTouchListener()).isNull();
    }

    @Test
    public void onCompletion_shouldMarkVideoAsFinished() throws Exception {
        initializeSubject();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.isVideoFinishedPlaying()).isTrue();
    }

    @Test
    public void onCompletion_whenAllTrackersTracked_whenNoPlaybackErrors_shouldPingCompletionTrackersOnlyOnce() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        VastAbsoluteProgressTracker testTracker = new VastAbsoluteProgressTracker(
                "testUrl" + MACRO_TAGS, 123);
        vastVideoConfig.addAbsoluteTrackers(Arrays.asList(testTracker));
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete_1" + MACRO_TAGS,
                        "complete_2" + MACRO_TAGS));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        testTracker.setTracked();
        setViewabilityTrackersTracked(vastVideoConfig);
        spyOnVideoView();
        setVideoViewParams(15000, 15000);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);
        verify(mockRequestQueue).add(argThat(isUrl(
                "complete_1?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "complete_2?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));

        // Completion trackers should still only be hit once
        getShadowVideoView().getOnCompletionListener().onCompletion(null);
        verify(mockRequestQueue).add(argThat(isUrl(
                "complete_1?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "complete_2?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
    }

    @Test
    public void onCompletion_whenSomeTrackersRemain_shouldNotPingCompletionTrackers() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete_1", "complete_2"));
        VastAbsoluteProgressTracker testTracker = new VastAbsoluteProgressTracker(
                "testUrl" + MACRO_TAGS, 123);
        // Never track the testTracker, so completion trackers should not be fired.
        vastVideoConfig.addAbsoluteTrackers(Arrays.asList(testTracker));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_1")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_2")));
    }

    @Test
    public void onCompletion_whenPlaybackError_shouldNotPingCompletionTrackers() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete_1", "complete_2"));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.setVideoError();
        spyOnVideoView();
        setVideoViewParams(12345, 15000);

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
    public void onCompletion_whenCompanionAdAvailable_shouldShowCompanionAdAndHideBlurredLastVideoFrame() throws Exception {
        final VastVideoConfig vastVideoConfig =
                (VastVideoConfig) bundle.getSerializable(VAST_VIDEO_CONFIG);
        vastVideoConfig.setSocialActionsCompanionAds(new HashMap<String, VastCompanionAdConfig>());
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);
        initializeSubject();

        final View companionView = subject.getLandscapeCompanionAdView();
        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.getVastVideoView().getBlurLastVideoFrameTask()).isNull();
        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onCompletion_whenCompanionAdAvailable_shouldOnlyShowTopGradientStripWidget() throws Exception {
        initializeSubject();

        final VastVideoGradientStripWidget topGradientStripWidget = subject.getTopGradientStripWidget();
        final VastVideoGradientStripWidget bottomGradientStripWidget = subject.getBottomGradientStripWidget();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(topGradientStripWidget.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(bottomGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onCompletion_whenCompanionAdNotAvailable_shouldHideCompanionAdAndShowBlurredLastVideoFrame() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setVastCompanionAd(null, null);
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        final View companionView = subject.getLandscapeCompanionAdView();
        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.getVastVideoView().getBlurLastVideoFrameTask()).isNotNull();
        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(blurredLastVideoFrameImageView.getDrawable()).isInstanceOf(BitmapDrawable.class);
        assertThat(
                ((BitmapDrawable) blurredLastVideoFrameImageView.getDrawable()).getBitmap()).isNotNull();
    }

    @Test
    public void onCompletion_whenCompanionAdNotAvailable_shouldHideBothGradientStripWidgets() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setVastCompanionAd(null, null);
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        final VastVideoGradientStripWidget topGradientStripWidget = subject.getTopGradientStripWidget();
        final VastVideoGradientStripWidget bottomGradientStripWidget = subject.getBottomGradientStripWidget();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(topGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
        assertThat(bottomGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
    }
    
    @Test
    public void onCompletion_whenCompanionAdNotAvailableAndBlurredLastVideoFrameNotPrepared_shouldShowBlackBackground() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setVastCompanionAd(null, null);
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        final View companionView = subject.getLandscapeCompanionAdView();
        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.getVastVideoView().getBlurLastVideoFrameTask()).isNull();
        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        Drawable background = subject.getLayout().getBackground();
        assertThat(((ColorDrawable) background).getColor()).isEqualTo(Color.BLACK);
    }

    @Test
    public void onCompletion_whenCompanionAdNotAvailableAndBlurredLastVideoFrameNotPrepared_shouldHideBothGradientStripWidgets() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setVastCompanionAd(null, null);
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();

        final VastVideoGradientStripWidget topGradientStripWidget = subject.getTopGradientStripWidget();
        final VastVideoGradientStripWidget bottomGradientStripWidget = subject.getBottomGradientStripWidget();

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(topGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
        assertThat(bottomGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onCompletion_withSocialActions_shouldShowCompanionAdAndShowBlurredLastVideoFrame() throws Exception {
        initializeSubject();

        final View companionView = subject.getLandscapeCompanionAdView();
        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        assertThat(subject.getVastVideoView().getBlurLastVideoFrameTask()).isNotNull();
        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.VISIBLE);
    }

    @Test
    public void onError_shouldFireVideoErrorAndReturnFalse() throws Exception {
        initializeSubject();

        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_FAIL, testBroadcastIdentifier);

        boolean result = getShadowVideoView().getOnErrorListener().onError(null, 0, 0);
        Robolectric.getForegroundThreadScheduler().unPause();

        assertThat(result).isFalse();
        verify(broadcastReceiver).onReceive(any(Context.class),
                argThat(new IntentIsEqual(expectedIntent)));
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

    @Test
    public void onError_shouldFireErrorTrackers() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete_1", "complete_2"));
        vastVideoConfig.addErrorTrackers(
                Collections.singletonList(new VastTracker("error" + MACRO_TAGS)));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.setVideoError();
        spyOnVideoView();
        setVideoViewParams(12345, 15000);

        getShadowVideoView().getOnErrorListener().onError(null, 0, 0);
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_1")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_2")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "error?errorcode=400&asseturi=video_url&contentplayhead=00:00:12.345")));
    }

    @Test
    public void onError_withMultipleCalls_shouldRepeatedlyFireErrorTrackers() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addErrorTrackers(
                Collections.singletonList(new VastTracker("error" + MACRO_TAGS)));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.setVideoError();
        spyOnVideoView();
        setVideoViewParams(12345, 15000);

        for(int i = 0; i < 10; i++) {
            getShadowVideoView().getOnErrorListener().onError(null, 0, 0);
            verify(mockRequestQueue).add(argThat(isUrl(
                    "error?errorcode=400&asseturi=video_url&contentplayhead=00:00:12.345")));
        }
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_shouldFireOffAllProgressTrackers() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("first" + MACRO_TAGS, 0.25f),
                        new VastFractionalProgressTracker("second" + MACRO_TAGS, 0.5f),
                        new VastFractionalProgressTracker("third" + MACRO_TAGS, 0.75f)));

        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(9002, 9002);
        subject.onResume();

        // this runs the videoProgressChecker and countdown runnable
        Robolectric.getForegroundThreadScheduler().unPause();

        verify(mockRequestQueue).add(
                argThat(isUrl("first?errorcode=&asseturi=video_url&contentplayhead=00:00:09.002")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "second?errorcode=&asseturi=video_url&contentplayhead=00:00:09.002")));
        verify(mockRequestQueue).add(
                argThat(isUrl("third?errorcode=&asseturi=video_url&contentplayhead=00:00:09.002")));
    }

    @Test
    public void videoRunnablesRun_whenDurationIsInvalid_shouldNotMakeAnyNetworkCalls() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(0, 100);

        subject.onResume();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);
        Robolectric.getForegroundThreadScheduler().runOneTask();
        Robolectric.getForegroundThreadScheduler().runOneTask();

        // make sure the repeated task hasn't run yet
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);
        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenCurrentTimeLessThanTwoSeconds_shouldNotFireStartTracker() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker("start", 2000)));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(1999, 100000);
        subject.onResume();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        Robolectric.getForegroundThreadScheduler().runOneTask();
        Robolectric.getForegroundThreadScheduler().runOneTask();
        // make sure the repeated task hasn't run yet
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        // Since it has not yet been a second, we expect that the start tracker has not been fired
        verifyZeroInteractions(mockRequestQueue);

        // run checker another time
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);
        Robolectric.getForegroundThreadScheduler().runOneTask();
        Robolectric.getForegroundThreadScheduler().runOneTask();

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenCurrentTimeGreaterThanTwoSeconds_shouldFireStartTracker() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker("start" + MACRO_TAGS, 2000)));
        vastVideoConfig.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker("later" + MACRO_TAGS, 3000)));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(2000, 100000);
        subject.onResume();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        Robolectric.getForegroundThreadScheduler().unPause();

        verify(mockRequestQueue).add(
                argThat(isUrl("start?errorcode=&asseturi=video_url&contentplayhead=00:00:02.000")));

        // run checker another time
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);
        Robolectric.getForegroundThreadScheduler().runOneTask();
        Robolectric.getForegroundThreadScheduler().runOneTask();

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenProgressIsPastFirstQuartile_shouldOnlyPingFirstQuartileTrackersOnce() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("first" + MACRO_TAGS, 0.25f)));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("don't call" + MACRO_TAGS, 0.28f)));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(26, 100);
        subject.onResume();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        Robolectric.getForegroundThreadScheduler().unPause();

        verify(mockRequestQueue).add(
                argThat(isUrl("first?errorcode=&asseturi=video_url&contentplayhead=00:00:00.026")));

        // run checker another time
        Robolectric.getForegroundThreadScheduler().runOneTask();
        Robolectric.getForegroundThreadScheduler().runOneTask();

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenProgressIsPastMidQuartile_shouldPingFirstQuartileTrackers_andMidQuartileTrackersBothOnlyOnce() throws Exception {

        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("first" + MACRO_TAGS, 0.25f)));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("second" + MACRO_TAGS, 0.5f)));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(51, 100);

        subject.onResume();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        Robolectric.getForegroundThreadScheduler().unPause();

        verify(mockRequestQueue).add(
                argThat(isUrl("first?errorcode=&asseturi=video_url&contentplayhead=00:00:00.051")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "second?errorcode=&asseturi=video_url&contentplayhead=00:00:00.051")));

        Robolectric.getForegroundThreadScheduler().runOneTask();
        Robolectric.getForegroundThreadScheduler().runOneTask();

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_whenProgressIsPastThirdQuartile_shouldPingFirstQuartileTrackers_andMidQuartileTrackers_andThirdQuartileTrackersAllOnlyOnce() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("first" + MACRO_TAGS, 0.25f)));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("second" + MACRO_TAGS, 0.5f)));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("third" + MACRO_TAGS, 0.75f)));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(76, 100);

        subject.onResume();
        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(2);

        Robolectric.getForegroundThreadScheduler().unPause();

        verify(mockRequestQueue).add(
                argThat(isUrl("first?errorcode=&asseturi=video_url&contentplayhead=00:00:00.076")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "second?errorcode=&asseturi=video_url&contentplayhead=00:00:00.076")));
        verify(mockRequestQueue).add(
                argThat(isUrl("third?errorcode=&asseturi=video_url&contentplayhead=00:00:00.076")));

        Robolectric.getForegroundThreadScheduler().runOneTask();
        Robolectric.getForegroundThreadScheduler().runOneTask();

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_asVideoPlays_shouldPingAllThreeTrackersIndividuallyOnce() throws Exception {
        //stub(mockMediaPlayer.getDuration()).toReturn(100);

        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker("first" + MACRO_TAGS, 0.25f)));
        vastVideoConfig.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("second" + MACRO_TAGS, 0.5f)));
        vastVideoConfig.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("third" + MACRO_TAGS, 0.75f)));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        when(spyVideoView.getDuration()).thenReturn(100);
        subject.onResume();

        // before any trackers are fired
        seekToAndAssertRequestsMade(1);

        seekToAndAssertRequestsMade(24);

        // after it hits first tracker
        seekToAndAssertRequestsMade(26,
                "first?errorcode=&asseturi=video_url&contentplayhead=00:00:00.026");

        // before mid quartile is hit
        seekToAndAssertRequestsMade(49);

        // after it hits mid trackers
        seekToAndAssertRequestsMade(51,
                "second?errorcode=&asseturi=video_url&contentplayhead=00:00:00.051");

        // before third quartile is hit
        seekToAndAssertRequestsMade(74);

        // after third quartile is hit
        seekToAndAssertRequestsMade(76,
                "third?errorcode=&asseturi=video_url&contentplayhead=00:00:00.076");

        // way after third quartile is hit
        seekToAndAssertRequestsMade(99);
    }

    private void seekToAndAssertRequestsMade(int position, String... trackingUrls) {
        when(spyVideoView.getCurrentPosition()).thenReturn(position);
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        for (String url : trackingUrls) {
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
        Robolectric.getForegroundThreadScheduler().unPause();

        assertThat(subject.isShowCloseButtonEventFired()).isTrue();
    }

    @Test
    public void videoRunnablesRun_whenCurrentPositionIsGreaterThanSkipOffset_shouldShowCloseButton() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("25%");    // skipoffset is at 2.5s
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(2501, 10000); // duration is 10s, current position is 1ms after skipoffset
        subject.onResume();


        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(2500);
        assertThat(subject.getHasSkipOffset()).isTrue();

        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        Robolectric.getForegroundThreadScheduler().unPause();

        assertThat(subject.isShowCloseButtonEventFired()).isTrue();
    }

    @Test
    public void videoRunnablesRun_whenCurrentPositionIsLessThanSkipOffset_shouldNotShowCloseButton() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:03");   // skipoffset is at 3s
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        spyOnVideoView();
        setVideoViewParams(2999, 10000); // duration is 10s, current position is 1ms before skipoffset
        subject.onResume();

        getShadowVideoView().getOnPreparedListener().onPrepared(null);

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3000);
        assertThat(subject.getHasSkipOffset()).isTrue();

        assertThat(subject.isShowCloseButtonEventFired()).isFalse();
        Robolectric.getForegroundThreadScheduler().unPause();

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
    public void onPause_shouldFirePauseTrackers() throws Exception {
        initializeSubject();

        subject.onPause();
        verify(mockRequestQueue).add(
                argThat(isUrl("pause?errorcode=&asseturi=video_url&contentplayhead=00:00:00.000")));
    }

    @Test
    public void onPause_withIsClosingFlagSet_shouldNotFirePauseTrackers() throws Exception {
        initializeSubject();
        subject.setIsClosing(true);

        subject.onPause();
        verifyNoMoreInteractions(mockRequestQueue);
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
    public void onResume_multipleTimes_shouldFirePauseResumeTrackersMultipleTimes() throws Exception {
        initializeSubject();
        spyOnVideoView();

        setVideoViewParams(7000, 10000);
        subject.onPause();

        setVideoViewParams(1000, 10000);
        subject.onResume();

        verify(mockRequestQueue).add(argThat(isUrl
                ("pause?errorcode=&asseturi=video_url&contentplayhead=00:00:07.000")));
        verify(mockRequestQueue).add(
                argThat(isUrl("resume?errorcode=&asseturi=video_url&contentplayhead=00:00:07.000")));

        subject.onPause();
        subject.onResume();

        verify(mockRequestQueue).add(
                argThat(isUrl("pause?errorcode=&asseturi=video_url&contentplayhead=00:00:07.000")));
        verify(mockRequestQueue).add(
                argThat(isUrl("resume?errorcode=&asseturi=video_url&contentplayhead=00:00:07.000")));
    }

    @Test
    public void onConfigurationChanged_withPortraitCompanionAdVisible_withDeviceLandscape_shouldMakeLandscapeCompanionAdVisible() throws Exception {
        initializeSubject();
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject.getPortraitCompanionAdView().setVisibility(View.VISIBLE);

        subject.onConfigurationChanged(null);

        assertThat(subject.getPortraitCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getLandscapeCompanionAdView().getVisibility()).isEqualTo(View.VISIBLE);
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_1)));
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_2)));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onConfigurationChanged_withLandscapeCompanionAdVisible_withDevicePortrait_shouldMakePortraitCompanionAdVisible() throws Exception {
        initializeSubject();
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject.getLandscapeCompanionAdView().setVisibility(View.VISIBLE);

        subject.onConfigurationChanged(null);

        assertThat(subject.getLandscapeCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getPortraitCompanionAdView().getVisibility()).isEqualTo(View.VISIBLE);
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_3)));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onConfigurationChanged_withPortraitCompanionAdVisible_withDevicePortrait_shouldKeepPortraitCompanionAdVisible() throws Exception {
        initializeSubject();
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
        subject.getPortraitCompanionAdView().setVisibility(View.VISIBLE);

        subject.onConfigurationChanged(null);

        assertThat(subject.getPortraitCompanionAdView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.getLandscapeCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_3)));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onConfigurationChanged_withNoCompanionAdVisible_shouldDoNothing() throws Exception {
        initializeSubject();
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(null);

        assertThat(subject.getPortraitCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getLandscapeCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onConfigurationChanged_whenCalledMultipleTimes_shouldOnlyEverFireEachCreativeViewTrackerOnce() throws Exception {
        initializeSubject();
        subject.getPortraitCompanionAdView().setVisibility(View.VISIBLE);

        for(int i = 0; i < 10; i++) {
            context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
            subject.onConfigurationChanged(null);
            context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
            subject.onConfigurationChanged(null);
        }
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_1)));
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_2)));
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_3)));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onConfigurationChanged_withNoCompanionAd_shouldDoNothing() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setNetworkMediaFileUrl("media_url");
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);
        initializeSubject();

        subject.onConfigurationChanged(null);

        verifyNoMoreInteractions(mockRequestQueue);
        assertThat(subject.getLandscapeCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getLandscapeCompanionAdView().getWidth()).isEqualTo(0);
        assertThat(subject.getLandscapeCompanionAdView().getHeight()).isEqualTo(0);
        assertThat(subject.getPortraitCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getPortraitCompanionAdView().getWidth()).isEqualTo(0);
        assertThat(subject.getPortraitCompanionAdView().getHeight()).isEqualTo(0);
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
    public void onClickCloseButtonImageView_whenCloseButtonIsVisible_shouldFireCloseTrackers() throws Exception {
        initializeSubject();
        spyOnVideoView();
        // Because it's almost never exactly 15 seconds
        when(spyVideoView.getDuration()).thenReturn(15094);
        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        subject.setCloseButtonVisible(true);

        // We don't have direct access to the CloseButtonWidget icon's close event, so we manually
        // invoke its onTouchListener's onTouch callback with a fake MotionEvent.ACTION_UP action.
        View.OnTouchListener closeButtonImageViewOnTouchListener =
                shadowOf(subject.getCloseButtonWidget().getImageView()).getOnTouchListener();
        closeButtonImageViewOnTouchListener.onTouch(null, GestureUtils.createActionUp(0, 0));

        verify(mockRequestQueue).add(
                argThat(isUrl("close?errorcode=&asseturi=video_url&contentplayhead=00:00:15.094")));
        verify(mockRequestQueue).add(
                argThat(isUrl("skip?errorcode=&asseturi=video_url&contentplayhead=00:00:15.094")));
    }

    @Test
    public void onClickCloseButtonTextView_whenCloseButtonIsVisible_shouldFireCloseTrackers() throws Exception {
        initializeSubject();
        spyOnVideoView();
        // Because it's almost never exactly 15 seconds
        when(spyVideoView.getDuration()).thenReturn(15203);
        getShadowVideoView().getOnPreparedListener().onPrepared(null);
        getShadowVideoView().getOnCompletionListener().onCompletion(null);

        subject.setCloseButtonVisible(true);

        // We don't have direct access to the CloseButtonWidget text's close event, so we manually
        // invoke its onTouchListener's onTouch callback with a fake MotionEvent.ACTION_UP action.
        View.OnTouchListener closeButtonTextViewOnTouchListener =
                shadowOf(subject.getCloseButtonWidget().getTextView()).getOnTouchListener();
        closeButtonTextViewOnTouchListener.onTouch(null, GestureUtils.createActionUp(0, 0));

        verify(mockRequestQueue).add(
                argThat(isUrl("close?errorcode=&asseturi=video_url&contentplayhead=00:00:15.203")));
        verify(mockRequestQueue).add(
                argThat(isUrl("skip?errorcode=&asseturi=video_url&contentplayhead=00:00:15.203")));
    }

    @Test
    public void createIconView_shouldLayoutAndReturnInvisibleVastIconView() throws Exception {
        initializeSubject();

        VastIconConfig vastIconConfig = mock(VastIconConfig.class);
        when(vastIconConfig.getWidth()).thenReturn(40);
        when(vastIconConfig.getHeight()).thenReturn(40);
        VastResource vastResource = mock(VastResource.class);
        when(vastResource.getType()).thenReturn(VastResource.Type.STATIC_RESOURCE);
        when(vastResource.getResource()).thenReturn("static");
        when(vastIconConfig.getVastResource()).thenReturn(vastResource);

        VastWebView view = (VastWebView) subject.createIconView(context, vastIconConfig, View.INVISIBLE);

        assertThat(view).isNotNull();
        assertThat(view.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(view.getVastWebViewClickListener()).isNotNull();
        assertThat(subject.getLayout().findViewById(view.getId())).isEqualTo(view);
    }

    @Test
    public void createIconView_withNullVastIcon_shouldReturnEmptyView() throws Exception {
        initializeSubject();

        assertThat(subject.createIconView(context, null, View.INVISIBLE)).isNotNull();
    }

    @Test
    public void VastWebView_onVastWebViewClick_shouldCallVastIconHandleClick() throws Exception {
        initializeSubject();

        VastIconConfig vastIconConfig = mock(VastIconConfig.class);
        when(vastIconConfig.getWidth()).thenReturn(40);
        when(vastIconConfig.getHeight()).thenReturn(40);
        VastResource vastResource = mock(VastResource.class);
        when(vastResource.getType()).thenReturn(VastResource.Type.STATIC_RESOURCE);
        when(vastResource.getResource()).thenReturn("static");
        when(vastIconConfig.getVastResource()).thenReturn(vastResource);

        VastWebView view = (VastWebView) subject.createIconView(context, vastIconConfig, View.INVISIBLE);

        view.getVastWebViewClickListener().onVastWebViewClick();
        verify(vastIconConfig).handleClick(any(Context.class), anyString(), eq("dsp_creative_id"));
    }

    @Test
    public void handleIconDisplay_withCurrentPositionGreaterThanOffset_shouldSetIconToVisible_shouldCallHandleImpression() throws Exception {
        initializeSubject();

        when(mMockVastIconConfig.getOffsetMS()).thenReturn(0);
        when(mMockVastIconConfig.getDurationMS()).thenReturn(1);

        subject.handleIconDisplay(0);

        assertThat(subject.getIconView().getVisibility()).isEqualTo(View.VISIBLE);
        verify(mMockVastIconConfig).handleImpression(any(Context.class), eq(0), eq("video_url"));
    }

    @Test
    public void handleIconDisplay_withCurrentPositionLessThanOffset_shouldReturn() throws Exception {
        initializeSubject();

        when(mMockVastIconConfig.getOffsetMS()).thenReturn(1);

        subject.handleIconDisplay(0);

        assertThat(subject.getIconView().getVisibility()).isEqualTo(View.INVISIBLE);
        verify(mMockVastIconConfig, never()).handleImpression(any(Context.class), eq(0),
                eq("video_url"));
    }

    @Test
    public void handleIconDisplay_withCurrentPositionGreaterThanOffsetPlusDuration_shouldSetIconToGone() throws Exception {
        initializeSubject();

        when(mMockVastIconConfig.getOffsetMS()).thenReturn(0);
        when(mMockVastIconConfig.getDurationMS()).thenReturn(1);

        subject.handleIconDisplay(2);

        assertThat(subject.getIconView().getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void makeInteractable_shouldHideCountdownWidgetAndShowCtaAndCloseButtonWidgetsAndShowSocialActions() throws Exception {
        initializeSubject();

        subject.makeVideoInteractable();

        assertThat(subject.getRadialCountdownWidget().getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getCloseButtonWidget().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.getSocialActionsView().getVisibility()).isEqualTo(View.VISIBLE);
    }

    private void initializeSubject() throws IllegalAccessException {
        subject = new VastVideoViewController((Activity) context, bundle, savedInstanceState,
                testBroadcastIdentifier, baseVideoViewControllerListener);
        subject.getVastVideoView().setMediaMetadataRetriever(mockMediaMetadataRetriever);
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

    private ShadowVastVideoView getShadowVideoView() {
        return (ShadowVastVideoView) Shadow.extract(subject.getVastVideoView());
    }

    private void setViewabilityTrackersTracked(VastVideoConfig vastVideoConfig) {
        for (VastFractionalProgressTracker tracker : vastVideoConfig.getFractionalTrackers()) {
            final String content = tracker.getContent();
            try {
                // Only mark trackers that match with viewability's VideoEvent enum
                Enum.valueOf(ExternalViewabilitySession.VideoEvent.class, content);
                tracker.setTracked();
            } catch (IllegalArgumentException e) {
                // pass
            }
        }
    }
}
