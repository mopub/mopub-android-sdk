package com.mopub.nativeads;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.mopub.common.Constants;
import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BaseVideoViewController.BaseVideoViewControllerListener;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.VastVideoConfig;
import com.mopub.mobileads.resource.CtaButtonDrawable;
import com.mopub.nativeads.MoPubCustomEventVideoNative.MoPubVideoNativeAd;
import com.mopub.nativeads.NativeFullScreenVideoView.Mode;
import com.mopub.nativeads.NativeVideoController.NativeVideoProgressRunnable.ProgressListener;
import com.mopub.nativeads.NativeVideoViewController.VideoState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class NativeVideoViewControllerTest {

    private NativeVideoViewController subject;
    private Activity activity;
    private Bundle intentExtras;

    @Mock private BaseVideoViewControllerListener mockBaseVideoViewControllerListener;
    @Mock private NativeFullScreenVideoView mockFullScreenVideoView;
    @Mock private NativeVideoController mockVideoController;
    @Mock private VastVideoConfig mockVastVideoConfig;
    @Mock private TextureView mockTextureView;
    @Mock private Bitmap mockBitmap;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        intentExtras = new Bundle();
        intentExtras.putLong(Constants.NATIVE_VIDEO_ID, 123);
        NativeVideoController.setForId(123, mockVideoController);

        when(mockVastVideoConfig.getCustomCtaText()).thenReturn("Learn More");
        when(mockFullScreenVideoView.getTextureView()).thenReturn(mockTextureView);
        when(mockTextureView.getBitmap()).thenReturn(mockBitmap);
        intentExtras.putSerializable(Constants.NATIVE_VAST_VIDEO_CONFIG, mockVastVideoConfig);

        subject = new NativeVideoViewController(activity, intentExtras, null,
                mockBaseVideoViewControllerListener, mockFullScreenVideoView);
    }

    @After
    public void tearDown() {
        NativeVideoController.remove(123);
    }

    @Test
    public void constructor_shouldSetCtaText() {
        subject = new NativeVideoViewController(activity, intentExtras, null,
                mockBaseVideoViewControllerListener);
        CtaButtonDrawable ctaButtonDrawable =
                (CtaButtonDrawable) subject.getNativeFullScreenVideoView().getCtaButton()
                        .getDrawable();

        assertThat(ctaButtonDrawable.getCtaText()).isEqualTo("Learn More");
    }

    @Test(expected = NullPointerException.class)
    public void constructor_withNullNativeVideoController_shouldThrowNPE() {
        NativeVideoController.remove(123);
        subject = new NativeVideoViewController(activity, intentExtras, null,
                mockBaseVideoViewControllerListener, mockFullScreenVideoView);
    }

    @Test(expected = NullPointerException.class)
    public void onCreate_withNullNativeVideoController_shouldThrowNPE() {
        intentExtras.remove(Constants.NATIVE_VAST_VIDEO_CONFIG);
        subject = new NativeVideoViewController(activity, intentExtras, null,
                mockBaseVideoViewControllerListener, mockFullScreenVideoView);
    }

    @Test
    public void onCreate_shouldSetupVideoView() {
        subject.onCreate();

        verify(mockFullScreenVideoView).setSurfaceTextureListener(subject);
        verify(mockFullScreenVideoView).setMode(Mode.LOADING);
        verify(mockFullScreenVideoView).setPlayControlClickListener(any(View.OnClickListener.class));
        verify(mockFullScreenVideoView).setCloseControlListener(any(View.OnClickListener.class));
        verify(mockFullScreenVideoView).setCtaClickListener(any(View.OnClickListener.class));
        verify(mockFullScreenVideoView).setPrivacyInformationClickListener(
                any(View.OnClickListener.class));
        verify(mockFullScreenVideoView).setLayoutParams(any(LayoutParams.class));
        verify(mockBaseVideoViewControllerListener).onSetContentView(mockFullScreenVideoView);
        verify(mockBaseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void onCreate_shouldSetupNativeVideoController() {
        subject.onCreate();

        verify(mockVideoController).setProgressListener(any(ProgressListener.class));
    }

    @Test
    public void NativeFullScreenVideoView_playControlClickListener_withVideoEnded_shouldResetFullScreenVideoProgress_shouldSeekTo0_shouldApplyPlaying() {
        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);

        subject.onCreate();
        subject.applyState(VideoState.ENDED);

        verify(mockFullScreenVideoView).setPlayControlClickListener(captor.capture());
        captor.getValue().onClick(null);

        verify(mockFullScreenVideoView).resetProgress();
        verify(mockVideoController).seekTo(0);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.PLAYING);
        verify(mockBaseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void NativeFullScreenVideoView_closeControlClickListener_shouldSetStatePaused_shouldNotDisableAppAudio_shouldFinishActivity() {
        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);

        subject.onCreate();

        verify(mockFullScreenVideoView).setCloseControlListener(captor.capture());
        captor.getValue().onClick(null);

        assertThat(subject.getVideoState()).isEqualTo(VideoState.PAUSED);
        verify(mockVideoController, never()).setAppAudioEnabled(false);
        verify(mockBaseVideoViewControllerListener).onFinish();
    }

    @Test
    public void NativeFullScreenVideoView_ctaClickListener_shouldSetPlayWhenReadyToFalse_shouldHandleCtaClick() {
        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);
        subject.onCreate();

        verify(mockFullScreenVideoView).setCtaClickListener(captor.capture());
        captor.getValue().onClick(null);

        verify(mockVideoController).setPlayWhenReady(false);
        verify(mockVideoController).handleCtaClick(activity);
        verify(mockBaseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void NativeFullScreenVideoView_privacyInformationIconClickListener_shouldSetPlayWhenReadyToFalse_shouldOpenInAppBrowser() {
        ArgumentCaptor<View.OnClickListener> captor = ArgumentCaptor.forClass(View.OnClickListener.class);

        subject.onCreate();

        verify(mockFullScreenVideoView).setPrivacyInformationClickListener(captor.capture());
        captor.getValue().onClick(null);

        verify(mockVideoController).setPlayWhenReady(false);

        final Intent startedActivity = ShadowApplication.getInstance().peekNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName())
                .isEqualTo(MoPubBrowser.class.getName());
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY))
                .isEqualTo(MoPubVideoNativeAd.PRIVACY_INFORMATION_CLICKTHROUGH_URL);
        verify(mockBaseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void NativeVideoController_progressListener_shouldUpdateFullScreenVideoProgress() {
        ArgumentCaptor<ProgressListener> captor = ArgumentCaptor.forClass(ProgressListener.class);

        subject.onCreate();

        verify(mockVideoController).setProgressListener(captor.capture());
        captor.getValue().updateProgress(10);

        verify(mockFullScreenVideoView).updateProgress(10);
        verify(mockBaseVideoViewControllerListener, never()).onFinish();
    }

    @Test
    public void onResume_shouldPrepareNativeVideoController_shouldSetListeners() {
        subject.onResume();

        verify(mockVideoController).prepare(subject);
        verify(mockVideoController).setListener(subject);
        verify(mockVideoController).setOnAudioFocusChangeListener(subject);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void onConfigurationChanged_shouldSetOrientationOfFullScreenVideoView() {
        Configuration configuration = new Configuration();
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
        subject.onConfigurationChanged(configuration);

        verify(mockFullScreenVideoView).setOrientation(Configuration.ORIENTATION_LANDSCAPE);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void onBackPressed_shouldApplyStatePaused_shouldNotDisableAppAudio() throws Exception {
        subject.onBackPressed();

        assertThat(subject.getVideoState()).isEqualTo(VideoState.PAUSED);
        verify(mockVideoController, never()).setAppAudioEnabled(false);
    }

    @Test
    public void onSurfaceTexutureAvailable_shouldSetTextureView_shouldSeekToLastPosition_shouldSetPlayWhenReadyTrue() {
        TextureView textureView = mock(TextureView.class);
        when(mockFullScreenVideoView.getTextureView()).thenReturn(textureView);
        when(mockVideoController.getCurrentPosition()).thenReturn(321L);

        subject.onSurfaceTextureAvailable(null, 0, 0);

        verify(mockVideoController).setTextureView(textureView);
        verify(mockVideoController).seekTo(321L);
        verify(mockVideoController).setPlayWhenReady(true);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void onSurfaceTextureDestroyed_shouldReleaseVideoController_shouldApplyStatePaused() {
        subject.onSurfaceTextureDestroyed(null);

        verify(mockVideoController).release(subject);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.PAUSED);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void onStateChanged_shouldAppropriatelySetVideoState() {
        subject.onStateChanged(true, NativeVideoController.STATE_IDLE);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.LOADING);

        subject.onStateChanged(true, NativeVideoController.STATE_BUFFERING);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.BUFFERING);

        subject.onStateChanged(true, NativeVideoController.STATE_READY);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.PLAYING);

        subject.onStateChanged(true, NativeVideoController.STATE_ENDED);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.ENDED);

        subject.onStateChanged(true, NativeVideoController.STATE_CLEARED);
        assertThat(subject.getVideoState()).isEqualTo(VideoState.ENDED);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void onError_shouldAppropriatelySetVideoState() {
        subject.onError(new Exception());
        assertThat(subject.getVideoState()).isEqualTo(VideoState.FAILED_LOAD);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void onAudioFocusChange_withFocusChangeAudioFocusLossOrAudioFocusLossTransient_shouldPauseVideo() throws Exception {
        subject.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS);

        assertThat(subject.getVideoState()).isEqualTo(VideoState.PAUSED);
    }

    @Test
    public void onAudioFocusChange_withFocusChangeAudioFocusLossTransientCanDuck_shouldLowerVolume() throws Exception {
        subject.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK);

        verify(mockVideoController).setAudioVolume(0.3f);
    }

    @Test
    public void onAudioFocusChange_withFocusChangeAudioFocusGain_shouldRaiseVolume() throws Exception {
        subject.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN);

        verify(mockVideoController).setAudioVolume(1.0f);
    }

    @Test
    public void applyState_withVideoStateFailedLoad_shouldSetPlayWhenReadyFalse_shouldSetAudioEnabledFalse_shouldSetAppAudioEnabledFalse_shouldSetModeLoading_shouldHandleError() {
        subject.applyState(VideoState.FAILED_LOAD);
        verify(mockVideoController).setPlayWhenReady(false);
        verify(mockVideoController).setAudioEnabled(false);
        verify(mockVideoController).setAppAudioEnabled(false);
        verify(mockFullScreenVideoView).setMode(Mode.LOADING);
        verify(mockVastVideoConfig).handleError(activity, null, 0);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void applyState_withVideoStateLoadingOrBuffering_shouldSetPlayWhenReadyTrue_shouldSetModeLoading() {
        subject.applyState(VideoState.LOADING);
        verify(mockVideoController).setPlayWhenReady(true);
        verify(mockFullScreenVideoView).setMode(Mode.LOADING);

        reset(mockVideoController);
        reset(mockFullScreenVideoView);

        subject.applyState(VideoState.BUFFERING);
        verify(mockVideoController).setPlayWhenReady(true);
        verify(mockFullScreenVideoView).setMode(Mode.LOADING);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void applyState_withVideoStatePlaying_shouldSetPlayWhenReadyTrue_shouldSetAudioEnabled_shouldSetAppAudioEnabled_shouldSetModePlaying() {
        subject.applyState(VideoState.PLAYING);
        verify(mockVideoController).setPlayWhenReady(true);
        verify(mockVideoController).setAudioEnabled(true);
        verify(mockVideoController).setAppAudioEnabled(true);
        verify(mockFullScreenVideoView).setMode(Mode.PLAYING);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void applyState_withVideoStatePaused_shouldSetAppAudioEnabledFalse_shouldSetPlayWhenReadyFalse_shouldSetModePaused() {
        subject.applyState(VideoState.PAUSED);
        verify(mockVideoController).setAppAudioEnabled(false);
        verify(mockVideoController).setPlayWhenReady(false);
        verify(mockFullScreenVideoView).setMode(Mode.PAUSED);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }

    @Test
    public void applyState_withVideoStateEnded_shuoldSetAppAudioEnabledFalse_shouldUpdateProgress_shouldSetModeFinished() {
        subject.applyState(VideoState.ENDED);
        verify(mockVideoController).setAppAudioEnabled(false);
        verify(mockFullScreenVideoView).updateProgress(1000);
        verify(mockFullScreenVideoView).setMode(Mode.FINISHED);
        verify(mockVastVideoConfig).handleComplete(activity, 0);
        verifyNoMoreInteractions(mockBaseVideoViewControllerListener);
    }
}
