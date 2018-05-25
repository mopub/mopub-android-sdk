package com.mopub.mobileads.util;

import android.os.Build;
import android.support.annotation.NonNull;
import android.webkit.CookieManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

public class WebViews {
    public static void onPause(@NonNull final WebView webView, boolean isFinishing) {
        // XXX
        // We need to call WebView#stopLoading and WebView#loadUrl here due to an Android
        // bug where the audio of an HTML5 video will continue to play after the activity has been
        // destroyed. The web view must stop then load an invalid url during the onPause lifecycle
        // event in order to stop the audio.
        if (isFinishing) {
            webView.stopLoading();
            webView.loadUrl("");
        }

        webView.onPause();
    }

    public static void setDisableJSChromeClient(@NonNull final WebView webView) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(@NonNull final WebView view, @NonNull final String url,
                    @NonNull final String message, @NonNull final JsResult result) {
                MoPubLog.d(message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsConfirm(@NonNull final WebView view, @NonNull final String url,
                    @NonNull final String message, @NonNull final JsResult result) {
                MoPubLog.d(message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsPrompt(@NonNull final WebView view, @NonNull final String url,
                    @NonNull final String message, @NonNull final String defaultValue,
                    @NonNull final JsPromptResult result) {
                MoPubLog.d(message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsBeforeUnload(@NonNull final WebView view, @NonNull final String url,
                    @NonNull final String message, @NonNull final JsResult result) {
                MoPubLog.d(message);
                result.confirm();
                return true;
            }
        });
    }

    public static void manageWebCookies() {
        CookieManager cookieManager = CookieManager.getInstance();
        if (MoPub.canCollectPersonalInformation()) {
            cookieManager.setAcceptCookie(true);
            CookieManager.setAcceptFileSchemeCookies(true);
            return;
        }

        // remove all cookies
        cookieManager.setAcceptCookie(false);
        CookieManager.setAcceptFileSchemeCookies(false);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeSessionCookies(null);
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
        } else {
            cookieManager.removeSessionCookie();
            cookieManager.removeAllCookie();
        }
    }

    public static void manageThirdPartyCookies(@NonNull final WebView webView){
        Preconditions.checkNotNull(webView);

        CookieManager cookieManager = CookieManager.getInstance();
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, MoPub.canCollectPersonalInformation());
        }
    }
}
