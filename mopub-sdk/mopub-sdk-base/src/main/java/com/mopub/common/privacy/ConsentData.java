// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Has all the getters for getting the current and consented data around vendor lists and
 * privacy policies.
 */
public interface ConsentData {
    /**
     * Returns the current vendor list version.
     */
    @Nullable
    String getCurrentVendorListVersion();

    /**
     * Returns a link for the current vendor list with the system default language.
     */
    @NonNull
    String getCurrentVendorListLink();

    /**
     * Returns a link for the current vendor list in a specific language. If the language is not a
     * valid language, this will return the link with the system default language.
     *
     * @param language ISO 639-1 2-letter language code
     */
    @NonNull
    String getCurrentVendorListLink(@Nullable final String language);

    /**
     * Returns the current privacy policy version.
     */
    @Nullable
    String getCurrentPrivacyPolicyVersion();

    /**
     * Returns a link for the current privacy policy with the system default language.
     */
    @NonNull
    String getCurrentPrivacyPolicyLink();

    /**
     * Returns a link for the current privacy policy in a specific language. If the language is not
     * a valid language, this will return the link with the system default language.
     *
     * @param language ISO 639-1 2-letter language code
     */
    @NonNull
    String getCurrentPrivacyPolicyLink(@Nullable final String language);

    /**
     * Returns the current vendor list in IAB's global vendor list format.
     */
    @Nullable
    String getCurrentVendorListIabFormat();

    /**
     * Returns the version of the privacy policy that the user has consented to.
     */
    @Nullable
    String getConsentedPrivacyPolicyVersion();

    /**
     * Returns the version of the vendor list that the user has consented to.
     */
    @Nullable
    String getConsentedVendorListVersion();

    /**
     * Returns the consented vendor list in IAB's global vendor list format.
     */
    @Nullable
    String getConsentedVendorListIabFormat();

    /**
     * Whether GDPR rules have been forced to apply by either the publisher or by MoPub. In
     * general, publishers should not need to directly access this.
     *
     * @return True means GDPR rules will be applied, false means respect MoPub's geofencing when
     * determining whether or not GDPR rules apply.
     */
    boolean isForceGdprApplies();
}
