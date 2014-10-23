package com.mopub.nativeads;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.mopub.common.DownloadResponse;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.HttpRequestInfo;

import java.util.List;
import java.util.Map;

import static com.mopub.nativeads.MoPubNative.EMPTY_EVENT_LISTENER;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class NativeResponseTest {

    private NativeResponse subject;
    private BaseForwardingNativeAd mNativeAd;
    private Activity context;
    private ViewGroup view;
    private MoPubNative.MoPubNativeListener moPubNativeListener;
    private NativeResponse subjectWMockBaseNativeAd;
    private NativeAdInterface mMockNativeAd;
    private boolean baseNativeAdRecordedImpression;
    private boolean baseNativeAdIsClicked;
    private DownloadResponse downloadResponse;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        mNativeAd = new BaseForwardingNativeAd() {
            @Override
            public void recordImpression() {
                baseNativeAdRecordedImpression = true;
            }

            @Override
            public void handleClick(final View view) {
                baseNativeAdIsClicked = true;
            }
        };
        mNativeAd.setTitle("title");
        mNativeAd.setText("text");
        mNativeAd.setMainImageUrl("mainImageUrl");
        mNativeAd.setIconImageUrl("iconImageUrl");
        mNativeAd.setClickDestinationUrl("clickDestinationUrl");
        mNativeAd.setCallToAction("callToAction");
        mNativeAd.addExtra("extra", "extraValue");
        mNativeAd.addExtra("extraImage", "extraImageUrl");
        mNativeAd.addImpressionTracker("impressionUrl");
        mNativeAd.setImpressionMinTimeViewed(500);

        view = new LinearLayout(context);

        final TestHttpResponseWithHeaders testHttpResponseWithHeaders = new TestHttpResponseWithHeaders(200, "");
        testHttpResponseWithHeaders.addHeader(ResponseHeader.IMPRESSION_URL.getKey(), "moPubImpressionTrackerUrl");
        testHttpResponseWithHeaders.addHeader(ResponseHeader.CLICKTHROUGH_URL.getKey(), "moPubClickTrackerUrl");
        downloadResponse = new DownloadResponse(testHttpResponseWithHeaders);

        moPubNativeListener = mock(MoPubNative.MoPubNativeListener.class);

        subject = new NativeResponse(context, downloadResponse, "adunit_id", mNativeAd, moPubNativeListener);

        mMockNativeAd = mock(NativeAdInterface.class);
        subjectWMockBaseNativeAd = new NativeResponse(context, downloadResponse, "adunit_id", mMockNativeAd, moPubNativeListener);
    }

    @Test
    public void constructor_shouldSetNativeEventListenerOnNativeAdInterface() {
        reset(mMockNativeAd);
        subject = new NativeResponse(context, downloadResponse, "adunit_id", mMockNativeAd, moPubNativeListener);
        verify(mMockNativeAd).setNativeEventListener(any(BaseForwardingNativeAd.NativeEventListener.class));
    }

    @Test
    public void getTitle_shouldReturnTitleFromBaseNativeAd() {
        assertThat(subject.getTitle()).isEqualTo("title");
    }

    @Test
    public void getTitle_shouldReturnTextFromBaseNativeAd() {
        assertThat(subject.getText()).isEqualTo("text");
    }

    @Test
    public void getMainImageUrl_shouldReturnMainImageUrlFromBaseNativeAd() {
        assertThat(subject.getMainImageUrl()).isEqualTo("mainImageUrl");
    }

    @Test
    public void getIconImageUrl_shouldReturnIconImageUrlFromBaseNativeAd() {
        assertThat(subject.getIconImageUrl()).isEqualTo("iconImageUrl");
    }

    @Test
    public void getClickDestinationUrl_shouldReturnClickDestinationUrlFromBaseNativeAd() {
        assertThat(subject.getClickDestinationUrl()).isEqualTo("clickDestinationUrl");
    }

    @Test
    public void getCallToAction_shouldReturnCallToActionFromBaseNativeAd() {
        assertThat(subject.getCallToAction()).isEqualTo("callToAction");
    }

    @Test
    public void getExtra_shouldReturnExtraFromBaseNativeAd() {
        assertThat(subject.getExtra("extra")).isEqualTo("extraValue");
    }

    @Test
    public void getExtras_shouldReturnCopyOfExtrasMapFromBaseNativeAd() {
        final Map<String, Object> extras = subject.getExtras();
        assertThat(extras.size()).isEqualTo(2);
        assertThat(extras.get("extra")).isEqualTo("extraValue");
        assertThat(extras.get("extraImage")).isEqualTo("extraImageUrl");
        assertThat(extras).isNotSameAs(mNativeAd.getExtras());
    }

    @Test
    public void getImpressionTrackers_shouldReturnImpressionTrackersFromMoPubAndFromBaseNativeAd() {
        final List<String> impressionTrackers = subject.getImpressionTrackers();
        assertThat(impressionTrackers).containsOnly("moPubImpressionTrackerUrl", "impressionUrl");
    }

    @Test
    public void getImpressionMinTimeViewed_shouldReturnImpressionMinTimeViewedFromBaseNativeAd() {
        assertThat(subject.getImpressionMinTimeViewed()).isEqualTo(500);
    }

    @Test
    public void getImpressionMinPercentageViewed_shouldReturnImpressionMinPercentageViewedFromBaseNativeAd() {
        assertThat(subject.getImpressionMinPercentageViewed()).isEqualTo(50);
    }

    @Test
    public void getClickTracker_shouldReturnMoPubClickTracker() {
        assertThat(subject.getClickTracker()).isEqualTo("moPubClickTrackerUrl");
    }

    @Test
    public void prepare_shouldCallPrepareOnBaseNativeAd() {
        subjectWMockBaseNativeAd.prepare(view);
        verify(mMockNativeAd).prepare(view);
    }

    @Test
    public void prepare_whenDestroyed_shouldReturnFast() {
        subjectWMockBaseNativeAd.destroy();
        subjectWMockBaseNativeAd.prepare(view);
        verify(mMockNativeAd, never()).prepare(view);
    }
    
    @Test
    public void prepare_withOverridingeClickTracker_shouldNotSetOnClickListener() throws Exception {
        when(mMockNativeAd.isOverridingClickTracker()).thenReturn(true);
        View view = mock(View.class);
        subjectWMockBaseNativeAd.prepare(view);
        verify(view, never()).setOnClickListener(any(NativeResponse.NativeViewClickListener.class));
    }

    @Test
    public void prepare_withoutOverridingClickTracker_shouldSetOnClickListener() throws Exception {
        when(mMockNativeAd.isOverridingClickTracker()).thenReturn(false);
        View view = mock(View.class);
        subjectWMockBaseNativeAd.prepare(view);
        verify(view).setOnClickListener(any(NativeResponse.NativeViewClickListener.class));
    }

    @Test
    public void prepare_shouldAttachClickListenersToViewTree() {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        Button callToActionView = new Button(context);
        callToActionView.setId((int) Utils.generateUniqueId());
        relativeLayout.addView(callToActionView);

        assertThat(relativeLayout.performClick()).isFalse();
        assertThat(callToActionView.performClick()).isFalse();

        subject.prepare(relativeLayout);

        assertThat(relativeLayout.performClick()).isTrue();
        assertThat(callToActionView.performClick()).isTrue();
    }

    @Test
    public void recordImpression_shouldRecordImpressionsAndCallIntoBaseNativeAdAndNotifyListenerIdempotently() {
        Robolectric.getFakeHttpLayer().addPendingHttpResponse(200, "ok");
        Robolectric.getFakeHttpLayer().addPendingHttpResponse(200, "ok");
        assertThat(subject.getRecordedImpression()).isFalse();

        subject.recordImpression(view);

        assertThat(subject.getRecordedImpression()).isTrue();

        List<HttpRequestInfo> httpRequestInfos = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(httpRequestInfos.size()).isEqualTo(2);
        assertThat(httpRequestInfos.get(0).getHttpRequest().getRequestLine().getUri()).isEqualTo("moPubImpressionTrackerUrl");
        assertThat(httpRequestInfos.get(1).getHttpRequest().getRequestLine().getUri()).isEqualTo("impressionUrl");

        assertThat(baseNativeAdRecordedImpression).isTrue();
        verify(moPubNativeListener).onNativeImpression(view);

        // reset state
        baseNativeAdRecordedImpression = false;
        Robolectric.getFakeHttpLayer().clearRequestInfos();
        reset(moPubNativeListener);

        // verify impression tracking doesn't fire again
        subject.recordImpression(view);
        assertThat(subject.getRecordedImpression()).isTrue();
        assertThat(Robolectric.getFakeHttpLayer().getSentHttpRequestInfos()).isEmpty();
        assertThat(baseNativeAdRecordedImpression).isFalse();
        verify(moPubNativeListener, never()).onNativeImpression(view);
    }

    @Test
    public void recordImpression_whenDestroyed_shouldReturnFast() {
        subject.destroy();
        subject.recordImpression(view);
        assertThat(subject.getRecordedImpression()).isFalse();
        assertThat(Robolectric.getFakeHttpLayer().getSentHttpRequestInfos()).isEmpty();
        assertThat(baseNativeAdRecordedImpression).isFalse();
        verify(moPubNativeListener, never()).onNativeImpression(view);
    }

    @Test
    public void handleClick_withNoBaseNativeAdClickDestinationUrl_shouldRecordClickAndCallIntoBaseNativeAdAndNotifyListener() {
        Robolectric.getFakeHttpLayer().addPendingHttpResponse(200, "ok");
        assertThat(subject.isClicked()).isFalse();

        subject.handleClick(view);

        assertThat(subject.isClicked()).isTrue();

        List<HttpRequestInfo> httpRequestInfos = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos();
        assertThat(httpRequestInfos.size()).isEqualTo(1);
        assertThat(httpRequestInfos.get(0).getHttpRequest().getRequestLine().getUri()).isEqualTo("moPubClickTrackerUrl");

        assertThat(baseNativeAdIsClicked).isTrue();
        verify(moPubNativeListener).onNativeClick(view);

        // reset state
        baseNativeAdIsClicked = false;
        Robolectric.getFakeHttpLayer().clearRequestInfos();
        reset(moPubNativeListener);

        // second time, tracking does not fire
        subject.handleClick(view);
        assertThat(subject.isClicked()).isTrue();
        assertThat(Robolectric.getFakeHttpLayer().getSentHttpRequestInfos()).isEmpty();
        assertThat(baseNativeAdRecordedImpression).isFalse();
        verify(moPubNativeListener).onNativeClick(view);
    }

    @Ignore("pending")
    @Test
    public void handleClick_withBaseNativeAdClickDestinationUrl_shouldRecordClickAndCallIntoBaseNativeAdAndOpenClickDestinationAndNotifyListener() {
        // Really difficult to test url resolution since it doesn't use the apache http client
    }

    @Test
    public void handleClick_whenDestroyed_shouldReturnFast() {
        subject.destroy();
        subject.handleClick(view);
        assertThat(subject.isClicked()).isFalse();
        assertThat(Robolectric.getFakeHttpLayer().getSentHttpRequestInfos()).isEmpty();
        assertThat(baseNativeAdIsClicked).isFalse();
        verify(moPubNativeListener, never()).onNativeClick(view);
    }

    @Test
    public void destroy_shouldCallIntoBaseNativeAd() {
        subjectWMockBaseNativeAd.destroy();
        assertThat(subjectWMockBaseNativeAd.isDestroyed()).isTrue();
        verify(mMockNativeAd).destroy();

        reset(mMockNativeAd);

        subjectWMockBaseNativeAd.destroy();
        verify(mMockNativeAd, never()).destroy();
    }

    @Test
    public void destroy_shouldSetMoPubNativeEventListenerToEmptyMoPubNativeListener() {
        assertThat(subjectWMockBaseNativeAd.getMoPubNativeEventListener()).isSameAs(moPubNativeListener);

        subjectWMockBaseNativeAd.destroy();

        assertThat(subjectWMockBaseNativeAd.getMoPubNativeEventListener()).isSameAs(EMPTY_EVENT_LISTENER);
    }

    // NativeViewClickListener tests
    @Test
    public void NativeViewClickListener_onClick_shouldQueueClickTrackerAndUrlResolutionTasks() {
        subject = mock(NativeResponse.class);
        NativeResponse.NativeViewClickListener nativeViewClickListener = subject.new NativeViewClickListener();

        View view = new View(context);
        nativeViewClickListener.onClick(view);
        verify(subject).handleClick(view);
    }

    @Ignore("pending")
    @Test
    public void loadExtrasImage_shouldAsyncLoadImages() {
        // no easy way to test this since nothing can be mocked
        // also not a critical test since it directly calls another service
    }
}
