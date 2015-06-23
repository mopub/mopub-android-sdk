package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
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

import static android.view.Gravity.CENTER_VERTICAL;
import static android.view.Gravity.RIGHT;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class ToolbarWidgetTest {
    private Context context;
    private ToolbarWidget subject;

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
    }

    private void initializeSubject() {
        subject = new ToolbarWidget.Builder(context)
                .weight(1f)
                .widgetGravity(CENTER_VERTICAL | RIGHT)
                .defaultText("Close")
                .drawable(new CloseButtonDrawable())
                .visibility(View.GONE)
                .build();
    }

    @Test
    public void updateImage_imageListenerOnResponse_shouldUseImageBitmap() throws Exception {
        initializeSubject();

        when(mockImageContainer.getBitmap()).thenReturn(mockBitmap);

        subject.updateImage(ICON_IMAGE_URL);

        verify(mockImageLoader).get(eq(ICON_IMAGE_URL), imageCaptor.capture());
        ImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onResponse(mockImageContainer, true);
        assertThat(((BitmapDrawable) subject.getImageViewDrawable()).getBitmap()).isEqualTo(mockBitmap);
    }

    @Test
    public void updateImage_imageListenerOnResponseWhenReturnedBitMapIsNull_shouldUseDefaultCloseButtonDrawable() throws Exception {
        initializeSubject();

        final ImageView imageViewSpy = spy(subject.getImageView());
        subject.setImageView(imageViewSpy);

        when(mockImageContainer.getBitmap()).thenReturn(null);

        subject.updateImage(ICON_IMAGE_URL);

        verify(mockImageLoader).get(eq(ICON_IMAGE_URL), imageCaptor.capture());
        ImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onResponse(mockImageContainer, true);
        verify(imageViewSpy, never()).setImageBitmap(any(Bitmap.class));
        assertThat(subject.getImageViewDrawable()).isInstanceOf(CloseButtonDrawable.class);
    }

    @Test
    public void updateImage_imageListenerOnErrorResponse_shouldUseDefaultCloseButtonDrawable() throws Exception {
        initializeSubject();

        final ImageView imageViewSpy = spy(subject.getImageView());
        subject.setImageView(imageViewSpy);

        subject.updateImage(ICON_IMAGE_URL);

        verify(mockImageLoader).get(eq(ICON_IMAGE_URL), imageCaptor.capture());
        ImageLoader.ImageListener listener = imageCaptor.getValue();
        listener.onErrorResponse(new VolleyError());
        verify(imageViewSpy, never()).setImageBitmap(any(Bitmap.class));
        assertThat(subject.getImageViewDrawable()).isInstanceOf(CloseButtonDrawable.class);
    }
}
