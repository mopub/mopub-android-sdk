package com.mopub.mobileads;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.common.AdReport;
import com.mopub.common.CreativeOrientation;
import com.mopub.common.DataKeys;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.mobileads.factories.HtmlInterstitialWebViewFactory;

import java.io.Serializable;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.mopub.common.DataKeys.CREATIVE_ORIENTATION_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.DataKeys.REDIRECT_URL_KEY;
import static com.mopub.common.DataKeys.SCROLLABLE_KEY;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_CLOSE;
import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.broadcastAction;
import static com.mopub.mobileads.HtmlWebViewClient.MOPUB_FAIL_LOAD;
import static com.mopub.mobileads.HtmlWebViewClient.MOPUB_FINISH_LOAD;

public class MoPubActivity extends BaseInterstitialActivity {
    @Nullable private HtmlInterstitialWebView mHtmlInterstitialWebView;
    @Nullable private ExternalViewabilitySessionManager mExternalViewabilitySessionManager;

    public static void start(Context context, String htmlData, AdReport adReport,
            boolean isScrollable, String redirectUrl, String clickthroughUrl,
            CreativeOrientation creativeOrientation, long broadcastIdentifier) {
        Intent intent = createIntent(context, htmlData, adReport, isScrollable,
                redirectUrl, clickthroughUrl, creativeOrientation, broadcastIdentifier);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException anfe) {
            Log.d("MoPubActivity", "MoPubActivity not found - did you declare it in AndroidManifest.xml?");
        }
    }

    static Intent createIntent(Context context,
            String htmlData, AdReport adReport, boolean isScrollable, String redirectUrl,
            String clickthroughUrl, CreativeOrientation orientation, long broadcastIdentifier) {
        Intent intent = new Intent(context, MoPubActivity.class);
        intent.putExtra(HTML_RESPONSE_BODY_KEY, htmlData);
        intent.putExtra(SCROLLABLE_KEY, isScrollable);
        intent.putExtra(CLICKTHROUGH_URL_KEY, clickthroughUrl);
        intent.putExtra(REDIRECT_URL_KEY, redirectUrl);
        intent.putExtra(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        intent.putExtra(AD_REPORT_KEY, adReport);
        intent.putExtra(CREATIVE_ORIENTATION_KEY, orientation);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    static void preRenderHtml(final Interstitial baseInterstitial,
            final Context context,
            final AdReport adReport,
            final CustomEventInterstitialListener customEventInterstitialListener,
            final String htmlData,
            final boolean isScrollable,
            final String redirectUrl,
            final String clickthroughUrl,
            final long broadcastIdentifier) {
        final HtmlInterstitialWebView htmlInterstitialWebView = HtmlInterstitialWebViewFactory.create(
                context.getApplicationContext(), adReport, customEventInterstitialListener,
                isScrollable, redirectUrl, clickthroughUrl);

        htmlInterstitialWebView.enablePlugins(false);
        htmlInterstitialWebView.enableJavascriptCaching();

        htmlInterstitialWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (MOPUB_FINISH_LOAD.equals(url)) {
                    customEventInterstitialListener.onInterstitialLoaded();
                } else if (MOPUB_FAIL_LOAD.equals(url)) {
                    customEventInterstitialListener.onInterstitialFailed(null);
                }

                return true;
            }
        });

        final ExternalViewabilitySessionManager externalViewabilitySessionManager =
                new ExternalViewabilitySessionManager(context);
        externalViewabilitySessionManager.createDisplaySession(context, htmlInterstitialWebView, true);

        htmlInterstitialWebView.loadHtmlResponse(htmlData);
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier, baseInterstitial,
                htmlInterstitialWebView, externalViewabilitySessionManager);
    }

    @Override
    public View getAdView() {
        Intent intent = getIntent();
        boolean isScrollable = intent.getBooleanExtra(SCROLLABLE_KEY, false);
        String redirectUrl = intent.getStringExtra(REDIRECT_URL_KEY);
        String clickthroughUrl = intent.getStringExtra(CLICKTHROUGH_URL_KEY);
        String htmlResponse = intent.getStringExtra(HTML_RESPONSE_BODY_KEY);

        final Long broadcastIdentifier = getBroadcastIdentifier();
        if (broadcastIdentifier != null) {
            // If a cache hit happens, the content is already loaded; therefore, this re-initializes
            // the WebView with a new {@link BroadcastingInterstitialListener}, enables plugins,
            // and fires the impression tracker.
            final WebViewCacheService.Config config =
                    WebViewCacheService.popWebViewConfig(broadcastIdentifier);
            if (config != null && config.getWebView() instanceof HtmlInterstitialWebView) {
                mHtmlInterstitialWebView = (HtmlInterstitialWebView) config.getWebView();
                mHtmlInterstitialWebView.init(new BroadcastingInterstitialListener(), isScrollable,
                        redirectUrl, clickthroughUrl, mAdReport != null ? mAdReport.getDspCreativeId(): null);
                mHtmlInterstitialWebView.enablePlugins(true);
                mHtmlInterstitialWebView.loadUrl(WEB_VIEW_DID_APPEAR.getUrl());

                mExternalViewabilitySessionManager = config.getViewabilityManager();

                return mHtmlInterstitialWebView;
            }
        }

        MoPubLog.d("WebView cache miss. Recreating the WebView.");
        mHtmlInterstitialWebView = HtmlInterstitialWebViewFactory.create(getApplicationContext(),
                mAdReport, new BroadcastingInterstitialListener(), isScrollable, redirectUrl, clickthroughUrl);
        
        mExternalViewabilitySessionManager = new ExternalViewabilitySessionManager(this);
        mExternalViewabilitySessionManager.createDisplaySession(this, mHtmlInterstitialWebView, true);
        mHtmlInterstitialWebView.loadHtmlResponse(htmlResponse);
        return mHtmlInterstitialWebView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lock the device orientation
        Serializable orientationExtra = getIntent().getSerializableExtra(DataKeys.CREATIVE_ORIENTATION_KEY);
        CreativeOrientation requestedOrientation;
        if (orientationExtra == null || !(orientationExtra instanceof CreativeOrientation)) {
            requestedOrientation = CreativeOrientation.UNDEFINED;
        } else {
            requestedOrientation = (CreativeOrientation) orientationExtra;
        }
        DeviceUtils.lockOrientation(this, requestedOrientation);

        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.startDeferredDisplaySession(this);
        }
        broadcastAction(this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_SHOW);
    }

    @Override
    protected void onDestroy() {
        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.endDisplaySession();
            mExternalViewabilitySessionManager = null;
        }
        if (mHtmlInterstitialWebView != null) {
            mHtmlInterstitialWebView.loadUrl(WEB_VIEW_DID_CLOSE.getUrl());
            mHtmlInterstitialWebView.destroy();
        }
        broadcastAction(this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_DISMISS);
        super.onDestroy();
    }

    class BroadcastingInterstitialListener implements CustomEventInterstitialListener {
        @Override
        public void onInterstitialLoaded() {
            if (mHtmlInterstitialWebView != null) {
                mHtmlInterstitialWebView.loadUrl(WEB_VIEW_DID_APPEAR.getUrl());
            }
        }

        @Override
        public void onInterstitialFailed(MoPubErrorCode errorCode) {
            broadcastAction(MoPubActivity.this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_FAIL);
            finish();
        }

        @Override
        public void onInterstitialShown() {
        }

        @Override
        public void onInterstitialClicked() {
            broadcastAction(MoPubActivity.this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_CLICK);
        }

        @Override
        public void onLeaveApplication() {
        }

        @Override
        public void onInterstitialDismissed() {
        }
    }
}
