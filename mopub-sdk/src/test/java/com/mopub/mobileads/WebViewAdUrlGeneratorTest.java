package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.mopub.common.ClientMetadata;
import com.mopub.common.GpsHelper;
import com.mopub.common.GpsHelperTest;
import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection.MethodBuilder;
import com.mopub.common.util.Utils;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;
import com.mopub.mraid.MraidNativeCommandHandler;
import com.mopub.network.PlayServicesUrlRewriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowLocationManager;
import org.robolectric.shadows.ShadowNetworkInfo;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
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
import static com.mopub.common.ClientMetadata.MoPubNetworkType;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {MoPubShadowTelephonyManager.class})
public class WebViewAdUrlGeneratorTest {

    private static final String TEST_UDID = "20b013c721c";
    private static final int TEST_SCREEN_WIDTH = 42;
    private static final int TEST_SCREEN_HEIGHT = 1337;
    private static final float TEST_DENSITY = 1.0f;

    private WebViewAdUrlGenerator subject;
    private String expectedUdid;
    private Configuration configuration;
    private MoPubShadowTelephonyManager shadowTelephonyManager;
    private ShadowConnectivityManager shadowConnectivityManager;
    private Activity context;
    private MethodBuilder methodBuilder;

    @Before
    public void setup() {
        context = spy(Robolectric.buildActivity(Activity.class).create().get());
        Shadows.shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);
        Shadows.shadowOf(context).grantPermissions(ACCESS_FINE_LOCATION);
        Shadows.shadowOf(context).grantPermissions(ACCESS_COARSE_LOCATION);

        // Set the expected screen dimensions to arbitrary numbers
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = TEST_SCREEN_WIDTH;
        mockDisplayMetrics.heightPixels = TEST_SCREEN_HEIGHT;
        mockDisplayMetrics.density = TEST_DENSITY;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);
        when(context.getPackageName()).thenReturn("testBundle");

        // Only do this on Android 17+ because getRealSize doesn't exist before then.
        // This is the default pathway.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final WindowManager mockWindowManager = mock(WindowManager.class);
            final Display mockDisplay = mock(Display.class);
            doAnswer(new Answer() {
                @Override
                public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                    final Point point = (Point) invocationOnMock.getArguments()[0];
                    point.x = TEST_SCREEN_WIDTH;
                    point.y = TEST_SCREEN_HEIGHT;
                    return null;
                }
            }).when(mockDisplay).getRealSize(any(Point.class));
            when(mockWindowManager.getDefaultDisplay()).thenReturn(mockDisplay);
            final Context spyApplicationContext = spy(context.getApplicationContext());
            when(spyApplicationContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mockWindowManager);
            when(context.getApplicationContext()).thenReturn(spyApplicationContext);
        }

        subject = new WebViewAdUrlGenerator(context,
                new MraidNativeCommandHandler().isStorePictureSupported(context));
        Settings.Secure.putString(RuntimeEnvironment.application.getContentResolver(), Settings.Secure.ANDROID_ID, TEST_UDID);
        expectedUdid = "sha%3A" + Utils.sha1(TEST_UDID);
        configuration = RuntimeEnvironment.application.getResources().getConfiguration();
        shadowTelephonyManager = (MoPubShadowTelephonyManager) Shadows.shadowOf((TelephonyManager) RuntimeEnvironment.application.getSystemService(Context.TELEPHONY_SERVICE));
        shadowConnectivityManager = Shadows.shadowOf((ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE));
        methodBuilder = TestMethodBuilderFactory.getSingletonMock();

        LocationService.clearLastKnownLocation();
    }

    @After
    public void tearDown() throws Exception {
        reset(methodBuilder);
    }

    @Test
    public void generateAdUrl_shouldIncludeMinimumFields() throws Exception {
        String expectedAdUrl = new AdUrlBuilder(expectedUdid).build();

        String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_withHttpsScheme() throws Exception {
        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).startsWith("http://");
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
                .withLatLon("20.1%2C30.0", "1", "101325")
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
        location.setTime(System.currentTimeMillis() - 101325);

        String adUrl = subject
                .withAdUnitId("adUnitId")
                .withKeywords("key:value")
                .withLocation(location)
                .generateUrlString("ads.mopub.com");

        // Only compare the seconds since millis can be off
        adUrl = adUrl.replaceFirst("llf=101[0-9]{3}", "llf=101325");

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

        Shadows.shadowOf(context).denyPermissions(ACCESS_NETWORK_STATE);
        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE));

        String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.UNKNOWN).build());
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
        final ClientMetadata clientMetadata = ClientMetadata.getInstance(context);
        clientMetadata.setAdvertisingInfo(adInfo.mAdId, adInfo.mLimitAdTrackingEnabled);

        expectedUdid = "ifa%3A" + adInfo.ADVERTISING_ID;
        String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withDnt(adInfo.LIMIT_AD_TRACKING_ENABLED)
                .build();
        assertThat(generateMinimumUrlString()).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_whenLocationServiceGpsProviderHasMostRecentLocation_shouldUseLocationServiceValue() {
        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("37.0,-122.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("5");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("1");
    }

    @Test
    public void generateAdUrl_whenLocationServiceGpsProviderHasMostRecentLocation_WithFineLocationPermissionOnly_shouldUseLocationServiceValue() {
        Shadows.shadowOf(context).denyPermissions(ACCESS_COARSE_LOCATION);

        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("37.0,-122.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("5");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("1");
    }

    @Test
    public void generateAdUrl_whenLocationServiceGpsProviderHasMostRecentLocation_WithCoarseLocationPermissionOnly_shouldUseDeveloperSuppliedLocation() {
        Shadows.shadowOf(context).denyPermissions(ACCESS_FINE_LOCATION);

        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("42.0,-42.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("3");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEmpty();
    }

    @Test
    public void generateAdUrl_whenLocationServiceGpsProviderHasMostRecentLocation_WithNoLocationPermission_shouldUseDeveloperSuppliedLocation() {
        Shadows.shadowOf(context).denyPermissions(ACCESS_FINE_LOCATION);
        Shadows.shadowOf(context).denyPermissions(ACCESS_COARSE_LOCATION);

        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("42.0,-42.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("3");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEmpty();
    }

    @Test
    public void generateAdUrl_whenDeveloperSuppliesMoreRecentLocationThanLocationService_shouldUseDeveloperSuppliedLocation() {
        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));

        // Mock out the LocationManager's last known location to be older than the
        // developer-supplied location.
        Location olderLocation = new Location("");
        olderLocation.setLatitude(40);
        olderLocation.setLongitude(-105);
        olderLocation.setAccuracy(8.0f);
        olderLocation.setTime(500);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, olderLocation);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("42.0,-42.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("3");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEmpty();
    }

    @Test
    public void generateAdUrl_whenLocationServiceNetworkProviderHasMostRecentLocation_shouldUseLocationServiceValue() {
        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(38);
        locationFromSdk.setLongitude(-123);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.NETWORK_PROVIDER,
                locationFromSdk);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("38.0,-123.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("5");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("1");
    }

    @Test
    public void generateAdUrl_whenLocationServiceNetworkProviderHasMostRecentLocation_WithFineLocationPermissionOnly_shouldUseLocationServiceValue() {
        Shadows.shadowOf(context).denyPermissions(ACCESS_COARSE_LOCATION);

        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(38);
        locationFromSdk.setLongitude(-123);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.NETWORK_PROVIDER,
                locationFromSdk);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("38.0,-123.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("5");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("1");
    }

    @Test
    public void generateAdUrl_whenLocationServiceNetworkProviderHasMostRecentLocation_WithCoarseLocationPermissionOnly_shouldUseLocationServiceValue() {
        Shadows.shadowOf(context).denyPermissions(ACCESS_FINE_LOCATION);

        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(38);
        locationFromSdk.setLongitude(-123);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.NETWORK_PROVIDER,
                locationFromSdk);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("38.0,-123.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("5");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("1");
    }

    @Test
    public void generateAdUrl_whenLocationServiceNetworkProviderHasMostRecentLocation_WithNoLocationPermission_shouldUseDeveloperSuppliedLocation() {
        Shadows.shadowOf(context).denyPermissions(ACCESS_FINE_LOCATION);
        Shadows.shadowOf(context).denyPermissions(ACCESS_COARSE_LOCATION);

        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(38);
        locationFromSdk.setLongitude(-123);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.NETWORK_PROVIDER,
                locationFromSdk);

        String adUrl = subject.withLocation(locationFromDeveloper)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("42.0,-42.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("3");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEmpty();
    }

    @Test
    public void generateAdUrl_withNullPackageName_withEmptyPackageName_shouldNotIncludeBundleKey() {
        when(context.getPackageName()).thenReturn(null).thenReturn("");

        final String adUrlNullPackageName = generateMinimumUrlString();
        final String adUrlEmptyPackageName = generateMinimumUrlString();

        assertThat(adUrlNullPackageName).doesNotContain("&bundle=");
        assertThat(adUrlEmptyPackageName).doesNotContain("&bundle=");
    }

    @Test
    public void enableLocationTracking_shouldIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.NORMAL);
        String adUrl = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isNotNull();
    }

    @Test
    public void disableLocationCollection_shouldNotIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.DISABLED);
        String adUrl = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isNullOrEmpty();
    }

    @Test
    public void disableLocationCollection_whenLocationServiceHasMostRecentLocation_shouldNotIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.DISABLED);

        // Mock out the LocationManager's last known location.
        ShadowLocationManager shadowLocationManager = Shadows.shadowOf(
                (LocationManager) RuntimeEnvironment.application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        String adUrl = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isNullOrEmpty();
    }

    private String getParameterFromRequestUrl(String requestString, String key) {
        Uri requestUri = Uri.parse(requestString);
        String parameter = requestUri.getQueryParameter(key);

        if (TextUtils.isEmpty(parameter)) {
            return "";
        }

        return parameter;
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
        private String latLonLastUpdated = "";
        private String mnc = "";
        private String mcc = "";
        private String countryIso = "";
        private String carrierName = "";
        private String dnt = "";
        private MoPubNetworkType networkType = MoPubNetworkType.MOBILE;
        private int externalStoragePermission;

        public AdUrlBuilder(String expectedUdid) {
            this.expectedUdid = expectedUdid;
        }

        public String build() {
            return "http://ads.mopub.com/m/ad" +
                    "?v=6" +
                    paramIfNotEmpty("id", adUnitId) +
                    "&nv=" + Uri.encode(MoPub.SDK_VERSION) +
                    "&dn=" + Build.MANUFACTURER +
                    "%2C" + Build.MODEL +
                    "%2C" + Build.PRODUCT +
                    "&bundle=" + "testBundle" +
                    paramIfNotEmpty("q", query) +
                    (TextUtils.isEmpty(latLon) ? "" :
                            "&ll=" + latLon + "&lla=" + locationAccuracy + "&llf=" + latLonLastUpdated) +
                    "&z=-0700" +
                    "&o=u" +
                    "&w=" + TEST_SCREEN_WIDTH +
                    "&h=" + TEST_SCREEN_HEIGHT +
                    "&sc_a=1.0" +
                    paramIfNotEmpty("mcc", mcc) +
                    paramIfNotEmpty("mnc", mnc) +
                    paramIfNotEmpty("iso", countryIso) +
                    paramIfNotEmpty("cn", carrierName) +
                    "&ct=" + networkType +
                    "&av=" + Uri.encode(BuildConfig.VERSION_NAME) +
                    "&udid=" + PlayServicesUrlRewriter.UDID_TEMPLATE +
                    "&dnt=" + PlayServicesUrlRewriter.DO_NOT_TRACK_TEMPLATE +
                    "&mr=1" +
                    "&android_perms_ext_storage=" + externalStoragePermission +
                    "&vv=3";
        }

        public AdUrlBuilder withAdUnitId(String adUnitId) {
            this.adUnitId = adUnitId;
            return this;
        }

        public AdUrlBuilder withQuery(String query) {
            this.query = query;
            return this;
        }

        public AdUrlBuilder withLatLon(String latLon, String locationAccuracy,
                String latLonLastUpdated) {
            this.latLon = latLon;
            this.locationAccuracy = locationAccuracy;
            this.latLonLastUpdated = latLonLastUpdated;
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

        public AdUrlBuilder withDnt(boolean dnt) {
            if (dnt) {
                this.dnt = "1";
            }
            return this;
        }

        private String paramIfNotEmpty(String key, String value) {
            if (TextUtils.isEmpty(value)) {
                return "";
            } else {
                return "&" + key + "=" + value;
            }
        }
    }
}
