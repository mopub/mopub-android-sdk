package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.common.util.ImageUtils;
import com.mopub.common.util.Streams;
import com.mopub.common.util.Strings;
import com.mopub.common.util.Utils;
import com.mopub.common.util.VersionCode;
import com.mopub.mobileads.resource.DrawableConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest;

public class VastVideoViewController extends BaseVideoViewController {
    static final String VAST_VIDEO_CONFIGURATION = "vast_video_configuration";
    static final String CURRENT_POSITION = "current_position";
    static final String RESUMED_VAST_CONFIGURATION = "resumed_vast_configuration";

    private static final long VIDEO_PROGRESS_TIMER_CHECKER_DELAY = 50;
    private static final long VIDEO_COUNTDOWN_UPDATE_INTERVAL = 250;
    private static final int MOPUB_BROWSER_REQUEST_CODE = 1;
    private static final int MAX_VIDEO_RETRIES = 1;
    private static final int VIDEO_VIEW_FILE_PERMISSION_ERROR = Integer.MIN_VALUE;
    private static final int SEEKER_POSITION_NOT_INITIALIZED = -1;

    /**
     * Android WebViews supposedly have padding on each side of 10 dp. However, through empirical
     * testing, the number is actually closer to 8 dp. Increasing the width and height of the
     * WebView by this many dp will make the images inside not get cut off. This also prevents the
     * image from being scrollable.
     */
    public static final int WEBVIEW_PADDING = 16;

    static final int DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON = 5 * 1000;
    static final int MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON = 16 * 1000;

    private final VastVideoConfiguration mVastVideoConfiguration;

    @NonNull private final VideoView mVideoView;
    @NonNull private VastVideoGradientStripWidget mTopGradientStripWidget;
    @NonNull private VastVideoGradientStripWidget mBottomGradientStripWidget;
    @NonNull private ImageView mBlurredLastVideoFrameImageView;

    @NonNull private VastVideoProgressBarWidget mProgressBarWidget;
    @NonNull private VastVideoRadialCountdownWidget mRadialCountdownWidget;
    @NonNull private VastVideoCtaButtonWidget mCtaButtonWidget;
    @NonNull private VastVideoCloseButtonWidget mCloseButtonWidget;

    @Nullable private final VastCompanionAd mVastCompanionAd;
    @NonNull private final View mCompanionAdView;
    @Nullable private final VastIcon mVastIcon;
    @NonNull private final View mIconView;

    @NonNull private final VastVideoViewProgressRunnable mProgressCheckerRunnable;
    @NonNull private final VastVideoViewCountdownRunnable mCountdownRunnable;
    @NonNull private final View.OnTouchListener mClickThroughListener;

    @Nullable private MediaMetadataRetriever mMediaMetadataRetriever;

    private int mShowCloseButtonDelay = DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;
    private boolean mShowCloseButtonEventFired;
    private int mSeekerPositionOnPause;
    private boolean mIsVideoFinishedPlaying;
    private int mVideoRetries;
    private boolean mVideoError;
    private boolean mHasSkipOffset = false;
    private boolean mIsCalibrationDone = false;
    private int mDuration;

    VastVideoViewController(final Context context,
            final Bundle intentExtras,
            @Nullable final Bundle savedInstanceState,
            final long broadcastIdentifier,
            final BaseVideoViewControllerListener baseVideoViewControllerListener)
            throws IllegalStateException {
        super(context, broadcastIdentifier, baseVideoViewControllerListener);
        mSeekerPositionOnPause = SEEKER_POSITION_NOT_INITIALIZED;

        Serializable resumedVastConfiguration = null;
        if (savedInstanceState != null) {
            resumedVastConfiguration =
                    savedInstanceState.getSerializable(RESUMED_VAST_CONFIGURATION);
        }
        Serializable serializable = intentExtras.getSerializable(VAST_VIDEO_CONFIGURATION);
        if (resumedVastConfiguration != null
                && resumedVastConfiguration instanceof VastVideoConfiguration) {
            mVastVideoConfiguration = (VastVideoConfiguration) resumedVastConfiguration;
            mSeekerPositionOnPause =
                    savedInstanceState.getInt(CURRENT_POSITION, SEEKER_POSITION_NOT_INITIALIZED);
        } else if (serializable != null && serializable instanceof VastVideoConfiguration) {
            mVastVideoConfiguration = (VastVideoConfiguration) serializable;
        } else {
            throw new IllegalStateException("VastVideoConfiguration is invalid");
        }

        if (mVastVideoConfiguration.getDiskMediaFileUrl() == null) {
            throw new IllegalStateException("VastVideoConfiguration does not have a video disk path");
        }

        mVastCompanionAd = mVastVideoConfiguration.getVastCompanionAd();
        mVastIcon = mVastVideoConfiguration.getVastIcon();

        mMediaMetadataRetriever = createMediaMetadataRetriever();

        mClickThroughListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && shouldAllowClickThrough()) {
                    makeVastTrackingHttpRequest(
                            mVastVideoConfiguration.getClickTrackers(),
                            null,
                            mIsVideoFinishedPlaying ? mDuration : getCurrentPosition(),
                            getNetworkMediaFileUrl(),
                            getContext()
                    );
                    handleClick(mVastVideoConfiguration.getClickThroughUrl());
                }
                return true;
            }
        };

        // Add widgets in the following order.
        // Ordering matters because some placements are relative to other widgets.

        // Solid black background
        getLayout().setBackgroundColor(Color.BLACK);

        // Video view
        mVideoView = createVideoView(context, View.VISIBLE);
        mVideoView.requestFocus();

        // Top transparent gradient strip overlaying top of screen
        addTopGradientStripWidget(context, View.VISIBLE);

        // Progress bar overlaying bottom of video view
        addProgressBarWidget(context, View.INVISIBLE);

        // Bottom transparent gradient strip above progress bar
        addBottomGradientStripWidget(context, View.VISIBLE);

        // Radial countdown timer snapped to top-right corner of screen
        addRadialCountdownWidget(context, View.INVISIBLE);

        // Companion ad view, set to invisible initially to have it be drawn to calculate size
        mCompanionAdView = createCompanionAdView(context, mVastCompanionAd, View.INVISIBLE);

        // Icon view
        mIconView = createIconView(context, mVastIcon, View.INVISIBLE);

        // Blurred last frame
        addBlurredLastVideoFrameImageView(context, View.INVISIBLE);

        // Close button snapped to top-right corner of screen
        addCloseButtonWidget(context, View.GONE);

        // Always add last to layout since it must be visible above all other views
        addCtaButtonWidget(context, View.INVISIBLE);

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

        makeVastTrackingHttpRequest(
                mVastVideoConfiguration.getImpressionTrackers(),
                null,
                getCurrentPosition(),
                getNetworkMediaFileUrl(),
                getContext()
        );
        broadcastAction(ACTION_INTERSTITIAL_SHOW);
    }

    @Override
    protected void onResume() {
        // When resuming, VideoView needs to reinitialize its MediaPlayer with the video path
        // and therefore reset the count to zero, to let it retry on error
        mVideoRetries = 0;
        startRunnables();

        if (mSeekerPositionOnPause > 0) {
            mVideoView.seekTo(mSeekerPositionOnPause);
        }
        if (!mIsVideoFinishedPlaying) {
            mVideoView.start();
        }
        if (mSeekerPositionOnPause != SEEKER_POSITION_NOT_INITIALIZED) {
            makeVastTrackingHttpRequest(
                    mVastVideoConfiguration.getResumeTrackers(),
                    null,
                    mSeekerPositionOnPause,
                    mVastVideoConfiguration.getNetworkMediaFileUrl(),
                    getContext()
            );
        }
    }

    @Override
    protected void onPause() {
        stopRunnables();
        mSeekerPositionOnPause = getCurrentPosition();
        mVideoView.pause();
        if (!mIsVideoFinishedPlaying) {
            makeVastTrackingHttpRequest(
                    mVastVideoConfiguration.getPauseTrackers(),
                    null,
                    getCurrentPosition(),
                    mVastVideoConfiguration.getNetworkMediaFileUrl(),
                    getContext()
            );
        }
    }

    @Override
    protected void onDestroy() {
        stopRunnables();
        broadcastAction(ACTION_INTERSTITIAL_DISMISS);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(CURRENT_POSITION, mSeekerPositionOnPause);
        outState.putSerializable(RESUMED_VAST_CONFIGURATION, mVastVideoConfiguration);
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

    private void prepareBlurredLastVideoFrame() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            if (mVastCompanionAd == null && mMediaMetadataRetriever != null) {

                try {
                    mMediaMetadataRetriever.setDataSource(mVastVideoConfiguration.getDiskMediaFileUrl());
                } catch (RuntimeException e) {
                    // XXX
                    // MediaMetadataRetriever.setDataSource() might throw RuntimeException,
                    // possibly due to decoding issues for certain video formats.
                    // http://stackoverflow.com/questions/9657280/mediaplayer-setdatasource-causes-ioexception-for-valid-file
                    if (e.getMessage().contains("0x80000000")) {
                        MoPubLog.d("MediaMetadataRetriever.setDataSource() failed: status = 0x80000000");
                        return;
                    } else {
                        throw e;
                    }
                }

                Bitmap lastVideoFrame = mMediaMetadataRetriever.getFrameAtTime(
                        getDuration() * 1000, MediaMetadataRetriever.OPTION_CLOSEST);

                Bitmap blurredLastVideoFrame = ImageUtils.applyFastGaussianBlurToBitmap(
                        lastVideoFrame, 4);

                mBlurredLastVideoFrameImageView.setImageBitmap(blurredLastVideoFrame);
                ImageUtils.setImageViewAlpha(mBlurredLastVideoFrameImageView,
                        DrawableConstants.BlurredLastVideoFrame.ALPHA);
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

    private VideoView createVideoView(@NonNull final Context context, int initialVisibility) {
        final VideoView videoView = new VideoView(context);

        videoView.setId((int) Utils.generateUniqueId());

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // Called when media source is ready for playback
                // The VideoView duration defaults to -1 when the video is not prepared or playing;
                // Therefore set it here so that we have access to it at all times
                mDuration = mVideoView.getDuration();
                adjustSkipOffset();
                prepareBlurredLastVideoFrame();
                mProgressBarWidget.calibrateAndMakeVisible(getDuration(), mShowCloseButtonDelay);
                mRadialCountdownWidget.calibrateAndMakeVisible(mShowCloseButtonDelay);
                mIsCalibrationDone = true;
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
                if (!mVideoError && remainingProgressTrackerCount() == 0) {
                    makeVastTrackingHttpRequest(
                            mVastVideoConfiguration.getCompleteTrackers(),
                            null,
                            getCurrentPosition(),
                            getNetworkMediaFileUrl(),
                            getContext()
                    );
                }

                videoView.setVisibility(View.INVISIBLE);

                mProgressBarWidget.setVisibility(View.GONE);
                mTopGradientStripWidget.setVisibility(View.GONE);
                mBottomGradientStripWidget.setVisibility(View.GONE);
                mIconView.setVisibility(View.GONE);

                mCtaButtonWidget.notifyVideoComplete();

                // Show companion ad if available
                if (mVastCompanionAd != null) {
                    mCompanionAdView.setVisibility(View.VISIBLE);
                    mVastCompanionAd.handleImpression(context, mDuration);
                } else if (mBlurredLastVideoFrameImageView.getDrawable() != null) {
                    // If there is no companion ad, show blurred last video frame with dark overlay
                    mBlurredLastVideoFrameImageView.setVisibility(View.VISIBLE);
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

                    makeVastTrackingHttpRequest(
                            mVastVideoConfiguration.getErrorTrackers(),
                            VastErrorCode.GENERAL_LINEAR_AD_ERROR,
                            getCurrentPosition(),
                            getNetworkMediaFileUrl(),
                            getContext()
                    );

                    return false;
                }
            }
        });

        videoView.setVideoPath(mVastVideoConfiguration.getDiskMediaFileUrl());
        videoView.setVisibility(initialVisibility);

        return videoView;
    }

    private void addTopGradientStripWidget(@NonNull final Context context, int initialVisibility) {
        mTopGradientStripWidget = new VastVideoGradientStripWidget(context,
                GradientDrawable.Orientation.TOP_BOTTOM,
                RelativeLayout.ALIGN_TOP,
                mVideoView.getId());
        mTopGradientStripWidget.setVisibility(initialVisibility);
        getLayout().addView(mTopGradientStripWidget);
    }

    private void addBottomGradientStripWidget(@NonNull final Context context, int initialVisibility) {
        mBottomGradientStripWidget = new VastVideoGradientStripWidget(context,
                GradientDrawable.Orientation.BOTTOM_TOP,
                RelativeLayout.ABOVE,
                mProgressBarWidget.getId());
        mBottomGradientStripWidget.setVisibility(initialVisibility);
        getLayout().addView(mBottomGradientStripWidget);
    }

    private void addProgressBarWidget(@NonNull final Context context, int initialVisibility) {
        mProgressBarWidget = new VastVideoProgressBarWidget(context, mVideoView.getId());
        mProgressBarWidget.setVisibility(initialVisibility);
        getLayout().addView(mProgressBarWidget);
    }

    private void addRadialCountdownWidget(@NonNull final Context context, int initialVisibility) {
        mRadialCountdownWidget = new VastVideoRadialCountdownWidget(context);
        mRadialCountdownWidget.setVisibility(initialVisibility);
        getLayout().addView(mRadialCountdownWidget);
    }

    private void addCtaButtonWidget(@NonNull final Context context, int initialVisibility) {
        boolean hasCompanionAd = (mVastCompanionAd != null);

        mCtaButtonWidget = new VastVideoCtaButtonWidget(context, mVideoView.getId(), hasCompanionAd);

        mCtaButtonWidget.setVisibility(initialVisibility);

        getLayout().addView(mCtaButtonWidget);

        mCtaButtonWidget.setOnTouchListener(mClickThroughListener);

        // Update custom CTA text if specified in VAST extension
        String customCtaText = mVastVideoConfiguration.getCustomCtaText();
        if (customCtaText != null) {
            mCtaButtonWidget.updateCtaText(customCtaText);
        }
    }

    private void addCloseButtonWidget(@NonNull final Context context, int initialVisibility) {
        mCloseButtonWidget = new VastVideoCloseButtonWidget(context);
        mCloseButtonWidget.setVisibility(initialVisibility);

        getLayout().addView(mCloseButtonWidget);

        final View.OnTouchListener closeOnTouchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                final int currentPosition;
                if (mIsVideoFinishedPlaying) {
                    currentPosition = mDuration;
                } else {
                    currentPosition = getCurrentPosition();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    makeVastTrackingHttpRequest(
                            mVastVideoConfiguration.getCloseTrackers(),
                            null,
                            currentPosition,
                            getNetworkMediaFileUrl(),
                            getContext()
                    );
                    makeVastTrackingHttpRequest(
                            mVastVideoConfiguration.getSkipTrackers(),
                            null,
                            currentPosition,
                            getNetworkMediaFileUrl(),
                            getContext()
                    );
                    getBaseVideoViewControllerListener().onFinish();
                }
                return true;
            }
        };

        mCloseButtonWidget.setOnTouchListenerToContent(closeOnTouchListener);

        // Update custom skip text if specified in VAST extensions
        final String customSkipText = mVastVideoConfiguration.getCustomSkipText();
        if (customSkipText != null) {
            mCloseButtonWidget.updateCloseButtonText(customSkipText);
        }

        // Update custom close icon if specified in VAST extensions
        final String customCloseIconUrl = mVastVideoConfiguration.getCustomCloseIconUrl();
        if (customCloseIconUrl != null) {
            mCloseButtonWidget.updateCloseButtonIcon(customCloseIconUrl);
        }
    }

    @Nullable
    private MediaMetadataRetriever createMediaMetadataRetriever() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
            return new MediaMetadataRetriever();
        }

        return null;
    }

    private void addBlurredLastVideoFrameImageView(@NonNull final Context context,
            int initialVisibility) {

        mBlurredLastVideoFrameImageView = new ImageView(context);
        mBlurredLastVideoFrameImageView.setVisibility(initialVisibility);

        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        getLayout().addView(mBlurredLastVideoFrameImageView, layoutParams);
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
    void handleClick(final String clickThroughUrl) {
        if (TextUtils.isEmpty(clickThroughUrl)) {
            return;
        }

        broadcastAction(ACTION_INTERSTITIAL_CLICK);

        new UrlHandler.Builder()
                .withSupportedUrlActions(
                        UrlAction.IGNORE_ABOUT_SCHEME,
                        UrlAction.OPEN_APP_MARKET,
                        UrlAction.OPEN_NATIVE_BROWSER,
                        UrlAction.OPEN_IN_APP_BROWSER,
                        UrlAction.HANDLE_SHARE_TWEET,
                        UrlAction.FOLLOW_DEEP_LINK_WITH_FALLBACK,
                        UrlAction.FOLLOW_DEEP_LINK)
                .withResultActions(new UrlHandler.ResultActions() {
                    @Override
                    public void urlHandlingSucceeded(@NonNull String url,
                            @NonNull UrlAction urlAction) {
                        if (urlAction == UrlAction.OPEN_IN_APP_BROWSER) {
                            Bundle bundle = new Bundle();
                            bundle.putString(MoPubBrowser.DESTINATION_URL_KEY, url);

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

    /**
     * Creates and lays out the webview used to display the companion ad.
     *
     * @param context         The context.
     * @param vastCompanionAd The data used to populate the view.
     * @return the populated webview
     */
    @NonNull
    @VisibleForTesting
    View createCompanionAdView(@NonNull final Context context,
            @Nullable final VastCompanionAd vastCompanionAd,
            int initialVisibility) {
        Preconditions.checkNotNull(context);

        if (vastCompanionAd == null) {
            final View emptyView = new View(context);
            emptyView.setVisibility(View.INVISIBLE);
            return emptyView;
        }

        RelativeLayout relativeLayout = new RelativeLayout(context);
        relativeLayout.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
        getLayout().addView(relativeLayout, layoutParams);

        VastWebView companionView = VastWebView.createView(context,
                vastCompanionAd.getVastResource());
        companionView.setVastWebViewClickListener(new VastWebView.VastWebViewClickListener() {
            @Override
            public void onVastWebViewClick() {
                broadcastAction(ACTION_INTERSTITIAL_CLICK);
                makeVastTrackingHttpRequest(
                        vastCompanionAd.getClickTrackers(),
                        null,
                        mDuration,
                        null,
                        context
                );
                vastCompanionAd.handleClick(context, MOPUB_BROWSER_REQUEST_CODE, null);
            }
        });
        companionView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                vastCompanionAd.handleClick(context, MOPUB_BROWSER_REQUEST_CODE, url);
                return true;
            }
        });

        companionView.setVisibility(initialVisibility);

        final RelativeLayout.LayoutParams companionAdLayout = new RelativeLayout.LayoutParams(
                Dips.dipsToIntPixels(vastCompanionAd.getWidth() + WEBVIEW_PADDING, context),
                Dips.dipsToIntPixels(vastCompanionAd.getHeight() + WEBVIEW_PADDING, context)
        );
        companionAdLayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        relativeLayout.addView(companionView, companionAdLayout);
        return companionView;
    }

    /**
     * Creates and lays out the webview used to display the icon.
     *
     * @param context the context.
     * @param vastIcon the data used to populate the view.
     * @return the populated webview.
     */
    @NonNull
    @VisibleForTesting
    View createIconView(@NonNull final Context context, @Nullable final VastIcon vastIcon, int initialVisibility) {
        Preconditions.checkNotNull(context);

        if (vastIcon == null) {
            return new View(context);
        }

        VastWebView iconView = VastWebView.createView(context, vastIcon.getVastResource());
        iconView.setVastWebViewClickListener(new VastWebView.VastWebViewClickListener() {
            @Override
            public void onVastWebViewClick() {
                makeVastTrackingHttpRequest(
                        vastIcon.getClickTrackingUris(),
                        null,
                        getCurrentPosition(),
                        getNetworkMediaFileUrl(),
                        context
                );
                vastIcon.handleClick(getContext(), null);
            }
        });
        iconView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                vastIcon.handleClick(getContext(), url);
                return true;
            }
        });
        iconView.setVisibility(initialVisibility);

        // Add extra room for the WebView to account for the natural padding in Android WebViews.
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(
                        Dips.asIntPixels(vastIcon.getWidth() + WEBVIEW_PADDING, context),
                        Dips.asIntPixels(vastIcon.getHeight() + WEBVIEW_PADDING, context));
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        getLayout().addView(iconView, layoutParams);

        return iconView;
    }

    int getDuration() {
        return mVideoView.getDuration();
    }

    int getCurrentPosition() {
        return mVideoView.getCurrentPosition();
    }

    void makeVideoInteractable() {
        mShowCloseButtonEventFired = true;

        mRadialCountdownWidget.setVisibility(View.GONE);
        mCloseButtonWidget.setVisibility(View.VISIBLE);

        mCtaButtonWidget.notifyVideoSkippable();
    }

    boolean shouldBeInteractable() {
        return !mShowCloseButtonEventFired && getCurrentPosition() >= mShowCloseButtonDelay;
    }

    void updateCountdown() {
        if (mIsCalibrationDone) {
            mRadialCountdownWidget.updateCountdownProgress(mShowCloseButtonDelay, getCurrentPosition());
        }
    }

    void updateProgressBar() {
        mProgressBarWidget.updateProgress(getCurrentPosition());
    }

    String getNetworkMediaFileUrl() {
        if (mVastVideoConfiguration == null) {
            return null;
        }
        return mVastVideoConfiguration.getNetworkMediaFileUrl();
    }

    /**
     * Displays and impresses the icon if the current position of the video is greater than the
     * offset of the icon. Once the current position is greater than the offset plus duration, the
     * icon is then hidden again.
     *
     * @param currentPosition the current position of the video in milliseconds.
     */
    void handleIconDisplay(int currentPosition) {
        if (mVastIcon == null || currentPosition < mVastIcon.getOffsetMS()) {
            return;
        }

        mIconView.setVisibility(View.VISIBLE);
        mVastIcon.handleImpression(getContext(), currentPosition, getNetworkMediaFileUrl());

        if (mVastIcon.getDurationMS() == null) {
            return;
        }

        if (currentPosition >= mVastIcon.getOffsetMS() + mVastIcon.getDurationMS()) {
            mIconView.setVisibility(View.GONE);
        }
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
    boolean isCalibrationDone() {
        return mIsCalibrationDone;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    View getCompanionAdView() {
        return mCompanionAdView;
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

    // for testing
    @Deprecated
    @VisibleForTesting
    View getIconView() {
        return mIconView;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    VastVideoGradientStripWidget getTopGradientStripWidget() {
        return mTopGradientStripWidget;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    VastVideoGradientStripWidget getBottomGradientStripWidget() {
        return mTopGradientStripWidget;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    VastVideoProgressBarWidget getProgressBarWidget() {
        return mProgressBarWidget;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    VastVideoRadialCountdownWidget getRadialCountdownWidget() {
        return mRadialCountdownWidget;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    VastVideoCtaButtonWidget getCtaButtonWidget() {
        return mCtaButtonWidget;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    VastVideoCloseButtonWidget getCloseButtonWidget() {
        return mCloseButtonWidget;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    ImageView getBlurredLastVideoFrameImageView() {
        return mBlurredLastVideoFrameImageView;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setMediaMetadataRetriever(@NonNull MediaMetadataRetriever mediaMetadataRetriever) {
        mMediaMetadataRetriever = mediaMetadataRetriever;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setRadialCountdownWidget(@NonNull VastVideoRadialCountdownWidget radialCountdownWidget) {
        mRadialCountdownWidget = radialCountdownWidget;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    void setProgressBarWidget(@NonNull VastVideoProgressBarWidget progressBarWidget) {
        mProgressBarWidget = progressBarWidget;
    }
}
