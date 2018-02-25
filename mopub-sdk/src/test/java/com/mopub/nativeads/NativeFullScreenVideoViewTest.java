package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Dips;
import com.mopub.common.util.Drawables;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.VastVideoProgressBarWidget;
import com.mopub.mobileads.resource.CloseButtonDrawable;
import com.mopub.mobileads.resource.CtaButtonDrawable;
import com.mopub.nativeads.NativeFullScreenVideoView.LoadingBackground;
import com.mopub.nativeads.NativeFullScreenVideoView.Mode;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class NativeFullScreenVideoViewTest {

    private NativeFullScreenVideoView subject;
    private Context context;

    private final int screenWidthDp = 410;
    private final int screenHeightDp = 730;
    private int videoWidthLandscapePx;
    private int videoHeightLandscapePx;
    private int videoWidthPortraitPx;
    private int videoHeightPortraitPx;

    private TextureView spyVideoTexture;
    private ImageView spyCachedImage;
    private ProgressBar spyLoadingSpinner;
    private ImageView spyBottomGradient;
    private ImageView spyTopGradient;
    private VastVideoProgressBarWidget spyVideoProgress;
    private View spyOverlay;
    private ImageView spyPlayButton;
    private ImageView spyPrivacyInformationIcon;
    private ImageView spyCtaButton;
    private ImageView spyCloseControl;

    @Mock TextureView.SurfaceTextureListener mockSurfaceTextureListener;
    @Mock SurfaceTexture mockSurfaceTexture;
    @Mock RectF mockRectF;
    @Mock Paint mockPaint;
    private Configuration configuration;

    @Before
    public void setUp() {
        context = Robolectric.buildActivity(Activity.class).create().get();

        configuration = context.getResources().getConfiguration();
        configuration.screenWidthDp = screenWidthDp;
        configuration.screenHeightDp = screenHeightDp;

        videoWidthLandscapePx = Dips.dipsToIntPixels((float) screenWidthDp, context);
        videoHeightLandscapePx = Dips.dipsToIntPixels((float) screenWidthDp * 9 / 16, context);
        videoWidthPortraitPx = Dips.dipsToIntPixels((float) screenHeightDp, context);
        videoHeightPortraitPx = Dips.dipsToIntPixels((float) screenHeightDp * 9 / 16, context);

        spyCachedImage = spy(new ImageView(context));
        spyVideoTexture = spy(new TextureView(context));
        spyLoadingSpinner = spy(new ProgressBar(context));
        spyBottomGradient = spy(new ImageView(context));
        spyTopGradient = spy(new ImageView(context));
        spyVideoProgress = spy(new VastVideoProgressBarWidget(context));
        spyOverlay = spy(new View(context));
        spyPlayButton = spy(new ImageView(context));
        spyPrivacyInformationIcon = spy(new ImageView(context));
        spyCtaButton = spy(new ImageView(context));
        spyCloseControl = spy(new ImageView(context));

        subject = new NativeFullScreenVideoView(context, Configuration.ORIENTATION_LANDSCAPE, "Learn More",
                spyCachedImage,
                spyVideoTexture, spyLoadingSpinner, spyBottomGradient, spyTopGradient,
                spyVideoProgress, spyOverlay, spyPlayButton, spyPrivacyInformationIcon, spyCtaButton,
                spyCloseControl);

    }

    @Test
    public void constructor_shouldInitializeModeToLoading() throws Exception {
        assertThat(subject.mMode).isEqualTo(Mode.LOADING);
    }

    @Test
    public void constructor_shouldSetMeasurementsCorrectly() throws Exception {
        assertThat(subject.mCtaWidthPx).isEqualTo(200);
        assertThat(subject.mCtaHeightPx).isEqualTo(42);
        assertThat(subject.mCtaMarginPx).isEqualTo(10);
        assertThat(subject.mCloseControlSizePx).isEqualTo(50);
        assertThat(subject.mClosePaddingPx).isEqualTo(8);
        assertThat(subject.mPrivacyInformationIconSizePx).isEqualTo(44);
        assertThat(subject.mPlayControlSizePx).isEqualTo(50);
        assertThat(subject.mGradientStripHeightPx).isEqualTo(45);
    }

    @Test
    public void constructor_shouldAddVideoTextureToLayout_shouldSetWidthAndHeight() throws Exception {
        assertThat(subject.findViewById(spyVideoTexture.getId())).isEqualTo(spyVideoTexture);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyVideoTexture.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(videoWidthLandscapePx);
        assertThat(layoutParams.height).isEqualTo(videoHeightLandscapePx);
        assertThat(layoutParams.getRules()[RelativeLayout.CENTER_IN_PARENT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void constructor_shouldAddLoadingSpinnerToLayout() throws Exception {
        assertThat(subject.findViewById(spyLoadingSpinner.getId())).isEqualTo(spyLoadingSpinner);
        assertThat(spyLoadingSpinner.isIndeterminate()).isTrue();
        assertThat(spyLoadingSpinner.getParent()).isEqualTo(subject);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyLoadingSpinner.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(subject.mPlayControlSizePx);
        assertThat(layoutParams.height).isEqualTo(subject.mPlayControlSizePx);
        assertThat(layoutParams.getRules()[RelativeLayout.CENTER_IN_PARENT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void constructor_shouldAddBottomGradientToLayout() throws Exception {
        assertThat(subject.findViewById(spyBottomGradient.getId())).isEqualTo(spyBottomGradient);
        GradientDrawable gradientDrawable = (GradientDrawable) spyBottomGradient.getDrawable();
        assertThat(gradientDrawable.getOrientation())
                .isEqualTo(GradientDrawable.Orientation.BOTTOM_TOP);
        assertThat(spyBottomGradient.getParent()).isEqualTo(subject);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyBottomGradient.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
        assertThat(layoutParams.height).isEqualTo(subject.mGradientStripHeightPx);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_BOTTOM])
                .isEqualTo(spyVideoTexture.getId());
    }

    @Test
    public void constructor_shouldAddTopGradientToLayout() throws Exception {
        assertThat(subject.findViewById(spyTopGradient.getId())).isEqualTo(spyTopGradient);
        GradientDrawable gradientDrawable = (GradientDrawable) spyTopGradient.getDrawable();
        assertThat(gradientDrawable.getOrientation())
                .isEqualTo(GradientDrawable.Orientation.TOP_BOTTOM);
        assertThat(spyTopGradient.getParent()).isEqualTo(subject);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyTopGradient.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
        assertThat(layoutParams.height).isEqualTo(subject.mGradientStripHeightPx);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_PARENT_TOP])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void constructor_shouldAddVideoProgressToLayout() throws Exception {
        assertThat(subject.findViewById(spyVideoProgress.getId())).isEqualTo(spyVideoProgress);
        verify(spyVideoProgress).setAnchorId(spyVideoTexture.getId());
        verify(spyVideoProgress).calibrateAndMakeVisible(1000, 0);
    }

    @Test
    public void constructor_shouldAddOverlayToLayout() throws Exception {
        assertThat(subject.findViewById(spyOverlay.getId())).isEqualTo(spyOverlay);
        verify(spyOverlay).setBackgroundColor(0x88000000);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyOverlay.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
        assertThat(layoutParams.height).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
        assertThat(layoutParams.getRules()[RelativeLayout.CENTER_IN_PARENT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void constructor_shouldAddPlayButtonToLayout() throws Exception {
        assertThat(subject.findViewById(spyOverlay.getId())).isEqualTo(spyOverlay);
        verify(spyOverlay).setBackgroundColor(0x88000000);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyOverlay.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
        assertThat(layoutParams.height).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
        assertThat(layoutParams.getRules()[RelativeLayout.CENTER_IN_PARENT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void constructor_shouldAddPrivacyInformationIconToLayout() throws Exception {
        assertThat(subject.findViewById(spyPrivacyInformationIcon.getId())).isEqualTo(
                spyPrivacyInformationIcon);
        verify(spyPrivacyInformationIcon).setImageDrawable(
                Drawables.NATIVE_PRIVACY_INFORMATION_ICON.createDrawable(context));
        assertThat(spyPrivacyInformationIcon.getPaddingLeft()).isEqualTo(subject.mClosePaddingPx);
        assertThat(spyPrivacyInformationIcon.getPaddingTop()).isEqualTo(subject.mClosePaddingPx);
        assertThat(spyPrivacyInformationIcon.getPaddingRight()).isEqualTo(
                subject.mClosePaddingPx * 2);
        assertThat(spyPrivacyInformationIcon.getPaddingBottom()).isEqualTo(
                subject.mClosePaddingPx * 2);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyPrivacyInformationIcon.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(subject.mPrivacyInformationIconSizePx);
        assertThat(layoutParams.height).isEqualTo(subject.mPrivacyInformationIconSizePx);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_TOP])
                .isEqualTo(spyVideoTexture.getId());
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_LEFT])
                .isEqualTo(spyVideoTexture.getId());
    }

    @Test
    public void constructor_shouldAddCtaButtonToLayout() throws Exception {
        assertThat(subject.findViewById(spyCtaButton.getId())).isEqualTo(spyCtaButton);
        CtaButtonDrawable ctaButtonDrawable = (CtaButtonDrawable) spyCtaButton.getDrawable();
        assertThat(ctaButtonDrawable.getCtaText()).isEqualTo("Learn More");

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyCtaButton.getLayoutParams();
        assertThat(layoutParams.leftMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.topMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.rightMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.bottomMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.width).isEqualTo(subject.mCtaWidthPx);
        assertThat(layoutParams.height).isEqualTo(subject.mCtaHeightPx);
        assertThat(layoutParams.getRules()[RelativeLayout.ABOVE])
                .isEqualTo(spyVideoProgress.getId());
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_PARENT_RIGHT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void constructor_shouldAddCloseControlToLayout() throws Exception {
        assertThat(subject.findViewById(spyCloseControl.getId())).isEqualTo(spyCloseControl);
        verify(spyCloseControl).setImageDrawable(any(CloseButtonDrawable.class));
        assertThat(spyCloseControl.getPaddingLeft()).isEqualTo(subject.mClosePaddingPx * 3);
        assertThat(spyCloseControl.getPaddingTop()).isEqualTo(subject.mClosePaddingPx);
        assertThat(spyCloseControl.getPaddingRight()).isEqualTo(subject.mClosePaddingPx);
        assertThat(spyCloseControl.getPaddingBottom()).isEqualTo(subject.mClosePaddingPx * 3);

                RelativeLayout.LayoutParams layoutParams =
                        (RelativeLayout.LayoutParams) spyCloseControl.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(subject.mCloseControlSizePx);
        assertThat(layoutParams.height).isEqualTo(subject.mCloseControlSizePx);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_TOP])
                .isEqualTo(spyVideoTexture.getId());
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_RIGHT])
                .isEqualTo(spyVideoTexture.getId());
    }

    @Test
    public void resetProgress_shouldCallVideoProgressReset() throws Exception {
        subject.resetProgress();
        verify(spyVideoProgress).reset();
    }

    @Test
    public void setMode_withModeLoading_shouldUpdateVisibilityAppropriately() throws Exception {
        // We init in loading state, so we need to get out of loading.
        subject.setMode(Mode.PAUSED);
        // Reset our spies.
        reset(spyVideoTexture, spyLoadingSpinner, spyBottomGradient, spyTopGradient,
                spyVideoProgress, spyOverlay, spyPlayButton, spyPrivacyInformationIcon, spyCtaButton,
                spyCloseControl);
        subject.setMode(Mode.LOADING);
        verify(spyLoadingSpinner).setVisibility(View.VISIBLE);
        verify(spyVideoProgress).setVisibility(View.INVISIBLE);
        verify(spyPlayButton).setVisibility(View.INVISIBLE);
        verify(spyOverlay).setVisibility(View.INVISIBLE);
    }

    @Test
    public void setMode_withModePlaying_shouldUpdateVisibilityAppropriately() throws Exception {
        // Reset our spies because the constructor calls updateViewState
        reset(spyVideoTexture, spyLoadingSpinner, spyBottomGradient, spyTopGradient,
                spyVideoProgress, spyOverlay, spyPlayButton, spyPrivacyInformationIcon, spyCtaButton,
                spyCloseControl);
        subject.setMode(Mode.PLAYING);
        verify(spyLoadingSpinner).setVisibility(View.INVISIBLE);
        verify(spyVideoProgress).setVisibility(View.VISIBLE);
        verify(spyPlayButton).setVisibility(View.INVISIBLE);
        verify(spyOverlay).setVisibility(View.INVISIBLE);
    }

    @Test
    public void setMode_withModePaused_shouldUpdateVisibilityAppropriately() throws Exception {
        // Reset our spies because the constructor calls updateViewState
        reset(spyVideoTexture, spyLoadingSpinner, spyBottomGradient, spyTopGradient,
                spyVideoProgress, spyOverlay, spyPlayButton, spyPrivacyInformationIcon, spyCtaButton,
                spyCloseControl);
        subject.setMode(Mode.PAUSED);
        verify(spyLoadingSpinner).setVisibility(View.INVISIBLE);
        verify(spyVideoProgress).setVisibility(View.VISIBLE);
        verify(spyPlayButton).setVisibility(View.VISIBLE);
        verify(spyOverlay).setVisibility(View.VISIBLE);
    }

    @Test
    public void setMode_withModeFinished_shouldUpdateVisibilityAppropriately() throws Exception {
        // Reset our spies because the constructor calls updateViewState
        reset(spyVideoTexture, spyLoadingSpinner, spyBottomGradient, spyTopGradient,
                spyVideoProgress, spyOverlay, spyPlayButton, spyPrivacyInformationIcon, spyCtaButton,
                spyCloseControl);
        subject.setMode(Mode.FINISHED);
        verify(spyLoadingSpinner).setVisibility(View.INVISIBLE);
        verify(spyVideoProgress).setVisibility(View.INVISIBLE);
        verify(spyPlayButton).setVisibility(View.VISIBLE);
        verify(spyOverlay).setVisibility(View.VISIBLE);
    }

    @Test
    public void setOrientation_withLandscape_shouldSetWidthAndHeightOfVideoTextureAppropriately() throws Exception {
        configuration.screenWidthDp = screenWidthDp;
        configuration.screenHeightDp = screenHeightDp;

        subject.setOrientation(Configuration.ORIENTATION_LANDSCAPE);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyVideoTexture.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(videoWidthLandscapePx);
        assertThat(layoutParams.height).isEqualTo(videoHeightLandscapePx);
        assertThat(layoutParams.getRules()[RelativeLayout.CENTER_IN_PARENT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void setOrientation_withPortrait_shouldSetWidthAndHeightOfVideoTextureAppropriately() throws Exception {
        configuration.screenWidthDp = screenHeightDp;
        configuration.screenHeightDp = screenWidthDp;

        subject.setOrientation(Configuration.ORIENTATION_PORTRAIT);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyVideoTexture.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(videoWidthPortraitPx);
        assertThat(layoutParams.height).isEqualTo(videoHeightPortraitPx);
        assertThat(layoutParams.getRules()[RelativeLayout.CENTER_IN_PARENT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void setOrientation_withLandscape_shouldSetControlLayoutsAppropriately() throws Exception {
        subject.setOrientation(Configuration.ORIENTATION_LANDSCAPE);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyPrivacyInformationIcon.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(subject.mPrivacyInformationIconSizePx);
        assertThat(layoutParams.height).isEqualTo(subject.mPrivacyInformationIconSizePx);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_TOP])
                .isEqualTo(spyVideoTexture.getId());
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_LEFT])
                .isEqualTo(spyVideoTexture.getId());

        assertThat(subject.findViewById(spyCtaButton.getId())).isEqualTo(spyCtaButton);
        CtaButtonDrawable ctaButtonDrawable = (CtaButtonDrawable) spyCtaButton.getDrawable();
        assertThat(ctaButtonDrawable.getCtaText()).isEqualTo("Learn More");

        layoutParams = (RelativeLayout.LayoutParams) spyCtaButton.getLayoutParams();
        assertThat(layoutParams.leftMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.topMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.rightMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.bottomMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.width).isEqualTo(subject.mCtaWidthPx);
        assertThat(layoutParams.height).isEqualTo(subject.mCtaHeightPx);
        assertThat(layoutParams.getRules()[RelativeLayout.ABOVE])
                .isEqualTo(spyVideoProgress.getId());
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_PARENT_RIGHT])
                .isEqualTo(RelativeLayout.TRUE);

        layoutParams = (RelativeLayout.LayoutParams) spyCloseControl.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(subject.mCloseControlSizePx);
        assertThat(layoutParams.height).isEqualTo(subject.mCloseControlSizePx);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_TOP])
                .isEqualTo(spyVideoTexture.getId());
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_RIGHT])
                .isEqualTo(spyVideoTexture.getId());
    }

    @Test
    public void setOrientation_withPortrait_shouldSetControlLayoutsAppropriately() throws Exception {
        subject.setOrientation(Configuration.ORIENTATION_PORTRAIT);

        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams) spyPrivacyInformationIcon.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(subject.mPrivacyInformationIconSizePx);
        assertThat(layoutParams.height).isEqualTo(subject.mPrivacyInformationIconSizePx);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_PARENT_TOP])
                .isEqualTo(RelativeLayout.TRUE);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_PARENT_LEFT])
                .isEqualTo(RelativeLayout.TRUE);

        layoutParams = (RelativeLayout.LayoutParams) spyCtaButton.getLayoutParams();
        assertThat(layoutParams.leftMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.topMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.rightMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.bottomMargin).isEqualTo(subject.mCtaMarginPx);
        assertThat(layoutParams.width).isEqualTo(subject.mCtaWidthPx);
        assertThat(layoutParams.height).isEqualTo(subject.mCtaHeightPx);
        assertThat(layoutParams.getRules()[RelativeLayout.BELOW])
                .isEqualTo(spyVideoTexture.getId());
        assertThat(layoutParams.getRules()[RelativeLayout.CENTER_HORIZONTAL])
                .isEqualTo(RelativeLayout.TRUE);

        layoutParams = (RelativeLayout.LayoutParams) spyCloseControl.getLayoutParams();
        assertThat(layoutParams.width).isEqualTo(subject.mCloseControlSizePx);
        assertThat(layoutParams.height).isEqualTo(subject.mCloseControlSizePx);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_PARENT_TOP])
                .isEqualTo(RelativeLayout.TRUE);
        assertThat(layoutParams.getRules()[RelativeLayout.ALIGN_PARENT_RIGHT])
                .isEqualTo(RelativeLayout.TRUE);
    }

    @Test
    public void setSurfaceTextureListener_withNullSurfaceTexture_shouldSetSurfaceTexture_shouldNotCallListener() throws Exception {
        when(spyVideoTexture.getSurfaceTexture()).thenReturn(null);

        subject.setSurfaceTextureListener(mockSurfaceTextureListener);

        verify(spyVideoTexture).setSurfaceTextureListener(mockSurfaceTextureListener);
        verify(mockSurfaceTextureListener, never()).
                onSurfaceTextureAvailable(any(SurfaceTexture.class), anyInt(), anyInt());
    }

    @Test
    public void setSurfaceTextureListener_withNonNullSurfaceTexture_shouldSetSurfaceTexture_shouldCallListener() throws Exception {
        when(spyVideoTexture.getSurfaceTexture()).thenReturn(mockSurfaceTexture);
        when(spyVideoTexture.getWidth()).thenReturn(videoWidthLandscapePx);
        when(spyVideoTexture.getHeight()).thenReturn(videoHeightLandscapePx);

        subject.setSurfaceTextureListener(mockSurfaceTextureListener);

        verify(spyVideoTexture).setSurfaceTextureListener(mockSurfaceTextureListener);
        verify(mockSurfaceTextureListener).
                onSurfaceTextureAvailable(mockSurfaceTexture, videoWidthLandscapePx,
                        videoHeightLandscapePx);
    }

    @Test
    public void updateProgress_shouldUpdateVideoProgress() throws Exception {
        subject.updateProgress(100);
        verify(spyVideoProgress).updateProgress(100);
    }

    @Test
    public void LoadingBackground_constructor_shouldInitializePaint() throws Exception {
        new LoadingBackground(context, mockRectF, mockPaint);

        verify(mockPaint).setColor(Color.BLACK);
        verify(mockPaint).setAlpha(128);
        verify(mockPaint).setAntiAlias(true);
    }

    @Test
    public void LoadingBackground_constructor_shouldInitializeMeasurements() throws Exception {
        LoadingBackground loadingBackground = new LoadingBackground(context, mockRectF, mockPaint);

        assertThat(loadingBackground.mCornerRadiusPx).isEqualTo(5);
    }

    @Test
    public void LoadingBackground_getOpacity_shouldReturn0() throws Exception {
        assertThat(new LoadingBackground(context, mockRectF, mockPaint).getOpacity())
                .isEqualTo(0);
    }
}
