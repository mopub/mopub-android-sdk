package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class BaseHtmlWebView extends BaseWebView {
    public BaseHtmlWebView(Context context) {
        super(context);

        //RHT: This is to disable hardware acceleration, which is believed to be the cause of signal 11 SIGSEGV.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        disableScrollingAndZoom();
        getSettings().setJavaScriptEnabled(true);
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void init(boolean isScrollable) {
        setWebViewScrollingEnabled(isScrollable);
    }

    @Override
    public void loadUrl(String url) {
        if (url == null) return;

        Log.d("MoPub", "Loading url: " + url);
        if (url.startsWith("javascript:")) {
            super.loadUrl(url);
        }
    }

    private void disableScrollingAndZoom() {
        setHorizontalScrollBarEnabled(false);
        setHorizontalScrollbarOverlay(false);
        setVerticalScrollBarEnabled(false);
        setVerticalScrollbarOverlay(false);
        getSettings().setSupportZoom(false);
    }

    void loadHtmlResponse(String htmlResponse) {
        loadDataWithBaseURL("http://ads.mopub.com/", htmlResponse, "text/html", "utf-8", null);
    }

    void setWebViewScrollingEnabled(boolean isScrollable) {
        if (isScrollable) {
            setOnTouchListener(null);
        } else {
            setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    return (event.getAction() == MotionEvent.ACTION_MOVE);
                }
            });
        }
    }
}
