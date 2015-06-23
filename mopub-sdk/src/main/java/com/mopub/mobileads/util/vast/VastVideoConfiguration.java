package com.mopub.mobileads.util.vast;

import android.support.annotation.Nullable;

import com.mopub.common.util.DeviceUtils;
import com.mopub.mobileads.VastAbsoluteProgressTracker;
import com.mopub.mobileads.VastFractionalProgressTracker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VastVideoConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ArrayList<String> mImpressionTrackers;
    private final ArrayList<VastFractionalProgressTracker> mFractionalTrackers;
    private final ArrayList<VastAbsoluteProgressTracker> mAbsoluteTrackers;
    private final ArrayList<String> mCompleteTrackers;
    private final ArrayList<String> mCloseTrackers;
    private final ArrayList<String> mSkipTrackers;
    private final ArrayList<String> mClickTrackers;
    private String mClickThroughUrl;
    private String mNetworkMediaFileUrl;
    private String mDiskMediaFileUrl;
    private String mSkipOffset;
    private VastCompanionAd mVastCompanionAd;

    // Custom extensions
    private String mCustomCtaText;
    private String mCustomSkipText;
    private String mCustomCloseIconUrl;
    private DeviceUtils.ForceOrientation mCustomForceOrientation = DeviceUtils.ForceOrientation.FORCE_LANDSCAPE; // Default is forcing landscape

    public VastVideoConfiguration() {
        mImpressionTrackers = new ArrayList<String>();
        mFractionalTrackers = new ArrayList<VastFractionalProgressTracker>();
        mAbsoluteTrackers = new ArrayList<VastAbsoluteProgressTracker>();
        mCompleteTrackers = new ArrayList<String>();
        mCloseTrackers = new ArrayList<String>();
        mSkipTrackers = new ArrayList<String>();
        mClickTrackers = new ArrayList<String>();
    }

    /**
     * Setters
     */

    public void addImpressionTrackers(final List<String> impressionTrackers) {
        mImpressionTrackers.addAll(impressionTrackers);
    }

    /**
     * Add trackers for percentage-based tracking. This includes all quartile trackers and any
     * "progress" events with other percentages.
     */
    public void addFractionalTrackers(final List<VastFractionalProgressTracker> fractionalTrackers) {
        mFractionalTrackers.addAll(fractionalTrackers);
        Collections.sort(mFractionalTrackers);
    }

    /**
     * Add trackers for absolute tracking. This includes start trackers, which have an absolute threshold of 2 seconds.
     */
    public void addAbsoluteTrackers(final List<VastAbsoluteProgressTracker> absoluteTrackers) {
        mAbsoluteTrackers.addAll(absoluteTrackers);
        Collections.sort(mAbsoluteTrackers);
    }

    public void addCompleteTrackers(final List<String> completeTrackers) {
        mCompleteTrackers.addAll(completeTrackers);
    }

    public void addCloseTrackers(final List<String> closeTrackers) {
        mCloseTrackers.addAll(closeTrackers);
    }

    public void addSkipTrackers(final List<String> skipTrackers) {
        mSkipTrackers.addAll(skipTrackers);
    }

    public void addClickTrackers(final List<String> clickTrackers) {
        mClickTrackers.addAll(clickTrackers);
    }

    public void setClickThroughUrl(final String clickThroughUrl) {
        mClickThroughUrl = clickThroughUrl;
    }

    public void setNetworkMediaFileUrl(final String networkMediaFileUrl) {
        mNetworkMediaFileUrl = networkMediaFileUrl;
    }

    public void setDiskMediaFileUrl(final String diskMediaFileUrl) {
        mDiskMediaFileUrl = diskMediaFileUrl;
    }

    public void setVastCompanionAd(final VastCompanionAd vastCompanionAd) {
        mVastCompanionAd = vastCompanionAd;
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

    public List<String> getImpressionTrackers() {
        return mImpressionTrackers;
    }

    public ArrayList<VastAbsoluteProgressTracker> getAbsoluteTrackers() {
        return mAbsoluteTrackers;
    }

    public ArrayList<VastFractionalProgressTracker> getFractionalTrackers() {
        return mFractionalTrackers;
    }

    public List<String> getCompleteTrackers() {
        return mCompleteTrackers;
    }

    public List<String> getCloseTrackers() {
        return mCloseTrackers;
    }

    public List<String> getSkipTrackers() {
        return mSkipTrackers;
    }

    public List<String> getClickTrackers() {
        return mClickTrackers;
    }

    public String getClickThroughUrl() {
        return mClickThroughUrl;
    }

    public String getNetworkMediaFileUrl() {
        return mNetworkMediaFileUrl;
    }

    public String getDiskMediaFileUrl() {
        return mDiskMediaFileUrl;
    }

    public VastCompanionAd getVastCompanionAd() {
        return mVastCompanionAd;
    }

    public String getCustomCtaText() {
        return mCustomCtaText;
    }

    public String getCustomSkipText() {
        return mCustomSkipText;
    }

    public String getCustomCloseIconUrl() {
        return mCustomCloseIconUrl;
    }

    /**
     * Get custom force orientation
     * @return ForceOrientation enum (default is FORCE_LANDSCAPE)
     */
    public DeviceUtils.ForceOrientation getCustomForceOrientation() {
        return mCustomForceOrientation;
    }

    public String getSkipOffset() {
        return mSkipOffset;
    }
}
