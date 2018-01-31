package com.mopub.nativeads;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Surface;
import android.view.TextureView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayer.ExoPlayerMessage;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.video.MediaCodecVideoRenderer;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.VastTracker;
import com.mopub.mobileads.VastVideoConfig;
import com.mopub.nativeads.NativeVideoController.Listener;
import com.mopub.nativeads.NativeVideoController.MoPubExoPlayerFactory;
import com.mopub.nativeads.NativeVideoController.NativeVideoProgressRunnable;
import com.mopub.nativeads.NativeVideoController.NativeVideoProgressRunnable.ProgressListener;
import com.mopub.nativeads.NativeVideoController.VisibilityTrackingEvent;
import com.mopub.nativeads.VisibilityTracker.VisibilityChecker;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static com.mopub.nativeads.NativeVideoController.STATE_BUFFERING;
import static com.mopub.nativeads.NativeVideoController.STATE_CLEARED;
import static com.mopub.nativeads.NativeVideoController.STATE_ENDED;
import static com.mopub.nativeads.NativeVideoController.STATE_IDLE;
import static com.mopub.nativeads.NativeVideoController.STATE_READY;
import static com.mopub.nativeads.NativeVideoController.createForId;
import static com.mopub.nativeads.NativeVideoController.getForId;
import static com.mopub.nativeads.NativeVideoController.remove;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class NativeVideoControllerTest {

    private NativeVideoController subject;
    private Activity activity;
    private ArrayList<VisibilityTrackingEvent> visibilityTrackingEvents;
    private VastVideoConfig vastVideoConfig;
    private NativeVideoProgressRunnable nativeVideoProgressRunnable;
    private NativeVideoProgressRunnable spyNativeVideoProgressRunnable;
    @Mock private ExoPlayer mockExoPlayer;
    @Mock private NativeVideoProgressRunnable mockNativeVideoProgressRunnable;
    @Mock private TextureView mockTextureView;
    @Mock private SurfaceTexture mockSurfaceTexture;
    @Mock private Listener mockListener;
    @Mock private ProgressListener mockProgressListener;
    @Mock private VastVideoConfig mockVastVideoConfig;
    @Mock private Handler mockHandler;
    @Mock private VisibilityChecker mockVisibilityChecker;
    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private AudioManager mockAudioManager;
    @Mock private MediaCodecVideoRenderer mockVideoRenderer;
    @Mock private MediaCodecAudioRenderer mockAudioRenderer;
    @Mock private TrackSelector mockTrackSelector;
    @Mock private LoadControl mockLoadControl;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();
        visibilityTrackingEvents = new ArrayList<VisibilityTrackingEvent>();

        VisibilityTrackingEvent visibilityTrackingEvent = new VisibilityTrackingEvent();
        visibilityTrackingEvent.minimumPercentageVisible = 10;
        visibilityTrackingEvent.totalRequiredPlayTimeMs = 10;
        visibilityTrackingEvent.strategy = new VisibilityTrackingEvent.OnTrackedStrategy() {
            @Override
            public void execute() {
                TrackingRequest.makeTrackingHttpRequest("trackingUrl1", activity);
            }
        };

        VisibilityTrackingEvent visibilityTrackingEvent2 = new VisibilityTrackingEvent();
        visibilityTrackingEvent2.minimumPercentageVisible = 20;
        visibilityTrackingEvent2.totalRequiredPlayTimeMs = 20;
        visibilityTrackingEvent2.strategy = new VisibilityTrackingEvent.OnTrackedStrategy() {
            @Override
            public void execute() {
                TrackingRequest.makeTrackingHttpRequest("trackingUrl2", activity);
            }
        };

        VisibilityTrackingEvent visibilityTrackingEvent3 = new VisibilityTrackingEvent();
        visibilityTrackingEvent3.minimumPercentageVisible = 30;
        visibilityTrackingEvent3.totalRequiredPlayTimeMs = 30;
        visibilityTrackingEvent3.strategy = new VisibilityTrackingEvent.OnTrackedStrategy() {
            @Override
            public void execute() {
                TrackingRequest.makeTrackingHttpRequest("trackingUrl3", activity);
            }
        };
        visibilityTrackingEvent3.isTracked = true;

        VisibilityTrackingEvent visibilityTrackingEvent4 = new VisibilityTrackingEvent();
        visibilityTrackingEvent4.minimumPercentageVisible = 9;
        visibilityTrackingEvent4.totalRequiredPlayTimeMs = 9;
        visibilityTrackingEvent4.strategy = new VisibilityTrackingEvent.OnTrackedStrategy() {
            @Override
            public void execute() {
                TrackingRequest.makeTrackingHttpRequest("trackingUrl4", activity);
            }
        };

        visibilityTrackingEvents.add(visibilityTrackingEvent);
        visibilityTrackingEvents.add(visibilityTrackingEvent2);
        visibilityTrackingEvents.add(visibilityTrackingEvent3);
        visibilityTrackingEvents.add(visibilityTrackingEvent4);

        vastVideoConfig = new VastVideoConfig();
        vastVideoConfig.setNetworkMediaFileUrl("networkMediaFileUrl");

        when(mockTextureView.getSurfaceTexture()).thenReturn(mockSurfaceTexture);

        subject = createForId(123,
                activity,
                vastVideoConfig,
                mockNativeVideoProgressRunnable,
                new MoPubExoPlayerFactory() {
                    @Override
                    public ExoPlayer newInstance(@NonNull final Renderer[] renderers,
                            @NonNull final TrackSelector trackSelector, @Nullable LoadControl loadControl) {
                        return mockExoPlayer;
                    }
                },
                null,
                mockAudioManager);

        nativeVideoProgressRunnable = new NativeVideoProgressRunnable(activity,
                mockHandler,
                visibilityTrackingEvents,
                mockVisibilityChecker,
                mockVastVideoConfig);
        nativeVideoProgressRunnable.setTextureView(mockTextureView);
        nativeVideoProgressRunnable.setExoPlayer(mockExoPlayer);
        nativeVideoProgressRunnable.setProgressListener(mockProgressListener);
        spyNativeVideoProgressRunnable = Mockito.spy(nativeVideoProgressRunnable);

        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @Test
    public void createForId_shouldAddNativeVideoControllerToMap_shouldReturnNativeVideoController() {
        NativeVideoController nativeVideoController =
                createForId(123, activity, visibilityTrackingEvents, vastVideoConfig, null);
        assertThat(nativeVideoController).isEqualTo(getForId(123));
    }

    @Test
    public void remove_shouldRemoveNativeVideoControllerFromMap() {
        NativeVideoController nativeVideoController =
                createForId(123, activity, visibilityTrackingEvents, vastVideoConfig, null);
        assertThat(nativeVideoController).isEqualTo(getForId(123));
        remove(123);
        assertThat(getForId(123)).isNull();
    }

    @Test
    public void setPlayWhenReady_shouldUpdatePlayWhenReady() {
        subject.prepare(this);
        subject.setPlayWhenReady(true);

        verify(mockExoPlayer).setPlayWhenReady(true);
    }

    @Test
    public void setPlayWhenReady_withPlayBackStateReady_withPlayWhenReadyTrue_shouldSetPlayeWhenReadyOnExoPlayer() {
        subject.prepare(this);
        reset(mockNativeVideoProgressRunnable);

        when(mockExoPlayer.getPlaybackState()).thenReturn(STATE_READY);
        when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
        subject.setPlayWhenReady(true);

        verify(mockExoPlayer).setPlayWhenReady(true);
    }

    @Test
    public void getPlaybackState_withNullExoPlayer_shouldReturnStateCleared() {
        assertThat(subject.getPlaybackState()).isEqualTo(STATE_CLEARED);
    }


    @Test
    public void getPlaybackState_withNonNullExoPlayer_shouldReturnExoPlayerState() {
        subject.prepare(this);

        when(mockExoPlayer.getPlaybackState()).thenReturn(STATE_BUFFERING);
        assertThat(subject.getPlaybackState()).isEqualTo(STATE_BUFFERING);
    }

    @Test
    public void setAudioEnabled_withTrue_shouldSetVolumeOnExoPlayer() {
        subject.prepare(this);
        reset(mockExoPlayer);
        subject.setAudioEnabled(true);

        ArgumentCaptor<ExoPlayerMessage> captor = ArgumentCaptor.forClass(ExoPlayerMessage.class);
        verify(mockExoPlayer).sendMessages(captor.capture());

        ExoPlayerMessage message = captor.getValue();
        assertThat(message.target).isInstanceOf(MediaCodecAudioRenderer.class);
        assertThat(message.messageType).isEqualTo(C.MSG_SET_VOLUME);
        assertThat(message.message).isEqualTo(1.0f);
    }

    @Test
    public void setAudioEnabled_withFalse_shouldDisableVolumeOnExoPlayer() {
        subject.prepare(this);
        // initialize the subject to true so that the next call with false will run
        subject.setAudioEnabled(true);
        reset(mockExoPlayer);

        subject.setAudioEnabled(false);

        ArgumentCaptor<ExoPlayerMessage> captor = ArgumentCaptor.forClass(ExoPlayerMessage.class);
        verify(mockExoPlayer).sendMessages(captor.capture());

        ExoPlayerMessage message = captor.getValue();
        assertThat(message.target).isInstanceOf(MediaCodecAudioRenderer.class);
        assertThat(message.messageType).isEqualTo(C.MSG_SET_VOLUME);
        assertThat(message.message).isEqualTo(0.0f);
    }

    @Test
    public void setAppAudioEnabled_withTrue_shouldRequestAudioFocus() throws Exception {
        subject.setAppAudioEnabled(true);

        verify(mockAudioManager).requestAudioFocus(
                subject, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        verify(mockAudioManager, never()).abandonAudioFocus(subject);
    }

    @Test
    public void setAppAudioEnabled_withFalse_shouldAbandonAudioFocus() throws Exception {
        subject.setAppAudioEnabled(true);
        subject.setAppAudioEnabled(false);

        verify(mockAudioManager).abandonAudioFocus(subject);
    }

    @Test
    public void setAudioVolume_withAudioEnabled_shouldSetExoPlayerVolume() throws Exception {
        subject.prepare(this);
        subject.setAudioEnabled(true);
        reset(mockExoPlayer);

        subject.setAudioVolume(0.3f);

        ArgumentCaptor<ExoPlayerMessage> captor = ArgumentCaptor.forClass(ExoPlayerMessage.class);
        verify(mockExoPlayer).sendMessages(captor.capture());

        ExoPlayerMessage message = captor.getValue();
        assertThat(message.target).isInstanceOf(MediaCodecAudioRenderer.class);
        assertThat(message.messageType).isEqualTo(C.MSG_SET_VOLUME);
        assertThat(message.message).isEqualTo(0.3f);
    }

    @Test
    public void setAudioVolume_withAudioDisabled_shouldDoNothing() throws Exception {
        subject.prepare(this);
        reset(mockExoPlayer);

        subject.setAudioVolume(0.3f);

        verify(mockExoPlayer, never()).sendMessages(any(ExoPlayerMessage.class));
    }

    @Test
    public void setTextureView_shouldSetTextureViewOnVideoProgressRunnable_shouldSetExoSurface() {
        subject.prepare(this);
        reset(mockExoPlayer);

        subject.setTextureView(mockTextureView);

        ArgumentCaptor<ExoPlayerMessage> captor = ArgumentCaptor.forClass(ExoPlayerMessage.class);
        verify(mockExoPlayer).sendMessages(captor.capture());

        ExoPlayerMessage message = captor.getValue();
        assertThat(message.target).isInstanceOf(MediaCodecVideoRenderer.class);
        assertThat(message.messageType).isEqualTo(C.MSG_SET_SURFACE);
        assertThat(message.message).isInstanceOf(Surface.class);
    }

    @Test
    public void prepare_shouldClearExistingPlayer() {
        // setup an initial player
        subject.prepare(this);
        subject.setTextureView(mockTextureView);

        reset(mockExoPlayer);
        reset(mockNativeVideoProgressRunnable);
        // This will clear the previous player
        subject.prepare(this);

        // Ensure the first two calls zero out the surface and disable audio
        ArgumentCaptor<ExoPlayerMessage> captor = ArgumentCaptor.forClass(ExoPlayerMessage.class);
        verify(mockExoPlayer, atLeast(2)).sendMessages(captor.capture());

        List<ExoPlayerMessage> messages = captor.getAllValues();

        assertThat(messages.get(0).target).isInstanceOf(MediaCodecVideoRenderer.class);
        assertThat(messages.get(0).messageType).isEqualTo(C.MSG_SET_SURFACE);
        assertThat(messages.get(0).message).isNull();
        assertThat(messages.get(1).target).isInstanceOf(MediaCodecAudioRenderer.class);
        assertThat(messages.get(1).messageType).isEqualTo(C.MSG_SET_VOLUME);
        assertThat(messages.get(1).message).isEqualTo(0f);

        verify(mockExoPlayer).stop();
        verify(mockExoPlayer).release();
        verify(mockNativeVideoProgressRunnable).stop();
        verify(mockNativeVideoProgressRunnable).setExoPlayer(null);
    }

    @Test
    public void prepare_shouldPreparePlayer() {
        MoPubExoPlayerFactory mockMoPubExoPlayerFactory = mock(MoPubExoPlayerFactory.class);
        when(mockMoPubExoPlayerFactory.newInstance(
                any(Renderer[].class),
                any(TrackSelector.class),
                any(LoadControl.class))
        ).thenReturn(mockExoPlayer);
        subject = createForId(123,
                activity,
                vastVideoConfig,
                mockNativeVideoProgressRunnable,
                mockMoPubExoPlayerFactory,
                null,
                mockAudioManager);
        subject.prepare(this);

        verify(mockMoPubExoPlayerFactory).newInstance(any(Renderer[].class),
                any(TrackSelector.class), any(LoadControl.class));
        verify(mockNativeVideoProgressRunnable).setExoPlayer(mockExoPlayer);
        verify(mockNativeVideoProgressRunnable).startRepeating(50);
        verify(mockExoPlayer).addListener(subject);
        verify(mockExoPlayer).prepare(any(MediaSource.class));

        // set audio and surface
        ArgumentCaptor<ExoPlayerMessage> captor = ArgumentCaptor.forClass(ExoPlayerMessage.class);
        verify(mockExoPlayer, times(2)).sendMessages(captor.capture());

        List<ExoPlayerMessage> messages = captor.getAllValues();
        assertThat(messages.get(0).target).isInstanceOf(MediaCodecAudioRenderer.class);
        assertThat(messages.get(0).messageType).isEqualTo(C.MSG_SET_VOLUME);
        assertThat(messages.get(0).message).isEqualTo(0f);
        assertThat(messages.get(1).target).isInstanceOf(MediaCodecVideoRenderer.class);
        assertThat(messages.get(1).messageType).isEqualTo(C.MSG_SET_SURFACE);
        assertThat(messages.get(1).message).isNull();

        // play when ready
        verify(mockExoPlayer).setPlayWhenReady(false);
    }

    @Test
    public void clear_shouldSetPlayWhenReadyFalse_shouldClearExistingPlayer() {
        // initialize to true so we can set to false
        subject.setPlayWhenReady(true);
        subject.prepare(this);

        reset(mockExoPlayer);
        reset(mockNativeVideoProgressRunnable);
        subject.clear();

        verify(mockExoPlayer).setPlayWhenReady(false);

        // clear exo player
        ArgumentCaptor<ExoPlayerMessage> captor = ArgumentCaptor.forClass(ExoPlayerMessage.class);
        verify(mockExoPlayer).sendMessages(captor.capture());
        ExoPlayerMessage message = captor.getValue();
        assertThat(message.target).isInstanceOf(MediaCodecVideoRenderer.class);
        assertThat(message.messageType).isEqualTo(C.MSG_SET_SURFACE);
        assertThat(message.message).isNull();

        verify(mockExoPlayer).stop();
        verify(mockExoPlayer).release();
        verify(mockNativeVideoProgressRunnable).setExoPlayer(null);
    }

    @Test
    public void release_withMatchingOwner_shouldClearExistingPlayer() {
        subject.prepare(this);

        reset(mockExoPlayer);
        reset(mockNativeVideoProgressRunnable);
        subject.release(this);

        // clear exo player
        ArgumentCaptor<ExoPlayerMessage> captor = ArgumentCaptor.forClass(ExoPlayerMessage.class);
        verify(mockExoPlayer).sendMessages(captor.capture());
        ExoPlayerMessage message = captor.getValue();
        assertThat(message.target).isInstanceOf(MediaCodecVideoRenderer.class);
        assertThat(message.messageType).isEqualTo(C.MSG_SET_SURFACE);
        assertThat(message.message).isNull();
        verify(mockExoPlayer).stop();
        verify(mockExoPlayer).release();
        verify(mockNativeVideoProgressRunnable).setExoPlayer(null);
    }

    @Test
    public void release_withMismatchingOwner_shouldNotClearExistingPlayer() {
        subject.prepare(this);

        reset(mockExoPlayer);
        reset(mockNativeVideoProgressRunnable);
        // can be any object as long as its not 'this'
        subject.release(mockExoPlayer);

        verifyNoMoreInteractions(mockExoPlayer);
        verifyNoMoreInteractions(mockNativeVideoProgressRunnable);
    }

    @Test
    public void onPlayerStateChanged_withStateEnded_shouldSetFinalFrame_shouldRequestProgressRunnableToStop() {
        subject.setTextureView(mockTextureView);
        subject.prepare(this);

        reset(mockExoPlayer);
        reset(mockNativeVideoProgressRunnable);
        Bitmap mockBitmap = mock(Bitmap.class);
        when(mockTextureView.getBitmap()).thenReturn(mockBitmap);
        subject.onPlayerStateChanged(true, STATE_ENDED);

        assertThat(subject.hasFinalFrame()).isTrue();
        assertThat(subject.getFinalFrame()).isNotNull();
        assertThat(((BitmapDrawable) subject.getFinalFrame()).getBitmap()).isEqualTo(mockBitmap);
        verify(mockNativeVideoProgressRunnable).requestStop();
    }

    @Test
    public void onPlayerStateChanged_withNonNullListener_shouldNotifyListener() {
        subject.prepare(this);
        subject.setListener(mockListener);

        subject.onPlayerStateChanged(true, STATE_IDLE);

        verify(mockListener).onStateChanged(true, STATE_IDLE);
    }

    @Test
    public void seekTo_shouldCallExoPlayerSeekTo_shouldCallProgressRunnableSeekTo() {
        subject.prepare(this);
        subject.seekTo(321);

        verify(mockExoPlayer).seekTo(321);
        verify(mockNativeVideoProgressRunnable).seekTo(321);
    }

    @Test
    public void getCurrentPosition_shouldReturnProgressRunnableCurrentPosition() {
        when(mockNativeVideoProgressRunnable.getCurrentPosition()).thenReturn(456L);
        assertThat(subject.getCurrentPosition()).isEqualTo(456L);
        verify(mockNativeVideoProgressRunnable).getCurrentPosition();
    }

    @Test
    public void getDuration_shouldReturnProgressRunnableDuration() {
        when(mockNativeVideoProgressRunnable.getDuration()).thenReturn(234L);
        assertThat(subject.getDuration()).isEqualTo(234L);
        verify(mockNativeVideoProgressRunnable).getDuration();
    }

    @Test
    public void onPlayerError_shouldNotifyListener_shouldRequestProgressRunnableToStop() {
        ExoPlaybackException exoPlaybackException = ExoPlaybackException.createForSource(new IOException(""));
        subject.setListener(mockListener);
        subject.onPlayerError(exoPlaybackException);

        verify(mockListener).onError(exoPlaybackException);
        verify(mockNativeVideoProgressRunnable).requestStop();
    }

    @Test
    public void handleCtaClick_shouldInvokeVastVideoConfigHandleClick() {
        subject = createForId(123,
                activity,
                mockVastVideoConfig,
                mockNativeVideoProgressRunnable,
                new MoPubExoPlayerFactory() {
                    @Override
                    public ExoPlayer newInstance(Renderer[] renderers, TrackSelector trackSelector,
                            LoadControl loadControl) {
                        return mockExoPlayer;
                    }
                },
                null,
                mockAudioManager);

        subject.handleCtaClick(activity);

        verify(mockVastVideoConfig).handleClickWithoutResult(activity, 0);
    }


    @Test
    public void NativeVideoProgressRunnable_doWork_shouldTrackEventsWithMinimumPercentVisibleWithMinimumTimePlayed() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(10L);
        when(mockExoPlayer.getDuration()).thenReturn(25L);
        when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);
        when(mockVisibilityChecker.isVisible(mockTextureView, mockTextureView,
                10, null)).thenReturn(true);
        when(mockVisibilityChecker.isVisible(mockTextureView, mockTextureView,
                20, null)).thenReturn(false);

        nativeVideoProgressRunnable.setUpdateIntervalMillis(10);
        nativeVideoProgressRunnable.doWork();

        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl1")));
        assertThat(visibilityTrackingEvents.get(0).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isFalse();

        // should not track same request twice
        reset(mockRequestQueue);
        nativeVideoProgressRunnable.doWork();

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void NativeVideoProgressRunnable_doWork_shouldUpdateProgressListenerWithPercentagePlayed() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(10L);
        when(mockExoPlayer.getDuration()).thenReturn(25L);
        when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);

        nativeVideoProgressRunnable.doWork();

        verify(mockProgressListener).updateProgress((int) (((float) 10L / 25L) * 1000));
    }

    @Test
    public void NativeVideoProgressRunnable_doWork_shouldFireUntriggeredTrackersFromVastVideoConfig() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(10L);
        when(mockExoPlayer.getDuration()).thenReturn(25L);
        when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);

        VastTracker vastTracker = new VastTracker("vastTrackingUrl");
        List<VastTracker> vastTrackers = new ArrayList<VastTracker>();
        vastTrackers.add(vastTracker);
        when(mockVastVideoConfig.getUntriggeredTrackersBefore(10, 25)).thenReturn(vastTrackers);

        nativeVideoProgressRunnable.doWork();

        verify(mockRequestQueue).add(argThat(isUrl("vastTrackingUrl")));
        assertThat(vastTracker.isTracked()).isTrue();

        reset(mockRequestQueue);
        nativeVideoProgressRunnable.doWork();

        verifyNoMoreInteractions(mockRequestQueue);
    }

    @Test
    public void NativeVideoProgressRunnable_doWork_withNullExoPlayer_shouldReturnFast() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(10L);
        when(mockExoPlayer.getDuration()).thenReturn(25L);
        when(mockExoPlayer.getPlayWhenReady()).thenReturn(true);

        VastTracker vastTracker = new VastTracker("vastTrackingUrl");
        List<VastTracker> vastTrackers = new ArrayList<VastTracker>();
        vastTrackers.add(vastTracker);
        when(mockVastVideoConfig.getUntriggeredTrackersBefore(10, 25)).thenReturn(vastTrackers);
        nativeVideoProgressRunnable.setUpdateIntervalMillis(10);

        nativeVideoProgressRunnable.setExoPlayer(null);
        nativeVideoProgressRunnable.doWork();

        assertThat(visibilityTrackingEvents.get(0).isTracked).isFalse();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isFalse();
        assertThat(vastTracker.isTracked()).isFalse();
        verifyNoMoreInteractions(mockVisibilityChecker);
        verifyNoMoreInteractions(mockVastVideoConfig);
        verifyNoMoreInteractions(mockRequestQueue);
        verifyNoMoreInteractions(mockProgressListener);
    }

    @Test
    public void NativeVideoProgressRunnable_doWork_withExoPlayerGetPlayWhenReadyFalse_shouldReturnFast() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(10L);
        when(mockExoPlayer.getDuration()).thenReturn(25L);
        when(mockExoPlayer.getPlayWhenReady()).thenReturn(false);

        VastTracker vastTracker = new VastTracker("vastTrackingUrl");
        List<VastTracker> vastTrackers = new ArrayList<VastTracker>();
        vastTrackers.add(vastTracker);
        when(mockVastVideoConfig.getUntriggeredTrackersBefore(10, 25)).thenReturn(vastTrackers);
        nativeVideoProgressRunnable.setUpdateIntervalMillis(10);

        nativeVideoProgressRunnable.doWork();

        assertThat(visibilityTrackingEvents.get(0).isTracked).isFalse();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isFalse();
        assertThat(vastTracker.isTracked()).isFalse();
        verifyNoMoreInteractions(mockVisibilityChecker);
        verifyNoMoreInteractions(mockVastVideoConfig);
        verifyNoMoreInteractions(mockRequestQueue);
        verifyNoMoreInteractions(mockProgressListener);
    }

    @Test
    public void NativeVideoProgressRunnable_checkImpressionTrackers_withForceTriggerFalse_shouldOnlyTriggerNotTrackedEvents_shouldNotStopRunnable() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
        when(mockExoPlayer.getDuration()).thenReturn(50L);
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView), anyInt(),
                Matchers.isNull(Integer.class)))
                .thenReturn(true);
        spyNativeVideoProgressRunnable.setUpdateIntervalMillis(50);

        spyNativeVideoProgressRunnable.checkImpressionTrackers(/* forceTrigger = */ false);

        assertThat(visibilityTrackingEvents.get(0).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(2).isTracked).isTrue();
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl1")));
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl2")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl3")));
        verify(spyNativeVideoProgressRunnable,never()).stop();
    }

    @Test
    public void NativeVideoProgressRunnable_checkImpressionTrackers_withForceTriggerFalse_withStopRequested_shouldOnlyTriggerNotTrackedEvents_shouldStopRunnable() {
        // Enough time has passed for all impressions to trigger organically
        when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
        when(mockExoPlayer.getDuration()).thenReturn(50L);
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView), anyInt(),
                Matchers.isNull(Integer.class)))
                .thenReturn(true);
        spyNativeVideoProgressRunnable.setUpdateIntervalMillis(50);
        spyNativeVideoProgressRunnable.requestStop();

        spyNativeVideoProgressRunnable.checkImpressionTrackers(/* forceTrigger = */ false);

        assertThat(visibilityTrackingEvents.get(0).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(2).isTracked).isTrue();
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl1")));
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl2")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl3")));
        verify(spyNativeVideoProgressRunnable).stop();
    }

    @Test
    public void NativeVideoProgressRunnable_checkImpressionTrackers_withForceTriggerTrue_shouldOnlyTriggerNotTrackedEvents_shouldNotStopRunnable() {
        // Not enough time has passed for impressions to trigger organically, but all of them will
        // be triggered because forceTrigger is true
        when(mockExoPlayer.getCurrentPosition()).thenReturn(5L);
        when(mockExoPlayer.getDuration()).thenReturn(50L);
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView), anyInt(),
                Matchers.isNull(Integer.class)))
                .thenReturn(true);
        spyNativeVideoProgressRunnable.setUpdateIntervalMillis(50);

        spyNativeVideoProgressRunnable.checkImpressionTrackers(/* forceTrigger = */ true);

        assertThat(visibilityTrackingEvents.get(0).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(2).isTracked).isTrue();
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl1")));
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl2")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl3")));
        verify(spyNativeVideoProgressRunnable, never()).stop();
    }

    @Test
    public void NativeVideoProgressRunnable_checkImpressionTrackers_withForceTriggerTrue_withStopRequested_shouldOnlyTriggerNotTrackedEvents_shouldStopRunnable() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(50L);
        when(mockExoPlayer.getDuration()).thenReturn(50L);
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView), anyInt(),
                Matchers.isNull(Integer.class)))
                .thenReturn(true);
        spyNativeVideoProgressRunnable.setUpdateIntervalMillis(50);
        spyNativeVideoProgressRunnable.requestStop();

        spyNativeVideoProgressRunnable.checkImpressionTrackers(/* forceTrigger = */ true);

        assertThat(visibilityTrackingEvents.get(0).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(2).isTracked).isTrue();
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl1")));
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl2")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl3")));
        verify(spyNativeVideoProgressRunnable).stop();
    }

    @Test
    public void NativeVideoProgressRunnable_checkImpressionTrackers_withForceTriggerFalse_withStopRequested_shouldOnlyTriggerVisibleAndPlayedEvents_shouldNotStopRunnable() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(10L);
        when(mockExoPlayer.getDuration()).thenReturn(10L);

        // visible: checks whether the minimum percentage visible is met
        // played: checks whether the required playtime is met
        // track: whether the impression should be organically triggered

        // trackingUrl1: visible & played = track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(10), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl2: visible & !played = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(20), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl3: already tracked = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(30), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl4: !visible & played = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(9), Matchers.isNull(Integer.class)))
                .thenReturn(false);
        spyNativeVideoProgressRunnable.setUpdateIntervalMillis(10);
        spyNativeVideoProgressRunnable.requestStop();

        spyNativeVideoProgressRunnable.checkImpressionTrackers(/* forceTrigger = */ false);

        assertThat(visibilityTrackingEvents.get(0).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isFalse();
        assertThat(visibilityTrackingEvents.get(2).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(3).isTracked).isFalse();
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl1")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl2")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl3")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl4")));
        verify(spyNativeVideoProgressRunnable, never()).stop();
    }

    @Test
    public void NativeVideoProgressRunnable_checkImpressionTrackers_withForceTriggerTrue_shouldTriggerAllUntrackedEvents_shouldNotStopRunnable() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(10L);
        when(mockExoPlayer.getDuration()).thenReturn(10L);

        // visible: checks whether the minimum percentage visible is met
        // played: checks whether the required playtime is met
        // track: whether the impression should be organically triggered

        // trackingUrl1: visible & played = track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(10), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl2: visible & !played = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(20), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl3: already tracked = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(30), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl4: !visible & played = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(9), Matchers.isNull(Integer.class)))
                .thenReturn(false);
        spyNativeVideoProgressRunnable.setUpdateIntervalMillis(10);

        spyNativeVideoProgressRunnable.checkImpressionTrackers(/* forceTrigger = */ true);

        // Because forceTrigger was true, tracking of all impressions is forced
        assertThat(visibilityTrackingEvents.get(0).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(2).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(3).isTracked).isTrue();
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl1")));
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl2")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl3")));
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl4")));
        verify(spyNativeVideoProgressRunnable, never()).stop();
    }

    @Test
    public void NativeVideoProgressRunnable_checkImpressionTrackers_withForceTriggerTrue_withStopRequested_shouldTriggerAllUntrackedEvents_shouldStopRunnable() {
        when(mockExoPlayer.getCurrentPosition()).thenReturn(10L);
        when(mockExoPlayer.getDuration()).thenReturn(10L);

        // visible: checks whether the minimum percentage visible is met
        // played: checks whether the required playtime is met
        // track: whether the impression should be organically triggered

        // trackingUrl1: visible & played = track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(10), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl2: visible & !played = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(20), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl3: already tracked = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(30), Matchers.isNull(Integer.class)))
                .thenReturn(true);
        // trackingUrl4: !visible & played = !track
        when(mockVisibilityChecker.isVisible(eq(mockTextureView), eq(mockTextureView),
                eq(9), Matchers.isNull(Integer.class)))
                .thenReturn(false);
        spyNativeVideoProgressRunnable.setUpdateIntervalMillis(10);
        spyNativeVideoProgressRunnable.requestStop();

        spyNativeVideoProgressRunnable.checkImpressionTrackers(/* forceTrigger = */ true);

        // Because forceTrigger was true, tracking of all impressions is forced
        assertThat(visibilityTrackingEvents.get(0).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(1).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(2).isTracked).isTrue();
        assertThat(visibilityTrackingEvents.get(3).isTracked).isTrue();
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl1")));
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl2")));
        verify(mockRequestQueue, never()).add(argThat(isUrl("trackingUrl3")));
        verify(mockRequestQueue).add(argThat(isUrl("trackingUrl4")));
        verify(spyNativeVideoProgressRunnable).stop();
    }
}
