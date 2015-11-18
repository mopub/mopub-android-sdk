package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.BuildConfig;
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
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class StaticNativeViewHolderTest {
    private Context context;
    private RelativeLayout relativeLayout;
    private ViewBinder viewBinder;
    private TextView titleView;
    private TextView textView;
    private TextView callToActionView;
    private ImageView mainImageView;
    private ImageView iconImageView;
    private TextView extrasTextView;
    private ImageView extrasImageView;
    private ImageView extrasImageView2;
    private ImageView privacyInformationIconImageView;

    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private MaxWidthImageLoader mockImageLoader;
    @Mock private ImageLoader.ImageContainer mockImageContainer;
    @Mock private Bitmap mockBitmap;

    @Captor private ArgumentCaptor<ImageLoader.ImageListener> mainImageCaptor;
    @Captor private ArgumentCaptor<ImageLoader.ImageListener> iconImageCaptor;

    @Before
    public void setUp() throws Exception {

        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        context = Robolectric.buildActivity(Activity.class).create().get();
        relativeLayout = new RelativeLayout(context);
        relativeLayout.setId((int) Utils.generateUniqueId());

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
        privacyInformationIconImageView = new ImageView(context);
        privacyInformationIconImageView.setId((int) Utils.generateUniqueId());

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
        relativeLayout.addView(privacyInformationIconImageView);
    }

    @Test
    public void fromViewBinder_shouldPopulateClassFields() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .textId(textView.getId())
                .callToActionId(callToActionView.getId())
                .mainImageId(mainImageView.getId())
                .iconImageId(iconImageView.getId())
                .privacyInformationIconImageId(privacyInformationIconImageView.getId())
                .build();

        StaticNativeViewHolder staticNativeViewHolder =
                StaticNativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(staticNativeViewHolder.titleView).isEqualTo(titleView);
        assertThat(staticNativeViewHolder.textView).isEqualTo(textView);
        assertThat(staticNativeViewHolder.callToActionView).isEqualTo(callToActionView);
        assertThat(staticNativeViewHolder.mainImageView).isEqualTo(mainImageView);
        assertThat(staticNativeViewHolder.iconImageView).isEqualTo(iconImageView);
        assertThat(staticNativeViewHolder.privacyInformationIconImageView).isEqualTo(
                privacyInformationIconImageView);
    }

    @Test
    public void fromViewBinder_withSubsetOfFields_shouldLeaveOtherFieldsNull() throws Exception {
        viewBinder = new ViewBinder.Builder(relativeLayout.getId())
                .titleId(titleView.getId())
                .iconImageId(iconImageView.getId())
                .build();

        StaticNativeViewHolder staticNativeViewHolder =
                StaticNativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(staticNativeViewHolder.titleView).isEqualTo(titleView);
        assertThat(staticNativeViewHolder.textView).isNull();
        assertThat(staticNativeViewHolder.callToActionView).isNull();
        assertThat(staticNativeViewHolder.mainImageView).isNull();
        assertThat(staticNativeViewHolder.iconImageView).isEqualTo(iconImageView);
        assertThat(staticNativeViewHolder.privacyInformationIconImageView).isNull();
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

        StaticNativeViewHolder staticNativeViewHolder =
                StaticNativeViewHolder.fromViewBinder(relativeLayout, viewBinder);

        assertThat(staticNativeViewHolder.titleView).isNull();
        assertThat(staticNativeViewHolder.textView).isNull();
        assertThat(staticNativeViewHolder.callToActionView).isNull();
        assertThat(staticNativeViewHolder.mainImageView).isNull();
        assertThat(staticNativeViewHolder.iconImageView).isNull();
        assertThat(staticNativeViewHolder.privacyInformationIconImageView).isNull();
    }
}
