package com.mopub.mobileads.util;

import android.util.Log;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.lang.reflect.Method;

public class WebViews {
    private static final String LOGTAG = "MoPub - WebViewsUtil";

    public static void onPause(WebView webView) {
        try {
            Method onPause = WebView.class.getDeclaredMethod("onPause");
            onPause.invoke(webView);
        } catch (Exception e) {
            // can't call this before API level 11
            return;
        }
    }

    public static void onResume(WebView webView) {
        try {
            Method onResume = WebView.class.getDeclaredMethod("onResume");
            onResume.invoke(webView);
        } catch (Exception e) {
            // can't call this before API level 11
            return;
        }
    }

    public static void setDisableJSChromeClient(WebView webView) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d(LOGTAG, message);
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                Log.d(LOGTAG, message);
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                Log.d(LOGTAG, message);
                return true;
            }

            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                Log.d(LOGTAG, message);
                return true;
            }
        });
    }
}
