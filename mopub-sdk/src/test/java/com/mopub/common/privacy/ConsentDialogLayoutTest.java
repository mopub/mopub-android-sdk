package com.mopub.common.privacy;

import android.app.Activity;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class ConsentDialogLayoutTest {

    @Mock
    private ConsentDialogLayout.ConsentListener consentListener;
    @Mock
    private ConsentDialogLayout.PageLoadListener pageLoadListener;
    @Mock
    private WebView mockWebView;

    private ConsentDialogLayout subject;
    private WebViewClient webViewClient;

    @Before
    public void setUp() throws Exception {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        subject = new ConsentDialogLayout(activity);
        Field webClientField = Reflection.getPrivateField(ConsentDialogLayout.class, "webViewClient");
        webViewClient = (WebViewClient) webClientField.get(subject);
    }

    @Test
    public void webViewClient_shouldOverrideUrlLoading_withConsentYes_shouldCallConsentClick() {
        subject.setConsentClickListener(consentListener);

        webViewClient.shouldOverrideUrlLoading(mockWebView, ConsentDialogLayout.URL_CONSENT_YES);

        verify(consentListener).onConsentClick(ConsentStatus.EXPLICIT_YES);
        verify(consentListener, never()).onCloseClick();
    }

    @Test
    public void webViewClient_shouldOverrideUrlLoading_withConsentNo_shouldCallConsentClick() {
        subject.setConsentClickListener(consentListener);

        webViewClient.shouldOverrideUrlLoading(mockWebView, ConsentDialogLayout.URL_CONSENT_NO);

        verify(consentListener).onConsentClick(ConsentStatus.EXPLICIT_NO);
        verify(consentListener, never()).onCloseClick();
    }

    @Test
    public void webViewClient_shouldOverrideUrlLoading_closeClick_shouldCallCloseClick() {
        subject.setConsentClickListener(consentListener);

        webViewClient.shouldOverrideUrlLoading(mockWebView, ConsentDialogLayout.URL_CLOSE);

        verify(consentListener, never()).onConsentClick(any(ConsentStatus.class));
        verify(consentListener).onCloseClick();
    }

    @Test
    public void webViewClient_shouldOverrideUrlLoading_withAnyOtherUrls_shouldNotCallListener() {
        subject.setConsentClickListener(consentListener);

        webViewClient.shouldOverrideUrlLoading(mockWebView, "some other url");

        verify(consentListener, never()).onConsentClick(any(ConsentStatus.class));
        verify(consentListener, never()).onCloseClick();
    }

    @Test
    public void webViewClient_onPageFinished_shouldCall_onLoadProgress() {
        subject.startLoading("html_page", pageLoadListener);

        webViewClient.onPageFinished(mockWebView, "some_url");

        verify(pageLoadListener).onLoadProgress(ConsentDialogLayout.FINISHED_LOADING);
    }
}
