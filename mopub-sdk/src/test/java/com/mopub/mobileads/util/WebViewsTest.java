package com.mopub.mobileads.util;

import android.webkit.WebView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class WebViewsTest {

    @Test
    public void pause_withIsFinishingTrue_shouldStopLoading_shouldLoadBlankUrl_shouldPauseWebView() throws Exception {
        WebView mockWebView = mock(WebView.class);

        WebViews.onPause(mockWebView, true);

        verify(mockWebView).stopLoading();
        verify(mockWebView).loadUrl("");
        verify(mockWebView).onPause();
    }

    @Test
    public void pause_withIsFinishingFalse_shouldPauseWebView() throws Exception {
        WebView mockWebView = mock(WebView.class);

        WebViews.onPause(mockWebView, false);

        verify(mockWebView, never()).stopLoading();
        verify(mockWebView, never()).loadUrl("");
        verify(mockWebView).onPause();
    }
}
