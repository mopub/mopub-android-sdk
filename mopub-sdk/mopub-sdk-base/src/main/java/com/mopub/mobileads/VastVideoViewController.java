package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.mopub.common.ExternalViewabilitySession.VideoEvent;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.IntentActions;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.resource.DrawableConstants;

import java.io.Serializable;
import java.util.Map;

import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
import static android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
import static com.mopub.common.MoPubBrowser.MOPUB_BROWSER_REQUEST_CODE;
import static com.mopub.mobileads.VastXmlManagerAggregator.ADS_BY_AD_SLOT_ID;
import static com.mopub.mobileads.VastXmlManagerAggregator.SOCIAL_ACTIONS_AD_SLOT_ID;
import static com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest;

public class VastVideoViewController extends BaseVideoViewController {
    static final String VAST_VIDEO_CONFIG = "vast_video_config";
    static final String CURRENT_POSITION = "current_position";
    static final String RESUMED_VAST_CONFIG = "resumed_vast_config";

    private static final long VIDEO_PROGRESS_TIMER_CHECKER_DELAY = 50;
    private static final long VIDEO_COUNTDOWN_UPDATE_INTERVAL = 250;
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

    private final VastVideoConfig mVastVideoConfig;

    @NonNull private final VastVideoView mVideoView;
    @NonNull private ExternalViewabilitySessionManager mExternalViewabilitySessionManager;
    @NonNull private VastVideoGradientStripWidget mTopGradientStripWidget;
    @NonNull private VastVideoGradientStripWidget mBottomGradientStripWidget;
    @NonNull private ImageView mBlurredLastVideoFrameImageView;

    @NonNull private VastVideoProgressBarWidget mProgressBarWidget;
    @NonNull private VastVideoRadialCountdownWidget mRadialCountdownWidget;
    @NonNull private VastVideoCtaButtonWidget mCtaButtonWidget;
    @NonNull private VastVideoCloseButtonWidget mCloseButtonWidget;

    @Nullable private VastCompanionAdConfig mVastCompanionAdConfig;
    @Nullable private final VastIconConfig mVastIconConfig;
    @NonNull private final View mLandscapeCompanionAdView;
    @NonNull private final View mPortraitCompanionAdView;
    @NonNull private final Map<String, VastCompanionAdConfig> mSocialActionsCompanionAds;
    @NonNull private View mAdsByView;
    @NonNull private final View mSocialActionsView;
    @NonNull private final View mIconView;

    @NonNull private final VastVideoViewProgressRunnable mProgressCheckerRunnable;
    @NonNull private final VastVideoViewCountdownRunnable mCountdownRunnable;
    @NonNull private final View.OnTouchListener mClickThroughListener;

    private int mShowCloseButtonDelay = DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;
    private boolean mShowCloseButtonEventFired;
    private int mSeekerPositionOnPause;
    private boolean mIsVideoFinishedPlaying;
    private boolean mVideoError;
    private boolean mHasSkipOffset = false;
    private boolean mIsCalibrationDone = false;
    private boolean mHasSocialActions = false;
    private int mDuration;

    /**
     * For when the video is closing.
     */
    private boolean mIsClosing = false;

    VastVideoViewController(final Activity activity,
            final Bundle intentExtras,
            @Nullable final Bundle savedInstanceState,
            final long broadcastIdentifier,
            final BaseVideoViewControllerListener baseVideoViewControllerListener)
            throws IllegalStateException {
        super(activity, broadcastIdentifier, baseVideoViewControllerListener);
        mSeekerPositionOnPause = SEEKER_POSITION_NOT_INITIALIZED;

        Serializable resumedVastConfiguration = null;
        if (savedInstanceState != null) {
            resumedVastConfiguration =
                    savedInstanceState.getSerializable(RESUMED_VAST_CONFIG);
        }
        Serializable serializable = intentExtras.getSerializable(VAST_VIDEO_CONFIG);
        if (resumedVastConfiguration != null
                && resumedVastConfiguration instanceof VastVideoConfig) {
            mVastVideoConfig = (VastVideoConfig) resumedVastConfiguration;
            mSeekerPositionOnPause =
                    savedInstanceState.getInt(CURRENT_POSITION, SEEKER_POSITION_NOT_INITIALIZED);
        } else if (serializable != null && serializable instanceof VastVideoConfig) {
            mVastVideoConfig = (VastVideoConfig) serializable;
        } else {
            throw new IllegalStateException("VastVideoConfig is invalid");
        }

        if (mVastVideoConfig.getDiskMediaFileUrl() == null) {
            throw new IllegalStateException("VastVideoConfig does not have a video disk path");
        }

        mVastCompanionAdConfig = mVastVideoConfig.getVastCompanionAd(
                activity.getResources().getConfiguration().orientation);
        mSocialActionsCompanionAds = mVastVideoConfig.getSocialActionsCompanionAds();
        mVastIconConfig = mVastVideoConfig.getVastIconConfig();

        mClickThroughListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && shouldAllowClickThrough()) {
                    mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.AD_CLICK_THRU,
                            getCurrentPosition());
                    mIsClosing = true;
                    broadcastAction(IntentActions.ACTION_INTERSTITIAL_CLICK);
                    mVastVideoConfig.handleClickForResult(activity,
                            mIsVideoFinishedPlaying ? mDuration : getCurrentPosition(),
                            MOPUB_BROWSER_REQUEST_CODE);
                }
                return true;
            }
        };

        // Add widgets in the following order.
        // Ordering matters because some placements are relative to other widgets.

        // Solid black background
        getLayout().setBackgroundColor(Color.BLACK);

        // Blurred last frame
        addBlurredLastVideoFrameImageView(activity, View.INVISIBLE);

        // Video view
        mVideoView = createVideoView(activity, View.VISIBLE);
        mVideoView.requestFocus();

        // Viewability measurements
        mExternalViewabilitySessionManager = new ExternalViewabilitySessionManager(activity);
        mExternalViewabilitySessionManager.createVideoSession(activity, mVideoView,
                mVastVideoConfig);
        mExternalViewabilitySessionManager.registerVideoObstruction(mBlurredLastVideoFrameImageView);

        // Companion ad view, set to invisible initially to have it be drawn to calculate size
        mLandscapeCompanionAdView = createCompanionAdView(activity,
                mVastVideoConfig.getVastCompanionAd(Configuration.ORIENTATION_LANDSCAPE),
                View.INVISIBLE);
        mPortraitCompanionAdView = createCompanionAdView(activity,
                mVastVideoConfig.getVastCompanionAd(Configuration.ORIENTATION_PORTRAIT),
                View.INVISIBLE);

        // Top transparent gradient strip overlaying top of screen
        addTopGradientStripWidget(activity);

        // Progress bar overlaying bottom of video view
        addProgressBarWidget(activity, View.INVISIBLE);

        // Bottom transparent gradient strip above progress bar
        addBottomGradientStripWidget(activity);

        // Radial countdown timer snapped to top-right corner of screen
        addRadialCountdownWidget(activity, View.INVISIBLE);

        // Icon view
        mIconView = createIconView(activity, mVastIconConfig, View.INVISIBLE);
        mIconView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // Wait until mIconView has been laid out to get accurate height
                        mAdsByView = createAdsByView(activity);
                        mIconView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
        );

        // CTA button
        addCtaButtonWidget(activity);

        // Social Actions
        final int ctaHeight = Dips.dipsToIntPixels(DrawableConstants.CtaButton.HEIGHT_DIPS,
                activity);
        mSocialActionsView = createSocialActionsView(activity,
                mSocialActionsCompanionAds.get(SOCIAL_ACTIONS_AD_SLOT_ID),
                ctaHeight, RelativeLayout.ALIGN_TOP, mCtaButtonWidget, View.INVISIBLE,
                DrawableConstants.SocialActions.SOCIAL_ACTIONS_LEFT_MARGIN_DIPS);

        // Close button snapped to top-right corner of screen
        // Always add last to layout since it must be visible above all other views
        addCloseButtonWidget(activity, View.GONE);

        Handler mainHandler = new Handler(Looper.getMainLooper());
        mProgressCheckerRunnable = new VastVideoViewProgressRunnable(this, mVastVideoConfig,
                mainHandler);
        mCountdownRunnable = new VastVideoViewCountdownRunnable(this, mainHandler);
    }

    @VisibleForTesting
    View createAdsByView(Activity activity) {
        return createSocialActionsView(activity,
                mSocialActionsCompanionAds.get(ADS_BY_AD_SLOT_ID),
                mIconView.getHeight(),
                RelativeLayout.RIGHT_OF, mIconView, View.VISIBLE,
                DrawableConstants.SocialActions.ADS_BY_LEFT_MARGIN_DIPS
        );
    }

    @Deprecated // Just use for testing
    @VisibleForTesting
    boolean getHasSocialActions() {
        return mHasSocialActions;
    }

    @Deprecated // Just use for testing
    @VisibleForTesting
    View getSocialActionsView() {
        return mSocialActionsView;
    }

    @Override
    protected VideoView getVideoView() {
        return mVideoView;
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        switch (mVastVideoConfig.getCustomForceOrientation()) {
            case FORCE_PORTRAIT:
                getBaseVideoViewControllerListener().onSetRequestedOrientation(SCREEN_ORIENTATION_PORTRAIT);
                break;
            case FORCE_LANDSCAPE:
                getBaseVideoViewControllerListener().onSetRequestedOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                break;
            case DEVICE_ORIENTATION:
                break;  // don't do anything
            case UNDEFINED:
                break;  // don't do anything
            default:
                break;
        }

        mVastVideoConfig.handleImpression(getContext(), getCurrentPosition());
        broadcastAction(IntentActions.ACTION_INTERSTITIAL_SHOW);
    }

    @Override
    protected void onResume() {
        startRunnables();

        if (mSeekerPositionOnPause > 0) {
            mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.AD_PLAYING, mSeekerPositionOnPause);
            mVideoView.seekTo(mSeekerPositionOnPause);
        } else {
            mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.AD_LOADED, getCurrentPosition());
        }

        if (!mIsVideoFinishedPlaying) {
            mVideoView.start();
        }
        if (mSeekerPositionOnPause != SEEKER_POSITION_NOT_INITIALIZED) {
            mVastVideoConfig.handleResume(getContext(), mSeekerPositionOnPause);
        }
    }

    @Override
    protected void onPause() {
        stopRunnables();
        mSeekerPositionOnPause = getCurrentPosition();
        mVideoView.pause();
        if (!mIsVideoFinishedPlaying && !mIsClosing) {
            mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.AD_PAUSED, getCurrentPosition());
            mVastVideoConfig.handlePause(getContext(), mSeekerPositionOnPause);
        }
    }

    @Override
    protected void onDestroy() {
        stopRunnables();
        mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.AD_STOPPED, getCurrentPosition());
        mExternalViewabilitySessionManager.endVideoSession();
        broadcastAction(IntentActions.ACTION_INTERSTITIAL_DISMISS);

        mVideoView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(CURRENT_POSITION, mSeekerPositionOnPause);
        outState.putSerializable(RESUMED_VAST_CONFIG, mVastVideoConfig);
    }

    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {
        final int orientation = getContext().getResources().getConfiguration().orientation;
        mVastCompanionAdConfig = mVastVideoConfig.getVastCompanionAd(orientation);
        if (mLandscapeCompanionAdView.getVisibility() == View.VISIBLE ||
                mPortraitCompanionAdView.getVisibility() == View.VISIBLE) {
            if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                mLandscapeCompanionAdView.setVisibility(View.INVISIBLE);
                mPortraitCompanionAdView.setVisibility(View.VISIBLE);
            } else {
                mPortraitCompanionAdView.setVisibility(View.INVISIBLE);
                mLandscapeCompanionAdView.setVisibility(View.VISIBLE);
            }
            if (mVastCompanionAdConfig != null) {
                mVastCompanionAdConfig.handleImpression(getContext(), mDuration);
            }
        }
    }

    @Override
    protected void onBackPressed() {
        if (!mIsVideoFinishedPlaying) {
            mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.AD_SKIPPED, getCurrentPosition());
        }
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

        // If this is a rewarded video, never allow it to be skippable.
        if (mVastVideoConfig.isRewardedVideo()) {
            mShowCloseButtonDelay = videoDuration;
            return;
        }

        // Default behavior: video is non-skippable if duration < 16 seconds
        if (videoDuration < MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON) {
            mShowCloseButtonDelay = videoDuration;
        }

        // Override if skipoffset attribute is specified in VAST
        final Integer skipOffsetMillis = mVastVideoConfig.getSkipOffsetMillis(videoDuration);
        if (skipOffsetMillis != null) {
            mShowCloseButtonDelay = skipOffsetMillis;
            mHasSkipOffset = true;
        }
    }

    private VastVideoView createVideoView(@NonNull final Context context, int initialVisibility) {
        if (mVastVideoConfig.getDiskMediaFileUrl() == null) {
            throw new IllegalStateException("VastVideoConfig does not have a video disk path");
        }
        final VastVideoView videoView = new VastVideoView(context);

        videoView.setId((int) Utils.generateUniqueId());

        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                // Called when media source is ready for playback
                // The VideoView duration defaults to -1 when the video is not prepared or playing;
                // Therefore set it here so that we have access to it at all times
                mDuration = mVideoView.getDuration();
                mExternalViewabilitySessionManager.onVideoPrepared(getLayout(), mDuration);
                adjustSkipOffset();
                if (mVastCompanionAdConfig == null || mHasSocialActions) {
                    videoView.prepareBlurredLastVideoFrame(mBlurredLastVideoFrameImageView,
                            mVastVideoConfig.getDiskMediaFileUrl());
                }
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
                if (mVastVideoConfig.isRewardedVideo()) {
                    broadcastAction(IntentActions.ACTION_REWARDED_VIDEO_COMPLETE);
                }

                // Only fire the completion tracker if we hit all the progress marks. Some Android implementations
                // fire the completion event even if the whole video isn't watched.
                if (!mVideoError && mVastVideoConfig.getRemainingProgressTrackerCount() == 0) {
                    mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.AD_COMPLETE, getCurrentPosition());
                    mVastVideoConfig.handleComplete(getContext(), getCurrentPosition());
                }

                videoView.setVisibility(View.INVISIBLE);

                mProgressBarWidget.setVisibility(View.GONE);

                if (mHasSocialActions) {
                    // Social Actions need the blurred last frame to fill the screen regardless
                    // of the companion ad.
                    if (mBlurredLastVideoFrameImageView.getDrawable() != null) {
                        mBlurredLastVideoFrameImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        mBlurredLastVideoFrameImageView.setVisibility(View.VISIBLE);
                    }
                    // Also, avoid removing DAA icon for social actions
                } else {
                    mIconView.setVisibility(View.GONE);
                }

                mTopGradientStripWidget.notifyVideoComplete();
                mBottomGradientStripWidget.notifyVideoComplete();
                mCtaButtonWidget.notifyVideoComplete();

                // Show companion ad if available
                if (mVastCompanionAdConfig != null) {
                    final int orientation = context.getResources().getConfiguration().orientation;
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                        mPortraitCompanionAdView.setVisibility(View.VISIBLE);
                    } else {
                        mLandscapeCompanionAdView.setVisibility(View.VISIBLE);
                    }
                    mVastCompanionAdConfig.handleImpression(context, mDuration);
                } else if (mBlurredLastVideoFrameImageView.getDrawable() != null) {
                    // If there is no companion ad, show blurred last video frame with dark overlay
                    mBlurredLastVideoFrameImageView.setVisibility(View.VISIBLE);
                }
            }
        });

        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(final MediaPlayer mediaPlayer, final int what, final int extra) {
                mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.RECORD_AD_ERROR,
                        getCurrentPosition());
                stopRunnables();
                makeVideoInteractable();
                videoError(false);
                mVideoError = true;

                mVastVideoConfig.handleError(getContext(),
                        VastErrorCode.GENERAL_LINEAR_AD_ERROR, getCurrentPosition());

                return false;
            }
        });

        videoView.setVideoPath(mVastVideoConfig.getDiskMediaFileUrl());
        videoView.setVisibility(initialVisibility);

        return videoView;
    }

    private void addTopGradientStripWidget(@NonNull final Context context) {
        boolean hasCompanionAd = (mVastCompanionAdConfig != null);

        mTopGradientStripWidget = new VastVideoGradientStripWidget(context,
                GradientDrawable.Orientation.TOP_BOTTOM,
                mVastVideoConfig.getCustomForceOrientation(),
                hasCompanionAd,
                View.VISIBLE,
                RelativeLayout.ALIGN_TOP,
                getLayout().getId());
        getLayout().addView(mTopGradientStripWidget);
        mExternalViewabilitySessionManager.registerVideoObstruction(mTopGradientStripWidget);
    }

    private void addBottomGradientStripWidget(@NonNull final Context context) {
        boolean hasCompanionAd = (mVastCompanionAdConfig != null);

        mBottomGradientStripWidget = new VastVideoGradientStripWidget(context,
                GradientDrawable.Orientation.BOTTOM_TOP,
                mVastVideoConfig.getCustomForceOrientation(),
                hasCompanionAd,
                View.GONE,
                RelativeLayout.ABOVE,
                mProgressBarWidget.getId());
        getLayout().addView(mBottomGradientStripWidget);
        mExternalViewabilitySessionManager.registerVideoObstruction(mBottomGradientStripWidget);
    }

    private void addProgressBarWidget(@NonNull final Context context, int initialVisibility) {
        mProgressBarWidget = new VastVideoProgressBarWidget(context);
        mProgressBarWidget.setAnchorId(mVideoView.getId());
        mProgressBarWidget.setVisibility(initialVisibility);
        getLayout().addView(mProgressBarWidget);
        mExternalViewabilitySessionManager.registerVideoObstruction(mProgressBarWidget);
    }

    private void addRadialCountdownWidget(@NonNull final Context context, int initialVisibility) {
        mRadialCountdownWidget = new VastVideoRadialCountdownWidget(context);
        mRadialCountdownWidget.setVisibility(initialVisibility);
        getLayout().addView(mRadialCountdownWidget);
        mExternalViewabilitySessionManager.registerVideoObstruction(mRadialCountdownWidget);
    }

    private void addCtaButtonWidget(@NonNull final Context context) {
        boolean hasCompanionAd = (mVastCompanionAdConfig != null);
        boolean hasClickthroughUrl = !TextUtils.isEmpty(
                mVastVideoConfig.getClickThroughUrl());

        mCtaButtonWidget = new VastVideoCtaButtonWidget(context, mVideoView.getId(), hasCompanionAd,
                hasClickthroughUrl);

        getLayout().addView(mCtaButtonWidget);
        mExternalViewabilitySessionManager.registerVideoObstruction(mCtaButtonWidget);

        mCtaButtonWidget.setOnTouchListener(mClickThroughListener);

        // Update custom CTA text if specified in VAST extension
        String customCtaText = mVastVideoConfig.getCustomCtaText();
        if (customCtaText != null) {
            mCtaButtonWidget.updateCtaText(customCtaText);
        }
    }

    private void addCloseButtonWidget(@NonNull final Context context, int initialVisibility) {
        mCloseButtonWidget = new VastVideoCloseButtonWidget(context);
        mCloseButtonWidget.setVisibility(initialVisibility);

        getLayout().addView(mCloseButtonWidget);
        mExternalViewabilitySessionManager.registerVideoObstruction(mCloseButtonWidget);

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
                    mIsClosing = true;
                    if (!mIsVideoFinishedPlaying) {
                        mExternalViewabilitySessionManager.recordVideoEvent(VideoEvent.AD_SKIPPED, getCurrentPosition());
                    }
                    mVastVideoConfig.handleClose(getContext(), currentPosition);
                    getBaseVideoViewControllerListener().onFinish();
                }
                return true;
            }
        };

        mCloseButtonWidget.setOnTouchListenerToContent(closeOnTouchListener);

        // Update custom skip text if specified in VAST extensions
        final String customSkipText = mVastVideoConfig.getCustomSkipText();
        if (customSkipText != null) {
            mCloseButtonWidget.updateCloseButtonText(customSkipText);
        }

        // Update custom close icon if specified in VAST extensions
        final String customCloseIconUrl = mVastVideoConfig.getCustomCloseIconUrl();
        if (customCloseIconUrl != null) {
            mCloseButtonWidget.updateCloseButtonIcon(customCloseIconUrl);
        }
    }

    private void addBlurredLastVideoFrameImageView(@NonNull final Context context,
            int initialVisibility) {

        mBlurredLastVideoFrameImageView = new ImageView(context);
        mBlurredLastVideoFrameImageView.setVisibility(initialVisibility);

        final RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);

        getLayout().addView(mBlurredLastVideoFrameImageView, layoutParams);
    }

    /**
     * Creates and lays out the webview used to display the companion ad.
     *
     * @param context         The context.
     * @param vastCompanionAdConfig The data used to populate the view.
     * @return the populated webview
     */
    @NonNull
    @VisibleForTesting
    View createCompanionAdView(@NonNull final Context context,
            @Nullable final VastCompanionAdConfig vastCompanionAdConfig,
            int initialVisibility) {
        Preconditions.checkNotNull(context);

        if (vastCompanionAdConfig == null) {
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
        mExternalViewabilitySessionManager.registerVideoObstruction(relativeLayout);

        VastWebView companionView = createCompanionVastWebView(context, vastCompanionAdConfig);

        companionView.setVisibility(initialVisibility);

        final RelativeLayout.LayoutParams companionAdLayout = new RelativeLayout.LayoutParams(
                Dips.dipsToIntPixels(vastCompanionAdConfig.getWidth() + WEBVIEW_PADDING, context),
                Dips.dipsToIntPixels(vastCompanionAdConfig.getHeight() + WEBVIEW_PADDING, context)
        );
        companionAdLayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        relativeLayout.addView(companionView, companionAdLayout);
        mExternalViewabilitySessionManager.registerVideoObstruction(companionView);

        return companionView;
    }

    /**
     * Creates and lays out the webviews used to display the social actions.
     *
     * @param context The context.
     * @param vastCompanionAdConfig The data used to populate the view.
     * @param anchorHeight The height in pixels of the view to use as anchor for the view.
     * @param layoutVerb The way to layout the view relative to the anchorView.
     * @param anchorView The view to use as anchor when laying out the view.
     * @param initialVisibility The visibility the view should have.
     * @return the populated webview
     */
    @NonNull
    @VisibleForTesting
    View createSocialActionsView(@NonNull final Context context,
            @Nullable final VastCompanionAdConfig vastCompanionAdConfig, final int anchorHeight,
            final int layoutVerb, @NonNull final View anchorView, final int initialVisibility,
            final int leftMarginDips) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(anchorView);

        if (vastCompanionAdConfig == null) {
            final View emptyView = new View(context);
            emptyView.setVisibility(View.INVISIBLE);
            return emptyView;
        }

        mHasSocialActions = true;
        mCtaButtonWidget.setHasSocialActions(mHasSocialActions);

        VastWebView companionView = createCompanionVastWebView(context, vastCompanionAdConfig);

        final int width = Dips.dipsToIntPixels(vastCompanionAdConfig.getWidth(), context);
        final int height = Dips.dipsToIntPixels(vastCompanionAdConfig.getHeight(), context);
        final int offset = (anchorHeight - height) / 2;
        final int leftMargin = Dips.dipsToIntPixels(leftMarginDips, context);

        final RelativeLayout.LayoutParams companionAdLayout =
                new RelativeLayout.LayoutParams(width, height);
        companionAdLayout.addRule(layoutVerb, anchorView.getId());
        companionAdLayout.addRule(RelativeLayout.ALIGN_TOP, anchorView.getId());
        companionAdLayout.setMargins(leftMargin, offset, 0, 0);

        RelativeLayout relativeLayout = new RelativeLayout(context);
        relativeLayout.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.WRAP_CONTENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);

        relativeLayout.addView(companionView, layoutParams);
        mExternalViewabilitySessionManager.registerVideoObstruction(companionView);

        getLayout().addView(relativeLayout, companionAdLayout);
        mExternalViewabilitySessionManager.registerVideoObstruction(relativeLayout);

        companionView.setVisibility(initialVisibility);
        return companionView;
    }

    /**
     * Creates and lays out the webview used to display the icon.
     *
     * @param context the context.
     * @param vastIconConfig the data used to populate the view.
     * @return the populated webview.
     */
    @NonNull
    @VisibleForTesting
    View createIconView(@NonNull final Context context, @Nullable final VastIconConfig vastIconConfig, int initialVisibility) {
        Preconditions.checkNotNull(context);

        if (vastIconConfig == null) {
            return new View(context);
        }

        VastWebView iconView = VastWebView.createView(context, vastIconConfig.getVastResource());
        iconView.setVastWebViewClickListener(new VastWebView.VastWebViewClickListener() {
            @Override
            public void onVastWebViewClick() {
                makeVastTrackingHttpRequest(
                        vastIconConfig.getClickTrackingUris(),
                        null,
                        getCurrentPosition(),
                        getNetworkMediaFileUrl(),
                        context
                );
                vastIconConfig.handleClick(getContext(), null, mVastVideoConfig.getDspCreativeId());
            }
        });
        iconView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                vastIconConfig.handleClick(getContext(), url, mVastVideoConfig.getDspCreativeId());
                return true;
            }
        });
        iconView.setVisibility(initialVisibility);

        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(
                        Dips.asIntPixels(vastIconConfig.getWidth(), context),
                        Dips.asIntPixels(vastIconConfig.getHeight(), context));
        final int leftMargin = Dips.dipsToIntPixels(
                DrawableConstants.PrivacyInfoIcon.LEFT_MARGIN_DIPS, context);
        final int topMargin = Dips.dipsToIntPixels(
                DrawableConstants.PrivacyInfoIcon.TOP_MARGIN_DIPS, context);
        layoutParams.setMargins(leftMargin, topMargin, 0, 0);

        getLayout().addView(iconView, layoutParams);
        mExternalViewabilitySessionManager.registerVideoObstruction(iconView);

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
        mSocialActionsView.setVisibility(View.VISIBLE);
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
        if (mVastVideoConfig == null) {
            return null;
        }
        return mVastVideoConfig.getNetworkMediaFileUrl();
    }

    /**
     * Displays and impresses the icon if the current position of the video is greater than the
     * offset of the icon. Once the current position is greater than the offset plus duration, the
     * icon is then hidden again.
     *
     * @param currentPosition the current position of the video in milliseconds.
     */
    void handleIconDisplay(int currentPosition) {
        if (mVastIconConfig == null || currentPosition < mVastIconConfig.getOffsetMS()) {
            return;
        }

        mIconView.setVisibility(View.VISIBLE);
        mVastIconConfig.handleImpression(getContext(), currentPosition, getNetworkMediaFileUrl());

        if (mVastIconConfig.getDurationMS() == null) {
            return;
        }

        if (currentPosition >= mVastIconConfig.getOffsetMS() + mVastIconConfig.getDurationMS()) {
            mIconView.setVisibility(View.GONE);
        }
    }

    void handleViewabilityQuartileEvent(@NonNull final String enumValue) {
        final VideoEvent videoEvent = Enum.valueOf(VideoEvent.class, enumValue);

        mExternalViewabilitySessionManager.recordVideoEvent(videoEvent, getCurrentPosition());
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

    @NonNull
    private VastWebView createCompanionVastWebView(@NonNull final Context context,
            @NonNull final VastCompanionAdConfig vastCompanionAdConfig) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(vastCompanionAdConfig);

        VastWebView companionView = VastWebView.createView(context,
                vastCompanionAdConfig.getVastResource());

        // For javascript, HTML, and IFrames, ignore the traditional clickthrough url and open all
        // new urls in the MoPub Browser. For static images, use the clickthrough url specified in
        // the VAST document. These two handleClicks make it so that the correct behavior happens
        // in these special cases. onVastWebViewClick is called in both circumstances to fire the
        // click trackers.
        companionView.setVastWebViewClickListener(new VastWebView.VastWebViewClickListener() {
            @Override
            public void onVastWebViewClick() {
                broadcastAction(IntentActions.ACTION_INTERSTITIAL_CLICK);
                makeVastTrackingHttpRequest(
                        vastCompanionAdConfig.getClickTrackers(),
                        null,
                        mDuration,
                        null,
                        context
                );
                vastCompanionAdConfig.handleClick(context, MOPUB_BROWSER_REQUEST_CODE, null, mVastVideoConfig.getDspCreativeId());
            }
        });
        companionView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                vastCompanionAdConfig.handleClick(context, MOPUB_BROWSER_REQUEST_CODE, url, mVastVideoConfig.getDspCreativeId());
                return true;
            }
        });

        return companionView;
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
    View getLandscapeCompanionAdView() {
        return mLandscapeCompanionAdView;
    }

    // for testing
    @Deprecated
    @VisibleForTesting
    View getPortraitCompanionAdView() {
        return mPortraitCompanionAdView;
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
    void setVideoError() {
        mVideoError = true;
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
        return mBottomGradientStripWidget;
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
    void setProgressBarWidget(@NonNull VastVideoProgressBarWidget progressBarWidget) {
        mProgressBarWidget = progressBarWidget;
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
    void setRadialCountdownWidget(@NonNull VastVideoRadialCountdownWidget radialCountdownWidget) {
        mRadialCountdownWidget = radialCountdownWidget;
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
    VastVideoView getVastVideoView() {
        return mVideoView;
    }

    @Deprecated
    @VisibleForTesting
    void setIsClosing(boolean isClosing) {
        mIsClosing = isClosing;
    }
}
