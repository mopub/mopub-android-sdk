package com.mopub.mobileads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;

class ConversionUrlGenerator extends BaseUrlGenerator {
    private static final String SESSION_TRACKER_KEY = "st";
    private static final String PACKAGE_NAME_KEY = "id";

    @NonNull
    private Context mContext;
    @Nullable
    private String mCurrentConsentStatus;
    @Nullable
    private String mConsentedVendorListVersion;
    @Nullable
    private String mConsentedPrivacyPolicyVersion;
    @Nullable
    private Boolean mGdprApplies;

    private boolean mSt;

    ConversionUrlGenerator(@NonNull final Context context) {
        mContext = context;
    }

    public ConversionUrlGenerator withCurrentConsentStatus(
            @Nullable final String currentConsentStatus) {
        mCurrentConsentStatus = currentConsentStatus;
        return this;
    }

    public ConversionUrlGenerator withGdprApplies(@Nullable final Boolean gdprApplies) {
        mGdprApplies = gdprApplies;
        return this;
    }

    public ConversionUrlGenerator withConsentedVendorListVersion(@Nullable final String consentedVendorListVersion) {
        mConsentedVendorListVersion = consentedVendorListVersion;
        return this;
    }

    public ConversionUrlGenerator withConsentedPrivacyPolicyVersion(@Nullable final String consentedPrivacyPolicyVersion) {
        mConsentedPrivacyPolicyVersion = consentedPrivacyPolicyVersion;
        return this;
    }

    public ConversionUrlGenerator withSessionTracker(final boolean st) {
        mSt = st;
        return this;
    }

    @Override
    public String generateUrlString(String serverHostname) {
        ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);

        initUrlString(serverHostname, Constants.CONVERSION_TRACKING_HANDLER);
        setApiVersion("6");
        setAppVersion(clientMetadata.getAppVersion());
        appendAdvertisingInfoTemplates();

        addParam(PACKAGE_NAME_KEY, mContext.getPackageName());
        if (mSt) {
            addParam(SESSION_TRACKER_KEY, true);
        }
        addParam(SDK_VERSION_KEY, MoPub.SDK_VERSION);
        addParam(CURRENT_CONSENT_STATUS_KEY, mCurrentConsentStatus);
        addParam(CONSENTED_VENDOR_LIST_VERSION_KEY, mConsentedVendorListVersion);
        addParam(CONSENTED_PRIVACY_POLICY_VERSION_KEY, mConsentedPrivacyPolicyVersion);
        if (mGdprApplies != null) {
            addParam(GDPR_APPLIES, mGdprApplies);
        }
        return getFinalUrlString();
    }
}
