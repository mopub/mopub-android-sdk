// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.DataKeys;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.HtmlControllerFactory;
import com.mopub.mobileads.factories.MraidControllerFactory;
import com.mopub.mraid.PlacementType;
import com.mopub.mraid.WebViewDebugListener;

import java.util.Map;

import static com.mopub.common.Constants.AD_EXPIRATION_DELAY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CLICKED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.EXPIRED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.common.util.JavaScriptWebViewCallbacks.WEB_VIEW_DID_APPEAR;
import static com.mopub.mobileads.MoPubErrorCode.INLINE_LOAD_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.INLINE_SHOW_ERROR;

public class MoPubInline extends BaseAd {
    public static final String ADAPTER_NAME = MoPubInline.class.getSimpleName();
    @Nullable
    private Context mContext;
    @Nullable
    private AdData mAdData;
    @Nullable
    private MoPubWebViewController mController;
    @Nullable
    private WebViewDebugListener mDebugListener;
    @Nullable
    private ExternalViewabilitySessionManager mExternalViewabilitySessionManager;
    @Nullable
    private Handler mHandler;
    @Nullable
    private Runnable mAdExpiration;

    @Override
    public void load(@NonNull final Context context,
                     @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        MoPubLog.log(MoPubLog.AdLogEvent.LOAD_ATTEMPTED, ADAPTER_NAME);

        mContext = context;
        mAdData = adData;
        mHandler = new Handler();
        mAdExpiration = () -> {
            MoPubLog.log(EXPIRED, ADAPTER_NAME, "time in seconds");
            mInteractionListener.onAdFailed(MoPubErrorCode.EXPIRED);
        };

        String clickthroughUrl;
        Map<String, String> extras = mAdData.getExtras();
        String dspCreativeId = mAdData.getDspCreativeId();

        if (extrasAreValid(extras)) {
            clickthroughUrl = extras.get(DataKeys.CLICKTHROUGH_URL_KEY);
        } else {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    INLINE_LOAD_ERROR.getIntCode(),
                    INLINE_LOAD_ERROR);
            mLoadListener.onAdLoadFailed(INLINE_LOAD_ERROR);
            return;
        }

        if ("mraid".equals(mAdData.getAdType())) {
            mController = MraidControllerFactory.create(
                    mContext,
                    dspCreativeId,
                    PlacementType.INLINE,
                    mAdData.getAllowCustomClose());
        } else if ("html".equals(mAdData.getAdType())) {
            mController = HtmlControllerFactory.create(context, dspCreativeId, clickthroughUrl);
        } else {
            // We can only handle MRAID and HTML here. Anything else should fail
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    INLINE_LOAD_ERROR.getIntCode(),
                    INLINE_LOAD_ERROR);
            mLoadListener.onAdLoadFailed(INLINE_LOAD_ERROR);
            return;
        }

        mController.setDebugListener(mDebugListener);
        mController.setMoPubWebViewListener(new BaseHtmlWebView.BaseWebViewListener() {
            @Override
            public void onLoaded(final View view) {
                // Honoring the server dimensions forces the WebView to be the size of the banner
                AdViewController.setShouldHonorServerDimensions(view);
                MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
                mLoadListener.onAdLoaded();
                mHandler.postDelayed(mAdExpiration, AD_EXPIRATION_DELAY);
            }

            @Override
            public void onFailedToLoad(final MoPubErrorCode errorCode) {
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                        INLINE_LOAD_ERROR.getIntCode(),
                        INLINE_LOAD_ERROR);
                mLoadListener.onAdLoadFailed(INLINE_LOAD_ERROR);
            }

            @Override
            public void onFailed() {
                MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                        INLINE_SHOW_ERROR.getIntCode(),
                        INLINE_SHOW_ERROR);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(INLINE_SHOW_ERROR);
                }
            }

            @Override
            public void onRenderProcessGone(@NonNull final MoPubErrorCode errorCode) {
                MoPubLog.log(LOAD_FAILED, ADAPTER_NAME, errorCode.getIntCode(), errorCode);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdFailed(errorCode);
                }
            }

            @Override
            public void onClicked() {
                MoPubLog.log(CLICKED, ADAPTER_NAME);
                if (mInteractionListener != null) {
                    mInteractionListener.onAdClicked();
                }
            }

            @Override
            public void onExpand() {
                if (mInteractionListener != null) {
                    mInteractionListener.onAdExpanded();
                }
            }

            @Override
            public void onResize(final boolean toOriginalSize) {
                if (mInteractionListener == null) {
                    return;
                }
                if (toOriginalSize) {
                    mInteractionListener.onAdResumeAutoRefresh();
                } else {
                    mInteractionListener.onAdPauseAutoRefresh();
                }
            }

            @Override
            public void onClose() {
                if (mInteractionListener != null) {
                    mInteractionListener.onAdCollapsed();
                }
            }
        });

        mController.fillContent(mAdData.getAdPayload(), new MoPubWebViewController.WebViewCacheListener() {
            @Override
            public void onReady(final @NonNull BaseWebView webView,
                                final @Nullable ExternalViewabilitySessionManager viewabilityManager) {
                webView.getSettings().setJavaScriptEnabled(true);

                // We only measure viewability when we have an activity context. This sets up a delayed
                // viewability session if we have the new pixel-counting banner impression tracking enabled.
                // Otherwise, set up a regular display session.
                if (context instanceof Activity) {
                    mExternalViewabilitySessionManager = new ExternalViewabilitySessionManager(
                            context);
                    mExternalViewabilitySessionManager.createDisplaySession(context, webView);
                }
            }
        });
    }

    @Override
    @Nullable
    protected View getAdView() {
        return (mController != null)
                ? mController.getAdContainer()
                : null;
    }

    @Override
    protected void onInvalidate() {
        if (mHandler != null && mAdExpiration != null) {
            mHandler.removeCallbacks(mAdExpiration);
        }

        mAdExpiration = null;
        mHandler = null;

        if (mExternalViewabilitySessionManager != null) {
            mExternalViewabilitySessionManager.endDisplaySession();
            mExternalViewabilitySessionManager = null;
        }
        if (mController != null) {
            mController.setMoPubWebViewListener(null);
            mController.destroy();
            mController = null;
        }
    }

    @Override
    protected void trackMpxAndThirdPartyImpressions() {
        if (mController == null) {
            return;
        }

        mController.loadJavascript(WEB_VIEW_DID_APPEAR.getJavascript());

        // mExternalViewabilitySessionManager is usually only null if the original Context given
        // to mMraidController was not an Activity Context. We don't need to start the deferred
        // viewability tracker since it wasn't created, and if it was, and the activity reference
        // was lost, something bad has happened, so we should drop the request.
        if (mExternalViewabilitySessionManager != null) {
            final Activity activity = mController.getWeakActivity().get();
            if (activity != null) {
                mExternalViewabilitySessionManager.startDeferredDisplaySession(activity);
            } else {
                MoPubLog.log(CUSTOM, ADAPTER_NAME, "Lost the activity for deferred Viewability tracking. Dropping session.");
            }
        }
    }

    @VisibleForTesting
    public void setDebugListener(@Nullable final WebViewDebugListener debugListener) {
        mDebugListener = debugListener;
        if (mController != null) {
            mController.setDebugListener(debugListener);
        }
    }

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        return null;
    }

    @NonNull
    @Override
    protected String getAdNetworkId() {
        return (mAdData != null && mAdData.getAdUnit() != null)
                ? mAdData.getAdUnit()
                : "";
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity, @NonNull final AdData adData) {
        return false;
    }

    private boolean extrasAreValid(@NonNull final Map<String, String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }
}
