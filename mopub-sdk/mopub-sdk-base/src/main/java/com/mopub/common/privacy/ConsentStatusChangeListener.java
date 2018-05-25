package com.mopub.common.privacy;

import android.support.annotation.NonNull;

/**
 * Add one of these to PersonalInfoManager#subscribeConsentStatusChangeListener to listen for
 * status changes.
 */
public interface ConsentStatusChangeListener {
    /**
     * Called when a consent status transition happens.
     *
     * @param oldConsentStatus              The old consent status.
     * @param newConsentStatus              The new consent status.
     * @param canCollectPersonalInformation True if the sdk is allowed to collect personally identifiable information, false otherwise.
     */
    void onConsentStateChange(@NonNull final ConsentStatus oldConsentStatus,
            @NonNull final ConsentStatus newConsentStatus,
            boolean canCollectPersonalInformation);
}
