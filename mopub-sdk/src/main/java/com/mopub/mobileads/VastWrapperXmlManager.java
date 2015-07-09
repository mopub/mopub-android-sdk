package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * This XML manager handles Wrapper nodes. Wrappers redirect to other VAST documents (which may
 * in turn redirect to more wrappers). Wrappers can also contain impression trackers,
 * trackers for a video ad, and companion ads.
 */
class VastWrapperXmlManager extends VastBaseInLineWrapperXmlManager {

    // Element names
    private static final String VAST_AD_TAG = "VASTAdTagURI";

    VastWrapperXmlManager(@NonNull final Node wrapperNode) {
        super(wrapperNode);
        Preconditions.checkNotNull(wrapperNode);
    }

    /**
     * Gets the redirect URI to the next VAST xml document. If no redirect URL, return null.
     *
     * @return The redirect URI or {@code null} if there isn't one.
     */
    @Nullable
    String getVastAdTagURI() {
        Node vastAdTagURINode = XmlUtils.getFirstMatchingChildNode(mNode, VAST_AD_TAG);
        return XmlUtils.getNodeValue(vastAdTagURINode);
    }
}
