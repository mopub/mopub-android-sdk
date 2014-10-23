package com.mopub.nativeads;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import com.mopub.common.AdUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.util.DateAndTime;
import com.mopub.common.util.Strings;

class NativeUrlGenerator extends AdUrlGenerator {
    private String mDesiredAssets;
    private String mSequenceNumber;

    NativeUrlGenerator(Context context) {
        super(context);
    }

    @Override
    public NativeUrlGenerator withAdUnitId(final String adUnitId) {
        mAdUnitId = adUnitId;
        return this;
    }

    NativeUrlGenerator withRequest(final RequestParameters requestParameters) {
        if (requestParameters != null) {
            mKeywords = requestParameters.getKeywords();
            mLocation = requestParameters.getLocation();
            mDesiredAssets = requestParameters.getDesiredAssets();
        }
        return this;
    }

    NativeUrlGenerator withSequenceNumber(final int sequenceNumber) {
        mSequenceNumber = String.valueOf(sequenceNumber);
        return this;
    }

    @Override
    public String generateUrlString(final String serverHostname) {
        initUrlString(serverHostname, Constants.NATIVE_HANDLER);

        setAdUnitId(mAdUnitId);

        setKeywords(mKeywords);

        Location location = mLocation;
        if (location == null) {
            location = LocationService.getLastKnownLocation(mContext,
                    MoPub.getLocationPrecision(),
                    MoPub.getLocationAwareness());
        }
        setLocation(location);

        ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);
        setSdkVersion(clientMetadata.getSdkVersion());

        setDeviceInfo(clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct());

        setUdid(clientMetadata.getUdid());

        setDoNotTrack(clientMetadata.getDoNoTrack());

        setTimezone(DateAndTime.getTimeZoneOffsetString());

        setOrientation(clientMetadata.getOrientationString());

        setDensity(clientMetadata.getDensity());

        String networkOperator = clientMetadata.getNetworkOperator();
        setMccCode(networkOperator);
        setMncCode(networkOperator);

        setIsoCountryCode(clientMetadata.getIsoCountryCode());
        setCarrierName(clientMetadata.getNetworkOperatorName());

        setNetworkType(clientMetadata.getActiveNetworkType());

        setAppVersion(clientMetadata.getAppVersion());

        setTwitterAppInstalledFlag();

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
        if (mDesiredAssets != null && !Strings.isEmpty(mDesiredAssets)) {
            addParam("assets", mDesiredAssets);
        }
    }

    @Override
    protected void setSdkVersion(String sdkVersion) {
        addParam("nsv", sdkVersion);
    }
}
