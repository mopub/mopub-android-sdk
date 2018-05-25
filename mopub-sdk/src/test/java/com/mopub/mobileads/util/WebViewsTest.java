package com.mopub.mobileads.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.webkit.CookieManager;
import android.webkit.WebView;

import com.mopub.TestSdkHelper;
import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.annotation.Config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
@PrepareForTest({MoPub.class, CookieManager.class})
public class WebViewsTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private CookieManager cookieManager;

    @Before
    public void setup() {
        mockStatic(MoPub.class);
        mockStatic(CookieManager.class);

        cookieManager = Mockito.mock(CookieManager.class);
        when(CookieManager.getInstance()).thenReturn(cookieManager);
    }

    @Test
    public void pause_withIsFinishingTrue_shouldStopLoading_shouldLoadBlankUrl_shouldPauseWebView() {
        WebView mockWebView = mock(WebView.class);

        WebViews.onPause(mockWebView, true);

        verify(mockWebView).stopLoading();
        verify(mockWebView).loadUrl("");
        verify(mockWebView).onPause();
    }

    @Test
    public void pause_withIsFinishingFalse_shouldPauseWebView() {
        WebView mockWebView = mock(WebView.class);

        WebViews.onPause(mockWebView, false);

        verify(mockWebView, never()).stopLoading();
        verify(mockWebView, never()).loadUrl("");
        verify(mockWebView).onPause();
    }

    @Test
    public void manageWebCookies_whenCanCollectPersonaInfoTrue_setsAcceptCookiesTrue() {
        when(MoPub.canCollectPersonalInformation()).thenReturn(true);

        WebViews.manageWebCookies();

        verify(cookieManager).setAcceptCookie(true);
        verifyStatic();
        CookieManager.setAcceptFileSchemeCookies(eq(true));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void manageWebCookies_api21orAbove_whenCanCollectPersonaInfoFalse_shouldRemoveCookies() {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.LOLLIPOP);
        when(MoPub.canCollectPersonalInformation()).thenReturn(false);

        WebViews.manageWebCookies();

        verify(cookieManager).setAcceptCookie(false);
        verify(cookieManager).removeSessionCookies(null);
        verify(cookieManager).removeAllCookies(null);
        verify(cookieManager).flush();
        verifyStatic();
        CookieManager.setAcceptFileSchemeCookies(eq(false));
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void manageWebCookies_api20orBelow_whenCanCollectPersonaInfoFalse_shouldRemoveCookies() {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.KITKAT);
        when(MoPub.canCollectPersonalInformation()).thenReturn(false);

        WebViews.manageWebCookies();

        verify(cookieManager).setAcceptCookie(false);
        verify(cookieManager).removeSessionCookie();
        verify(cookieManager).removeAllCookie();
        verifyStatic();
        CookieManager.setAcceptFileSchemeCookies(eq(false));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void manageThirdPartyCookies_api21orAbove_whenCanCollectPersonaInfoTrue_shouldAcceptCookies() {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.LOLLIPOP);
        when(MoPub.canCollectPersonalInformation()).thenReturn(true);
        WebView mockWebView = mock(WebView.class);

        WebViews.manageThirdPartyCookies(mockWebView);

        verify(cookieManager).setAcceptThirdPartyCookies(eq(mockWebView), eq(true));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Config(sdk = Build.VERSION_CODES.LOLLIPOP)
    @Test
    public void manageThirdPartyCookies_api21orAbove_whenCanCollectPersonaInfoFalse_shouldRemoveCookies() {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.LOLLIPOP);
        when(MoPub.canCollectPersonalInformation()).thenReturn(false);
        WebView mockWebView = mock(WebView.class);

        WebViews.manageThirdPartyCookies(mockWebView);

        verify(cookieManager).setAcceptThirdPartyCookies(eq(mockWebView), eq(false));
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Config(sdk = Build.VERSION_CODES.KITKAT)
    @Test
    public void manageThirdPartyCookies_api20orBelow_whenCanCollectPersonaInfoFalse_shouldDoNothing() {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.KITKAT);
        when(MoPub.canCollectPersonalInformation()).thenReturn(false);
        WebView mockWebView = mock(WebView.class);

        WebViews.manageThirdPartyCookies(mockWebView);

        verify(cookieManager, never()).setAcceptThirdPartyCookies(any(WebView.class), anyBoolean());
    }
}
