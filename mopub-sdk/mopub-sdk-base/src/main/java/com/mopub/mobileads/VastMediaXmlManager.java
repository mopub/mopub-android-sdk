package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

/**
 * This XML manager handles the actual video.
 */
class VastMediaXmlManager {

    // Attribute names
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String DELIVERY = "delivery";
    private static final String VIDEO_TYPE  = "type";

    @NonNull private final Node mMediaNode;

    VastMediaXmlManager(@NonNull final Node mediaNode) {
        Preconditions.checkNotNull(mediaNode, "mediaNode cannot be null");
        mMediaNode = mediaNode;
    }

    /**
     * 'progressive' for progressive download (e.g. HTTP) or 'streaming' for streaming protocols
     * or {@code null} if not specified. MoPub expects to download the video. This is a required
     * attribute.
     *
     * @return String of delivery type or {@code null}
     */
    @Nullable
    String getDelivery() {
        return XmlUtils.getAttributeValue(mMediaNode, DELIVERY);
    }

    /**
     * Expected width of the video in pixels or {@code null} if not specified. This is a
     * required attribute.
     *
     * @return Integer width of video or {@code null}
     */
    @Nullable
    Integer getWidth() {
        return XmlUtils.getAttributeValueAsInt(mMediaNode, WIDTH);
    }

    /**
     * Expected height of the video in pixels or {@code null} if not specified. This is a
     * required attribute.
     *
     * @return Integer height of video or {@code null}
     */
    @Nullable
    Integer getHeight() {
        return XmlUtils.getAttributeValueAsInt(mMediaNode, HEIGHT);
    }

    /**
     * The MIME file type of the video or {@code null} if not specified. This is a required
     * attribute. (e.g. 'video/x-flv' or 'video/mp4').
     *
     * @return The String type or {@code null}
     */
    @Nullable
    String getType() {
        return XmlUtils.getAttributeValue(mMediaNode, VIDEO_TYPE);
    }

    /**
     * The URL of the video or {@code null} if not specified.
     *
     * @return String url of video or {@code null}
     */
    @Nullable
    String getMediaUrl() {
        return XmlUtils.getNodeValue(mMediaNode);
    }
}
