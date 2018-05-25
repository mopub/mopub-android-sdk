package com.mopub.common.privacy;

import android.content.Context;
import android.support.annotation.NonNull;

import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;

import static com.mopub.common.Constants.GDPR_CONSENT_HANDLER;

public class ConsentDialogUrlGenerator extends BaseUrlGenerator {
    /**
     * Current device default language.
     */
    private static final String LANGUAGE_KEY = "language";

    @NonNull
    private final Context mContext;
    @NonNull
    private final String mAdUnitId;

    ConsentDialogUrlGenerator(@NonNull final Context context,
                              @NonNull final String adUnitId) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adUnitId);

        mContext = context.getApplicationContext();
        mAdUnitId = adUnitId;
    }

    @Override
    public String generateUrlString(String serverHostname) {
        initUrlString(serverHostname, GDPR_CONSENT_HANDLER);

        addParam(AD_UNIT_ID_KEY, mAdUnitId);
        addParam(SDK_VERSION_KEY, MoPub.SDK_VERSION);
        addParam(LANGUAGE_KEY, ClientMetadata.getCurrentLanguage(mContext));
        return getFinalUrlString();
    }
}
