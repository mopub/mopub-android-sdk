package com.mopub.mobileads;

import android.app.Activity;
import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.webkit.WebView;

import com.mopub.common.MoPubBrowser;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class HtmlWebViewClientTest {

    private static final String CLICKTHROUGH_URL = "https://clickthrough";

    private HtmlWebViewClient subject;
    private HtmlWebViewListener htmlWebViewListener;
    private BaseHtmlWebView htmlWebView;
    private Context context;

    @Before
    public void setUp() throws Exception {
        htmlWebViewListener = mock(HtmlWebViewListener.class);
        htmlWebView = mock(BaseHtmlWebView.class);
        context = Robolectric.buildActivity(Activity.class).create().get().getApplicationContext();
        when(htmlWebView.getContext()).thenReturn(context);
        when(htmlWebView.wasClicked()).thenReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, CLICKTHROUGH_URL,
                "redirect", "dsp_creative_id");
        while(shadowOf((Application) context).getNextStartedActivity() != null) {}
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubFinishLoad_shouldCallAdDidLoad() throws Exception {
        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "mopub://finishLoad");

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onLoaded(eq(htmlWebView));
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubClose_shouldCallAdDidClose() throws Exception {
        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "mopub://close");

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onCollapsed();
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubFailLoad_shouldCallLoadFailUrl_shouldStopCurrentLoad() throws Exception {
        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "mopub://failLoad");

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onFailed(UNSPECIFIED);
        verify(htmlWebView).stopLoading();
    }

    @Test
    public void shouldOverrideUrlLoading_withPhoneIntent_shouldStartDefaultIntent() throws Exception {
        assertPhoneUrlStartedCorrectIntent("tel:");
        assertPhoneUrlStartedCorrectIntent("voicemail:");
        assertPhoneUrlStartedCorrectIntent("sms:");
        assertPhoneUrlStartedCorrectIntent("mailto:");
        assertPhoneUrlStartedCorrectIntent("geo:");
        assertPhoneUrlStartedCorrectIntent("google.streetview:");
    }

    @Test
    public void shouldOverrideUrlLoading_withCustomApplicationIntent_withUserClick_andCanHandleCustomIntent_shouldTryToLaunchCustomIntent() throws Exception {
        String customUrl = "myintent://something";
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);
        shadowOf(context.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(customUrl)), new ResolveInfo());

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, customUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();
        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent).isNotNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withCustomApplicationIntent_withoutUserClick_shouldNotTryToLaunchIntent() throws Exception {
        String customUrl = "myintent://something";
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, customUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withCustomApplicationIntent_withUserClick_butCanNotHandleCustomIntent_shouldFailSilently() throws Exception {
        String customUrl = "myintent://something";
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, customUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedIntent).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withHttpUrl_withUserClick_shouldOpenBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, "dsp_creative_id");
        String validUrl = "https://www.mopub.com";
        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, validUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();

        Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(validUrl);
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DSP_CREATIVE_ID)).isEqualTo("dsp_creative_id");
        assertThat(startedActivity.getData()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withHttpUrl_withoutUserClick_shouldNotOpenBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);
        String validUrl = "https://www.mopub.com";
        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, validUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withClickTrackingRedirect_withUserClick_shouldNotChangeUrl() throws Exception {
        String validUrl = "https://www.mopub.com";
        stub(htmlWebView.wasClicked()).toReturn(true);

        subject.shouldOverrideUrlLoading(htmlWebView, validUrl);

        Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(validUrl);
    }

    @Test
    public void shouldOverrideUrlLoading_withClickTrackingRedirect_withoutUserClick_shouldChangeUrl() throws Exception {
        String validUrl = "https://www.mopub.com";
        stub(htmlWebView.wasClicked()).toReturn(false);

        subject.shouldOverrideUrlLoading(htmlWebView, validUrl);

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withEmptyUrl_withUserClick_shouldFailSilently() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "");

        Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withEmptyUrl_withoutUserClick_shouldLoadAboutBlank() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "");

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_withUserClick_shouldStartIntentWithActionView() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        boolean shouldOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView,
                "mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.mopub.com");

        assertThat(shouldOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();
        verify(htmlWebView).onResetUserClick();
        Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo("android.intent.action.VIEW");
        assertThat(startedActivity.getData().toString()).isEqualTo("https://www.mopub.com");
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_withoutUserClick_shouldStartIntentWithActionView() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        boolean shouldOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView,
                "mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.mopub.com");

        assertThat(shouldOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_butOpaqueUri_withUserClick_shouldNotBeHandledByNativeBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        String opaqueNativeBrowserUriString = "mopubnativebrowser:navigate?url=https%3A%2F%2Fwww.mopub.com";
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        boolean shouldOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, opaqueNativeBrowserUriString);

        assertThat(shouldOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
        Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_butOpaqueUri_withoutUserClick_shouldNotLoad() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        String opaqueNativeBrowserUriString = "mopubnativebrowser:navigate?url=https%3A%2F%2Fwww.mopub.com";
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        boolean shouldOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, opaqueNativeBrowserUriString);

        assertThat(shouldOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_withInvalidHostSchemeUrl_withUserClick_shouldFailSilently() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        boolean shouldOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "something://blah?url=invalid");

        assertThat(shouldOverrideUrl).isTrue();
        Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity).isNull();

        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_withInvalidHostSchemeUrl_withoutUserClick_shouldNotInvokeNativeBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        boolean shouldOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "something://blah?url=invalid");

        assertThat(shouldOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withAboutBlankUrl_shouldFailSilently() {
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "about:blank");

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubFinishLoad_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("mopub://finishLoad");
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubClose_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("mopub://close");
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubFailLoad_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("mopub://fail");
    }

    @Test
    public void shouldOverrideUrlLoading_withAboutScheme_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("about:blank");
    }

    @Test
    public void shouldOverrideUrlLoading_withPhoneScheme_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("tel:");
    }

    @Test
    public void shouldOverrideUrlLoading_withMarketUrl_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("https://play.google.com/");
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserUrl_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("mopubnativebrowser://");
    }

    @Test
    public void shouldOverrideUrlLoading_withInAppBrowserUrl_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("https://twitter.com");
    }

    @Test
    public void shouldOverrideUrlLoading_withDeepLinkUrl_withoutUserClick_shouldDoNothing() throws Exception {
        assertNothingHappensWithoutClick("myapp://view");
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirect_withUserClick_shouldOpenInBrowser() throws Exception {
        String url = "redirectUrlToLoad";
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, "redirect", "dsp_creative_id");
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, url, null);

        verify(view).stopLoading();

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(url);
        assertThat(intent.getStringExtra(MoPubBrowser.DSP_CREATIVE_ID)).isEqualTo("dsp_creative_id");
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirect_withoutUserClick_shouldOpenInBrowser() throws Exception {
        String url = "redirectUrlToLoad";
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, "redirect", "dsp_creative_id");
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, url, null);

        verify(view).stopLoading();

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirectAndHasClickthrough_withUserClick_shouldNotChangeUrl_shouldOpenInBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        String url = "redirectUrlToLoad";
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, url, null);

        verify(view).stopLoading();

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(url);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirectAndHasClickthrough_withoutUserClick_shouldNotOpenInBrowser() throws Exception {
        when(htmlWebView.wasClicked()).thenReturn(false);
        String url = "redirectUrlToLoad";
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, url, null);

        verify(view).stopLoading();

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirectAndHasClickthrough_withUserClick_whenMoPubBrowserCannotHandleIntent_shouldFailSilently() throws Exception {
        Context mockContext = mock(Context.class);
        stub(htmlWebView.wasClicked()).toReturn(true);
        stub(htmlWebView.getContext()).toReturn(mockContext);
        String url = "redirectUrlToLoad";

        // We only want startActivity() to throw an exception the first time we call it.
        doThrow(new ActivityNotFoundException())
                .doNothing()
                .when(mockContext).startActivity(any(Intent.class));

        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, CLICKTHROUGH_URL, "redirect", "dsp_creative_id");
        subject.onPageStarted(htmlWebView, url, null);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext).startActivity(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertThat(intent.getData()).isNull();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
    }

    @Test
    public void onPageStarted_whenLoadedUrlDoesntStartWithRedirect_shouldDoNothing() throws Exception {
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, "this doesn't start with redirect", null);

        verify(view, never()).stopLoading();

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    private void assertPhoneUrlStartedCorrectIntent(String url) {
        boolean didOverrideUrl;

        stub(htmlWebView.wasClicked()).toReturn(true);
        didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, url);
        Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData().toString()).isEqualTo(url);
        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();
        verify(htmlWebView).onResetUserClick();
        reset(htmlWebViewListener);
        reset(htmlWebView);

        stub(htmlWebView.wasClicked()).toReturn(false);
        didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, url);
        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
        reset(htmlWebViewListener);
        reset(htmlWebView);
    }

    private void assertNothingHappensWithoutClick(final String url) {
        stub(htmlWebView.wasClicked()).toReturn(false);

        subject.shouldOverrideUrlLoading(htmlWebView, url);

        verify(htmlWebViewListener, never()).onClicked();
        verify(htmlWebView, never()).onResetUserClick();
        Intent startedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedActivity).isNull();
    }
}
