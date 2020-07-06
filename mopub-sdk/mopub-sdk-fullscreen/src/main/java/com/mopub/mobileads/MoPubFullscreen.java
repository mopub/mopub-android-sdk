// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.common.CacheService;
import com.mopub.common.CreativeOrientation;
import com.mopub.common.DataKeys;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.FullAdType;
import com.mopub.common.LifecycleListener;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;
import com.mopub.mobileads.factories.HtmlControllerFactory;
import com.mopub.mobileads.factories.VastManagerFactory;
import com.mopub.mraid.MraidBridge;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.PlacementType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.mopub.common.Constants.AD_EXPIRATION_DELAY;
import static com.mopub.common.DataKeys.CLICKTHROUGH_URL_KEY;
import static com.mopub.common.DataKeys.CREATIVE_ORIENTATION_KEY;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.CUSTOM_WITH_THROWABLE;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.EXPIRED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_FAILED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.LOAD_SUCCESS;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_ATTEMPTED;
import static com.mopub.common.logging.MoPubLog.AdapterLogEvent.SHOW_FAILED;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
import static com.mopub.mobileads.MoPubErrorCode.FULLSCREEN_LOAD_ERROR;

/**
 * MoPubFullscreen unifies all rewarded and non-rewarded fullscreen base ads for HTML, MRAID,
 * and VAST ad types.
 */
public class MoPubFullscreen extends BaseAd implements VastManager.VastManagerListener {
    public static final String ADAPTER_NAME = MoPubFullscreen.class.getSimpleName();

    private static final String HTML = "html";
    private static final String MRAID = "mraid";

    @Nullable
    private EventForwardingBroadcastReceiver mBroadcastReceiver;
    @Nullable
    private Context mContext;
    private long mBroadcastIdentifier;
    @Nullable
    AdData mAdData;
    private ExternalViewabilitySessionManager mExternalViewabilitySessionManager;
    @Nullable
    private VastManager mVastManager;
    @Nullable
    private JSONObject mVideoTrackers;
    @Nullable
    private Map<String, String> mExternalViewabilityTrackers;
    @Nullable
    private Handler mHandler;
    @Nullable
    private Runnable mAdExpiration;
    private boolean mReady;

    @Nullable
    @Override
    protected LifecycleListener getLifecycleListener() {
        // This base ad does not need additional lifecycle listeners.
        return null;
    }

    @Override
    public void load(@NonNull final Context context,
                     @NonNull final AdData adData) {

        Preconditions.checkNotNull(mLoadListener);
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);

        mContext = context;
        mAdData = adData;

        extractExtras(adData.getExtras());

        try {
            mBroadcastIdentifier = adData.getBroadcastIdentifier();
        } catch (ClassCastException e) {
            MoPubLog.log(CUSTOM, "LocalExtras contained an incorrect type.");
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.INTERNAL_ERROR.getIntCode(),
                    MoPubErrorCode.INTERNAL_ERROR);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.INTERNAL_ERROR);
            return;
        }

        preRender();

        MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
    }

    @Override
    @NonNull
    public String getAdNetworkId() {
        String adNetworkId = MoPubFullscreen.class.getName();
        if (mAdData != null) {
            if (!TextUtils.isEmpty(mAdData.getAdUnit())) {
                adNetworkId = mAdData.getAdUnit();
            }
        }
        if (MoPubFullscreen.class.getName().equals(adNetworkId)) {
            MoPubLog.log(MoPubLog.SdkLogEvent.CUSTOM, "Called getAdNetworkId before load() " +
                    "or no ad unit associated. Returning class name.");
        }
        return adNetworkId;
    }

    protected void extractExtras(final Map<String, String> serverExtras) {
        if (mAdData == null) {
            MoPubLog.log(CUSTOM, "Error extracting extras due to null ad data.");
            throw new IllegalStateException("Ad Data cannot be null here.");
        }

        mAdData.setOrientation(CreativeOrientation.fromString(serverExtras.get(CREATIVE_ORIENTATION_KEY)));

        final String externalViewabilityTrackers =
                serverExtras.get(DataKeys.EXTERNAL_VIDEO_VIEWABILITY_TRACKERS_KEY);
        try {
            mExternalViewabilityTrackers = Json.jsonStringToMap(externalViewabilityTrackers);
        } catch (JSONException e) {
            MoPubLog.log(CUSTOM, "Failed to parse video viewability trackers to JSON: " +
                    externalViewabilityTrackers);
        }

        final String videoTrackers = serverExtras.get(DataKeys.VIDEO_TRACKERS_KEY);
        if (!TextUtils.isEmpty(videoTrackers)) {
            try {
                mVideoTrackers = new JSONObject(videoTrackers);
            } catch (JSONException e) {
                MoPubLog.log(CUSTOM_WITH_THROWABLE, "Failed to parse video trackers to JSON: " + videoTrackers, e);
                mVideoTrackers = null;
            }
        }
    }

    protected void preRender() {
        MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);

        if (!CacheService.initializeDiskCache(mContext)) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    MoPubErrorCode.VIDEO_CACHE_ERROR.getIntCode(),
                    MoPubErrorCode.VIDEO_CACHE_ERROR);
            mLoadListener.onAdLoadFailed(MoPubErrorCode.VIDEO_CACHE_ERROR);
            return;
        }

        if (mAdData == null) {
            mLoadListener.onAdLoadFailed(MoPubErrorCode.NETWORK_INVALID_STATE);
            return;
        }

        mHandler = new Handler();
        mAdExpiration = () -> {
            MoPubLog.log(EXPIRED, ADAPTER_NAME, "time in seconds");
            mInteractionListener.onAdFailed(MoPubErrorCode.EXPIRED);
            mReady = false;
        };

        if (FullAdType.VAST.equals(mAdData.getFullAdType())) {
            mVastManager = VastManagerFactory.create(mContext);
            mVastManager.prepareVastVideoConfiguration(mAdData.getAdPayload(), this,
                    mAdData.getDspCreativeId(), mContext);
        } else {
            preRenderWeb(mContext, mAdData);
        }
    }

    public void preRenderWeb(@NonNull final Context context,
                             @NonNull final AdData adData) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(adData);

        MoPubLog.log(LOAD_ATTEMPTED, ADAPTER_NAME);

        final Long broadcastIdentifier = adData.getBroadcastIdentifier();
        Preconditions.checkNotNull(broadcastIdentifier);

        final String htmlData = adData.getAdPayload();
        Preconditions.checkNotNull(htmlData);

        BaseWebView baseWebView;
        MoPubWebViewController moPubWebViewController;

        if (MRAID.equals(adData.getAdType())) {
            baseWebView = new MraidBridge.MraidWebView(context);
            baseWebView.enableJavascriptCaching();

            moPubWebViewController = new MraidController(context,
                    adData.getDspCreativeId(),
                    PlacementType.INTERSTITIAL,
                    adData.getAllowCustomClose());

            baseWebView.enableJavascriptCaching();
        } else if (HTML.equals(adData.getAdType())) {
            baseWebView = new HtmlWebView(context);

            moPubWebViewController = HtmlControllerFactory.create(context,
                    adData.getDspCreativeId(),
                    adData.getExtras().get(CLICKTHROUGH_URL_KEY));
        } else {
            mLoadListener.onAdLoadFailed(FULLSCREEN_LOAD_ERROR);
            return;
        }

        moPubWebViewController.setMoPubWebViewListener(new MoPubFullScreenWebListener(mLoadListener));

        final ExternalViewabilitySessionManager externalViewabilitySessionManager =
                new ExternalViewabilitySessionManager(context);
        externalViewabilitySessionManager.createDisplaySession(context, baseWebView);

        moPubWebViewController.fillContent(htmlData, null);

        WebViewCacheService.storeWebViewConfig(broadcastIdentifier,
                baseWebView,
                this,
                externalViewabilitySessionManager,
                moPubWebViewController);
    }

    @Override
    protected void show() {
        MoPubLog.log(SHOW_ATTEMPTED, ADAPTER_NAME);

        if (!mReady || mContext == null) {
            MoPubLog.log(SHOW_FAILED, ADAPTER_NAME,
                    ADAPTER_CONFIGURATION_ERROR.getIntCode(),
                    ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        mBroadcastReceiver = new EventForwardingBroadcastReceiver(mInteractionListener, mBroadcastIdentifier);
        mBroadcastReceiver.register(mBroadcastReceiver, mContext);
        MoPubFullscreenActivity.start(mContext, mAdData);
    }

    @Override
    public void onInvalidate() {
        if (mVastManager != null) {
            mVastManager.cancel();
        }
        markNotReady();
        mAdExpiration = null;
        mHandler = null;
        mLoadListener = null;
        mInteractionListener = null;

        if (mBroadcastReceiver != null) {
            mBroadcastReceiver.unregister(mBroadcastReceiver);
            mBroadcastReceiver = null;
        }
    }

    @Override
    protected boolean checkAndInitializeSdk(@NonNull final Activity launcherActivity,
                                            @NonNull final AdData adData) {
        return false;
    }

    /*
     * VastManager.VastManagerListener implementation
     */

    @Override
    public void onVastVideoConfigurationPrepared(@Nullable final VastVideoConfig vastVideoConfig) {
        if (vastVideoConfig == null || mAdData == null) {
            if (mLoadListener != null) {
                mLoadListener.onAdLoadFailed(MoPubErrorCode.VIDEO_DOWNLOAD_ERROR);
            }
            return;
        }

        vastVideoConfig.addVideoTrackers(mVideoTrackers);
        vastVideoConfig.addExternalViewabilityTrackers(mExternalViewabilityTrackers);
        if (mAdData.isRewarded()) {
            vastVideoConfig.setRewarded(true);
        }

        mAdData.setVastVideoConfigString(vastVideoConfig.toJsonString());

        if (mLoadListener != null) {
            mLoadListener.onAdLoaded();
        }

        markReady();
    }

    @VisibleForTesting
    void markReady() {
        mReady = true;
        if (mAdData == null || !mAdData.isRewarded() || mHandler == null || mAdExpiration == null) {
            return;
        }
        mHandler.postDelayed(mAdExpiration, AD_EXPIRATION_DELAY);
    }

    @VisibleForTesting
    void markNotReady() {
        mReady = false;
        if (mAdData == null || !mAdData.isRewarded() || mHandler == null || mAdExpiration == null) {
            return;
        }
        mHandler.removeCallbacks(mAdExpiration);
    }

    @Deprecated
    @VisibleForTesting
    @Nullable
    VastManager getVastManager() {
        return mVastManager;
    }

    @Deprecated
    @VisibleForTesting
    void setVastManager(@Nullable final VastManager vastManager) {
        mVastManager = vastManager;
    }

    @Deprecated
    @VisibleForTesting
    void setReady(final boolean ready) {
        mReady = ready;
    }

    @Deprecated
    @VisibleForTesting
    void setHandler(@Nullable final Handler handler) {
        mHandler = handler;
    }

    private class MoPubFullScreenWebListener implements BaseHtmlWebView.BaseWebViewListener {

        final AdLifecycleListener.LoadListener loadListener;

        MoPubFullScreenWebListener(final AdLifecycleListener.LoadListener loadListener) {
            this.loadListener = loadListener;
        }

        @Override
        public void onLoaded(final View view) {
            MoPubLog.log(LOAD_SUCCESS, ADAPTER_NAME);
            markReady();
            loadListener.onAdLoaded();
        }

        @Override
        public void onFailedToLoad(final MoPubErrorCode errorCode) {
            MoPubLog.log(LOAD_FAILED, ADAPTER_NAME,
                    errorCode.getIntCode(),
                    errorCode);
            markNotReady();
            loadListener.onAdLoadFailed(errorCode);
        }

        @Override
        public void onFailed() { /* NO-OP */ }

        @Override
        public void onRenderProcessGone(@NonNull final MoPubErrorCode errorCode) { /* NO-OP */ }

        @Override
        public void onClicked() { /* NO-OP */ }

        @Override
        public void onExpand() { /* NO-OP */ }

        @Override
        public void onResize(final boolean toOriginalSize) { /* NO-OP */ }

        public void onClose() { /* NO-OP */ }

    }
}
