package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

/**
 * This XML manager handles the initial Ad node.
 * There can be many Ad nodes in a VAST document, and this individually separates each one.
 * The VAST 3.0 spec for Ad nodes states it can have either <i>one</i> InLine or <i>one</i>
 * Wrapper as children. However, our implementation handles the case of having both an InLine
 * and a Wrapper.
 */
class VastAdXmlManager {

    // Element names
    private static final String INLINE = "InLine";
    private static final String WRAPPER = "Wrapper";

    // Attribute names
    private static final String SEQUENCE = "sequence";

    @NonNull private final Node mAdNode;

    VastAdXmlManager(@NonNull final Node adNode) {
        Preconditions.checkNotNull(adNode);
        mAdNode = adNode;
    }

    /**
     * If there is an InLine section, return its XML manager.
     *
     * @return The {@link VastInLineXmlManager} or {@code null} if there is no InLine child node.
     */
    @Nullable
    VastInLineXmlManager getInLineXmlManager() {
        Node inLineNode = XmlUtils.getFirstMatchingChildNode(mAdNode, INLINE);
        VastInLineXmlManager vastInLineXmlManager = null;
        if (inLineNode != null) {
            vastInLineXmlManager = new VastInLineXmlManager(inLineNode);
        }
        return vastInLineXmlManager;
    }

    /**
     * If there is a Wrapper section, return its XML manager.
     *
     * @return The {@link VastWrapperXmlManager} or {@code null} if there is no Wrapper child node.
     */
    @Nullable
    VastWrapperXmlManager getWrapperXmlManager() {
        Node wrapperNode = XmlUtils.getFirstMatchingChildNode(mAdNode, WRAPPER);
        VastWrapperXmlManager vastWrapperXmlManager = null;
        if (wrapperNode != null) {
            vastWrapperXmlManager = new VastWrapperXmlManager(wrapperNode);
        }
        return vastWrapperXmlManager;
    }

    /**
     * Gets the attribute for sequence number. This attribute is optional.
     *
     * @return The sequence number
     */
    @Nullable
    String getSequence() {
        return XmlUtils.getAttributeValue(mAdNode, SEQUENCE);
    }
}
