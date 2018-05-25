package com.mopub.mobileads;

import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mopub.common.CacheService;
import com.mopub.common.DataKeys;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Json;
import com.mopub.mobileads.factories.VastManagerFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class VastVideoInterstitial extends ResponseBodyInterstitial implements VastManager.VastManagerListener {
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private String mVastResponse;
    private VastManager mVastManager;
    private VastVideoConfig mVastVideoConfig;
    @Nullable private JSONObject mVideoTrackers;
    @Nullable private Map<String, String> mExternalViewabilityTrackers;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mVastResponse = serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY);

        final String externalViewabilityTrackers =
                serverExtras.get(DataKeys.EXTERNAL_VIDEO_VIEWABILITY_TRACKERS_KEY);
        try {
            mExternalViewabilityTrackers = Json.jsonStringToMap(externalViewabilityTrackers);
        } catch (JSONException e) {
            MoPubLog.d("Failed to parse video viewability trackers to JSON: " +
                    externalViewabilityTrackers);
        }

        final String videoTrackers = serverExtras.get(DataKeys.VIDEO_TRACKERS_KEY);
        if (TextUtils.isEmpty(videoTrackers)) {
            return;
        }
        try {
            mVideoTrackers = new JSONObject(videoTrackers);
        } catch (JSONException e) {
            MoPubLog.d("Failed to parse video trackers to JSON: " + videoTrackers, e);
            mVideoTrackers = null;
        }
    }

    @Override
    protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener) {
        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (!CacheService.initializeDiskCache(mContext)) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_CACHE_ERROR);
            return;
        }

        mVastManager = VastManagerFactory.create(mContext);
        mVastManager.prepareVastVideoConfiguration(mVastResponse, this,
                mAdReport.getDspCreativeId(), mContext);
    }

    @Override
    public void showInterstitial() {
        MraidVideoPlayerActivity.startVast(mContext, mVastVideoConfig, mBroadcastIdentifier);
    }

    @Override
    public void onInvalidate() {
        if (mVastManager != null) {
            mVastManager.cancel();
        }

        super.onInvalidate();
    }

    /*
     * VastManager.VastManagerListener implementation
     */

    @Override
    public void onVastVideoConfigurationPrepared(final VastVideoConfig vastVideoConfig) {
        if (vastVideoConfig == null) {
            mCustomEventInterstitialListener
                    .onInterstitialFailed(MoPubErrorCode.VIDEO_DOWNLOAD_ERROR);
            return;
        }

        mVastVideoConfig = vastVideoConfig;
        mVastVideoConfig.addVideoTrackers(mVideoTrackers);
        mVastVideoConfig.addExternalViewabilityTrackers(mExternalViewabilityTrackers);
        mCustomEventInterstitialListener.onInterstitialLoaded();
    }


    @Deprecated // for testing
    String getVastResponse() {
        return mVastResponse;
    }

    @Deprecated // for testing
    void setVastManager(VastManager vastManager) {
        mVastManager = vastManager;
    }
}
