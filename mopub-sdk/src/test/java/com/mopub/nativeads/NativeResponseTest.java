package com.mopub.nativeads;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.Request;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.util.List;
import java.util.Map;

import static com.mopub.nativeads.MoPubNative.EMPTY_EVENT_LISTENER;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
    @Mock
    private MoPubRequestQueue mockRequestQueue;
    private SpinningProgressView mockSpinningProgressView;


    @Before
    public void setUp() throws Exception {
        setupWithClickUrl("clickDestinationUrl");
    }

    private void setupWithClickUrl(String clickUrl) {
        context = Robolectric.buildActivity(Activity.class).create().get();
        mNativeAd = new BaseForwardingNativeAd() {
            @Override
            public void recordImpression() {
                baseNativeAdRecordedImpression = true;
            }

            @Override
            public void handleClick(@NonNull final View view) {
                baseNativeAdIsClicked = true;
            }
        };
        mNativeAd.setTitle("title");
        mNativeAd.setText("text");
        mNativeAd.setMainImageUrl("mainImageUrl");
        mNativeAd.setIconImageUrl("iconImageUrl");
        mNativeAd.setClickDestinationUrl(clickUrl);
        mNativeAd.setCallToAction("callToAction");
        mNativeAd.addExtra("extra", "extraValue");
        mNativeAd.addExtra("extraImage", "extraImageUrl");
        mNativeAd.addImpressionTracker("impressionUrl");
        mNativeAd.setImpressionMinTimeViewed(500);

        view = new LinearLayout(context);

        Networking.setRequestQueueForTesting(mockRequestQueue);

        moPubNativeListener = mock(MoPubNative.MoPubNativeListener.class);

        subject = new NativeResponse(context,
                "moPubImpressionTrackerUrl",
                "moPubClickTrackerUrl",
                "adunit_id", mNativeAd, moPubNativeListener);

        mMockNativeAd = mock(NativeAdInterface.class);
        subjectWMockBaseNativeAd = new NativeResponse(context,
                "moPubImpressionTrackerUrl",
                "moPubClickTrackerUrl",
                "adunit_id", mMockNativeAd, moPubNativeListener);

        mockSpinningProgressView = mock(SpinningProgressView.class);
    }

    @Test
    public void constructor_shouldSetNativeEventListenerOnNativeAdInterface() {
        reset(mMockNativeAd);
        subject = new NativeResponse(context, "moPubImpressionTrackerUrl", "moPubClickTrackerUrl",
                "adunit_id", mMockNativeAd, moPubNativeListener);
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
        assertThat(subject.getRecordedImpression()).isFalse();

        subject.recordImpression(view);

        assertThat(subject.getRecordedImpression()).isTrue();

        assertThat(baseNativeAdRecordedImpression).isTrue();
        verify(moPubNativeListener).onNativeImpression(view);
        // There are two impression trackers here.
        verify(mockRequestQueue, times(2)).add(any(Request.class));

        // reset state
        baseNativeAdRecordedImpression = false;
        reset(moPubNativeListener);
        reset(mockRequestQueue);

        // verify impression tracking doesn't fire again
        subject.recordImpression(view);
        assertThat(subject.getRecordedImpression()).isTrue();
        assertThat(baseNativeAdRecordedImpression).isFalse();
        verify(moPubNativeListener, never()).onNativeImpression(view);
        verify(mockRequestQueue, never()).add(any(Request.class));
    }

    @Test
    public void recordImpression_whenDestroyed_shouldReturnFast() {
        subject.destroy();
        subject.recordImpression(view);
        assertThat(subject.getRecordedImpression()).isFalse();
        assertThat(baseNativeAdRecordedImpression).isFalse();
        verify(moPubNativeListener, never()).onNativeImpression(view);
        verify(mockRequestQueue, never()).add(any(Request.class));
    }

    @Test
    public void handleClick_withNoBaseNativeAdClickDestinationUrl_shouldRecordClickAndCallIntoBaseNativeAdAndNotifyListener() {
        assertThat(subject.isClicked()).isFalse();

        subject.handleClick(view);

        assertThat(subject.isClicked()).isTrue();

        assertThat(baseNativeAdIsClicked).isTrue();
        verify(moPubNativeListener).onNativeClick(view);
        verify(mockRequestQueue).add(any(Request.class));

        // reset state
        baseNativeAdIsClicked = false;
        reset(moPubNativeListener);
        reset(mockRequestQueue);

        // second time, tracking does not fire
        subject.handleClick(view);
        assertThat(subject.isClicked()).isTrue();
        assertThat(baseNativeAdRecordedImpression).isFalse();
        verify(moPubNativeListener).onNativeClick(view);
        verifyZeroInteractions(mockRequestQueue);
    }

    @Ignore("pending")
    @Test
    public void handleClick_withBaseNativeAdClickDestinationUrl_shouldRecordClickAndCallIntoBaseNativeAdAndOpenClickDestinationAndNotifyListener() {
        // Really difficult to test url resolution since it doesn't use the apache http client
    }

    @Test
    public void handleClick_shouldShowSpinner_shouldRemoveSpinner_WhenSucceeded() {
        setupWithClickUrl("http://www.mopub.com");

        Robolectric.getBackgroundScheduler().pause();

        subject.handleClick(view, mockSpinningProgressView);

        verify(mockSpinningProgressView).addToRoot(view);
        Robolectric.getBackgroundScheduler().unPause();
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void handleClick_shouldShowSpinner_shouldRemoveSpinner_WhenFailed() {
        setupWithClickUrl("");

        Robolectric.getBackgroundScheduler().pause();

        subject.handleClick(view, mockSpinningProgressView);

        verify(mockSpinningProgressView).addToRoot(view);
        Robolectric.getBackgroundScheduler().unPause();
        verify(mockSpinningProgressView).removeFromRoot();
    }

    @Test
    public void handleClick_withNullView_shouldNotShowSpinner() {
        setupWithClickUrl("http://www.mopub.com");

        Robolectric.getBackgroundScheduler().pause();

        subject.handleClick(null, mockSpinningProgressView);

        verify(mockSpinningProgressView, never()).addToRoot(view);
        Robolectric.getBackgroundScheduler().unPause();
        verify(mockSpinningProgressView, never()).removeFromRoot();
    }

    @Test
    public void handleClick_whenDestroyed_shouldReturnFast() {
        subject.destroy();
        subject.handleClick(view);
        assertThat(subject.isClicked()).isFalse();
        assertThat(baseNativeAdIsClicked).isFalse();
        verify(moPubNativeListener, never()).onNativeClick(view);
        verifyZeroInteractions(mockRequestQueue);
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
