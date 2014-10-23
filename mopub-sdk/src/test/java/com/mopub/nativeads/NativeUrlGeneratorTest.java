package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.mopub.common.MoPub;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(shadows = {MoPubShadowTelephonyManager.class})
public class NativeUrlGeneratorTest {
    public static final String AD_UNIT_ID = "1234";
    private Activity context;
    private NativeUrlGenerator subject;
    private MoPubShadowTelephonyManager shadowTelephonyManager;

    @Before
    public void setup() {
        context = new Activity();
        shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);
        shadowTelephonyManager = (MoPubShadowTelephonyManager)
                shadowOf((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
    }

    @Test
    public void generateNativeAdUrl_shouldIncludeDesiredAssetIfSet() throws Exception {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.of(RequestParameters.NativeAdAsset.TITLE);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(1);
        assertThat(desiredAssets).contains("title");
    }

    @Test
    public void generateNativeAdUrl_shouldIncludeDesiredAssetsIfSet() throws Exception {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.of(RequestParameters.NativeAdAsset.TITLE, RequestParameters.NativeAdAsset.TEXT, RequestParameters.NativeAdAsset.ICON_IMAGE);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(3);
        assertThat(desiredAssets).contains("title", "text", "iconimage");
    }

    @Test
    public void generateNativeAdUrl_shouldNotIncludeDesiredAssetsIfNotSet() throws Exception {
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(0);
    }

    @Test
    public void generateNativeAdUrl_shouldNotIncludeDesiredAssetsIfNoAssetsAreSet() throws Exception {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.noneOf(RequestParameters.NativeAdAsset.class);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(0);
    }

    @Test
    public void generateNativeAdUrl_needsButDoesNotHaveReadPhoneState_shouldNotContainOperatorName() {
        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(false);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isNullOrEmpty();
    }
    
    @Test
    public void generateNativeAdUrl_needsAndHasReadPhoneState_shouldContainOperatorName() {
        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(true);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isEqualTo("TEST_CARRIER");
    }

    @Test
    public void generateNativeAdUrl_doesNotNeedReadPhoneState_shouldContainOperatorName() {
        shadowTelephonyManager.setNeedsReadPhoneState(false);
        shadowTelephonyManager.setReadPhoneStatePermission(false);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isEqualTo("TEST_CARRIER");
    }

    @Test
    public void enableLocation_shouldIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.NORMAL);
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        String requestString = generateMinimumUrlString();
        assertThat(getLocationFromRequestUrl(requestString)).isNotNull();
    }

    @Test
    public void disableLocation_shouldNotIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.DISABLED);
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        String requestString = generateMinimumUrlString();
        assertThat(getLocationFromRequestUrl(requestString)).isNullOrEmpty();
    }

    private List<String> getDesiredAssetsListFromRequestUrlString(String requestString) {
        Uri requestUri = Uri.parse(requestString);

        String desiredAssetsString = requestUri.getQueryParameter("assets");
        return (desiredAssetsString == null) ? new ArrayList<String>() : Arrays.asList(desiredAssetsString.split(","));
    }

    private String getNetworkOperatorNameFromRequestUrl(String requestString) {
        Uri requestUri = Uri.parse(requestString);

        String networkOperatorName = requestUri.getQueryParameter("cn");

        if (TextUtils.isEmpty(networkOperatorName)) {
            return "";
        }

        return networkOperatorName;
    }

    private String getLocationFromRequestUrl(String requestString) {
        Uri requestUri = Uri.parse(requestString);
        String location = requestUri.getQueryParameter("ll");

        if (TextUtils.isEmpty(location)) {
            return "";
        }

        return location;
    }

    private String generateMinimumUrlString() {
        return subject.generateUrlString("ads.mopub.com");
    }
}
