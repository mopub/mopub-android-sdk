package com.mopub.mraid;

import android.support.annotation.NonNull;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.mopub.common.VisibleForTesting;

/**
 * Debugging callback interface to make it easier for integration tests to debug MRAID ads.
 */
@VisibleForTesting
public interface MraidWebViewDebugListener {
    /**
     * @see WebChromeClient#onJsAlert(WebView, String, String, JsResult)
     */
    boolean onJsAlert(@NonNull String message, @NonNull JsResult result);

    /**
     * @see WebChromeClient#onConsoleMessage(ConsoleMessage)
     */
    boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage);
}
