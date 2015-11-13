package com.mopub.mobileads;

import com.mopub.common.CacheService;
import com.mopub.common.DataKeys;
import com.mopub.mobileads.factories.VastManagerFactory;

import java.util.Map;

class VastVideoInterstitial extends ResponseBodyInterstitial implements VastManager.VastManagerListener {
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private String mVastResponse;
    private VastManager mVastManager;
    private VastVideoConfig mVastVideoConfig;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mVastResponse = serverExtras.get(DataKeys.HTML_RESPONSE_BODY_KEY);
    }

    @Override
    protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener) {
        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (!CacheService.initializeDiskCache(mContext)) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_CACHE_ERROR);
            return;
        }

        mVastManager = VastManagerFactory.create(mContext);
        mVastManager.prepareVastVideoConfiguration(mVastResponse, this, mContext);
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
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_DOWNLOAD_ERROR);
            return;
        }

        mVastVideoConfig = vastVideoConfig;
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
