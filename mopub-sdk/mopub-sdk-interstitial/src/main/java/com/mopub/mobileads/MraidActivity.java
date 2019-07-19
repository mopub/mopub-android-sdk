// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;

import com.mopub.common.AdReport;
import com.mopub.common.Constants;
import com.mopub.common.CreativeOrientation;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.DeviceUtils;
import com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import com.mopub.mraid.MraidBridge;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.MraidController.MraidListener;
import com.mopub.mraid.MraidController.UseCustomCloseListener;
import com.mopub.mraid.MraidWebViewClient;
import com.mopub.mraid.MraidWebViewDebugListener;
import com.mopub.mraid.PlacementType;
import com.mopub.network.Networking;

import java.io.Serializable;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.CREATIVE_ORIENTATION_KEY;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_CLICK;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.common.IntentActions.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.DID_APPEAR;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_FAILED;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.SHOW_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdLogEvent.WILL_DISAPPEAR;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_CLOSE;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.broadcastAction;
import static com.mopub.mobileads.HtmlWebViewClient.MOPUB_FAIL_LOAD;

public class MraidActivity extends BaseInterstitialActivity {
    @Nullable private MraidController mMraidController;
    @Nullable private MraidWebViewDebugListener mDebugListener;
    @Nullable protected ExternalViewabilitySessionManager mExternalViewabilitySessionManager;

    public static void preRenderHtml(@NonNull final Interstitial mraidInterstitial,
            @NonNull final Context context,
            @NonNull final CustomEventInterstitialListener customEventInterstitialListener,
            @NonNull final Long broadcastIdentifier,
            @Nullable final AdReport adReport) {
        Preconditions.checkNotNull(mraidInterstitial);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(customEventInterstitialListener);
        Preconditions.checkNotNull(broadcastIdentifier);

        preRenderHtml(mraidInterstitial, customEventInterstitialListener, getResponseString(adReport),
                new MraidBridge.MraidWebView(context), broadcastIdentifier,
                new MraidController(context, adReport, PlacementType.INTERSTITIAL));
    }

    @VisibleForTesting
    static void preRenderHtml(@NonNull final Interstitial mraidInterstitial,
            @NonNull final CustomEventInterstitialListener customEventInterstitialListener,
            @Nullable final String htmlData,
            @NonNull final BaseWebView mraidWebView,
            @NonNull final Long broadcastIdentifier,
            @NonNull final MraidController mraidController) {
        MoPubLog.log(LOAD_ATTEMPTED);
        Preconditions.checkNotNull(mraidInterstitial);
        Preconditions.checkNotNull(customEventInterstitialListener);
        Preconditions.checkNotNull(mraidWebView);
        Preconditions.checkNotNull(broadcastIdentifier);
        Preconditions.checkNotNull(mraidController);

        mraidWebView.enablePlugins(false);
        mraidWebView.enableJavascriptCaching();
        final Context context = mraidWebView.getContext();

        mraidWebView.setWebViewClient(new MraidWebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (MOPUB_FAIL_LOAD.equals(url)) {
                    MoPubLog.log(LOAD_FAILED, MoPubErrorCode.VIDEO_CACHE_ERROR.getIntCode(),
                            MoPubErrorCode.VIDEO_CACHE_ERROR);
                    customEventInterstitialListener.onInterstitialFailed(
                            MoPubErrorCode.MRAID_LOAD_ERROR);
                }
                return true;
            }

            @Override
            public void onPageFinished(final WebView view, final String url) {
                MoPubLog.log(LOAD_SUCCESS);
                customEventInterstitialListener.onInterstitialLoaded();
                mraidController.onPreloadFinished(mraidWebView);
            }

            @Override
            public void onReceivedError(final WebView view, final int errorCode,
                    final String description,
                    final String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                MoPubLog.log(LOAD_FAILED, MoPubErrorCode.VIDEO_CACHE_ERROR.getIntCode(),
                        MoPubErrorCode.VIDEO_CACHE_ERROR);
                customEventInterstitialListener.onInterstitialFailed(
                        MoPubErrorCode.MRAID_LOAD_ERROR);
            }
        });

        final ExternalViewabilitySessionManager externalViewabilitySessionManager =
                new ExternalViewabilitySessionManager(context);
        externalViewabilitySessionManager.createDisplaySession(context, mraidWebView, true);

        mraidWebView.loadDataWithBaseURL(Networking.getBaseUrlScheme() + "://" + Constants.HOST + "/",
                htmlData, "text/html", "UTF-8", null);

        WebViewCacheService.storeWebViewConfig(broadcastIdentifier, mraidInterstitial,
                mraidWebView, externalViewabilitySessionManager, mraidController);
    }

    public static void start(@NonNull final Context context,
            @Nullable final AdReport adreport,
            final long broadcastIdentifier,
            @Nullable final CreativeOrientation orientation) {
        MoPubLog.log(SHOW_ATTEMPTED);
        final Intent intent = createIntent(context, adreport, broadcastIdentifier,
                orientation);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            MoPubLog.log(SHOW_FAILED, MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                    MoPubErrorCode.INTERNAL_ERROR);
            Log.d("MraidInterstitial", "MraidActivity.class not found. Did you declare MraidActivity in your manifest?");
        }
    }

    @VisibleForTesting
    protected static Intent createIntent(@NonNull final Context context,
            @Nullable final AdReport adReport,
            final long broadcastIdentifier,
            @Nullable final CreativeOrientation orientation) {
        Intent intent = new Intent(context, MraidActivity.class);
        intent.putExtra(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        intent.putExtra(AD_REPORT_KEY, adReport);
        intent.putExtra(CREATIVE_ORIENTATION_KEY, orientation);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @Override
    public View getAdView() {
        final String htmlData = getResponseString();
        if (TextUtils.isEmpty(htmlData)) {
            MoPubLog.log(CUSTOM, "MraidActivity received a null HTML body. Finishing the activity.");
            finish();
            return new View(this);
        }

        final Long broadcastIdentifier = getBroadcastIdentifier();
        WebViewCacheService.Config config = null;
        if (broadcastIdentifier != null) {
            config = WebViewCacheService.popWebViewConfig(broadcastIdentifier);
        }

        if (config != null && config.getController() != null) {
            mMraidController = config.getController();
        } else {
            mMraidController = new MraidController(
                    this, mAdReport, PlacementType.INTERSTITIAL);
        }

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
                MoPubLog.log(CUSTOM, "MraidActivity failed to load. Finishing the activity");
                if (getBroadcastIdentifier() != null) {
                    broadcastAction(MraidActivity.this, getBroadcastIdentifier(),
                            ACTION_INTERSTITIAL_FAIL);
                }
                finish();
            }

            @Override
            public void onRenderProcessGone(@NonNull final MoPubErrorCode errorCode) {
                MoPubLog.log(CUSTOM, "Finishing the activity due to a problem: " + errorCode);
                finish();
            }

            public void onClose() {
                MoPubLog.log(WILL_DISAPPEAR);
                mMraidController.loadJavascript(WEB_VIEW_DID_CLOSE.getJavascript());
                finish();
            }

            @Override
            public void onExpand() {
                // No-op. The interstitial is always expanded.
            }

            @Override
            public void onResize(final boolean toOriginalSize) {
                // No-op. The interstitial is always expanded.
            }

            @Override
            public void onOpen() {
                MoPubLog.log(DID_APPEAR);
                if (getBroadcastIdentifier()!= null) {
                    broadcastAction(MraidActivity.this, getBroadcastIdentifier(),
                            ACTION_INTERSTITIAL_CLICK);
                }
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

        if (config != null) {
            mExternalViewabilitySessionManager = config.getViewabilityManager();
        } else {
            mMraidController.fillContent(htmlData,
                    new MraidController.MraidWebViewCacheListener() {
                        @Override
                        public void onReady(@NonNull final MraidBridge.MraidWebView webView,
                                @Nullable final ExternalViewabilitySessionManager viewabilityManager) {
                            if (viewabilityManager != null) {
                                mExternalViewabilitySessionManager = viewabilityManager;
                            } else {
                                mExternalViewabilitySessionManager = new ExternalViewabilitySessionManager(MraidActivity.this);
                                mExternalViewabilitySessionManager.createDisplaySession(MraidActivity.this, webView, true);
                            }
                        }
                    });
        }

        return mMraidController.getAdContainer();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MoPubLog.log(SHOW_SUCCESS);

        final Serializable orientationExtra = getIntent().getSerializableExtra(
                CREATIVE_ORIENTATION_KEY);
        CreativeOrientation requestedOrientation = CreativeOrientation.DEVICE;
        if (orientationExtra instanceof CreativeOrientation) {
            requestedOrientation = (CreativeOrientation) orientationExtra;
        }
        DeviceUtils.lockOrientation(this, requestedOrientation);

        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.startDeferredDisplaySession(this);
        }
        if (getBroadcastIdentifier()!= null) {
            broadcastAction(this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_SHOW);
        }

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

        if (mMraidController != null) {
            mMraidController.onShow(this);
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
        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.endDisplaySession();
            mExternalViewabilitySessionManager = null;
        }
        if (mMraidController != null) {
            mMraidController.destroy();
        }

        if (getBroadcastIdentifier()!= null) {
            broadcastAction(this, getBroadcastIdentifier(), ACTION_INTERSTITIAL_DISMISS);
        }
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
