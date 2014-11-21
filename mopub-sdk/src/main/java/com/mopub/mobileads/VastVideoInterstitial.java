package com.mopub.mobileads;

import android.net.Uri;

import com.mopub.common.CacheService;
import com.mopub.mobileads.factories.VastManagerFactory;
import com.mopub.mobileads.util.vast.VastManager;
import com.mopub.mobileads.util.vast.VastVideoConfiguration;

import java.util.Map;

class VastVideoInterstitial extends ResponseBodyInterstitial implements VastManager.VastManagerListener {
    private CustomEventInterstitialListener mCustomEventInterstitialListener;
    private String mVastResponse;
    private VastManager mVastManager;
    private VastVideoConfiguration mVastVideoConfiguration;

    @Override
    protected void extractExtras(Map<String, String> serverExtras) {
        mVastResponse = Uri.decode(serverExtras.get(AdFetcher.HTML_RESPONSE_BODY_KEY));
    }

    @Override
    protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener) {
        mCustomEventInterstitialListener = customEventInterstitialListener;

        if (!CacheService.initializeDiskCache(mContext)) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_CACHE_ERROR);
            return;
        }

        mVastManager = VastManagerFactory.create(mContext);
        mVastManager.prepareVastVideoConfiguration(mVastResponse, this);
    }

    @Override
    protected void showInterstitial() {
        MraidVideoPlayerActivity.startVast(mContext, mVastVideoConfiguration, mAdConfiguration);
    }

    @Override
    protected void onInvalidate() {
        if (mVastManager != null) {
            mVastManager.cancel();
        }

        super.onInvalidate();
    }

    /*
     * VastManager.VastManagerListener implementation
     */

    @Override
    public void onVastVideoConfigurationPrepared(final VastVideoConfiguration vastVideoConfiguration) {
        if (vastVideoConfiguration == null) {
            mCustomEventInterstitialListener.onInterstitialFailed(MoPubErrorCode.VIDEO_DOWNLOAD_ERROR);
            return;
        }

        mVastVideoConfiguration = vastVideoConfiguration;
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
