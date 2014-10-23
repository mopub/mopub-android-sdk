package com.mopub.nativeads;

import android.content.Context;

import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;

class PositioningUrlGenerator extends BaseUrlGenerator {
    private static final String POSITIONING_API_VERSION = "1";

    private final Context mContext;
    private String mAdUnitId;

    public PositioningUrlGenerator(Context context) {
        mContext = context;
    }

    public PositioningUrlGenerator withAdUnitId(final String adUnitId) {
        mAdUnitId = adUnitId;
        return this;
    }

    @Override
    public String generateUrlString(final String serverHostname) {
        initUrlString(serverHostname, Constants.POSITIONING_HANDLER);

        setAdUnitId(mAdUnitId);

        setApiVersion(POSITIONING_API_VERSION);

        ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);

        setSdkVersion(clientMetadata.getSdkVersion());

        setDeviceInfo(clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct());

        setUdid(clientMetadata.getUdid());

        setAppVersion(clientMetadata.getAppVersion());

        return getFinalUrlString();
    }

    private void setAdUnitId(String adUnitId) {
        addParam("id", adUnitId);
    }

    private void setSdkVersion(String sdkVersion) {
        addParam("nsv", sdkVersion);
    }
}
