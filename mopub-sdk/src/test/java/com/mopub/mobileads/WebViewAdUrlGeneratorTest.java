package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.mopub.common.AdUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.GpsHelper;
import com.mopub.common.GpsHelperTest;
import com.mopub.common.MoPub;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection.MethodBuilder;
import com.mopub.common.util.Utils;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetworkInfo;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.net.ConnectivityManager.TYPE_DUMMY;
import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_MOBILE_DUN;
import static android.net.ConnectivityManager.TYPE_MOBILE_HIPRI;
import static android.net.ConnectivityManager.TYPE_MOBILE_MMS;
import static android.net.ConnectivityManager.TYPE_MOBILE_SUPL;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
import static com.mopub.common.AdUrlGenerator.TwitterAppInstalledStatus;
import static com.mopub.common.ClientMetadata.MoPubNetworkType;
import static com.mopub.common.util.Strings.isEmpty;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.application;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(shadows = {MoPubShadowTelephonyManager.class})
public class WebViewAdUrlGeneratorTest {

    private WebViewAdUrlGenerator subject;
    private static final String TEST_UDID = "20b013c721c";
    private String expectedUdid;
    private Configuration configuration;
    private MoPubShadowTelephonyManager shadowTelephonyManager;
    private ShadowConnectivityManager shadowConnectivityManager;
    private Activity context;
    private MethodBuilder methodBuilder;

    @Before
    public void setup() {
        context = new Activity();
        shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);
        subject = new WebViewAdUrlGenerator(context);
        Settings.Secure.putString(application.getContentResolver(), Settings.Secure.ANDROID_ID, TEST_UDID);
        expectedUdid = "sha%3A" + Utils.sha1(TEST_UDID);
        configuration = application.getResources().getConfiguration();
        shadowTelephonyManager = (MoPubShadowTelephonyManager) shadowOf((TelephonyManager) application.getSystemService(Context.TELEPHONY_SERVICE));
        shadowConnectivityManager = shadowOf((ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE));
        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
    }

    @After
    public void tearDown() throws Exception {
        AdUrlGenerator.setTwitterAppInstalledStatus(TwitterAppInstalledStatus.UNKNOWN);
        reset(methodBuilder);
    }

    @Test
    public void generateAdUrl_shouldIncludeMinimumFields() throws Exception {
        String expectedAdUrl = new AdUrlBuilder(expectedUdid).build();

        String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_shouldRunMultipleTimes() throws Exception {
        String expectedAdUrl = new AdUrlBuilder(expectedUdid).build();

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(expectedAdUrl);
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_shouldIncludeAllFields() throws Exception {
        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withAdUnitId("adUnitId")
                .withQuery("key%3Avalue")
                .withLatLon("20.1%2C30.0", "1")
                .withMcc("123")
                .withMnc("456")
                .withCountryIso("expected%20country")
                .withCarrierName("expected%20carrier")
                .withExternalStoragePermission(false)
                .build();

        shadowTelephonyManager.setNetworkOperator("123456");
        shadowTelephonyManager.setNetworkCountryIso("expected country");
        shadowTelephonyManager.setNetworkOperatorName("expected carrier");

        Location location = new Location("");
        location.setLatitude(20.1);
        location.setLongitude(30.0);
        location.setAccuracy(1.23f); // should get rounded to "1"

        String adUrl = subject
                .withAdUnitId("adUnitId")
                .withKeywords("key:value")
                .withLocation(location)
                .generateUrlString("ads.mopub.com");

        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_shouldRecognizeOrientation() throws Exception {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
        assertThat(generateMinimumUrlString()).contains("&o=l");
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
        assertThat(generateMinimumUrlString()).contains("&o=p");
        configuration.orientation = Configuration.ORIENTATION_SQUARE;
        assertThat(generateMinimumUrlString()).contains("&o=s");
    }

    @Test
    public void generateAdUrl_shouldHandleFunkyNetworkOperatorCodes() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        shadowTelephonyManager.setNetworkOperator("123456");
        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("123").withMnc("456").build());

        ClientMetadata.clearForTesting();
        shadowTelephonyManager.setNetworkOperator("12345");
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("123").withMnc("45").build());

        ClientMetadata.clearForTesting();
        shadowTelephonyManager.setNetworkOperator("1234");
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("123").withMnc("4").build());

        ClientMetadata.clearForTesting();
        shadowTelephonyManager.setNetworkOperator("123");
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("123").withMnc("").build());

        ClientMetadata.clearForTesting();
        shadowTelephonyManager.setNetworkOperator("12");
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("12").withMnc("").build());
    }

    @Test
    public void generateAdUrl_needsAndDoesNotHaveReadPhoneState_shouldNotContainOperatorName() {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(false);

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withCarrierName("").build());
    }

    @Test
    public void generateAdUrl_needsAndHasReadPhoneState_shouldContainOperatorName() {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(true);
        shadowTelephonyManager.setNetworkOperatorName("TEST_NAME");

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withCarrierName("TEST_NAME").build());
    }

    @Test
    public void generateAdUrl_doesNotNeedReadPhoneState_shouldContainOperatorName() {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        shadowTelephonyManager.setNeedsReadPhoneState(false);
        shadowTelephonyManager.setReadPhoneStatePermission(false);
        shadowTelephonyManager.setNetworkOperatorName("TEST_NAME");

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withCarrierName("TEST_NAME").build());
    }

    @Test
    public void generateAdurl_whenOnCDMA_shouldGetOwnerStringFromSimCard() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);
        shadowTelephonyManager.setPhoneType(TelephonyManager.PHONE_TYPE_CDMA);
        shadowTelephonyManager.setSimState(TelephonyManager.SIM_STATE_READY);
        shadowTelephonyManager.setNetworkOperator("123456");
        shadowTelephonyManager.setSimOperator("789012");
        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("789").withMnc("012").build());
    }

    @Test
    public void generateAdurl_whenSimNotReady_shouldDefaultToNetworkOperator() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);
        shadowTelephonyManager.setPhoneType(TelephonyManager.PHONE_TYPE_CDMA);
        shadowTelephonyManager.setSimState(TelephonyManager.SIM_STATE_ABSENT);
        shadowTelephonyManager.setNetworkOperator("123456");
        shadowTelephonyManager.setSimOperator("789012");
        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("123").withMnc("456").build());
    }

    @Test
    public void generateAdUrl_shouldSetNetworkType() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);
        String adUrl;

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_DUMMY));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.UNKNOWN).build());

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_ETHERNET));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.ETHERNET).build());

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_WIFI));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.WIFI).build());

        // bunch of random mobile types just to make life more interesting
        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.MOBILE).build());

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE_DUN));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.MOBILE).build());

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE_HIPRI));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.MOBILE).build());

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE_MMS));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.MOBILE).build());

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE_SUPL));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.MOBILE).build());
    }

    @Test
    public void generateAdUrl_whenNoNetworkPermission_shouldGenerateUnknownNetworkType() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        shadowOf(context).denyPermissions(ACCESS_NETWORK_STATE);
        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE));

        String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.UNKNOWN).build());
    }

    @Test
    public void generateAdUrl_whenTwitterIsNotInstalled_shouldProcessAndNotSetTwitterInstallStatusOnFirstRequest() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        WebViewAdUrlGenerator spySubject = Mockito.spy(subject);
        AdUrlGenerator.setTwitterAppInstalledStatus(TwitterAppInstalledStatus.UNKNOWN);
        doReturn(TwitterAppInstalledStatus.NOT_INSTALLED).when(spySubject).getTwitterAppInstallStatus();

        String adUrl = spySubject.generateUrlString("ads.mopub.com");

        assertThat(adUrl).isEqualTo(urlBuilder.withTwitterAppInstalledStatus(TwitterAppInstalledStatus.NOT_INSTALLED).build());
    }

    @Test
    public void generateAdUrl_whenTwitterIsInstalled_shouldProcessAndSetTwitterInstallStatusOnFirstRequest() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        WebViewAdUrlGenerator spySubject = Mockito.spy(subject);
        AdUrlGenerator.setTwitterAppInstalledStatus(TwitterAppInstalledStatus.UNKNOWN);
        doReturn(TwitterAppInstalledStatus.INSTALLED).when(spySubject).getTwitterAppInstallStatus();

        String adUrl = spySubject.generateUrlString("ads.mopub.com");

        assertThat(adUrl).isEqualTo(urlBuilder.withTwitterAppInstalledStatus(TwitterAppInstalledStatus.INSTALLED).build());
    }

    @Test
    public void generateAdUrl_shouldNotProcessTwitterInstallStatusIfStatusIsAlreadySet() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        WebViewAdUrlGenerator spySubject = Mockito.spy(subject);
        AdUrlGenerator.setTwitterAppInstalledStatus(TwitterAppInstalledStatus.NOT_INSTALLED);
        doReturn(TwitterAppInstalledStatus.INSTALLED).when(spySubject).getTwitterAppInstallStatus();

        String adUrl = spySubject.generateUrlString("ads.mopub.com");

        assertThat(adUrl).isEqualTo(urlBuilder.withTwitterAppInstalledStatus(TwitterAppInstalledStatus.NOT_INSTALLED).build());
    }

    @Test
    public void generateAdUrl_shouldTolerateNullActiveNetwork() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);
        shadowConnectivityManager.setActiveNetworkInfo(null);

        String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.UNKNOWN).build());
    }

    @Test
    public void generateAdUrl_whenGooglePlayServicesIsLinkedAndAdInfoIsCached_shouldUseAdInfoParams() throws Exception {
        GpsHelper.setClassNamesForTesting();
        when(methodBuilder.setStatic(any(Class.class))).thenReturn(methodBuilder);
        when(methodBuilder.addParam(any(Class.class), any())).thenReturn(methodBuilder);
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE);

        GpsHelperTest.TestAdInfo adInfo = new GpsHelperTest.TestAdInfo();
        SharedPreferencesHelper.getSharedPreferences(context)
                .edit()
                .putString(GpsHelper.ADVERTISING_ID_KEY, adInfo.ADVERTISING_ID)
                .putBoolean(GpsHelper.IS_LIMIT_AD_TRACKING_ENABLED_KEY, adInfo.LIMIT_AD_TRACKING_ENABLED)
                .commit();

        expectedUdid = "ifa%3A" + adInfo.ADVERTISING_ID;
        String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withDnt(adInfo.LIMIT_AD_TRACKING_ENABLED)
                .build();
        assertThat(generateMinimumUrlString()).isEqualTo(expectedAdUrl);
    }

    @Test
    public void enableLocationTracking_shouldIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.NORMAL);
        String adUrl = generateMinimumUrlString();
        assertThat(getLocationFromRequestUrl(adUrl)).isNotNull();
    }

    @Test
    public void disableLocationCollection_shouldNotIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.DISABLED);
        String adUrl = generateMinimumUrlString();
        assertThat(getLocationFromRequestUrl(adUrl)).isNullOrEmpty();
    }

    private String getLocationFromRequestUrl(String requestString) {
        Uri requestUri = Uri.parse(requestString);
        String location = requestUri.getQueryParameter("ll");

        if (TextUtils.isEmpty(location)) {
            return "";
        }

        return location;
    }

    private NetworkInfo createNetworkInfo(int type) {
        return ShadowNetworkInfo.newInstance(null,
                type,
                NETWORK_TYPE_UNKNOWN, true, true);
    }

    private String generateMinimumUrlString() {
        return subject.generateUrlString("ads.mopub.com");
    }

    private static class AdUrlBuilder {
        private String expectedUdid;
        private String adUnitId = "";
        private String query = "";
        private String latLon = "";
        private String locationAccuracy = "";
        private String mnc = "";
        private String mcc = "";
        private String countryIso = "";
        private String carrierName = "";
        private String dnt = "";
        private MoPubNetworkType networkType = MoPubNetworkType.MOBILE;
        private TwitterAppInstalledStatus twitterAppInstalledStatus = TwitterAppInstalledStatus.UNKNOWN;
        private int externalStoragePermission;

        public AdUrlBuilder(String expectedUdid) {
            this.expectedUdid = expectedUdid;
        }

        public String build() {
            return "http://ads.mopub.com/m/ad" +
                    "?v=6" +
                    paramIfNotEmpty("id", adUnitId) +
                    "&nv=" + MoPub.SDK_VERSION +
                    "&dn=" + Build.MANUFACTURER +
                    "%2C" + Build.MODEL +
                    "%2C" + Build.PRODUCT +
                    "&udid=" + expectedUdid +
                    paramIfNotEmpty("dnt", dnt) +
                    paramIfNotEmpty("q", query) +
                    (isEmpty(latLon) ? "" : "&ll=" + latLon + "&lla=" + locationAccuracy) +
                    "&z=-0700" +
                    "&o=u" +
                    "&sc_a=1.0" +
                    "&mr=1" +
                    paramIfNotEmpty("mcc", mcc) +
                    paramIfNotEmpty("mnc", mnc) +
                    paramIfNotEmpty("iso", countryIso) +
                    paramIfNotEmpty("cn", carrierName) +
                    "&ct=" + networkType +
                    "&av=1.0" +
                    "&android_perms_ext_storage=" + externalStoragePermission +
                    ((twitterAppInstalledStatus == TwitterAppInstalledStatus.INSTALLED) ? "&ts=1" : "");

        }

        public AdUrlBuilder withAdUnitId(String adUnitId) {
            this.adUnitId = adUnitId;
            return this;
        }

        public AdUrlBuilder withQuery(String query) {
            this.query = query;
            return this;
        }

        public AdUrlBuilder withLatLon(String latLon, String locationAccuracy) {
            this.latLon = latLon;
            this.locationAccuracy = locationAccuracy;
            return this;
        }

        public AdUrlBuilder withMcc(String mcc) {
            this.mcc = mcc;
            return this;
        }

        public AdUrlBuilder withMnc(String mnc) {
            this.mnc = mnc;
            return this;
        }

        public AdUrlBuilder withCountryIso(String countryIso) {
            this.countryIso = countryIso;
            return this;
        }

        public AdUrlBuilder withCarrierName(String carrierName) {
            this.carrierName = carrierName;
            return this;
        }

        public AdUrlBuilder withNetworkType(MoPubNetworkType networkType) {
            this.networkType = networkType;
            return this;
        }

        public AdUrlBuilder withExternalStoragePermission(boolean enabled) {
            this.externalStoragePermission = enabled ? 1 : 0;
            return this;
        }

        public AdUrlBuilder withTwitterAppInstalledStatus(TwitterAppInstalledStatus status) {
            this.twitterAppInstalledStatus = status;
            return this;
        }

        public AdUrlBuilder withDnt(boolean dnt) {
            if (dnt) {
                this.dnt = "1";
            }
            return this;
        }

        private String paramIfNotEmpty(String key, String value) {
            if (isEmpty(value)) {
                return "";
            } else {
                return "&" + key + "=" + value;
            }
        }
    }
}
