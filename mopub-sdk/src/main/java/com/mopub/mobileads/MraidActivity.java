package com.mopub.mobileads;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.common.AdReport;
import com.mopub.common.Constants;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.MraidController.MraidListener;
import com.mopub.mraid.MraidController.UseCustomCloseListener;
import com.mopub.mraid.MraidWebViewDebugListener;
import com.mopub.mraid.PlacementType;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.BaseInterstitialActivity.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.mobileads.BaseInterstitialActivity.JavaScriptWebViewCallbacks.WEB_VIEW_DID_CLOSE;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.broadcastAction;

public class MraidActivity extends BaseInterstitialActivity {
    @Nullable private MraidController mMraidController;
    @Nullable private MraidWebViewDebugListener mDebugListener;

    public static void preRenderHtml(@NonNull final Context context,
            @NonNull final CustomEventInterstitialListener customEventInterstitialListener,
            @NonNull final String htmlData) {
        BaseWebView dummyWebView = new BaseWebView(context);

        dummyWebView.enablePlugins(false);
        dummyWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, final String url) {
                customEventInterstitialListener.onInterstitialLoaded();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }

            @Override
            public void onReceivedError(final WebView view, final int errorCode,
                    final String description,
                    final String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                customEventInterstitialListener.onInterstitialFailed(
                        MoPubErrorCode.MRAID_LOAD_ERROR);
            }
        });

        dummyWebView.loadDataWithBaseURL("http://" + Constants.HOST + "/",
                htmlData, "text/html", "UTF-8", null);
    }

    public static void start(@NonNull Context context, @Nullable AdReport adreport, @NonNull String htmlData, long broadcastIdentifier) {
        Intent intent = createIntent(context, adreport, htmlData, broadcastIdentifier);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Log.d("MraidInterstitial", "MraidActivity.class not found. Did you declare MraidActivity in your manifest?");
        }
    }

    @VisibleForTesting
    protected static Intent createIntent(@NonNull Context context, @Nullable AdReport adReport,
            @NonNull String htmlData, long broadcastIdentifier) {
        Intent intent = new Intent(context, MraidActivity.class);
        intent.putExtra(HTML_RESPONSE_BODY_KEY, htmlData);
        intent.putExtra(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        intent.putExtra(AD_REPORT_KEY, adReport);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public View getAdView() {
        String htmlData = getIntent().getStringExtra(HTML_RESPONSE_BODY_KEY);
        if (htmlData == null) {
            MoPubLog.w("MraidActivity received a null HTML body. Finishing the activity.");
            finish();
            return new View(this);
        }

        mMraidController = new MraidController(
                this, mAdReport, PlacementType.INTERSTITIAL);

        mMraidController.setDebugListener(mDebugListener);
        mMraidController.setMraidListener(new MraidListener() {
            @Override
            public void onLoaded(View view) {
                // This is only done for the interstitial. Banners have a different mechanism
                // for tracking third party impressions.
                mMraidController.loadJavascript(WEB_VIEW_DID_APPEAR.getJavascript());
            }

            @Override
            public void onFailedToLoad() {
                MoPubLog.d("MraidActivity failed to load. Finishing the activity");
                broadcastAction(MraidActivity.this, getBroadcastIdentifier(),
                        ACTION_INTERSTITIAL_FAIL);
                finish();
            }

            public void onClose() {
                mMraidController.loadJavascript(WEB_VIEW_DID_CLOSE.getJavascript());
                finish();
            }

            @Override
            public void onExpand() {
                // No-op. The interstitial is always expanded.
            }

            @Override
            public void onOpen() {
                broadcastAction(MraidActivity.this, getBroadcastIdentifier(),
                        ACTION_INTERSTITIAL_CLICK);
            }
        });

        // Needed because the Activity provides the close button, not the controller. This
        // gets called if the creative calls mraid.useCustomClose.
        mMraidController.setUseCustomCloseListener(new UseCustomCloseListener() {
            public void useCustomCloseChanged(boolean useCustomClose) {
                if (useCustomClose) {
                    hideInterstitialCloseButton();
                } else {
                    showInterstitialCloseButton();
                }
            }
        });

        mMraidController.loadContent(htmlData);
        return mMraidController.getAdContainer();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadcastAction(this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_SHOW);

        if (VERSION.SDK_INT >= VERSION_CODES.ICE_CREAM_SANDWICH) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
    }

    @Override
    protected void onPause() {
        if (mMraidController != null) {
            mMraidController.pause(isFinishing());
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMraidController != null) {
            mMraidController.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMraidController != null) {
            mMraidController.destroy();
        }

        broadcastAction(this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_DISMISS);
        super.onDestroy();
    }

    @VisibleForTesting
    public void setDebugListener(@Nullable MraidWebViewDebugListener debugListener) {
        mDebugListener = debugListener;
        if (mMraidController != null) {
            mMraidController.setDebugListener(debugListener);
        }
    }
}
