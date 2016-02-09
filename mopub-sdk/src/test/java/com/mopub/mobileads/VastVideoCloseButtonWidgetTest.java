package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.resource.CloseButtonDrawable;
import com.mopub.network.MaxWidthImageLoader;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.VolleyError;
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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastVideoCloseButtonWidgetTest {
    private Context context;
    private VastVideoCloseButtonWidget subject;

    private static final String ICON_IMAGE_URL = "iconimageurl";

    @Mock
    MoPubRequestQueue mockRequestQueue;
    @Mock
    private MaxWidthImageLoader mockImageLoader;
    @Mock
    private ImageLoader.ImageContainer mockImageContainer;
    @Mock
    private Bitmap mockBitmap;
    @Captor
    private ArgumentCaptor<ImageLoader.ImageListener> imageCaptor;

    @Before
    public void setUp() throws Exception {
        Networking.setRequestQueueForTesting(mockRequestQueue);
        Networking.setImageLoaderForTesting(mockImageLoader);
        context = Robolectric.buildActivity(Activity.class).create().get();
        subject = new VastVideoCloseButtonWidget(context);
    }

    @Test
    public void updateCloseButtonIcon_imageListenerOnResponse_shouldUseImageBitmap() throws Exception {
        when(mockImageContainer.getBitmap()).thenReturn(mockBitmap);

        subject.updateCloseButtonIcon(ICON_IMAGE_URL);

        verify(mockImageLoader).get(eq(ICON_IMAGE_URL), imageCaptor.capture());
        ImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onResponse(mockImageContainer, true);
        assertThat(((BitmapDrawable) subject.getImageView().getDrawable()).getBitmap()).isEqualTo(mockBitmap);
    }

    @Test
    public void updateImage_imageListenerOnResponseWhenReturnedBitMapIsNull_shouldUseDefaultCloseButtonDrawable() throws Exception {
        final ImageView imageViewSpy = spy(subject.getImageView());
        subject.setImageView(imageViewSpy);

        when(mockImageContainer.getBitmap()).thenReturn(null);

        subject.updateCloseButtonIcon(ICON_IMAGE_URL);

        verify(mockImageLoader).get(eq(ICON_IMAGE_URL), imageCaptor.capture());
        ImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onResponse(mockImageContainer, true);
        verify(imageViewSpy, never()).setImageBitmap(any(Bitmap.class));
        assertThat(subject.getImageView().getDrawable()).isInstanceOf(CloseButtonDrawable.class);
    }

    @Test
    public void updateImage_imageListenerOnErrorResponse_shouldUseDefaultCloseButtonDrawable() throws Exception {
        final ImageView imageViewSpy = spy(subject.getImageView());
        subject.setImageView(imageViewSpy);

        subject.updateCloseButtonIcon(ICON_IMAGE_URL);

        verify(mockImageLoader).get(eq(ICON_IMAGE_URL), imageCaptor.capture());
        ImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onErrorResponse(new VolleyError());
        verify(imageViewSpy, never()).setImageBitmap(any(Bitmap.class));
        assertThat(subject.getImageView().getDrawable()).isInstanceOf(CloseButtonDrawable.class);
    }
}
