package com.mopub.common;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import com.mopub.common.privacy.MoPubIdentifierTest;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;
import com.mopub.common.util.Reflection;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class,
        shadows = {MoPubShadowTelephonyManager.class})
public class ClientMetadataTest {

    private Activity activityContext;
    private MoPubShadowTelephonyManager shadowTelephonyManager;
    private PersonalInfoManager mockPersonalInfoManager;

    @Before
    public void setUp() throws Exception {
        activityContext = Robolectric.buildActivity(Activity.class).create().get();
        Shadows.shadowOf(activityContext).grantPermissions(ACCESS_NETWORK_STATE);
        shadowTelephonyManager = (MoPubShadowTelephonyManager)
                Shadows.shadowOf((TelephonyManager) activityContext.getSystemService(Context.TELEPHONY_SERVICE));
        mockPersonalInfoManager = mock(PersonalInfoManager.class);
        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(activityContext, false);
    }

    @After
    public void tearDown() {
        MoPubIdentifierTest.clearPreferences(activityContext);
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
    public void testCachedData_shouldBeAvailable() throws Exception {
        shadowTelephonyManager.setNetworkOperatorName("testNetworkOperatorName");
        shadowTelephonyManager.setNetworkOperator("testNetworkOperator");
        shadowTelephonyManager.setNetworkCountryIso("1");
        shadowTelephonyManager.setSimCountryIso("1");

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
            .setStatic(MoPub.class)
            .setAccessible()
            .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
            .execute();

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);
        // Telephony manager data.
        assertThat(clientMetadata.getNetworkOperatorForUrl()).isEqualTo("testNetworkOperator");
        assertThat(clientMetadata.getNetworkOperatorName()).isEqualTo("testNetworkOperatorName");
        assertThat(clientMetadata.getIsoCountryCode()).isEqualTo("1");
    }

    @Test
    public void testCachedData_shouldNotBeAvailableWhenConsentIsFalse() throws Exception {
        shadowTelephonyManager.setNetworkOperatorName("testNetworkOperatorName");
        shadowTelephonyManager.setNetworkOperator("testNetworkOperator");
        shadowTelephonyManager.setNetworkCountryIso("1");
        shadowTelephonyManager.setSimCountryIso("1");

        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(activityContext);
        // Telephony manager data.
        assertThat(clientMetadata.getIsoCountryCode()).isEqualTo("");
    }
}
