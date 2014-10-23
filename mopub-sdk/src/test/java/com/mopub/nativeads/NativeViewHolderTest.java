package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.CacheService;
import com.mopub.common.DownloadResponse;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;
import com.mopub.nativeads.test.support.MoPubShadowBitmap;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(shadows={MoPubShadowBitmap.class})
public class NativeViewHolderTest {
    private Context context;
    private RelativeLayout relativeLayout;
    private ViewGroup viewGroup;
    private NativeResponse nativeResponse;
    private ViewBinder viewBinder;
    private MoPubNative.MoPubNativeListener mopubNativeListener;
    private TextView titleView;
    private TextView textView;
    private TextView callToActionView;
    private ImageView mainImageView;
    private ImageView iconImageView;
    private TextView extrasTextView;
    private ImageView extrasImageView;
    private ImageView extrasImageView2;
    private String mainImageUrl;
    private String iconImageUrl;
    private String mainImageData;
    private String iconImageData;
    private Bitmap iconImage;
    private Bitmap mainImage;
    private String extrasImageData;
    private String extrasImageData2;
    private Bitmap extrasImage2;
    private Bitmap extrasImage;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        relativeLayout = new RelativeLayout(context);
        relativeLayout.setId((int) Utils.generateUniqueId());
        viewGroup = new LinearLayout(context);

        // Fields in the web ui
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

        // Extras
        extrasTextView = new TextView(context);
        extrasTextView.setId((int) Utils.generateUniqueId());
        extrasImageView = new ImageView(context);
        extrasImageView.setId((int) Utils.generateUniqueId());
        extrasImageView2 = new ImageView(context);
        extrasImageView2.setId((int) Utils.generateUniqueId());

        relativeLayout.addView(titleView);
        relativeLayout.addView(textView);
        relativeLayout.addView(callToActionView);
        relativeLayout.addView(mainImageView);
        relativeLayout.addView(iconImageView);
        relativeLayout.addView(extrasTextView);
        relativeLayout.addView(extrasImageView);
        relativeLayout.addView(extrasImageView2);

        mainImageUrl = "mainimageurl";
        iconImageUrl = "iconimageurl";
        mainImageData = "mainimagedata";
        iconImageData = "iconimagedata";
        extrasImageData = "extrasimagedata";
        extrasImageData2 = "extrasimagedata2";
        iconImage = BitmapFactory.decodeByteArray(iconImageData.getBytes(), 0, iconImageData.getBytes().length);
        mainImage = BitmapFactory.decodeByteArray(mainImageData.getBytes(), 0, mainImageData.getBytes().length);
        extrasImage = BitmapFactory.decodeByteArray(extrasImageData.getBytes(), 0, extrasImageData.getBytes().length);
        extrasImage2 = BitmapFactory.decodeByteArray(extrasImageData2.getBytes(), 0, extrasImageData2.getBytes().length);
    }

    @Test
    public void fromViewBinder_shouldPopulateClassFields() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(textView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(nativeViewHolder.titleView).isEqualTo(titleView);
        assertThat(nativeViewHolder.textView).isEqualTo(textView);
        assertThat(nativeViewHolder.callToActionView).isEqualTo(callToActionView);
        assertThat(nativeViewHolder.mainImageView).isEqualTo(mainImageView);
        assertThat(nativeViewHolder.iconImageView).isEqualTo(iconImageView);
    }

    @Test
    public void fromViewBinder_withSubsetOfFields_shouldLeaveOtherFieldsNull() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(nativeViewHolder.titleView).isEqualTo(titleView);
        assertThat(nativeViewHolder.textView).isNull();
        assertThat(nativeViewHolder.callToActionView).isNull();
        assertThat(nativeViewHolder.mainImageView).isNull();
        assertThat(nativeViewHolder.iconImageView).isEqualTo(iconImageView);
    }

    @Test
    public void fromViewBinder_withNonExistantIds_shouldLeaveFieldsNull() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId((int) Utils.generateUniqueId())
                .textId((int) Utils.generateUniqueId())
                .callToActionId((int) Utils.generateUniqueId())
                .mainImageId((int) Utils.generateUniqueId())
                .iconImageId((int) Utils.generateUniqueId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(nativeViewHolder.titleView).isNull();
        assertThat(nativeViewHolder.textView).isNull();
        assertThat(nativeViewHolder.callToActionView).isNull();
        assertThat(nativeViewHolder.mainImageView).isNull();
        assertThat(nativeViewHolder.iconImageView).isNull();
    }

    @Test
    public void update_shouldAddValuesToViews() throws Exception {
        // Setup for cache state for image gets
        CacheService.initialize(context);
        CacheService.putToBitmapCache(mainImageUrl, mainImage);
        CacheService.putToBitmapCache(iconImageUrl, iconImage);

        BaseForwardingNativeAd nativeAd = new BaseForwardingNativeAd() {};
        nativeAd.setTitle("titletext");
        nativeAd.setText("texttext");
        nativeAd.setMainImageUrl("mainimageurl");
        nativeAd.setIconImageUrl("iconimageurl");
        nativeAd.setCallToAction("cta");

        final DownloadResponse downloadResponse = new DownloadResponse(new TestHttpResponseWithHeaders(200, ""));
        nativeResponse = new NativeResponse(context, downloadResponse, "adunit_id", nativeAd, null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(textView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        nativeViewHolder.update(nativeResponse);

        assertThat(titleView.getText()).isEqualTo("titletext");
        assertThat(textView.getText()).isEqualTo("texttext");
        assertThat(callToActionView.getText()).isEqualTo("cta");
        assertThat(shadowOf(ImageViewServiceTest.getBitmapFromImageView(mainImageView))
                .getCreatedFromBytes()).isEqualTo(mainImageData.getBytes());
        assertThat(shadowOf(ImageViewServiceTest.getBitmapFromImageView(iconImageView))
                .getCreatedFromBytes()).isEqualTo(iconImageData.getBytes());
    }

    @Test
    public void update_withMissingNativeResponseFields_shouldClearPreviousValues() throws Exception {
        // Set previous values that should be cleared
        titleView.setText("previoustitletext");
        textView.setText("previoustexttext");
        callToActionView.setText("previousctatext");
        mainImageView.setImageBitmap(BitmapFactory.decodeByteArray("previousmainimagedata".getBytes(), 0, "previousmainimagedata".getBytes().length));
        iconImageView.setImageBitmap(BitmapFactory.decodeByteArray("previousiconimagedata".getBytes(), 0, "previousiconimagedata".getBytes().length));

        // Only required fields in native response
        final DownloadResponse downloadResponse = new DownloadResponse(new TestHttpResponseWithHeaders(200, ""));
        nativeResponse = new NativeResponse(context, downloadResponse, "adunit_id", mock(BaseForwardingNativeAd.class), null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(textView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        nativeViewHolder.update(nativeResponse);

        assertThat(titleView.getText()).isEqualTo("");
        assertThat(textView.getText()).isEqualTo("");
        assertThat(callToActionView.getText()).isEqualTo("");
        assertThat(mainImageView.getDrawable()).isNull();
        assertThat(iconImageView.getDrawable()).isNull();
    }

    @Test
    public void update_withDifferentViewBinder_shouldNotClearPreviousValues() throws Exception {
        // Set previous values that should be cleared
        titleView.setText("previoustitletext");
        textView.setText("previoustexttext");

        BaseForwardingNativeAd nativeAd = new BaseForwardingNativeAd() {};
        nativeAd.setCallToAction("cta");
        final DownloadResponse downloadResponse = new DownloadResponse(new TestHttpResponseWithHeaders(200, ""));
        nativeResponse = new NativeResponse(context, downloadResponse, "adunit_id", nativeAd, null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .callToActionId(callToActionView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        nativeViewHolder.update(nativeResponse);

        assertThat(titleView.getText()).isEqualTo("previoustitletext");
        assertThat(textView.getText()).isEqualTo("previoustexttext");
        assertThat(callToActionView.getText()).isEqualTo("cta");
    }

    @Test
    public void updateExtras_shouldAddValuesToViews() throws Exception {
        // Setup for cache state for image gets
        CacheService.initialize(context);
        CacheService.putToBitmapCache("extrasimageurl", extrasImage);
        CacheService.putToBitmapCache("extrasimageurl2", extrasImage2);

        BaseForwardingNativeAd nativeAd = new BaseForwardingNativeAd() {};
        nativeAd.addExtra("extrastext", "extrastexttext");
        nativeAd.addExtra("extrasimage", "extrasimageurl");
        nativeAd.addExtra("extrasimage2", "extrasimageurl2");
        final DownloadResponse downloadResponse = new DownloadResponse(new TestHttpResponseWithHeaders(200, ""));
        nativeResponse = new NativeResponse(context, downloadResponse, "adunit_id", nativeAd, null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .addExtra("extrastext", extrasTextView.getId())
                .addExtra("extrasimage", extrasImageView.getId())
                .addExtra("extrasimage2", extrasImageView2.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        nativeViewHolder.updateExtras(relativeLayout, nativeResponse, viewBinder);

        assertThat(extrasTextView.getText()).isEqualTo("extrastexttext");
        assertThat(shadowOf(ImageViewServiceTest.getBitmapFromImageView(extrasImageView))
                .getCreatedFromBytes()).isEqualTo("extrasimagedata".getBytes());
        assertThat(shadowOf(ImageViewServiceTest.getBitmapFromImageView(extrasImageView2))
                .getCreatedFromBytes()).isEqualTo("extrasimagedata2".getBytes());
    }

    @Test
    public void updateExtras_withMissingExtrasValues_shouldClearPreviousValues() throws Exception {
        extrasTextView.setText("previousextrastext");
        extrasImageView.setImageBitmap(BitmapFactory.decodeByteArray("previousextrasimagedata".getBytes(), 0, "previousextrasimagedata".getBytes().length));
        extrasImageView2.setImageBitmap(BitmapFactory.decodeByteArray("previousextrasimagedata2".getBytes(), 0, "previousextrasimagedata2".getBytes().length));

        final DownloadResponse downloadResponse = new DownloadResponse(new TestHttpResponseWithHeaders(200, ""));
        nativeResponse = new NativeResponse(context, downloadResponse, "adunit_id", new BaseForwardingNativeAd(){}, null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .addExtra("extrastext", extrasTextView.getId())
                .addExtra("extrasimage", extrasImageView.getId())
                .addExtra("extrasimage2", extrasImageView2.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(extrasTextView.getText()).isEqualTo("previousextrastext");
        assertThat(shadowOf(ImageViewServiceTest.getBitmapFromImageView(extrasImageView))
                .getCreatedFromBytes()).isEqualTo("previousextrasimagedata".getBytes());
        assertThat(shadowOf(ImageViewServiceTest.getBitmapFromImageView(extrasImageView2))
                .getCreatedFromBytes()).isEqualTo("previousextrasimagedata2".getBytes());

        nativeViewHolder.updateExtras(relativeLayout, nativeResponse, viewBinder);

        assertThat(extrasTextView.getText()).isEqualTo("");
        assertThat(extrasImageView.getDrawable()).isNull();
        assertThat(extrasImageView2.getDrawable()).isNull();
    }

    @Test
    public void updateExtras_withMismatchingViewTypes_shouldSetTextViewToImageUrlAndSetExtrasImageViewToNull() throws Exception {
        BaseForwardingNativeAd nativeAd = new BaseForwardingNativeAd() {};
        nativeAd.addExtra("extrastext", "extrastexttext");
        nativeAd.addExtra("extrasimage", "extrasimageurl");

        final DownloadResponse downloadResponse = new DownloadResponse(new TestHttpResponseWithHeaders(200, ""));
        nativeResponse = new NativeResponse(context, downloadResponse, "adunit_id", nativeAd, null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .addExtra("extrastext", extrasImageView.getId())
                .addExtra("extrasimage", extrasTextView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(extrasTextView.getText()).isEqualTo("");
        assertThat(extrasImageView.getDrawable()).isNull();

        nativeViewHolder.updateExtras(relativeLayout, nativeResponse, viewBinder);

        assertThat(extrasTextView.getText()).isEqualTo("extrasimageurl");
        assertThat(extrasImageView.getDrawable()).isNull();
    }

    @Test
    public void fromViewBinder_withMixedViewTypes_shouldReturnNull() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(mainImageView.getId())
                .textId(textView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(nativeViewHolder).isNull();
    }
}
