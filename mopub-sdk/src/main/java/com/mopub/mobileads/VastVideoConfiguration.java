package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.util.DeviceUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VastVideoConfiguration implements Serializable {
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
    @Nullable private VastCompanionAd mVastCompanionAd;
    @Nullable private VastIcon mVastIcon;

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

    public VastVideoConfiguration() {
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

    public void setVastCompanionAd(@Nullable final VastCompanionAd vastCompanionAd) {
        mVastCompanionAd = vastCompanionAd;
    }

    public void setVastIcon(@Nullable final VastIcon vastIcon) {
        mVastIcon = vastIcon;
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
    public VastCompanionAd getVastCompanionAd() {
        return mVastCompanionAd;
    }

    @Nullable
    public VastIcon getVastIcon() {
        return mVastIcon;
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
     * Get custom force orientation
     * @return ForceOrientation enum (default is FORCE_LANDSCAPE)
     */
    @NonNull
    public DeviceUtils.ForceOrientation getCustomForceOrientation() {
        return mCustomForceOrientation;
    }

    @Nullable
    public String getSkipOffset() {
        return mSkipOffset;
    }
}
