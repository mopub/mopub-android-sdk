package com.mopub.common.privacy;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Has all the getters for getting the current and consented data around vendor lists and
 * privacy policies.
 */
public interface ConsentData {
    @Nullable
    String getCurrentVendorListVersion();

    @NonNull
    String getCurrentVendorListLink();

    @NonNull
    String getCurrentVendorListLink(@Nullable final String language);

    @Nullable
    String getCurrentPrivacyPolicyVersion();

    @NonNull
    String getCurrentPrivacyPolicyLink();

    @NonNull
    String getCurrentPrivacyPolicyLink(@Nullable final String language);

    @Nullable
    String getCurrentVendorListIabFormat();

    @Nullable
    String getConsentedPrivacyPolicyVersion();

    @Nullable
    String getConsentedVendorListVersion();

    @Nullable
    String getConsentedVendorListIabFormat();
}
