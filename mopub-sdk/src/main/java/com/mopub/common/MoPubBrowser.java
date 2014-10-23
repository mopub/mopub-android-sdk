package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
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

import com.mopub.common.logging.MoPubLog;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static com.mopub.common.util.Drawables.BACKGROUND;
import static com.mopub.common.util.Drawables.CLOSE;
import static com.mopub.common.util.Drawables.LEFT_ARROW;
import static com.mopub.common.util.Drawables.REFRESH;
import static com.mopub.common.util.Drawables.RIGHT_ARROW;
import static com.mopub.common.util.Drawables.UNLEFT_ARROW;
import static com.mopub.common.util.Drawables.UNRIGHT_ARROW;
import static com.mopub.common.util.IntentUtils.deviceCanHandleIntent;
import static com.mopub.common.util.IntentUtils.isDeepLink;

public class MoPubBrowser extends Activity {
    public static final String DESTINATION_URL_KEY = "URL";
    private static final int INNER_LAYOUT_ID = 1;

    private WebView mWebView;
    private ImageButton mBackButton;
    private ImageButton mForwardButton;
    private ImageButton mRefreshButton;
    private ImageButton mCloseButton;

    public static void open(final Context context, final String url) {
        MoPubLog.d("Opening url in MoPubBrowser: " + url);
        final Intent intent = new Intent(context, MoPubBrowser.class);
        intent.putExtra(DESTINATION_URL_KEY, url);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(Activity.RESULT_OK);

        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

        setContentView(getMoPubBrowserView());

        initializeWebView();
        initializeButtons();
        enableCookies();
    }

    private void initializeWebView() {
        WebSettings webSettings = mWebView.getSettings();

        webSettings.setJavaScriptEnabled(true);

        /**
         * Pinch to zoom is apparently not enabled by default on all devices, so
         * declare zoom support explicitly.
         * http://stackoverflow.com/questions/5125851/enable-disable-zoom-in-android-webview
         */
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setUseWideViewPort(true);

        mWebView.loadUrl(getIntent().getStringExtra(DESTINATION_URL_KEY));
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description,
                    String failingUrl) {
                Toast.makeText(MoPubBrowser.this, "MoPubBrowser error: " + description, Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) {
                    return false;
                }

                final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                if (isDeepLink(url) && deviceCanHandleIntent(MoPubBrowser.this, intent)) {
                    startActivity(intent);
                    finish();
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mForwardButton.setImageDrawable(UNRIGHT_ARROW.decodeImage(MoPubBrowser.this));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                Drawable backImageDrawable = view.canGoBack()
                        ? LEFT_ARROW.decodeImage(MoPubBrowser.this)
                        : UNLEFT_ARROW.decodeImage(MoPubBrowser.this);
                mBackButton.setImageDrawable(backImageDrawable);

                Drawable forwardImageDrawable = view.canGoForward()
                        ? RIGHT_ARROW.decodeImage(MoPubBrowser.this)
                        : UNRIGHT_ARROW.decodeImage(MoPubBrowser.this);
                mForwardButton.setImageDrawable(forwardImageDrawable);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView webView, int progress) {
                setTitle("Loading...");
                setProgress(progress * 100);
                if (progress == 100) {
                    setTitle(webView.getUrl());
                }
            }
        });
    }

    private void initializeButtons() {
        mBackButton.setBackgroundColor(Color.TRANSPARENT);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
            }
        });

        mForwardButton.setBackgroundColor(Color.TRANSPARENT);
        mForwardButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mWebView.canGoForward()) {
                    mWebView.goForward();
                }
            }
        });

        mRefreshButton.setBackgroundColor(Color.TRANSPARENT);
        mRefreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mWebView.reload();
            }
        });

        mCloseButton.setBackgroundColor(Color.TRANSPARENT);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MoPubBrowser.this.finish();
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

    private View getMoPubBrowserView() {
        LinearLayout moPubBrowserView = new LinearLayout(this);
        LinearLayout.LayoutParams browserLayoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        moPubBrowserView.setLayoutParams(browserLayoutParams);
        moPubBrowserView.setOrientation(LinearLayout.VERTICAL);

        RelativeLayout outerLayout = new RelativeLayout(this);
        LinearLayout.LayoutParams outerLayoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        outerLayout.setLayoutParams(outerLayoutParams);
        moPubBrowserView.addView(outerLayout);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setId(INNER_LAYOUT_ID);
        RelativeLayout.LayoutParams innerLayoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
        innerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        innerLayout.setLayoutParams(innerLayoutParams);
        innerLayout.setBackgroundDrawable(BACKGROUND.decodeImage(this));
        outerLayout.addView(innerLayout);

        mBackButton = getButton(LEFT_ARROW.decodeImage(this));
        mForwardButton = getButton(RIGHT_ARROW.decodeImage(this));
        mRefreshButton = getButton(REFRESH.decodeImage(this));
        mCloseButton = getButton(CLOSE.decodeImage(this));

        innerLayout.addView(mBackButton);
        innerLayout.addView(mForwardButton);
        innerLayout.addView(mRefreshButton);
        innerLayout.addView(mCloseButton);

        mWebView = new WebView(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
        layoutParams.addRule(RelativeLayout.ABOVE, INNER_LAYOUT_ID);
        mWebView.setLayoutParams(layoutParams);
        outerLayout.addView(mWebView);

        return moPubBrowserView;
    }

    private ImageButton getButton(final Drawable drawable) {
        ImageButton imageButton = new ImageButton(this);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1f);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        imageButton.setLayoutParams(layoutParams);

        imageButton.setImageDrawable(drawable);

        return imageButton;
    }
}
