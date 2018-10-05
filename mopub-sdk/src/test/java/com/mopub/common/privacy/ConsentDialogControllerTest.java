// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.mopub.common.Constants;
import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Intents;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.MoPubRequestQueueTest;
import com.mopub.network.Networking;
import com.mopub.volley.VolleyError;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static com.mopub.network.MoPubNetworkError.Reason.BAD_BODY;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*", "org.json.*"})
@PrepareForTest({Networking.class, Intents.class})
public class ConsentDialogControllerTest {
    private static final String AD_UNIT_ID = "ad_unit_id";
    private static final String HTML_TEXT = "html_text";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private ConsentDialogResponse dialogResponse;
    private ConsentDialogController subject;
    private PersonalInfoData personalInfoData;

    // mock objects
    private MoPubRequestQueue mockRequestQueue;
    private ConsentDialogListener mockDialogListener;

    @Before
    public void setup() {
        Activity activity = Robolectric.buildActivity(Activity.class).get();
        Context context = activity.getApplicationContext();
        mockRequestQueue = Mockito.mock(MoPubRequestQueueTest.TestMoPubRequestQueue.class);
        mockDialogListener = Mockito.mock(ConsentDialogListener.class);
        dialogResponse = new ConsentDialogResponse(HTML_TEXT);
        personalInfoData = new PersonalInfoData(context, AD_UNIT_ID);

        PowerMockito.mockStatic(Networking.class);
        when(Networking.getRequestQueue(context)).thenReturn(mockRequestQueue);
        when(Networking.getScheme()).thenReturn(Constants.HTTPS);

        PowerMockito.mockStatic(Intents.class);

        subject = new ConsentDialogController(context);
    }

    @Test
    public void loadConsentDialog_whenReadyIsFalse_whenRequestInFlightIsFalse_shouldAddRequestToNetworkQueue() {
        ArgumentCaptor<ConsentDialogRequest> requestCaptor = ArgumentCaptor.forClass(ConsentDialogRequest.class);

        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);

        assertThat(subject.mRequestInFlight).isTrue();
        verify(mockDialogListener, never()).onConsentDialogLoaded();
        verify(mockRequestQueue).add(requestCaptor.capture());
        ConsentDialogRequest request = requestCaptor.getValue();
        String originalUrl = request.getOriginalUrl();
        String minUrl = generateTestUrl();
        assertThat(minUrl).isEqualTo(originalUrl);
    }

    @Test
    public void loadConsentDialog_whenReadyIsTrue_whenRequestInFlightIsFalse_shouldNotAddRequestToNetworkQueue() {
        subject.mReady = true;

        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);

        assertThat(subject.mRequestInFlight).isFalse();
        verify(mockDialogListener).onConsentDialogLoaded(); // should call listener immediately
        verify(mockRequestQueue, never()).add(any(ConsentDialogRequest.class));
    }

    @Test
    public void loadConsentDialog_whenReadyIsTrue_withListenerNotSet_shouldNotCrash() {
        subject.mReady = true;

        subject.loadConsentDialog(null, true, personalInfoData);

        assertThat(subject.mRequestInFlight).isFalse();
        verify(mockRequestQueue, never()).add(any(ConsentDialogRequest.class));
    }

    @Test
    public void loadConsentDialog_whenRequestInFlightIsTrue_shouldNotCreateNewRequest_shouldNotCallListener() {
        subject.mRequestInFlight = true;

        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);

        verify(mockDialogListener, never()).onConsentDialogLoaded();
        verify(mockRequestQueue, never()).add(any(ConsentDialogRequest.class));
    }

    @Test
    public void onSuccess_withValidResponse_shouldCallConsentDialogLoaded() {
        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);
        subject.onSuccess(dialogResponse);

        assertThat(subject.mReady).isTrue();
        assertThat(subject.mRequestInFlight).isFalse();
        verify(mockDialogListener).onConsentDialogLoaded();
        verify(mockDialogListener, never()).onConsentDialogLoadFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void onSuccess_withEmptyResponse_shouldNotCallConsentDialogLoaded() {
        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);
        subject.onSuccess(new ConsentDialogResponse(""));

        assertThat(subject.mReady).isFalse();
        assertThat(subject.mRequestInFlight).isFalse();
        verify(mockDialogListener, never()).onConsentDialogLoaded();
        verify(mockDialogListener).onConsentDialogLoadFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void onErrorResponse_shouldResetState_shouldCallDialogFailed() {
        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);
        subject.onErrorResponse(new VolleyError());

        assertThat(subject.mReady).isFalse();
        assertThat(subject.mRequestInFlight).isFalse();
        verify(mockDialogListener, never()).onConsentDialogLoaded();
        verify(mockDialogListener).onConsentDialogLoadFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void onErrorResponse_withErrorBadBody_shouldResetState_shouldCallDialogFailed() {
        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);
        subject.onErrorResponse(new MoPubNetworkError(BAD_BODY));

        assertThat(subject.mReady).isFalse();
        assertThat(subject.mRequestInFlight).isFalse();
        verify(mockDialogListener, never()).onConsentDialogLoaded();
        verify(mockDialogListener).onConsentDialogLoadFailed(MoPubErrorCode.INTERNAL_ERROR);
    }

    @Test
    public void showConsentDialog_whenDataIsReady_shouldStartActivity_shouldResetControllerState() throws Exception {
        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);
        subject.onSuccess(dialogResponse);

        subject.showConsentDialog();

        assertThat(subject.mReady).isFalse();
        assertThat(subject.mRequestInFlight).isFalse();
        verifyStatic();
        Intents.startActivity(any(Context.class), any(Intent.class));
    }

    @Test
    public void showConsentDialog_whenDataIsNotReady_shouldNotStartActivity() throws Exception {
        subject.loadConsentDialog(mockDialogListener, true, personalInfoData);
        subject.onErrorResponse(new MoPubNetworkError(BAD_BODY));

        subject.showConsentDialog();

        assertThat(subject.mReady).isFalse();
        assertThat(subject.mRequestInFlight).isFalse();
        verifyStatic(never());
        Intents.startActivity(any(Context.class), any(Intent.class));
    }

    // test utils
    private String generateTestUrl() {
        return "https://" + Constants.HOST + "/m/gdpr_consent_dialog" +
                "?id=" + Uri.encode(AD_UNIT_ID) +
                "&current_consent_status=unknown" +
                "&nv=" + Uri.encode(MoPub.SDK_VERSION) +
                "&language=en" +
                "&gdpr_applies=1" +
                "&force_gdpr_applies=0" +
                "&bundle=com.mopub.mobileads";
    }
}
