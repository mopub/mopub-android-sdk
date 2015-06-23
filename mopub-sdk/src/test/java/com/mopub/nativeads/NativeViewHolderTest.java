package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;
import com.mopub.network.MaxWidthImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.toolbox.ImageLoader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
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

    private static final String IMPRESSION_URL = "http://example.com";
    private static final String CLICK_URL = "http://test.com";
    private static final String AD_UNIT_ID = "http://blah.org";

    @Mock
    private MoPubRequestQueue mockRequestQueue;
    @Mock
    private MaxWidthImageLoader mockImageLoader;
    @Mock
    private ImageLoader.ImageContainer mockImageContainer;
    @Mock
    private Bitmap mockBitmap;

    @Captor
    private ArgumentCaptor<ImageLoader.ImageListener> mainImageCaptor;
    @Captor
    private ArgumentCaptor<ImageLoader.ImageListener> iconImageCaptor;


    @Before
    public void setUp() throws Exception {

        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        context = Robolectric.buildActivity(Activity.class).create().get();
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

        BaseForwardingNativeAd nativeAd = new BaseForwardingNativeAd() {};
        nativeAd.setTitle("titletext");
        nativeAd.setText("texttext");
        nativeAd.setMainImageUrl("mainimageurl");
        nativeAd.setIconImageUrl("iconimageurl");
        nativeAd.setCallToAction("cta");

        nativeResponse = new NativeResponse(context,
                IMPRESSION_URL, CLICK_URL, AD_UNIT_ID, nativeAd, null);

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
        verify(mockImageLoader).get(eq("mainimageurl"), mainImageCaptor.capture());
        verify(mockImageLoader).get(eq("iconimageurl"), iconImageCaptor.capture());

        stub(mockImageContainer.getBitmap()).toReturn(mockBitmap);
        mainImageCaptor.getValue().onResponse(mockImageContainer, true);
        iconImageCaptor.getValue().onResponse(mockImageContainer, true);

        assertThat(((BitmapDrawable) mainImageView.getDrawable()).getBitmap()).isEqualTo(mockBitmap);
        assertThat(((BitmapDrawable) iconImageView.getDrawable()).getBitmap()).isEqualTo(mockBitmap);
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
        nativeResponse = new NativeResponse(context,
                IMPRESSION_URL, CLICK_URL, AD_UNIT_ID, mock(BaseForwardingNativeAd.class), null);

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

        nativeResponse = new NativeResponse(context,
                IMPRESSION_URL, CLICK_URL, AD_UNIT_ID, nativeAd, null);

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

        BaseForwardingNativeAd nativeAd = new BaseForwardingNativeAd() {};
        nativeAd.addExtra("extrastext", "extrastexttext");
        nativeAd.addExtra("extrasimage", "extrasimageurl");
        nativeAd.addExtra("extrasimage2", "extrasimageurl2");

        nativeResponse = new NativeResponse(context,
                IMPRESSION_URL, CLICK_URL, AD_UNIT_ID, nativeAd, null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .addExtra("extrastext", extrasTextView.getId())
                .addExtra("extrasimage", extrasImageView.getId())
                .addExtra("extrasimage2", extrasImageView2.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        nativeViewHolder.updateExtras(relativeLayout, nativeResponse, viewBinder);

        assertThat(extrasTextView.getText()).isEqualTo("extrastexttext");

        verify(mockImageLoader).get(eq("extrasimageurl"), mainImageCaptor.capture());
        verify(mockImageLoader).get(eq("extrasimageurl2"), iconImageCaptor.capture());

        stub(mockImageContainer.getBitmap()).toReturn(mockBitmap);
        mainImageCaptor.getValue().onResponse(mockImageContainer, true);
        iconImageCaptor.getValue().onResponse(mockImageContainer, true);

        assertThat(((BitmapDrawable) extrasImageView.getDrawable()).getBitmap()).isEqualTo(mockBitmap);
        assertThat(((BitmapDrawable) extrasImageView2.getDrawable()).getBitmap()).isEqualTo(mockBitmap);
    }

    @Test
    public void updateExtras_withMissingExtrasValues_shouldClearPreviousValues() throws Exception {
        extrasTextView.setText("previousextrastext");
        extrasImageView.setImageBitmap(BitmapFactory.decodeByteArray("previousextrasimagedata".getBytes(), 0, "previousextrasimagedata".getBytes().length));
        extrasImageView2.setImageBitmap(BitmapFactory.decodeByteArray("previousextrasimagedata2".getBytes(), 0, "previousextrasimagedata2".getBytes().length));

        nativeResponse = new NativeResponse(context,
                IMPRESSION_URL, CLICK_URL, AD_UNIT_ID, new BaseForwardingNativeAd(){}, null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .addExtra("extrastext", extrasTextView.getId())
                .addExtra("extrasimage", extrasImageView.getId())
                .addExtra("extrasimage2", extrasImageView2.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(extrasTextView.getText()).isEqualTo("previousextrastext");

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

        nativeResponse = new NativeResponse(context,
                IMPRESSION_URL, CLICK_URL, AD_UNIT_ID, nativeAd, null);

        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .addExtra("extrastext", extrasImageView.getId())
                .addExtra("extrasimage", extrasTextView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(extrasTextView.getText()).isEqualTo("");
        assertThat(extrasImageView.getDrawable()).isNull();

        nativeViewHolder.updateExtras(relativeLayout, nativeResponse, viewBinder);

        // Volley's imageloader will set this to a bitmapdrawable with no bitmap
        assertThat(extrasTextView.getText()).isEqualTo("extrasimageurl");
        assertThat(extrasImageView.getDrawable()).isNull();
    }

    public void fromViewBinder_withMixedViewTypes_shouldReturnEmptyViewHolder() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(mainImageView.getId())
                .textId(textView.getId())
                .build();

        NativeViewHolder nativeViewHolder =
                NativeViewHolder.fromViewBinder(relativeLayout, viewBinder);
        assertThat(nativeViewHolder).isEqualTo(NativeViewHolder.EMPTY_VIEW_HOLDER);
    }
}
