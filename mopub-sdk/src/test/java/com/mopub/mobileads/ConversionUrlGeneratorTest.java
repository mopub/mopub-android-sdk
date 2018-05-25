package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;
import com.mopub.nativeads.NativeUrlGeneratorTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(ClientMetadata.class)
public class ConversionUrlGeneratorTest {
    private static final String APP_VERSION = "app_version";
    private static final String CONSENT_STATUS = "consent_status";
    private static final String PRIVACY_VERSION = "privacy_version";
    private static final String VENDOR_LIST_VERSION = "vendor_list_version";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private Context context;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();

        ClientMetadata clientMetadata = mock(ClientMetadata.class);
        when(clientMetadata.getAppVersion()).thenReturn(APP_VERSION);

        PowerMockito.mockStatic(ClientMetadata.class);
        when(ClientMetadata.getInstance(context)).thenReturn(clientMetadata);
    }

    //https://ads.mopub.com/m/open?v=6&av=app_version&udid=mp_tmpl_advertising_id&dnt=mp_tmpl_do_not_track&id=com.mopub.mobileads&st=1&nv=5.0.0&current_consent_status=consent_status&consented_vendor_list_version=vendor_list_version&consented_privacy_policy_version=privacy_version&gdpr_applies=0
    @Test
    public void generateUrlString_allParametersSet_shouldReturnValidUrl() {
        ConversionUrlGenerator subject = new ConversionUrlGenerator(context);

        String url = subject.withGdprApplies(false)
                .withCurrentConsentStatus(CONSENT_STATUS)
                .withConsentedPrivacyPolicyVersion(PRIVACY_VERSION)
                .withConsentedVendorListVersion(VENDOR_LIST_VERSION)
                .withSessionTracker(true)
                .generateUrlString(Constants.HOST);

        assertThat(url).startsWith(Constants.HTTPS + "://" + Constants.HOST + Constants.CONVERSION_TRACKING_HANDLER);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "v")).isEqualTo("6");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "nv")).isEqualTo(MoPub.SDK_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "av")).isEqualTo(APP_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "udid")).isEqualTo("mp_tmpl_advertising_id");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "st")).isEqualTo("1");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "current_consent_status")).isEqualTo(CONSENT_STATUS);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "gdpr_applies")).isEqualTo("0");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "consented_vendor_list_version")).isEqualTo(VENDOR_LIST_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "consented_privacy_policy_version")).isEqualTo(PRIVACY_VERSION);
    }

    @Test
    public void generateUrlString_allParametersNoSt_shouldReturnValidUrl() {
        ConversionUrlGenerator subject = new ConversionUrlGenerator(context);

        String url = subject.withGdprApplies(false)
                .withCurrentConsentStatus(CONSENT_STATUS)
                .withConsentedPrivacyPolicyVersion(PRIVACY_VERSION)
                .withConsentedVendorListVersion(VENDOR_LIST_VERSION)
                .withSessionTracker(false)
                .generateUrlString(Constants.HOST);

        assertThat(url).startsWith(Constants.HTTPS + "://" + Constants.HOST + Constants.CONVERSION_TRACKING_HANDLER);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "v")).isEqualTo("6");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "nv")).isEqualTo(MoPub.SDK_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "av")).isEqualTo(APP_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "udid")).isEqualTo("mp_tmpl_advertising_id");
        assertThat(url.indexOf("&st=")).isEqualTo(-1);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "current_consent_status")).isEqualTo(CONSENT_STATUS);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "gdpr_applies")).isEqualTo("0");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "consented_vendor_list_version")).isEqualTo(VENDOR_LIST_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "consented_privacy_policy_version")).isEqualTo(PRIVACY_VERSION);
    }
}
