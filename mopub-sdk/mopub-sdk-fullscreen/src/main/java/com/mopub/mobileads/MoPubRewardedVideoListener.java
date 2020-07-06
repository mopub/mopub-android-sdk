// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import androidx.annotation.NonNull;

import com.mopub.common.MoPubReward;

import java.util.Set;

/**
 * Listener for rewarded video events. Implementers of this interface will receive events for all
 * rewarded video ad units in the app.:
 */
public interface MoPubRewardedVideoListener {

    /**
     * Called when the adUnitId has loaded. At this point you should be able to call
     * {@link com.mopub.mobileads.MoPubRewardedVideos#showRewardedVideo(String)} to show the video.
     */
    public void onRewardedVideoLoadSuccess(@NonNull String adUnitId);

    /**
     * Called when a video fails to load for the given ad unit id. The provided error code will
     * give more insight into the reason for the failure to load.
     */
    public void onRewardedVideoLoadFailure(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode);

    /**
     * Called when a rewarded video starts playing.
     */
    public void onRewardedVideoStarted(@NonNull String adUnitId);

    /**
     * Called when there is an error during video playback.
     */
    public void onRewardedVideoPlaybackError(@NonNull String adUnitId, @NonNull MoPubErrorCode errorCode);

    /**
     * Called when a rewarded video is clicked.
     */
    public void onRewardedVideoClicked(@NonNull String adUnitId);

    /**
     * Called when a rewarded video is closed. At this point your application should resume.
     */
    public void onRewardedVideoClosed(@NonNull String adUnitId);

    /**
     * Called when a rewarded video is completed and the user should be rewarded.
     */
    public void onRewardedVideoCompleted(@NonNull Set<String> adUnitIds, @NonNull MoPubReward reward);
}
