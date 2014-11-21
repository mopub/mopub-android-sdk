package com.mopub.mobileads;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.mopub.mobileads.MraidView.ExpansionStyle;
import com.mopub.mobileads.MraidView.NativeCloseButtonStyle;
import com.mopub.mobileads.MraidView.PlacementType;
import com.mopub.mobileads.MraidView.ViewState;
import com.mopub.mobileads.factories.MraidViewFactory;
import com.mopub.mobileads.util.WebViews;

import static com.mopub.common.util.VersionCode.ICE_CREAM_SANDWICH;
import static com.mopub.common.util.VersionCode.currentApiLevel;
import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.BaseInterstitialActivity.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.mobileads.BaseInterstitialActivity.JavaScriptWebViewCallbacks.WEB_VIEW_DID_CLOSE;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.broadcastAction;

public class MraidActivity extends BaseInterstitialActivity {
    private MraidView mMraidView;

    static void preRenderHtml(final Context context, final CustomEventInterstitial.CustomEventInterstitialListener customEventInterstitialListener, final String htmlData) {
        MraidView dummyMraidView = MraidViewFactory.create(context, null, ExpansionStyle.DISABLED, NativeCloseButtonStyle.ALWAYS_VISIBLE, PlacementType.INTERSTITIAL);

        dummyMraidView.enablePlugins(false);
        dummyMraidView.setMraidListener(new MraidView.MraidListener() {
            @Override
            public void onReady(MraidView view) {
                customEventInterstitialListener.onInterstitialLoaded();
            }

            @Override
            public void onFailure(MraidView view) {
                customEventInterstitialListener.onInterstitialFailed(null);
            }

            @Override
            public void onExpand(MraidView view) {
            }

            @Override
            public void onOpen(MraidView view) {
            }

            @Override
            public void onClose(MraidView view, MraidView.ViewState newViewState) {
            }
        });
        dummyMraidView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                customEventInterstitialListener.onInterstitialLoaded();
            }
        });
        dummyMraidView.loadHtmlData(htmlData);
    }

    public static void start(Context context, String htmlData, AdConfiguration adConfiguration) {
        Intent intent = createIntent(context, htmlData, adConfiguration);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException anfe) {
            Log.d("MraidInterstitial", "MraidActivity.class not found. Did you declare MraidActivity in your manifest?");
        }
    }

    private static Intent createIntent(Context context, String htmlData, AdConfiguration adConfiguration) {
        Intent intent = new Intent(context, MraidActivity.class);
        intent.putExtra(HTML_RESPONSE_BODY_KEY, htmlData);
        intent.putExtra(AD_CONFIGURATION_KEY, adConfiguration);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public View getAdView() {
        mMraidView = MraidViewFactory.create(this, getAdConfiguration(), ExpansionStyle.DISABLED, NativeCloseButtonStyle.AD_CONTROLLED, PlacementType.INTERSTITIAL);

        mMraidView.setMraidListener(new MraidView.BaseMraidListener(){
            public void onReady(MraidView view) {
                mMraidView.loadUrl(WEB_VIEW_DID_APPEAR.getUrl());
                showInterstitialCloseButton();
            }

            @Override
            public void onOpen(MraidView view) {
                broadcastAction(MraidActivity.this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_CLICK);
            }

            public void onClose(MraidView view, ViewState newViewState) {
                mMraidView.loadUrl(WEB_VIEW_DID_CLOSE.getUrl());
                finish();
            }
        });

        mMraidView.setOnCloseButtonStateChange(new MraidView.OnCloseButtonStateChangeListener() {
            public void onCloseButtonStateChange(MraidView view, boolean enabled) {
                if (enabled) {
                    showInterstitialCloseButton();
                } else {
                    hideInterstitialCloseButton();
                }
            }
        });

        String source = getIntent().getStringExtra(HTML_RESPONSE_BODY_KEY);
        mMraidView.loadHtmlData(source);

        return mMraidView;
    }

    @TargetApi(11)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        broadcastAction(this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_SHOW);

        if (currentApiLevel().isAtLeast(ICE_CREAM_SANDWICH)) {
            getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        addCloseEventRegion();
    }

    @Override
    protected void onPause() {
        super.onPause();
        WebViews.onPause(mMraidView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WebViews.onResume(mMraidView);
    }

    @Override
    protected void onDestroy() {
        mMraidView.destroy();
        broadcastAction(this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_DISMISS);
        super.onDestroy();
    }
}
