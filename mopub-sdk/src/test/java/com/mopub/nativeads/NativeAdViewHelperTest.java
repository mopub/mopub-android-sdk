package com.mopub.nativeads;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class NativeAdViewHelperTest {
    private Activity context;
    private RelativeLayout relativeLayout;
    private ViewGroup viewGroup;
    private BaseForwardingNativeAd mNativeAd;
    private ViewBinder viewBinder;
    private TextView titleView;
    private TextView textView;
    private TextView callToActionView;

    @Mock private NativeResponse mockNativeResponse1;
    @Mock private NativeResponse mockNativeResponse2;
    @Mock private ImpressionTracker mockImpressionTracker;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        relativeLayout = new RelativeLayout(context);
        relativeLayout.setId((int) Utils.generateUniqueId());
        viewGroup = new LinearLayout(context);

        mNativeAd = new BaseForwardingNativeAd() {};
        mNativeAd.setTitle("test title");
        mNativeAd.setText("test text");
        mNativeAd.setCallToAction("test call to action");

        titleView = new TextView(context);
        titleView.setId((int) Utils.generateUniqueId());
        textView = new TextView(context);
        textView.setId((int) Utils.generateUniqueId());
        callToActionView = new Button(context);
        callToActionView.setId((int) Utils.generateUniqueId());

        relativeLayout.addView(titleView);
        relativeLayout.addView(textView);
        relativeLayout.addView(callToActionView);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(textView.getId())
                .callToActionId(callToActionView.getId())
                .build();

        when(mockNativeResponse1.isDestroyed()).thenReturn(false);
        when(mockNativeResponse2.isDestroyed()).thenReturn(false);
    }

    @Test
    public void getAdView_shouldReturnPopulatedView() throws Exception {
        when(mockNativeResponse1.getTitle()).thenReturn("test title");
        when(mockNativeResponse1.getText()).thenReturn("test text");
        when(mockNativeResponse1.getCallToAction()).thenReturn("test call to action");

        View view = NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, mockNativeResponse1, viewBinder, null);

        assertThat(((TextView)view.findViewById(titleView.getId())).getText()).isEqualTo("test title");
        assertThat(((TextView)view.findViewById(textView.getId())).getText()).isEqualTo("test text");
        assertThat(((TextView)view.findViewById(callToActionView.getId())).getText()).isEqualTo("test call to action");

        // not testing images due to testing complexity
    }

    @Test
    public void getAdView_withNullViewBinder_shouldReturnEmptyView() throws Exception {
        View view = NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, mockNativeResponse1, null, null);

        assertThat(view).isNotNull();
        assertThat(view).isNotEqualTo(relativeLayout);
    }

    @Test
    public void getAdView_withNullNativeResponse_shouldReturnGONEConvertView() throws Exception {
        View view = NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, null, viewBinder, null);

        assertThat(view).isEqualTo(relativeLayout);
        assertThat(view.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void getAdView_withDestroyedNativeResponse_shouldReturnGONEConvertView() throws Exception {
        when(mockNativeResponse1.isDestroyed()).thenReturn(true);
        View view = NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, mockNativeResponse1, viewBinder, null);

        assertThat(view).isEqualTo(relativeLayout);
        assertThat(view.getVisibility()).isEqualTo(View.GONE);
    }
    
    @Test
    public void getAdView_shouldRemoveViewFromImpressionTracker_shouldClearPreviousNativeResponse() throws Exception {
        NativeAdViewHelper.sImpressionTrackerMap.put(context, mockImpressionTracker);

        NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, mockNativeResponse1, viewBinder, null);
        verify(mockImpressionTracker).removeView(relativeLayout);

        // Second call should clear the first NativeResponse
        NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, mockNativeResponse2, viewBinder, null);
        verify(mockImpressionTracker, times(2)).removeView(relativeLayout);
        verify(mockNativeResponse1).clear(relativeLayout);

        // Third call should clear the second NativeResponse
        NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, mockNativeResponse1, viewBinder, null);
        verify(mockImpressionTracker, times(3)).removeView(relativeLayout);
        verify(mockNativeResponse2).clear(relativeLayout);
    }

    @Test
    public void getAdView_withNetworkImpressionTracker_shouldNotAddViewToImpressionTracker_shouldPrepareNativeResponse() throws Exception {
        NativeAdViewHelper.sImpressionTrackerMap.put(context, mockImpressionTracker);
        when(mockNativeResponse1.isOverridingImpressionTracker()).thenReturn(true);

        NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, mockNativeResponse1, viewBinder, null);

        verify(mockImpressionTracker, never()).addView(any(View.class), any(NativeResponse.class));
        verify(mockNativeResponse1).prepare(relativeLayout);
    }

    @Test
    public void getAdView_withoutNetworkImpressionTracker_shouldAddViewToImpressionTracker_shouldPrepareNativeResponse() throws Exception {
        NativeAdViewHelper.sImpressionTrackerMap.put(context, mockImpressionTracker);
        when(mockNativeResponse1.isOverridingImpressionTracker()).thenReturn(false);

        NativeAdViewHelper.getAdView(relativeLayout, viewGroup, context, mockNativeResponse1, viewBinder, null);

        verify(mockImpressionTracker).addView(relativeLayout, mockNativeResponse1);
        verify(mockNativeResponse1).prepare(relativeLayout);
    }
}
