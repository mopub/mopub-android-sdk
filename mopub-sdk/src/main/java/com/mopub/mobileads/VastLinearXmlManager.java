package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Strings;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This XML manager handles the meta data around the video file. This includes video progress
 * trackers and click trackers. This also houses the manager for the actual media file.
 */
class VastLinearXmlManager {

    // Element names
    private static final String TRACKING_EVENTS = "TrackingEvents";
    private static final String VIDEO_CLICKS = "VideoClicks";
    private static final String VIDEO_TRACKER = "Tracking";
    private static final String CLICK_THROUGH = "ClickThrough";
    private static final String CLICK_TRACKER = "ClickTracking";
    private static final String MEDIA_FILES = "MediaFiles";
    private static final String MEDIA_FILE = "MediaFile";
    public static final String ICONS = "Icons";
    public static final String ICON = "Icon";

    // Attribute names
    private static final String EVENT = "event";
    private static final String OFFSET = "offset";
    private static final String SKIP_OFFSET = "skipoffset";

    // Attribute values
    private static final String CREATIVE_VIEW = "creativeView";
    private static final String START = "start";
    private static final String FIRST_QUARTILE = "firstQuartile";
    private static final String MIDPOINT = "midpoint";
    private static final String THIRD_QUARTILE = "thirdQuartile";
    private static final String COMPLETE = "complete";
    private static final String PAUSE = "pause";
    private static final String RESUME = "resume";
    private static final String CLOSE = "close";
    private static final String CLOSE_LINEAR = "closeLinear";
    private static final String PROGRESS = "progress";
    private static final String SKIP = "skip";

    private static final int CREATIVE_VIEW_TRACKER_THRESHOLD = 0;
    private static final int START_TRACKER_THRESHOLD = 2000;
    private static final float FIRST_QUARTER_MARKER = 0.25f;
    private static final float MID_POINT_MARKER = 0.50f;
    private static final float THIRD_QUARTER_MARKER = 0.75f;

    @NonNull private final Node mLinearNode;

    VastLinearXmlManager(@NonNull final Node linearNode) {
        Preconditions.checkNotNull(linearNode);
        mLinearNode = linearNode;
    }

    /**
     * Return a sorted list of the video's percent-based progress-trackers. These are the
     * quartile trackers and any "progress" nodes with percent-based offsets.
     *
     * Quartile trackers look like:
     * {@code
     * <Tracking event="firstQuartile">
     *     <![CDATA[trackingURL]]>
     * </Tracking>
     * }
     *
     * Percent-based progress trackers look like:
     * {@code
     * <Tracking event="progress" offset="11%">
     *     <![CDATA[trackingURL]]>
     * </Tracking>
     * }
     */
    @NonNull
    List<VastFractionalProgressTracker> getFractionalProgressTrackers() {
        // Add all the quartile trackers from VAST 2.0:
        List<VastFractionalProgressTracker> percentTrackers = new ArrayList<VastFractionalProgressTracker>();

        addQuartileTrackerWithFraction(percentTrackers, getVideoTrackersByAttribute(FIRST_QUARTILE), FIRST_QUARTER_MARKER);
        addQuartileTrackerWithFraction(percentTrackers, getVideoTrackersByAttribute(MIDPOINT), MID_POINT_MARKER);
        addQuartileTrackerWithFraction(percentTrackers, getVideoTrackersByAttribute(THIRD_QUARTILE), THIRD_QUARTER_MARKER);

        final Node trackingEvents = XmlUtils.getFirstMatchingChildNode(mLinearNode, TRACKING_EVENTS);
        if (trackingEvents != null) {
            // Get any other trackers with event="progress" offset="n%"
            final List<Node> progressNodes = XmlUtils.getMatchingChildNodes(trackingEvents,
                    VIDEO_TRACKER, EVENT, Collections.singletonList(PROGRESS));

            for (Node progressNode : progressNodes) {
                String offsetString = XmlUtils.getAttributeValue(progressNode, OFFSET);
                if (offsetString == null) {
                    continue;
                }
                offsetString = offsetString.trim();
                if (Strings.isPercentageTracker(offsetString)) {
                    String trackingUrl = XmlUtils.getNodeValue(progressNode);
                    try {
                        float trackingFraction =
                                Float.parseFloat(offsetString.replace("%", "")) / 100f;
                        percentTrackers.add(new VastFractionalProgressTracker(trackingUrl, trackingFraction));
                    } catch (NumberFormatException e) {
                        MoPubLog.d(String.format("Failed to parse VAST progress tracker %s",
                                offsetString));
                    }
                }
            }
        }

        // Sort the list so we can quickly index it in the video progress runnable.
        Collections.sort(percentTrackers);
        return percentTrackers;
    }

    /**
     * Return a sorted list of the video's absolute progress trackers. This includes start trackers
     * and any "progress" nodes with absolute offsets.
     *
     * Start trackers live in nodes like:
     * {@code
     * <Tracking event="start">
     *     <![CDATA[trackingURL]]>
     * </Tracking>
     * }
     * Absolute progress trackers look like:
     * {@code
     * <Tracking event="progress" offset="00:00:10.000">
     *     <![CDATA[trackingURL]]>
     * </Tracking>
     * }
     */
    @NonNull
    List<VastAbsoluteProgressTracker> getAbsoluteProgressTrackers() {
        List<VastAbsoluteProgressTracker> trackers = new ArrayList<VastAbsoluteProgressTracker>();

        // Start trackers are treated as absolute trackers with a 2s offset.
        final List<String> startTrackers = getVideoTrackersByAttribute(START);
        for (String url : startTrackers) {
            trackers.add(new VastAbsoluteProgressTracker(url, START_TRACKER_THRESHOLD));
        }

        final Node trackingEvents = XmlUtils.getFirstMatchingChildNode(mLinearNode, TRACKING_EVENTS);
        if (trackingEvents != null) {
            // Parse progress trackers and extract the absolute offsets of the form "HH:MM:SS[.mmm]"

            final List<Node> progressNodes = XmlUtils.getMatchingChildNodes(trackingEvents,
                    VIDEO_TRACKER, EVENT, Collections.singletonList(PROGRESS));
            for (Node progressNode : progressNodes) {
                String offsetString = XmlUtils.getAttributeValue(progressNode, OFFSET);
                if (offsetString == null) {
                    continue;
                }
                offsetString = offsetString.trim();
                if (Strings.isAbsoluteTracker(offsetString)) {
                    String trackingUrl = XmlUtils.getNodeValue(progressNode);
                    try {
                        Integer trackingMilliseconds = Strings.parseAbsoluteOffset(offsetString);
                        if (trackingMilliseconds != null) {
                            trackers.add(new VastAbsoluteProgressTracker(trackingUrl, trackingMilliseconds));
                        }
                    } catch (NumberFormatException e) {
                        MoPubLog.d(String.format("Failed to parse VAST progress tracker %s",
                                offsetString));
                    }
                }
            }

            // Parse creativeView trackers
            final List<Node> creativeViewNodes = XmlUtils.getMatchingChildNodes(trackingEvents,
                    VIDEO_TRACKER, EVENT, Collections.singletonList(CREATIVE_VIEW));
            for (Node creativeViewNode : creativeViewNodes) {
                trackers.add(
                        new VastAbsoluteProgressTracker(XmlUtils.getNodeValue(creativeViewNode),
                                CREATIVE_VIEW_TRACKER_THRESHOLD));
            }
        }

        // Sort the list so we can quickly index it in the video progress runnable.
        Collections.sort(trackers);
        return trackers;
    }

    /**
     * Gets a list of URLs for when the video finishes playing. This list may be empty.
     *
     * @return List of String URLs of video complete trackers.
     */
    @NonNull
    List<VastTracker> getVideoCompleteTrackers() {
        return getVideoTrackersByAttributeAsVastTrackers(COMPLETE);
    }

    /**
     * Gets a list of URLs for when the video pauses. This list may be empty.
     *
     * @return List of String URLs of pause trackers.
     */
    @NonNull
    List<VastTracker> getPauseTrackers() {
        List<String> trackers = getVideoTrackersByAttribute(PAUSE);
        List<VastTracker> vastRepeatableTrackers = new ArrayList<VastTracker>();
        for (String tracker : trackers) {
            vastRepeatableTrackers.add(new VastTracker(tracker, true));
        }
        return vastRepeatableTrackers;
    }

    /**
     * Gets a list of URLs for when the video resumes. This list may be empty.
     *
     * @return List of String URLs of resume trackers.
     */
    @NonNull
    List<VastTracker> getResumeTrackers() {
        List<String> trackers = getVideoTrackersByAttribute(RESUME);
        List<VastTracker> vastRepeatableTrackers = new ArrayList<VastTracker>();
        for (String tracker : trackers) {
            vastRepeatableTrackers.add(new VastTracker(tracker, true));
        }
        return vastRepeatableTrackers;
    }

    /**
     * Gets a list of URLs for when the video closes. This list may be empty.
     *
     * @return List of String URLs of video closes.
     */
    @NonNull
    List<VastTracker> getVideoCloseTrackers() {
        List<VastTracker> closeTrackers = getVideoTrackersByAttributeAsVastTrackers(CLOSE);
        closeTrackers.addAll(getVideoTrackersByAttributeAsVastTrackers(CLOSE_LINEAR));
        return closeTrackers;
    }

    /**
     * Gets a list of URLs for when the user skips the video. This list may be empty.
     *
     * @return List of String URLs of video skip trackers.
     */
    @NonNull
    List<VastTracker> getVideoSkipTrackers() {
        return getVideoTrackersByAttributeAsVastTrackers(SKIP);
    }

    /**
     * Gets the clickthrough url. May be null.
     *
     * @return The clickthrough URL or {@code null} if there isn't one.
     */
    @Nullable
    String getClickThroughUrl() {
        final Node videoClicks = XmlUtils.getFirstMatchingChildNode(mLinearNode, VIDEO_CLICKS);
        if (videoClicks == null) {
            return null;
        }
        return XmlUtils.getNodeValue(XmlUtils.getFirstMatchingChildNode(videoClicks, CLICK_THROUGH));
    }

    /**
     * Gets a list of URLs to track the video click event. This list may be empty.
     *
     * @return List of String URLs of click trackers.
     */
    @NonNull
    List<VastTracker> getClickTrackers() {
        List<VastTracker> clickTrackers = new ArrayList<VastTracker>();

        final Node videoClicks = XmlUtils.getFirstMatchingChildNode(mLinearNode, VIDEO_CLICKS);
        if (videoClicks == null) {
            return clickTrackers;
        }

        final List<Node> clickTrackerNodes = XmlUtils.getMatchingChildNodes(
                videoClicks,
                CLICK_TRACKER
        );

        for (Node clickTrackerNode : clickTrackerNodes) {
            String tracker = XmlUtils.getNodeValue(clickTrackerNode);
            if (tracker != null) {
                clickTrackers.add(new VastTracker(tracker));
            }
        }
        return clickTrackers;
    }

    /**
     * Gets where the video can be skipped from. This can be in a percentage or in the format
     * 'hh:mm:ss(.mmm)' (for example, a video that is skippable 5 seconds into a 20 second video
     * would be '25%', '00:00:05', or '00:00:05.000').
     *
     * @return The skip offset, or {@code null} if there isn't one.
     */
    @Nullable
    String getSkipOffset() {
        final String skipOffsetString = XmlUtils.getAttributeValue(mLinearNode, SKIP_OFFSET);
        if (skipOffsetString == null) {
            return null;
        }

        if (skipOffsetString.trim().isEmpty()) {
            return null;
        }

        return skipOffsetString.trim();
    }

    /**
     * If there is a Media section, return its XML manager.
     *
     * @return The {@link VastMediaXmlManager}
     * or an empty list if there is no Media child node.
     */
    @NonNull
    List<VastMediaXmlManager> getMediaXmlManagers() {
        final List<VastMediaXmlManager> mediaXmlManagers = new ArrayList<VastMediaXmlManager>();

        final Node mediaFiles = XmlUtils.getFirstMatchingChildNode(mLinearNode, MEDIA_FILES);
        if (mediaFiles == null) {
            return mediaXmlManagers;
        }

        List<Node> mediaNodes = XmlUtils.getMatchingChildNodes(mediaFiles, MEDIA_FILE);
        for (Node mediaNode : mediaNodes) {
            mediaXmlManagers.add(new VastMediaXmlManager(mediaNode));
        }

        return mediaXmlManagers;
    }

    @NonNull
    List<VastIconXmlManager> getIconXmlManagers() {
        final List<VastIconXmlManager> iconXmlManagers = new ArrayList<VastIconXmlManager>();

        final Node icons= XmlUtils.getFirstMatchingChildNode(mLinearNode, ICONS);
        if (icons == null) {
            return iconXmlManagers;
        }

        List<Node> iconNodes = XmlUtils.getMatchingChildNodes(icons, ICON);
        for (Node iconNode : iconNodes) {
            iconXmlManagers.add(new VastIconXmlManager(iconNode));
        }

        return iconXmlManagers;
    }

    @NonNull
    private List<VastTracker> getVideoTrackersByAttributeAsVastTrackers(
            @NonNull final String attributeValue) {
        List<String> trackers = getVideoTrackersByAttribute(attributeValue);
        List<VastTracker> vastTrackers = new ArrayList<VastTracker>(trackers.size());
        for (String tracker : trackers) {
            vastTrackers.add(new VastTracker(tracker));
        }
        return vastTrackers;
    }

    /**
     * This helper method makes it easy to get a tracking event with a specific name in a linear
     * node. This returns an empty list if there isn't a tracker with that event name.
     *
     * @param attributeValue The name of the tracking event
     * @return List of URLs with that tracker name or an empty list if none found.
     */
    @NonNull
    private List<String> getVideoTrackersByAttribute(@NonNull final String attributeValue) {
        Preconditions.checkNotNull(attributeValue);
        List<String> videoTrackers = new ArrayList<String>();

        final Node trackingEvents = XmlUtils.getFirstMatchingChildNode(mLinearNode, TRACKING_EVENTS);
        if (trackingEvents == null) {
            return videoTrackers;
        }

        final List<Node> videoTrackerNodes = XmlUtils.getMatchingChildNodes(
                trackingEvents,
                VIDEO_TRACKER,
                EVENT,
                Collections.singletonList(attributeValue)
        );

        for (Node videoTrackerNode : videoTrackerNodes) {
            String tracker = XmlUtils.getNodeValue(videoTrackerNode);
            if (tracker != null) {
                videoTrackers.add(tracker);
            }
        }

        return videoTrackers;
    }

    private void addQuartileTrackerWithFraction(
            @NonNull final List<VastFractionalProgressTracker> trackers,
            @NonNull final List<String> urls, float fraction) {
        Preconditions.checkNotNull(trackers, "trackers cannot be null");
        Preconditions.checkNotNull(urls, "urls cannot be null");
        for (String url : urls) {
            trackers.add(new VastFractionalProgressTracker(url, fraction));
        }
    }
}
