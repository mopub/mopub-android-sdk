package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;

import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BaseVideoPlayerActivity;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.MraidVideoPlayerActivity;
import com.mopub.mobileads.VastManager;
import com.mopub.mobileads.VastTracker;
import com.mopub.mobileads.VastVideoConfig;
import com.mopub.mobileads.VideoViewabilityTracker;
import com.mopub.nativeads.BaseNativeAd.NativeEventListener;
import com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import com.mopub.nativeads.MoPubCustomEventVideoNative.HeaderVisibilityStrategy;
import com.mopub.nativeads.MoPubCustomEventVideoNative.MoPubVideoNativeAd;
import com.mopub.nativeads.MoPubCustomEventVideoNative.MoPubVideoNativeAd.VideoState;
import com.mopub.nativeads.MoPubCustomEventVideoNative.NativeVideoControllerFactory;
import com.mopub.nativeads.MoPubCustomEventVideoNative.PayloadVisibilityStrategy;
import com.mopub.nativeads.MoPubCustomEventVideoNative.VideoResponseHeaders;
import com.mopub.nativeads.NativeVideoController.VisibilityTrackingEvent;
import com.mopub.network.MaxWidthImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.toolbox.ImageLoader;
import com.mopub.volley.toolbox.ImageLoader.ImageListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubVideoNativeAdTest {

    private MoPubVideoNativeAd subject;
    private Activity activity;
    private JSONObject jsonObject;
    private Map<String, String> serverExtras;
    private VideoResponseHeaders videoResponseHeaders;

    @Mock private CustomEventNativeListener mockCustomEventNativeListener;
    @Mock private VastVideoConfig mockVastVideoConfig;
    @Mock private MaxWidthImageLoader mockImageLoader;
    @Mock private ImageLoader.ImageContainer mockImageContainer;
    @Mock private NativeVideoController mockNativeVideoController;
    @Mock private VisibilityTracker mockVisibilityTracker;
    @Mock private NativeVideoControllerFactory mockNativeVideoControllerFactory;
    @Mock private MediaLayout mockMediaLayout;
    @Mock private View mockRootView;
    @Mock private SurfaceTexture mockSurfaceTexture;
    @Mock private TextureView mockTextureView;
    @Mock private Drawable mockDrawable;
    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private NativeEventListener mockNativeEventListener;
    @Mock private VastManager mockVastManager;

    @Before
    public void setUp() throws Exception {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        jsonObject = new JSONObject();
        jsonObject.put("imptracker", new JSONArray("[\"url1\", \"url2\"]"));
        jsonObject.put("clktracker", "json click tracker");
        jsonObject.put("title", "title");
        jsonObject.put("text", "text");
        jsonObject.put("mainimage", "mainimageurl");
        jsonObject.put("iconimage", "iconimageurl");
        jsonObject.put("clk", "clk");
        jsonObject.put("fallback", "fallback");
        jsonObject.put("ctatext", "ctatext");
        jsonObject.put("video", "video");
        jsonObject.put("extraimage", "extraimageurl");

        serverExtras = new HashMap<String, String>();
        serverExtras.put("Play-Visible-Percent", "10");
        serverExtras.put("Pause-Visible-Percent", "5");
        serverExtras.put("Impression-Min-Visible-Percent", "15");
        serverExtras.put("Impression-Visible-Ms", "100");
        serverExtras.put("Max-Buffer-Ms", "20");
        videoResponseHeaders = new VideoResponseHeaders(serverExtras);

        when(mockVastVideoConfig.getVideoViewabilityTracker())
                .thenReturn(new VideoViewabilityTracker(98, 76, "viewabilityTracker"));

        List<VastTracker> vastTrackers = new ArrayList<VastTracker>();
        vastTrackers.add(new VastTracker("vastimpression1"));
        vastTrackers.add(new VastTracker("vastimpression2"));

        when(mockVastVideoConfig.getImpressionTrackers()).thenReturn(vastTrackers);

        subject = new MoPubVideoNativeAd(
                activity, jsonObject, mockCustomEventNativeListener, videoResponseHeaders,
                mockVisibilityTracker, mockNativeVideoControllerFactory,
                "header click tracker", mockVastManager);
        subject.setNativeEventListener(mockNativeEventListener);
        // noinspection unchecked
        when(mockNativeVideoControllerFactory
                .createForId(anyInt(), any(Context.class), any(List.class), eq(mockVastVideoConfig)))
                .thenReturn(mockNativeVideoController);

        when(mockImageLoader.get(anyString(), any(ImageListener.class)))
                .then(new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                        ImageListener listener = ((ImageListener) invocationOnMock.getArguments()[1]);
                        listener.onResponse(mockImageContainer, false);
                        return null;
                    }
                });
        when(mockMediaLayout.getTextureView()).thenReturn(mockTextureView);

        stub(mockImageContainer.getBitmap()).toReturn(mock(Bitmap.class));
        Networking.setImageLoaderForTesting(mockImageLoader);
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @After
    public void tearDown() {
        Networking.setImageLoaderForTesting(null);
        Networking.setRequestQueueForTesting(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadAd_withoutImpTrackerRequiredKey_shouldThrowIllegalArgumentException() {
        jsonObject.remove("imptracker");
        subject.loadAd();
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadAd_withoutClkTrackerRequiredKey_shouldThrowIllegalArgumentException() {
        jsonObject.remove("clktracker");
        subject.loadAd();
    }

    @Test(expected = IllegalArgumentException.class)
    public void loadAd_withInvalidValueForRequiredKey_shouldThrowIllegalArgumentException() throws Exception {
        jsonObject.put("imptracker", 123);
        subject.loadAd();
    }

    @Test
    public void loadAd_withInvalidValueForOptionalKey_shouldNotThrowExcpetion() throws Exception {
        jsonObject.put("title", 123);
        subject.loadAd();
    }

    @Test
    public void loadAd_shouldInitializeAssetValues() {
        subject.loadAd();

        assertThat(subject.getImpressionTrackers()).containsOnly("url1", "url2");
        assertThat(subject.getTitle()).isEqualTo("title");
        assertThat(subject.getText()).isEqualTo("text");
        assertThat(subject.getMainImageUrl()).isEqualTo("mainimageurl");
        assertThat(subject.getIconImageUrl()).isEqualTo("iconimageurl");
        assertThat(subject.getClickDestinationUrl()).isEqualTo("clk");
        assertThat(subject.getCallToAction()).isEqualTo("ctatext");
        assertThat(subject.getPrivacyInformationIconClickThroughUrl()).isEqualTo(
                "https://www.mopub.com/optout/");
        assertThat(subject.getVastVideo()).isEqualTo("video");
        assertThat(subject.getExtra("extraimage")).isEqualTo("extraimageurl");
        assertThat(subject.getExtras()).hasSize(1);
    }

    @Test
    public void loadAd_shouldPrecacheImages_andLoadVastXml() {
        subject.loadAd();

        verify(mockImageLoader).get(eq("mainimageurl"), any(ImageListener.class));
        verify(mockImageLoader).get(eq("iconimageurl"), any(ImageListener.class));
        verify(mockImageLoader).get(eq("extraimageurl"), any(ImageListener.class));
        verify(mockVastManager).prepareVastVideoConfiguration(eq("video"),
                any(VastManager.VastManagerListener.class), anyString(), any(Context.class));
    }

    @Test
    public void onVastVideoConfigurationPrepared_withNullVastVideoConfig_shouldNotifyListenerFailed() {
        subject.onVastVideoConfigurationPrepared(null);
        verify(mockCustomEventNativeListener).onNativeAdFailed(NativeErrorCode.INVALID_RESPONSE);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onVastVideoConfigurationPrepared_shouldConstructNativeVideoController_shouldNotifyListenerOfAdLoaded() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);

        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockNativeVideoControllerFactory).createForId(anyInt(),
                eq(activity.getApplicationContext()),
                argumentCaptor.capture(),
                eq(mockVastVideoConfig));

        List<VisibilityTrackingEvent> visibilityTrackingEvents = (List<VisibilityTrackingEvent>) argumentCaptor.getValue();
        assertThat(visibilityTrackingEvents.get(0).strategy).isInstanceOf(HeaderVisibilityStrategy.class);
        assertThat(visibilityTrackingEvents.get(0).minimumPercentageVisible).isEqualTo(15);
        assertThat(visibilityTrackingEvents.get(0).totalRequiredPlayTimeMs).isEqualTo(100);

        assertThat(visibilityTrackingEvents.get(1).strategy).isInstanceOf(PayloadVisibilityStrategy.class);
        assertThat(visibilityTrackingEvents.get(1).minimumPercentageVisible).isEqualTo(15);
        assertThat(visibilityTrackingEvents.get(1).totalRequiredPlayTimeMs).isEqualTo(100);

        assertThat(visibilityTrackingEvents.get(2).strategy).isInstanceOf(PayloadVisibilityStrategy.class);
        assertThat(visibilityTrackingEvents.get(2).minimumPercentageVisible).isEqualTo(15);
        assertThat(visibilityTrackingEvents.get(2).totalRequiredPlayTimeMs).isEqualTo(100);

        assertThat(visibilityTrackingEvents.get(3).strategy).isInstanceOf(PayloadVisibilityStrategy.class);
        assertThat(visibilityTrackingEvents.get(3).minimumPercentageVisible).isEqualTo(76);
        assertThat(visibilityTrackingEvents.get(3).totalRequiredPlayTimeMs).isEqualTo(98);

        verify(mockVastVideoConfig).getImpressionTrackers();
        verify(mockVastVideoConfig).addClickTrackers(any(List.class));
        verify(mockVastVideoConfig).setClickThroughUrl("clk");
        verify(mockCustomEventNativeListener).onNativeAdLoaded(subject);
    }

    @Ignore("This test is broken")
    @SuppressWarnings("unchecked")
    @Test
    public void onVastVideoConfigurationPrepared_shouldMergeHeaderAndJsonClickTrackers() {
        final ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);

        verify(mockVastVideoConfig).addClickTrackers(argumentCaptor.capture());
        final List<VastTracker> actualClickTrackers = (List<VastTracker>) argumentCaptor.getValue();
        assertThat(actualClickTrackers.size()).isEqualTo(2);
        final VastTracker headerClickTracker = actualClickTrackers.get(0);
        final VastTracker jsonClickTracker = actualClickTrackers.get(1);
        assertThat(headerClickTracker.getContent()).isEqualTo("header click tracker");
        assertThat(headerClickTracker.isRepeatable()).isFalse();
        assertThat(jsonClickTracker.getContent()).isEqualTo("json click tracker");
        assertThat(jsonClickTracker.isRepeatable()).isFalse();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onVastVideoConfigurationPrepared_shouldDedupeHeaderAndJsonClickTrackers() throws Exception {
        jsonObject.remove("clktracker");
        jsonObject.put("clktracker", "header click tracker");
        final ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);

        verify(mockVastVideoConfig).addClickTrackers(argumentCaptor.capture());
        final List<VastTracker> actualClickTrackers = (List<VastTracker>) argumentCaptor.getValue();
        assertThat(actualClickTrackers.size()).isEqualTo(1);
        final VastTracker clickTracker = actualClickTrackers.get(0);
        assertThat(clickTracker.getContent()).isEqualTo("header click tracker");
        assertThat(clickTracker.isRepeatable()).isFalse();
    }

    @Ignore("This test is broken")
    @SuppressWarnings("unchecked")
    @Test
    public void onVastVideoConfigurationPrepared_shouldAcceptJsonArrayClickTrackers() throws Exception {
        jsonObject.remove("clktracker");
        jsonObject.put("clktracker",
                new JSONArray("[\"json click tracker 1\", \"json click tracker 2\"]"));
        final ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);

        verify(mockVastVideoConfig).addClickTrackers(argumentCaptor.capture());
        final List<VastTracker> actualClickTrackers = (List<VastTracker>) argumentCaptor.getValue();
        assertThat(actualClickTrackers.size()).isEqualTo(3);
        final VastTracker jsonClickTracker1 = actualClickTrackers.get(0);
        final VastTracker jsonClickTracker2 = actualClickTrackers.get(1);
        final VastTracker headerClickTracker = actualClickTrackers.get(2);
        assertThat(jsonClickTracker1.getContent()).isEqualTo("json click tracker 1");
        assertThat(jsonClickTracker1.isRepeatable()).isFalse();
        assertThat(jsonClickTracker2.getContent()).isEqualTo("json click tracker 2");
        assertThat(jsonClickTracker2.isRepeatable()).isFalse();
        assertThat(headerClickTracker.getContent()).isEqualTo("header click tracker");
        assertThat(headerClickTracker.isRepeatable()).isFalse();
    }

    @Ignore("This test is broken")
    @SuppressWarnings("unchecked")
    @Test
    public void onVastVideoConfigurationPrepared_shouldDedupeJsonArrayClickTrackers() throws Exception {
        jsonObject.remove("clktracker");
        jsonObject.put("clktracker",
                new JSONArray("[\"json click tracker\", \"header click tracker\"]"));
        final ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);

        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);

        verify(mockVastVideoConfig).addClickTrackers(argumentCaptor.capture());
        final List<VastTracker> actualClickTrackers = (List<VastTracker>) argumentCaptor.getValue();
        assertThat(actualClickTrackers.size()).isEqualTo(2);
        final VastTracker headerClickTracker = actualClickTrackers.get(0);
        final VastTracker jsonClickTracker = actualClickTrackers.get(1);
        assertThat(headerClickTracker.getContent()).isEqualTo("header click tracker");
        assertThat(headerClickTracker.isRepeatable()).isFalse();
        assertThat(jsonClickTracker.getContent()).isEqualTo("json click tracker");
        assertThat(jsonClickTracker.isRepeatable()).isFalse();
    }

    @Test
    public void render_shouldAddViewToVisibilityTracker() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        verify(mockVisibilityTracker).addView(mockRootView, mockMediaLayout, 10, 5, null);
    }

    @Test
    public void render_shouldSetupMediaLayout() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        verify(mockMediaLayout).setSurfaceTextureListener(any(SurfaceTextureListener.class));
        verify(mockMediaLayout).setPlayButtonClickListener(any(View.OnClickListener.class));
        verify(mockMediaLayout).setMuteControlClickListener(any(View.OnClickListener.class));
        verify(mockMediaLayout).setOnClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void render_shouldApplyStatePaused() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        verify(mockMediaLayout).setMode(MediaLayout.Mode.PAUSED);
    }

    @Test
    public void render_withPlaybackStateCleared_shouldPrepareNativeVideoController() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        when(mockNativeVideoController.getPlaybackState()).thenReturn(NativeVideoController
                .STATE_CLEARED);
        subject.render(mockMediaLayout);

        verify(mockNativeVideoController).prepare(subject);
    }

    @Test
    public void MediaLayout_surfaceTextureListener_onSurfaceTextureAvailable_shouldSetupNativeVideoController_shouldResetMediaLayoutProgress() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        ArgumentCaptor<SurfaceTextureListener> argumentCaptor =
                ArgumentCaptor.forClass(SurfaceTextureListener.class);
        verify(mockMediaLayout).setSurfaceTextureListener(argumentCaptor.capture());
        SurfaceTextureListener surfaceTextureListener = argumentCaptor.getValue();

        surfaceTextureListener.onSurfaceTextureAvailable(mockSurfaceTexture, 0, 0);

        verify(mockNativeVideoController).setListener(subject);
        verify(mockNativeVideoController).setOnAudioFocusChangeListener(subject);
        verify(mockNativeVideoController).setProgressListener(subject);
        verify(mockNativeVideoController).setTextureView(mockMediaLayout.getTextureView());
        verify(mockMediaLayout).resetProgress();
    }

    @Test
    public void MediaLayout_surfaceTextureListener_onSurfaceTextureAvailable_withCurrentPositionWithinThreshhold_withStateEnded_shouldSetFinalFrameAsMainImage() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        ArgumentCaptor<SurfaceTextureListener> argumentCaptor =
                ArgumentCaptor.forClass(SurfaceTextureListener.class);
        verify(mockMediaLayout).setSurfaceTextureListener(argumentCaptor.capture());
        SurfaceTextureListener surfaceTextureListener = argumentCaptor.getValue();

        when(mockNativeVideoController.getPlaybackState())
                .thenReturn(NativeVideoController.STATE_ENDED);
        when(mockNativeVideoController.getCurrentPosition()).thenReturn(9L);
        when(mockNativeVideoController.getDuration()).thenReturn(10L);
        when(mockNativeVideoController.hasFinalFrame()).thenReturn(true);
        when(mockNativeVideoController.getFinalFrame()).thenReturn(mockDrawable);

        surfaceTextureListener.onSurfaceTextureAvailable(mockSurfaceTexture, 0, 0);

        verify(mockMediaLayout).setMainImageDrawable(mockNativeVideoController.getFinalFrame());
    }

    @Test
    public void MediaLayout_surfaceTextureListener_onSurfaceTextureAvailable_withNeedsPrepare_shouldPrepareNativeVideoController() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.render(mockMediaLayout);

        ArgumentCaptor<SurfaceTextureListener> argumentCaptor =
                ArgumentCaptor.forClass(SurfaceTextureListener.class);
        verify(mockMediaLayout).setSurfaceTextureListener(argumentCaptor.capture());
        SurfaceTextureListener surfaceTextureListener = argumentCaptor.getValue();

        surfaceTextureListener.onSurfaceTextureAvailable(mockSurfaceTexture, 0, 0);

        verify(mockNativeVideoController).prepare(subject);
    }

    @Test
    public void MediaLayout_surfaceTextureListener_onSurfaceTextureDestroyed_shouldSetNeedsPrepareTrue_shouldReleaseMoPubNativeVideoAd_shouldApplyStatePaused() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        ArgumentCaptor<SurfaceTextureListener> argumentCaptor =
                ArgumentCaptor.forClass(SurfaceTextureListener.class);
        verify(mockMediaLayout).setSurfaceTextureListener(argumentCaptor.capture());
        SurfaceTextureListener surfaceTextureListener = argumentCaptor.getValue();

        surfaceTextureListener.onSurfaceTextureDestroyed(mockSurfaceTexture);

        assertThat(subject.needsPrepare()).isTrue();
        verify(mockNativeVideoController).release(subject);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.PAUSED);
    }

    @Test
    public void MediaLayout_playButtonClickListener_shouldResetMediaLayoutProgress_shouldSeekTo0_shouldSetEndedFalse_shouldSetNeedsSeekFalse() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        ArgumentCaptor<View.OnClickListener> argumentCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(mockMediaLayout).setPlayButtonClickListener(argumentCaptor.capture());

        View.OnClickListener onClickListener = argumentCaptor.getValue();
        onClickListener.onClick(null);

        verify(mockMediaLayout).resetProgress();
        verify(mockNativeVideoController).seekTo(0);
        assertThat(subject.hasEnded()).isEqualTo(false);
        assertThat(subject.needsSeek()).isEqualTo(false);
    }

    @Test
    public void MediaLayout_muteButtonClickListener_withStateReady_shouldToggleMutedState() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        ArgumentCaptor<View.OnClickListener> argumentCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(mockMediaLayout).setMuteControlClickListener(argumentCaptor.capture());

        View.OnClickListener onClickListener = argumentCaptor.getValue();
        onClickListener.onClick(null);
        assertThat(subject.isMuted()).isFalse();

        onClickListener.onClick(null);
        assertThat(subject.isMuted()).isTrue();
    }

    @Test
    public void MediaLayout_clickListener_shouldPrepareToLeaveView_shouldTriggerImpressionTracker_shouldNotDisableAppAudio_shouldStartFullScreenVideoActivity() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        ArgumentCaptor<View.OnClickListener> argumentCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(mockMediaLayout).setOnClickListener(argumentCaptor.capture());

        reset(mockNativeVideoController);
        View.OnClickListener onClickListener = argumentCaptor.getValue();
        onClickListener.onClick(null);

        assertThat(subject.needsSeek()).isTrue();
        assertThat(subject.needsPrepare()).isTrue();
        assertThat(subject.needsPrepare()).isTrue();
        verify(mockNativeVideoController).setListener(null);
        verify(mockNativeVideoController).setOnAudioFocusChangeListener(null);
        verify(mockNativeVideoController).setProgressListener(null);
        verify(mockNativeVideoController).clear();
        verify(mockNativeVideoController).triggerImpressionTrackers();
        verify(mockMediaLayout).setMode(MediaLayout.Mode.PAUSED);
        verify(mockNativeVideoController, never()).setAppAudioEnabled(anyBoolean());

        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo(MraidVideoPlayerActivity.class.getName());
        assertThat(startedActivity.getStringExtra(BaseVideoPlayerActivity.VIDEO_CLASS_EXTRAS_KEY))
                .isEqualTo("native");
        assertThat(startedActivity.getLongExtra(Constants.NATIVE_VIDEO_ID, 0L))
                .isGreaterThan(0L);
        assertThat(startedActivity.getSerializableExtra(Constants
                .NATIVE_VAST_VIDEO_CONFIG))
                .isEqualTo(mockVastVideoConfig);
    }

    @Test
    public void prepare_shouldSetOnClickListenerOnView() {
        subject.prepare(mockRootView);
        verify(mockRootView).setOnClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void RootView_onClickListener_onClick_shouldPrepareToLeaveView_shouldNotDisableAppAudio_shouldNotNotifyAdClicked_shouldTriggerImpressionTrackers_shouldshouldHandleCtaClick() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        ArgumentCaptor<View.OnClickListener> argumentCaptor =
                ArgumentCaptor.forClass(View.OnClickListener.class);
        verify(mockRootView).setOnClickListener(argumentCaptor.capture());

        View.OnClickListener onClickListener = argumentCaptor.getValue();
        reset(mockNativeVideoController);
        onClickListener.onClick(null);

        assertThat(subject.needsSeek()).isTrue();
        assertThat(subject.needsPrepare()).isTrue();
        assertThat(subject.needsPrepare()).isTrue();
        verify(mockNativeVideoController).setListener(null);
        verify(mockNativeVideoController).setOnAudioFocusChangeListener(null);
        verify(mockNativeVideoController).setProgressListener(null);
        verify(mockNativeVideoController).clear();
        verify(mockMediaLayout).setMode(MediaLayout.Mode.PAUSED);
        verify(mockNativeVideoController).handleCtaClick(activity.getApplicationContext());
        verify(mockNativeEventListener, never()).onAdClicked();
        verify(mockNativeVideoController).triggerImpressionTrackers();
        verify(mockNativeVideoController, never()).setAppAudioEnabled(anyBoolean());
    }

    @Test
    public void clear_shouldClearNativeVideoController_shouldSetMediaLayoutModeImage_shouldSetMediaLayoutNull_shouldSetMediaLayoutListenersToNull_shouldRemoveMediaLayoutFromVisibilityTracker() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);
        subject.clear(mockRootView);

        verify(mockNativeVideoController).clear();
        verify(mockMediaLayout).setMode(MediaLayout.Mode.IMAGE);
        verify(mockMediaLayout).setSurfaceTextureListener(null);
        verify(mockMediaLayout).setPlayButtonClickListener(null);
        verify(mockMediaLayout).setMuteControlClickListener(null);
        verify(mockMediaLayout).setOnClickListener(null);
        verify(mockVisibilityTracker).removeView(mockMediaLayout);
        assertThat(subject.getMediaLayout()).isNull();
    }

    @Test
    public void destroy_shouldSetPlayWhenReadyFalse_shouldReleaseNativeVideoController_shouldRemoveNativeVideoController_shouldDestroyVisibilityTracker() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        reset(mockNativeVideoController);
        subject.destroy();

        verify(mockMediaLayout).setMode(MediaLayout.Mode.IMAGE);
        verify(mockMediaLayout).setSurfaceTextureListener(null);
        verify(mockMediaLayout).setPlayButtonClickListener(null);
        verify(mockMediaLayout).setMuteControlClickListener(null);
        verify(mockMediaLayout).setOnClickListener(null);
        verify(mockVisibilityTracker).removeView(mockMediaLayout);

        assertThat(subject.getMediaLayout()).isNull();
        verify(mockNativeVideoController).setPlayWhenReady(false);
        verify(mockNativeVideoController).release(subject);
        assertThat(NativeVideoController.getForId(subject.getId())).isNull();
        verify(mockVisibilityTracker).destroy();
    }

    @Test
    public void onStateChanged_shouldAppropriatelySetVideoState() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.onStateChanged(true, NativeVideoController.STATE_IDLE);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.LOADING);

        subject.onStateChanged(true, NativeVideoController.STATE_BUFFERING);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.BUFFERING);

        subject.onStateChanged(true, NativeVideoController.STATE_READY);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.PAUSED);

        subject.setLatestVisibility(true);
        subject.onStateChanged(true, NativeVideoController.STATE_READY);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.PLAYING_MUTED);

        subject.setMuted(false);
        subject.onStateChanged(true, NativeVideoController.STATE_READY);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.PLAYING);

        subject.onStateChanged(true, NativeVideoController.STATE_ENDED);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.ENDED);
        assertThat(subject.hasEnded()).isTrue();
    }

    @Test
    public void onError_shouldSetVideoStateError() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.onError(new Exception());

        assertThat(subject.getVideoState()).isEqualTo(VideoState.FAILED_LOAD);
    }

    @Test
    public void updateProgress_shouldUpdateMediaLayoutProgress() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.updateProgress(312);

        verify(mockMediaLayout).updateProgress(312);
    }

    @Test
    public void onAudioFocusChange_withFocusChangeAudioFocusLossOrAudioFocusLossTransient_shouldMuteTheVideo() throws Exception {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.setMuted(false);
        subject.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);

        assertThat(subject.isMuted()).isTrue();

        subject.setMuted(false);
        subject.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT);

        assertThat(subject.isMuted()).isTrue();
    }

    @Test
    public void onAudioFocusChange_withFocusChangeAudioFocusLossTransientCanDuck_shouldLowerVolume() throws Exception {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);

        verify(mockNativeVideoController).setAudioVolume(0.3f);
    }

    @Test
    public void onAudioFocusChange_withFocusChangeAudioFocusGain_shouldRaiseVolume() throws Exception {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);

        verify(mockNativeVideoController).setAudioVolume(1.0f);
    }

    @Test
    public void applyState_shouldHandleError_shouldSetAppAudioEnabledFalse_shouldSetMediaLayoutModeImage() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        reset(mockNativeVideoController);
        subject.applyState(VideoState.FAILED_LOAD);

        verify(mockVastVideoConfig).handleError(activity.getApplicationContext(), null, 0);
        verify(mockNativeVideoController).setAppAudioEnabled(false);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.IMAGE);
    }

    @Test
    public void applyState_withVideoStateCreatedOrLoading_shouldSetPlayWhenReadyTrue_shouldSetMediaLayoutModeLoading() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.applyState(VideoState.CREATED);

        verify(mockNativeVideoController).setPlayWhenReady(true);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.LOADING);

        reset(mockNativeVideoController);
        reset(mockMediaLayout);
        subject.applyState(VideoState.LOADING);

        verify(mockNativeVideoController).setPlayWhenReady(true);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.LOADING);
    }

    @Test
    public void applyState_withVideoStateBuffering_shouldSetPlayWhenReadyTrue_shouldSetMediaLayoutModeBuffering() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.applyState(VideoState.BUFFERING);

        verify(mockNativeVideoController).setPlayWhenReady(true);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.BUFFERING);
    }

    @Test
    public void applyState_withVideoStatePaused_withTransitionToFullScreenFalse_shouldSetAppAudioEnabledFalse_shouldSetPlayWhenReadyFalse_shouldSetMediaLayoutModePaused() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.applyState(VideoState.PAUSED);

        verify(mockNativeVideoController).setAppAudioEnabled(false);
        verify(mockNativeVideoController).setPlayWhenReady(false);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.PAUSED);
    }

    @Test
    public void applyState_withVideoStatePlaying_shouldSetPlayWhenReadyTrue_shouldSetAudioEnabledTrue_shouldSetAppAudioEnabledTrue_shouldSetMediaLayoutModePlaying_shouldSetMuteStateUnmuted() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        reset(mockNativeVideoController);
        subject.applyState(VideoState.PLAYING);

        verify(mockNativeVideoController).setPlayWhenReady(true);
        verify(mockNativeVideoController).setAudioEnabled(true);
        verify(mockNativeVideoController).setAppAudioEnabled(true);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.PLAYING);
        verify(mockMediaLayout).setMuteState(MediaLayout.MuteState.UNMUTED);
    }

    @Test
    public void applyState_withVideoStatePlaying_withNeedsSeek_shouldSeekToCurrentPosition() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        assertThat(subject.needsSeek()).isTrue();
        when(mockNativeVideoController.getCurrentPosition()).thenReturn(543L);
        subject.applyState(VideoState.PLAYING);

        verify(mockNativeVideoController).seekTo(mockNativeVideoController.getCurrentPosition());
    }

    @Test
    public void applyState_withVideoStatePlayingMuted_shouldSetPlayWhenReadyTrue_shouldSetAudioEnabledFalse_shouldSetAppAudioEnabledTrue_shouldSetMediaLayoutModePlaying_shouldSetMuteStateMuted() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        reset(mockNativeVideoController);
        subject.applyState(VideoState.PLAYING_MUTED);

        verify(mockNativeVideoController).setPlayWhenReady(true);
        verify(mockNativeVideoController).setAudioEnabled(false);
        verify(mockNativeVideoController).setAppAudioEnabled(false);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.PLAYING);
        verify(mockMediaLayout).setMuteState(MediaLayout.MuteState.MUTED);
    }

    @Test
    public void applyState_withVideoStatePlayingMuted_withNeedsSeek_shouldSeekToCurrentPosition() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        assertThat(subject.needsSeek()).isTrue();
        when(mockNativeVideoController.getCurrentPosition()).thenReturn(543L);
        subject.applyState(VideoState.PLAYING_MUTED);

        verify(mockNativeVideoController).seekTo(mockNativeVideoController.getCurrentPosition());
    }

    @Test
    public void applyState_withVideoStatePlayingEnded_shouldHandleComplete_shouldSetAppAudioEnabledTrue_shouldSetMediaLayoutModeFinished_shouldUpdateMediaLayoutProgress1000() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        reset(mockNativeVideoController);
        subject.applyState(VideoState.ENDED);

        verify(mockVastVideoConfig).handleComplete(activity.getApplicationContext(), 0);
        verify(mockNativeVideoController).setAppAudioEnabled(false);
        verify(mockMediaLayout).setMode(MediaLayout.Mode.FINISHED);
        verify(mockMediaLayout).updateProgress(1000);

        verify(mockMediaLayout, never()).setMainImageDrawable(any(Drawable.class));
    }

    @Test
    public void applyState_withVideoStatePlayingEnded_withFinalFrame_shouldSetMainImageDrawableOfMediaLayout() {
        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        when(mockNativeVideoController.hasFinalFrame()).thenReturn(true);
        when(mockNativeVideoController.getFinalFrame()).thenReturn(mockDrawable);
        subject.applyState(VideoState.ENDED);

        verify(mockMediaLayout).setMainImageDrawable(mockNativeVideoController.getFinalFrame());
    }

    @Test
    public void applyState_withVideoStatePause_afterVideoStatePlayingMuted_shouldFirePauseTrackers() {
        final ArrayList<VastTracker> testList = new ArrayList<VastTracker>();
        testList.add(new VastTracker("testUrl", true));

        when(mockVastVideoConfig.getPauseTrackers()).thenReturn(testList);

        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.applyState(VideoState.PLAYING_MUTED);
        subject.applyState(VideoState.PAUSED);

        verify(mockVastVideoConfig).getPauseTrackers();
        verify(mockRequestQueue).add(argThat(isUrl("testUrl")));
    }

    @Test
    public void applyState_withVideoStatePlayingMuted_afterPaused_afterPlaying_shouldFireResumeTrackers() {
        final ArrayList<VastTracker> testList = new ArrayList<VastTracker>();
        testList.add(new VastTracker("testResumeUrl", true));

        when(mockVastVideoConfig.getResumeTrackers()).thenReturn(testList);

        subject.loadAd();
        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);
        subject.prepare(mockRootView);
        subject.render(mockMediaLayout);

        subject.applyState(VideoState.PLAYING_MUTED);
        subject.applyState(VideoState.PAUSED);
        subject.applyState(VideoState.BUFFERING);
        subject.applyState(VideoState.PLAYING_MUTED);

        verify(mockVastVideoConfig).getPauseTrackers();
        verify(mockRequestQueue).add(argThat(isUrl("testResumeUrl")));
    }

    @Test
    public void HeaderVisibilityStrategy_execute_shouldNotifyAdImpressed() throws Exception {
        HeaderVisibilityStrategy headerVisibilityStrategy
                = new HeaderVisibilityStrategy(subject);
        headerVisibilityStrategy.execute();

        verify(mockNativeEventListener).onAdImpressed();
    }

    @Test
    public void PayloadVisibilityStrategy_execute_shouldMakeTrackingRequest() throws Exception {
        PayloadVisibilityStrategy payloadVisibilityStrategy
                = new PayloadVisibilityStrategy(activity, "payloadUrl");
        payloadVisibilityStrategy.execute();

        verify(mockRequestQueue).add(argThat(isUrl("payloadUrl")));
    }
}
