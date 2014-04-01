/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.mopub.mobileads.util.HttpUtils;

import java.util.*;
import java.util.concurrent.*;

import static com.mopub.mobileads.MraidVideoPlayerActivity.VIDEO_URL;

class VastVideoView extends BaseVideoView {
    static final String VIDEO_START_TRACKERS = "video_start_trackers";
    static final String VIDEO_FIRST_QUARTER_TRACKERS = "video_first_quarter_trackers";
    static final String VIDEO_MID_POINT_TRACKERS = "video_mid_point_trackers";
    static final String VIDEO_THIRD_QUARTER_TRACKERS = "video_third_quarter_trackers";
    static final String VIDEO_COMPLETE_TRACKERS = "video_complete_trackers";
    static final String VIDEO_IMPRESSION_TRACKERS = "video_impression_trackers";
    static final String VIDEO_CLICK_THROUGH_URL = "video_click_through_url";
    static final String VIDEO_CLICK_THROUGH_TRACKERS = "video_click_through_trackers";

    private static final float FIRST_QUARTER_MARKER = 0.25f;
    private static final float MID_POINT_MARKER = 0.50f;
    private static final float THIRD_QUARTER_MARKER = 0.75f;
    private static final long VIDEO_PROGRESS_TIMER_CHECKER_DELAY = 50;

    private static final ThreadPoolExecutor sThreadPoolExecutor = new ThreadPoolExecutor(10, 50, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    public static final int MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON = 16 * 1000;
    public static final int DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON = 5 * 1000;
    private final BaseVideoViewListener mBaseVideoViewListener;

    private String mVideoUrl;
    private ArrayList<String> mVideoStartTrackers;
    private ArrayList<String> mFirstQuarterTrackers;
    private ArrayList<String> mMidPointTrackers;
    private ArrayList<String> mThirdQuarterTrackers;
    private ArrayList<String> mCompletionTrackers;
    private ArrayList<String> mImpressionTrackers;
    private String mClickThroughUrl;
    private ArrayList<String> mClickThroughTrackers;
    private Handler mHandler;
    private Runnable mVideoProgressCheckerRunnable;
    private boolean mIsVideoProgressShouldBeChecked;
    private int mShowCloseButtonDelay = DEFAULT_VIDEO_DURATION_FOR_CLOSE_BUTTON;

    private boolean mShowCloseButtonEventFired;
    private boolean mIsFirstMarkHit;
    private boolean mIsSecondMarkHit;
    private boolean mIsThirdMarkHit;
    private int mSeekerPositionOnPause;
    private boolean mIsVideoFinishedPlaying;

    public VastVideoView(final Context context, final Intent intent, final BaseVideoViewListener baseVideoViewListener) {
        super(context);

        mBaseVideoViewListener = baseVideoViewListener;
        mHandler = new Handler();
        mIsVideoProgressShouldBeChecked = true;
        mSeekerPositionOnPause = -1;

        mVideoUrl = intent.getStringExtra(VIDEO_URL);
        mVideoStartTrackers = intent.getStringArrayListExtra(VIDEO_START_TRACKERS);
        mFirstQuarterTrackers = intent.getStringArrayListExtra(VIDEO_FIRST_QUARTER_TRACKERS);
        mMidPointTrackers = intent.getStringArrayListExtra(VIDEO_MID_POINT_TRACKERS);
        mThirdQuarterTrackers = intent.getStringArrayListExtra(VIDEO_THIRD_QUARTER_TRACKERS);
        mCompletionTrackers = intent.getStringArrayListExtra(VIDEO_COMPLETE_TRACKERS);
        mImpressionTrackers = intent.getStringArrayListExtra(VIDEO_IMPRESSION_TRACKERS);
        mClickThroughUrl = intent.getStringExtra(VIDEO_CLICK_THROUGH_URL);
        mClickThroughTrackers = intent.getStringArrayListExtra(VIDEO_CLICK_THROUGH_TRACKERS);

        setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (getDuration() < MAX_VIDEO_DURATION_FOR_CLOSE_BUTTON) {
                    mShowCloseButtonDelay = getDuration();
                }
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null && audioManager.getRingerMode() != AudioManager.RINGER_MODE_NORMAL) {
                    mp.setVolume(0.0f, 0.0f);
                }
            }
        });

        setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP && shouldAllowClickThrough()) {
                    pingOnBackgroundThread(mClickThroughTrackers);

                    if (mBaseVideoViewListener != null) {
                        mBaseVideoViewListener.videoClicked();
                    }

                    Intent mraidBrowserIntent = new Intent(context, MraidBrowser.class);
                    mraidBrowserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    mraidBrowserIntent.putExtra(MraidBrowser.URL_EXTRA, mClickThroughUrl);
                    context.startActivity(mraidBrowserIntent);
                }

                return true;
            }
        });

        setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopProgressChecker();
                fireShowCloseButtonEvent();
                if (mBaseVideoViewListener != null) {
                    mBaseVideoViewListener.videoCompleted(false);
                }
                pingOnBackgroundThread(mCompletionTrackers);

                mIsVideoFinishedPlaying = true;
            }
        });

        setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
                stopProgressChecker();

                if (baseVideoViewListener != null) {
                    baseVideoViewListener.videoError(false);
                }

                return false;
            }
        });

        mVideoProgressCheckerRunnable = new Runnable() {
            @Override
            public void run() {
                float videoLength = getDuration();
                if (videoLength > 0) {
                    float progressPercentage = getCurrentPosition() / videoLength;

                    if (progressPercentage > FIRST_QUARTER_MARKER && !mIsFirstMarkHit) {
                        mIsFirstMarkHit = true;
                        pingOnBackgroundThread(mFirstQuarterTrackers);
                    }

                    if (progressPercentage > MID_POINT_MARKER && !mIsSecondMarkHit) {
                        mIsSecondMarkHit = true;
                        pingOnBackgroundThread(mMidPointTrackers);
                    }

                    if (progressPercentage > THIRD_QUARTER_MARKER && !mIsThirdMarkHit) {
                        mIsThirdMarkHit = true;
                        pingOnBackgroundThread(mThirdQuarterTrackers);
                    }

                    if (shouldShowCloseButton()) {
                        fireShowCloseButtonEvent();
                    }
                }

                if (mIsVideoProgressShouldBeChecked) {
                    mHandler.postDelayed(mVideoProgressCheckerRunnable, VIDEO_PROGRESS_TIMER_CHECKER_DELAY);
                }
            }
        };

        setVideoPath(mVideoUrl);
        requestFocus();

        pingOnBackgroundThread(mVideoStartTrackers);
        pingOnBackgroundThread(mImpressionTrackers);

        mHandler.post(mVideoProgressCheckerRunnable);
    }

    private void fireShowCloseButtonEvent() {
        mShowCloseButtonEventFired = true;

        if (mBaseVideoViewListener != null) {
            mBaseVideoViewListener.showCloseButton();
        }
    }

    private boolean shouldShowCloseButton() {
        return !mShowCloseButtonEventFired && getCurrentPosition() > mShowCloseButtonDelay;
    }

    private boolean shouldAllowClickThrough(){
        return mShowCloseButtonEventFired;
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopProgressChecker();

        mSeekerPositionOnPause = getCurrentPosition();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsVideoProgressShouldBeChecked = true;
        mHandler.post(mVideoProgressCheckerRunnable);

        seekTo(mSeekerPositionOnPause);

        if (!mIsVideoFinishedPlaying) {
            start();
        }
    }

    private void pingOnBackgroundThread(List<String> urls) {
        if (urls == null) {
            return;
        }

        for (final String url : urls) {
            sThreadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        HttpUtils.ping(url);
                    } catch (Exception e) {
                        Log.d("MoPub", "Unable to track video impression url: " + url);
                    }
                }
            });
        }
    }

    private void stopProgressChecker() {
        mIsVideoProgressShouldBeChecked = false;
        mHandler.removeCallbacks(mVideoProgressCheckerRunnable);
    }

    @Deprecated // for testing
    void setIsVideoProgressShouldBeChecked(boolean value) {
        mIsVideoProgressShouldBeChecked = value;
    }

    @Deprecated // for testing
    int getShowCloseButtonDelay() {
        return mShowCloseButtonDelay;
    }

    @Deprecated // for testing
    void setCloseButtonVisible(boolean visible) {
        mShowCloseButtonEventFired = visible;
    }
}
