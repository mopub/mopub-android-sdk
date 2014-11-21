package com.mopub.nativeads;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.DownloadResponse;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.mopub.nativeads.MoPubNative.MoPubNativeListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SdkTestRunner.class)
public class MoPubNativeAdRendererTest {
    private MoPubNativeAdRenderer subject;
    private Activity context;
    private RelativeLayout relativeLayout;
    private ViewGroup viewGroup;
    private NativeResponse nativeResponse;
    private BaseForwardingNativeAd mNativeAd;
    private ViewBinder viewBinder;
    private TextView titleView;
    private TextView textView;
    private TextView callToActionView;
    private ImageView mainImageView;
    private ImageView iconImageView;
    private ImageView badView;

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
        mNativeAd.setClickDestinationUrl("destinationUrl");

        final TestHttpResponseWithHeaders testHttpResponseWithHeaders = new TestHttpResponseWithHeaders(200, "");
        testHttpResponseWithHeaders.addHeader(ResponseHeader.CLICKTHROUGH_URL.getKey(), "clickTrackerUrl");
        final DownloadResponse downloadResponse = new DownloadResponse(testHttpResponseWithHeaders);
        nativeResponse = new NativeResponse(context, downloadResponse, "test ID", mNativeAd, mock(MoPubNativeListener.class));

        titleView = new TextView(context);
        titleView.setId((int) Utils.generateUniqueId());
        textView = new TextView(context);
        textView.setId((int) Utils.generateUniqueId());
        callToActionView = new Button(context);
        callToActionView.setId((int) Utils.generateUniqueId());
        mainImageView = new ImageView(context);
        mainImageView.setId((int) Utils.generateUniqueId());
        iconImageView = new ImageView(context);
        iconImageView.setId((int) Utils.generateUniqueId());
        badView = new ImageView(context);
        badView.setId((int) Utils.generateUniqueId());

        relativeLayout.addView(titleView);
        relativeLayout.addView(textView);
        relativeLayout.addView(callToActionView);
        relativeLayout.addView(mainImageView);
        relativeLayout.addView(iconImageView);
        relativeLayout.addView(badView);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(textView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        subject = new MoPubNativeAdRenderer(viewBinder);
    }

    @Test(expected = NullPointerException.class)
    public void createAdView_withNullContext_shouldThrowNPE() {
        subject.createAdView(null, viewGroup);
    }

    @Test(expected = NullPointerException.class)
    public void renderAdView_withNullView_shouldThrowNPE() {
        subject.renderAdView(null, nativeResponse);
    }

    @Test(expected = NullPointerException.class)
    public void renderAdView_withNullNativeResponse_shouldThrowNPE() {
        subject.renderAdView(relativeLayout, null);
    }

    @Rule public ExpectedException exception = ExpectedException.none();
    public void renderAdView_withNullViewBinder_shouldThrowNPE() {
        subject = new MoPubNativeAdRenderer(null);

        exception.expect(NullPointerException.class);
        subject.renderAdView(relativeLayout, nativeResponse);
    }

    @Test
    public void renderAdView_shouldReturnPopulatedView() {
        subject.renderAdView(relativeLayout, nativeResponse);

        assertThat(((TextView)relativeLayout.findViewById(titleView.getId())).getText()).isEqualTo("test title");
        assertThat(((TextView)relativeLayout.findViewById(textView.getId())).getText()).isEqualTo(
                "test text");
        assertThat(((TextView)relativeLayout.findViewById(callToActionView.getId())).getText()).isEqualTo("test call to action");

        // not testing images due to testing complexity
    }

    @Test
    public void renderAdView_withFailedViewBinder_shouldReturnFast() {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(badView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        subject = new MoPubNativeAdRenderer(viewBinder);
        subject.renderAdView(relativeLayout, nativeResponse);

        assertThat(((TextView)relativeLayout.findViewById(titleView.getId())).getText()).isEqualTo("");
        assertThat(((TextView)relativeLayout.findViewById(textView.getId())).getText()).isEqualTo(
                "");
        assertThat(((TextView)relativeLayout.findViewById(callToActionView.getId())).getText()).isEqualTo(
                "");
    }

    @Test
    public void getOrCreateNativeViewHolder_withNoViewHolder_shouldCreateNativeViewHolder() {
        final NativeViewHolder viewHolder =
                subject.getOrCreateNativeViewHolder(relativeLayout, viewBinder);

        final NativeViewHolder expectedViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);
        compareNativeViewHolders(expectedViewHolder, viewHolder);
    }

    @Test
    public void getOrCreateNativeViewHolder_withViewHolder_shouldNotReCreateNativeViewHolder() {
        final NativeViewHolder viewHolder =
                subject.getOrCreateNativeViewHolder(relativeLayout, viewBinder);

        assertThat(subject.getOrCreateNativeViewHolder(relativeLayout, viewBinder))
                .isEqualTo(viewHolder);
    }

    static private void compareNativeViewHolders(final NativeViewHolder actualViewHolder,
            final NativeViewHolder expectedViewHolder) {
        assertThat(actualViewHolder.titleView).isEqualTo(expectedViewHolder.titleView);
        assertThat(actualViewHolder.textView).isEqualTo(expectedViewHolder.textView);
        assertThat(actualViewHolder.callToActionView).isEqualTo(expectedViewHolder.callToActionView);
        assertThat(actualViewHolder.mainImageView).isEqualTo(expectedViewHolder.mainImageView);
        assertThat(actualViewHolder.iconImageView).isEqualTo(expectedViewHolder.iconImageView);
    }
}