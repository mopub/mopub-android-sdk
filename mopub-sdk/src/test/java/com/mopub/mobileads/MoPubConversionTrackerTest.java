package com.mopub.mobileads;

import android.app.Activity;
import android.content.SharedPreferences;

import com.mopub.common.MoPub;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.privacy.ConsentData;
import com.mopub.common.privacy.ConsentStatus;
import com.mopub.common.privacy.PersonalInfoManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.network.TrackingRequest;
import com.mopub.volley.VolleyError;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubConversionTrackerTest {
    private MoPubConversionTracker subject;
    private Activity context;
    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Captor
    private ArgumentCaptor<TrackingRequest> requestCaptor;

    private static final String TEST_UDID = "20b013c721c";
    private PersonalInfoManager mockPersonalInfoManager;
    private SharedPreferences mSharedPreferences;
    private String mPackageName;
    private String mWantToTrack;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        Networking.setRequestQueueForTesting(mockRequestQueue);

        mockPersonalInfoManager = mock(PersonalInfoManager.class);
        final ConsentData mockConsentData = mock(ConsentData.class);
        when(mockPersonalInfoManager.getConsentData()).thenReturn(mockConsentData);
        when(mockConsentData.getConsentedPrivacyPolicyVersion()).thenReturn("privacy_policy_version");
        when(mockConsentData.getConsentedVendorListVersion()).thenReturn("vendor_list_version");
        when(mockPersonalInfoManager.gdprApplies()).thenReturn(true);

        subject = new MoPubConversionTracker(context);
    }

    @Test
    public void reportAppOpen_Twice_shouldCallOnlyOnce() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        when(mockPersonalInfoManager.getPersonalInfoConsentStatus()).thenReturn(ConsentStatus.EXPLICIT_YES);

        prepareMoPub_getPersonalInfoManager();

        subject.reportAppOpen(false);
        verify(mockRequestQueue).add(requestCaptor.capture());

        reset(mockRequestQueue);
        requestCaptor.getValue().deliverResponse(null);

        subject.reportAppOpen(false);
        verify(mockRequestQueue, never()).add(any(TrackingRequest.class));
    }

    @Test
    public void reportAppOpen_fails_shouldCallAgain() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        when(mockPersonalInfoManager.getPersonalInfoConsentStatus()).thenReturn(ConsentStatus.EXPLICIT_YES);

        prepareMoPub_getPersonalInfoManager();

        subject.reportAppOpen(true);
        verify(mockRequestQueue).add(requestCaptor.capture());

        reset(mockRequestQueue);
        requestCaptor.getValue().deliverError(new VolleyError());

        subject.reportAppOpen(true);
        verify(mockRequestQueue).add(any(TrackingRequest.class));
    }

    @Test
    public void reportAppOpen_shouldNotTrackIfConsentIsFalse() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);
        prepareMoPub_getPersonalInfoManager();

        subject.reportAppOpen(false);
        mPackageName = context.getPackageName();
        mWantToTrack = mPackageName + " wantToTrack";
        mSharedPreferences = SharedPreferencesHelper.getSharedPreferences(context);
        assertThat(mSharedPreferences.getBoolean(mWantToTrack, false)).isTrue();
    }

    @Test
    public void reportAppOpen_shouldTrackWhenConsentIsFalse() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(false);
        prepareMoPub_getPersonalInfoManager();

        assertThat(subject.shouldTrack()).isFalse();
    }

    @Test
    public void reportAppOpen_shouldTrackWhenConsentIsTrueAndWantToTrack() throws Exception {
        when(mockPersonalInfoManager.canCollectPersonalInformation()).thenReturn(true);
        prepareMoPub_getPersonalInfoManager();

        mPackageName = context.getPackageName();
        mWantToTrack = mPackageName + " wantToTrack";
        mSharedPreferences = SharedPreferencesHelper.getSharedPreferences(context);
        mSharedPreferences
                .edit()
                .putBoolean(mWantToTrack, true)
                .commit();

        assertThat(subject.shouldTrack()).isTrue();
    }

    private void prepareMoPub_getPersonalInfoManager() throws Exception {
        new Reflection.MethodBuilder(null, "setPersonalInfoManager")
                .setStatic(MoPub.class)
                .setAccessible()
                .addParam(PersonalInfoManager.class, mockPersonalInfoManager)
                .execute();
    }
}

