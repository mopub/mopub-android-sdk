package com.mopub.common.privacy;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Possible GDPR consent states.
 */
public enum ConsentStatus {
    /**
     * User has explicitly granted consent.
     */
    EXPLICIT_YES("explicit_yes"),

    /**
     * User has explicitly denied consent.
     */
    EXPLICIT_NO("explicit_no"),

    /**
     * The consent state is unknown due to not having synced to the server, or the user has never
     * set a consent state.
     */
    UNKNOWN("unknown"),

    /**
     * The SDK has set the consent state to EXPLICIT_YES, but that is not something this publisher
     * can do since they are not whitelisted. This state is treated as if the user has not granted
     * consent, but the user should no longer be prompted with the dialog.
     */
    POTENTIAL_WHITELIST("potential_whitelist"),

    /**
     * "Do Not Track". The user has set the limit ad tracking flag on their device. This is as if
     * the user has denied consent.
     */
    DNT("dnt");

    @NonNull final private String mValue;

    ConsentStatus(@NonNull final String value) {
        mValue = value;
    }

    /**
     * AdServer expects these values in this format.
     *
     * @return String value of the enum.
     */
    @NonNull
    public String getValue() {
        return mValue;
    }

    @NonNull
    public static ConsentStatus fromString(@Nullable final String name) {
        if (name == null) {
            return UNKNOWN;
        }

        for (ConsentStatus consentState : ConsentStatus.values()) {
            if (name.equals(consentState.name())) {
                return consentState;
            }
        }

        return UNKNOWN;
    }
}
