// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class HtmlControllerTest {

    @Mock private BaseHtmlWebView.BaseWebViewListener mockWebViewListener;

    private Activity activity;
    private HtmlController subject;

    @Before
    public void setUp() {
        WebViewCacheService.clearAll();

        activity = spy(Robolectric.buildActivity(Activity.class).create().get());

        subject = new HtmlController(activity, "", "fake_html_click_url");
        subject.setMoPubWebViewListener(mockWebViewListener);
        subject.fillContent("fake_html_data", null);
    }

    @Test
    public void getAdContainer_whenWebViewNotNull_shouldReturnWebView() {
        final BaseWebView mockBaseWebView = mock(BaseWebView.class);
        subject.setWebView(mockBaseWebView);

        assertThat(subject.getAdContainer()).isEqualTo(mockBaseWebView);
    }

    @Test
    public void getAdContainer_whenWebViewNull_shouldReturnDefaultAdContainer() {
        subject.setWebView(null);

        assertThat(subject.getAdContainer()).isEqualTo(subject.mDefaultAdContainer);
    }

    @Test
    public void destroy_shouldDestroyWebView_shouldSetWebViewToNull() {
        final BaseWebView mockBaseWebView = mock(BaseWebView.class);
        subject.setWebView(mockBaseWebView);

        subject.destroy();

        verify(mockBaseWebView).destroy();
        assertThat(subject.getWebView()).isNull();
    }



}
