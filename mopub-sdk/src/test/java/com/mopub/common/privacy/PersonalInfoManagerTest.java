// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;
import android.content.Context;
import android.os.SystemClock;

import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.MoPubRequestQueueTest;
import com.mopub.network.Networking;
import com.mopub.volley.Request;

import org.junit.After;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@Config(constants = BuildConfig.class)
@PrepareForTest({ClientMetadata.class, Networking.class, AdvertisingId.class, MoPub.class})
public class PersonalInfoManagerTest {

    private static int DEFAULT_TIME_MS = 300000;
    private static int SHORT_TIME_MS = 150000;
    private static int LONG_TIME_MS = 310000;

    private Activity activity;
    private PersonalInfoManager subject;
    private PersonalInfoData personalInfoData;
    private ConsentStatusChangeListener mockConsentStatusChangeListener;
    private ClientMetadata mockClientMetadata;
    private MoPubIdentifier mockMoPubIdentifier;
    private AdvertisingId mockAdvertisingId;
    private MoPubRequestQueue mockRequestQueue;

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).get();

        PowerMockito.mockStatic(ClientMetadata.class);
        mockClientMetadata = PowerMockito.mock(ClientMetadata.class);
        mockMoPubIdentifier = mock(MoPubIdentifier.class);
        mockAdvertisingId = PowerMockito.mock(AdvertisingId.class);
        PowerMockito.when(mockAdvertisingId.getIfaWithPrefix()).thenReturn("udid");

        PowerMockito.when(ClientMetadata.getInstance(any(Context.class))).thenReturn(
                mockClientMetadata);
        PowerMockito.when(mockClientMetadata.getMoPubIdentifier()).thenReturn(mockMoPubIdentifier);
        when(mockMoPubIdentifier.getAdvertisingInfo()).thenReturn(mockAdvertisingId);

        PowerMockito.mockStatic(Networking.class);
        mockRequestQueue = mock(MoPubRequestQueueTest.TestMoPubRequestQueue2.class);
        when(Networking.getRequestQueue(any(Context.class))).thenReturn(mockRequestQueue);
        when(Networking.getScheme()).thenReturn("https");

        PowerMockito.mockStatic(MoPub.class);
        when(MoPub.isSdkInitialized()).thenReturn(true);

        subject = new PersonalInfoManager(activity, "adunit", null);
        personalInfoData = subject.getPersonalInfoData();
        mockConsentStatusChangeListener = mock(ConsentStatusChangeListener.class);
        subject.subscribeConsentStatusChangeListener(mockConsentStatusChangeListener);
    }

    @After
    public void tearDown() {
        final PersonalInfoData personalInfoData = subject.getPersonalInfoData();
        personalInfoData.setConsentStatus(ConsentStatus.UNKNOWN);
        personalInfoData.setLastSuccessfullySyncedConsentStatus(null);
        personalInfoData.setConsentChangeReason(null);
        personalInfoData.setForceGdprApplies(false);
        personalInfoData.setUdid(null);
        personalInfoData.setLastChangedMs(null);
        personalInfoData.setConsentStatusBeforeDnt(null);
        personalInfoData.setWhitelisted(false);
        personalInfoData.setCurrentVendorListVersion(null);
        personalInfoData.setCurrentVendorListLink(null);
        personalInfoData.setCurrentPrivacyPolicyVersion(null);
        personalInfoData.setCurrentPrivacyPolicyLink(null);
        personalInfoData.setCurrentVendorListIabFormat(null);
        personalInfoData.setCurrentVendorListIabHash(null);
        personalInfoData.setConsentedVendorListVersion(null);
        personalInfoData.setConsentedPrivacyPolicyVersion(null);
        personalInfoData.setConsentedVendorListIabFormat(null);
        personalInfoData.setExtras(null);
        personalInfoData.setShouldReacquireConsent(false);
        personalInfoData.setGdprApplies(null);
        personalInfoData.writeToDisk();
    }

    @Test
    public void shouldShowConsentDialog_withGdprAppliesNull_shouldReturnFalse() {
        subject.getPersonalInfoData().setGdprApplies(null);

        final boolean actual = subject.shouldShowConsentDialog();

        assertThat(actual).isFalse();
    }

    @Test
    public void shouldShowConsentDialog_withGdprAppliesFalse_shouldReturnFalse() {
        subject.getPersonalInfoData().setGdprApplies(false);

        final boolean actual = subject.shouldShowConsentDialog();

        assertThat(actual).isFalse();
    }

    @Test
    public void shouldShowConsentDialog_withGdprAppliesTrue_withVariousConsentStatuses_shouldReturnCorrectValue() {
        personalInfoData.setGdprApplies(true);

        personalInfoData.setConsentStatus(ConsentStatus.UNKNOWN);
        assertThat(subject.shouldShowConsentDialog()).isTrue();

        personalInfoData.setConsentStatus(ConsentStatus.POTENTIAL_WHITELIST);
        assertThat(subject.shouldShowConsentDialog()).isFalse();

        personalInfoData.setConsentStatus(ConsentStatus.DNT);
        assertThat(subject.shouldShowConsentDialog()).isFalse();

        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);
        assertThat(subject.shouldShowConsentDialog()).isFalse();

        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_NO);
        assertThat(subject.shouldShowConsentDialog()).isFalse();
    }

    @Test
    public void shouldShowConsentDialog_withGdprAppliesTrue_withShouldReacquireConsentTrue_withConsentStatusYes_withConsentStatusUnknown_shouldReturnTrue() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setShouldReacquireConsent(true);

        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);
        assertThat(subject.shouldShowConsentDialog()).isTrue();

        personalInfoData.setConsentStatus(ConsentStatus.UNKNOWN);
        assertThat(subject.shouldShowConsentDialog()).isTrue();
    }

    @Test
    public void shouldShowConsentDialog_withGdprAppliesTrue_withShouldReacquireConsentTrue_withConsentStatusNotYesOrUnknown_shouldReturnFalse() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setShouldReacquireConsent(true);

        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_NO);
        assertThat(subject.shouldShowConsentDialog()).isFalse();

        personalInfoData.setConsentStatus(ConsentStatus.POTENTIAL_WHITELIST);
        assertThat(subject.shouldShowConsentDialog()).isFalse();

        personalInfoData.setConsentStatus(ConsentStatus.DNT);
        assertThat(subject.shouldShowConsentDialog()).isFalse();
    }

    @Test
    public void canCollectPersonalInformation_withGdprAppliesNull_shouldReturnFalse() {
        personalInfoData.setGdprApplies(null);
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);

        final boolean actual = subject.canCollectPersonalInformation();

        assertThat(actual).isFalse();
    }

    @Test
    public void canCollectPersonalInformation_withGdprAppliesFalse_shouldReturnTrue() {
        personalInfoData.setGdprApplies(false);

        assertThat(subject.canCollectPersonalInformation()).isTrue();

        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_NO);
        assertThat(subject.canCollectPersonalInformation()).isTrue();

        personalInfoData.setConsentStatus(ConsentStatus.DNT);
        assertThat(subject.canCollectPersonalInformation()).isTrue();

        personalInfoData.setConsentStatus(ConsentStatus.POTENTIAL_WHITELIST);
        assertThat(subject.canCollectPersonalInformation()).isTrue();

        personalInfoData.setWhitelisted(true);
        assertThat(subject.canCollectPersonalInformation()).isTrue();

        // It should not matter what is set. GdprApplies false means can collect personal information
        personalInfoData.setConsentedVendorListIabFormat("abc");
        assertThat(subject.canCollectPersonalInformation()).isTrue();
    }

    @Test
    public void gdprApplies_withForceGdprAppliesTrue_shouldReturnTrue() {
        personalInfoData.setForceGdprApplies(true);

        personalInfoData.setGdprApplies(null);
        assertThat(subject.gdprApplies()).isTrue();

        personalInfoData.setGdprApplies(true);
        assertThat(subject.gdprApplies()).isTrue();

        personalInfoData.setGdprApplies(false);
        assertThat(subject.gdprApplies()).isTrue();

        personalInfoData.setConsentStatus(ConsentStatus.DNT);
        assertThat(subject.gdprApplies()).isTrue();
    }

    @Test
    public void gdprApplies_withForceGdprAppliesFalse_shouldRespectGdprApplies() {
        personalInfoData.setForceGdprApplies(false);

        personalInfoData.setGdprApplies(null);
        assertThat(subject.gdprApplies()).isNull();

        personalInfoData.setGdprApplies(false);
        assertThat(subject.gdprApplies()).isFalse();

        personalInfoData.setGdprApplies(true);
        assertThat(subject.gdprApplies()).isTrue();
    }

    @Test
    public void forceGdprApplies_withGdprAppliesFalse_shouldForceGdprToTrue_shouldFiresOnConsentStatusChangeListeners() {
        personalInfoData.setGdprApplies(false);
        // Precondition just to verify gdprApplies is actually false for now
        assertThat(subject.gdprApplies()).isFalse();
        assertThat(subject.canCollectPersonalInformation()).isTrue();

        subject.forceGdprApplies();

        assertThat(subject.gdprApplies()).isTrue();
        assertThat(personalInfoData.isForceGdprApplies()).isTrue();
        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.UNKNOWN, false);
    }

    @Test
    public void forceGdprApplies_withForceGdprAppliesAlreadySet_shouldDoNothing() {
        personalInfoData.setGdprApplies(false);
        personalInfoData.setForceGdprApplies(true);

        subject.forceGdprApplies();

        assertThat(subject.gdprApplies()).isTrue();
        verifyZeroInteractions(mockConsentStatusChangeListener);
    }

    @Test
    public void forceGdprApplies_withGdprAppliesTrue_shouldSetForceGdprApplies_shouldNotFireOnConsentStatusChangeListeners() {
        personalInfoData.setGdprApplies(true);
        // Precondition just to verify gdprApplies is actually true for now
        assertThat(subject.gdprApplies()).isTrue();
        assertThat(subject.canCollectPersonalInformation()).isFalse();

        subject.forceGdprApplies();

        assertThat(subject.gdprApplies()).isTrue();
        assertThat(personalInfoData.isForceGdprApplies()).isTrue();
        verifyZeroInteractions(mockConsentStatusChangeListener);
    }

    @Test
    public void grantConsent_withWhitelistedApp_shouldTransitionToExplicitYes() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setWhitelisted(true);

        subject.grantConsent();

        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.EXPLICIT_YES, true);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.EXPLICIT_YES);
    }

    @Test
    public void grantConsent_withNonWhitelistedApp_shouldTransitionToPotentialWhitelist() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setWhitelisted(false);

        subject.grantConsent();

        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.POTENTIAL_WHITELIST, false);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.POTENTIAL_WHITELIST);
    }

    @Test
    public void grantConsent_withDnt_shouldDoNothing() {
        when(mockAdvertisingId.isDoNotTrack()).thenReturn(true);

        subject.grantConsent();

        verifyZeroInteractions(mockConsentStatusChangeListener);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.UNKNOWN);
    }

    @Test
    public void revokeConsent_shouldSetConsentStatusToNo() {
        subject.revokeConsent();

        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.EXPLICIT_NO, false);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.EXPLICIT_NO);
    }

    @Test
    public void revokeConsent_withDnt_shouldDoNothing() {
        when(mockAdvertisingId.isDoNotTrack()).thenReturn(true);

        subject.revokeConsent();

        verifyZeroInteractions(mockConsentStatusChangeListener);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.UNKNOWN);
    }

    @Test
    public void changeConsentStateFromDialog_withYes_shouldChangeStatusToExplicitYes() {
        personalInfoData.setGdprApplies(true);

        subject.changeConsentStateFromDialog(ConsentStatus.EXPLICIT_YES);

        verify(mockRequestQueue).add(any(Request.class));
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.EXPLICIT_YES);
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo(
                ConsentChangeReason.GRANTED_BY_USER.getReason());
        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.EXPLICIT_YES, true);
    }

    @Test
    public void changeConsentStateFromDialog_withNo_shouldChangeStatusToExplicitNo() {
        personalInfoData.setGdprApplies(true);

        subject.changeConsentStateFromDialog(ConsentStatus.EXPLICIT_NO);

        verify(mockRequestQueue).add(any(Request.class));
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.EXPLICIT_NO);
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo(
                ConsentChangeReason.DENIED_BY_USER.getReason());
        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.EXPLICIT_NO, false);
    }

    @Test
    public void shouldMakeSyncRequest_withMostMajorCases_shouldReturnCorrectBoolean() {
        boolean actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false, null, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false, null, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false, null, DEFAULT_TIME_MS,
                "udid", false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false, null, DEFAULT_TIME_MS,
                "udid", true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, false,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, true, null, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, true, null, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, true, null, DEFAULT_TIME_MS,
                "udid", false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, true, null, DEFAULT_TIME_MS,
                "udid", true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, true,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, true,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, true,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, null, true,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, false, null, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, false, null, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, false, null, DEFAULT_TIME_MS,
                "udid", false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, false, null, DEFAULT_TIME_MS,
                "udid", true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, false,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, false,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, false,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, false,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, true, null, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, true, null, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, true, null, DEFAULT_TIME_MS,
                "udid", false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, true, null, DEFAULT_TIME_MS,
                "udid", true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, true,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, true,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, true,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, false, true,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, false, null, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, false, null, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, false, null, DEFAULT_TIME_MS,
                "udid", false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, false, null, DEFAULT_TIME_MS,
                "udid", true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, false,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, false,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, false,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, false,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, true, null, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, true, null, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, true, null, DEFAULT_TIME_MS,
                "udid", false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, true, null, DEFAULT_TIME_MS,
                "udid", true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, true,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, true,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, true,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(false, true, true,
                SystemClock.uptimeMillis() - SHORT_TIME_MS, DEFAULT_TIME_MS,
                null, true);
        assertThat(actual).isTrue();

        actual = PersonalInfoManager.shouldMakeSyncRequest(true, true, false,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();

        actual = PersonalInfoManager.shouldMakeSyncRequest(true, true, true,
                SystemClock.uptimeMillis() - LONG_TIME_MS, DEFAULT_TIME_MS,
                null, false);
        assertThat(actual).isFalse();
    }

    @Test
    public void requestSync_withSdkInitialized_withShouldMakeSyncRequestTrue_shouldAddSyncRequestToRequestQueue() {
        personalInfoData.setGdprApplies(true);

        subject.requestSync(true);

        verify(mockRequestQueue).add(any(SyncRequest.class));
    }

    @Test
    public void requestSync_withSdkNotInitialized_shouldDoNothing() {
        personalInfoData.setGdprApplies(true);
        when(MoPub.isSdkInitialized()).thenReturn(false);

        subject.requestSync(true);

        verifyZeroInteractions(mockRequestQueue);
    }

    @Test
    public void requestSyncNoParams_withSdkNotInitialized_shouldAddSyncRequestToRequestQueue() {
        personalInfoData.setGdprApplies(true);
        when(MoPub.isSdkInitialized()).thenReturn(false);

        subject.requestSync();

        verify(mockRequestQueue).add(any(SyncRequest.class));
    }

    @Test
    public void serverOverrideListener_onForceExplicitNo_withNullMessage_shouldChangeStatusToNo() {
        personalInfoData.setGdprApplies(true);

        subject.getServerOverrideListener().onForceExplicitNo(null);

        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.EXPLICIT_NO, false);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.EXPLICIT_NO);
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo(
                ConsentChangeReason.REVOKED_BY_SERVER.getReason());
    }

    @Test
    public void serverOverrideListener_onForceExplicitNo_withAMessage_shouldChangeStatusToNo() {
        personalInfoData.setGdprApplies(true);

        subject.getServerOverrideListener().onForceExplicitNo("message");

        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.EXPLICIT_NO, false);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.EXPLICIT_NO);
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("message");
    }

    @Test
    public void serverOverrideListener_onInvalidateConsent_withNullMessage_shouldChangeStatusToUnknown() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);

        subject.getServerOverrideListener().onInvalidateConsent(null);

        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.EXPLICIT_YES,
                ConsentStatus.UNKNOWN, false);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.UNKNOWN);
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo(
                ConsentChangeReason.REACQUIRE_BY_SERVER.getReason());
    }

    @Test
    public void serverOverrideListener_onInvalidateConsent_withAMessage_shouldChangeStatusToUnknown() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);

        subject.getServerOverrideListener().onInvalidateConsent("message");

        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.EXPLICIT_YES,
                ConsentStatus.UNKNOWN, false);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.UNKNOWN);
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("message");
    }

    @Test
    public void serverOverrideListener_onReacquireConsent_withNullMessage_shouldSetReacquireConsentFlag() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);

        subject.getServerOverrideListener().onReacquireConsent(null);

        verifyZeroInteractions(mockConsentStatusChangeListener);
        assertThat(personalInfoData.shouldReacquireConsent()).isTrue();
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.EXPLICIT_YES);
        assertThat(personalInfoData.getConsentChangeReason()).isNull();
    }

    @Test
    public void serverOverrideListener_onReacquireConsent_withAMessage_shouldSetReacquireConsentFlag_shouldSetConsentChangeReason() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);

        subject.getServerOverrideListener().onReacquireConsent("message");

        verifyZeroInteractions(mockConsentStatusChangeListener);
        assertThat(personalInfoData.shouldReacquireConsent()).isTrue();
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.EXPLICIT_YES);
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("message");
    }

    @Test
    public void serverOverrideListener_onForceGdprApplies_shouldForceGdprApplies() {
        personalInfoData.setGdprApplies(false);

        subject.getServerOverrideListener().onForceGdprApplies();

        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.UNKNOWN, false);
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.UNKNOWN);
        assertThat(personalInfoData.isForceGdprApplies()).isTrue();
        assertThat(subject.gdprApplies()).isTrue();
    }

    @Test
    public void attemptStateTransition_withSameConsentStatus_shouldDoNothing() {
        subject.attemptStateTransition(ConsentStatus.UNKNOWN, "no reason");

        verifyZeroInteractions(mockConsentStatusChangeListener);

        personalInfoData.setCurrentPrivacyPolicyVersion("2");
        personalInfoData.setConsentedPrivacyPolicyVersion("1");
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);
        subject.attemptStateTransition(ConsentStatus.EXPLICIT_YES, "no reason");
        verifyZeroInteractions(mockConsentStatusChangeListener);
        assertThat(personalInfoData.getConsentedPrivacyPolicyVersion()).isEqualTo("1");
    }

    @Test
    public void attemptStateTransition_withUnknownToYes_shouldSetConsentedVersions_shouldSetUdid() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setCurrentPrivacyPolicyVersion("1");
        personalInfoData.setCurrentVendorListVersion("2");
        personalInfoData.setCurrentVendorListIabFormat("3");

        subject.attemptStateTransition(ConsentStatus.EXPLICIT_YES, "reason");

        assertThat(personalInfoData.getConsentedPrivacyPolicyVersion()).isEqualTo("1");
        assertThat(personalInfoData.getConsentedVendorListVersion()).isEqualTo("2");
        assertThat(personalInfoData.getConsentedVendorListIabFormat()).isEqualTo("3");
        assertThat(personalInfoData.getUdid()).isEqualTo("udid");
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("reason");
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.EXPLICIT_YES);
        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.EXPLICIT_YES, true);
    }

    @Test
    public void attemptStateTransition_withPotentialWhitelistToYes_shouldNotSetConsentedVersions() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setConsentStatus(ConsentStatus.POTENTIAL_WHITELIST);
        personalInfoData.setCurrentPrivacyPolicyVersion("1");
        personalInfoData.setCurrentVendorListVersion("2");
        personalInfoData.setCurrentVendorListIabFormat("3");

        subject.attemptStateTransition(ConsentStatus.EXPLICIT_YES, "reason");

        assertThat(personalInfoData.getConsentedPrivacyPolicyVersion()).isEqualTo(null);
        assertThat(personalInfoData.getConsentedVendorListVersion()).isEqualTo(null);
        assertThat(personalInfoData.getConsentedVendorListIabFormat()).isEqualTo(null);
        assertThat(personalInfoData.getUdid()).isEqualTo("udid");
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("reason");
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.EXPLICIT_YES);
        verify(mockConsentStatusChangeListener).onConsentStateChange(
                ConsentStatus.POTENTIAL_WHITELIST,
                ConsentStatus.EXPLICIT_YES, true);
    }

    @Test
    public void attemptStateTransition_withYesToDnt_shouldClearPersonalDataExceptUdid() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);
        personalInfoData.setConsentedPrivacyPolicyVersion("1");
        personalInfoData.setConsentedVendorListVersion("2");
        personalInfoData.setConsentedVendorListIabFormat("3");
        personalInfoData.setUdid("udid");

        subject.attemptStateTransition(ConsentStatus.DNT, "reason");

        assertThat(personalInfoData.getConsentedPrivacyPolicyVersion()).isEqualTo(null);
        assertThat(personalInfoData.getConsentedVendorListVersion()).isEqualTo(null);
        assertThat(personalInfoData.getConsentedVendorListIabFormat()).isEqualTo(null);
        assertThat(personalInfoData.getUdid()).isEqualTo("udid");
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("reason");
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.DNT);
        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.EXPLICIT_YES,
                ConsentStatus.DNT, false);
    }

    @Test
    public void attemptStateTransition_withUnknownToDnt() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);
        personalInfoData.setCurrentPrivacyPolicyVersion("1");
        personalInfoData.setCurrentVendorListVersion("2");
        personalInfoData.setCurrentVendorListIabFormat("3");

        subject.attemptStateTransition(ConsentStatus.DNT, "reason");

        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("reason");
        assertThat(personalInfoData.getConsentedPrivacyPolicyVersion()).isEqualTo(null);
        assertThat(personalInfoData.getConsentedVendorListVersion()).isEqualTo(null);
        assertThat(personalInfoData.getConsentedVendorListIabFormat()).isEqualTo(null);
        assertThat(personalInfoData.getUdid()).isNull();
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.DNT);
        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.EXPLICIT_YES,
                ConsentStatus.DNT, false);
    }

    @Test
    public void attemptStateTransition_withYesToNo_shouldClearPersonalDataExceptUdid() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setConsentStatus(ConsentStatus.EXPLICIT_YES);
        personalInfoData.setConsentedPrivacyPolicyVersion("1");
        personalInfoData.setConsentedVendorListVersion("2");
        personalInfoData.setConsentedVendorListIabFormat("3");
        personalInfoData.setUdid("udid");

        subject.attemptStateTransition(ConsentStatus.EXPLICIT_NO, "reason");

        assertThat(personalInfoData.getConsentedPrivacyPolicyVersion()).isEqualTo(null);
        assertThat(personalInfoData.getConsentedVendorListVersion()).isEqualTo(null);
        assertThat(personalInfoData.getConsentedVendorListIabFormat()).isEqualTo(null);
        assertThat(personalInfoData.getUdid()).isEqualTo("udid");
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("reason");
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(ConsentStatus.EXPLICIT_NO);
        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.EXPLICIT_YES,
                ConsentStatus.EXPLICIT_NO, false);
    }

    @Test
    public void attemptStateTransition_withUnknownToPotentialWhitelist_shouldSetConsentedVersions_shouldSetUdid() {
        personalInfoData.setGdprApplies(true);
        personalInfoData.setCurrentPrivacyPolicyVersion("1");
        personalInfoData.setCurrentVendorListVersion("2");
        personalInfoData.setCurrentVendorListIabFormat("3");

        subject.attemptStateTransition(ConsentStatus.POTENTIAL_WHITELIST, "reason");

        assertThat(personalInfoData.getConsentedPrivacyPolicyVersion()).isEqualTo("1");
        assertThat(personalInfoData.getConsentedVendorListVersion()).isEqualTo("2");
        assertThat(personalInfoData.getConsentedVendorListIabFormat()).isEqualTo("3");
        assertThat(personalInfoData.getUdid()).isNull();
        assertThat(personalInfoData.getConsentChangeReason()).isEqualTo("reason");
        assertThat(subject.getPersonalInfoConsentStatus()).isEqualTo(
                ConsentStatus.POTENTIAL_WHITELIST);
        verify(mockConsentStatusChangeListener).onConsentStateChange(ConsentStatus.UNKNOWN,
                ConsentStatus.POTENTIAL_WHITELIST, false);
    }
}
