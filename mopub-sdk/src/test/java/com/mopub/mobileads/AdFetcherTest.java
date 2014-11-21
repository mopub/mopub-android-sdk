package com.mopub.mobileads;

import android.os.Build;
import android.os.Build.VERSION_CODES;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.factories.AdFetchTaskFactory;
import com.mopub.mobileads.test.support.TestAdFetchTaskFactory;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.apache.http.HttpResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

import static com.mopub.common.util.ResponseHeader.AD_TYPE;
import static com.mopub.common.util.ResponseHeader.CUSTOM_EVENT_DATA;
import static com.mopub.common.util.ResponseHeader.CUSTOM_EVENT_NAME;
import static com.mopub.common.util.ResponseHeader.FULL_AD_TYPE;
import static com.mopub.common.util.ResponseHeader.NATIVE_PARAMS;
import static com.mopub.common.util.VersionCode.GINGERBREAD;
import static com.mopub.common.util.VersionCode.ICE_CREAM_SANDWICH;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class AdFetcherTest {
    private AdFetcher subject;
    private AdViewController adViewController;
    private MoPubView moPubView;
    private HttpResponse response;

    @Before
    public void setup() {
        adViewController = mock(AdViewController.class);
        moPubView = mock(MoPubView.class);
        stub(adViewController.getMoPubView()).toReturn(moPubView);

        subject = new AdFetcher(adViewController, "expected userAgent");
        response = new TestHttpResponseWithHeaders(200, "yahoo!!!");
    }

    @Test
    public void shouldSendResponseToAdView() {
        Robolectric.addPendingHttpResponse(response);

        subject.fetchAdForUrl("url");

        verify(adViewController).configureUsingHttpResponse(eq(response));
    }

    @Test
    public void fetchAdForUrl_shouldRouteMillennialBannerToCustomEventHandling() throws Exception {
        String json = "{\"adWidth\": 320, \"adHeight\": 50, \"adUnitID\": \"44310\"}";
        stub(adViewController.getAdConfiguration()).toReturn(mock(AdConfiguration.class));
        response.addHeader(AD_TYPE.getKey(), "millennial_native");
        response.addHeader(NATIVE_PARAMS.getKey(), json);
        Robolectric.addPendingHttpResponse(response);

        subject.fetchAdForUrl("ignored_url");

        Map<String, String> paramsMap = new HashMap<String, String>();
        paramsMap.put(CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MillennialBanner");
        paramsMap.put(CUSTOM_EVENT_DATA.getKey(), json);

        verify(moPubView).loadCustomEvent(eq(paramsMap));
    }

    @Test
    public void fetchAdForUrl_shouldRouteMillennialInterstitialToCustomEventHandling() throws Exception {
        AdViewController interstitialAdViewController = mock(AdViewController.class);
        MoPubInterstitial.MoPubInterstitialView moPubInterstitialView = mock(MoPubInterstitial.MoPubInterstitialView.class);
        stub(interstitialAdViewController.getMoPubView()).toReturn(moPubInterstitialView);
        stub(interstitialAdViewController.getAdConfiguration()).toReturn(mock(AdConfiguration.class));
        subject = new AdFetcher(interstitialAdViewController, "expected userAgent");

        String json = "{\"adWidth\": 320, \"adHeight\": 480, \"adUnitID\": \"44310\"}";
        response.addHeader(AD_TYPE.getKey(), "interstitial");
        response.addHeader(FULL_AD_TYPE.getKey(), "millennial_full");
        response.addHeader(NATIVE_PARAMS.getKey(), json);
        Robolectric.addPendingHttpResponse(response);

        subject.fetchAdForUrl("ignored_url");

        Map<String, String> paramsMap = new HashMap<String, String>();
        paramsMap.put(CUSTOM_EVENT_NAME.getKey(), "com.mopub.mobileads.MillennialInterstitial");
        paramsMap.put(CUSTOM_EVENT_DATA.getKey(), json);

        verify(moPubInterstitialView).loadCustomEvent(eq(paramsMap));
    }

    @Config(reportSdk = VERSION_CODES.ICE_CREAM_SANDWICH)
    @Test
    public void fetchAdForUrl_atLeastIcs_shouldExecuteUsingAnExecutor() throws Exception {
        AdFetchTaskFactory.setInstance(new TestAdFetchTaskFactory());
        AdFetchTask adFetchTask = TestAdFetchTaskFactory.getSingletonMock();

        subject.fetchAdForUrl("some url");

        verify(adFetchTask).executeOnExecutor(any(Executor.class), eq("some url"));
        verify(adFetchTask, never()).execute(anyString());
    }

    @Config(reportSdk = VERSION_CODES.GINGERBREAD_MR1)
    @Test
    public void fetchAdForUrl_beforeHoneycomb_shouldExecuteWithoutAnExecutor() throws Exception {
        AdFetchTaskFactory.setInstance(new TestAdFetchTaskFactory());
        AdFetchTask adFetchTask = TestAdFetchTaskFactory.getSingletonMock();

        subject.fetchAdForUrl("some url");

        verify(adFetchTask, never()).executeOnExecutor(any(Executor.class), anyString());
        verify(adFetchTask).execute(eq("some url"));
    }
}
