package com.mopub.nativeads;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.MoPubCustomEventNative.MoPubStaticNativeAd;
import com.mopub.network.MaxWidthImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.toolbox.ImageLoader;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubStaticNativeAdRendererTest {
    private MoPubStaticNativeAdRenderer subject;
    private StaticNativeAd mStaticNativeAd;
    @Mock private RelativeLayout relativeLayout;
    @Mock private ViewGroup viewGroup;
    private ViewBinder viewBinder;
    @Mock private TextView titleView;
    @Mock private TextView textView;
    @Mock private TextView callToActionView;
    @Mock private ImageView mainImageView;
    @Mock private ImageView iconImageView;
    @Mock private ImageView privacyInformationIconImageView;
    @Mock private ImageView badView;
    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private MaxWidthImageLoader mockImageLoader;
    @Mock private ImageLoader.ImageContainer mockImageContainer;

    @Before
    public void setUp() throws Exception {
        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        stub(mockImageContainer.getBitmap()).toReturn(mock(Bitmap.class));

        when(relativeLayout.getId()).thenReturn((int) Utils.generateUniqueId());

        mStaticNativeAd = new StaticNativeAd() {};
        mStaticNativeAd.setTitle("test title");
        mStaticNativeAd.setText("test text");
        mStaticNativeAd.setCallToAction("test call to action");
        mStaticNativeAd.setClickDestinationUrl("destinationUrl");
        mStaticNativeAd.setMainImageUrl("testUrl");
        mStaticNativeAd.setIconImageUrl("testUrl");

        setViewIdInLayout(titleView, relativeLayout);
        setViewIdInLayout(textView, relativeLayout);
        setViewIdInLayout(callToActionView, relativeLayout);
        setViewIdInLayout(mainImageView, relativeLayout);
        setViewIdInLayout(iconImageView, relativeLayout);
        setViewIdInLayout(privacyInformationIconImageView, relativeLayout);
        setViewIdInLayout(badView, relativeLayout);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(textView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .privacyInformationIconImageId(privacyInformationIconImageView.getId())
                .build();

        subject = new MoPubStaticNativeAdRenderer(viewBinder);
    }

    private void setViewIdInLayout(View mockView, RelativeLayout mockLayout) {
        int id = (int) Utils.generateUniqueId();
        when(mockView.getId()).thenReturn(id);
        when(relativeLayout.findViewById(eq(id))).thenReturn(mockView);
    }

    @Test(expected = NullPointerException.class)
    public void createAdView_withNullContext_shouldThrowNPE() {
        subject.createAdView(null, viewGroup);
    }

    @Test(expected = NullPointerException.class)
    public void renderAdView_withNullView_shouldThrowNPE() {
        subject.renderAdView(null, mStaticNativeAd);
    }

    @Test(expected = NullPointerException.class)
    public void renderAdView_withNullNativeAd_shouldThrowNPE() {
        subject.renderAdView(relativeLayout, null);
    }

    @Rule public ExpectedException exception = ExpectedException.none();

    @Test
    public void renderAdView_withNullViewBinder_shouldThrowNPE() {
        subject = new MoPubStaticNativeAdRenderer(null);

        exception.expect(NullPointerException.class);
        subject.renderAdView(relativeLayout, mStaticNativeAd);
    }

    @Test
    public void renderAdView_shouldReturnPopulatedView() {
        subject.renderAdView(relativeLayout, mStaticNativeAd);

        verify(titleView).setText(eq("test title"));
        verify(textView).setText(eq("test text"));
        verify(callToActionView).setText(eq("test call to action"));

        // not testing images due to testing complexity
    }

    @Test
    public void renderAdView_withFailedViewBinder_shouldNotWriteViews() {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(badView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        subject = new MoPubStaticNativeAdRenderer(viewBinder);
        subject.renderAdView(relativeLayout, mStaticNativeAd);

        verify(titleView, never()).setText(anyString());
        verify(textView, never()).setText(anyString());
        verify(callToActionView, never()).setText(anyString());
        verify(mainImageView, times(2)).getId();
        verifyNoMoreInteractions(mainImageView);
        verify(iconImageView, times(2)).getId();
        verifyNoMoreInteractions(iconImageView);
    }

    @Test
    public void renderAdView_withNoViewHolder_shouldCreateNativeViewHolder() {
        subject.renderAdView(relativeLayout, mStaticNativeAd);

        StaticNativeViewHolder expectedViewHolder = StaticNativeViewHolder.fromViewBinder(relativeLayout, viewBinder);
        StaticNativeViewHolder viewHolder = subject.mViewHolderMap.get(relativeLayout);
        compareNativeViewHolders(expectedViewHolder, viewHolder);
    }

    @Test
    public void getOrCreateNativeViewHolder_withViewHolder_shouldNotReCreateNativeViewHolder() {
        subject.renderAdView(relativeLayout, mStaticNativeAd);
        StaticNativeViewHolder expectedViewHolder = subject.mViewHolderMap.get(relativeLayout);
        subject.renderAdView(relativeLayout, mStaticNativeAd);

        StaticNativeViewHolder viewHolder = subject.mViewHolderMap.get(relativeLayout);
        assertThat(viewHolder).isEqualTo(expectedViewHolder);
    }

    static private void compareNativeViewHolders(final StaticNativeViewHolder actualViewHolder,
            final StaticNativeViewHolder expectedViewHolder) {
        assertThat(actualViewHolder.titleView).isEqualTo(expectedViewHolder.titleView);
        assertThat(actualViewHolder.textView).isEqualTo(expectedViewHolder.textView);
        assertThat(actualViewHolder.callToActionView).isEqualTo(expectedViewHolder.callToActionView);
        assertThat(actualViewHolder.mainImageView).isEqualTo(expectedViewHolder.mainImageView);
        assertThat(actualViewHolder.iconImageView).isEqualTo(expectedViewHolder.iconImageView);
        assertThat(actualViewHolder.privacyInformationIconImageView).isEqualTo(
                expectedViewHolder.privacyInformationIconImageView);
    }

    @Test
    public void supports_withCorrectInstanceOfBaseNativeAd_shouldReturnTrue() throws Exception {
        assertThat(subject.supports(new StaticNativeAd() {})).isTrue();
        assertThat(subject.supports(mock(MoPubStaticNativeAd.class))).isTrue();
        assertThat(subject.supports(mock(BaseNativeAd.class))).isFalse();
        assertThat(subject.supports(mock(MoPubCustomEventVideoNative.MoPubVideoNativeAd.class)))
                .isFalse();
    }
}
