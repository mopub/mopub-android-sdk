package com.mopub.avid;

import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class AvidUtil {

    public static WebView findWebView(View view) {
        if(view == null) {
            return null;
        }
        if(view instanceof WebView) {
            return (WebView)view;
        }
        if(!(view instanceof ViewGroup)) {
            return null;
        }
        WebView webView = null;
        final ViewGroup viewGroup = (ViewGroup)view;
        final int count = viewGroup.getChildCount();
        for(int i = count - 1; i >=0 ; i--) {
            View child = viewGroup.getChildAt(i);
            webView = findWebView(child);
            if(webView != null) {
                break;
            }
        }
        return webView;
    }

}
