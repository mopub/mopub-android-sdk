package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This XML manager handles companion ads.
 */
class VastCompanionAdXmlManager {

    // Element names
    private static final String VIDEO_TRACKER = "Tracking";
    private static final String TRACKING_EVENTS = "TrackingEvents";
    private static final String COMPANION_CLICK_THROUGH = "CompanionClickThrough";
    private static final String COMPANION_CLICK_TRACKING = "CompanionClickTracking";

    // Attribute names
    private static final String EVENT = "event";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String AD_SLOT_ID = "adSlotID";

    // Attribute values
    private static final String CREATIVE_VIEW = "creativeView";

    @NonNull private final Node mCompanionNode;
    @NonNull private final VastResourceXmlManager mResourceXmlManager;

    VastCompanionAdXmlManager(@NonNull final Node companionNode) {
        Preconditions.checkNotNull(companionNode, "companionNode cannot be null");
        mCompanionNode = companionNode;
        mResourceXmlManager = new VastResourceXmlManager(companionNode);
    }

    /**
     * Gets the width attribute for the companion ad or {@code null} if not present. This
     * attribute is required according to the VAST 3.0 spec.
     *
     * @return Integer width attribute or {@code null}.
     */
    @Nullable
    Integer getWidth() {
        return XmlUtils.getAttributeValueAsInt(mCompanionNode, WIDTH);
    }

    /**
     * Gets the height attribute for the companion ad or {@code null} if not present. This
     * attribute is required according to the VAST 3.0 spec.
     *
     * @return Integer height attribute or {@code null}.
     */
    @Nullable
    Integer getHeight() {
        return XmlUtils.getAttributeValueAsInt(mCompanionNode, HEIGHT);
    }

    /**
     * Gets the adSlotID attribute from the companion ad or {@code null} if not present.
     *
     * @return String adSlotId attribute or {@code null}.
     */
    @Nullable
    String getAdSlotId() {
        return XmlUtils.getAttributeValue(mCompanionNode, AD_SLOT_ID);
    }

    @NonNull
    VastResourceXmlManager getResourceXmlManager() {
        return mResourceXmlManager;
    }

    /**
     * Gets the clickthrough url of this companion ad or {@code null} if it does not exist.
     *
     * @return The String clickthrough URL or {@code null}
     */
    @Nullable
    String getClickThroughUrl() {
        final Node node = XmlUtils.getFirstMatchingChildNode(
                mCompanionNode,
                COMPANION_CLICK_THROUGH
        );
        return XmlUtils.getNodeValue(node);
    }

    /**
     * Gets a list of click trackers for this companion ad. If none are present, return an empty
     * list.
     *
     * @return List of click tracker URLs or an empty list.
     */
    @NonNull
    List<VastTracker> getClickTrackers() {
        final List<VastTracker> companionAdClickTrackers = new ArrayList<VastTracker>();
        final List<Node> trackerNodes = XmlUtils.getMatchingChildNodes(mCompanionNode, COMPANION_CLICK_TRACKING);
        if (trackerNodes == null) {
            return companionAdClickTrackers;
        }
        for (final Node trackerNode : trackerNodes) {
            String uri = XmlUtils.getNodeValue(trackerNode);
            if (!TextUtils.isEmpty(uri)) {
                companionAdClickTrackers.add(new VastTracker(uri));
            }
        }
        return companionAdClickTrackers;
    }

    /**
     * Gets a list of creativeView trackers for this companion ad. These are impression trackers for
     * just this companion ad as the normal impression trackers are reserved for the whole ad. These
     * should fire when this companion ad is shown. If none are present, return an empty list.
     *
     * @return List of creative view URLs or an empty list.
     */
    @NonNull
    List<VastTracker> getCompanionCreativeViewTrackers() {
        final List<VastTracker> companionCreativeViewTrackers = new ArrayList<VastTracker>();
        final Node node = XmlUtils.getFirstMatchingChildNode(
                mCompanionNode,
                TRACKING_EVENTS
        );

        if (node == null) {
            return companionCreativeViewTrackers;
        }

        final List<Node> trackerNodes = XmlUtils.getMatchingChildNodes(
                node,
                VIDEO_TRACKER,
                EVENT,
                Collections.singletonList(CREATIVE_VIEW)
        );

        for (final Node trackerNode : trackerNodes) {
            final String trackerUrl = XmlUtils.getNodeValue(trackerNode);
            companionCreativeViewTrackers.add(new VastTracker(trackerUrl));
        }

        return companionCreativeViewTrackers;
    }

    boolean hasResources() {
        return !TextUtils.isEmpty(mResourceXmlManager.getStaticResource()) ||
                !TextUtils.isEmpty(mResourceXmlManager.getHTMLResource()) ||
                !TextUtils.isEmpty(mResourceXmlManager.getIFrameResource());
    }
}
