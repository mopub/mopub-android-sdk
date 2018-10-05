// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.NativeUrlGeneratorTest;
import com.mopub.network.PlayServicesUrlRewriter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest(ClientMetadata.class)
public class SyncUrlGeneratorTest {
    private static final String APP_VERSION = "appVersion";
    private static final String AD_UNIT = "adUnit";
    private static final String UDID = "udid";
    private static final String LAST_CHANGED_MS = "lastChangedMs";
    private static final String CONSENT_CHANGE_REASON = "consentChangeReason";
    private static final String CONSENTED_VENDOR_LIST_VERSION = "consentedVendorListVersion";
    private static final String CONSENTED_PRIVACY_POLICY_VERSION = "consentedPrivacyPolicyVersion";
    private static final String IAB_HASH = "iabHash";
    private static final String EXTRAS = "extras";

    private Context context;
    private SyncUrlGenerator subject;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        context = Robolectric.buildActivity(Activity.class).get();
        subject = new SyncUrlGenerator(context, ConsentStatus.UNKNOWN.getValue());

        ClientMetadata clientMetadata = mock(ClientMetadata.class);
        when(clientMetadata.getAppVersion()).thenReturn(APP_VERSION);

        PowerMockito.mockStatic(ClientMetadata.class);
        when(ClientMetadata.getInstance(any(Context.class))).thenReturn(clientMetadata);
    }

    @Test
    public void generateUrlString_withAllParams_shouldGenerateFullUrl() {
        subject.withAdUnitId(AD_UNIT);
        subject.withUdid(UDID);
        subject.withGdprApplies(true);
        subject.withForceGdprApplies(true);
        subject.withForceGdprAppliesChanged(true);
        subject.withLastChangedMs(LAST_CHANGED_MS);
        subject.withLastConsentStatus(ConsentStatus.UNKNOWN);
        subject.withConsentChangeReason(CONSENT_CHANGE_REASON);
        subject.withConsentedVendorListVersion(CONSENTED_VENDOR_LIST_VERSION);
        subject.withConsentedPrivacyPolicyVersion(CONSENTED_PRIVACY_POLICY_VERSION);
        subject.withCachedVendorListIabHash(IAB_HASH);
        subject.withExtras(EXTRAS);

        final String url = subject.generateUrlString("hostname");

        assertThat(url).startsWith(
                Constants.HTTPS + "://" + "hostname" + Constants.GDPR_SYNC_HANDLER);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "id")).isEqualTo(AD_UNIT);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "nv")).isEqualTo(
                MoPub.SDK_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url, "udid")).isEqualTo(
                UDID);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "current_consent_status")).isEqualTo(ConsentStatus.UNKNOWN.getValue());
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "gdpr_applies")).isEqualTo("1");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "force_gdpr_applies")).isEqualTo("1");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "forced_gdpr_applies_changed")).isEqualTo("1");
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "last_changed_ms")).isEqualTo(LAST_CHANGED_MS);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "consent_change_reason")).isEqualTo(CONSENT_CHANGE_REASON);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "consented_vendor_list_version")).isEqualTo(CONSENTED_VENDOR_LIST_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "consented_privacy_policy_version")).isEqualTo(CONSENTED_PRIVACY_POLICY_VERSION);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "cached_vendor_list_iab_hash")).isEqualTo(IAB_HASH);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "extras")).isEqualTo(EXTRAS);
        assertThat(NativeUrlGeneratorTest.getParameterFromRequestUrl(url,
                "dnt")).isEqualTo(PlayServicesUrlRewriter.DO_NOT_TRACK_TEMPLATE);
    }

    @Test
    public void generateUrlString_withMinimumParams_shouldGenerateValidUrl() {
        final String url = subject.generateUrlString("minurl");

        assertThat(url).isEqualTo("https://minurl/m/gdpr_sync?nv=" + MoPub.SDK_VERSION +
        "&current_consent_status=unknown&force_gdpr_applies=0&dnt=mp_tmpl_do_not_track");
    }

    @Test
    public void generateUrlString_withExtrasThatShouldBeUrlEncoded_shouldGenerateValidUrl() {
        subject = new SyncUrlGenerator(context, ConsentStatus.EXPLICIT_YES.getValue());
        subject.withExtras("!@#$%^&*()_;'[]{}|\\");

        final String url = subject.generateUrlString("host");

        assertThat(url).isEqualTo("https://host/m/gdpr_sync?nv=" + MoPub.SDK_VERSION +
                "&current_consent_status=explicit_yes" +
                "&extras=!%40%23%24%25%5E%26*()_%3B'%5B%5D%7B%7D%7C%5C" +
                "&force_gdpr_applies=0&dnt=mp_tmpl_do_not_track");
    }

}
