package com.mopub.mobileads.util.vast;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

class VastXmlManager {
    private static final String ROOT_TAG = "MPMoVideoXMLDocRoot";
    private static final String ROOT_TAG_OPEN = "<" + ROOT_TAG + ">";
    private static final String ROOT_TAG_CLOSE = "</" + ROOT_TAG + ">";

    // Element names
    private static final String IMPRESSION_TRACKER = "Impression";
    private static final String VIDEO_TRACKER = "Tracking";
    private static final String CLICK_THROUGH = "ClickThrough";
    private static final String CLICK_TRACKER = "ClickTracking";
    private static final String MEDIA_FILE = "MediaFile";
    private static final String VAST_AD_TAG = "VASTAdTagURI";
    private static final String MP_IMPRESSION_TRACKER = "MP_TRACKING_URL";
    private static final String COMPANION = "Companion";

    // Attribute names
    private static final String EVENT = "event";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";

    // Attibute values
    private static final String START = "start";
    private static final String FIRST_QUARTILE = "firstQuartile";
    private static final String MIDPOINT = "midpoint";
    private static final String THIRD_QUARTILE = "thirdQuartile";
    private static final String COMPLETE = "complete";

    // This class currently assumes an image type companion ad since that is what we are supporting
    class ImageCompanionAdXmlManager {
        // Element name
        private static final String TRACKING_EVENTS = "TrackingEvents";
        private static final String COMPANION_STATIC_RESOURCE = "StaticResource";
        private static final String COMPANION_CLICK_THROUGH = "CompanionClickThrough";
        // Attribute value
        private static final String CREATIVE_VIEW = "creativeView";
        // Attribute name
        private static final String CREATIVE_TYPE = "creativeType";
        private final Node mCompanionNode;

        ImageCompanionAdXmlManager(final Node companionNode) throws IllegalArgumentException {
            if (companionNode == null) {
                throw new IllegalArgumentException("Companion node cannot be null");
            }
            mCompanionNode = companionNode;
        }

        Integer getWidth() {
            return XmlUtils.getAttributeValueAsInt(mCompanionNode, WIDTH);
        }

        Integer getHeight() {
            return XmlUtils.getAttributeValueAsInt(mCompanionNode, HEIGHT);
        }

        String getType() {
            final Node node = XmlUtils.getFirstMatchingChildNode(
                    mCompanionNode,
                    COMPANION_STATIC_RESOURCE
            );
            return XmlUtils.getAttributeValue(node, CREATIVE_TYPE);
        }

        String getImageUrl() {
            final Node node = XmlUtils.getFirstMatchingChildNode(
                    mCompanionNode,
                    COMPANION_STATIC_RESOURCE
            );
            return XmlUtils.getNodeValue(node);
        }

        String getClickThroughUrl() {
            final Node node = XmlUtils.getFirstMatchingChildNode(
                    mCompanionNode,
                    COMPANION_CLICK_THROUGH
            );
            return XmlUtils.getNodeValue(node);
        }

        List<String> getClickTrackers() {
            final List<String> companionAdClickTrackers = new ArrayList<String>();
            final Node node = XmlUtils.getFirstMatchingChildNode(
                    mCompanionNode,
                    TRACKING_EVENTS
            );

            if (node == null) {
                return companionAdClickTrackers;
            }

            final List<Node> trackerNodes = XmlUtils.getMatchingChildNodes(
                    node,
                    VIDEO_TRACKER,
                    EVENT,
                    Arrays.asList(CREATIVE_VIEW)
            );

            for (final Node trackerNode : trackerNodes) {
                if (trackerNode.getFirstChild() != null) {
                    companionAdClickTrackers.add(trackerNode.getFirstChild().getNodeValue().trim());
                }
            }

            return companionAdClickTrackers;
        }
    }

    class MediaXmlManager {
        // Attribute names
        private static final String DELIVERY = "delivery";
        private static final String VIDEO_TYPE  = "type";
        private final Node mMediaNode;

        MediaXmlManager(final Node mediaNode) throws IllegalArgumentException {
            if (mediaNode == null) {
                throw new IllegalArgumentException("Media node cannot be null");
            }
            mMediaNode = mediaNode;
        }

        String getDelivery() {
            return XmlUtils.getAttributeValue(mMediaNode, DELIVERY);
        }

        Integer getWidth() {
            return XmlUtils.getAttributeValueAsInt(mMediaNode, WIDTH);
        }

        Integer getHeight() {
            return XmlUtils.getAttributeValueAsInt(mMediaNode, HEIGHT);
        }

        String getType() {
            return XmlUtils.getAttributeValue(mMediaNode, VIDEO_TYPE);
        }

        String getMediaUrl() {
            return XmlUtils.getNodeValue(mMediaNode);
        }
    }

    private Document mVastDoc;

    void parseVastXml(String xmlString) throws ParserConfigurationException, IOException, SAXException {
        // if the xml string starts with <?xml?>, this tag can break parsing if it isn't formatted exactly right
        // or if it's not the first line of the document...we're just going to strip it
        xmlString = xmlString.replaceFirst("<\\?.*\\?>", "");

        // adserver may embed additional impression trackers as a sibling node of <VAST>
        // wrap entire document in root node for this case.
        String documentString = ROOT_TAG_OPEN + xmlString + ROOT_TAG_CLOSE;

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setCoalescing(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        mVastDoc = documentBuilder.parse(new InputSource(new StringReader(documentString)));
    }

    String getVastAdTagURI() {
        List<String> uriWrapper = XmlUtils.getStringDataAsList(mVastDoc, VAST_AD_TAG);
        return (uriWrapper.size() > 0) ? uriWrapper.get(0) : null;
    }

    List<String> getImpressionTrackers() {
        List<String> impressionTrackers = XmlUtils.getStringDataAsList(mVastDoc, IMPRESSION_TRACKER);
        impressionTrackers.addAll(XmlUtils.getStringDataAsList(mVastDoc, MP_IMPRESSION_TRACKER));

        return impressionTrackers;
    }

    List<String> getVideoStartTrackers() {
        return getVideoTrackerByAttribute(START);
    }

    List<String> getVideoFirstQuartileTrackers() {
        return getVideoTrackerByAttribute(FIRST_QUARTILE);
    }

    List<String> getVideoMidpointTrackers() {
        return getVideoTrackerByAttribute(MIDPOINT);
    }

    List<String> getVideoThirdQuartileTrackers() {
        return getVideoTrackerByAttribute(THIRD_QUARTILE);
    }

    List<String> getVideoCompleteTrackers() {
        return getVideoTrackerByAttribute(COMPLETE);
    }

    String getClickThroughUrl() {
        List<String> clickUrlWrapper = XmlUtils.getStringDataAsList(mVastDoc, CLICK_THROUGH);
        return (clickUrlWrapper.size() > 0) ? clickUrlWrapper.get(0) : null;
    }

    List<String> getClickTrackers() {
        return XmlUtils.getStringDataAsList(mVastDoc, CLICK_TRACKER);
    }

    String getMediaFileUrl() {
        List<String> urlWrapper = XmlUtils.getStringDataAsList(mVastDoc, MEDIA_FILE);
        return (urlWrapper.size() > 0) ? urlWrapper.get(0) : null;
    }

    List<MediaXmlManager> getMediaXmlManagers() {
        final NodeList nodes = mVastDoc.getElementsByTagName(MEDIA_FILE);
        final List<MediaXmlManager> mediaXmlManagers =
                new ArrayList<MediaXmlManager>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); ++i) {
            mediaXmlManagers.add(new MediaXmlManager(nodes.item(i)));
        }
        return mediaXmlManagers;
    }

    List<ImageCompanionAdXmlManager> getCompanionAdXmlManagers() {
        final NodeList nodes = mVastDoc.getElementsByTagName(COMPANION);
        final List<ImageCompanionAdXmlManager> imageCompanionAdXmlManagers =
                new ArrayList<ImageCompanionAdXmlManager>(nodes.getLength());
        for (int i = 0; i < nodes.getLength(); ++i) {
            imageCompanionAdXmlManagers.add(new ImageCompanionAdXmlManager(nodes.item(i)));
        }
        return imageCompanionAdXmlManagers;
    }

    private List<String> getVideoTrackerByAttribute(final String attributeValue) {
        return XmlUtils.getStringDataAsList(mVastDoc, VIDEO_TRACKER, EVENT, attributeValue);
    }
}
