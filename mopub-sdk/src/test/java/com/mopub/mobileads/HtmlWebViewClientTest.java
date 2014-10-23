package com.mopub.mobileads;

import android.app.Activity;
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

import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class HtmlWebViewClientTest {

    private HtmlWebViewClient subject;
    private HtmlWebViewListener htmlWebViewListener;
    private BaseHtmlWebView htmlWebView;

    @Before
    public void setUp() throws Exception {
        htmlWebViewListener = mock(HtmlWebViewListener.class);
        htmlWebView = mock(BaseHtmlWebView.class);
        stub(htmlWebView.getContext()).toReturn(new Activity());
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, "clickthrough", "redirect");
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
    public void shouldOverrideUrlLoading_withMoPubFailLoad_shouldCallLoadFailUrl() throws Exception {
        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "mopub://failLoad");

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onFailed(UNSPECIFIED);
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubCustom_withUserClick_shouldStartCustomIntent() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "mopub://custom?fnc=myFnc&data=myData");

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();
        Intent startedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedActivity).isNotNull();
        assertThat(startedActivity.getAction()).isEqualTo("myFnc");
        assertThat(startedActivity.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(startedActivity.getStringExtra(HtmlBannerWebView.EXTRA_AD_CLICK_DATA)).isEqualTo("myData");
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubCustom_withoutUserClick_shouldNotStartActivity() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "mopub://custom?fnc=myFnc&data=myData");

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        Intent startedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedActivity).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubCustomAndNullData_withUserClick_shouldStartCustomIntent() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "mopub://custom?fnc=myFnc");

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();
        Intent startedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedActivity).isNotNull();
        assertThat(startedActivity.getAction()).isEqualTo("myFnc");
        assertThat(startedActivity.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(startedActivity.getStringExtra(HtmlBannerWebView.EXTRA_AD_CLICK_DATA)).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withMoPubCustomAndNullData_withoutUserClick_shouldNotStartCustomIntent() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, "mopub://custom?fnc=myFnc");

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        Intent startedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedActivity).isNull();
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
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);
        Robolectric.packageManager.addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(customUrl)), new ResolveInfo());

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, customUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();
        assertActivityStarted();
    }

    @Test
    public void shouldOverrideUrlLoading_withCustomApplicationIntent_withoutUserClick_shouldNotTryToLaunchIntent() throws Exception {
        String customUrl = "myintent://something";
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, customUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withCustomApplicationIntent_withUserClick_butCanNotHandleCustomIntent_shouldDefaultToMoPubBrowser() throws Exception {
        String customUrl = "myintent://something";
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, customUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();
        Intent startedIntent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
    }

    @Test
    public void shouldOverrideUrlLoading_withHttpUrl_withUserClick_shouldOpenBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);
        String validUrl = "http://www.mopub.com";
        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, validUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();

        Intent startedActivity = assertActivityStarted();
        assertThat(startedActivity.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(validUrl);
        assertThat(startedActivity.getData()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withHttpUrl_withoutUserClick_shouldNotOpenBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);
        String validUrl = "http://www.mopub.com";
        boolean didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, validUrl);

        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withClickTrackingRedirect_withUserClick_shouldNotChangeUrl() throws Exception {
        String validUrl = "http://www.mopub.com";
        stub(htmlWebView.wasClicked()).toReturn(true);

        subject.shouldOverrideUrlLoading(htmlWebView, validUrl);

        Intent startedActivity = assertActivityStarted();
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(validUrl);
    }

    @Test
    public void shouldOverrideUrlLoading_withClickTrackingRedirect_withoutUserClick_shouldChangeUrl() throws Exception {
        String validUrl = "http://www.mopub.com";
        stub(htmlWebView.wasClicked()).toReturn(false);

        subject.shouldOverrideUrlLoading(htmlWebView, validUrl);

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withEmptyUrl_withUserClick_shouldLoadAboutBlank() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "");

        Intent startedActivity = assertActivityStarted();
        assertThat(startedActivity.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo("about:blank");
        assertThat(startedActivity.getData()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withEmptyUrl_withoutUserClick_shouldLoadAboutBlank() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "");

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_withUserClick_shouldStartIntentWithActionView() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "mopubnativebrowser://navigate?url=http://mopub.com");

        Intent startedActivity = assertActivityStarted();
        assertThat(isWebsiteUrl(startedActivity.getData().toString()));
        assertThat(startedActivity.getAction()).isEqualTo("android.intent.action.VIEW");
        verify(htmlWebViewListener).onClicked();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_withoutUserClick_shouldStartIntentWithActionView() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "mopubnativebrowser://navigate?url=http://mopub.com");

        verify(htmlWebViewListener, never()).onClicked();
        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_butOpaqueUri_withUserClick_shouldNotBeHandledByNativeBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        String opaqueNativeBrowserUriString = "mopubnativebrowser:navigate?url=http://mopub.com";
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, opaqueNativeBrowserUriString);

        Intent startedActivity = assertActivityStarted();
        assertThat(startedActivity.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
        assertThat(startedActivity.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(opaqueNativeBrowserUriString);
        assertThat(startedActivity.getData()).isNull();
        verify(htmlWebViewListener).onClicked();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_butOpaqueUri_withoutUserClick_shouldNotLoad() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        String opaqueNativeBrowserUriString = "mopubnativebrowser:navigate?url=http://mopub.com";
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, opaqueNativeBrowserUriString);

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_withInvalidHostSchemeUrl_withUserClick_shouldNotInvokeNativeBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "something://blah?url=invalid");

        Intent startedActivity = assertActivityStarted();
        assertThat(startedActivity.getAction()).isNotEqualTo("android.intent.action.VIEW");
        verify(htmlWebViewListener).onClicked();
    }

    @Test
    public void shouldOverrideUrlLoading_withNativeBrowserScheme_withInvalidHostSchemeUrl_withoutUserClick_shouldNotInvokeNativeBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, null);

        subject.shouldOverrideUrlLoading(htmlWebView, "something://blah?url=invalid");

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    private boolean isWebsiteUrl(String url){
        return url.startsWith("http://") || url.startsWith("https://");
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirect_withUserClick_shouldOpenInBrowser() throws Exception {
        String url = "redirectUrlToLoad";
        stub(htmlWebView.wasClicked()).toReturn(true);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, "redirect");
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, url, null);

        verify(view).stopLoading();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(url);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirect_withoutUserClick_shouldOpenInBrowser() throws Exception {
        String url = "redirectUrlToLoad";
        stub(htmlWebView.wasClicked()).toReturn(false);
        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, null, "redirect");
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, url, null);

        verify(view).stopLoading();

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirectAndHasClickthrough_withUserClick_shouldNotChangeUrl_shouldOpenInBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        String url = "redirectUrlToLoad";
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, url, null);

        verify(view).stopLoading();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(intent.getStringExtra(MoPubBrowser.DESTINATION_URL_KEY)).isEqualTo(url);
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(intent.getComponent().getClassName()).isEqualTo("com.mopub.common.MoPubBrowser");
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirectAndHasClickthrough_withoutUserClick_shouldNotOpenInBrowser() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        String url = "redirectUrlToLoad";
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, url, null);

        verify(view).stopLoading();

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void onPageStarted_whenLoadedUrlStartsWithRedirectAndHasClickthrough_withUserClick_whenMoPubBrowserCannotHandleIntent_shouldOpenInNativeBrowser() throws Exception {
        Context mockContext = mock(Context.class);
        stub(htmlWebView.wasClicked()).toReturn(true);
        stub(htmlWebView.getContext()).toReturn(mockContext);
        String url = "redirectUrlToLoad";

        // We only want startActivity() to throw an exception the first time we call it.
        doThrow(new ActivityNotFoundException())
                .doNothing()
                .when(mockContext).startActivity(any(Intent.class));

        subject = new HtmlWebViewClient(htmlWebViewListener, htmlWebView, "clickthrough", "redirect");
        subject.onPageStarted(htmlWebView, url, null);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(mockContext, times(2)).startActivity(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getAllValues().get(1);
        assertThat(intent.getAction()).isEqualTo("android.intent.action.VIEW");
        assertThat(intent.getData().toString()).isEqualTo("about:blank");
        assertThat(intent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        verify(htmlWebViewListener, times(1)).onClicked();
    }

    @Test
    public void onPageStarted_whenLoadedUrlDoesntStartWithRedirect_shouldDoNothing() throws Exception {
        WebView view = mock(WebView.class);
        subject.onPageStarted(view, "this doesn't start with redirect", null);

        verify(view, never()).stopLoading();

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
    }

    @Test
    public void launchIntentForUserClick_shouldStartActivityAndResetClickStatusAndReturnTrue() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        Context context = mock(Context.class);
        Intent intent = mock(Intent.class);

        boolean result = subject.launchIntentForUserClick(context, intent, null);

        verify(context).startActivity(eq(intent));
        verify(htmlWebView).onResetUserClick();
        assertThat(result).isTrue();
    }

    @Test
    public void launchIntentForUserClick_whenUserHasNotClicked_shouldNotStartActivityAndReturnFalse() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(false);
        Context context = mock(Context.class);
        Intent intent = mock(Intent.class);

        boolean result = subject.launchIntentForUserClick(context, intent, null);

        verify(context, never()).startActivity(any(Intent.class));
        verify(htmlWebView, never()).onResetUserClick();
        assertThat(result).isFalse();
    }

    @Test
    public void launchIntentForUserClick_whenNoMatchingActivity_shouldNotStartActivityAndReturnFalse() throws Exception {
        Context context = mock(Context.class);
        Intent intent = mock(Intent.class);

        stub(htmlWebView.wasClicked()).toReturn(true);
        doThrow(new ActivityNotFoundException()).when(context).startActivity(any(Intent.class));

        boolean result = subject.launchIntentForUserClick(context, intent, null);

        verify(htmlWebView, never()).onResetUserClick();
        assertThat(result).isFalse();
    }

    @Test
    public void launchIntentForUserClick_whenContextIsNull_shouldNotStartActivityAndReturnFalse() throws Exception {
        stub(htmlWebView.wasClicked()).toReturn(true);
        Intent intent = new Intent();

        boolean result = subject.launchIntentForUserClick(null, intent, null);

        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
        verify(htmlWebView, never()).onResetUserClick();
        assertThat(result).isFalse();
    }

    private void assertPhoneUrlStartedCorrectIntent(String url) {
        boolean didOverrideUrl;

        stub(htmlWebView.wasClicked()).toReturn(true);
        didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, url);
        Intent startedActivity = assertActivityStarted();
        assertThat(startedActivity.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(startedActivity.getData().toString()).isEqualTo(url);
        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener).onClicked();
        reset(htmlWebViewListener);

        stub(htmlWebView.wasClicked()).toReturn(false);
        didOverrideUrl = subject.shouldOverrideUrlLoading(htmlWebView, url);
        assertThat(Robolectric.getShadowApplication().getNextStartedActivity()).isNull();
        assertThat(didOverrideUrl).isTrue();
        verify(htmlWebViewListener, never()).onClicked();
        reset(htmlWebViewListener);
    }

    private Intent assertActivityStarted() {
        Intent startedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedActivity).isNotNull();
        assertThat(startedActivity.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        return startedActivity;
    }
}
