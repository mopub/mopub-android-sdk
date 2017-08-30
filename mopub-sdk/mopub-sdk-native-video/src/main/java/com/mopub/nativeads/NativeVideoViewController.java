package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.VideoView;

import com.mopub.common.Constants;
import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.BaseVideoViewController;
import com.mopub.mobileads.VastVideoConfig;
import com.mopub.nativeads.MoPubCustomEventVideoNative.MoPubVideoNativeAd;
import com.mopub.nativeads.NativeFullScreenVideoView.Mode;
import com.mopub.nativeads.NativeVideoController.NativeVideoProgressRunnable;

public class NativeVideoViewController extends BaseVideoViewController implements TextureView
        .SurfaceTextureListener, NativeVideoController.Listener,
        AudioManager.OnAudioFocusChangeListener {

    enum VideoState { NONE, LOADING, BUFFERING, PAUSED, PLAYING, ENDED, FAILED_LOAD }

    @NonNull private VideoState mVideoState;
    @NonNull private VastVideoConfig mVastVideoConfig;
    @NonNull private final NativeFullScreenVideoView mFullScreenVideoView;
    @NonNull private final NativeVideoController mNativeVideoController;
    @Nullable private Bitmap mCachedVideoFrame;
    /*
     * This state variable prevents the view from flickering when NativeVideoController state
     * changes but the video has already finished playing.
     */
    private boolean mEnded;
    private boolean mError;
    private int mLatestVideoControllerState;

    public NativeVideoViewController(@NonNull final Context context,
            @NonNull final Bundle intentExtras,
            @NonNull final Bundle savedInstanceState,
            @NonNull final BaseVideoViewControllerListener baseVideoViewControllerListener) {
        this(context, intentExtras, savedInstanceState, baseVideoViewControllerListener,
                new NativeFullScreenVideoView(context,
                        context.getResources().getConfiguration().orientation,
                        ((VastVideoConfig) intentExtras.get(Constants.NATIVE_VAST_VIDEO_CONFIG))
                                .getCustomCtaText()));
    }

    @VisibleForTesting
    NativeVideoViewController(@NonNull final Context context,
            @NonNull final Bundle intentExtras,
            @NonNull final Bundle savedInstanceState,
            @NonNull final BaseVideoViewControllerListener baseVideoViewControllerListener,
            @NonNull final NativeFullScreenVideoView fullScreenVideoView) {
        super(context, null, baseVideoViewControllerListener);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(intentExtras);
        Preconditions.checkNotNull(baseVideoViewControllerListener);
        Preconditions.checkNotNull(fullScreenVideoView);

        mVideoState = VideoState.NONE;
        mVastVideoConfig = ((VastVideoConfig) intentExtras.get(Constants.NATIVE_VAST_VIDEO_CONFIG));
        mFullScreenVideoView = fullScreenVideoView;
        final long videoId = (long) intentExtras.get(Constants.NATIVE_VIDEO_ID);
        mNativeVideoController = NativeVideoController.getForId(videoId);

        // Variables being checked below may be null but if they are it indicates
        // a serious error in setting up this activity and we should detect it
        // as soon as possible
        Preconditions.checkNotNull(mVastVideoConfig);
        Preconditions.checkNotNull(mNativeVideoController);
    }

    @Override
    protected VideoView getVideoView() {
        return null;
    }

    @Override
    protected void onCreate() {
        mFullScreenVideoView.setSurfaceTextureListener(this);
        mFullScreenVideoView.setMode(Mode.LOADING);
        mFullScreenVideoView.setPlayControlClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEnded) {
                    mEnded = false;
                    mFullScreenVideoView.resetProgress();
                    mNativeVideoController.seekTo(0);
                }
                applyState(VideoState.PLAYING);
            }
        });

        mFullScreenVideoView.setCloseControlListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyState(VideoState.PAUSED, true);
                getBaseVideoViewControllerListener().onFinish();
            }
        });

        mFullScreenVideoView.setCtaClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNativeVideoController.setPlayWhenReady(false);
                mCachedVideoFrame = mFullScreenVideoView.getTextureView().getBitmap();
                mNativeVideoController.handleCtaClick((Activity) getContext());
            }
        });

        mFullScreenVideoView.setPrivacyInformationClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNativeVideoController.setPlayWhenReady(false);
                mCachedVideoFrame = mFullScreenVideoView.getTextureView().getBitmap();
                new UrlHandler.Builder().withSupportedUrlActions(UrlAction.OPEN_IN_APP_BROWSER)
                        .build().handleUrl(getContext(),
                        MoPubVideoNativeAd.PRIVACY_INFORMATION_CLICKTHROUGH_URL);
            }
        });

        final LayoutParams adViewLayout =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        mFullScreenVideoView.setLayoutParams(adViewLayout);
        getBaseVideoViewControllerListener().onSetContentView(mFullScreenVideoView);

        mNativeVideoController.setProgressListener(new NativeVideoProgressRunnable
                .ProgressListener() {

            @Override
            public void updateProgress(final int progressTenthPercent) {
                mFullScreenVideoView.updateProgress(progressTenthPercent);
            }
        });
    }

    @Override
    protected void onResume() {
        if (mCachedVideoFrame != null) {
            mFullScreenVideoView.setCachedVideoFrame(mCachedVideoFrame);
        }
        mNativeVideoController.prepare(this);
        mNativeVideoController.setListener(this);
        mNativeVideoController.setOnAudioFocusChangeListener(this);
    }

    @Override
    protected void onPause() { }

    @Override
    protected void onDestroy() { }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) { }

    @Override
    protected void onConfigurationChanged(final Configuration configuration) {
        mFullScreenVideoView.setOrientation(configuration.orientation);
    }

    @Override
    protected void onBackPressed() {
        applyState(VideoState.PAUSED, true);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mNativeVideoController.setTextureView(mFullScreenVideoView.getTextureView());

        if (!mEnded) {
            mNativeVideoController.seekTo(mNativeVideoController.getCurrentPosition());
        }
        mNativeVideoController.setPlayWhenReady(!mEnded);
        long currentPosition = mNativeVideoController.getCurrentPosition();
        long duration = mNativeVideoController.getDuration();
        long remaining = duration - currentPosition;
        if (remaining < NativeVideoController.RESUME_FINISHED_THRESHOLD) {
            mEnded = true;
            maybeChangeState();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }

    @Override
    public boolean onSurfaceTextureDestroyed(final SurfaceTexture surface) {
        mNativeVideoController.release(this);
        applyState(VideoState.PAUSED);
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) { }

    @Override
    public void onStateChanged(final boolean playWhenReady, final int playbackState) {
        mLatestVideoControllerState = playbackState;
        maybeChangeState();
    }

    @Override
    public void onError(final Exception e) {
        MoPubLog.w("Error playing back video.", e);
        mError = true;
        maybeChangeState();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            // Pause Video
            applyState(VideoState.PAUSED);
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // Lower the volume
            mNativeVideoController.setAudioVolume(0.3f);
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // Resume playback
            mNativeVideoController.setAudioVolume(1.0f);
            maybeChangeState();
        }
    }

    private void maybeChangeState() {
        VideoState newState = mVideoState;

        if (mError) {
            newState = VideoState.FAILED_LOAD;
        } else if (mEnded) {
            newState = VideoState.ENDED;
        } else {
            if (mLatestVideoControllerState == NativeVideoController.STATE_IDLE) {
                newState = VideoState.LOADING;
            } else if (mLatestVideoControllerState == NativeVideoController.STATE_BUFFERING) {
                newState = VideoState.BUFFERING;
            } else if (mLatestVideoControllerState == NativeVideoController.STATE_READY) {
                newState = VideoState.PLAYING;
            } else if (mLatestVideoControllerState == NativeVideoController.STATE_ENDED
                    || mLatestVideoControllerState == NativeVideoController.STATE_CLEARED){
                newState = VideoState.ENDED;
            }
        }

        applyState(newState);
    }

    @VisibleForTesting
    void applyState(@NonNull final VideoState videoState) {
        applyState(videoState, false);
    }

    @VisibleForTesting
    void applyState(@NonNull final VideoState videoState, boolean transitionToInline) {
        Preconditions.checkNotNull(videoState);
        if (mVideoState == videoState) {
            return;
        }

        switch (videoState) {
            case FAILED_LOAD:
                // Spin endlessly for an error state
                mNativeVideoController.setPlayWhenReady(false);
                mNativeVideoController.setAudioEnabled(false);
                mNativeVideoController.setAppAudioEnabled(false);
                mFullScreenVideoView.setMode(Mode.LOADING);
                mVastVideoConfig.handleError(getContext(), null, 0);
                break;
            case LOADING:
            case BUFFERING:
                mNativeVideoController.setPlayWhenReady(true);
                mFullScreenVideoView.setMode(Mode.LOADING);
                break;
            case PLAYING:
                mNativeVideoController.setPlayWhenReady(true);
                mNativeVideoController.setAudioEnabled(true);
                mNativeVideoController.setAppAudioEnabled(true);
                mFullScreenVideoView.setMode(Mode.PLAYING);
                break;
            case PAUSED:
                if (!transitionToInline) {
                    mNativeVideoController.setAppAudioEnabled(false);
                }
                mNativeVideoController.setPlayWhenReady(false);
                mFullScreenVideoView.setMode(Mode.PAUSED);
                break;
            case ENDED:
                mEnded = true;
                mNativeVideoController.setAppAudioEnabled(false);
                mFullScreenVideoView.updateProgress(1000);
                mFullScreenVideoView.setMode(Mode.FINISHED);
                mVastVideoConfig.handleComplete(getContext(), 0);
                break;
            default:
                // nothing
        }

        mVideoState = videoState;
    }

    @Deprecated
    @VisibleForTesting
    NativeFullScreenVideoView getNativeFullScreenVideoView() {
        return mFullScreenVideoView;
    }

    @Deprecated
    @VisibleForTesting
    VideoState getVideoState() {
        return mVideoState;
    }
}
