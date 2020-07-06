// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import android.content.Context;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.mopub.common.util.Dips;
import com.mopub.mobileads.BaseVideoViewController;
import com.mopub.mobileads.base.R;

import static android.view.View.GONE;
import static android.view.View.OnClickListener;
import static android.view.View.VISIBLE;
import static com.mopub.mobileads.BaseVideoPlayerActivity.VIDEO_URL;

public class MraidVideoViewController extends BaseVideoViewController {
    private static final float CLOSE_BUTTON_SIZE = 50f;
    private static final float CLOSE_BUTTON_PADDING = 8f;

    private final VideoView mVideoView;
    private ImageButton mCloseButton;
    private int mButtonPadding;
    private int mButtonSize;

    public MraidVideoViewController(final Context context,
            final Bundle intentExtras,
            final Bundle savedInstanceState,
            final BaseVideoViewControllerListener baseVideoViewControllerListener) {
        // No broadcast identifiers are used by MraidVideoViews.
        super(context, null, baseVideoViewControllerListener);

        mVideoView = new VideoView(context);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mCloseButton.setVisibility(VISIBLE);
                videoCompleted(true);
            }
        });

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                mCloseButton.setVisibility(VISIBLE);
                videoError(false);

                return false;
            }
        });

        mVideoView.setVideoPath(intentExtras.getString(VIDEO_URL));
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        mButtonSize = Dips.asIntPixels(CLOSE_BUTTON_SIZE, getContext());
        mButtonPadding = Dips.asIntPixels(CLOSE_BUTTON_PADDING, getContext());
        createInterstitialCloseButton();
        mCloseButton.setVisibility(GONE);
        mVideoView.start();
    }

    @Override
    protected VideoView getVideoView() {
        return mVideoView;
    }

    @Override
    protected void onDestroy() {}

    @Override
    protected void onPause() {}

    @Override
    protected void onResume() {}

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {}

    @Override
    protected void onConfigurationChanged(final Configuration newConfig) {}

    @Override
    protected void onBackPressed() {}

    private void createInterstitialCloseButton() {
        mCloseButton = new ImageButton(getContext());
        //noinspection deprecation
        mCloseButton.setBackgroundDrawable(null);
        mCloseButton.setImageDrawable(
                ContextCompat.getDrawable(getContext(), R.drawable.ic_mopub_close_button));
        mCloseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                getBaseVideoViewControllerListener().onFinish();
            }
        });

        RelativeLayout.LayoutParams buttonLayout = new RelativeLayout.LayoutParams(mButtonSize, mButtonSize);
        buttonLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        buttonLayout.setMargins(mButtonPadding, 0, mButtonPadding, 0);
        getLayout().addView(mCloseButton, buttonLayout);
    }
}
