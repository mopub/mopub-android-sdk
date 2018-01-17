package com.mopub.mobileads;

import android.support.annotation.NonNull;

/**
 * Certified with Vungle SDK 5.3.0
 */
public interface VungleRouterListener {
    void onAdEnd(@NonNull String var1, boolean var2, boolean var3);

    void onAdStart(@NonNull String var1);

    void onUnableToPlayAd(@NonNull String var1, String var2);

    void onAdAvailabilityUpdate(@NonNull String var1, boolean var2);
}
