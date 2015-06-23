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
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
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
import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.event.BaseEvent;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Drawables;
import com.mopub.common.util.Streams;
import com.mopub.common.util.Strings;
import com.mopub.common.util.VersionCode;
import com.mopub.mobileads.util.vast.VastCompanionAd;
import com.mopub.mobileads.util.vast.VastVideoConfiguration;
import com.mopub.network.TrackingRequest;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static com.mopub.common.HttpClient.initializeHttpGet;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.network.TrackingRequest.makeTrackingHttpRequest;

public class VastVideoViewController extends BaseVideoViewController implements DownloadTask.DownloadTaskListener {
    static final String VAST_VIDEO_CONFIGURATION = "vast_video_configuration";

    private static final long VIDEO_PROGRESS_TIMER_CHECKER_DELAY = 50;
    private static final long VIDEO_COUNTDOWN_UPDATE_INTERVAL = 250;
    private static final int MOPUB_BROWSER_REQUEST_CODE = 1;
    private static final int MAX_VIDEO_RETRIES = 1;
    private static final int VIDEO_VIEW_FILE_PERMISSION_ERROR = Integer.MIN_VALUE;

    static final int DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON = 5 * 1000;
    static final int MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON = 16 * 1000;

    private final VastVideoConfiguration mVastVideoConfiguration;
    private final VastCompanionAd mVastCompanionAd;
    private final VastVideoToolbar mVastVideoToolbar;
    private final VideoView mVideoView;
    private final ImageView mCompanionAdImageView;
    private final View.OnTouchListener mClickThroughListener;

    private final VastVideoViewProgressRunnable mProgressCheckerRunnable;
    private final VastVideoViewCountdownRunnable mCountdownRunnable;
    private int mShowCloseButtonDelay = DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;

    private boolean mShowCloseButtonEventFired;

    private int mSeekerPositionOnPause;
    private boolean mIsVideoFinishedPlaying;
    private int mVideoRetries;

    private boolean mVideoError;
    private boolean mCompletionTrackerFired;

    private boolean mHasSkipOffset = false;

    VastVideoViewController(final Context context,
            final Bundle bundle,
            final long broadcastIdentifier,
            final BaseVideoViewControllerListener baseVideoViewControllerListener)
            throws IllegalStateException {
        super(context, broadcastIdentifier, baseVideoViewControllerListener);
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

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mProgressCheckerRunnable = new VastVideoViewProgressRunnable(this, mainHandler);
        mCountdownRunnable = new VastVideoViewCountdownRunnable(this, mainHandler);
    }

    @Override
    protected VideoView getVideoView() {
        return mVideoView;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        switch (mVastVideoConfiguration.getCustomForceOrientation()) {
            case FORCE_PORTRAIT:
                getBaseVideoViewControllerListener().onSetRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
                break;
            case FORCE_LANDSCAPE:
                getBaseVideoViewControllerListener().onSetRequestedOrientation(SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case DEVICE_ORIENTATION:
                break;  // don't do anything
            case UNDEFINED:
                break;  // don't do anything
            default:
                break;
        }

        downloadCompanionAd();

        makeTrackingHttpRequest(
                mVastVideoConfiguration.getImpressionTrackers(),
                getContext(),
                BaseEvent.Name.IMPRESSION_REQUEST
        );
        broadcastAction(ACTION_INTERSTITIAL_SHOW);
    }

    @Override
    protected void onResume() {
        // When resuming, VideoView needs to reinitialize its MediaPlayer with the video path
        // and therefore reset the count to zero, to let it retry on error
        mVideoRetries = 0;
        startRunnables();

        mVideoView.seekTo(mSeekerPositionOnPause);
        if (!mIsVideoFinishedPlaying) {
            mVideoView.start();
        }
    }

    @Override
    protected void onPause() {
        stopRunnables();
        mSeekerPositionOnPause = getCurrentPosition();
        mVideoView.pause();
    }

    @Override
    protected void onDestroy() {
        stopRunnables();
        broadcastAction(ACTION_INTERSTITIAL_DISMISS);
    }

    // Enable the device's back button when the video close button has been displayed
    @Override
    public boolean backButtonEnabled() {
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

    private void adjustSkipOffset() {
        int videoDuration = getDuration();

        // Default behavior: video is non-skippable if duration < 16 seconds
        if (videoDuration < MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON) {
            mShowCloseButtonDelay = videoDuration;
        }

        // Override if skipoffset attribute is specified in VAST
        String skipOffsetString = mVastVideoConfiguration.getSkipOffset();
        if (skipOffsetString != null) {
            try {
                if (Strings.isAbsoluteTracker(skipOffsetString)) {
                    Integer skipOffsetMilliseconds = Strings.parseAbsoluteOffset(skipOffsetString);
                    if (skipOffsetMilliseconds != null && skipOffsetMilliseconds < videoDuration) {
                        mShowCloseButtonDelay = skipOffsetMilliseconds;
                        mHasSkipOffset = true;
                    }
                } else if (Strings.isPercentageTracker(skipOffsetString)) {
                    float percentage = Float.parseFloat(skipOffsetString.replace("%", "")) / 100f;
                    int skipOffsetMillisecondsRounded = Math.round(videoDuration * percentage);
                    if (skipOffsetMillisecondsRounded < videoDuration) {
                        mShowCloseButtonDelay = skipOffsetMillisecondsRounded;
                        mHasSkipOffset = true;
                    }
                } else {
                    MoPubLog.d(String.format("Invalid VAST skipoffset format: %s", skipOffsetString));
                }
            } catch (NumberFormatException e) {
                MoPubLog.d(String.format("Failed to parse skipoffset %s", skipOffsetString));
            }
        }
    }

    /**
     * Returns untriggered VAST progress trackers with a progress before the provided position.
     *
     * @param currentPositionMillis the current video position in milliseconds.
     * @param videoLengthMillis the total video length.
     */
    @NonNull
    List<VastTracker> getUntriggeredTrackersBefore(int currentPositionMillis, int videoLengthMillis) {
        if (Preconditions.NoThrow.checkArgument(videoLengthMillis > 0)) {
            float progressFraction = currentPositionMillis / (float) (videoLengthMillis);
            List<VastTracker> untriggeredTrackers = new ArrayList<VastTracker>();

            final ArrayList<VastAbsoluteProgressTracker> absoluteTrackers = mVastVideoConfiguration.getAbsoluteTrackers();
            VastAbsoluteProgressTracker absoluteTest = new VastAbsoluteProgressTracker("", currentPositionMillis);
            int absoluteTrackerCount = absoluteTrackers.size();
            for (int i = 0; i < absoluteTrackerCount; i++) {
                VastAbsoluteProgressTracker tracker = absoluteTrackers.get(i);
                if (tracker.compareTo(absoluteTest) > 0) {
                    break;
                }
                if (!tracker.isTracked()) {
                    untriggeredTrackers.add(tracker);
                }
            }

            final ArrayList<VastFractionalProgressTracker> fractionalTrackers = mVastVideoConfiguration.getFractionalTrackers();
            final VastFractionalProgressTracker fractionalTest = new VastFractionalProgressTracker("", progressFraction);
            int fractionalTrackerCount = fractionalTrackers.size();
            for (int i = 0; i < fractionalTrackerCount; i++) {
                VastFractionalProgressTracker tracker = fractionalTrackers.get(i);
                if (tracker.compareTo(fractionalTest) > 0) {
                    break;
                }
                if (!tracker.isTracked()) {
                    untriggeredTrackers.add(tracker);
                }
            }

            return untriggeredTrackers;
        } else {
            return Collections.emptyList();
        }
    }

    private int remainingProgressTrackerCount() {
        return getUntriggeredTrackersBefore(Integer.MAX_VALUE, Integer.MAX_VALUE).size();
    }

    private void createVideoBackground(final Context context) {
        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.argb(0, 0, 0, 0), Color.argb(255, 0, 0, 0)}
        );
        Drawable[] layers = new Drawable[2];
        layers[0] = Drawables.THATCHED_BACKGROUND.createDrawable(context);
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
                    TrackingRequest.makeTrackingHttpRequest(
                            mVastVideoConfiguration.getCloseTrackers(), context);
                    TrackingRequest.makeTrackingHttpRequest(
                            mVastVideoConfiguration.getSkipTrackers(), context);
                    getBaseVideoViewControllerListener().onFinish();
                }
                return true;
            }
        });
        vastVideoToolbar.setLearnMoreButtonOnTouchListener(mClickThroughListener);

        // update custom CTA text if specified in VAST extension
        String customCtaText = mVastVideoConfiguration.getCustomCtaText();
        if (customCtaText != null) {
            vastVideoToolbar.updateLearnMoreButtonText(customCtaText);
        }

        // update custom skip text if specified in VAST extensions
        String customSkipText = mVastVideoConfiguration.getCustomSkipText();
        if (customSkipText != null) {
            vastVideoToolbar.updateCloseButtonText(customSkipText);
        }

        // update custom close icon if specified in VAST extensions
        String customCloseIconUrl = mVastVideoConfiguration.getCustomCloseIconUrl();
        if (customCloseIconUrl != null) {
            vastVideoToolbar.updateCloseButtonIcon(customCloseIconUrl);
        }

        return vastVideoToolbar;
    }

    private VideoView createVideoView(final Context context) {
        final VideoView videoView = new VideoView(context);
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // Called when media source is ready for playback
                adjustSkipOffset();
            }
        });
        videoView.setOnTouchListener(mClickThroughListener);

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopRunnables();
                makeVideoInteractable();

                videoCompleted(false);
                mIsVideoFinishedPlaying = true;

                // Only fire the completion tracker if we hit all the progress marks. Some Android implementations
                // fire the completion event even if the whole video isn't watched.
                if (!mVideoError && remainingProgressTrackerCount() == 0 && !mCompletionTrackerFired) {
                    makeTrackingHttpRequest(mVastVideoConfiguration.getCompleteTrackers(), context);
                    mCompletionTrackerFired = true;
                }

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
                    stopRunnables();
                    makeVideoInteractable();
                    videoError(false);
                    mVideoError = true;
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

    /**
     * Called upon user click. Attempts open mopubnativebrowser links in the device browser and all
     * other links in the MoPub in-app browser.
     */
    @VisibleForTesting
    void handleClick(final List<String> clickThroughTrackers, final String clickThroughUrl) {
        makeTrackingHttpRequest(clickThroughTrackers, getContext(), BaseEvent.Name.CLICK_REQUEST);

        if (TextUtils.isEmpty(clickThroughUrl)) {
            return;
        }

        broadcastAction(ACTION_INTERSTITIAL_CLICK);

        new UrlHandler.Builder()
                .withSupportedUrlActions(
                        UrlAction.IGNORE_ABOUT_SCHEME,
                        UrlAction.OPEN_NATIVE_BROWSER,
                        UrlAction.OPEN_IN_APP_BROWSER,
                        UrlAction.HANDLE_SHARE_TWEET)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                        if (urlAction == UrlAction.OPEN_IN_APP_BROWSER) {
                            Bundle bundle = new Bundle();
                            bundle.putString(MoPubBrowser.DESTINATION_URL_KEY, clickThroughUrl);

                            getBaseVideoViewControllerListener().onStartActivityForResult(
                                    MoPubBrowser.class, MOPUB_BROWSER_REQUEST_CODE, bundle);
                        }
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                    }
                })
                .withoutMoPubBrowser()
                .build().handleUrl(getContext(), clickThroughUrl);
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

    int getDuration() {
        return mVideoView.getDuration();
    }

    int getCurrentPosition() {
        return mVideoView.getCurrentPosition();
    }

    boolean isLongVideo(final int duration) {
        return (duration >= MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON);
    }

    void makeVideoInteractable() {
        mShowCloseButtonEventFired = true;
        mVastVideoToolbar.makeInteractable();
    }

    boolean shouldBeInteractable() {
        return !mShowCloseButtonEventFired && getCurrentPosition() >= mShowCloseButtonDelay;
    }

    boolean shouldShowCountdown() {
        // show countdown if any of the following conditions is satisfied:
        // 1) long video
        // 2) skipoffset is specified in VAST and is less than video duration
        final int duration = getDuration();
        return isLongVideo(duration) || (mHasSkipOffset && mShowCloseButtonDelay < duration);
    }

    void updateCountdown() {
        mVastVideoToolbar.updateCountdownWidget(mShowCloseButtonDelay - getCurrentPosition());
    }

    void updateDuration() {
        mVastVideoToolbar.updateDurationWidget(getDuration() - getCurrentPosition());
    }

    private boolean shouldAllowClickThrough() {
        return mShowCloseButtonEventFired;
    }

    private void startRunnables() {
        mProgressCheckerRunnable.startRepeating(VIDEO_PROGRESS_TIMER_CHECKER_DELAY);
        mCountdownRunnable.startRepeating(VIDEO_COUNTDOWN_UPDATE_INTERVAL);
    }

    private void stopRunnables() {
        mProgressCheckerRunnable.stop();
        mCountdownRunnable.stop();
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    VastVideoViewProgressRunnable getProgressCheckerRunnable() {
        return mProgressCheckerRunnable;
    }

    @Deprecated
    @VisibleForTesting
    VastVideoViewCountdownRunnable getCountdownRunnable() {
        return mCountdownRunnable;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    int getVideoRetries() {
        return mVideoRetries;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    boolean getHasSkipOffset() {
        return mHasSkipOffset;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    int getShowCloseButtonDelay() {
        return mShowCloseButtonDelay;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    boolean isShowCloseButtonEventFired() {
        return mShowCloseButtonEventFired;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setCloseButtonVisible(boolean visible) {
        mShowCloseButtonEventFired = visible;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    boolean isVideoFinishedPlaying() {
        return mIsVideoFinishedPlaying;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    ImageView getCompanionAdImageView() {
        return mCompanionAdImageView;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setVideoError() {
        mVideoError = true;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    boolean getVideoError() {
        return mVideoError;
    }
}
