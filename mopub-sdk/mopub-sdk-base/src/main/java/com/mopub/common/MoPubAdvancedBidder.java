package com.mopub.common;

import android.content.Context;

/**
 * Interface for all advanced bidders.
 */
public interface MoPubAdvancedBidder {
    public String getToken(Context context);
    public String getCreativeNetworkName();
}
