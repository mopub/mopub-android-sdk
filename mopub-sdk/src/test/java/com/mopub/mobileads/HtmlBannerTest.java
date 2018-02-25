package com.mopub.mobileads;

import android.app.Activity;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestHtmlBannerWebViewFactory;
import com.mopub.mobileads.test.support.TestMoPubViewFactory;
import com.mopub.network.AdResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.DataKeys.REDIRECT_URL_KEY;
import static com.mopub.common.DataKeys.SCROLLABLE_KEY;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class HtmlBannerTest {

    private HtmlBanner subject;
    private HtmlBannerWebView htmlBannerWebView;
    private CustomEventBanner.CustomEventBannerListener customEventBannerListener;
    private Map<String, Object> localExtras;
    private Map<String, String> serverExtras;
    private Activity context;
    private String responseBody;

    @Before
    public void setup() {
        subject = new HtmlBanner();
        htmlBannerWebView = TestHtmlBannerWebViewFactory.getSingletonMock();
        customEventBannerListener = mock(CustomEventBanner.CustomEventBannerListener.class);
        context = Robolectric.buildActivity(Activity.class).create().get();
        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();
        responseBody = "expected response body";
        serverExtras.put(HTML_RESPONSE_BODY_KEY, responseBody);
        serverExtras.put(SCROLLABLE_KEY, "false");
    }

    @Test
    public void loadBanner_shouldPopulateTheHtmlWebViewWithHtml() throws Exception {
        subject.loadBanner(context, customEventBannerListener, localExtras, serverExtras);

        assertThat(TestHtmlBannerWebViewFactory.getLatestListener()).isSameAs(customEventBannerListener);
        assertThat(TestHtmlBannerWebViewFactory.getLatestIsScrollable()).isFalse();
        assertThat(TestHtmlBannerWebViewFactory.getLatestRedirectUrl()).isNull();
        assertThat(TestHtmlBannerWebViewFactory.getLatestClickthroughUrl()).isNull();
        verify(htmlBannerWebView).loadHtmlResponse(responseBody);
    }

    @Test
    public void loadBanner_whenNoHtmlResponse_shouldNotifyBannerFailed() throws Exception {
        serverExtras.remove(HTML_RESPONSE_BODY_KEY);
        subject.loadBanner(context, customEventBannerListener, localExtras, serverExtras);

        verify(customEventBannerListener).onBannerFailed(eq(NETWORK_INVALID_STATE));
        assertThat(TestHtmlBannerWebViewFactory.getLatestListener()).isNull();
        assertThat(TestHtmlBannerWebViewFactory.getLatestIsScrollable()).isFalse();
        assertThat(TestHtmlBannerWebViewFactory.getLatestRedirectUrl()).isNull();
        assertThat(TestHtmlBannerWebViewFactory.getLatestClickthroughUrl()).isNull();
        verify(htmlBannerWebView, never()).loadHtmlResponse(anyString());
    }

    @Test
    public void loadBanner_shouldPassParametersThrough() throws Exception {
        serverExtras.put(SCROLLABLE_KEY, "true");
        serverExtras.put(REDIRECT_URL_KEY, "redirectUrl");
        serverExtras.put(CLICKTHROUGH_URL_KEY, "clickthroughUrl");
        subject.loadBanner(context, customEventBannerListener, localExtras, serverExtras);

        assertThat(TestHtmlBannerWebViewFactory.getLatestListener()).isSameAs(customEventBannerListener);
        assertThat(TestHtmlBannerWebViewFactory.getLatestIsScrollable()).isTrue();
        assertThat(TestHtmlBannerWebViewFactory.getLatestRedirectUrl()).isEqualTo("redirectUrl");
        assertThat(TestHtmlBannerWebViewFactory.getLatestClickthroughUrl()).isEqualTo("clickthroughUrl");
        verify(htmlBannerWebView).loadHtmlResponse(responseBody);
    }

    @Test
    public void loadBanner_withTrueFlag_shouldSetBannerImpressionPixelCountEnabledTrue() {
        assertThat(subject.isBannerImpressionPixelCountEnabled()).isFalse();

        localExtras.put(DataKeys.BANNER_IMPRESSION_PIXEL_COUNT_ENABLED, true);

        subject.loadBanner(context, customEventBannerListener, localExtras, serverExtras);

        assertThat(subject.isBannerImpressionPixelCountEnabled()).isTrue();
    }

    @Test
    public void loadBanner_withFalseFlag_shouldSetBannerImpressionPixelCountEnabledFalse() {
        assertThat(subject.isBannerImpressionPixelCountEnabled()).isFalse();

        localExtras.put(DataKeys.BANNER_IMPRESSION_PIXEL_COUNT_ENABLED, false);

        subject.loadBanner(context, customEventBannerListener, localExtras, serverExtras);

        assertThat(subject.isBannerImpressionPixelCountEnabled()).isFalse();
    }

    @Test
    public void onInvalidate_shouldDestroyTheHtmlWebView() throws Exception {
        subject.loadBanner(context, customEventBannerListener, localExtras, serverExtras);
        subject.onInvalidate();

        verify(htmlBannerWebView).destroy();
    }

    @Test
    public void loadBanner_shouldCauseServerDimensionsToBeHonoredWhenLayingOutView() throws Exception {
        subject.loadBanner(context, customEventBannerListener, localExtras, serverExtras);
        MoPubView moPubView = TestMoPubViewFactory.getSingletonMock();
        stub(moPubView.getContext()).toReturn(context);
        AdViewController adViewController = new AdViewController(context, moPubView);


        AdResponse adResponse = new AdResponse.Builder().setDimensions(320, 50).build();
        adViewController.onAdLoadSuccess(adResponse);

        adViewController.setAdContentView(htmlBannerWebView);
        ArgumentCaptor<FrameLayout.LayoutParams> layoutParamsCaptor = ArgumentCaptor.forClass(FrameLayout.LayoutParams.class);
        verify(moPubView).addView(eq(htmlBannerWebView), layoutParamsCaptor.capture());
        FrameLayout.LayoutParams layoutParams = layoutParamsCaptor.getValue();

        assertThat(layoutParams.width).isEqualTo(320);
        assertThat(layoutParams.height).isEqualTo(50);
        assertThat(layoutParams.gravity).isEqualTo(Gravity.CENTER);
    }

    @Test
    public void trackMpxAndThirdPartyImpressions_shouldFireJavascriptWebViewDidAppear() throws Exception {
        subject.loadBanner(context, customEventBannerListener, localExtras, serverExtras);
        subject.trackMpxAndThirdPartyImpressions();

        verify(htmlBannerWebView).loadHtmlResponse(responseBody);
        verify(htmlBannerWebView).loadUrl(eq("javascript:webviewDidAppear();"));
    }
}
