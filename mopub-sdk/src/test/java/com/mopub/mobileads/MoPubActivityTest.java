package com.mopub.mobileads;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestHtmlInterstitialWebViewFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLocalBroadcastManager;

import static com.mopub.mobileads.AdFetcher.CLICKTHROUGH_URL_KEY;
import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.AdFetcher.REDIRECT_URL_KEY;
import static com.mopub.mobileads.AdFetcher.SCROLLABLE_KEY;
import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.getHtmlInterstitialIntentFilter;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.HtmlInterstitialWebView.MoPubUriJavascriptFireFinishLoadListener;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
public class MoPubActivityTest extends BaseInterstitialActivityTest {
    public static final String EXPECTED_HTML_DATA = "htmlData";
    public static final boolean EXPECTED_IS_SCROLLABLE = true;
    public static final String EXPECTED_REDIRECT_URL = "redirectUrl";
    public static final String EXPECTED_CLICKTHROUGH_URL = "http://expected_url";

    private HtmlInterstitialWebView htmlInterstitialWebView;
    private Activity context;
    private CustomEventInterstitialListener customEventInterstitialListener;

    @Before
    public void setUp() throws Exception {
        super.setup();

        adConfiguration = mock(AdConfiguration.class, withSettings().serializable());
        stub(adConfiguration.getBroadcastIdentifier()).toReturn(testBroadcastIdentifier);

        Intent moPubActivityIntent = createMoPubActivityIntent(EXPECTED_HTML_DATA, EXPECTED_IS_SCROLLABLE, EXPECTED_REDIRECT_URL, EXPECTED_CLICKTHROUGH_URL, adConfiguration);
        htmlInterstitialWebView = TestHtmlInterstitialWebViewFactory.getSingletonMock();
        resetMockedView(htmlInterstitialWebView);
        subject = Robolectric.buildActivity(MoPubActivity.class).withIntent(moPubActivityIntent).create().get();

        context = new Activity();
        customEventInterstitialListener = mock(CustomEventInterstitialListener.class);

        reset(htmlInterstitialWebView);
        resetMockedView(htmlInterstitialWebView);
    }

    @Test
    public void preRenderHtml_shouldPreloadTheHtml() throws Exception {
        String htmlData = "this is nonsense";
        MoPubActivity.preRenderHtml(context, customEventInterstitialListener, htmlData);

        verify(htmlInterstitialWebView).enablePlugins(eq(false));
        verify(htmlInterstitialWebView).addMoPubUriJavascriptInterface(any(MoPubUriJavascriptFireFinishLoadListener.class));
        verify(htmlInterstitialWebView).loadHtmlResponse(htmlData);
    }

    @Test
    public void preRenderHtml_shouldHaveAWebViewClientThatForwardsFinishLoad() throws Exception {
        MoPubActivity.preRenderHtml(context, customEventInterstitialListener, null);

        ArgumentCaptor<WebViewClient> webViewClientCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(htmlInterstitialWebView).setWebViewClient(webViewClientCaptor.capture());
        WebViewClient webViewClient = webViewClientCaptor.getValue();

        webViewClient.shouldOverrideUrlLoading(null, "mopub://finishLoad");

        verify(customEventInterstitialListener).onInterstitialLoaded();
        verify(customEventInterstitialListener, never()).onInterstitialFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void preRenderHtml_shouldHaveAWebViewClientThatForwardsFailLoad() throws Exception {
        MoPubActivity.preRenderHtml(context, customEventInterstitialListener, null);

        ArgumentCaptor<WebViewClient> webViewClientCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(htmlInterstitialWebView).setWebViewClient(webViewClientCaptor.capture());
        WebViewClient webViewClient = webViewClientCaptor.getValue();

        webViewClient.shouldOverrideUrlLoading(null, "mopub://failLoad");

        verify(customEventInterstitialListener, never()).onInterstitialLoaded();
        verify(customEventInterstitialListener).onInterstitialFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void preRenderHtml_shouldHaveAMoPubUriInterfaceThatForwardsOnInterstitialLoaded() throws Exception {
        MoPubActivity.preRenderHtml(context, customEventInterstitialListener, null);

        ArgumentCaptor<MoPubUriJavascriptFireFinishLoadListener> moPubUriJavascriptFireFinishLoadListenerCaptor = ArgumentCaptor.forClass(MoPubUriJavascriptFireFinishLoadListener.class);
        verify(htmlInterstitialWebView).addMoPubUriJavascriptInterface(moPubUriJavascriptFireFinishLoadListenerCaptor.capture());
        MoPubUriJavascriptFireFinishLoadListener moPubUriJavascriptFireFinishLoadListener = moPubUriJavascriptFireFinishLoadListenerCaptor.getValue();

        moPubUriJavascriptFireFinishLoadListener.onInterstitialLoaded();

        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Test
    public void onCreate_shouldSetContentView() throws Exception {
        subject.onCreate(null);

        assertThat(getContentView(subject).getChildCount()).isEqualTo(2);
    }

    @Test
    public void onCreate_shouldLayoutWebView() throws Exception {
        subject.onCreate(null);

        ArgumentCaptor<RelativeLayout.LayoutParams> captor = ArgumentCaptor.forClass(RelativeLayout.LayoutParams.class);
        verify(htmlInterstitialWebView).setLayoutParams(captor.capture());
        RelativeLayout.LayoutParams actualLayoutParams = captor.getValue();

        assertThat(actualLayoutParams.width).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
        assertThat(actualLayoutParams.height).isEqualTo(RelativeLayout.LayoutParams.MATCH_PARENT);
    }

    @Test
    public void getAdView_shouldReturnPopulatedHtmlWebView() throws Exception {
        View adView = subject.getAdView();

        assertThat(adView).isSameAs(htmlInterstitialWebView);
        assertThat(TestHtmlInterstitialWebViewFactory.getLatestListener()).isNotNull();
        assertThat(TestHtmlInterstitialWebViewFactory.getLatestIsScrollable()).isEqualTo(EXPECTED_IS_SCROLLABLE);
        assertThat(TestHtmlInterstitialWebViewFactory.getLatestClickthroughUrl()).isEqualTo(EXPECTED_CLICKTHROUGH_URL);
        assertThat(TestHtmlInterstitialWebViewFactory.getLatestRedirectUrl()).isEqualTo(EXPECTED_REDIRECT_URL);
        verify(htmlInterstitialWebView).loadHtmlResponse(EXPECTED_HTML_DATA);
    }

    @Test
    public void onDestroy_shouldDestroyMoPubView() throws Exception {
        subject.onCreate(null);
        subject.onDestroy();

        verify(htmlInterstitialWebView).destroy();
        assertThat(getContentView(subject).getChildCount()).isEqualTo(0);
    }

    @Test
    public void onDestroy_shouldFireJavascriptWebviewDidClose() throws Exception {
        subject.onCreate(null);
        subject.onDestroy();

        verify(htmlInterstitialWebView).loadUrl(eq("javascript:webviewDidClose();"));
    }

    @Test
    public void start_shouldStartMoPubActivityWithCorrectParameters() throws Exception {
        MoPubActivity.start(subject, "expectedResponse", true, "redirectUrl", "clickthroughUrl", adConfiguration);

        Intent nextStartedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(nextStartedActivity.getStringExtra(HTML_RESPONSE_BODY_KEY)).isEqualTo("expectedResponse");
        assertThat(nextStartedActivity.getBooleanExtra(SCROLLABLE_KEY, false)).isTrue();
        assertThat(nextStartedActivity.getStringExtra(REDIRECT_URL_KEY)).isEqualTo("redirectUrl");
        assertThat(nextStartedActivity.getStringExtra(CLICKTHROUGH_URL_KEY)).isEqualTo("clickthroughUrl");
        assertThat(nextStartedActivity.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(nextStartedActivity.getComponent().getClassName()).isEqualTo("com.mopub.mobileads.MoPubActivity");
    }

    @Test
    public void getAdView_shouldCreateHtmlInterstitialWebViewAndLoadResponse() throws Exception {
        subject.getAdView();

        assertThat(TestHtmlInterstitialWebViewFactory.getLatestListener()).isNotNull();
        assertThat(TestHtmlInterstitialWebViewFactory.getLatestIsScrollable()).isEqualTo(EXPECTED_IS_SCROLLABLE);
        assertThat(TestHtmlInterstitialWebViewFactory.getLatestRedirectUrl()).isEqualTo(EXPECTED_REDIRECT_URL);
        assertThat(TestHtmlInterstitialWebViewFactory.getLatestClickthroughUrl()).isEqualTo(EXPECTED_CLICKTHROUGH_URL);
        verify(htmlInterstitialWebView).loadHtmlResponse(EXPECTED_HTML_DATA);
    }

    @Test
    public void getAdView_shouldSetUpForBroadcastingClicks() throws Exception {
        subject.getAdView();
        BroadcastReceiver broadcastReceiver = mock(BroadcastReceiver.class);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        TestHtmlInterstitialWebViewFactory.getLatestListener().onInterstitialClicked();

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(broadcastReceiver).onReceive(any(Context.class), intentCaptor.capture());
        Intent intent = intentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(ACTION_INTERSTITIAL_CLICK);
    }

    @Test
    public void getAdView_shouldSetUpForBroadcastingFail() throws Exception {
        subject.getAdView();
        BroadcastReceiver broadcastReceiver = mock(BroadcastReceiver.class);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        TestHtmlInterstitialWebViewFactory.getLatestListener().onInterstitialFailed(UNSPECIFIED);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(broadcastReceiver).onReceive(any(Context.class), intentCaptor.capture());
        Intent intent = intentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(ACTION_INTERSTITIAL_FAIL);

        assertThat(shadowOf(subject).isFinishing()).isTrue();
    }

    @Test
    public void broadcastingInterstitialListener_onInterstitialLoaded_shouldCallJavascriptWebViewDidAppear() throws Exception {
        MoPubActivity.BroadcastingInterstitialListener broadcastingInterstitialListener = ((MoPubActivity) subject).new BroadcastingInterstitialListener();

        broadcastingInterstitialListener.onInterstitialLoaded();

        verify(htmlInterstitialWebView).loadUrl(eq("javascript:webviewDidAppear();"));
    }

    @Test
    public void broadcastingInterstitialListener_onInterstitialFailed_shouldBroadcastFailAndFinish() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_FAIL, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        MoPubActivity.BroadcastingInterstitialListener broadcastingInterstitialListener = ((MoPubActivity) subject).new BroadcastingInterstitialListener();
        broadcastingInterstitialListener.onInterstitialFailed(null);

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
        assertThat(shadowOf(subject).isFinishing()).isTrue();
    }

    @Test
    public void broadcastingInterstitialListener_onInterstitialClicked_shouldBroadcastClick() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, testBroadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver, getHtmlInterstitialIntentFilter());

        MoPubActivity.BroadcastingInterstitialListener broadcastingInterstitialListener = ((MoPubActivity) subject).new BroadcastingInterstitialListener();
        broadcastingInterstitialListener.onInterstitialClicked();

        verify(broadcastReceiver).onReceive(any(Context.class), eq(expectedIntent));
    }

    private Intent createMoPubActivityIntent(String htmlData, boolean isScrollable, String redirectUrl, String clickthroughUrl, AdConfiguration adConfiguration) {
        return MoPubActivity.createIntent(new Activity(), htmlData, isScrollable, redirectUrl, clickthroughUrl, adConfiguration);
    }
}

