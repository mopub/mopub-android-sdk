// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.AppEngineInfo;
import com.mopub.common.BaseAdapterConfiguration;
import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.LocationService;
import com.mopub.common.LocationServiceTest;
import com.mopub.common.MoPub;
import com.mopub.common.OnNetworkInitializationFinishedListener;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.privacy.AdvertisingId;
import com.mopub.common.privacy.ConsentData;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.MoPubIdentifier;
import com.mopub.common.privacy.MoPubIdentifierTest;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.Reflection.MethodBuilder;
import com.mopub.common.util.Utils;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.test.support.MoPubShadowConnectivityManager;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;
import com.mopub.network.PlayServicesUrlRewriter;
import com.mopub.network.RequestRateTrackerTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.android.util.concurrent.RoboExecutorService;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowNetworkInfo;

import java.util.Map;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.net.ConnectivityManager.TYPE_DUMMY;
import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.telephony.TelephonyManager.NETWORK_TYPE_UNKNOWN;
import static com.mopub.common.ClientMetadata.MoPubNetworkType;
import static com.mopub.common.MoPubTest.INIT_ADUNIT;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(shadows = {MoPubShadowTelephonyManager.class, MoPubShadowConnectivityManager.class})
public class WebViewAdUrlGeneratorTest {

    private static final String TEST_UDID = "20b013c721c";
    private static final int TEST_SCREEN_WIDTH = 42;
    private static final int TEST_SCREEN_HEIGHT = 1337;
    private static final int TEST_SCREEN_SAFE_WIDTH = 0;
    private static final int TEST_SCREEN_SAFE_HEIGHT = 0;
    private static final float TEST_DENSITY = 1.0f;

    private WebViewAdUrlGenerator subject;
    private String expectedUdid;
    private Configuration configuration;
    private MoPubShadowTelephonyManager shadowTelephonyManager;
    private MoPubShadowConnectivityManager shadowConnectivityManager;
    private Activity context;
    private Context spyApplicationContext;
    private MethodBuilder methodBuilder;
    private PersonalInfoManager mockPersonalInfoManager;
    private ConsentData mockConsentData;

    @Before
    public void setup() throws Exception {
        context = spy(Robolectric.buildActivity(Activity.class).create().get());
        Shadows.shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);
        Shadows.shadowOf(context).grantPermissions(ACCESS_FINE_LOCATION);
        Shadows.shadowOf(context).grantPermissions(ACCESS_COARSE_LOCATION);
        Shadows.shadowOf(context).grantPermissions(READ_PHONE_STATE);

        AsyncTasks.setExecutor(new RoboExecutorService());

        // Set the expected screen dimensions to arbitrary numbers
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = TEST_SCREEN_WIDTH;
        mockDisplayMetrics.heightPixels = TEST_SCREEN_HEIGHT;
        mockDisplayMetrics.density = TEST_DENSITY;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);
        when(context.getPackageName()).thenReturn("testBundle");


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
        spyApplicationContext = spy(context.getApplicationContext());
        when(spyApplicationContext.getResources()).thenReturn(spyResources);
        when(spyApplicationContext.getPackageName()).thenReturn("testBundle");
        PackageManager mockPackageManager = mock(PackageManager.class);
        PackageInfo mockPackageInfo = mock(PackageInfo.class);
        mockPackageInfo.versionName = BuildConfig.VERSION_NAME;
        when(mockPackageManager.getPackageInfo(any(String.class), anyInt())).thenReturn(mockPackageInfo);
        when(spyApplicationContext.getPackageManager()).thenReturn(mockPackageManager);
        when(spyApplicationContext.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mockWindowManager);
        when(context.getApplicationContext()).thenReturn(spyApplicationContext);
        when(spyApplicationContext.getApplicationContext()).thenReturn(spyApplicationContext);

        mockConsentData = mock(ConsentData.class);
        mockPersonalInfoManager = mock(PersonalInfoManager.class);
        when(mockPersonalInfoManager.getPersonalInfoConsentStatus()).thenReturn(ConsentStatus.UNKNOWN);
        when(mockPersonalInfoManager.getConsentData()).thenReturn(mockConsentData);
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        subject = new WebViewAdUrlGenerator(context);
        Settings.Secure.putString(RuntimeEnvironment.application.getContentResolver(), Settings.Secure.ANDROID_ID, TEST_UDID);
        expectedUdid = "sha%3A" + Utils.sha1(TEST_UDID);
        configuration = RuntimeEnvironment.application.getResources().getConfiguration();
        shadowTelephonyManager = (MoPubShadowTelephonyManager) Shadows.shadowOf((TelephonyManager) RuntimeEnvironment.application.getSystemService(Context.TELEPHONY_SERVICE));
        shadowConnectivityManager = (MoPubShadowConnectivityManager) Shadows.shadowOf((ConnectivityManager) RuntimeEnvironment.application.getSystemService(Context.CONNECTIVITY_SERVICE));
        shadowConnectivityManager.clearAllNetworks();
        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE));
        methodBuilder = TestMethodBuilderFactory.getSingletonMock();

        LocationService.clearLastKnownLocation();
        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(context, false);
    }

    @After
    public void tearDown() throws Exception {
        reset(methodBuilder);
        ClientMetadata.clearForTesting();
        RequestRateTrackerTest.clearRequestRateTracker();
        MoPubIdentifierTest.clearPreferences(context);
        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
        BaseUrlGenerator.setAppEngineInfo(null);
        BaseUrlGenerator.setWrapperVersion("");
    }

    @Test
    public void generateAdUrl_shouldIncludeMinimumFields() {
        String expectedAdUrl = new AdUrlBuilder(expectedUdid).withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue()).build();

        String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_withHttpsScheme() {
        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).startsWith("https://");
    }

    @Test
    public void generateAdUrl_shouldRunMultipleTimes() throws Exception{
        String expectedAdUrl = new AdUrlBuilder(expectedUdid).withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue()).build();

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(expectedAdUrl);
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_shouldIncludeAllFields() throws Exception {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(
                INIT_ADUNIT).withAdditionalNetwork(
                WebViewAdapterConfiguration.class.getName()).build();
        MoPub.initializeSdk(context, sdkConfiguration, null);
        ShadowLooper.runUiThreadTasks();
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        ClientMetadata.clearForTesting();
        RequestRateTrackerTest.prepareRequestRateTracker("adUnitId", 99, "some_reason");

        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withAdUnitId("adUnitId")
                .withKeywordsQuery("keywordsKey%3AkeywordsValue")
                .withUserDataQuery("userDataKey%3AuserDataValue")
                .withLatLon("20.1%2C30.0", "1", "101325")
                .withMcc("123")
                .withMnc("456")
                .withCountryIso("expected%20country")
                .withCarrierName("expected%20carrier")
                .withAbt("{\"UrlGeneratorTest\":{\"token\":\"WebViewAdvancedBidderToken\"}}")
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .withBackoffMs(99)
                .withBackoffReason("some_reason")
                .build();

        shadowTelephonyManager.setNetworkOperator("123456");
        shadowTelephonyManager.setNetworkCountryIso("expected country");
        shadowTelephonyManager.setNetworkOperatorName("expected carrier");

        Location location = new Location("");
        location.setLatitude(20.1);
        location.setLongitude(30.0);
        location.setAccuracy(1.23f); // should get rounded to "1"
        location.setTime(System.currentTimeMillis() - 101325);
        LocationServiceTest.setLastLocation(location, 10, MoPub.LocationAwareness.NORMAL);


        String adUrl = subject
                .withAdUnitId("adUnitId")
                .withKeywords("keywordsKey:keywordsValue")
                .withUserDataKeywords("userDataKey:userDataValue")
                .generateUrlString("ads.mopub.com");

        // Only compare the seconds since millis can be off
        adUrl = adUrl.replaceFirst("llf=101[0-9]{3}", "llf=101325");

        assertThat(adUrl).isEqualTo(expectedAdUrl);

        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
    }

    @Test
    public void generateAdUrl_shouldNotLocationFieldsWhenConsentIsFalse() throws Exception {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(
                INIT_ADUNIT).withAdditionalNetwork(
                WebViewAdapterConfiguration.class.getName()).build();
        MoPub.initializeSdk(context, sdkConfiguration, null);
        ShadowLooper.runUiThreadTasks();
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withAdUnitId("adUnitId")
                .withKeywordsQuery("keywordsKey%3AkeywordsValue")
                .withAbt("{\"UrlGeneratorTest\":{\"token\":\"WebViewAdvancedBidderToken\"}}")
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();

        shadowTelephonyManager.setNetworkOperator("123456");
        shadowTelephonyManager.setNetworkCountryIso("expected country");
        shadowTelephonyManager.setNetworkOperatorName("expected carrier");

        Location location = new Location("");
        location.setLatitude(20.1);
        location.setLongitude(30.0);
        location.setAccuracy(1.23f); // should get rounded to "1"
        location.setTime(System.currentTimeMillis() - 101325);

        LocationServiceTest.setLastLocation(location, 10, MoPub.LocationAwareness.NORMAL);

        String adUrl = subject
                .withAdUnitId("adUnitId")
                .withKeywords("keywordsKey:keywordsValue")
                .generateUrlString("ads.mopub.com");

        // Only compare the seconds since millis can be off
        adUrl = adUrl.replaceFirst("llf=101[0-9]{3}", "llf=101325");

        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_withNoInit_shouldNotIncludeAbt() throws Exception {
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();

        String adUrl = subject.generateUrlString("ads.mopub.com");
        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_withCollectUserDataDisabled_shouldNotIncludeKeywords() throws Exception {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(INIT_ADUNIT)
                .withAdditionalNetwork(WebViewAdapterConfiguration.class.getName()).build();
        MoPub.initializeSdk(context, sdkConfiguration, null);
        ShadowLooper.runUiThreadTasks();
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        // expected has no keywords
        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withAdUnitId("adUnitId")
                .withKeywordsQuery("keywordsKey%3AkeywordsValue")
                .withAbt("{\"UrlGeneratorTest\":{\"token\":\"WebViewAdvancedBidderToken\"}}")
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();

        shadowTelephonyManager.setNetworkOperator("123456");
        shadowTelephonyManager.setNetworkCountryIso("expected country");
        shadowTelephonyManager.setNetworkOperatorName("expected carrier");

        Location location = new Location("");
        location.setLatitude(20.1);
        location.setLongitude(30.0);
        location.setAccuracy(1.23f); // should get rounded to "1"
        location.setTime(System.currentTimeMillis() - 101325);

        LocationServiceTest.setLastLocation(location, 10, MoPub.LocationAwareness.NORMAL);

        String adUrl = subject
                .withAdUnitId("adUnitId")
                .withUserDataKeywords("key:value")
                .withKeywords("keywordsKey:keywordsValue")
                .generateUrlString("ads.mopub.com");

        // Only compare the seconds since millis can be off
        adUrl = adUrl.replaceFirst("llf=101[0-9]{3}", "llf=101325");

        assertThat(adUrl).isEqualTo(expectedAdUrl);

        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
    }

    @Test
    public void generateAdUrl_withAdvancedBiddingEnabled_shouldIncludeAbt() throws Exception {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(
                "b195f8dd8ded45fe847ad89ed1d016da").withAdditionalNetwork(
                WebViewAdapterConfiguration.class.getName()).build();
        MoPub.initializeSdk(context, sdkConfiguration, null);
        ShadowLooper.runUiThreadTasks();

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withAbt("{\"UrlGeneratorTest\":{\"token\":\"WebViewAdvancedBidderToken\"}}")
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();

        String adUrl = subject.generateUrlString("ads.mopub.com");
        assertThat(adUrl).isEqualTo(expectedAdUrl);

        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
    }

    @Test
    public void generateAdUrl_shouldRecognizeOrientation() {
        configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
        assertThat(generateMinimumUrlString()).contains("&o=l");
        configuration.orientation = Configuration.ORIENTATION_PORTRAIT;
        assertThat(generateMinimumUrlString()).contains("&o=p");
        configuration.orientation = Configuration.ORIENTATION_SQUARE;
        assertThat(generateMinimumUrlString()).contains("&o=s");
    }

    @Test
    public void generateAdUrl_shouldHandleFunkyNetworkOperatorCodes() {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid).withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue());

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
    public void generateAdUrl_needsAndDoesNotHaveReadPhoneState_shouldNotContainOperatorName() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withCarrierName("")
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();

        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(false);

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_shouldIncludeGdprAppliesWhenAvailable() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        when(mockPersonalInfoManager.gdprApplies()).thenReturn(true);

        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withGdprApplies("1")
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_shouldIncludeConsentedPrivacyPolicyVersionWhenAvailable() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        when(mockPersonalInfoManager.getConsentData()).thenReturn(mockConsentData);
        when(mockConsentData.getConsentedPrivacyPolicyVersion()).thenReturn("10");

        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withConsentedPrivacyPolicyVersion("10")
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_shouldIncludeConsentedVendorListVersionWhenAvailable() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        when(mockPersonalInfoManager.getConsentData()).thenReturn(mockConsentData);
        when(mockConsentData.getConsentedVendorListVersion()).thenReturn("15");

        final String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withConsentedVendorListVersion("15")
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(expectedAdUrl);
    }


    @Test
    public void generateAdUrl_needsAndHasReadPhoneState_shouldContainOperatorName() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid).withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue());

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(true);
        shadowTelephonyManager.setNetworkOperatorName("TEST_NAME");

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withCarrierName("TEST_NAME").build());
    }

    @Test
    public void generateAdUrl_doesNotNeedReadPhoneState_shouldContainOperatorName() {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid).withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue());

        shadowTelephonyManager.setNeedsReadPhoneState(false);
        shadowTelephonyManager.setReadPhoneStatePermission(false);
        shadowTelephonyManager.setNetworkOperatorName("TEST_NAME");

        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withCarrierName("TEST_NAME").build());
    }

    @Test
    public void generateAdurl_whenOnCDMA_shouldGetOwnerStringFromSimCard() {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid).withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue());
        shadowTelephonyManager.setPhoneType(TelephonyManager.PHONE_TYPE_CDMA);
        shadowTelephonyManager.setSimState(TelephonyManager.SIM_STATE_READY);
        shadowTelephonyManager.setNetworkOperator("123456");
        shadowTelephonyManager.setSimOperator("789012");
        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("789").withMnc("012").build());
    }

    @Test
    public void generateAdurl_whenSimNotReady_shouldDefaultToNetworkOperator() {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);
        shadowTelephonyManager.setPhoneType(TelephonyManager.PHONE_TYPE_CDMA);
        shadowTelephonyManager.setSimState(TelephonyManager.SIM_STATE_ABSENT);
        shadowTelephonyManager.setNetworkOperator("123456");
        shadowTelephonyManager.setSimOperator("789012");
        String adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withMcc("123")
                                            .withMnc("456").
                                            withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                                            .build());
    }

    @Test
    public void generateAdUrl_shouldSetNetworkType() throws Exception {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid).withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue());
        String adUrl;

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        shadowConnectivityManager.setActiveNetworkInfo(null);
        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_DUMMY));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.UNKNOWN).build());

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_ETHERNET));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.ETHERNET).build());
        shadowConnectivityManager.clearAllNetworks();

        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_WIFI));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.WIFI).build());

        shadowConnectivityManager.setNetworkInfo(TYPE_WIFI, null);
        shadowConnectivityManager.clearAllNetworks();
        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE));

        // bunch of random mobile types just to make life more interesting
        shadowConnectivityManager.setActiveNetworkInfo(
                createNetworkInfo(TYPE_MOBILE, TelephonyManager.NETWORK_TYPE_GPRS));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.GG).build());

        shadowConnectivityManager.setActiveNetworkInfo(
                createNetworkInfo(TYPE_MOBILE, TelephonyManager.NETWORK_TYPE_HSPA));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.GGG).build());

        shadowConnectivityManager.setActiveNetworkInfo(
                createNetworkInfo(TYPE_MOBILE, TelephonyManager.NETWORK_TYPE_EVDO_0));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.GGG).build());

        shadowConnectivityManager.setActiveNetworkInfo(
                createNetworkInfo(TYPE_MOBILE, TelephonyManager.NETWORK_TYPE_LTE));
        adUrl = generateMinimumUrlString();
        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.GGGG).build());
    }

    @Test
    public void generateAdUrl_whenNoNetworkPermission_shouldGenerateUnknownNetworkType() {
        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid);

        Shadows.shadowOf(context).denyPermissions(ACCESS_NETWORK_STATE);
        shadowConnectivityManager.setActiveNetworkInfo(createNetworkInfo(TYPE_MOBILE));

        String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.UNKNOWN)
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue()).build());
    }

    @Test
    public void generateAdUrl_shouldTolerateNullActiveNetwork() throws Exception {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(INIT_ADUNIT)
                .withAdditionalNetwork(WebViewAdapterConfiguration.class.getName()).build();
        MoPub.initializeSdk(context, sdkConfiguration, null);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);

        AdUrlBuilder urlBuilder = new AdUrlBuilder(expectedUdid)
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .withAbt("{\"UrlGeneratorTest\":{\"token\":\"WebViewAdvancedBidderToken\"}}");
        shadowConnectivityManager.setActiveNetworkInfo(null);

        String adUrl = generateMinimumUrlString();

        assertThat(adUrl).isEqualTo(urlBuilder.withNetworkType(MoPubNetworkType.UNKNOWN).build());

        new Reflection.MethodBuilder(null, "resetMoPub")
                .setStatic(MoPub.class)
                .setAccessible()
                .execute();
    }

    @Test
    public void generateAdUrl_whenAdInfoIsCached_shouldUseAdInfoParams() throws Exception {
        SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder(INIT_ADUNIT).build();
        MoPub.initializeSdk(context, sdkConfiguration, null);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(context, false);

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(context);
        MoPubIdentifier identifier = clientMetadata.getMoPubIdentifier();
        AdvertisingId adInfo = identifier.getAdvertisingInfo();

        String generatedAdUrl = generateMinimumUrlString();
        expectedUdid = "ifa%3A" + adInfo.getIdWithPrefix(true);
        String expectedAdUrl = new AdUrlBuilder(expectedUdid)
                .withDnt(adInfo.isDoNotTrack())
                .withCurrentConsentStatus(ConsentStatus.UNKNOWN.getValue())
                .build();
        assertThat(generatedAdUrl).isEqualTo(expectedAdUrl);
    }

    @Test
    public void generateAdUrl_withNullPackageName_withEmptyPackageName_shouldNotIncludeBundleKey() {
        when(spyApplicationContext.getPackageName()).thenReturn(null).thenReturn("");

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

    @Test
    public void generateAdUrl_whenEngineInfoIsNotSet_shouldIncludeEngineInfo() {
        String adUrl = generateMinimumUrlString();

        assertThat(getParameterFromRequestUrl(adUrl, "e_name")).isNullOrEmpty();
        assertThat(getParameterFromRequestUrl(adUrl, "e_ver")).isNullOrEmpty();
    }

    @Test
    public void generateAdUrl_whenEngineInfoIsSet_shouldIncludeEngineInfo() {
        MoPub.setEngineInformation(new AppEngineInfo("ename", "eversion"));
        String adUrl = generateMinimumUrlString();

        assertEquals(getParameterFromRequestUrl(adUrl, "e_name"), "ename");
        assertEquals(getParameterFromRequestUrl(adUrl, "e_ver"), "eversion");
    }

    @Test
    public void generateAdUrl_withWrapperVersion_shouldIncludeWrapperVersion() {
        MoPub.setWrapperVersion("WebViewAdUrlGeneratorTestVersion");

        String adUrl = generateMinimumUrlString();

        assertEquals(getParameterFromRequestUrl(adUrl, "w_ver"), "WebViewAdUrlGeneratorTestVersion");
    }

    private String getParameterFromRequestUrl(String requestString, String key) {
        Uri requestUri = Uri.parse(requestString);
        String parameter = requestUri.getQueryParameter(key);

        if (TextUtils.isEmpty(parameter)) {
            return "";
        }

        return parameter;
    }

    private NetworkInfo createNetworkInfo(int type, int subtype) {
        return ShadowNetworkInfo.newInstance(null,
                type,
                subtype, true, true);
    }

    private NetworkInfo createNetworkInfo(int type) {
       return createNetworkInfo(type, NETWORK_TYPE_UNKNOWN);
    }

    private String generateMinimumUrlString() {
        return subject.generateUrlString("ads.mopub.com");
    }

    private static class AdUrlBuilder {
        private String expectedUdid;
        private String adUnitId = "";
        private String keywordsQuery = "";
        private String userDataQuery = "";
        private String latLon = "";
        private String locationAccuracy = "";
        private String latLonLastUpdated = "";
        private String mnc = "";
        private String mcc = "";
        private String countryIso = "";
        private String carrierName = "";
        private String dnt = "";
        private MoPubNetworkType networkType = MoPubNetworkType.MOBILE;
        private String abt = "";
        private String currentConsentStatus = "";
        private String gdprApplies = "0";
        private String forceGdprApplies = "0";
        private String consentedPrivacyPolicyVersion = "";
        private String consentedVendorListVersion = "";
        private String backoffMs = "";
        private String backoffReason = "";

        public AdUrlBuilder(String expectedUdid) {
            this.expectedUdid = expectedUdid;
        }

        public String build() {
            return "https://ads.mopub.com/m/ad" +
                    "?v=6" +
                    paramIfNotEmpty("id", adUnitId) +
                    "&nv=" + Uri.encode(MoPub.SDK_VERSION) +
                    "&dn=" + Build.MANUFACTURER +
                    "%2C" + Build.MODEL +
                    "%2C" + Build.PRODUCT +
                    "&bundle=" + "testBundle" +
                    paramIfNotEmpty("q", keywordsQuery) +
                    paramIfNotEmpty("user_data_q", userDataQuery) +
                    (TextUtils.isEmpty(latLon) ? "" :
                            "&ll=" + latLon + "&lla=" + locationAccuracy + "&llf=" + latLonLastUpdated + "&llsdk=1") +
                    "&z=-0700" +
                    "&o=p" +
                    "&cw=" + TEST_SCREEN_SAFE_WIDTH +
                    "&ch=" + TEST_SCREEN_SAFE_HEIGHT +
                    "&w=" + TEST_SCREEN_WIDTH +
                    "&h=" + TEST_SCREEN_HEIGHT +
                    "&sc=1.0" +
                    paramIfNotEmpty("mcc", mcc) +
                    paramIfNotEmpty("mnc", mnc) +
                    paramIfNotEmpty("iso", countryIso) +
                    paramIfNotEmpty("cn", carrierName) +
                    "&ct=" + networkType +
                    "&av=" + Uri.encode(BuildConfig.VERSION_NAME) +
                    (TextUtils.isEmpty(abt) ? "" : "&abt=" + Uri.encode(abt)) +
                    "&udid=" + PlayServicesUrlRewriter.UDID_TEMPLATE +
                    "&dnt=" + PlayServicesUrlRewriter.DO_NOT_TRACK_TEMPLATE +
                    "&mid=" + PlayServicesUrlRewriter.MOPUB_ID_TEMPLATE +
                    paramIfNotEmpty("gdpr_applies", gdprApplies) +
                    paramIfNotEmpty("force_gdpr_applies", forceGdprApplies) +
                    paramIfNotEmpty("current_consent_status", currentConsentStatus) +
                    paramIfNotEmpty("consented_privacy_policy_version", consentedPrivacyPolicyVersion) +
                    paramIfNotEmpty("consented_vendor_list_version", consentedVendorListVersion) +
                    paramIfNotEmpty("backoff_ms", backoffMs) +
                    paramIfNotEmpty("backoff_reason", backoffReason) +
                    "&mr=1" +
                    "&vv=0";
        }

        public AdUrlBuilder withAdUnitId(String adUnitId) {
            this.adUnitId = adUnitId;
            return this;
        }

        public AdUrlBuilder withUserDataQuery(String query) {
            this.userDataQuery = query;
            return this;
        }
        public AdUrlBuilder withKeywordsQuery(String query) {
            this.keywordsQuery = query;
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

        public AdUrlBuilder withDnt(boolean dnt) {
            if (dnt) {
                this.dnt = "1";
            }
            return this;
        }

        public AdUrlBuilder withAbt(String abt) {
            this.abt = abt;
            return this;
        }

        public AdUrlBuilder withCurrentConsentStatus(String currentConsentStatus) {
            this.currentConsentStatus = currentConsentStatus;
            return this;
        }

        public AdUrlBuilder withGdprApplies(String gdprApplies) {
            this.gdprApplies = gdprApplies;
            return this;
        }

        public AdUrlBuilder withConsentedPrivacyPolicyVersion(String consentedPrivacyPolicyVersion) {
            this.consentedPrivacyPolicyVersion = consentedPrivacyPolicyVersion;
            return this;
        }

        public AdUrlBuilder withConsentedVendorListVersion(String consentedVendorListVersion) {
            this.consentedVendorListVersion = consentedVendorListVersion;
            return this;
        }

        AdUrlBuilder withBackoffMs(@Nullable final Integer backoffMs) {
            if (backoffMs != null) {
                this.backoffMs = String.valueOf(backoffMs);
            }
            return this;
        }

        AdUrlBuilder withBackoffReason(@Nullable final String backoffReason) {
            this.backoffReason = backoffReason;
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

    private static class WebViewAdapterConfiguration extends BaseAdapterConfiguration {

        @NonNull
        @Override
        public String getAdapterVersion() {
            return "adapterVersion";
        }

        @Nullable
        @Override
        public String getBiddingToken(@NonNull final Context context) {
            return "WebViewAdvancedBidderToken";
        }

        @NonNull
        @Override
        public String getMoPubNetworkName() {
            return "UrlGeneratorTest";
        }

        @NonNull
        @Override
        public String getNetworkSdkVersion() {
            return "networkVersion";
        }

        @Override
        public void initializeNetwork(@NonNull final Context context,
                @Nullable final Map<String, String> configuration,
                @NonNull final OnNetworkInitializationFinishedListener listener) {
        }
    }

}
