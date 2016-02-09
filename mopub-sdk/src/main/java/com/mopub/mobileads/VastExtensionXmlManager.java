package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

/**
 * This XML manager handles Extension nodes.
 */
public class VastExtensionXmlManager {
    // Elements
    public static final String VIDEO_VIEWABILITY_TRACKER = "MoPubViewabilityTracker";

    // Attributes
    public static final String TYPE = "type";

    private final Node mExtensionNode;

    public VastExtensionXmlManager(@NonNull Node extensionNode) {
        Preconditions.checkNotNull(extensionNode);

        this.mExtensionNode = extensionNode;
    }

    /**
     * If there is an Extension node with a MoPubViewabilityTracker element, return its data object.
     *
     * @return The {@link VideoViewabilityTracker} parsed from the given node or null if missing or
     * invalid.
     */
    @Nullable
    VideoViewabilityTracker getVideoViewabilityTracker() {
        Node videoViewabilityTrackerNode =
                XmlUtils.getFirstMatchingChildNode(mExtensionNode, VIDEO_VIEWABILITY_TRACKER);
        if (videoViewabilityTrackerNode == null) {
            return null;
        }

        VideoViewabilityTrackerXmlManager videoViewabilityTrackerXmlManager =
                new VideoViewabilityTrackerXmlManager(videoViewabilityTrackerNode);
        Integer viewablePlaytime = videoViewabilityTrackerXmlManager.getViewablePlaytimeMS();
        Integer percentViewable = videoViewabilityTrackerXmlManager.getPercentViewable();
        String videoViewabilityTrackerUrl =
                videoViewabilityTrackerXmlManager.getVideoViewabilityTrackerUrl();

        if (viewablePlaytime == null || percentViewable == null
                || TextUtils.isEmpty(videoViewabilityTrackerUrl)) {
            return null;
        }

        return new VideoViewabilityTracker(viewablePlaytime, percentViewable,
                videoViewabilityTrackerUrl);
    }

    /**
     * If the node has a "type" attribute, return its value.
     *
     * @return A String with the value of the "type" attribute or null if missing.
     */
    @Nullable
    String getType() {
        return XmlUtils.getAttributeValue(mExtensionNode, TYPE);
    }
}
