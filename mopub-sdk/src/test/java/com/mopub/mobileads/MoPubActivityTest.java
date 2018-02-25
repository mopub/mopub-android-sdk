package com.mopub.mobileads;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.mopub.common.AdReport;
import com.mopub.common.CreativeOrientation;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestHtmlInterstitialWebViewFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;

import static com.mopub.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.mopub.common.DataKeys.CREATIVE_ORIENTATION_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.DataKeys.REDIRECT_URL_KEY;
import static com.mopub.common.DataKeys.SCROLLABLE_KEY;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.MoPubErrorCode.UNSPECIFIED;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubActivityTest {
    private static final String EXPECTED_HTML_DATA = "htmlData";
    private static final boolean EXPECTED_IS_SCROLLABLE = true;
    @Mock private AdReport mockAdReport;
    private static final String EXPECTED_REDIRECT_URL = "redirectUrl";
    private static final String EXPECTED_CLICKTHROUGH_URL = "https://expected_url";
    private static final CreativeOrientation EXPECTED_ORIENTATION = CreativeOrientation.PORTRAIT;

    @Mock private BroadcastReceiver broadcastReceiver;
    private long testBroadcastIdentifier = 2222;

    private HtmlInterstitialWebView htmlInterstitialWebView;
    private CustomEventInterstitialListener customEventInterstitialListener;
    @Mock private HtmlInterstitial htmlInterstitial;

    private MoPubActivity subject;

    @Before
    public void setUp() throws Exception {
        htmlInterstitialWebView = TestHtmlInterstitialWebViewFactory.getSingletonMock();
        resetMockedView(htmlInterstitialWebView);

        Context context = Robolectric.buildActivity(Activity.class).create().get();
        Intent moPubActivityIntent = MoPubActivity.createIntent(context,
                EXPECTED_HTML_DATA, mockAdReport, EXPECTED_IS_SCROLLABLE,
                EXPECTED_REDIRECT_URL,
                EXPECTED_CLICKTHROUGH_URL, EXPECTED_ORIENTATION, testBroadcastIdentifier);

        final ActivityController<MoPubActivity> subjectController = Robolectric.buildActivity(
                MoPubActivity.class, moPubActivityIntent);
        subject = subjectController.get();
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                        testBroadcastIdentifier).getIntentFilter());
        subjectController.create();

        customEventInterstitialListener = mock(CustomEventInterstitialListener.class);
    }

    @Test
    public void onCreate_shouldHaveLockedOrientation() {
        // Since robolectric doesn't set a requested orientation, verifying that we have a value tells us that one was set.
        assertThat(subject.getRequestedOrientation()).isIn(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void preRenderHtml_shouldPreloadTheHtml() throws Exception {
        String htmlData = "this is nonsense";
        MoPubActivity.preRenderHtml(htmlInterstitial, subject, mockAdReport,
                customEventInterstitialListener, htmlData, true, "redirectUrl",
                "clickthroughUrl", testBroadcastIdentifier);

        verify(htmlInterstitialWebView).enablePlugins(eq(false));
        verify(htmlInterstitialWebView).loadHtmlResponse(htmlData);
    }

    @Test
    public void preRenderHtml_shouldEnableJavascriptCachingForDummyWebView() {
        MoPubActivity.preRenderHtml(htmlInterstitial, subject, mockAdReport,
                customEventInterstitialListener, "html_data", true, "redirectUrl",
                "clickthroughUrl", testBroadcastIdentifier);

        verify(htmlInterstitialWebView).enableJavascriptCaching();
    }

    @Test
    public void preRenderHtml_shouldHaveAWebViewClientThatForwardsFinishLoad() throws Exception {
        MoPubActivity.preRenderHtml(htmlInterstitial, subject, mockAdReport,
                customEventInterstitialListener, null, true, "redirectUrl",
                "clickthroughUrl", testBroadcastIdentifier);

        ArgumentCaptor<WebViewClient> webViewClientCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(htmlInterstitialWebView).setWebViewClient(webViewClientCaptor.capture());
        WebViewClient webViewClient = webViewClientCaptor.getValue();

        webViewClient.shouldOverrideUrlLoading(null, "mopub://finishLoad");

        verify(customEventInterstitialListener).onInterstitialLoaded();
        verify(customEventInterstitialListener, never()).onInterstitialFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void preRenderHtml_shouldHaveAWebViewClientThatForwardsFailLoad() throws Exception {
        MoPubActivity.preRenderHtml(htmlInterstitial, subject, mockAdReport,
                customEventInterstitialListener, null, true, "redirectUrl",
                "clickthroughUrl", testBroadcastIdentifier);

        ArgumentCaptor<WebViewClient> webViewClientCaptor = ArgumentCaptor.forClass(WebViewClient.class);
        verify(htmlInterstitialWebView).setWebViewClient(webViewClientCaptor.capture());
        WebViewClient webViewClient = webViewClientCaptor.getValue();

        webViewClient.shouldOverrideUrlLoading(null, "mopub://failLoad");

        verify(customEventInterstitialListener, never()).onInterstitialLoaded();
        verify(customEventInterstitialListener).onInterstitialFailed(any(MoPubErrorCode.class));
    }

    @Test
    public void onCreate_shouldSetContentView() throws Exception {
        // onCreate is called above in #setup

        assertThat(getContentView().getChildCount()).isEqualTo(1);
    }

    @Test
    public void getAdView_shouldReturnPopulatedHtmlWebView() throws Exception {
        // This is needed because we preload in onCreate and the mock gets triggered.
        resetMockedView(htmlInterstitialWebView);
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
        // onCreate is called in #setup
        subject.onDestroy();

        verify(htmlInterstitialWebView).destroy();
        assertThat(getContentView().getChildCount()).isEqualTo(0);
    }

    @Test
    public void onDestroy_shouldFireJavascriptWebviewDidClose() throws Exception {
        // onCreate is called in #setup
        subject.onDestroy();

        verify(htmlInterstitialWebView).loadUrl(eq("javascript:webviewDidClose();"));
    }

    @Test
    public void start_shouldStartMoPubActivityWithCorrectParameters() throws Exception {
        MoPubActivity.start(subject, "expectedResponse", mockAdReport, true, "redirectUrl", "clickthroughUrl", CreativeOrientation.PORTRAIT, testBroadcastIdentifier);

        Intent nextStartedActivity = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(nextStartedActivity.getStringExtra(HTML_RESPONSE_BODY_KEY)).isEqualTo("expectedResponse");
        assertThat(nextStartedActivity.getBooleanExtra(SCROLLABLE_KEY, false)).isTrue();
        assertThat(nextStartedActivity.getStringExtra(REDIRECT_URL_KEY)).isEqualTo("redirectUrl");
        assertThat(nextStartedActivity.getStringExtra(CLICKTHROUGH_URL_KEY)).isEqualTo("clickthroughUrl");
        assertThat(nextStartedActivity.getSerializableExtra(CREATIVE_ORIENTATION_KEY)).isEqualTo(CreativeOrientation.PORTRAIT);
        assertThat(nextStartedActivity.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(nextStartedActivity.getComponent().getClassName()).isEqualTo("com.mopub.mobileads.MoPubActivity");
    }

    @Test
    public void getAdView_shouldCreateHtmlInterstitialWebViewAndLoadResponse() throws Exception {
        // This is needed because we preload in onCreate and the mock gets triggered.
        resetMockedView(htmlInterstitialWebView);
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
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                        testBroadcastIdentifier).getIntentFilter());

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
        ShadowLocalBroadcastManager.getInstance(subject).registerReceiver(broadcastReceiver,
                new EventForwardingBroadcastReceiver(customEventInterstitialListener,
                        testBroadcastIdentifier).getIntentFilter());

        TestHtmlInterstitialWebViewFactory.getLatestListener().onInterstitialFailed(UNSPECIFIED);

        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(broadcastReceiver).onReceive(any(Context.class), intentCaptor.capture());
        Intent intent = intentCaptor.getValue();
        assertThat(intent.getAction()).isEqualTo(ACTION_INTERSTITIAL_FAIL);

        assertThat(subject.isFinishing()).isTrue();
    }

    @Test
    public void broadcastingInterstitialListener_onInterstitialLoaded_withWebViewCacheMiss_shouldCallJavascriptWebViewDidAppear() throws Exception {
        MoPubActivity.BroadcastingInterstitialListener broadcastingInterstitialListener = ((MoPubActivity) subject).new BroadcastingInterstitialListener();
        WebViewCacheService.clearAll();

        broadcastingInterstitialListener.onInterstitialLoaded();

        verify(htmlInterstitialWebView).loadUrl(eq("javascript:webviewDidAppear();"));
    }

    @Test
    public void broadcastingInterstitialListener_onInterstitialFailed_shouldBroadcastFailAndFinish() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_FAIL, testBroadcastIdentifier);

        MoPubActivity.BroadcastingInterstitialListener broadcastingInterstitialListener = ((MoPubActivity) subject).new BroadcastingInterstitialListener();
        broadcastingInterstitialListener.onInterstitialFailed(null);

        verify(broadcastReceiver).onReceive(any(Context.class), argThat(new IntentIsEqual(expectedIntent)));
        assertThat(((ShadowActivity) Shadow.extract(subject)).isFinishing()).isTrue();
    }

    @Test
    public void broadcastingInterstitialListener_onInterstitialClicked_shouldBroadcastClick() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_CLICK, testBroadcastIdentifier);

        MoPubActivity.BroadcastingInterstitialListener broadcastingInterstitialListener = ((MoPubActivity) subject).new BroadcastingInterstitialListener();
        broadcastingInterstitialListener.onInterstitialClicked();

        verify(broadcastReceiver).onReceive(any(Context.class), argThat(new IntentIsEqual(expectedIntent)));
    }

    @Test
    public void onCreate_shouldBroadcastInterstitialShow() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, testBroadcastIdentifier);

        verify(broadcastReceiver).onReceive(any(Context.class), argThat(new IntentIsEqual(expectedIntent)));
    }

    @Test
    public void onDestroy_shouldBroadcastInterstitialDismiss() throws Exception {
        Intent expectedIntent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, testBroadcastIdentifier);

        subject.onDestroy();

        verify(broadcastReceiver).onReceive(any(Context.class), argThat(new IntentIsEqual(expectedIntent)));
    }

    private FrameLayout getContentView() {
        return (FrameLayout) ((ViewGroup) subject.findViewById(android.R.id.content)).getChildAt(0);
    }

    protected void resetMockedView(View view) {
        reset(view);
        when(view.getLayoutParams()).thenReturn(
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));
    }

}

