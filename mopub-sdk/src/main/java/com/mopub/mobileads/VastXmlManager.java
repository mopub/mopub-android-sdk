package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.Preconditions;
import com.mopub.common.util.DeviceUtils.ForceOrientation;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Parses the VAST XML to spec. See www.iab.net for details.
 * Currently using the VAST 3.0 spec found here: http://www.iab.net/media/file/VASTv3.0.pdf.
 */
class VastXmlManager {
    private static final String ROOT_TAG = "MPMoVideoXMLDocRoot";
    private static final String ROOT_TAG_OPEN = "<" + ROOT_TAG + ">";
    private static final String ROOT_TAG_CLOSE = "</" + ROOT_TAG + ">";

    // Element names
    private static final String AD = "Ad";
    private static final String ERROR = "Error";

    // Custom element names for VAST 3.0 extensions
    private static final String MP_IMPRESSION_TRACKER = "MP_TRACKING_URL";
    private static final String CUSTOM_CTA_TEXT = "MoPubCtaText";
    private static final String CUSTOM_SKIP_TEXT = "MoPubSkipText";
    private static final String CUSTOM_CLOSE_ICON = "MoPubCloseIcon";
    private static final String CUSTOM_FORCE_ORIENTATION = "MoPubForceOrientation";

    // Constants for custom extensions
    private static final int MAX_CTA_TEXT_LENGTH = 15;
    private static final int MAX_SKIP_TEXT_LENGTH = 8;

    @Nullable private Document mVastDoc;

    /**
     * Helper function that builds a document and tries to parse the XML.
     *
     * @param xmlString The XML to parse
     * @throws ParserConfigurationException If the parser is poorly configured
     * @throws IOException                  If we can't read the document for any reason
     * @throws SAXException                 If the XML is poorly formatted
     */
    void parseVastXml(@NonNull String xmlString) throws ParserConfigurationException,
            IOException, SAXException {
        Preconditions.checkNotNull(xmlString, "xmlString cannot be null");

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

    /**
     * If there is an Ad element, return its XML manager. This is the starting point for VAST
     * XML documents, and VAST has this as the expected first child. There may be more than one Ad
     * node in a VAST document. This returns a list of all Ad nodes or an empty list if no Ad nodes
     * were found.
     *
     * @return A List of {@link VastAdXmlManager} or an
     * empty list if there is no Ad child node.
     */

    @NonNull
    List<VastAdXmlManager> getAdXmlManagers() {
        List<VastAdXmlManager> vastAdXmlManagers = new ArrayList<VastAdXmlManager>();
        if (mVastDoc == null) {
            return vastAdXmlManagers;
        }
        NodeList nodes = mVastDoc.getElementsByTagName(AD);
        for (int i = 0; i < nodes.getLength(); ++i) {
            vastAdXmlManagers.add(new VastAdXmlManager(nodes.item(i)));
        }
        return vastAdXmlManagers;
    }

    /**
     * Vast documents can have just an error element. This usually is used to indicate that no ad is
     * available. This gets the url of the error tracker.
     *
     * @return The URL of the error tracker or null if it does not exist.
     */
    @Nullable
    VastTracker getErrorTracker() {
        if (mVastDoc == null) {
            return null;
        }
        String errorTracker = XmlUtils.getFirstMatchingStringData(mVastDoc, ERROR);
        if (TextUtils.isEmpty(errorTracker)) {
            return null;
        }
        return new VastTracker(errorTracker);
    }

    /**
     * Gets a list of MoPub specific impression trackers.
     *
     * @return List of URL impression trackers or an empty list if none present.
     */
    @NonNull
    List<VastTracker> getMoPubImpressionTrackers() {
        List<String> trackers = XmlUtils.getStringDataAsList(mVastDoc, MP_IMPRESSION_TRACKER);
        List<VastTracker> vastTrackers = new ArrayList<VastTracker>(trackers.size());
        for (String tracker : trackers) {
            vastTrackers.add(new VastTracker(tracker));
        }
        return vastTrackers;
    }

    /**
     * Gets the custom call to action text or {@code null} if not specified or too long.
     *
     * @return String cta or {@code null}
     */
    @Nullable
    String getCustomCtaText() {
        String customCtaText = XmlUtils.getFirstMatchingStringData(mVastDoc, CUSTOM_CTA_TEXT);
        if (customCtaText != null && customCtaText.length() <= MAX_CTA_TEXT_LENGTH) {
            return customCtaText;
        }

        return null;
    }

    /**
     * Gets the custom text of the skip button or {@code null} if not specified or too long.
     *
     * @return String skip text or {@code null}
     */
    @Nullable
    String getCustomSkipText() {
        String customSkipText = XmlUtils.getFirstMatchingStringData(mVastDoc, CUSTOM_SKIP_TEXT);
        if (customSkipText != null && customSkipText.length() <= MAX_SKIP_TEXT_LENGTH) {
            return customSkipText;
        }

        return null;
    }

    /**
     * Gets the custom icon URL or {@code null} if none specified.
     *
     * @return String URL of the custom icon or {@code null}
     */
    @Nullable
    String getCustomCloseIconUrl() {
        return XmlUtils.getFirstMatchingStringData(mVastDoc, CUSTOM_CLOSE_ICON);
    }

    /**
     * Gets the orientation that this ad should be forced in. This returns UNDEFINED if not
     * specified.
     *
     * @return {@code ForceOrientation} orientation or {@code UNDEFINED}
     */
    @NonNull
    ForceOrientation getCustomForceOrientation() {
        return ForceOrientation.getForceOrientation(
                XmlUtils.getFirstMatchingStringData(mVastDoc, CUSTOM_FORCE_ORIENTATION));
    }
}
