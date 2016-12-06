package com.mopub.mraid;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.mopub.common.AdReport;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.factories.MraidControllerFactory;
import com.mopub.mraid.MraidController.MraidListener;

import java.util.Map;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.MoPubErrorCode.MRAID_LOAD_ERROR;

class MraidBanner extends CustomEventBanner {

    @Nullable private MraidController mMraidController;
    @Nullable private CustomEventBannerListener mBannerListener;
    @Nullable private MraidWebViewDebugListener mDebugListener;

    @Override
    protected void loadBanner(@NonNull Context context,
                    @NonNull CustomEventBannerListener customEventBannerListener,
                    @NonNull Map<String, Object> localExtras,
                    @NonNull Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;

        String htmlData;
        if (extrasAreValid(serverExtras)) {
            htmlData = serverExtras.get(HTML_RESPONSE_BODY_KEY);
        } else {
            mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            return;
        }

        try {
            AdReport adReport = (AdReport) localExtras.get(AD_REPORT_KEY);
            mMraidController = MraidControllerFactory.create(
                    context, adReport, PlacementType.INLINE);
        } catch (ClassCastException e) {
            MoPubLog.w("MRAID banner creating failed:", e);
            mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            return;
        }

        mMraidController.setDebugListener(mDebugListener);
        mMraidController.setMraidListener(new MraidListener() {
            @Override
            public void onLoaded(View view) {
                // Honoring the server dimensions forces the WebView to be the size of the banner
                AdViewController.setShouldHonorServerDimensions(view);
                mBannerListener.onBannerLoaded(view);
            }

            @Override
            public void onFailedToLoad() {
                mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            }

            @Override
            public void onExpand() {
                mBannerListener.onBannerExpanded();
                mBannerListener.onBannerClicked();
            }

            @Override
            public void onOpen() {
                mBannerListener.onBannerClicked();
            }

            @Override
            public void onClose() {
                mBannerListener.onBannerCollapsed();
            }
        });
        mMraidController.loadContent(htmlData);
    }

    @Override
    protected void onInvalidate() {
        if (mMraidController != null) {
            mMraidController.setMraidListener(null);
            mMraidController.destroy();
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }

    @VisibleForTesting
    public void setDebugListener(@Nullable MraidWebViewDebugListener debugListener) {
        mDebugListener = debugListener;
        if (mMraidController != null) {
            mMraidController.setDebugListener(debugListener);
        }
    }
}
