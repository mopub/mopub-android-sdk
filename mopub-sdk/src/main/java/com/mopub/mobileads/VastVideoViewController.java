package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;
import com.mopub.common.DownloadResponse;
import com.mopub.common.DownloadTask;
import com.mopub.common.HttpResponses;
import com.mopub.common.MoPubBrowser;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Drawables;
import com.mopub.common.util.Streams;
import com.mopub.common.util.VersionCode;
import com.mopub.mobileads.util.vast.VastCompanionAd;
import com.mopub.mobileads.util.vast.VastVideoConfiguration;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static com.mopub.common.HttpClient.initializeHttpGet;
import static com.mopub.common.HttpClient.makeTrackingHttpRequest;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;

public class VastVideoViewController extends BaseVideoViewController implements DownloadTask.DownloadTaskListener {
    static final String VAST_VIDEO_CONFIGURATION = "vast_video_configuration";

    private static final float FIRST_QUARTER_MARKER = 0.25f;
    private static final float MID_POINT_MARKER = 0.50f;
    private static final float THIRD_QUARTER_MARKER = 0.75f;
    private static final long VIDEO_PROGRESS_TIMER_CHECKER_DELAY = 50;
    private static final int MOPUB_BROWSER_REQUEST_CODE = 1;
    private static final int MAX_VIDEO_RETRIES = 1;
    private static final int VIDEO_VIEW_FILE_PERMISSION_ERROR = Integer.MIN_VALUE;

    private static final ThreadPoolExecutor sThreadPoolExecutor = new ThreadPoolExecutor(10, 50, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    static final int DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON = 5 * 1000;
    static final int MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON = 16 * 1000;

    private final VastVideoConfiguration mVastVideoConfiguration;
    private final VastCompanionAd mVastCompanionAd;
    private final VastVideoToolbar mVastVideoToolbar;
    private final VideoView mVideoView;
    private final ImageView mCompanionAdImageView;
    private final View.OnTouchListener mClickThroughListener;

    private final Handler mHandler;
    private final Runnable mVideoProgressCheckerRunnable;
    private boolean mIsVideoProgressShouldBeChecked;
    private int mShowCloseButtonDelay = DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;

    private boolean mShowCloseButtonEventFired;
    private boolean mIsStartMarkHit;
    private boolean mIsFirstMarkHit;
    private boolean mIsSecondMarkHit;
    private boolean mIsThirdMarkHit;
    private int mSeekerPositionOnPause;
    private boolean mIsVideoFinishedPlaying;
    private int mVideoRetries;

    VastVideoViewController(final Context context,
            final Bundle bundle,
            final long broadcastIdentifier,
            final BaseVideoViewControllerListener baseVideoViewControllerListener)
            throws IllegalStateException {
        super(context, broadcastIdentifier, baseVideoViewControllerListener);
        mHandler = new Handler();
        mIsVideoProgressShouldBeChecked = false;
        mSeekerPositionOnPause = -1;
        mVideoRetries = 0;

        Serializable serializable = bundle.getSerializable(VAST_VIDEO_CONFIGURATION);
        if (serializable != null && serializable instanceof VastVideoConfiguration) {
            mVastVideoConfiguration = (VastVideoConfiguration) serializable;
        } else {
            throw new IllegalStateException("VastVideoConfiguration is invalid");
        }

        if (mVastVideoConfiguration.getDiskMediaFileUrl() == null) {
            throw new IllegalStateException("VastVideoConfiguration does not have a video disk path");
        }

        mVastCompanionAd = mVastVideoConfiguration.getVastCompanionAd();

        mClickThroughListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && shouldAllowClickThrough()) {
                    handleClick(
                            mVastVideoConfiguration.getClickTrackers(),
                            mVastVideoConfiguration.getClickThroughUrl()
                    );
                }
                return true;
            }
        };

        createVideoBackground(context);

        mVideoView = createVideoView(context);
        mVideoView.requestFocus();

        mVastVideoToolbar = createVastVideoToolBar(context);
        getLayout().addView(mVastVideoToolbar);

        mCompanionAdImageView = createCompanionAdImageView(context);

        makeTrackingHttpRequest(mVastVideoConfiguration.getImpressionTrackers(), context);

        mVideoProgressCheckerRunnable = createVideoProgressCheckerRunnable();
    }

    @Override
    VideoView getVideoView() {
        return mVideoView;
    }

    @Override
    void onCreate() {
        super.onCreate();
        getBaseVideoViewControllerListener().onSetRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);

        broadcastAction(ACTION_INTERSTITIAL_SHOW);

        downloadCompanionAd();
    }

    @Override
    void onResume() {
        // When resuming, VideoView needs to reinitialize its MediaPlayer with the video path
        // and therefore reset the count to zero, to let it retry on error
        mVideoRetries = 0;
        startProgressChecker();

        mVideoView.seekTo(mSeekerPositionOnPause);
        if (!mIsVideoFinishedPlaying) {
            mVideoView.start();
        }
    }

    @Override
    void onPause() {
        stopProgressChecker();
        mSeekerPositionOnPause = mVideoView.getCurrentPosition();
        mVideoView.pause();
    }

    @Override
    void onDestroy() {
        stopProgressChecker();
        broadcastAction(ACTION_INTERSTITIAL_DISMISS);
    }

    // Enable the device's back button when the video close button has been displayed
    @Override
    boolean backButtonEnabled() {
        return mShowCloseButtonEventFired;
    }

    @Override
    void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == MOPUB_BROWSER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            getBaseVideoViewControllerListener().onFinish();
        }
    }

    // DownloadTaskListener
    @Override
    public void onComplete(String url, DownloadResponse downloadResponse) {
        if (downloadResponse != null && downloadResponse.getStatusCode() == HttpStatus.SC_OK) {
            final Bitmap companionAdBitmap = HttpResponses.asBitmap(downloadResponse);
            if (companionAdBitmap != null) {
                // If Bitmap fits in ImageView, then don't use MATCH_PARENT
                final int width = Dips.dipsToIntPixels(companionAdBitmap.getWidth(), getContext());
                final int height = Dips.dipsToIntPixels(companionAdBitmap.getHeight(), getContext());
                final int imageViewWidth = mCompanionAdImageView.getMeasuredWidth();
                final int imageViewHeight = mCompanionAdImageView.getMeasuredHeight();
                if (width < imageViewWidth && height < imageViewHeight) {
                    mCompanionAdImageView.getLayoutParams().width = width;
                    mCompanionAdImageView.getLayoutParams().height = height;
                }
                mCompanionAdImageView.setImageBitmap(companionAdBitmap);
                mCompanionAdImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mVastCompanionAd != null) {
                            handleClick(
                                    mVastCompanionAd.getClickTrackers(),
                                    mVastCompanionAd.getClickThroughUrl()
                            );
                        }
                    }
                });
            }
        }
    }

    private void downloadCompanionAd() {
        if (mVastCompanionAd != null) {
            try {
                final HttpGet httpGet = initializeHttpGet(mVastCompanionAd.getImageUrl(), getContext());
                final DownloadTask downloadTask = new DownloadTask(this);
                AsyncTasks.safeExecuteOnExecutor(downloadTask, httpGet);
            } catch (Exception e) {
                MoPubLog.d("Failed to download companion ad", e);
            }
        }
    }

    private Runnable createVideoProgressCheckerRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                float videoLength = mVideoView.getDuration();
                float currentPosition = mVideoView.getCurrentPosition();

                if (videoLength > 0) {
                    float progressPercentage = currentPosition / videoLength;

                    if (!mIsStartMarkHit && currentPosition >= 1000) {
                        mIsStartMarkHit = true;
                        makeTrackingHttpRequest(mVastVideoConfiguration.getStartTrackers(), getContext());
                    }

                    if (!mIsFirstMarkHit && progressPercentage > FIRST_QUARTER_MARKER) {
                        mIsFirstMarkHit = true;
                        makeTrackingHttpRequest(mVastVideoConfiguration.getFirstQuartileTrackers(), getContext());
                    }

                    if (!mIsSecondMarkHit && progressPercentage > MID_POINT_MARKER) {
                        mIsSecondMarkHit = true;
                        makeTrackingHttpRequest(mVastVideoConfiguration.getMidpointTrackers(), getContext());
                    }

                    if (!mIsThirdMarkHit && progressPercentage > THIRD_QUARTER_MARKER) {
                        mIsThirdMarkHit = true;
                        makeTrackingHttpRequest(mVastVideoConfiguration.getThirdQuartileTrackers(), getContext());
                    }

                    if (isLongVideo(mVideoView.getDuration()) ) {
                        mVastVideoToolbar.updateCountdownWidget(mShowCloseButtonDelay - mVideoView.getCurrentPosition());
                    }

                    if (shouldBeInteractable()) {
                        makeVideoInteractable();
                    }
                }

                mVastVideoToolbar.updateDurationWidget(mVideoView.getDuration() - mVideoView.getCurrentPosition());

                if (mIsVideoProgressShouldBeChecked) {
                    mHandler.postDelayed(mVideoProgressCheckerRunnable, VIDEO_PROGRESS_TIMER_CHECKER_DELAY);
                }
            }
        };
    }

    private void createVideoBackground(final Context context) {
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {Color.argb(0,0,0,0), Color.argb(255,0,0,0)}
        );
        Drawable[] layers = new Drawable[2];
        layers[0] = Drawables.THATCHED_BACKGROUND.decodeImage(context);
        layers[1] = gradientDrawable;
        LayerDrawable layerList = new LayerDrawable(layers);
        getLayout().setBackgroundDrawable(layerList);
    }

    private VastVideoToolbar createVastVideoToolBar(final Context context) {
        final VastVideoToolbar vastVideoToolbar = new VastVideoToolbar(context);
        vastVideoToolbar.setCloseButtonOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    getBaseVideoViewControllerListener().onFinish();
                }
                return true;
            }
        });
        vastVideoToolbar.setLearnMoreButtonOnTouchListener(mClickThroughListener);
        return vastVideoToolbar;
    }

    private VideoView createVideoView(final Context context) {
        final VideoView videoView = new VideoView(context);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mVideoView.getDuration() < MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON) {
                    mShowCloseButtonDelay = mVideoView.getDuration();
                }
            }
        });
        videoView.setOnTouchListener(mClickThroughListener);

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopProgressChecker();
                makeVideoInteractable();

                videoCompleted(false);

                makeTrackingHttpRequest(mVastVideoConfiguration.getCompleteTrackers(), context);
                mIsVideoFinishedPlaying = true;

                videoView.setVisibility(View.GONE);
                // check the drawable to see if the image view was populated with content
                if (mCompanionAdImageView.getDrawable() != null) {
                    mCompanionAdImageView.setVisibility(View.VISIBLE);
                }
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(final MediaPlayer mediaPlayer, final int what, final int extra) {
                if (retryMediaPlayer(mediaPlayer, what, extra)) {
                    return true;
                } else {
                    stopProgressChecker();
                    makeVideoInteractable();
                    videoError(false);
                    return false;
                }
            }
        });

        videoView.setVideoPath(mVastVideoConfiguration.getDiskMediaFileUrl());

        return videoView;
    }

    boolean retryMediaPlayer(final MediaPlayer mediaPlayer, final int what, final int extra) {
        // XXX
        // VideoView has a bug in versions lower than Jelly Bean, Api Level 16, Android 4.1
        // For api < 16, VideoView is not able to read files written to disk since it reads them in
        // a Context different from the Application and therefore does not have correct permission.
        // To solve this problem we obtain the video file descriptor ourselves with valid permissions
        // and pass it to the underlying MediaPlayer in VideoView.
        if (VersionCode.currentApiLevel().isBelow(VersionCode.JELLY_BEAN)
                && what == MediaPlayer.MEDIA_ERROR_UNKNOWN
                && extra == VIDEO_VIEW_FILE_PERMISSION_ERROR
                && mVideoRetries < MAX_VIDEO_RETRIES) {

            FileInputStream inputStream = null;
            try {
                mediaPlayer.reset();
                final File file = new File(mVastVideoConfiguration.getDiskMediaFileUrl());
                inputStream = new FileInputStream(file);
                mediaPlayer.setDataSource(inputStream.getFD());

                // XXX
                // VideoView has a callback registered with the MediaPlayer to set a flag when the
                // media file has been prepared. Start also sets a flag in VideoView indicating the
                // desired state is to play the video. Therefore, whichever method finishes last
                // will check both flags and begin playing the video.
                mediaPlayer.prepareAsync();
                mVideoView.start();
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                Streams.closeStream(inputStream);
                mVideoRetries++;
            }
        }
        return false;
    }

    private ImageView createCompanionAdImageView(final Context context) {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        relativeLayout.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.BELOW, mVastVideoToolbar.getId());
        getLayout().addView(relativeLayout, layoutParams);

        ImageView imageView = new ImageView(context);
        // Set to invisible to have it be drawn to calculate size
        imageView.setVisibility(View.INVISIBLE);

        final RelativeLayout.LayoutParams companionAdLayout = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );

        relativeLayout.addView(imageView, companionAdLayout);
        return imageView;
    }

    private void handleClick(final List<String> clickThroughTrackers, final String clickThroughUrl) {
        makeTrackingHttpRequest(clickThroughTrackers, getContext());

        videoClicked();

        Bundle bundle = new Bundle();
        bundle.putString(MoPubBrowser.DESTINATION_URL_KEY, clickThroughUrl);

        getBaseVideoViewControllerListener().onStartActivityForResult(MoPubBrowser.class,
                MOPUB_BROWSER_REQUEST_CODE, bundle);
    }

    private boolean isLongVideo(final int duration) {
        return (duration >= MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    private void makeVideoInteractable() {
        mShowCloseButtonEventFired = true;
        mVastVideoToolbar.makeInteractable();
    }

    private boolean shouldBeInteractable() {
        return !mShowCloseButtonEventFired && mVideoView.getCurrentPosition() > mShowCloseButtonDelay;
    }

    private boolean shouldAllowClickThrough() {
        return mShowCloseButtonEventFired;
    }

    private void startProgressChecker() {
        if (!mIsVideoProgressShouldBeChecked) {
            mIsVideoProgressShouldBeChecked = true;
            mHandler.post(mVideoProgressCheckerRunnable);
        }
    }

    private void stopProgressChecker() {
        if (mIsVideoProgressShouldBeChecked) {
            mIsVideoProgressShouldBeChecked = false;
            mHandler.removeCallbacks(mVideoProgressCheckerRunnable);
        }
    }

    // for testing
    @Deprecated
    boolean getIsVideoProgressShouldBeChecked() {
        return mIsVideoProgressShouldBeChecked;
    }

    // for testing
    @Deprecated
    int getVideoRetries() {
        return mVideoRetries;
    }

    // for testing
    @Deprecated
    Runnable getVideoProgressCheckerRunnable() {
        return mVideoProgressCheckerRunnable;
    }

    // for testing
    @Deprecated
    int getShowCloseButtonDelay() {
        return mShowCloseButtonDelay;
    }

    // for testing
    @Deprecated
    boolean isShowCloseButtonEventFired() {
        return mShowCloseButtonEventFired;
    }

    // for testing
    @Deprecated
    void setCloseButtonVisible(boolean visible) {
        mShowCloseButtonEventFired = visible;
    }

    // for testing
    @Deprecated
    boolean isVideoFinishedPlaying() {
        return mIsVideoFinishedPlaying;
    }

    // for testing
    @Deprecated
    ImageView getCompanionAdImageView() {
        return mCompanionAdImageView;
    }
}
