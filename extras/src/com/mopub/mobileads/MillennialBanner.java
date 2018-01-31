package com.mopub.mobileads;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import com.millennialmedia.AppInfo;
import com.millennialmedia.CreativeInfo;
import com.millennialmedia.InlineAd;
import com.millennialmedia.InlineAd.AdSize;
import com.millennialmedia.InlineAd.InlineAdMetadata;
import com.millennialmedia.InlineAd.InlineErrorStatus;
import com.millennialmedia.MMException;
import com.millennialmedia.MMLog;
import com.millennialmedia.MMSDK;
import com.mopub.common.MoPub;

import java.util.Map;


/**
 * Compatible with version 6.6 of the Millennial Media SDK.
 */

final class MillennialBanner extends CustomEventBanner {

    private static final String TAG = MillennialBanner.class.getSimpleName();

    private static final String DCN_KEY = "dcn";
    private static final String APID_KEY = "adUnitID";
    private static final String AD_WIDTH_KEY = "adWidth";
    private static final String AD_HEIGHT_KEY = "adHeight";

    private InlineAd inlineAd;
    private CustomEventBannerListener bannerListener;
    private FrameLayout internalView;

    static {
        Log.i(TAG, "Millennial Media Adapter Version: " + MillennialUtils.VERSION);
    }


    public CreativeInfo getCreativeInfo() {

        if (inlineAd == null) {
            return null;
        }

        return inlineAd.getCreativeInfo();
    }

    
    @Override
    protected void loadBanner(final Context context, final CustomEventBannerListener customEventBannerListener,
                              final Map<String, Object> localExtras, final Map<String, String> serverExtras) {

        bannerListener = customEventBannerListener;
        if (!MillennialUtils.initSdk(context)) {
            Log.e(TAG, "MM SDK must be initialized with an Activity or Application context.");
            bannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
            return;
        }

        String apid = serverExtras.get(APID_KEY);
        int width = Integer.parseInt(serverExtras.get(AD_WIDTH_KEY));
        int height = Integer.parseInt(serverExtras.get(AD_HEIGHT_KEY));

        if (MillennialUtils.isEmpty(apid) || (width < 0) || (height < 0)) {
            Log.e(TAG, "We were given invalid extras! Make sure placement ID, width, and height are specified.");
            bannerListener.onBannerFailed(MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR);
            return;
        }

        // Set DCN on the AppInfo if provided
        String dcn = serverExtras.get(DCN_KEY);
        AppInfo ai = new AppInfo().setMediator("mopubsdk");
        if (!MillennialUtils.isEmpty(dcn)) {
            ai.setSiteId(dcn);
        }

        try {

            MMSDK.setAppInfo(ai);

            internalView = new FrameLayout(context);

            LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            internalView.setLayoutParams(lp);

            inlineAd = InlineAd.createInstance(apid, internalView);
            InlineAdMetadata mInlineAdMetadata = new InlineAdMetadata().setAdSize(new AdSize(width, height));

            inlineAd.setListener(new MillennialInlineListener());

            MMSDK.setLocationEnabled(MoPub.getLocationAwareness() != MoPub.LocationAwareness.DISABLED);

            AdViewController.setShouldHonorServerDimensions(internalView);

            inlineAd.request(mInlineAdMetadata);

        } catch (MMException e) {
            Log.e(TAG, "MM SDK exception occurred obtaining an inline ad unit.", e);
            bannerListener.onBannerFailed(MoPubErrorCode.INTERNAL_ERROR);
        }
    }


    @Override
    protected void onInvalidate() {
        // Destroy any hanging references.
        if (inlineAd != null) {
            inlineAd.destroy();
            inlineAd = null;
        }
    }


    class MillennialInlineListener implements InlineAd.InlineListener {

        @Override
        public void onAdLeftApplication(InlineAd inlineAd) {
            // onLeaveApplication is an alias to on clicked. We are not required to call this.

            // @formatter:off
            // https://github.com/mopub/mopub-android-sdk/blob/940eee70fe1980b4869d61cb5d668ccbab75c0ee/mopub-sdk/mopub-sdk-interstitial/src/main/java/com/mopub/mobileads/CustomEventInterstitial.java
            // @formatter:on
            Log.d(TAG, "Millennial Inline Ad - Leaving application");
        }


        @Override
        public void onClicked(InlineAd inlineAd) {

            Log.d(TAG, "Millennial Inline Ad - Ad clicked");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    bannerListener.onBannerClicked();
                }
            });
        }


        @Override
        public void onCollapsed(InlineAd inlineAd) {

            Log.d(TAG, "Millennial Inline Ad - Banner collapsed");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    bannerListener.onBannerCollapsed();
                }
            });

        }


        @Override
        public void onExpanded(InlineAd inlineAd) {

            Log.d(TAG, "Millennial Inline Ad - Banner expanded");
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    bannerListener.onBannerExpanded();
                }
            });
        }


        @Override
        public void onRequestFailed(InlineAd inlineAd, InlineErrorStatus inlineErrorStatus) {

            Log.d(TAG, "Millennial Inline Ad - Banner failed (" + inlineErrorStatus.getErrorCode() + "): " +
                    inlineErrorStatus.getDescription());

            MoPubErrorCode mopubErrorCode;

            switch (inlineErrorStatus.getErrorCode()) {
                case InlineErrorStatus.ADAPTER_NOT_FOUND:
                    mopubErrorCode = MoPubErrorCode.ADAPTER_CONFIGURATION_ERROR;
                    break;
                case InlineErrorStatus.DISPLAY_FAILED:
                    mopubErrorCode = MoPubErrorCode.INTERNAL_ERROR;
                    break;
                case InlineErrorStatus.INIT_FAILED:
                    mopubErrorCode = MoPubErrorCode.WARMUP;
                    break;
                case InlineErrorStatus.NO_NETWORK:
                    mopubErrorCode = MoPubErrorCode.NO_CONNECTION;
                    break;
                case InlineErrorStatus.UNKNOWN:
                    mopubErrorCode = MoPubErrorCode.UNSPECIFIED;
                    break;
                case InlineErrorStatus.LOAD_FAILED:
                default:
                    mopubErrorCode = MoPubErrorCode.NETWORK_NO_FILL;
            }

            final MoPubErrorCode fErrorCode = mopubErrorCode;
            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    bannerListener.onBannerFailed(fErrorCode);
                }
            });

        }


        @Override
        public void onRequestSucceeded(InlineAd inlineAd) {

            Log.d(TAG, "Millennial Inline Ad - Banner request succeeded");

            CreativeInfo creativeInfo = getCreativeInfo();

            if ((creativeInfo != null) && MMLog.isDebugEnabled()) {
                MMLog.d(TAG, "Banner Creative Info: " + creativeInfo);
            }

            MillennialUtils.postOnUiThread(new Runnable() {
                @Override
                public void run() {

                    bannerListener.onBannerLoaded(internalView);
                }
            });
        }


        @Override
        public void onResize(InlineAd inlineAd, int w, int h) {

            Log.d(TAG, "Millennial Inline Ad - Banner about to resize (width: " + w + ", height: " + h + ")");
        }


        @Override
        public void onResized(InlineAd inlineAd, int w, int h, boolean isClosed) {

            Log.d(TAG, "Millennial Inline Ad - Banner resized (width: " + w + ", height: " + h + "). " +
                    (isClosed ? "Returned to original placement." : "Got a fresh, new place."));

        }
    }
}
