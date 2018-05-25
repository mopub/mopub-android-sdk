package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.AdUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;

class NativeUrlGenerator extends AdUrlGenerator {
    @Nullable private String mDesiredAssets;
    @Nullable private String mSequenceNumber;

    NativeUrlGenerator(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public NativeUrlGenerator withAdUnitId(final String adUnitId) {
        mAdUnitId = adUnitId;
        return this;
    }

    @NonNull
    NativeUrlGenerator withRequest(@Nullable final RequestParameters requestParameters) {
        if (requestParameters != null) {
            final boolean canCollectPersonalInformation = MoPub.canCollectPersonalInformation();

            mUserDataKeywords = canCollectPersonalInformation ? requestParameters.getUserDataKeywords() : null;
            mLocation = canCollectPersonalInformation ? requestParameters.getLocation() : null;

            mKeywords = requestParameters.getKeywords();
            mDesiredAssets = requestParameters.getDesiredAssets();
        }
        return this;
    }

    @NonNull
    NativeUrlGenerator withSequenceNumber(final int sequenceNumber) {
        mSequenceNumber = String.valueOf(sequenceNumber);
        return this;
    }

    @Override
    public String generateUrlString(final String serverHostname) {
        initUrlString(serverHostname, Constants.AD_HANDLER);

        ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);
        addBaseParams(clientMetadata);

        setDesiredAssets();

        setSequenceNumber();

        return getFinalUrlString();
    }

    private void setSequenceNumber() {
       if (!TextUtils.isEmpty(mSequenceNumber)) {
           addParam("MAGIC_NO", mSequenceNumber);
       }
    }

    private void setDesiredAssets() {
        if (!TextUtils.isEmpty(mDesiredAssets)) {
            addParam("assets", mDesiredAssets);
        }
    }
}
