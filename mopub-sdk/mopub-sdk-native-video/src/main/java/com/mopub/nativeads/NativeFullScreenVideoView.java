package com.mopub.nativeads;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Drawables;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.VastVideoProgressBarWidget;
import com.mopub.mobileads.resource.CloseButtonDrawable;
import com.mopub.mobileads.resource.CtaButtonDrawable;
import com.mopub.mobileads.resource.DrawableConstants;
import com.mopub.mobileads.resource.DrawableConstants.GradientStrip;

public class NativeFullScreenVideoView extends RelativeLayout {

    public enum Mode { LOADING, PLAYING, PAUSED, FINISHED }

    @VisibleForTesting @NonNull Mode mMode;
    private int mOrientation;

    // Views
    @NonNull private final ImageView mCachedVideoFrameView;
    @NonNull private final TextureView mVideoTexture;
    @NonNull private final ProgressBar mLoadingSpinner;
    @NonNull private final ImageView mBottomGradient;
    @NonNull private final ImageView mTopGradient;
    @NonNull private final VastVideoProgressBarWidget mVideoProgress;
    @NonNull private final View mOverlay;
    @NonNull private final ImageView mPlayButton;
    @NonNull private final ImageView mPrivacyInformationIcon;
    @NonNull private final ImageView mCtaButton;
    @NonNull private final ImageView mCloseControl;

    // Measurements
    @VisibleForTesting final int mCtaWidthPx;
    @VisibleForTesting final int mCtaHeightPx;
    @VisibleForTesting final int mCtaMarginPx;
    @VisibleForTesting final int mCloseControlSizePx;
    @VisibleForTesting final int mClosePaddingPx;
    @VisibleForTesting final int mPrivacyInformationIconSizePx;
    @VisibleForTesting final int mPlayControlSizePx;
    @VisibleForTesting final int mGradientStripHeightPx;

    public NativeFullScreenVideoView(@NonNull final Context context, int orientation, @Nullable String ctaText) {
        this(context, orientation, ctaText,
                new ImageView(context),
                new TextureView(context), new ProgressBar(context), new ImageView(context),
                new ImageView(context), new VastVideoProgressBarWidget(context), new View(context),
                new ImageView(context), new ImageView(context), new ImageView(context),
                new ImageView(context));
    }

    @VisibleForTesting
    NativeFullScreenVideoView(@NonNull final Context context, int orientation,
            @Nullable final String ctaText,
            @NonNull final ImageView cachedImageView,
            @NonNull final TextureView videoTexture,
            @NonNull final ProgressBar loadingSpinner, @NonNull final ImageView bottomGradient,
            @NonNull final ImageView topGradient,
            @NonNull final VastVideoProgressBarWidget videoProgress, @NonNull final View overlay,
            @NonNull final ImageView playButton, @NonNull final ImageView privacyInformationIcon,
            @NonNull final ImageView ctaButton, @NonNull final ImageView closeControl) {
        super(context);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(cachedImageView);
        Preconditions.checkNotNull(videoTexture);
        Preconditions.checkNotNull(loadingSpinner);
        Preconditions.checkNotNull(bottomGradient);
        Preconditions.checkNotNull(topGradient);
        Preconditions.checkNotNull(videoProgress);
        Preconditions.checkNotNull(overlay);
        Preconditions.checkNotNull(playButton);
        Preconditions.checkNotNull(privacyInformationIcon);
        Preconditions.checkNotNull(ctaButton);
        Preconditions.checkNotNull(closeControl);

        mOrientation = orientation;
        mMode = Mode.LOADING;

        mCtaWidthPx = Dips.asIntPixels(200, context);
        mCtaHeightPx = Dips.asIntPixels(42, context);
        mCtaMarginPx = Dips.asIntPixels(10, context);
        mCloseControlSizePx = Dips.asIntPixels(50, context);
        mClosePaddingPx = Dips.asIntPixels(8, context);
        mPrivacyInformationIconSizePx = Dips.asIntPixels(44, context);
        mPlayControlSizePx = Dips.asIntPixels(50, context);
        mGradientStripHeightPx = Dips.asIntPixels(45, context);

        // Instantiate and initialize the views.
        final RelativeLayout.LayoutParams videoTextureLayoutParams =
                new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT);
        videoTextureLayoutParams.addRule(CENTER_IN_PARENT);

        mVideoTexture = videoTexture;
        mVideoTexture.setId((int) Utils.generateUniqueId());
        mVideoTexture.setLayoutParams(videoTextureLayoutParams);
        addView(mVideoTexture);

        mCachedVideoFrameView = cachedImageView;
        mCachedVideoFrameView.setId((int) Utils.generateUniqueId());
        mCachedVideoFrameView.setLayoutParams(videoTextureLayoutParams);
        mCachedVideoFrameView.setBackgroundColor(Color.TRANSPARENT);
        addView(mCachedVideoFrameView);

        RelativeLayout.LayoutParams spinnerParams =
                new RelativeLayout.LayoutParams(mPlayControlSizePx, mPlayControlSizePx);
        spinnerParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mLoadingSpinner = loadingSpinner;
        mLoadingSpinner.setId((int) Utils.generateUniqueId());
        mLoadingSpinner.setBackground(new LoadingBackground(context));
        mLoadingSpinner.setLayoutParams(spinnerParams);
        mLoadingSpinner.setIndeterminate(true);
        addView(mLoadingSpinner);

        RelativeLayout.LayoutParams bottomGradientParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, mGradientStripHeightPx);
        bottomGradientParams.addRule(RelativeLayout.ALIGN_BOTTOM, mVideoTexture.getId());
        mBottomGradient = bottomGradient;
        mBottomGradient.setId((int) Utils.generateUniqueId());
        mBottomGradient.setLayoutParams(bottomGradientParams);
        final GradientDrawable bottomGradientDrawable = new GradientDrawable(Orientation.BOTTOM_TOP,
                new int[] {GradientStrip.START_COLOR, GradientStrip.END_COLOR});
        mBottomGradient.setImageDrawable(bottomGradientDrawable);
        addView(mBottomGradient);

        RelativeLayout.LayoutParams topGradientParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, mGradientStripHeightPx);
        topGradientParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mTopGradient = topGradient;
        mTopGradient.setId((int) Utils.generateUniqueId());
        mTopGradient.setLayoutParams(topGradientParams);
        final GradientDrawable topGradientDrawable = new GradientDrawable(Orientation.TOP_BOTTOM,
                new int[] {GradientStrip.START_COLOR, GradientStrip.END_COLOR});
        mTopGradient.setImageDrawable(topGradientDrawable);
        addView(mTopGradient);

        mVideoProgress = videoProgress;
        mVideoProgress.setId((int) Utils.generateUniqueId());
        mVideoProgress.setAnchorId(mVideoTexture.getId());
        mVideoProgress.calibrateAndMakeVisible(1000, 0);
        addView(mVideoProgress);

        final RelativeLayout.LayoutParams overlayParams =
                new LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        overlayParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mOverlay = overlay;
        mOverlay.setId((int) Utils.generateUniqueId());
        mOverlay.setLayoutParams(overlayParams);
        mOverlay.setBackgroundColor(DrawableConstants.TRANSPARENT_GRAY);
        addView(mOverlay);

        RelativeLayout.LayoutParams playButtonParams =
                new RelativeLayout.LayoutParams(mPlayControlSizePx, mPlayControlSizePx);
        playButtonParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        mPlayButton = playButton;
        mPlayButton.setId((int) Utils.generateUniqueId());
        mPlayButton.setLayoutParams(playButtonParams);
        mPlayButton.setImageDrawable(Drawables.NATIVE_PLAY.createDrawable(context));
        addView(mPlayButton);

        mPrivacyInformationIcon = privacyInformationIcon;
        mPrivacyInformationIcon.setId((int) Utils.generateUniqueId());
        mPrivacyInformationIcon.setImageDrawable(
                Drawables.NATIVE_PRIVACY_INFORMATION_ICON.createDrawable(context));
        mPrivacyInformationIcon.setPadding(mClosePaddingPx, mClosePaddingPx, mClosePaddingPx * 2,
                mClosePaddingPx * 2);
        addView(mPrivacyInformationIcon);

        CtaButtonDrawable ctaDrawable = new CtaButtonDrawable(context);
        if (!TextUtils.isEmpty(ctaText)) {
            ctaDrawable.setCtaText(ctaText);
        }
        mCtaButton = ctaButton;
        mCtaButton.setId((int) Utils.generateUniqueId());
        mCtaButton.setImageDrawable(ctaDrawable);
        addView(mCtaButton);

        mCloseControl = closeControl;
        mCloseControl.setId((int) Utils.generateUniqueId());
        mCloseControl.setImageDrawable(new CloseButtonDrawable());
        mCloseControl.setPadding(mClosePaddingPx * 3, mClosePaddingPx, mClosePaddingPx, mClosePaddingPx * 3);
        addView(mCloseControl);

        updateViewState();
    }

    public void resetProgress() {
        mVideoProgress.reset();
    }

    public void setMode(@NonNull final Mode mode) {
        Preconditions.checkNotNull(mode);
        if (mMode == mode) {
            return;
        }

        mMode = mode;
        updateViewState();
    }

    @NonNull
    public TextureView getTextureView() {
        return mVideoTexture;
    }

    public void setOrientation(final int orientation) {
        if (mOrientation == orientation) {
            return;
        }
        mOrientation = orientation;
        updateViewState();
    }

    public void setSurfaceTextureListener(
            @Nullable final SurfaceTextureListener surfaceTextureListener) {
        mVideoTexture.setSurfaceTextureListener(surfaceTextureListener);

        SurfaceTexture surfaceTexture = mVideoTexture.getSurfaceTexture();
        if (surfaceTexture != null && surfaceTextureListener != null) {
            surfaceTextureListener.onSurfaceTextureAvailable(
                    surfaceTexture, mVideoTexture.getWidth(), mVideoTexture.getHeight());
        }
    }

    public void setCloseControlListener(@Nullable OnClickListener closeControlListener) {
        mCloseControl.setOnClickListener(closeControlListener);
    }

    public void setPrivacyInformationClickListener(
            @Nullable OnClickListener privacyInformationListener) {
        mPrivacyInformationIcon.setOnClickListener(privacyInformationListener);
    }

    public void setCtaClickListener(@Nullable OnClickListener ctaListener) {
        mCtaButton.setOnClickListener(ctaListener);
    }

    public void setPlayControlClickListener(@Nullable OnClickListener playControlListener) {
        mPlayButton.setOnClickListener(playControlListener);
        mOverlay.setOnClickListener(playControlListener);
    }

    public void updateProgress(final int progressPercentage) {
        mVideoProgress.updateProgress(progressPercentage);
    }

    public void setCachedVideoFrame(@Nullable Bitmap cachedVideoFrame) {
        mCachedVideoFrameView.setImageBitmap(cachedVideoFrame);
    }

    private void updateViewState() {
        switch (mMode) {
            case LOADING:
                setCachedImageVisibility(VISIBLE);
                setLoadingSpinnerVisibility(VISIBLE);
                setVideoProgressVisibility(INVISIBLE);
                setPlayButtonVisibility(INVISIBLE);
                break;
            case PLAYING:
                setCachedImageVisibility(INVISIBLE);
                setLoadingSpinnerVisibility(INVISIBLE);
                setVideoProgressVisibility(VISIBLE);
                setPlayButtonVisibility(INVISIBLE);
                break;
            case PAUSED:
                setCachedImageVisibility(INVISIBLE);
                setLoadingSpinnerVisibility(INVISIBLE);
                setVideoProgressVisibility(VISIBLE);
                setPlayButtonVisibility(VISIBLE);
                break;
            case FINISHED:
                setCachedImageVisibility(VISIBLE);
                setLoadingSpinnerVisibility(INVISIBLE);
                setVideoProgressVisibility(INVISIBLE);
                setPlayButtonVisibility(VISIBLE);
                break;
            default:
                break;
        }

        updateVideoTextureLayout();
        updateControlLayouts();
    }

    private void setCachedImageVisibility(final int visibility) {
        mCachedVideoFrameView.setVisibility(visibility);
    }

    private void setLoadingSpinnerVisibility(final int visibility) {
        mLoadingSpinner.setVisibility(visibility);
    }

    private void setVideoProgressVisibility(final int visibility) {
        mVideoProgress.setVisibility(visibility);
    }

    private void setPlayButtonVisibility(final int visibility) {
        mPlayButton.setVisibility(visibility);
        mOverlay.setVisibility(visibility);
    }

    private void updateVideoTextureLayout() {
        final Configuration configuration = getContext().getResources().getConfiguration();
        ViewGroup.LayoutParams layoutParams = mVideoTexture.getLayoutParams();
        int newWidth = Dips.dipsToIntPixels((float) configuration.screenWidthDp, getContext());
        if (newWidth != layoutParams.width) {
            layoutParams.width = newWidth;
        }
        int newHeight = Dips.dipsToIntPixels((float) configuration.screenWidthDp * 9 / 16, getContext());
        if (newHeight != layoutParams.height) {
            layoutParams.height = newHeight;
        }
    }

    private void updateControlLayouts() {
        final RelativeLayout.LayoutParams ctaParams =
                new RelativeLayout.LayoutParams(mCtaWidthPx, mCtaHeightPx);
        ctaParams.setMargins(mCtaMarginPx, mCtaMarginPx, mCtaMarginPx, mCtaMarginPx);
        final RelativeLayout.LayoutParams privacyInformationIconParams =
                new RelativeLayout.LayoutParams(mPrivacyInformationIconSizePx,
                        mPrivacyInformationIconSizePx);
        final RelativeLayout.LayoutParams closeParams =
                new RelativeLayout.LayoutParams(mCloseControlSizePx, mCloseControlSizePx);


        switch (mOrientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                ctaParams.addRule(RelativeLayout.BELOW, mVideoTexture.getId());
                ctaParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

                privacyInformationIconParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                privacyInformationIconParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);

                closeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                closeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                ctaParams.addRule(RelativeLayout.ABOVE, mVideoProgress.getId());
                ctaParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

                privacyInformationIconParams.addRule(RelativeLayout.ALIGN_TOP, mVideoTexture.getId());
                privacyInformationIconParams.addRule(RelativeLayout.ALIGN_LEFT, mVideoTexture.getId());

                closeParams.addRule(RelativeLayout.ALIGN_TOP, mVideoTexture.getId());
                closeParams.addRule(RelativeLayout.ALIGN_RIGHT, mVideoTexture.getId());
                break;
            default:
                // Do nothing.
                break;
        }

        mCtaButton.setLayoutParams(ctaParams);
        mPrivacyInformationIcon.setLayoutParams(privacyInformationIconParams);
        mCloseControl.setLayoutParams(closeParams);
    }

    @VisibleForTesting
    static class LoadingBackground extends Drawable {
        @NonNull private final RectF mButtonRect;
        @NonNull private final Paint mPaint;
        @VisibleForTesting final int mCornerRadiusPx;

        LoadingBackground(@NonNull final Context context) {
            this(context, new RectF(), new Paint());
        }

        LoadingBackground(@NonNull final Context context,
                @NonNull final RectF rectF,
                @NonNull final Paint paint) {
            Preconditions.checkNotNull(context);
            Preconditions.checkNotNull(rectF);
            Preconditions.checkNotNull(paint);

            mButtonRect = rectF;
            mPaint = paint;
            mPaint.setColor(Color.BLACK);
            mPaint.setAlpha(128);
            mPaint.setAntiAlias(true);
            mCornerRadiusPx = Dips.asIntPixels(5, context);
        }

        @Override
        public void draw(Canvas canvas) {
            mButtonRect.set(getBounds());

            // Rounded rectangle background fill
            canvas.drawRoundRect(mButtonRect, mCornerRadiusPx, mCornerRadiusPx, mPaint);
        }

        @Override
        public void setAlpha(int alpha) { }

        @Override
        public void setColorFilter(ColorFilter cf) { }

        @Override
        public int getOpacity() {
            return PixelFormat.UNKNOWN;
        }
    }

    @Deprecated
    @VisibleForTesting
    ImageView getCtaButton() {
        return mCtaButton;
    }
}
