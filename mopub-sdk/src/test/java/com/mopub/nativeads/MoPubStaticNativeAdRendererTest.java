package com.mopub.nativeads;

import android.app.Activity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;
import com.mopub.nativeads.MoPubCustomEventNative.MoPubStaticNativeAd;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SdkTestRunner.class)
public class MoPubStaticNativeAdRendererTest {
    private MoPubStaticNativeAdRenderer subject;
    private StaticNativeAd mStaticNativeAd;
    private RelativeLayout relativeLayout;
    private ViewGroup viewGroup;
    private ViewBinder viewBinder;
    private TextView titleView;
    private TextView textView;
    private TextView callToActionView;
    private ImageView mainImageView;
    private ImageView iconImageView;
    private ImageView badView;

    @Before
    public void setUp() throws Exception {
        Activity context = Robolectric.buildActivity(Activity.class).create().get();
        relativeLayout = new RelativeLayout(context);
        relativeLayout.setId((int) Utils.generateUniqueId());
        viewGroup = new LinearLayout(context);

        mStaticNativeAd = new StaticNativeAd() {};
        mStaticNativeAd.setTitle("test title");
        mStaticNativeAd.setText("test text");
        mStaticNativeAd.setCallToAction("test call to action");
        mStaticNativeAd.setClickDestinationUrl("destinationUrl");

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

        subject = new MoPubStaticNativeAdRenderer(viewBinder);
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

        assertThat(((TextView)relativeLayout.findViewById(titleView.getId())).getText())
                .isEqualTo("test title");
        assertThat(((TextView)relativeLayout.findViewById(textView.getId())).getText())
                .isEqualTo("test text");
        assertThat(((TextView)relativeLayout.findViewById(callToActionView.getId())).getText())
                .isEqualTo("test call to action");

        // not testing images due to testing complexity
    }

    @Test
    public void renderAdView_withFailedViewBinder_shouldReturnEmptyViews() {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(badView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        subject = new MoPubStaticNativeAdRenderer(viewBinder);
        subject.renderAdView(relativeLayout, mStaticNativeAd);

        assertThat(((TextView)relativeLayout.findViewById(titleView.getId())).getText())
                .isEqualTo("");
        assertThat(((TextView)relativeLayout.findViewById(textView.getId())).getText())
                .isEqualTo("");
        assertThat(((TextView)relativeLayout.findViewById(callToActionView.getId())).getText())
                .isEqualTo("");
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
    }

    @Test
    public void supports_withInstanceOfBaseForwardingNativeAd_shouldReturnTrue() throws Exception {
        assertThat(subject.supports(new StaticNativeAd() {})).isTrue();
        assertThat(subject.supports(mock(MoPubStaticNativeAd.class))).isTrue();
        assertThat(subject.supports(mock(BaseNativeAd.class))).isFalse();
    }
}