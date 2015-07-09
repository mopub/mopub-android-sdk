package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Strings;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * This XML manager handles Vast 3.0 icons.
 */
public class VastIconXmlManager {

    // Element names
    public static final String ICON_CLICKS = "IconClicks";
    public static final String ICON_CLICK_TRACKING = "IconClickTracking";
    public static final String ICON_CLICK_THROUGH = "IconClickThrough";
    public static final String ICON_VIEW_TRACKING = "IconViewTracking";

    // Attribute names
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String OFFSET = "offset";
    public static final String DURATION = "duration";

    @NonNull private final Node mIconNode;
    @NonNull private final VastResourceXmlManager mResourceXmlManager;

    VastIconXmlManager(@NonNull final Node iconNode) {
        Preconditions.checkNotNull(iconNode);
        mIconNode = iconNode;
        mResourceXmlManager = new VastResourceXmlManager(iconNode);
    }

    /**
     * Gets the width attribute for the icon in dp or {@code null} if not present. This attribute
     * is required according to the VAST 3.0 spec.
     *
     * @return Integer width attribute or {@code null}.
     */
    @Nullable
    Integer getWidth() {
        return XmlUtils.getAttributeValueAsInt(mIconNode, WIDTH);
    }

    /**
     * Gets the height attribute for the icon in dp or {@code null} if not present. This attribute
     * is required according to the VAST 3.0 spec.
     *
     * @return Integer height attribute or {@code null}.
     */
    @Nullable
    Integer getHeight() {
        return XmlUtils.getAttributeValueAsInt(mIconNode, HEIGHT);
    }

    /**
     * Gets the offset attribute for the icon or {@code null} if not present or not formatted
     * correctly. It represents the time in milliseconds into the video that the icon will be
     * displayed. The attribute value is represented as HH:MM:SS[.mmm] and this method translates
     * it to milliseconds. This attribute is optional according to the VAST 3.0 spec.
     *
     * @return Integer offset in milliseconds attribute or {@code null}.
     */
    @Nullable
    Integer getOffsetMS() {
        String iconOffsetStr = XmlUtils.getAttributeValue(mIconNode, OFFSET);
        Integer iconOffset = null;
        try {
            iconOffset = Strings.parseAbsoluteOffset(iconOffsetStr);
        } catch (NumberFormatException e) {
            MoPubLog.d(String.format("Invalid VAST icon offset format: %s:", iconOffsetStr));
        }
        return iconOffset;
    }

    /**
     * Gets the duration attribute for the icon or {@code null} if not present or not formatted
     * correctly. It represents the duration in milliseconds that the icon will be displayed.
     * The attribute value is represented as HH:MM:SS[.mmm] and this method translates it to
     * milliseconds. This attribute is optional according to the VAST 3.0 spec.
     *
     * @return Integer duration in milliseconds attribute or {@code null}.
     */
    @Nullable
    Integer getDurationMS() {
        String iconDurationStr = XmlUtils.getAttributeValue(mIconNode, DURATION);
        Integer iconDuration = null;
        try {
            iconDuration = Strings.parseAbsoluteOffset(iconDurationStr);
        } catch (NumberFormatException e) {
            MoPubLog.d(String.format("Invalid VAST icon duration format: %s:", iconDurationStr));
        }
        return iconDuration;
    }

    @NonNull
    VastResourceXmlManager getResourceXmlManager() {
        return mResourceXmlManager;
    }

    @NonNull
    List<VastTracker> getClickTrackingUris() {
        Node iconClicksNode = XmlUtils.getFirstMatchingChildNode(mIconNode, ICON_CLICKS);
        List<VastTracker> iconClickTrackingUris = new ArrayList<VastTracker>();
        if (iconClicksNode == null) {
            return iconClickTrackingUris;
        }

        List<Node> iconClickTrackingNodes =
                XmlUtils.getMatchingChildNodes(iconClicksNode, ICON_CLICK_TRACKING);
        for (Node iconClickTrackingNode : iconClickTrackingNodes) {
            String uri = XmlUtils.getNodeValue(iconClickTrackingNode);
            if (uri != null) {
                iconClickTrackingUris.add(new VastTracker(uri));
            }
        }
        return iconClickTrackingUris;
    }

    @Nullable
    String getClickThroughUri() {
        Node iconClicksNode = XmlUtils.getFirstMatchingChildNode(mIconNode, ICON_CLICKS);
        if (iconClicksNode == null) {
            return null;
        }

        Node iconClickThroughNode =
                XmlUtils.getFirstMatchingChildNode(iconClicksNode, ICON_CLICK_THROUGH);
        return XmlUtils.getNodeValue(iconClickThroughNode);
    }

    @NonNull
    List<VastTracker> getViewTrackingUris() {
        List<Node> iconViewTrackingNodes =
                XmlUtils.getMatchingChildNodes(mIconNode, ICON_VIEW_TRACKING);
        List<VastTracker> iconViewTrackingUris = new ArrayList<VastTracker>();

        for (Node iconViewTrackingNode : iconViewTrackingNodes) {
            String uri = XmlUtils.getNodeValue(iconViewTrackingNode);
            if (uri != null) {
                iconViewTrackingUris.add(new VastTracker(uri));
            }
        }
        return iconViewTrackingUris;
    }
}
