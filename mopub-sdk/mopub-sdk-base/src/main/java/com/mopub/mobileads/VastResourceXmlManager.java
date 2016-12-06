package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

public class VastResourceXmlManager {

    // Element names
    public static final String STATIC_RESOURCE = "StaticResource";
    public static final String IFRAME_RESOURCE = "IFrameResource";
    public static final String HTML_RESOURCE = "HTMLResource";

    // Attribute names
    public static final String CREATIVE_TYPE = "creativeType";

    @NonNull private final Node mResourceNode;

    VastResourceXmlManager(@NonNull final Node resourceNode) {
        Preconditions.checkNotNull(resourceNode);
        mResourceNode = resourceNode;
    }

    /**
     * If this node has a static resource, then this method returns the static resource data,
     * if present. This returns {@code null} if this node does not have a static resource.
     *
     * @return The static resource data or {@code null}
     */
    @Nullable
    String getStaticResource() {
        return XmlUtils.getNodeValue(XmlUtils.getFirstMatchingChildNode(mResourceNode, STATIC_RESOURCE));
    }

    /**
     * If this node has a static resource, then this method returns the type of the static resource.
     * This returns {@code null} if this node does not have a static resource.
     *
     * @return The static resource type or {@code null}
     */
    @Nullable
    String getStaticResourceType() {
        Node staticResource = XmlUtils.getFirstMatchingChildNode(mResourceNode, STATIC_RESOURCE);
        String attribute = XmlUtils.getAttributeValue(staticResource, CREATIVE_TYPE);
        if (attribute != null) {
            return attribute.toLowerCase();
        }
        return null;
    }

    /**
     * If this node has an iframe resource, then this method returns the iframe resource data, if
     * present. This returns {@code null} if this node does not have an iframe resource.
     *
     * @return The iframe resource data or {@code null}
     */
    @Nullable
    String getIFrameResource() {
        return XmlUtils.getNodeValue(XmlUtils.getFirstMatchingChildNode(mResourceNode, IFRAME_RESOURCE));
    }

    /**
     * If this node has an HTML resource, then this method returns the HTML resource data, if
     * present. This returns {@code null} if this node does not have an HTML resource.
     *
     * @return The HTML resource data or {@code null}
     */
    @Nullable
    String getHTMLResource() {
        return XmlUtils.getNodeValue(XmlUtils.getFirstMatchingChildNode(mResourceNode, HTML_RESOURCE));
    }
}
