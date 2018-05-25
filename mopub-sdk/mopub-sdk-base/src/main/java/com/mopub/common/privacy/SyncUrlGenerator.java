package com.mopub.common.privacy;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.network.PlayServicesUrlRewriter;

public class SyncUrlGenerator extends BaseUrlGenerator {

    /**
     * Unix time, in ms, of the last time the consent status was changed.
     */
    private static final String LAST_CHANGED_MS_KEY = "last_changed_ms";

    /**
     * Previous consent state acknowledged by the server.
     */
    private static final String LAST_CONSENT_STATUS_KEY = "last_consent_status";

    /**
     * The reason why the consent state changed, iff the current state has changed.
     */
    private static final String CONSENT_CHANGE_REASON_KEY = "consent_change_reason";

    /**
     * IAB's vendor list.
     */
    private static final String CACHED_VENDOR_LIST_IAB_HASH_KEY = "cached_vendor_list_iab_hash";

    /**
     * Any other server data that the server wants for the SDK to hang on to.
     */
    private static final String EXTRAS_KEY = "extras";

    @NonNull private final Context mContext;
    @Nullable private String mAdUnitId;
    @Nullable private String mUdid;
    @Nullable private String mLastChangedMs;
    @Nullable private String mLastConsentStatus;
    @NonNull private final String mCurrentConsentStatus;
    @Nullable private String mConsentChangeReason;
    @Nullable private String mConsentedVendorListVersion;
    @Nullable private String mConsentedPrivacyPolicyVersion;
    @Nullable private String mCachedVendorListIabHash;
    @Nullable private String mExtras;
    @Nullable private Boolean mGdprApplies;

    public SyncUrlGenerator(@NonNull final Context context,
            @NonNull final String currentConsentStatus) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(currentConsentStatus);

        mContext = context.getApplicationContext();
        mCurrentConsentStatus = currentConsentStatus;
    }

    public SyncUrlGenerator withAdUnitId(@Nullable final String adUnitId) {
        mAdUnitId = adUnitId;
        return this;
    }

    public SyncUrlGenerator withUdid(@Nullable final String udid) {
        mUdid = udid;
        return this;
    }

    public SyncUrlGenerator withGdprApplies(@Nullable final Boolean gdprApplies) {
        mGdprApplies = gdprApplies;
        return this;
    }

    public SyncUrlGenerator withLastChangedMs(@Nullable final String lastChangedMs) {
        mLastChangedMs = lastChangedMs;
        return this;
    }

    public SyncUrlGenerator withLastConsentStatus(@Nullable final ConsentStatus lastConsentStatus) {
        mLastConsentStatus = lastConsentStatus == null ? null : lastConsentStatus.getValue();
        return this;
    }

    public SyncUrlGenerator withConsentChangeReason(@Nullable final String consentChangeReason) {
        mConsentChangeReason = consentChangeReason;
        return this;
    }

    public SyncUrlGenerator withConsentedVendorListVersion(
            @Nullable final String consentedVendorListVersion) {
        mConsentedVendorListVersion = consentedVendorListVersion;
        return this;
    }

    public SyncUrlGenerator withConsentedPrivacyPolicyVersion(
            @Nullable final String consentedPrivacyPolicyVersion) {
        mConsentedPrivacyPolicyVersion = consentedPrivacyPolicyVersion;
        return this;
    }

    public SyncUrlGenerator withCachedVendorListIabHash(
            @Nullable final String cachedVendorListIabHash) {
        mCachedVendorListIabHash = cachedVendorListIabHash;
        return this;
    }

    public SyncUrlGenerator withExtras(@Nullable final String extras) {
        mExtras = extras;
        return this;
    }

    @Override
    public String generateUrlString(@NonNull final String serverHostname) {
        initUrlString(serverHostname, Constants.GDPR_SYNC_HANDLER);

        addParam(AD_UNIT_ID_KEY, mAdUnitId);
        addParam(SDK_VERSION_KEY, MoPub.SDK_VERSION);
        addParam(LAST_CHANGED_MS_KEY, mLastChangedMs);
        addParam(LAST_CONSENT_STATUS_KEY, mLastConsentStatus);
        addParam(CURRENT_CONSENT_STATUS_KEY, mCurrentConsentStatus);
        addParam(CONSENT_CHANGE_REASON_KEY, mConsentChangeReason);
        addParam(CONSENTED_VENDOR_LIST_VERSION_KEY, mConsentedVendorListVersion);
        addParam(CONSENTED_PRIVACY_POLICY_VERSION_KEY, mConsentedPrivacyPolicyVersion);
        addParam(CACHED_VENDOR_LIST_IAB_HASH_KEY, mCachedVendorListIabHash);
        addParam(EXTRAS_KEY, mExtras);
        addParam(UDID_KEY, mUdid);
        if (mGdprApplies != null) {
            addParam(GDPR_APPLIES, mGdprApplies ? "1" : "0");
        }
        addParam(BUNDLE_ID_KEY, ClientMetadata.getInstance(mContext).getAppPackageName());
        addParam(DNT_KEY, PlayServicesUrlRewriter.DO_NOT_TRACK_TEMPLATE);

        return getFinalUrlString();
    }
}
