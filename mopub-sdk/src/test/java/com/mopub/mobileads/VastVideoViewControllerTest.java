// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

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
import android.graphics.drawable.VectorDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.media2.common.SessionPlayer;
import androidx.media2.player.MediaPlayer;
import androidx.media2.widget.VideoView;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.GestureUtils;
import com.mopub.mobileads.test.support.TestMediaPlayerFactory;
import com.mopub.mobileads.test.support.TestVideoViewFactory;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowRelativeLayout;
import org.robolectric.shadows.ShadowTextView;
import org.robolectric.shadows.ShadowView;
import org.robolectric.shadows.httpclient.FakeHttp;
import org.robolectric.shadows.httpclient.RequestMatcher;
import org.robolectric.shadows.httpclient.TestHttpResponse;

import java.util.Arrays;
import java.util.Collections;

import static com.mopub.common.DataKeys.AD_DATA_KEY;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_DISMISS;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_FAIL;
import static com.mopub.common.IntentActions.ACTION_FULLSCREEN_SHOW;
import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static com.mopub.common.VolleyRequestMatcher.isUrlStartingWith;
import static com.mopub.mobileads.BaseVideoViewController.BaseVideoViewControllerListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.VastVideoViewController.CURRENT_POSITION;
import static com.mopub.mobileads.VastVideoViewController.DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;
import static com.mopub.mobileads.VastVideoViewController.MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON;
import static com.mopub.mobileads.VastVideoViewController.RESUMED_VAST_CONFIG;
import static com.mopub.mobileads.VastVideoViewController.VAST_VIDEO_CONFIG;
import static com.mopub.volley.toolbox.ImageLoader.ImageListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.validateMockitoUsage;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(qualifiers = "w800dp-h480dp")
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
    private static final String RESOLVED_CLICKTHROUGH_URL = "https://www.mopub.com/en";
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

    @Mock
    private BaseVideoViewControllerListener baseVideoViewControllerListener;
    @Mock
    private EventForwardingBroadcastReceiver broadcastReceiver;
    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Mock
    MaxWidthImageLoader mockImageLoader;
    @Mock
    private MediaMetadataRetriever mockMediaMetadataRetriever;
    @Mock
    private Bitmap mockBitmap;

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
                Arrays.asList(new VastAbsoluteProgressTracker.Builder("start" + MACRO_TAGS, 2000).build()));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("first" + MACRO_TAGS, 0.25f).build(),
                        new VastFractionalProgressTracker.Builder("mid" + MACRO_TAGS, 0.5f).build(),
                        new VastFractionalProgressTracker.Builder("third" + MACRO_TAGS, 0.75f).build()));
        vastVideoConfig.addPauseTrackers(
                Arrays.asList(new VastTracker.Builder("pause" + MACRO_TAGS).isRepeatable(true).build()));
        vastVideoConfig.addResumeTrackers(
                Arrays.asList(new VastTracker.Builder("resume" + MACRO_TAGS).isRepeatable(true).build()));
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete" + MACRO_TAGS));
        vastVideoConfig.addCloseTrackers(
                VastUtils.stringsToVastTrackers("close" + MACRO_TAGS));
        vastVideoConfig.addSkipTrackers(VastUtils.stringsToVastTrackers("skip" + MACRO_TAGS));
        vastVideoConfig.addImpressionTrackers(
                VastUtils.stringsToVastTrackers("imp" + MACRO_TAGS));
        vastVideoConfig.addErrorTrackers(
                Collections.singletonList(new VastTracker.Builder("error" + MACRO_TAGS).build()));
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

        final VastResource vastResource = new VastResource("static",
                VastResource.Type.STATIC_RESOURCE,
                VastResource.CreativeType.IMAGE,
                40,
                40);

        final VastIconConfig vastIconConfig = new VastIconConfig(40, 40, 5000, 10000,
                vastResource,
                VastUtils.stringsToVastTrackers("iconClickTrackerOne", "iconClickTrackerTwo"),
                "iconClickThroughUri",
                VastUtils.stringsToVastTrackers("iconViewTrackerOne", "iconViewTrackerTwo"));

        vastVideoConfig.setVastIconConfig(vastIconConfig);

        when(mockMediaMetadataRetriever.getFrameAtTime(anyLong(), anyInt())).thenReturn(mockBitmap);

        final AdData adData = new AdData.Builder()
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, adData);

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

        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(null,
                        testBroadcastIdentifier).getIntentFilter());

        expectedUserAgent = new WebView(context).getSettings().getUserAgentString();
    }

    @After
    public void tearDown() throws Exception {
        Robolectric.getForegroundThreadScheduler().reset();
        Robolectric.getBackgroundThreadScheduler().reset();

        LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);

        validateMockitoUsage(); // makes sure that issues from one test don't carry over to the next
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
                subject.getClickThroughListener());
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

        RadialCountdownWidget radialCountdownWidget = subject.getRadialCountdownWidget();
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
        assertThat(((VastWebView) iconView).getVastWebViewClickListener()).isNotNull();
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
    public void constructor_shouldAddBlurredLastVideoFrameWidgetToLayoutAndSetInvisibleWithListeners() throws Exception {
        initializeSubject();

        ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();
        assertThat(blurredLastVideoFrameImageView.getParent()).isEqualTo(subject.getLayout());
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);
        ShadowView blurredLastVideoFrameImageViewShadow = shadowOf(blurredLastVideoFrameImageView);
        // This has been changed for the new player which allows a click on the blurred frame
        assertThat(blurredLastVideoFrameImageViewShadow.getOnTouchListener()).isNotNull();
    }

    @Test
    public void constructor_shouldSetVideoListenersAndVideoPath() throws Exception {
        initializeSubject();

        assertThat(subject.getPlayerCallback()).isNotNull();
        assertThat(subject.getClickThroughListener()).isNotNull();
        assertThat(subject.getVastVideoConfig().getDiskMediaFileUrl()).isEqualTo("disk_video_path");
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
    public void constructor_withMissingVastVideoConfiguration_shouldThrowIllegalArgumentException() throws Exception {
        bundle.clear();
        try {
            initializeSubject();
            fail("VastVideoViewController didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void constructor_withNullVastVideoConfigurationDiskMediaFileUrl_shouldThrowIllegalArgumentException() throws Exception {
        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(new VastVideoConfig().toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        try {
            initializeSubject();
            fail("VastVideoViewController didn't throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void constructor_whenCustomCtaTextNotSpecified_shouldUseDefaultCtaText() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

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

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        assertThat(subject.getCtaButtonWidget().getCtaText()).isEqualTo(
                "custom CTA text");
    }

    @Test
    public void constructor_whenCustomSkipTextNotSpecified_shouldUseDefaultSkipText() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        assertThat(subject.getCloseButtonWidget().getTextView().getText().toString()).isEqualTo(
                "");
    }

    @Test
    public void constructor_whenCustomSkipTextSpecified_shouldUseCustomSkipText() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setCustomSkipText("custom skip text");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        assertThat(subject.getCloseButtonWidget().getTextView().getText().toString()).isEqualTo(
                "custom skip text");
    }

    @Test
    public void constructor_whenCustomCloseIconNotSpecified_shouldUseDefaultCloseIcon() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        Drawable imageViewDrawable = subject.getCloseButtonWidget().getImageView().getDrawable();

        // Default close icon is an instance of VectorDrawable
        assertThat(imageViewDrawable).isInstanceOf(VectorDrawable.class);
    }

    @Test
    public void constructor_whenCustomCloseIconSpecified_shouldUseCustomCloseIcon() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setCustomCloseIconUrl(
                "https://ton.twitter.com/exchange-media/images/v4/star_icon_3x_1.png");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

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

        subject.onResume();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        verify(mockMediaPlayer).seekTo(eq(123l), eq(MediaPlayer.SEEK_CLOSEST));
    }

    @Test
    public void constructor_shouldCreateLayoutAndReturnInvisibleVastIconView() throws Exception {
        initializeSubject();

        VastWebView view = (VastWebView) ReflectionUtils.getValueIncludingSuperclasses("iconView", subject);

        assertThat(view).isNotNull();
        assertThat(view.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(view.getVastWebViewClickListener()).isNotNull();
        assertThat((VastWebView) subject.getLayout().findViewById(view.getId())).isEqualTo(view);
    }

    @Test
    public void onCreate_shouldFireImpressionTracker() throws Exception {
        initializeSubject();

        subject.onCreate();
        verify(mockRequestQueue).add(
                argThat(isUrl("imp?errorcode=&asseturi=video_url&contentplayhead=00:00:00.000")));
    }

    @Test
    public void onCreate_shouldNotBroadcastInterstitialShow() throws Exception {
        // This broadcast is handled by FullscreenAdController and should not happen here.
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_SHOW, testBroadcastIdentifier);

        initializeSubject();

        Robolectric.getForegroundThreadScheduler().unPause();
        subject.onCreate();
        verify(broadcastReceiver, never()).onReceive(any(Context.class),
                argThat(new IntentIsEqual(expectedIntent)));
    }

    @Test
    public void VastWebView_onVastWebViewClick_shouldCallVastCompanionAdHandleClick() throws Exception {
        initializeSubject();

        final VastCompanionAdConfig spyVastCompanionAdConfig =
                spy(subject.getVastVideoConfig().getVastCompanionAd(Configuration.ORIENTATION_LANDSCAPE));
        final VastWebView vastWebView = subject.createWebView(spyVastCompanionAdConfig);

        vastWebView.getVastWebViewClickListener().onVastWebViewClick();

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<Integer> requestCodeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> webViewClickThroughUrlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> dspCreativeIdCaptor = ArgumentCaptor.forClass(String.class);

        verify(spyVastCompanionAdConfig).handleClick(contextCaptor.capture(),
                requestCodeCaptor.capture(),
                webViewClickThroughUrlCaptor.capture(),
                dspCreativeIdCaptor.capture());

        assertThat(contextCaptor.getValue()).isEqualTo(subject.getContext());
        assertThat(requestCodeCaptor.getValue()).isEqualTo(1);
        assertThat(webViewClickThroughUrlCaptor.getValue()).isNull();
        assertThat(dspCreativeIdCaptor.getValue()).isEqualTo("dsp_creative_id");
    }


    @Test
    public void createCompanionAdView_shouldLayoutAndReturnInvisibleVastIconView() throws Exception {
        initializeSubject();

        final VastVideoConfig vastVideoConfig = subject.getVastVideoConfig();

        final VastWebView view = (VastWebView) subject.createCompanionAdView(vastVideoConfig, Configuration.ORIENTATION_LANDSCAPE, View.INVISIBLE);

        assertThat(view).isNotNull();
        assertThat(view.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(view.getVastWebViewClickListener()).isNotNull();
        assertThat((VastWebView) subject.getLayout().findViewById(view.getId())).isEqualTo(view);
    }

    @Test
    public void createCompanionAdView_withNullCompanionAd_shouldReturnEmptyView() throws Exception {
        initializeSubject();

        final VastVideoConfig vastVideoConfig = subject.getVastVideoConfig();

        assertThat(subject.createCompanionAdView(vastVideoConfig, Configuration.ORIENTATION_LANDSCAPE, View.INVISIBLE))
                .isNotNull();
    }

    @Test
    public void onDestroy_shouldBroadcastInterstitialDismiss() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_DISMISS,
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
        subject.setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.onDestroy();

        verify(mockBlurLastVideoFrameTask).cancel(true);
    }

    @Test
    public void onDestroy_withBlurLastVideoFrameTaskStillPending_shouldCancelTask() throws Exception {
        initializeSubject();

        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.PENDING);
        subject.setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.onDestroy();

        verify(mockBlurLastVideoFrameTask).cancel(true);
    }

    @Test
    public void onDestroy_withBlurLastVideoFrameTaskFinished_shouldNotCancelTask() throws Exception {
        initializeSubject();

        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.FINISHED);
        subject.setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

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
    public void onActivityResult_withIsClosingFalse_shouldNotCallFinish() throws Exception {
        initializeSubject();
        subject.setClosing(false);
        subject.onActivityResult(expectedBrowserRequestCode, Activity.RESULT_OK, null);

        verify(baseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void onActivityResult_withIsClosingTrue_shouldCallFinish() throws Exception {
        initializeSubject();
        subject.setClosing(true);
        subject.onActivityResult(expectedBrowserRequestCode, Activity.RESULT_OK, null);

        verify(baseVideoViewControllerListener).onFinish();
    }

    @Test
    public void onActivityResult_withIncorrectRequestCode_shouldNotCallFinish() throws Exception {
        initializeSubject();
        subject.onActivityResult(1000, Activity.RESULT_OK, null); // 1000 is the incorrect request code

        verify(baseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void onActivityResult_withIncorrectResultCode_shouldNotCallFinish() throws Exception {
        initializeSubject();
        subject.onActivityResult(expectedBrowserRequestCode, Activity.RESULT_CANCELED, null); // Activity.RESULT_CANCELED is an incorrect result code

        verify(baseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLessThan16Seconds_andClickBeforeEnd_shouldDoNothing() throws Exception {
        initializeSubject();
        setVideoViewParams(15990, 15999);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        Robolectric.getForegroundThreadScheduler().unPause();

        subject.getClickThroughListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Intent nextStartedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLessThan16Seconds_andClickAfterEnd_shouldTrackClick_shouldStartMoPubBrowser() throws Exception {
        initializeSubject();
        setVideoViewParams(15999, 15999);
        subject.onResume();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        Robolectric.getForegroundThreadScheduler().unPause();

        subject.getClickThroughListener().onTouch(null, GestureUtils.createActionUp(0, 0));

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
        setVideoViewParams(4999, 100000);
        subject.onResume();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        Robolectric.getForegroundThreadScheduler().unPause();

        subject.getClickThroughListener().onTouch(null, GestureUtils.createActionUp(0, 0));

        Intent nextStartedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(nextStartedActivity).isNull();
    }

    @Test
    public void onTouch_withTouchUp_whenVideoLongerThan16Seconds_andClickAfter5Seconds_shouldStartMoPubBrowser() throws Exception {
        initializeSubject();
        setVideoViewParams(5001, 100000);
        subject.onResume();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        Robolectric.getForegroundThreadScheduler().unPause();

        subject.getClickThroughListener().onTouch(null, GestureUtils.createActionUp(0, 0));

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
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addClickTrackers(
                VastUtils.stringsToVastTrackers("click_1" + MACRO_TAGS, "click_2" + MACRO_TAGS));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(10000, 15142);
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        subject.setShouldAllowClose(true);

        subject.getClickThroughListener().onTouch(null, GestureUtils.createActionUp(0, 0));
        verify(mockRequestQueue).add(argThat(isUrl(
                "click_1?errorcode=&asseturi=video_url&contentplayhead=00:00:15.142")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "click_2?errorcode=&asseturi=video_url&contentplayhead=00:00:15.142")));
    }

    @Test
    public void onTouch_whenCloseButtonNotVisible_shouldNotPingClickThroughTrackers() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addClickTrackers(VastUtils.stringsToVastTrackers("click_1",
                "click_2"));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        subject.setShouldAllowClose(false);

        subject.getClickThroughListener().onTouch(null, GestureUtils.createActionUp(0, 0));
        assertThat(FakeHttp.httpRequestWasMade()).isFalse();
    }

    @Test
    public void onTouch_withActionTouchDown_shouldConsumeMotionEvent() throws Exception {
        initializeSubject();

        final boolean result = subject.getClickThroughListener().onTouch(null, GestureUtils.createActionDown(
                0, 0));

        assertThat(result).isTrue();
    }

    @Test
    public void onPrepared_whenDurationIsLessThanMaxVideoDurationForCloseButton_shouldSetShowCloseButtonDelayToDuration() throws Exception {
        initializeSubject();
        setVideoViewParams(0, 1000);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(1000);
    }

    @Test
    public void onPrepared_whenDurationIsGreaterThanMaxVideoDurationForCloseButton_shouldNotSetShowCloseButtonDelay() throws Exception {
        initializeSubject();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onPrepared_whenPercentSkipOffsetSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("25%");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 10000);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(2500);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();
    }

    @Test
    public void onPrepared_whenAbsoluteSkipOffsetSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:03");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 10000);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3000);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();
    }

    @Test
    public void onPrepared_whenAbsoluteSkipOffsetWithMillisecondsSpecified_shouldSetShowCloseButtonDelayToSkipOffset() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:03.141");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 10000);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3141);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsNull_shouldNotSetShowCloseButtonDelay() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset(null);

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNullOrEmpty();
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidAbsoluteFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("123:4:56.7");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidPercentFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("101%");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onPrepared_whenSkipOffsetHasInvalidFractionalPercentFormat_shouldNotSetShowCloseButtonDelay() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("3.14%");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onPrepared_whenSkipOffsetIsNegative_shouldNotSetShowCloseButtonDelay() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("-00:00:03");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(
                DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onPrepared_whenSkipOffsetIsZero_shouldSetShowCloseButtonDelayToZero() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:00");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON + 1);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(0);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsLongerThanDurationForShortVideo_shouldSetShowCloseButtonDelay() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:11");   // 11s

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 10000);    // 10s: short video

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(10 * 1000);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();
    }

    @Test
    public void onPrepared_whenSkipOffsetIsLongerThanDurationForLongVideo_shouldSetShowCloseButtonDelay() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:21");   // 21s

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 20000);    // 20s: long video

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(20 * 1000);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();
    }

    @Test
    public void onPrepared_whenSkipOffset100Percent_shouldSetShowCloseButtonDelayToVideoDuration() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("100%");   // 20000 ms

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 20000);    // 20s: long video

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(20000);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();
    }

    @Test
    public void onPrepared_whenSkipOffsetGreaterThan100Percent_shouldSetShowCloseButtonDelayToDefault() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("101%");   // 20200 ms

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 20000);    // 20s: long video

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();
        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    @Test
    public void onPrepared_shouldCalibrateAndMakeVisibleRadialCountdownWidget() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:05");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 10000);

        final RadialCountdownWidget radialCountdownWidgetSpy = spy(subject.getRadialCountdownWidget());
        subject.setRadialCountdownWidget(radialCountdownWidgetSpy);

        assertThat(subject.isCalibrationDone()).isFalse();
        assertThat(radialCountdownWidgetSpy.getVisibility()).isEqualTo(View.INVISIBLE);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.isCalibrationDone()).isTrue();
        assertThat(radialCountdownWidgetSpy.getVisibility()).isEqualTo(View.VISIBLE);
        verify(radialCountdownWidgetSpy).calibrateAndMakeVisible(5000);
    }

    @Test
    public void onPrepared_shouldCalibrateAndMakeVisibleProgressBarWidget() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:05");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(0, 10000);

        final VastVideoProgressBarWidget progressBarWidgetSpy = spy(subject.getProgressBarWidget());
        subject.setProgressBarWidget(progressBarWidgetSpy);

        assertThat(subject.isCalibrationDone()).isFalse();
        assertThat(progressBarWidgetSpy.getVisibility()).isEqualTo(View.INVISIBLE);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.isCalibrationDone()).isTrue();
        assertThat(progressBarWidgetSpy.getVisibility()).isEqualTo(View.VISIBLE);
        verify(progressBarWidgetSpy).calibrateAndMakeVisible(10000, 5000);
    }

    @Test
    public void onPrepared_shouldSetBlurredLastVideoFrame() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();
        assertThat(blurredLastVideoFrameImageView.getDrawable()).isInstanceOf(BitmapDrawable.class);
        assertThat(
                ((BitmapDrawable) blurredLastVideoFrameImageView.getDrawable()).getBitmap()).isNotNull();

        assertThat(subject.getBlurredLastVideoFrameImageView().getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onCompletion_shouldMarkVideoAsFinished() throws Exception {
        initializeSubject();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        assertThat(subject.isComplete()).isTrue();
    }

    @Test
    public void onCompletion_whenAllTrackersTracked_whenNoPlaybackErrors_shouldPingCompletionTrackersOnlyOnce() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        VastAbsoluteProgressTracker testTracker = new VastAbsoluteProgressTracker.Builder(
                "testUrl" + MACRO_TAGS, 123).build();
        vastVideoConfig.addAbsoluteTrackers(Arrays.asList(testTracker));
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete_1" + MACRO_TAGS,
                        "complete_2" + MACRO_TAGS));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        testTracker.setTracked();
        setFractionalProgressTrackersTracked(subject.getVastVideoConfig());
        setAbsoluteProgressTrackersTracked(subject.getVastVideoConfig());
        setVideoViewParams(15000, 15000);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);
        verify(mockRequestQueue).add(argThat(isUrl(
                "complete_1?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "complete_2?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));

        // Completion trackers should still only be hit once
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);
        verify(mockRequestQueue).add(argThat(isUrl(
                "complete_1?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verify(mockRequestQueue).add(argThat(isUrl(
                "complete_2?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
    }

    @Test
    public void onCompletion_whenSomeTrackersRemain_shouldNotPingCompletionTrackers() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete_1", "complete_2"));
        VastAbsoluteProgressTracker testTracker = new VastAbsoluteProgressTracker.Builder(
                "testUrl" + MACRO_TAGS, 123).build();
        // Never track the testTracker, so completion trackers should not be fired.
        vastVideoConfig.addAbsoluteTrackers(Arrays.asList(testTracker));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_1")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_2")));
    }

    @Test
    public void onCompletion_whenPlaybackError_shouldNotPingCompletionTrackers() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addCompleteTrackers(
                VastUtils.stringsToVastTrackers("complete_1", "complete_2"));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        subject.setVideoError(true);
        setVideoViewParams(12345, 15000);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_1")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("complete_2")));
    }

    @Test
    public void onCompletion_shouldPreventOnResumeFromStartingVideo() throws Exception {
        initializeSubject();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        subject.onResume();

        assertThat(mockMediaPlayer.getPlayerState()).isNotEqualTo(SessionPlayer.PLAYER_STATE_PLAYING);
    }

    @Test
    public void onCompletion_shouldStopProgressCheckerAndCountdown() throws Exception {
        initializeSubject();
        subject.onResume();

        reset(spyCountdownRunnable, spyCountdownRunnable);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        verify(spyCountdownRunnable).stop();
        verify(spyProgressRunnable).stop();
    }

    @Test
    public void onCompletion_whenCompanionAdAvailable_shouldShowCompanionAdAndHideBlurredLastVideoFrame() throws Exception {
        final VideoView mockVideoView = TestVideoViewFactory.Companion.getMockVideoView();
        reset(mockVideoView);

        initializeSubject();

        final View companionView = subject.getLandscapeCompanionAdView();
        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);
        verify(mockVideoView).setVisibility(eq(View.INVISIBLE));

        assertThat(subject.getBlurLastVideoFrameTask()).isNull();
        assertThat(companionView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);
    }

    @Test
    public void onCompletion_whenCompanionAdAvailable_shouldOnlyShowTopGradientStripWidget() throws Exception {
        initializeSubject();

        final VastVideoGradientStripWidget topGradientStripWidget = subject.getTopGradientStripWidget();
        final VastVideoGradientStripWidget bottomGradientStripWidget = subject.getBottomGradientStripWidget();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        assertThat(topGradientStripWidget.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(bottomGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onCompletion_whenCompanionAdNotAvailable_shouldHideCompanionAdAndShowBlurredLastVideoFrame() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setVastCompanionAd(null, null);

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        final VideoView mockVideoView = TestVideoViewFactory.Companion.getMockVideoView();
        reset(mockVideoView);

        initializeSubject();

        final View companionView = subject.getLandscapeCompanionAdView();
        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();

        assertThat(subject.getVideoView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);
        verify(mockVideoView).setVisibility(eq(View.INVISIBLE));

        assertThat(subject.getBlurLastVideoFrameTask()).isNotNull();
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(blurredLastVideoFrameImageView.getDrawable()).isInstanceOf(BitmapDrawable.class);
        assertThat(
                ((BitmapDrawable) blurredLastVideoFrameImageView.getDrawable()).getBitmap()).isNotNull();
    }

    @Test
    public void onCompletion_whenCompanionAdNotAvailable_shouldHideBothGradientStripWidgets() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setVastCompanionAd(null, null);

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        final VastVideoGradientStripWidget topGradientStripWidget = subject.getTopGradientStripWidget();
        final VastVideoGradientStripWidget bottomGradientStripWidget = subject.getBottomGradientStripWidget();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        Robolectric.getBackgroundThreadScheduler().unPause();
        Robolectric.getForegroundThreadScheduler().unPause();
        Thread.sleep(NETWORK_DELAY);

        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        assertThat(topGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
        assertThat(bottomGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onCompletion_whenCompanionAdNotAvailableAndBlurredLastVideoFrameNotPrepared_shouldShowBlackBackground() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setVastCompanionAd(null, null);

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        final VideoView mockVideoView = TestVideoViewFactory.Companion.getMockVideoView();
        reset(mockVideoView);

        initializeSubject();

        final View companionView = subject.getLandscapeCompanionAdView();
        final ImageView blurredLastVideoFrameImageView = subject.getBlurredLastVideoFrameImageView();

        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        verify(mockVideoView).setVisibility(eq(View.INVISIBLE));
        assertThat(companionView.getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(blurredLastVideoFrameImageView.getVisibility()).isEqualTo(View.INVISIBLE);

        Drawable background = subject.getLayout().getBackground();
        assertThat(((ColorDrawable) background).getColor()).isEqualTo(Color.BLACK);
    }

    @Test
    public void onCompletion_whenCompanionAdNotAvailableAndBlurredLastVideoFrameNotPrepared_shouldHideBothGradientStripWidgets() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setVastCompanionAd(null, null);

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        final VastVideoGradientStripWidget topGradientStripWidget = subject.getTopGradientStripWidget();
        final VastVideoGradientStripWidget bottomGradientStripWidget = subject.getBottomGradientStripWidget();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        assertThat(topGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
        assertThat(bottomGradientStripWidget.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void onError_shouldFireVideoErrorAndSetVideoErrorTrue() throws Exception {
        initializeSubject();

        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_FULLSCREEN_FAIL, testBroadcastIdentifier);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlayerStateChanged(mockMediaPlayer, SessionPlayer.PLAYER_STATE_ERROR);
        Robolectric.getForegroundThreadScheduler().unPause();

        assertThat(subject.getVideoError()).isTrue();
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
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlayerStateChanged(mockMediaPlayer, SessionPlayer.PLAYER_STATE_ERROR);

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
                Collections.singletonList(new VastTracker.Builder("error" + MACRO_TAGS).build()));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.setVideoError(true);
        setVideoViewParams(12345, 15000);

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlayerStateChanged(mockMediaPlayer, SessionPlayer.PLAYER_STATE_ERROR);

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
                Collections.singletonList(new VastTracker.Builder("error" + MACRO_TAGS).build()));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
        subject.setVideoError(true);
        setVideoViewParams(12345, 15000);

        for (int i = 0; i < 10; i++) {
            final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
            mockMediaPlayer.prepare().isDone();
            subject.getPlayerCallback().onPlayerStateChanged(mockMediaPlayer, SessionPlayer.PLAYER_STATE_ERROR);
            verify(mockRequestQueue).add(argThat(isUrl(
                    "error?errorcode=400&asseturi=video_url&contentplayhead=00:00:12.345")));
        }
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void videoRunnablesRun_shouldFireOffAllProgressTrackers() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("first" + MACRO_TAGS, 0.25f).build(),
                        new VastFractionalProgressTracker.Builder("second" + MACRO_TAGS, 0.5f).build(),
                        new VastFractionalProgressTracker.Builder("third" + MACRO_TAGS, 0.75f).build()));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
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
    public void videoRunnablesRun_whenCurrentTimeLessThanSeconds_shouldNotFireStartTracker() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker.Builder("start", 2000).build()));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
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
    public void videoRunnablesRun_whenCurrentTimeGreaterThanSeconds_shouldFireStartTracker() throws Exception {
        VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker.Builder("start" + MACRO_TAGS, 2000).build()));
        vastVideoConfig.addAbsoluteTrackers(
                Arrays.asList(new VastAbsoluteProgressTracker.Builder("later" + MACRO_TAGS, 3000).build()));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
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
                Arrays.asList(new VastFractionalProgressTracker.Builder("first" + MACRO_TAGS, 0.25f).build()));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("don't call" + MACRO_TAGS, 0.28f).build()));
        bundle.putSerializable(VAST_VIDEO_CONFIG, vastVideoConfig);

        initializeSubject();
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
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("first" + MACRO_TAGS, 0.25f).build()));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("second" + MACRO_TAGS, 0.5f).build()));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
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
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("first" + MACRO_TAGS, 0.25f).build()));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("second" + MACRO_TAGS, 0.5f).build()));
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("third" + MACRO_TAGS, 0.75f).build()));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
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
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("video_url");
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.addFractionalTrackers(
                Arrays.asList(new VastFractionalProgressTracker.Builder("first" + MACRO_TAGS, 0.25f).build()));
        vastVideoConfig.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker.Builder("second" + MACRO_TAGS, 0.5f).build()));
        vastVideoConfig.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker.Builder("third" + MACRO_TAGS, 0.75f).build()));

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        when(mockMediaPlayer.getDuration()).thenReturn(100l);
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
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        when(mockMediaPlayer.getCurrentPosition()).thenReturn((long) position);
        Robolectric.getForegroundThreadScheduler().advanceToLastPostedRunnable();

        for (String url : trackingUrls) {
            verify(mockRequestQueue).add(argThat(isUrl(url)));
        }
    }

    @Test
    public void videoRunnablesRun_whenCurrentPositionIsGreaterThanShowCloseButtonDelay_shouldShowCloseButton() throws Exception {

        initializeSubject();
        setVideoViewParams(5001, 5002);
        subject.onResume();

        assertThat(subject.getShouldAllowClose()).isFalse();
        Robolectric.getForegroundThreadScheduler().unPause();

        assertThat(subject.getShouldAllowClose()).isTrue();
    }

    @Test
    public void videoRunnablesRun_whenCurrentPositionIsGreaterThanSkipOffset_shouldShowCloseButton() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("25%");    // skipoffset is at 2.5s

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(2501, 10000); // duration is 10s, current position is 1ms after skipoffset
        subject.onResume();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(2500);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();

        assertThat(subject.getShouldAllowClose()).isFalse();
        Robolectric.getForegroundThreadScheduler().unPause();

        assertThat(subject.getShouldAllowClose()).isTrue();
    }

    @Test
    public void videoRunnablesRun_whenCurrentPositionIsLessThanSkipOffset_shouldNotShowCloseButton() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setSkipOffset("00:00:03");   // skipoffset is at 3s

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();
        setVideoViewParams(2999, 10000); // duration is 10s, current position is 1ms before skipoffset
        subject.onResume();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        mockMediaPlayer.prepare().isDone();

        assertThat(subject.getShowCloseButtonDelay()).isEqualTo(3000);
        assertThat(subject.getVastVideoConfig().getSkipOffset()).isNotEmpty();

        assertThat(subject.getShouldAllowClose()).isFalse();
        Robolectric.getForegroundThreadScheduler().unPause();

        assertThat(subject.getShouldAllowClose()).isFalse();
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
        subject.setClosing(true);

        subject.onPause();
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onPause_shouldPauseAudioFocusHandler() throws Exception {
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();

        initializeSubject();

        subject.onPause();

        verify(mockMediaPlayer.getAudioFocusHandler()).close();
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
    public void onResume_withNoPreviousPosition_withIsCompleteFalse_shouldCallMediaPlayerPlay() throws Exception {
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        reset(mockMediaPlayer);

        initializeSubject();

        subject.setComplete(false);

        setVideoViewParams(0, 10000);

        subject.onPause();

        subject.onResume();

        verify(mockMediaPlayer).play();
    }

    @Test
    public void onResume_withNoPreviousPosition_withIsCompleteTrue_shouldNotCallMediaPlayerPlay() throws Exception {
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        reset(mockMediaPlayer);

        initializeSubject();

        subject.setComplete(true);

        setVideoViewParams(0, 10000);

        subject.onPause();

        subject.onResume();

        verify(mockMediaPlayer, never()).play();
    }

    @Test
    public void onResume_shouldSeekToPrePausedPosition() throws Exception {
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        reset(mockMediaPlayer);

        initializeSubject();
        setVideoViewParams(7000, 10000);

        subject.onPause();

        setVideoViewParams(1000, 10000);

        subject.onResume();

        verify(mockMediaPlayer).seekTo(eq(7000l), eq(MediaPlayer.SEEK_CLOSEST));
    }

    @Test
    public void onResume_multipleTimes_shouldFirePauseResumeTrackersMultipleTimes() throws Exception {
        initializeSubject();

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
    public void onResume_whenComplete_shouldNotFireResumeTrackers() throws Exception {
        initializeSubject();

        setVideoViewParams(10001, 10000);
        subject.onPause();
        subject.setComplete(true);
        subject.onResume();

        verify(mockRequestQueue, never()).add(argThat(isUrlStartingWith("resume?")));
    }

    @Test
    public void onConfigurationChanged_withPortraitCompanionAdVisible_withDeviceLandscape_shouldMakeLandscapeCompanionAdVisible() throws Exception {
        initializeSubject();
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject.getPortraitCompanionAdView().setVisibility(View.VISIBLE);

        subject.onConfigurationChanged(context.getResources().getConfiguration());

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

        subject.onConfigurationChanged(context.getResources().getConfiguration());

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

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getPortraitCompanionAdView().getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(subject.getLandscapeCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_3)));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onConfigurationChanged_withNoCompanionAdVisible_shouldDoNothing() throws Exception {
        initializeSubject();
        context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;

        subject.onConfigurationChanged(context.getResources().getConfiguration());

        assertThat(subject.getPortraitCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getLandscapeCompanionAdView().getVisibility()).isEqualTo(View.INVISIBLE);
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onConfigurationChanged_whenCalledMultipleTimes_shouldOnlyEverFireEachCreativeViewTrackerOnce() throws Exception {
        initializeSubject();
        subject.getPortraitCompanionAdView().setVisibility(View.VISIBLE);

        for (int i = 0; i < 10; i++) {
            context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_LANDSCAPE;
            subject.onConfigurationChanged(context.getResources().getConfiguration());
            context.getResources().getConfiguration().orientation = Configuration.ORIENTATION_PORTRAIT;
            subject.onConfigurationChanged(context.getResources().getConfiguration());
        }
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_1)));
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_2)));
        verify(mockRequestQueue).add(argThat(isUrl(COMPANION_CREATIVE_VIEW_URL_3)));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onConfigurationChanged_withNoCompanionAd_shouldDoNothing() throws Exception {
        final VastVideoConfig vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setDiskMediaFileUrl("disk_video_path");
        vastVideoConfig.setNetworkMediaFileUrl("media_url");

        final AdData currentAdData = bundle.getParcelable(AD_DATA_KEY);
        final AdData newAdData = new AdData.Builder().fromAdData(currentAdData)
                .vastVideoConfig(vastVideoConfig.toJsonString())
                .build();
        bundle.putParcelable(AD_DATA_KEY, newAdData);

        initializeSubject();

        subject.onConfigurationChanged(context.getResources().getConfiguration());

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

        subject.setShouldAllowClose(true);

        assertThat(subject.backButtonEnabled()).isTrue();
    }

    @Test
    public void onClickCloseButtonImageView_whenCloseButtonIsVisible_shouldFireCloseTrackers() throws Exception {
        initializeSubject();
        // Because it's almost never exactly 15 seconds
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        when(mockMediaPlayer.getDuration()).thenReturn(15094l);
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        subject.setShouldAllowClose(true);

        // We don't have direct access to the CloseButtonWidget icon's close event, so we manually
        // invoke its onTouchListener's onTouch callback with a fake MotionEvent.ACTION_UP action.
        View.OnTouchListener closeButtonImageViewOnTouchListener =
                shadowOf(subject.getCloseButtonWidget().getImageView()).getOnTouchListener();
        closeButtonImageViewOnTouchListener.onTouch(null, GestureUtils.createActionUp(0, 0));

        verify(mockRequestQueue).add(
                argThat(isUrl("close?errorcode=&asseturi=video_url&contentplayhead=00:00:15.094")));
    }

    @Test
    public void onClickCloseButtonTextView_whenCloseButtonIsVisible_whenGteDuration_shouldFireCloseTrackers() throws Exception {
        initializeSubject();
        // Because it's almost never exactly 15 seconds
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        when(mockMediaPlayer.getDuration()).thenReturn(15203l);
        mockMediaPlayer.prepare().isDone();
        subject.getPlayerCallback().onPlaybackCompleted(mockMediaPlayer);

        subject.setShouldAllowClose(true);

        // We don't have direct access to the CloseButtonWidget text's close event, so we manually
        // invoke its onTouchListener's onTouch callback with a fake MotionEvent.ACTION_UP action.
        View.OnTouchListener closeButtonTextViewOnTouchListener =
                shadowOf(subject.getCloseButtonWidget().getTextView()).getOnTouchListener();
        closeButtonTextViewOnTouchListener.onTouch(null, GestureUtils.createActionUp(0, 0));

        verify(mockRequestQueue).add(
                argThat(isUrl("close?errorcode=&asseturi=video_url&contentplayhead=00:00:15.203")));
    }

    @Test
    public void onClickCloseButtonTextView_whenCloseButtonIsVisible_whenLessThanDuration_shouldFireCloseTrackers_shouldFireSkipTrackers() throws Exception {
        initializeSubject();
        // Because it's almost never exactly 15 seconds
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        when(mockMediaPlayer.getDuration()).thenReturn(15000l);
        when(mockMediaPlayer.getCurrentPosition()).thenReturn(14999l);
        mockMediaPlayer.prepare().isDone();

        subject.setShouldAllowClose(true);

        // We don't have direct access to the CloseButtonWidget text's close event, so we manually
        // invoke its onTouchListener's onTouch callback with a fake MotionEvent.ACTION_UP action.
        View.OnTouchListener closeButtonTextViewOnTouchListener =
                shadowOf(subject.getCloseButtonWidget().getTextView()).getOnTouchListener();
        closeButtonTextViewOnTouchListener.onTouch(null, GestureUtils.createActionUp(0, 0));

        verify(mockRequestQueue).add(
                argThat(isUrl("close?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verify(mockRequestQueue).add(
                argThat(isUrl("skip?errorcode=&asseturi=video_url&contentplayhead=00:00:14.999")));
    }

    @Test
    public void onClickCloseButtonTextView_whenCompletionNotFired_whenCloseButtonIsVisible_whenGreaterThanDuration_shouldFireCloseTrackers_shouldFireCompleteTrackers_shouldNotFireSkipTrackers() throws Exception {
        initializeSubject();

        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        when(mockMediaPlayer.getDuration()).thenReturn(15000l);
        when(mockMediaPlayer.getCurrentPosition()).thenReturn(15001l);
        mockMediaPlayer.prepare().isDone();

        subject.setShouldAllowClose(true);
        subject.setComplete(false);

        // We don't have direct access to the CloseButtonWidget text's close event, so we manually
        // invoke its onTouchListener's onTouch callback with a fake MotionEvent.ACTION_UP action.
        View.OnTouchListener closeButtonTextViewOnTouchListener =
                shadowOf(subject.getCloseButtonWidget().getTextView()).getOnTouchListener();
        closeButtonTextViewOnTouchListener.onTouch(null, GestureUtils.createActionUp(0, 0));

        verify(mockRequestQueue).add(argThat(isUrl(
                "complete?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verify(mockRequestQueue).add(
                argThat(isUrl("close?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void onClickCloseButtonTextView_whenCompletionNotFired_whenCloseButtonIsVisible_whenEqualToDuration_shouldFireCloseTrackers_shouldFireCompleteTrackers_shouldNotFireSkipTrackers() throws Exception {
        initializeSubject();

        // Because it's almost never exactly 15 seconds
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();
        when(mockMediaPlayer.getDuration()).thenReturn(15000l);
        when(mockMediaPlayer.getCurrentPosition()).thenReturn(15000l);
        mockMediaPlayer.prepare().isDone();

        subject.setShouldAllowClose(true);
        subject.setComplete(false);

        // We don't have direct access to the CloseButtonWidget text's close event, so we manually
        // invoke its onTouchListener's onTouch callback with a fake MotionEvent.ACTION_UP action.
        View.OnTouchListener closeButtonTextViewOnTouchListener =
                shadowOf(subject.getCloseButtonWidget().getTextView()).getOnTouchListener();
        closeButtonTextViewOnTouchListener.onTouch(null, GestureUtils.createActionUp(0, 0));

        verify(mockRequestQueue).add(argThat(isUrl(
                "complete?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verify(mockRequestQueue).add(
                argThat(isUrl("close?errorcode=&asseturi=video_url&contentplayhead=00:00:15.000")));
        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void VastWebView_onVastWebViewClick_shouldCallVastIconHandleClick() throws Exception {
        initializeSubject();

        VastIconConfig mockVastIconConfig = mock(VastIconConfig.class);
        when(mockVastIconConfig.getWidth()).thenReturn(40);
        when(mockVastIconConfig.getHeight()).thenReturn(40);
        VastResource mockVastResource = mock(VastResource.class);
        when(mockVastResource.getType()).thenReturn(VastResource.Type.STATIC_RESOURCE);
        when(mockVastResource.getResource()).thenReturn("static");
        when(mockVastIconConfig.getVastResource()).thenReturn(mockVastResource);

        ReflectionUtils.setVariableValueInObject(subject, "vastIconConfig", mockVastIconConfig);

        VastWebView view = (VastWebView) ReflectionUtils.getValueIncludingSuperclasses("iconView", subject);

        view.getVastWebViewClickListener().onVastWebViewClick();
        verify(mockVastIconConfig).handleClick(any(Context.class), any(String.class), eq("dsp_creative_id"));
    }

    @Test
    public void handleIconDisplay_withCurrentPositionGreaterThanOffset_shouldSetIconToVisible_shouldCallHandleImpression() throws Exception {
        initializeSubject();

        VastIconConfig mockVastIconConfig = mock(VastIconConfig.class);
        when(mockVastIconConfig.getOffsetMS()).thenReturn(0);
        when(mockVastIconConfig.getDurationMS()).thenReturn(1);

        ReflectionUtils.setVariableValueInObject(subject, "vastIconConfig", mockVastIconConfig);

        subject.handleIconDisplay(0);

        assertThat(subject.getIconView().getVisibility()).isEqualTo(View.VISIBLE);
        verify(mockVastIconConfig).handleImpression(any(Context.class), eq(0), eq("video_url"));
    }

    @Test
    public void handleIconDisplay_withCurrentPositionLessThanOffset_shouldReturn() throws Exception {
        initializeSubject();

        VastIconConfig mockVastIconConfig = mock(VastIconConfig.class);
        when(mockVastIconConfig.getOffsetMS()).thenReturn(1);

        ReflectionUtils.setVariableValueInObject(subject, "vastIconConfig", mockVastIconConfig);

        VastWebView view = (VastWebView) ReflectionUtils.getValueIncludingSuperclasses("iconView", subject);

        subject.handleIconDisplay(0);

        assertThat(subject.getIconView().getVisibility()).isEqualTo(View.INVISIBLE);
        verify(mockVastIconConfig, never()).handleImpression(any(Context.class), eq(0),
                eq("video_url"));
    }

    @Test
    public void handleIconDisplay_withCurrentPositionGreaterThanOffsetPlusDuration_shouldSetIconToGone() throws Exception {
        initializeSubject();

        VastIconConfig mockVastIconConfig = mock(VastIconConfig.class);
        when(mockVastIconConfig.getOffsetMS()).thenReturn(0);
        when(mockVastIconConfig.getDurationMS()).thenReturn(1);

        ReflectionUtils.setVariableValueInObject(subject, "vastIconConfig", mockVastIconConfig);

        subject.handleIconDisplay(2);

        assertThat(subject.getIconView().getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void makeInteractable_shouldHideCountdownWidgetAndShowCtaAndCloseButtonWidgets() throws Exception {
        initializeSubject();

        subject.updateCountdown(true);

        assertThat(subject.getRadialCountdownWidget().getVisibility()).isEqualTo(View.GONE);
        assertThat(subject.getCloseButtonWidget().getVisibility()).isEqualTo(View.VISIBLE);
    }

    private void initializeSubject() throws IllegalAccessException {
        subject = new VastVideoViewController((Activity) context, bundle, savedInstanceState,
                testBroadcastIdentifier, baseVideoViewControllerListener);
        subject.setMediaMetadataRetriever(mockMediaMetadataRetriever);
        setVideoViewParams(0, 10000);// default to position 0 and duration 10s
        spyOnRunnables();
    }

    private void spyOnRunnables() throws IllegalAccessException {
        final VastVideoViewProgressRunnable progressCheckerRunnable = (VastVideoViewProgressRunnable) ReflectionUtils.getValueIncludingSuperclasses("progressCheckerRunnable", subject);
        spyProgressRunnable = spy(progressCheckerRunnable);

        final VastVideoViewCountdownRunnable countdownRunnable = (VastVideoViewCountdownRunnable) ReflectionUtils.getValueIncludingSuperclasses("countdownRunnable", subject);
        spyCountdownRunnable = spy(countdownRunnable);

        ReflectionUtils.setVariableValueInObject(subject, "progressCheckerRunnable", spyProgressRunnable);
        ReflectionUtils.setVariableValueInObject(subject, "countdownRunnable", spyCountdownRunnable);
    }

    private void setVideoViewParams(long currentPosition, long duration) {
        final MediaPlayer mockMediaPlayer = TestMediaPlayerFactory.Companion.getMockMediaPlayer();

        when(mockMediaPlayer.getCurrentPosition()).thenReturn(currentPosition);
        when(mockMediaPlayer.getDuration()).thenReturn(duration);
    }

    private void setFractionalProgressTrackersTracked(VastVideoConfig vastVideoConfig) {
        for (VastFractionalProgressTracker tracker : vastVideoConfig.getFractionalTrackers()) {
            tracker.setTracked();
        }
    }

    private void setAbsoluteProgressTrackersTracked(VastVideoConfig vastVideoConfig) {
        for (VastAbsoluteProgressTracker tracker : vastVideoConfig.getAbsoluteTrackers()) {
            tracker.setTracked();
        }
    }
}
