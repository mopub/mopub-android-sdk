package com.mopub.mobileads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.Preconditions;
import com.mopub.common.UrlAction;
import com.mopub.common.UrlHandler;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.Intents;
import com.mopub.common.util.Strings;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mopub.network.TrackingRequest.makeVastTrackingHttpRequest;

public class VastVideoConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @NonNull private final ArrayList<VastTracker> mImpressionTrackers;
    @NonNull private final ArrayList<VastFractionalProgressTracker> mFractionalTrackers;
    @NonNull private final ArrayList<VastAbsoluteProgressTracker> mAbsoluteTrackers;
    @NonNull private final ArrayList<VastTracker> mPauseTrackers;
    @NonNull private final ArrayList<VastTracker> mResumeTrackers;
    @NonNull private final ArrayList<VastTracker> mCompleteTrackers;
    @NonNull private final ArrayList<VastTracker> mCloseTrackers;
    @NonNull private final ArrayList<VastTracker> mSkipTrackers;
    @NonNull private final ArrayList<VastTracker> mClickTrackers;
    @NonNull private final ArrayList<VastTracker> mErrorTrackers;
    @Nullable private String mClickThroughUrl;
    @Nullable private String mNetworkMediaFileUrl;
    @Nullable private String mDiskMediaFileUrl;
    @Nullable private String mSkipOffset;
    @Nullable private VastCompanionAdConfig mLandscapeVastCompanionAdConfig;
    @Nullable private VastCompanionAdConfig mPortraitVastCompanionAdConfig;
    @Nullable private VastIconConfig mVastIconConfig;

    // Custom extensions
    @Nullable private String mCustomCtaText;
    @Nullable private String mCustomSkipText;
    @Nullable private String mCustomCloseIconUrl;
    @NonNull private DeviceUtils.ForceOrientation mCustomForceOrientation = DeviceUtils.ForceOrientation.FORCE_LANDSCAPE; // Default is forcing landscape

    /**
     * Flag to indicate if the VAST xml document has explicitly set the orientation as opposed to
     * using the default.
     */
    private boolean mIsForceOrientationSet;

    public VastVideoConfig() {
        mImpressionTrackers = new ArrayList<VastTracker>();
        mFractionalTrackers = new ArrayList<VastFractionalProgressTracker>();
        mAbsoluteTrackers = new ArrayList<VastAbsoluteProgressTracker>();
        mPauseTrackers = new ArrayList<VastTracker>();
        mResumeTrackers = new ArrayList<VastTracker>();
        mCompleteTrackers = new ArrayList<VastTracker>();
        mCloseTrackers = new ArrayList<VastTracker>();
        mSkipTrackers = new ArrayList<VastTracker>();
        mClickTrackers = new ArrayList<VastTracker>();
        mErrorTrackers = new ArrayList<VastTracker>();
    }

    /**
     * Setters
     */

    public void addImpressionTrackers(@NonNull final List<VastTracker> impressionTrackers) {
        Preconditions.checkNotNull(impressionTrackers, "impressionTrackers cannot be null");
        mImpressionTrackers.addAll(impressionTrackers);
    }

    /**
     * Add trackers for percentage-based tracking. This includes all quartile trackers and any
     * "progress" events with other percentages.
     */
    public void addFractionalTrackers(@NonNull final List<VastFractionalProgressTracker> fractionalTrackers) {
        Preconditions.checkNotNull(fractionalTrackers, "fractionalTrackers cannot be null");
        mFractionalTrackers.addAll(fractionalTrackers);
        Collections.sort(mFractionalTrackers);
    }

    /**
     * Add trackers for absolute tracking. This includes start trackers, which have an absolute threshold of 2 seconds.
     */
    public void addAbsoluteTrackers(@NonNull final List<VastAbsoluteProgressTracker> absoluteTrackers) {
        Preconditions.checkNotNull(absoluteTrackers, "absoluteTrackers cannot be null");
        mAbsoluteTrackers.addAll(absoluteTrackers);
        Collections.sort(mAbsoluteTrackers);
    }

    public void addCompleteTrackers(@NonNull final List<VastTracker> completeTrackers) {
        Preconditions.checkNotNull(completeTrackers, "completeTrackers cannot be null");
        mCompleteTrackers.addAll(completeTrackers);
    }

    /**
     * Add trackers for when the video is paused.
     *
     * @param pauseTrackers List of String URLs to hit
     */
    public void addPauseTrackers(@NonNull List<VastTracker> pauseTrackers) {
        Preconditions.checkNotNull(pauseTrackers, "pauseTrackers cannot be null");
        mPauseTrackers.addAll(pauseTrackers);
    }

    /**
     * Add trackers for when the video is resumed.
     *
     * @param resumeTrackers List of String URLs to hit
     */
    public void addResumeTrackers(@NonNull List<VastTracker> resumeTrackers) {
        Preconditions.checkNotNull(resumeTrackers, "resumeTrackers cannot be null");
        mResumeTrackers.addAll(resumeTrackers);
    }

    public void addCloseTrackers(@NonNull final List<VastTracker> closeTrackers) {
        Preconditions.checkNotNull(closeTrackers, "closeTrackers cannot be null");
        mCloseTrackers.addAll(closeTrackers);
    }

    public void addSkipTrackers(@NonNull final List<VastTracker> skipTrackers) {
        Preconditions.checkNotNull(skipTrackers, "skipTrackers cannot be null");
        mSkipTrackers.addAll(skipTrackers);
    }

    public void addClickTrackers(@NonNull final List<VastTracker> clickTrackers) {
        Preconditions.checkNotNull(clickTrackers, "clickTrackers cannot be null");
        mClickTrackers.addAll(clickTrackers);
    }

    /**
     * Add trackers for errors.
     *
     * @param errorTrackers A URL to hit when an error happens.
     */
    public void addErrorTrackers(@NonNull final List<VastTracker> errorTrackers) {
        Preconditions.checkNotNull(errorTrackers, "errorTrackers cannot be null");
        mErrorTrackers.addAll(errorTrackers);
    }

    public void setClickThroughUrl(@Nullable final String clickThroughUrl) {
        mClickThroughUrl = clickThroughUrl;
    }

    public void setNetworkMediaFileUrl(@Nullable final String networkMediaFileUrl) {
        mNetworkMediaFileUrl = networkMediaFileUrl;
    }

    public void setDiskMediaFileUrl(@Nullable final String diskMediaFileUrl) {
        mDiskMediaFileUrl = diskMediaFileUrl;
    }

    public void setVastCompanionAd(@Nullable final VastCompanionAdConfig landscapeVastCompanionAdConfig,
            @Nullable final VastCompanionAdConfig portraitVastCompanionAdConfig) {
        mLandscapeVastCompanionAdConfig = landscapeVastCompanionAdConfig;
        mPortraitVastCompanionAdConfig = portraitVastCompanionAdConfig;
    }

    public void setVastIconConfig(@Nullable final VastIconConfig vastIconConfig) {
        mVastIconConfig = vastIconConfig;
    }

    public void setCustomCtaText(@Nullable final String customCtaText) {
        if (customCtaText != null) {
            mCustomCtaText = customCtaText;
        }
    }

    public void setCustomSkipText(@Nullable final String customSkipText) {
        if (customSkipText != null) {
            mCustomSkipText = customSkipText;
        }
    }

    public void setCustomCloseIconUrl(@Nullable final String customCloseIconUrl) {
        if (customCloseIconUrl != null) {
            mCustomCloseIconUrl = customCloseIconUrl;
        }
    }

    public void setCustomForceOrientation(@Nullable final DeviceUtils.ForceOrientation customForceOrientation) {
        if (customForceOrientation != null && customForceOrientation != DeviceUtils.ForceOrientation.UNDEFINED) {
            mCustomForceOrientation = customForceOrientation;
            mIsForceOrientationSet = true;
        }
    }

    public void setSkipOffset(@Nullable final String skipOffset) {
        if (skipOffset != null) {
            mSkipOffset = skipOffset;
        }
    }

    /**
     * Getters
     */

    @NonNull
    public List<VastTracker> getImpressionTrackers() {
        return mImpressionTrackers;
    }

    @NonNull
    public ArrayList<VastAbsoluteProgressTracker> getAbsoluteTrackers() {
        return mAbsoluteTrackers;
    }

    @NonNull
    public ArrayList<VastFractionalProgressTracker> getFractionalTrackers() {
        return mFractionalTrackers;
    }

    @NonNull
    public List<VastTracker> getPauseTrackers() {
        return mPauseTrackers;
    }

    @NonNull
    public List<VastTracker> getResumeTrackers() {
        return mResumeTrackers;
    }

    @NonNull
    public List<VastTracker> getCompleteTrackers() {
        return mCompleteTrackers;
    }

    @NonNull
    public List<VastTracker> getCloseTrackers() {
        return mCloseTrackers;
    }

    @NonNull
    public List<VastTracker> getSkipTrackers() {
        return mSkipTrackers;
    }

    @NonNull
    public List<VastTracker> getClickTrackers() {
        return mClickTrackers;
    }

    /**
     * Gets a list of error trackers.
     *
     * @return List of String URLs.
     */
    @NonNull
    public List<VastTracker> getErrorTrackers() {
        return mErrorTrackers;
    }

    @Nullable
    public String getClickThroughUrl() {
        return mClickThroughUrl;
    }

    @Nullable
    public String getNetworkMediaFileUrl() {
        return mNetworkMediaFileUrl;
    }

    @Nullable
    public String getDiskMediaFileUrl() {
        return mDiskMediaFileUrl;
    }

    @Nullable
    public VastCompanionAdConfig getVastCompanionAd(final int orientation) {
        switch (orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                return mPortraitVastCompanionAdConfig;
            case Configuration.ORIENTATION_LANDSCAPE:
                return mLandscapeVastCompanionAdConfig;
            default:
                return mLandscapeVastCompanionAdConfig;
        }
    }

    @Nullable
    public VastIconConfig getVastIconConfig() {
        return mVastIconConfig;
    }

    @Nullable
    public String getCustomCtaText() {
        return mCustomCtaText;
    }

    @Nullable
    public String getCustomSkipText() {
        return mCustomSkipText;
    }

    @Nullable
    public String getCustomCloseIconUrl() {
        return mCustomCloseIconUrl;
    }

    public boolean isCustomForceOrientationSet() {
        return mIsForceOrientationSet;
    }

    /**
     * Returns whether or not there is a companion ad set. There must be both a landscape and a
     * portrait companion ad set for this to be true.
     *
     * @return true if both the landscape and portrait companion ads are set, false otherwise.
     */
    public boolean hasCompanionAd() {
        return mLandscapeVastCompanionAdConfig != null && mPortraitVastCompanionAdConfig != null;
    }

    /**
     * Get custom force orientation
     * @return ForceOrientation enum (default is FORCE_LANDSCAPE)
     */
    @NonNull
    public DeviceUtils.ForceOrientation getCustomForceOrientation() {
        return mCustomForceOrientation;
    }

    /**
     * Gets the String specified in the VAST document regarding the skip offset. This should be in
     * the form HH:MM:SS[.mmm] or n%. (e.g. 00:00:12, 00:00:12.345, 42%).
     *
     * @return String representation of the skip offset or {@code null} if not set.
     */
    @Nullable
    public String getSkipOffsetString() {
        return mSkipOffset;
    }

    /**
     * Called when the video starts playing.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    public void handleImpression(@NonNull final Context context, int contentPlayHead) {
        Preconditions.checkNotNull(context, "context cannot be null");
        makeVastTrackingHttpRequest(
                mImpressionTrackers,
                null,
                contentPlayHead,
                mNetworkMediaFileUrl,
                context
        );
    }

    /**
     * Called when the video is clicked. Handles forwarding the user to the specified click through
     * url.
     *
     * @param activity        This has to be an activity to call startActivityForResult.
     * @param contentPlayHead Current video playback time when clicked.
     * @param requestCode     The code that identifies what kind of activity request is going to be
     *                        made
     */
    public void handleClick(@NonNull final Activity activity, final int contentPlayHead,
            final int requestCode) {
        Preconditions.checkNotNull(activity, "activity cannot be null");

        makeVastTrackingHttpRequest(
                mClickTrackers,
                null,
                contentPlayHead,
                mNetworkMediaFileUrl,
                activity
        );

        if (TextUtils.isEmpty(mClickThroughUrl)) {
            return;
        }

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

                            final Class clazz = MoPubBrowser.class;
                            final Intent intent = Intents.getStartActivityIntent(
                                    activity, clazz, bundle);
                            try {
                                activity.startActivityForResult(intent, requestCode);
                            } catch (ActivityNotFoundException e) {
                                MoPubLog.d("Activity " + clazz.getName() + " not found. Did you " +
                                        "declare it in your AndroidManifest.xml?");
                            }
                        }
                    }

                    @Override
                    public void urlHandlingFailed(@NonNull String url,
                            @NonNull UrlAction lastFailedUrlAction) {
                    }
                })
                .withoutMoPubBrowser()
                .build().handleUrl(activity, mClickThroughUrl);
    }

    /**
     * Called when the video is not finished and is resumed from the middle of the video.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    public void handleResume(@NonNull final Context context, int contentPlayHead) {
        Preconditions.checkNotNull(context, "context cannot be null");
        makeVastTrackingHttpRequest(
                mResumeTrackers,
                null,
                contentPlayHead,
                mNetworkMediaFileUrl,
                context
        );
    }

    /**
     * Called when the video is not finished and is paused.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    public void handlePause(@NonNull final Context context, int contentPlayHead) {
        Preconditions.checkNotNull(context, "context cannot be null");
        makeVastTrackingHttpRequest(
                mPauseTrackers,
                null,
                contentPlayHead,
                mNetworkMediaFileUrl,
                context
        );
    }

    /**
     * Called when the video is closed or skipped.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    public void handleClose(@NonNull Context context, int contentPlayHead) {
        Preconditions.checkNotNull(context, "context cannot be null");
        makeVastTrackingHttpRequest(
                mCloseTrackers,
                null,
                contentPlayHead,
                mNetworkMediaFileUrl,
                context
        );

        makeVastTrackingHttpRequest(
                mSkipTrackers,
                null,
                contentPlayHead,
                mNetworkMediaFileUrl,
                context
        );
    }

    /**
     * Called when the video is played completely without skipping.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time (should be duration of video).
     */
    public void handleComplete(@NonNull Context context, int contentPlayHead) {
        Preconditions.checkNotNull(context, "context cannot be null");
        makeVastTrackingHttpRequest(
                mCompleteTrackers,
                null,
                contentPlayHead,
                mNetworkMediaFileUrl,
                context
        );
    }

    /**
     * Called when there is a problem with the video. Refer to the possible {@link VastErrorCode}s
     * for a list of problems.
     *
     * @param context         The context. Can be application or activity context.
     * @param contentPlayHead Current video playback time.
     */
    public void handleError(@NonNull Context context, @NonNull VastErrorCode errorCode,
            int contentPlayHead) {
        Preconditions.checkNotNull(context, "context cannot be null");
        makeVastTrackingHttpRequest(
                mErrorTrackers,
                errorCode,
                contentPlayHead,
                mNetworkMediaFileUrl,
                context
        );
    }

    /**
     * Returns untriggered VAST progress trackers with a progress before the provided position.
     *
     * @param currentPositionMillis the current video position in milliseconds.
     * @param videoLengthMillis the total video length.
     */
    @NonNull
    public List<VastTracker> getUntriggeredTrackersBefore(int currentPositionMillis, int videoLengthMillis) {
        if (Preconditions.NoThrow.checkArgument(videoLengthMillis > 0)) {
            float progressFraction = currentPositionMillis / (float) (videoLengthMillis);
            List<VastTracker> untriggeredTrackers = new ArrayList<VastTracker>();

            VastAbsoluteProgressTracker absoluteTest = new VastAbsoluteProgressTracker("", currentPositionMillis);
            int absoluteTrackerCount = mAbsoluteTrackers.size();
            for (int i = 0; i < absoluteTrackerCount; i++) {
                VastAbsoluteProgressTracker tracker = mAbsoluteTrackers.get(i);
                if (tracker.compareTo(absoluteTest) > 0) {
                    break;
                }
                if (!tracker.isTracked()) {
                    untriggeredTrackers.add(tracker);
                }
            }

            final VastFractionalProgressTracker fractionalTest = new VastFractionalProgressTracker("", progressFraction);
            int fractionalTrackerCount = mFractionalTrackers.size();
            for (int i = 0; i < fractionalTrackerCount; i++) {
                VastFractionalProgressTracker tracker = mFractionalTrackers.get(i);
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

    /**
     * Returns the number of untriggered progress trackers.
     *
     * @return Integer count >= 0 of the remaining progress trackers.
     */
    public int getRemainingProgressTrackerCount() {
        return getUntriggeredTrackersBefore(Integer.MAX_VALUE, Integer.MAX_VALUE).size();
    }

    /**
     * Gets the skip offset in milliseconds. If the skip offset would be past the video duration,
     * this returns null. If an error occurs, this returns null.
     *
     * @param videoDuration Used to calculate percentage based offsets.
     * @return The skip offset in milliseconds. Can return null.
     */
    @Nullable
    public Integer getSkipOffsetMillis(final int videoDuration) {
        if (mSkipOffset != null) {
            try {
                if (Strings.isAbsoluteTracker(mSkipOffset)) {
                    Integer skipOffsetMilliseconds = Strings.parseAbsoluteOffset(mSkipOffset);
                    if (skipOffsetMilliseconds != null && skipOffsetMilliseconds < videoDuration) {
                        return skipOffsetMilliseconds;
                    }
                } else if (Strings.isPercentageTracker(mSkipOffset)) {
                    float percentage = Float.parseFloat(mSkipOffset.replace("%", "")) / 100f;
                    int skipOffsetMillisecondsRounded = Math.round(videoDuration * percentage);
                    if (skipOffsetMillisecondsRounded < videoDuration) {
                        return skipOffsetMillisecondsRounded;
                    }
                } else {
                    MoPubLog.d(
                            String.format("Invalid VAST skipoffset format: %s", mSkipOffset));
                }
            } catch (NumberFormatException e) {
                MoPubLog.d(String.format("Failed to parse skipoffset %s", mSkipOffset));
            }
        }
        return null;
    }
}
