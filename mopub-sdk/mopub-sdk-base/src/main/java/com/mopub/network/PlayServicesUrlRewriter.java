package com.mopub.network;

import android.net.Uri;

import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.privacy.AdvertisingId;
import com.mopub.common.privacy.MoPubIdentifier;
import com.mopub.volley.toolbox.HurlStack;

/**
 * Url Rewriter that replaces MoPub templates for Google Advertising ID and Do Not Track settings
 * when a request is queued for dispatch by the HurlStack in Volley.
 */
public class PlayServicesUrlRewriter implements HurlStack.UrlRewriter {
    public static final String UDID_TEMPLATE = "mp_tmpl_advertising_id";
    public static final String DO_NOT_TRACK_TEMPLATE = "mp_tmpl_do_not_track";

    public PlayServicesUrlRewriter() {
    }

    @Override
    public String rewriteUrl(final String url) {
        if (!url.contains(UDID_TEMPLATE) && !url.contains(DO_NOT_TRACK_TEMPLATE)) {
            return url;
        }

        ClientMetadata clientMetadata = ClientMetadata.getInstance();
        if (clientMetadata == null) {
            return url;
        }
        MoPubIdentifier identifier = clientMetadata.getMoPubIdentifier();
        AdvertisingId info = identifier.getAdvertisingInfo();
        String toReturn = url.replace(UDID_TEMPLATE,
                Uri.encode(info.getIdWithPrefix(MoPub.canCollectPersonalInformation())));
        toReturn = toReturn.replace(DO_NOT_TRACK_TEMPLATE, info.isDoNotTrack() ? "1" : "0");
        return toReturn;
    }
}
