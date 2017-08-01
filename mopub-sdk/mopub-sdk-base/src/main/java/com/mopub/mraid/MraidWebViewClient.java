package com.mopub.mraid;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.mobileads.resource.MraidJavascript;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;

/**
 * Handles injecting the MRAID javascript when encountering mraid.js urls
 */
public class MraidWebViewClient extends WebViewClient {

    private static final String MRAID_JS = "mraid.js";
    private static final String MRAID_INJECTION_JAVASCRIPT = "javascript:"
            + MraidJavascript.JAVASCRIPT_SOURCE;

    @SuppressWarnings("deprecation") // new method will simply call this one
    @Override
    public WebResourceResponse shouldInterceptRequest(@NonNull final WebView view,
            @NonNull final String url) {
        if (matchesInjectionUrl(url)) {
            return createMraidInjectionResponse();
        } else {
            return super.shouldInterceptRequest(view, url);
        }
    }

    @VisibleForTesting
    boolean matchesInjectionUrl(@NonNull final String url) {
        final Uri uri = Uri.parse(url.toLowerCase(Locale.US));
        return MRAID_JS.equals(uri.getLastPathSegment());
    }

    private WebResourceResponse createMraidInjectionResponse() {
        InputStream data = new ByteArrayInputStream(MRAID_INJECTION_JAVASCRIPT.getBytes());
        return new WebResourceResponse("text/javascript", "UTF-8", data);
    }
}
