package com.mopub.mobileads;

import android.content.Context;
import android.location.Location;

import com.mopub.common.AdUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.util.DateAndTime;

import static com.mopub.mobileads.util.Mraids.isStorePictureSupported;

public class WebViewAdUrlGenerator extends AdUrlGenerator {
    public WebViewAdUrlGenerator(Context context) {
        super(context);
    }

    @Override
    public String generateUrlString(String serverHostname) {
        initUrlString(serverHostname, MoPubView.AD_HANDLER);

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);

        setApiVersion("6");

        setAdUnitId(mAdUnitId);

        setSdkVersion(clientMetadata.getSdkVersion());

        setDeviceInfo(clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct());

        setUdid(clientMetadata.getUdid());

        setDoNotTrack(clientMetadata.getDoNoTrack());

        setKeywords(mKeywords);

        Location location = mLocation;
        if (location == null) {
            location = LocationService.getLastKnownLocation(mContext,
                    MoPub.getLocationPrecision(),
                    MoPub.getLocationAwareness());
        }
        setLocation(location);

        setTimezone(DateAndTime.getTimeZoneOffsetString());

        setOrientation(clientMetadata.getOrientationString());

        setDensity(clientMetadata.getDensity());

        setMraidFlag(detectIsMraidSupported());

        String networkOperator = clientMetadata.getNetworkOperator();
        setMccCode(networkOperator);
        setMncCode(networkOperator);

        setIsoCountryCode(clientMetadata.getIsoCountryCode());
        setCarrierName(clientMetadata.getNetworkOperatorName());

        setNetworkType(clientMetadata.getActiveNetworkType());

        setAppVersion(clientMetadata.getAppVersion());

        setExternalStoragePermission(isStorePictureSupported(mContext));

        setTwitterAppInstalledFlag();

        return getFinalUrlString();
    }

    private boolean detectIsMraidSupported() {
        boolean mraid = true;
        try {
            Class.forName("com.mopub.mobileads.MraidView");
        } catch (ClassNotFoundException e) {
            mraid = false;
        }
        return mraid;
    }
}
