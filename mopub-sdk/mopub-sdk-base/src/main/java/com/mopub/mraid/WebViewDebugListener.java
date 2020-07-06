// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mraid;

import androidx.annotation.NonNull;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.mopub.common.VisibleForTesting;

/**
 * Debugging callback interface to make it easier for integration tests to debug MRAID ads.
 */
@VisibleForTesting
public interface WebViewDebugListener {
    /**
     * @see WebChromeClient#onJsAlert(WebView, String, String, JsResult)
     */
    boolean onJsAlert(@NonNull String message, @NonNull JsResult result);

    /**
     * @see WebChromeClient#onConsoleMessage(ConsoleMessage)
     */
    boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage);
}
