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

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import static android.view.ViewGroup.LayoutParams.FILL_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.mopub.mobileads.resource.Drawables.BACKGROUND;
import static com.mopub.mobileads.resource.Drawables.CLOSE;
import static com.mopub.mobileads.resource.Drawables.LEFT_ARROW;
import static com.mopub.mobileads.resource.Drawables.REFRESH;
import static com.mopub.mobileads.resource.Drawables.RIGHT_ARROW;
import static com.mopub.mobileads.resource.Drawables.UNLEFT_ARROW;
import static com.mopub.mobileads.resource.Drawables.UNRIGHT_ARROW;

public class MraidBrowser extends Activity {
    
    public static final String URL_EXTRA = "extra_url";
    public static final int INNER_LAYOUT_ID = 1;
    private WebView mWebView;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageButton mRefreshButton;
    private ImageButton mCloseButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
        
        setContentView(getMraidBrowserView());
        
        Intent intent = getIntent();
        initializeWebView(intent);
        initializeButtons();
        enableCookies();
    }

    private void initializeWebView(Intent intent) {
        WebSettings webSettings = mWebView.getSettings();
        
        webSettings.setJavaScriptEnabled(true);
        
        /* Pinch to zoom is apparently not enabled by default on all devices, so
         * declare zoom support explicitly.
         * http://stackoverflow.com/questions/5125851/enable-disable-zoom-in-android-webview
         */
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);
        
        mWebView.loadUrl(intent.getStringExtra(URL_EXTRA));
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description,
                                        String failingUrl) {
                Activity a = (Activity) view.getContext();
                Toast.makeText(a, "MRAID error: " + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;

                Uri uri = Uri.parse(url);
                String host = uri.getHost();

                if ((url.startsWith("http:") || url.startsWith("https:"))
                        && !"play.google.com".equals(host)
                        && !"market.android.com".equals(host)) {
                    return false;
                }

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (ActivityNotFoundException exception) {
                    Log.w("MoPub", "Unable to start activity for " + url + ". " +
                            "Ensure that your phone can handle this intent.");
                }

                finish();
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mForwardButton.setImageDrawable(UNRIGHT_ARROW.decodeImage(MraidBrowser.this));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                Drawable backImageDrawable = view.canGoBack()
                        ? LEFT_ARROW.decodeImage(MraidBrowser.this)
                        : UNLEFT_ARROW.decodeImage(MraidBrowser.this);
                mBackButton.setImageDrawable(backImageDrawable);

                Drawable forwardImageDrawable = view.canGoForward()
                        ? RIGHT_ARROW.decodeImage(MraidBrowser.this)
                        : UNRIGHT_ARROW.decodeImage(MraidBrowser.this);
                mForwardButton.setImageDrawable(forwardImageDrawable);
            }
        });
        
        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                Activity a = (Activity) view.getContext();
                a.setTitle("Loading...");
                a.setProgress(progress * 100);
                if (progress == 100) a.setTitle(view.getUrl());
            }
        });
    }
    
    private void initializeButtons() {
        mBackButton.setBackgroundColor(Color.TRANSPARENT);
        mBackButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mWebView.canGoBack()) mWebView.goBack();
            }
        });
        
        mForwardButton.setBackgroundColor(Color.TRANSPARENT);
        mForwardButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (mWebView.canGoForward()) mWebView.goForward();
            }
        });
        
        mRefreshButton.setBackgroundColor(Color.TRANSPARENT);
        mRefreshButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mWebView.reload();
            }
        });
        
        mCloseButton.setBackgroundColor(Color.TRANSPARENT);
        mCloseButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                MraidBrowser.this.finish();
            }
        });
    }
    
    private void enableCookies() {
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }

    private View getMraidBrowserView() {
        LinearLayout mraidBrowserView = new LinearLayout(this);
        LinearLayout.LayoutParams browserLayoutParams = new LinearLayout.LayoutParams(FILL_PARENT, FILL_PARENT);
        mraidBrowserView.setLayoutParams(browserLayoutParams);
        mraidBrowserView.setOrientation(LinearLayout.VERTICAL);

        RelativeLayout outerLayout = new RelativeLayout(this);
        LinearLayout.LayoutParams outerLayoutParams = new LinearLayout.LayoutParams(FILL_PARENT, WRAP_CONTENT);
        outerLayout.setLayoutParams(outerLayoutParams);
        mraidBrowserView.addView(outerLayout);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setId(INNER_LAYOUT_ID);
        RelativeLayout.LayoutParams innerLayoutParams = new RelativeLayout.LayoutParams(FILL_PARENT, WRAP_CONTENT);
        innerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        innerLayout.setLayoutParams(innerLayoutParams);
        innerLayout.setBackgroundDrawable(BACKGROUND.decodeImage(MraidBrowser.this));
        outerLayout.addView(innerLayout);

        mBackButton = getButton(LEFT_ARROW.decodeImage(MraidBrowser.this));
        mForwardButton = getButton(RIGHT_ARROW.decodeImage(MraidBrowser.this));
        mRefreshButton = getButton(REFRESH.decodeImage(MraidBrowser.this));
        mCloseButton = getButton(CLOSE.decodeImage(MraidBrowser.this));

        innerLayout.addView(mBackButton);
        innerLayout.addView(mForwardButton);
        innerLayout.addView(mRefreshButton);
        innerLayout.addView(mCloseButton);

        mWebView = new WebView(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(FILL_PARENT, FILL_PARENT);
        layoutParams.addRule(RelativeLayout.ABOVE, INNER_LAYOUT_ID);
        mWebView.setLayoutParams(layoutParams);
        outerLayout.addView(mWebView);

        return mraidBrowserView;
    }

    private ImageButton getButton(Drawable drawable) {
        ImageButton imageButton = new ImageButton(this);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1f);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        imageButton.setLayoutParams(layoutParams);

        imageButton.setImageDrawable(drawable);

        return imageButton;
    }
}
