package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This XML manager handles Extension nodes.
 */
public class VastExtensionXmlManager {
    // Elements
    public static final String VIDEO_VIEWABILITY_TRACKER = "MoPubViewabilityTracker";
    public static final String AD_VERIFICATIONS = "AdVerifications";
    public static final String VERIFICATION = "Verification";
    public static final String AVID = "AVID";

    // Attributes
    public static final String VENDOR = "vendor";
    public static final String TYPE = "type";
    public static final String ID = "id";
    public static final String MOAT = "Moat";

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
     * If there is an Extension node with an AVID element, return associated JavaScriptResources
     * from buyer tags.
     *
     * @return Set of JavaScriptResources in string form, or null if AVID node is missing.
     */
    @Nullable
    Set<String> getAvidJavaScriptResources() {
        final Node avidNode = XmlUtils.getFirstMatchingChildNode(mExtensionNode, AVID);
        if (avidNode == null) {
            return null;
        }

        return new AvidBuyerTagXmlManager(avidNode).getJavaScriptResources();
    }

    /**
     * If the Extension node contains Moat-related Verification nodes, return their corresponding
     * impression pixels from buyer tags.
     *
     * @return Set of impression pixels in string form, or null if no Moat Verification nodes
     * are present.
     */
    @Nullable
    Set<String> getMoatImpressionPixels() {
        final Node adVerification = XmlUtils.getFirstMatchingChildNode(mExtensionNode, AD_VERIFICATIONS);
        if (adVerification == null) {
            return null;
        }

        final List<Node> moatNodes = XmlUtils.getMatchingChildNodes(adVerification, VERIFICATION,
                VENDOR, Collections.singletonList(MOAT));
        if (moatNodes == null || moatNodes.isEmpty()) {
            return null;
        }

        return new MoatBuyerTagXmlManager(moatNodes).getImpressionPixelsXml();
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
