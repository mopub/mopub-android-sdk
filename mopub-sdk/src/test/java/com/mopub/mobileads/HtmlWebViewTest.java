// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.webkit.WebViewClient;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SdkTestRunner.class)
public class HtmlWebViewTest {
    private HtmlWebView subject;
    private BaseHtmlWebView.BaseWebViewListener baseWebViewListener;
    private String clickthroughUrl;
    private String dspCreativeId;

    @Before
    public void setup() {
        subject = new HtmlWebView(Robolectric.buildActivity(Activity.class).create().get());
        baseWebViewListener = mock(BaseHtmlWebView.BaseWebViewListener.class);
        clickthroughUrl = "clickthroughUrl";
        dspCreativeId = "dspCreativeId";
    }

    @Test
    public void init_shouldSetupWebViewClient() {
        subject.init(baseWebViewListener, clickthroughUrl, dspCreativeId);
        WebViewClient webViewClient = Shadows.shadowOf(subject).getWebViewClient();
        assertThat(webViewClient).isNotNull();
        assertThat(webViewClient).isInstanceOf(HtmlWebViewClient.class);
    }
}
