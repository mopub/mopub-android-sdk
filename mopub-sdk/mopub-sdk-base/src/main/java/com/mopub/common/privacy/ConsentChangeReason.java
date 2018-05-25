package com.mopub.common.privacy;

import android.support.annotation.NonNull;

public enum ConsentChangeReason {
    GRANTED_BY_USER("Consent was explicitly granted by the user"),
    GRANTED_BY_WHITELISTED_PUB("Consent was explicitly granted by a whitelisted publisher"),
    GRANTED_BY_NOT_WHITELISTED_PUB(
            "Consent was explicitly granted by a publisher who is not whitelisted"),
    DENIED_BY_USER("Consent was explicitly denied by the user"),
    DENIED_BY_PUB("Consent was explicitly denied by the publisher"),
    DENIED_BY_DNT_ON("Limit ad tracking was enabled and consent implicitly denied by the user"),
    DNT_OFF("Limit ad tracking was disabled"),
    REACQUIRE_BECAUSE_DNT_OFF(
            "Consent needs to be reacquired because the user disabled limit ad tracking"),
    REACQUIRE_BECAUSE_PRIVACY_POLICY(
            "Consent needs to be reacquired because the privacy policy has changed"),
    REACUIRE_BECAUSE_VENDOR_LIST(
            "Consent needs to be reacquired because the vendor list has changed"),
    REAQUIRE_BECAUSE_IAB_VENDOR_LIST(
            "Consent needs to be reacquired because the IAB vendor list has changed"),
    REVOKED_BY_SERVER("Consent was revoked by the server"),
    REACQUIRE_BY_SERVER("Server requires that consent needs to be reacquired"),
    IFA_CHANGED("Consent needs to be reacquired because the IFA has changed");

    @NonNull private final String mReason;

    ConsentChangeReason(@NonNull final String reason) {
        mReason = reason;
    }

    @NonNull
    public String getReason() {
        return mReason;
    }
}
