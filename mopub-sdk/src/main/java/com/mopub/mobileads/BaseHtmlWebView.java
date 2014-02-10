/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import static com.mopub.mobileads.ViewGestureDetector.UserClickListener;
import static com.mopub.mobileads.util.VersionCode.ICE_CREAM_SANDWICH;
import static com.mopub.mobileads.util.VersionCode.currentApiLevel;


public class BaseHtmlWebView extends BaseWebView implements UserClickListener {
    private final ViewGestureDetector mViewGestureDetector;
    private boolean mClicked;

    public BaseHtmlWebView(Context context, AdConfiguration adConfiguration) {
        super(context);

        //RHT: This is to disable hardware acceleration, which is believed to be the cause of signal 11 SIGSEGV.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        disableScrollingAndZoom();
        getSettings().setJavaScriptEnabled(true);

        mViewGestureDetector = new ViewGestureDetector(context, this, adConfiguration);
        mViewGestureDetector.setUserClickListener(this);

        if (currentApiLevel().isAtLeast(ICE_CREAM_SANDWICH)) {
            enablePlugins(true);
        }
        setBackgroundColor(Color.TRANSPARENT);
    }

    public void init(boolean isScrollable) {
        initializeOnTouchListener(isScrollable);
    }

    @Override
    public void loadUrl(String url) {
        if (url == null) return;

        Log.d("MoPub", "Loading url: " + url);
        if (url.startsWith("javascript:")) {
            // If destroy has been called on this webview, we won't be able to navigate
            try {
                super.loadUrl(url);
            } catch (NullPointerException e) {
                Log.e("MoPub", "Attempt to call javascript on destroyed WebView", e);
            }
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

    void initializeOnTouchListener(final boolean isScrollable) {
        setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mViewGestureDetector.sendTouchEvent(event);

                // We're not handling events if the current action is ACTION_MOVE
                return (event.getAction() == MotionEvent.ACTION_MOVE) && !isScrollable;
            }
        });
    }

    @Override
    public void onUserClick() {
        mClicked = true;
    }

    @Override
    public void onResetUserClick() {
        mClicked = false;
    }

    @Override
    public boolean wasClicked() {
        return mClicked;
    }
}
