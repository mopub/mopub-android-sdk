package com.mopub.common;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class,
        shadows = {MoPubShadowTelephonyManager.class})
public class ClientMetadataTest {

    public Activity activityContext;
    private MoPubShadowTelephonyManager shadowTelephonyManager;

    @Before
    public void setUp() throws Exception {
        activityContext = Robolectric.buildActivity(Activity.class).create().get();
        Shadows.shadowOf(activityContext).grantPermissions(ACCESS_NETWORK_STATE);
        shadowTelephonyManager = (MoPubShadowTelephonyManager)
                Shadows.shadowOf((TelephonyManager) activityContext.getSystemService(Context.TELEPHONY_SERVICE));
    }

    @After
    public void tearDown() throws Exception {
        ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putString(resolver, "limit_ad_tracking", null);
        Settings.Secure.putString(resolver, "advertising_id", null);
    }

    // This has to be first or the singleton will be initialized by an earlier test. We should
    // destroy the application between tests to get around this.
    @Test
    public void getWithoutContext_shouldReturnNull() {
        final ClientMetadata clientMetadata = ClientMetadata.getInstance();
        assertThat(clientMetadata).isNull();
    }

    @Test
    public void getWithContext_shouldReturnInstance() {
        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);
        assertThat(clientMetadata).isNotNull();
    }

    @Test
    public void getWithoutContextAfterInit_shouldReturnInstance() {
        ClientMetadata.getInstance(activityContext);
        final ClientMetadata clientMetadata = ClientMetadata.getInstance();
        assertThat(clientMetadata).isNotNull();
    }

    @Test
    public void testCachedData_shouldBeAvailable() {
        shadowTelephonyManager.setNetworkOperatorName("testNetworkOperatorName");
        shadowTelephonyManager.setNetworkOperator("testNetworkOperator");
        shadowTelephonyManager.setNetworkCountryIso("1");
        shadowTelephonyManager.setSimCountryIso("1");

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);
        // Telephony manager data.
        assertThat(clientMetadata.getNetworkOperatorForUrl()).isEqualTo("testNetworkOperator");
        assertThat(clientMetadata.getNetworkOperatorName()).isEqualTo("testNetworkOperatorName");
        assertThat(clientMetadata.getIsoCountryCode()).isEqualTo("1");

        // Other cached data.
        assertThat(clientMetadata.getDeviceId()).isNotNull().isNotEmpty();
    }

    @Test
    public void constructor_onAmazonDevice_shouldSetAmazonAdvertisingInfo() {
        ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putInt(resolver, "limit_ad_tracking", 1);
        Settings.Secure.putString(resolver, "advertising_id", "this-is-an-ifa");

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);

        assertThat(clientMetadata.getDeviceId()).isEqualTo("ifa:this-is-an-ifa");
        assertThat(clientMetadata.isDoNotTrackSet()).isTrue();
        assertThat(clientMetadata.isAdvertisingInfoSet()).isTrue();
    }

    @Test
    public void constructor_onNonAmazonDevice_shouldSetSha() {
        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);

        assertThat(clientMetadata.getDeviceId()).startsWith("sha:");
        assertThat(clientMetadata.isDoNotTrackSet()).isFalse();
        assertThat(clientMetadata.isAdvertisingInfoSet()).isFalse();
    }

    @Test
    public void constructor_onAmazonDevice_withoutLimitAdTracking_shouldSetSha() {
        ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putString(resolver, "advertising_id", "this-is-an-ifa");

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);

        assertThat(clientMetadata.getDeviceId()).startsWith("sha:");
        assertThat(clientMetadata.isDoNotTrackSet()).isFalse();
        assertThat(clientMetadata.isAdvertisingInfoSet()).isFalse();
    }

    @Test
    public void constructor_onAmazonDevice_withoutAdvertisingId_shouldSetSha() {
        ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putInt(resolver, "limit_ad_tracking", 1);

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);

        assertThat(clientMetadata.getDeviceId()).startsWith("sha:");
        assertThat(clientMetadata.isDoNotTrackSet()).isFalse();
        assertThat(clientMetadata.isAdvertisingInfoSet()).isFalse();
    }

    @Test
    public void constructor_onAmazonDevice_withEmptyAdvertisingId_shouldSetSha() {
        ContentResolver resolver = RuntimeEnvironment.application.getContentResolver();
        Settings.Secure.putInt(resolver, "limit_ad_tracking", 1);
        Settings.Secure.putString(resolver, "advertising_id", "");

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);

        assertThat(clientMetadata.getDeviceId()).startsWith("sha:");
        assertThat(clientMetadata.isDoNotTrackSet()).isFalse();
        assertThat(clientMetadata.isAdvertisingInfoSet()).isFalse();
    }
}
