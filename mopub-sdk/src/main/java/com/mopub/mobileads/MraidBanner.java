package com.mopub.mobileads;

import android.content.Context;
import android.net.Uri;

import com.mopub.mobileads.MraidView.ViewState;
import com.mopub.mobileads.factories.MraidViewFactory;

import java.util.Map;

import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.MoPubErrorCode.MRAID_LOAD_ERROR;
import static com.mopub.mobileads.MraidView.MraidListener;

class MraidBanner extends CustomEventBanner {
    private MraidView mMraidView;
    private CustomEventBannerListener mBannerListener;

    @Override
    protected void loadBanner(Context context,
                    CustomEventBannerListener customEventBannerListener,
                    Map<String, Object> localExtras,
                    Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;

        String htmlData;
        if (extrasAreValid(serverExtras)) {
            htmlData = Uri.decode(serverExtras.get(HTML_RESPONSE_BODY_KEY));
        } else {
            mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            return;
        }

        AdConfiguration adConfiguration = AdConfiguration.extractFromMap(localExtras);
        mMraidView = MraidViewFactory.create(context, adConfiguration);
        mMraidView.loadHtmlData(htmlData);
        initMraidListener();
    }

    @Override
    protected void onInvalidate() {
        if (mMraidView != null) {
            resetMraidListener();
            mMraidView.destroy();
        }
    }

    private void onReady() {
        mBannerListener.onBannerLoaded(mMraidView);
    }

    private void onFail() {
        mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
    }

    private void onExpand() {
        mBannerListener.onBannerExpanded();
        mBannerListener.onBannerClicked();
    }

    private void onOpen() {
        mBannerListener.onBannerClicked();
    }

    private void onClose() {
        mBannerListener.onBannerCollapsed();
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }

    private void initMraidListener() {
        mMraidView.setMraidListener(new MraidListener() {
            public void onReady(MraidView view) {
                MraidBanner.this.onReady();
            }
            public void onFailure(MraidView view) {
                onFail();
            }
            public void onExpand(MraidView view) {
                MraidBanner.this.onExpand();
            }
            public void onOpen(MraidView view) {
                MraidBanner.this.onOpen();
            }
            public void onClose(MraidView view, ViewState newViewState) {
                MraidBanner.this.onClose();
            }
        });
    }

    private void resetMraidListener() {
        mMraidView.setMraidListener(null);
    }
}
