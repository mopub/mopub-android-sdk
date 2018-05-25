package com.mopub.common.privacy;

import android.support.annotation.NonNull;

/**
 * Keys for gdpr sync, consent dialog requests, and setting consent state.
 */
public enum PrivacyKey {
    IS_GDPR_REGION("is_gdpr_region"),
    IS_WHITELISTED("is_whitelisted"),
    FORCE_EXPLICIT_NO("force_explicit_no"),
    INVALIDATE_CONSENT("invalidate_consent"),
    REACQUIRE_CONSENT("reacquire_consent"),
    EXTRAS("extras"),
    CURRENT_VENDOR_LIST_VERSION("current_vendor_list_version"),
    CURRENT_VENDOR_LIST_LINK("current_vendor_list_link"),
    CURRENT_PRIVACY_POLICY_VERSION("current_privacy_policy_version"),
    CURRENT_PRIVACY_POLICY_LINK("current_privacy_policy_link"),
    CURRENT_VENDOR_LIST_IAB_FORMAT("current_vendor_list_iab_format"),
    CURRENT_VENDOR_LIST_IAB_HASH("current_vendor_list_iab_hash"),
    CALL_AGAIN_AFTER_SECS("call_again_after_secs"),
    CONSENT_CHANGE_REASON("consent_change_reason");

    @NonNull private final String key;
    PrivacyKey(@NonNull final String key) {
        this.key = key;
    }

    @NonNull
    public String getKey() {
        return this.key;
    }
}
