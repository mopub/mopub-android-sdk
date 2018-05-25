package com.mopub.nativeads;

import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Drawables;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.VastVideoProgressBarWidget;
import com.mopub.mobileads.resource.DrawableConstants.GradientStrip;

public class MediaLayout extends RelativeLayout {
    public enum Mode { IMAGE, PLAYING, LOADING, BUFFERING, PAUSED, FINISHED }
    public enum MuteState { MUTED, UNMUTED }

    private static final int GRADIENT_STRIP_HEIGHT_DIPS = 35;
    private static final int MUTE_SIZE_DIPS = 36;
    private static final int CONTROL_SIZE_DIPS = 40;
    private static final int PINNER_PADDING_DIPS = 10;

    private static final float ASPECT_MULTIPLIER_WIDTH_TO_HEIGHT = 9f / 16;
    private static final float ASPECT_MULTIPLIER_HEIGHT_TO_WIDTH = 16f / 9;

    @NonNull private volatile Mode mMode = Mode.IMAGE;
    @NonNull private MuteState mMuteState;

    @NonNull private ImageView mMainImageView;

    // These views are video-only, ordered by their z index. Don't create them if they aren't needed.
    @Nullable private TextureView mVideoTextureView;
    @Nullable private ProgressBar mLoadingSpinner;
    @Nullable private ImageView mPlayButton;
    @Nullable private ImageView mBottomGradient;
    @Nullable private ImageView mTopGradient;
    @Nullable private VastVideoProgressBarWidget mVideoProgress;
    @Nullable private ImageView mMuteControl;
    @Nullable private View mOverlay;
    @Nullable private Drawable mMutedDrawable;
    @Nullable private Drawable mUnmutedDrawable;

    private boolean mIsInitialized;

    // Measurements
    private final int mControlSizePx;
    private final int mGradientStripHeightPx;
    private final int mMuteSizePx;
    private final int mPaddingPx;

    // Constructors
    public MediaLayout(@NonNull final Context context) {
        this(context, null);
    }

    public MediaLayout(@NonNull final Context context, @Nullable final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MediaLayout(@NonNull final Context context, @Nullable final AttributeSet attrs,
            final int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Preconditions.checkNotNull(context);

        mMuteState = MuteState.MUTED;

        // Create and layout the main imageView and set its modes.
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        mMainImageView = new ImageView(context);
        mMainImageView.setLayoutParams(params);
        mMainImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(mMainImageView);

        mControlSizePx = Dips.asIntPixels(CONTROL_SIZE_DIPS, context);
        mGradientStripHeightPx = Dips.asIntPixels(GRADIENT_STRIP_HEIGHT_DIPS, context);
        mMuteSizePx = Dips.asIntPixels(MUTE_SIZE_DIPS, context);
        mPaddingPx = Dips.asIntPixels(PINNER_PADDING_DIPS, context);
    }

    public void setSurfaceTextureListener(@Nullable final TextureView.SurfaceTextureListener stl) {
        if (mVideoTextureView != null) {
            mVideoTextureView.setSurfaceTextureListener(stl);

            SurfaceTexture st = mVideoTextureView.getSurfaceTexture();
            if (st != null && stl != null) {
                stl.onSurfaceTextureAvailable(st, mVideoTextureView.getWidth(), mVideoTextureView.getHeight());
            }
        }
    }

    /**
     * Users should call this method when the view will be used for video. Video views are not
     * instantiated in the image-only case in order to save time and memory.
     */
    public void initForVideo() {
        if (mIsInitialized) {
            return;
        }

        // Init and set up all the video view items.
        final LayoutParams videoTextureLayoutParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        videoTextureLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mVideoTextureView = new TextureView(getContext());
        mVideoTextureView.setLayoutParams(videoTextureLayoutParams);
        mVideoTextureView.setId((int) Utils.generateUniqueId());
        addView(mVideoTextureView);

        // Place texture beneath image.
        mMainImageView.bringToFront();

        final LayoutParams loadingSpinnerParams = new LayoutParams(mControlSizePx, mControlSizePx);
        loadingSpinnerParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        loadingSpinnerParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        mLoadingSpinner = new ProgressBar(getContext());
        mLoadingSpinner.setLayoutParams(loadingSpinnerParams);
        mLoadingSpinner.setPadding(0, mPaddingPx, mPaddingPx, 0);
        mLoadingSpinner.setIndeterminate(true);
        addView(mLoadingSpinner);

        final LayoutParams bottomGradientParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, mGradientStripHeightPx);
        bottomGradientParams.addRule(RelativeLayout.ALIGN_BOTTOM, mVideoTextureView.getId());
        mBottomGradient = new ImageView(getContext());
        mBottomGradient.setLayoutParams(bottomGradientParams);
        final GradientDrawable bottomGradientDrawable =
                new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {GradientStrip.START_COLOR, GradientStrip.END_COLOR});
        mBottomGradient.setImageDrawable(bottomGradientDrawable);
        addView(mBottomGradient);

        final LayoutParams topGradientParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, mGradientStripHeightPx);
        topGradientParams.addRule(RelativeLayout.ALIGN_TOP, mVideoTextureView.getId());
        mTopGradient = new ImageView(getContext());
        mTopGradient.setLayoutParams(topGradientParams);
        final GradientDrawable topGradientDrawable =
                new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[] {GradientStrip.START_COLOR, GradientStrip.END_COLOR});
        mTopGradient.setImageDrawable(topGradientDrawable);
        addView(mTopGradient);

        mVideoProgress = new VastVideoProgressBarWidget(getContext());
        mVideoProgress.setAnchorId(mVideoTextureView.getId());
        mVideoProgress.calibrateAndMakeVisible(1000, 0);
        addView(mVideoProgress);

        mMutedDrawable = Drawables.NATIVE_MUTED.createDrawable(getContext());
        mUnmutedDrawable = Drawables.NATIVE_UNMUTED.createDrawable(getContext());
        final LayoutParams muteControlParams = new LayoutParams(mMuteSizePx, mMuteSizePx);
        muteControlParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        muteControlParams.addRule(RelativeLayout.ABOVE, mVideoProgress.getId());
        mMuteControl = new ImageView(getContext());
        mMuteControl.setLayoutParams(muteControlParams);
        mMuteControl.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mMuteControl.setPadding(mPaddingPx, mPaddingPx, mPaddingPx, mPaddingPx);
        mMuteControl.setImageDrawable(mMutedDrawable);
        addView(mMuteControl);

        final LayoutParams overlayParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        overlayParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mOverlay = new View(getContext());
        mOverlay.setLayoutParams(overlayParams);
        mOverlay.setBackgroundColor(Color.TRANSPARENT);
        addView(mOverlay);

        final LayoutParams playButtonParams = new LayoutParams(mControlSizePx, mControlSizePx);
        playButtonParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mPlayButton = new ImageView(getContext());
        mPlayButton.setLayoutParams(playButtonParams);
        mPlayButton.setImageDrawable(Drawables.NATIVE_PLAY.createDrawable(getContext()));
        addView(mPlayButton);

        mIsInitialized = true;
        updateViewState();
    }

    /**
     * Resets the view, removing all the OnClickListeners and setting the view to hide
     */
    public void reset() {
        setMode(Mode.IMAGE);
        setPlayButtonClickListener(null);
        setMuteControlClickListener(null);
        setVideoClickListener(null);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        final int measWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int measHeight = MeasureSpec.getSize(heightMeasureSpec);

        final int curWidth = getMeasuredWidth();
        final int curHeight = getMeasuredHeight();

        int finalWidth;
        if (widthMode == MeasureSpec.EXACTLY) {
            finalWidth = measWidth;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            // Cap width at max width.
            finalWidth = Math.min(measWidth, curWidth);
        } else {
            // MeasWidth is meaningless. Stay with current width.
            finalWidth = curWidth;
        }

        // Set height based on width + height constraints.
        int finalHeight = (int) (ASPECT_MULTIPLIER_WIDTH_TO_HEIGHT * finalWidth);

        // Check if the layout is giving us bounds smaller than we want, conform to those if needed.
        if (heightMode == MeasureSpec.EXACTLY && measHeight < finalHeight) {
            finalHeight = measHeight;
            finalWidth = (int) (ASPECT_MULTIPLIER_HEIGHT_TO_WIDTH * finalHeight);
        }

        if (Math.abs(finalHeight - curHeight) >= 2
                || Math.abs(finalWidth - curWidth) >= 2) {
            MoPubLog.v(String.format("Resetting mediaLayout size to w: %d h: %d", finalWidth, finalHeight));
            getLayoutParams().width = finalWidth;
            getLayoutParams().height = finalHeight;
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setMainImageDrawable(@NonNull Drawable drawable) {
        Preconditions.checkNotNull(drawable);
        mMainImageView.setImageDrawable(drawable);
    }

    public void resetProgress() {
        if (mVideoProgress != null) {
            mVideoProgress.reset();
        }
    }

    public void updateProgress(final int progressTenthPercentage) {
        if (mVideoProgress != null) {
            mVideoProgress.updateProgress(progressTenthPercentage);
        }
    }

    public TextureView getTextureView() {
        return mVideoTextureView;
    }

    public void setMode(@NonNull final Mode mode) {
        Preconditions.checkNotNull(mode);
        mMode = mode;
        post(new Runnable() {
            @Override
            public void run() {
                updateViewState();
            }
        });
    }

    @Nullable
    public ImageView getMainImageView() {
        return mMainImageView;
    }

    public void setMuteControlClickListener(@Nullable OnClickListener muteControlListener) {
        if (mMuteControl != null) {
            mMuteControl.setOnClickListener(muteControlListener);
        }
    }

    public void setPlayButtonClickListener(@Nullable OnClickListener playButtonListener) {
        if (mPlayButton != null && mOverlay != null) {
            mOverlay.setOnClickListener(playButtonListener);
            mPlayButton.setOnClickListener(playButtonListener);
        }
    }

    public void setVideoClickListener(@Nullable OnClickListener videoClickListener) {
        if (mVideoTextureView != null) {
            mVideoTextureView.setOnClickListener(videoClickListener);
        }
    }

    public void setMuteState(@NonNull final MuteState muteState) {
        Preconditions.checkNotNull(muteState);
        if (muteState == mMuteState) {
            return;
        }
        mMuteState = muteState;
        if (mMuteControl != null) {
            switch (mMuteState) {
                case MUTED:
                    mMuteControl.setImageDrawable(mMutedDrawable);
                    break;
                case UNMUTED:
                default:
                    mMuteControl.setImageDrawable(mUnmutedDrawable);
            }
        }
    }

    private void updateViewState() {
        switch (mMode) {
            case IMAGE:
                setMainImageVisibility(VISIBLE);
                setLoadingSpinnerVisibility(INVISIBLE);
                setVideoControlVisibility(INVISIBLE);
                setPlayButtonVisibility(INVISIBLE);
                break;
            case LOADING:
                setMainImageVisibility(VISIBLE);
                setLoadingSpinnerVisibility(VISIBLE);
                setVideoControlVisibility(INVISIBLE);
                setPlayButtonVisibility(INVISIBLE);
                break;
            case BUFFERING:
                setMainImageVisibility(INVISIBLE);
                setLoadingSpinnerVisibility(VISIBLE);
                setVideoControlVisibility(VISIBLE);
                setPlayButtonVisibility(INVISIBLE);
            case PLAYING:
                setMainImageVisibility(INVISIBLE);
                setLoadingSpinnerVisibility(INVISIBLE);
                setVideoControlVisibility(VISIBLE);
                setPlayButtonVisibility(INVISIBLE);
                break;
            case PAUSED:
                setMainImageVisibility(INVISIBLE);
                setLoadingSpinnerVisibility(INVISIBLE);
                setVideoControlVisibility(VISIBLE);
                setPlayButtonVisibility(VISIBLE);
                break;
            case FINISHED:
                setMainImageVisibility(VISIBLE);
                setLoadingSpinnerVisibility(INVISIBLE);
                setVideoControlVisibility(INVISIBLE);
                setPlayButtonVisibility(VISIBLE);
                break;
            default:
                break;
        }
    }

    private void setMainImageVisibility(final int visibility) {
        mMainImageView.setVisibility(visibility);
    }

    private void setLoadingSpinnerVisibility(final int visibility) {
        if (mLoadingSpinner != null) {
            mLoadingSpinner.setVisibility(visibility);
        }

        if (mTopGradient != null) {
            mTopGradient.setVisibility(visibility);
        }
    }

    private void setVideoControlVisibility(final int visibility) {
        if (mBottomGradient != null) {
            mBottomGradient.setVisibility(visibility);
        }

        if (mVideoProgress != null) {
            mVideoProgress.setVisibility(visibility);
        }

        if (mMuteControl != null) {
            mMuteControl.setVisibility(visibility);
        }
    }

    private void setPlayButtonVisibility(final int visibility) {
        if (mPlayButton != null && mOverlay != null) {
            mPlayButton.setVisibility(visibility);
            mOverlay.setVisibility(visibility);
        }
    }
}
