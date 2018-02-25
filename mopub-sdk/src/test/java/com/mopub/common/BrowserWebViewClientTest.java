package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.WebView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class BrowserWebViewClientTest {

    private final WebView MOOT_WEB_VIEW = null;

    private BrowserWebViewClient subject;
    private Context context;
    private MoPubBrowser mockMoPubBrowser;
    private WebView mockWebView;

    @Before
    public void setUp() {
        mockMoPubBrowser = mock(MoPubBrowser.class);
        context = Robolectric.buildActivity(Activity.class).create().get().getApplicationContext();
        mockWebView = mock(WebView.class);

        doCallRealMethod().when(mockMoPubBrowser).setWebView(mockWebView);
        doCallRealMethod().when(mockMoPubBrowser).getWebView();
        when(mockMoPubBrowser.getApplicationContext()).thenReturn(context);

        mockMoPubBrowser.setWebView(mockWebView);
        subject = new BrowserWebViewClient(mockMoPubBrowser);
        Networking.setUserAgentForTesting("user_agent");
    }

    @Test
    public void shouldOverrideUrlLoading_withHTTPUrl_shouldReturnTrue_shouldLoadUrl() {
        final String url = "https://twitter.com";

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isTrue();
        verify(mockWebView).loadUrl(url);
        verify(mockMoPubBrowser, never()).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withTelUrl_shouldReturnTrue_shouldFinish() {
        final String url = "tel:123456789";

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isTrue();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withDeeplinkUrl_shouldReturnTrue_shouldFinish() {
        final String url = "twitter://timeline";

        makeDeeplinkResolvable(url);

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isTrue();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withDeeplinkPlusUrl_withSuccessfulPrimaryUrl_shouldReturnTrue_shouldFinish() {
        final String primaryUrl = "twitter://timeline";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl);

        makeDeeplinkResolvable(primaryUrl);

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isTrue();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withDeeplinkPlusUrl_withFailedPrimaryUrl_withHTTPFallbackUrl_shouldReturnTrue_shouldLoadFallbackUrl_shouldNotFinish() {
        final String primaryUrl = "missingApp://somePath";
        final String fallbackUrl = "https://twitter.com/";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl);

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isTrue();
        verify(mockWebView).loadUrl(fallbackUrl);
        verify(mockMoPubBrowser, never()).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withDeeplinkPlusUrl_withFailedPrimaryUrl_withTelFallbackUrl_shouldReturnTrue_shouldFinish() {
        final String primaryUrl = "missingApp://somePath";
        final String fallbackUrl = "tel:123456789";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl);

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isTrue();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser, times(1)).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withDeeplinkPlusUrl_withFailedPrimaryUrl_withDeeplinkFallbackUrl_shouldReturnTrue_shouldFinish() {
        final String primaryUrl = "missingApp://somePath";
        final String fallbackUrl = "twitter://timeline";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl);

        makeDeeplinkResolvable(fallbackUrl);

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isTrue();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser, times(1)).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withDeeplinkPlusUrl_withEncodedQueryString_shouldReturnTrue_shouldFinish() {
        final String primaryUrl = "ebay://launch?nav=home&referrer=https%3A%2F%2Frover.ebay.com%2Frover%2F1%2F711-212056-53654-1%2F4%3Fmpt%3Dcache_buster%26ff6%3Dclick_id%26ff7%3Difa%26ff9%3Dsegment_name%26ff18%3Dcreative_name%26siteid%3D0%26ipn%3Dadmain2%26placement%3D418737%26ck%3D23932_main%26mpvc%3D";
        final String fallbackUrl = "https://ebay.com";
        final String url = "deeplink+://navigate?primaryUrl=" + Uri.encode(primaryUrl)
                + "&fallbackUrl=" + Uri.encode(fallbackUrl);

        makeDeeplinkResolvable(primaryUrl);

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isTrue();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser, times(1)).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withEmptyUrl_shouldReturnFalse_shouldDoNothing() {
        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, "")).isFalse();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser, never()).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withNullUrl_shouldReturnFalse_shouldDoNothing() {
        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, (String) null)).isFalse();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser, never()).finish();
    }

    @Test
    public void shouldOverrideUrlLoading_withoutMatchingSupporedUrl_shouldReturnFalse_shouldDoNothing() {
        final String url = "mopubscheme://close";
        subject = new BrowserWebViewClient(mockMoPubBrowser);

        assertThat(subject.shouldOverrideUrlLoading(MOOT_WEB_VIEW, url)).isFalse();
        verify(mockWebView, never()).loadUrl(anyString());
        verify(mockMoPubBrowser, never()).finish();
    }

    private void makeDeeplinkResolvable(String deeplink) {
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse(deeplink)), new ResolveInfo());
    }
}
