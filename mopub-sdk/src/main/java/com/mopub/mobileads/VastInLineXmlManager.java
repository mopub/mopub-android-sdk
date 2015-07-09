package com.mopub.mobileads;

import android.support.annotation.NonNull;

import org.w3c.dom.Node;

/**
 * This XML manager handles InLine nodes. An InLine node can contain impression trackers,
 * video ads, and companion ads.
 */
class VastInLineXmlManager extends VastBaseInLineWrapperXmlManager {

    VastInLineXmlManager(@NonNull final Node inLineNode) {
        super(inLineNode);
    }
}
