package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Strings;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

/**
 * Data Object for the MoPubViewabilityTracker VAST Custom Extension.
 */
public class VideoViewabilityTrackerXmlManager {
    // Attributes
    public static final String VIEWABLE_PLAYTIME = "viewablePlaytime";
    public static final String PERCENT_VIEWABLE = "percentViewable";

    private final Node mVideoViewabilityNode;

    VideoViewabilityTrackerXmlManager(@NonNull final Node videoViewabilityNode) {
        Preconditions.checkNotNull(videoViewabilityNode);

        mVideoViewabilityNode = videoViewabilityNode;
    }

    /**
     * The amount of milliseconds the video must be playing to fire the impression, parsed from the
     * "viewablePlaytime" attribute. The following two formats are valid for this attribute:
     *     HH:MM:SS[.mmm]
     *     SS[.mmm]
     *
     * @return The value of the "viewablePlaytime" attribute parsed into milliseconds or null if
     * missing or invalid.
     */
    @Nullable
    Integer getViewablePlaytimeMS() {
        String viewablePlaytimeStr =
                XmlUtils.getAttributeValue(mVideoViewabilityNode, VIEWABLE_PLAYTIME);
        if (viewablePlaytimeStr == null) {
            return null;
        }

        Integer viewablePlaytimeMS = null;
        if (Strings.isAbsoluteTracker(viewablePlaytimeStr)) {
            try {
                viewablePlaytimeMS = Strings.parseAbsoluteOffset(viewablePlaytimeStr);
            } catch (NumberFormatException e) {
                MoPubLog.d(String.format("Invalid VAST viewablePlaytime format " +
                        "for \"HH:MM:SS[.mmm]\": %s:", viewablePlaytimeStr));
            }
        } else {
            try {
                viewablePlaytimeMS = (int) (Float.parseFloat(viewablePlaytimeStr) * 1000);
            } catch (NumberFormatException e) {
                MoPubLog.d(String.format("Invalid VAST viewablePlaytime format" +
                        " for \"SS[.mmm]\": %s:", viewablePlaytimeStr));
            }
        }

        if (viewablePlaytimeMS == null || viewablePlaytimeMS < 0) {
            return null;
        }

        return viewablePlaytimeMS;
    }

    /**
     * The percentage of the video that must be in view for the tracker to be fired, parsed from the
     * "percentViewable" attribute. The attribute may or may not have the percentage sign though it
     * must be a number between 0 and 100, inclusive. Any decimal digits are discarded (e.g.,
     * "99.9%" is parsed into "99").
     *
     * @return The value of the percentViewable attribute parsed into an integer between 0 and 100,
     * or null if missing or invalid.
     */
    @Nullable
    Integer getPercentViewable() {
        String percentViewableStr =
                XmlUtils.getAttributeValue(mVideoViewabilityNode, PERCENT_VIEWABLE);
        if (percentViewableStr == null) {
            return null;
        }

        Integer percentViewable = null;
        try {
            percentViewable = (int) (Float.parseFloat(percentViewableStr.replace("%", "")));
        } catch (NumberFormatException e) {
            MoPubLog.d(String.format("Invalid VAST percentViewable format for \"d{1,3}%%\": %s:",
                    percentViewableStr));
        }

        if (percentViewable == null || percentViewable < 0 || percentViewable > 100) {
            return null;
        }

        return percentViewable;
    }

    /**
     * The tracker URL to be fired when the viewablePlaytime and percentViewable values are met,
     * parsed from the body of the node.
     *
     * @return A String with the tracker URL or null if missing.
     */
    @Nullable
    String getVideoViewabilityTrackerUrl() {
        return XmlUtils.getNodeValue(mVideoViewabilityNode);
    }
}
