package com.mopub.common.privacy;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.Preconditions;

public class SyncResponse {

    private final boolean mIsGdprRegion;
    private final boolean mForceExplicitNo;
    private final boolean mInvalidateConsent;
    private final boolean mReacquireConsent;
    private final boolean mIsWhitelisted;
    @NonNull private final String mCurrentVendorListVersion;
    @NonNull private final String mCurrentVendorListLink;
    @NonNull private final String mCurrentPrivacyPolicyVersion;
    @NonNull private final String mCurrentPrivacyPolicyLink;
    @Nullable private final String mCurrentVendorListIabFormat;
    @NonNull private final String mCurrentVendorListIabHash;
    @Nullable private final String mCallAgainAfterSecs;
    @Nullable private final String mExtras;
    @Nullable private final String mConsentChangeReason;

    public boolean isGdprRegion() {
        return mIsGdprRegion;
    }

    public boolean isForceExplicitNo() {
        return mForceExplicitNo;
    }

    public boolean isInvalidateConsent() {
        return mInvalidateConsent;
    }

    public boolean isReacquireConsent() {
        return mReacquireConsent;
    }

    public boolean isWhitelisted() {
        return mIsWhitelisted;
    }

    @NonNull
    public String getCurrentVendorListVersion() {
        return mCurrentVendorListVersion;
    }

    @NonNull
    public String getCurrentVendorListLink() {
        return mCurrentVendorListLink;
    }

    @NonNull
    public String getCurrentPrivacyPolicyVersion() {
        return mCurrentPrivacyPolicyVersion;
    }

    @NonNull
    public String getCurrentPrivacyPolicyLink() {
        return mCurrentPrivacyPolicyLink;
    }

    @Nullable
    public String getCurrentVendorListIabFormat() {
        return mCurrentVendorListIabFormat;
    }

    @NonNull
    public String getCurrentVendorListIabHash() {
        return mCurrentVendorListIabHash;
    }

    @Nullable
    public String getCallAgainAfterSecs() {
        return mCallAgainAfterSecs;
    }

    @Nullable
    String getExtras() {
        return mExtras;
    }

    @Nullable
    public String getConsentChangeReason() {
        return mConsentChangeReason;
    }

    private SyncResponse(@NonNull final String isGdprRegion,
            @Nullable final String forceExplicitNo,
            @Nullable final String invalidateConsent,
            @Nullable final String reacquireConsent,
            @NonNull final String isWhitelisted,
            @NonNull final String currentVendorListVersion,
            @NonNull final String currentVendorListLink,
            @NonNull final String currentPrivacyPolicyVersion,
            @NonNull final String currentPrivacyPolicyLink,
            @Nullable final String currentVendorListIabFormat,
            @NonNull final String currentVendorListIabHash,
            @Nullable final String callAgainAfterSecs,
            @Nullable final String extras,
            @Nullable final String consentChangeReason) {
        Preconditions.checkNotNull(isGdprRegion);
        Preconditions.checkNotNull(isWhitelisted);
        Preconditions.checkNotNull(currentVendorListVersion);
        Preconditions.checkNotNull(currentVendorListLink);
        Preconditions.checkNotNull(currentPrivacyPolicyVersion);
        Preconditions.checkNotNull(currentPrivacyPolicyLink);
        Preconditions.checkNotNull(currentVendorListIabHash);

        // Default for this is true
        mIsGdprRegion = !"0".equals(isGdprRegion);

        // Default for the next four is false
        mForceExplicitNo = "1".equals(forceExplicitNo);
        mInvalidateConsent = "1".equals(invalidateConsent);
        mReacquireConsent = "1".equals(reacquireConsent);
        mIsWhitelisted = "1".equals(isWhitelisted);

        mCurrentVendorListVersion = currentVendorListVersion;
        mCurrentVendorListLink = currentVendorListLink;
        mCurrentPrivacyPolicyVersion = currentPrivacyPolicyVersion;
        mCurrentPrivacyPolicyLink = currentPrivacyPolicyLink;
        mCurrentVendorListIabFormat = currentVendorListIabFormat;
        mCurrentVendorListIabHash = currentVendorListIabHash;
        mCallAgainAfterSecs = callAgainAfterSecs;
        mExtras = extras;
        mConsentChangeReason = consentChangeReason;
    }

    public static class Builder {
        private String isGdprRegion;
        private String forceExplicitNo;
        private String invalidateConsent;
        private String reacquireConsent;
        private String isWhitelisted;
        private String currentVendorListVersion;
        private String currentVendorListLink;
        private String currentPrivacyPolicyVersion;
        private String currentPrivacyPolicyLink;
        private String currentVendorListIabFormat;
        private String currentVendorListIabHash;
        private String callAgainAfterSecs;
        private String extras;
        private String consentChangeReason;

        public Builder() {
        }

        public Builder setIsGdprRegion(@NonNull final String isGdprRegion) {
            this.isGdprRegion = isGdprRegion;
            return this;
        }

        public Builder setForceExplicitNo(@Nullable final String forceExplicitNo) {
            this.forceExplicitNo = forceExplicitNo;
            return this;
        }

        public Builder setInvalidateConsent(@Nullable final String invalidateConsent) {
            this.invalidateConsent = invalidateConsent;
            return this;
        }

        public Builder setReacquireConsent(@Nullable final String reacquireConsent) {
            this.reacquireConsent = reacquireConsent;
            return this;
        }

        public Builder setIsWhitelisted(@NonNull final String isWhitelisted) {
            this.isWhitelisted = isWhitelisted;
            return this;
        }

        public Builder setCurrentVendorListVersion(@NonNull final String currentVendorListVersion) {
            this.currentVendorListVersion = currentVendorListVersion;
            return this;
        }

        public Builder setCurrentVendorListLink(@NonNull final String currentVendorListLink) {
            this.currentVendorListLink = currentVendorListLink;
            return this;
        }

        public Builder setCurrentPrivacyPolicyVersion(
                @NonNull final String currentPrivacyPolicyVersion) {
            this.currentPrivacyPolicyVersion = currentPrivacyPolicyVersion;
            return this;
        }

        public Builder setCurrentPrivacyPolicyLink(@NonNull final String currentPrivacyPolicyLink) {
            this.currentPrivacyPolicyLink = currentPrivacyPolicyLink;
            return this;
        }

        public Builder setCurrentVendorListIabFormat(
                @Nullable final String currentVendorListIabFormat) {
            this.currentVendorListIabFormat = currentVendorListIabFormat;
            return this;
        }

        public Builder setCurrentVendorListIabHash(@NonNull final String currentVendorListIabHash) {
            this.currentVendorListIabHash = currentVendorListIabHash;
            return this;
        }

        public Builder setCallAgainAfterSecs(@Nullable final String callAgainAfterSecs) {
            this.callAgainAfterSecs = callAgainAfterSecs;
            return this;
        }

        public Builder setExtras(@Nullable final String extras) {
            this.extras = extras;
            return this;
        }

        public Builder setConsentChangeReason(@Nullable final String consentChangeReason) {
            this.consentChangeReason = consentChangeReason;
            return this;
        }

        public SyncResponse build() {
            return new SyncResponse(isGdprRegion, forceExplicitNo, invalidateConsent,
                    reacquireConsent, isWhitelisted, currentVendorListVersion,
                    currentVendorListLink, currentPrivacyPolicyVersion, currentPrivacyPolicyLink,
                    currentVendorListIabFormat, currentVendorListIabHash, callAgainAfterSecs,
                    extras, consentChangeReason);
        }
    }
}
