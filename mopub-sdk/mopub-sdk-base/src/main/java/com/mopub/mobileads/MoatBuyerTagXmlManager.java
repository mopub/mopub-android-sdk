package com.mopub.mobileads;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;
import com.mopub.mobileads.util.XmlUtils;

import org.w3c.dom.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Data Object for Moat's VAST Custom Extension.
 */
class MoatBuyerTagXmlManager {
    // Elements
    private static final String VIEWABLE_IMPRESSION = "ViewableImpression";

    // Attributes
    private static final String ID = "id";

    private final List<Node> mMoatVerificationNodes;

    MoatBuyerTagXmlManager(@NonNull final List<Node> moatVerificationNodes) {
        Preconditions.checkNotNull(moatVerificationNodes);

        mMoatVerificationNodes = moatVerificationNodes;
    }

    /**
     * Return the corresponding impression pixels for Moat-related Verification nodes.
     *
     * Expected Extension node:
     * <Extension>
     *   <AdVerifications>
     *     <Verification vendor="Moat">
     *       <ViewableImpression id="${BUYER_AD_SERVER_MACRO[S]}">
     *         <![CDATA[
     *           https://px.moatads.com/pixel.gif?moatPartnerCode=${MOAT_PARTNER_CODE}
     *         ]]
     *       </ViewableImpression>
     *     </Verification>
     *   </AdVerifications>
     * </Extension>
     *
     * @return Collection of impression pixel tags in string form, i.e.
     * <ViewableImpression id="${BUYER_AD_SERVER_MACRO[S]}">
     *   <![CDATA[https://px.moatads.com/pixel.gif?moatPartnerCode=${MOAT_PARTNER_CODE}]]
     * </ViewableImpression>
     */
    @NonNull
    Set<String> getImpressionPixelsXml() {
        final Set<String> impressionPixelsXml = new HashSet<String>();

        for (final Node verification : mMoatVerificationNodes) {
            if (verification == null) {
                continue;
            }

            final Node viewableImpression = XmlUtils.getFirstMatchingChildNode(verification,
                    VIEWABLE_IMPRESSION);

            final String viewableImpressionXml = getViewableImpressionXml(viewableImpression);
            if (viewableImpressionXml != null) {
                impressionPixelsXml.add(viewableImpressionXml);
            }
        }

        return impressionPixelsXml;
    }

    @Nullable
    private String getViewableImpressionXml(@Nullable final Node viewableImpression) {
        if (viewableImpression == null || !viewableImpression.hasAttributes()) {
            return null;
        }

        final String idAttribute = XmlUtils.getAttributeValue(viewableImpression, ID);
        final String content = XmlUtils.getNodeValue(viewableImpression);
        return String.format(Locale.US,
                "<ViewableImpression id=\"%s\"><![CDATA[%s]]</ViewableImpression>",
                idAttribute, content);
    }
}
