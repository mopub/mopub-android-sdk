package com.mopub.mobileads;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Handler;
import android.webkit.JavascriptInterface;

import static com.mopub.common.util.VersionCode.HONEYCOMB;
import static com.mopub.common.util.VersionCode.currentApiLevel;
import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;

public class HtmlInterstitialWebView extends BaseHtmlWebView {
    private Handler mHandler;

    protected static final String MOPUB_JS_INTERFACE_NAME = "mopubUriInterface";

    interface MoPubUriJavascriptFireFinishLoadListener {
        abstract void onInterstitialLoaded();
    }

    public HtmlInterstitialWebView(Context context, AdConfiguration adConfiguration) {
        super(context, adConfiguration);

        mHandler = new Handler();
    }

    public void init(final CustomEventInterstitialListener customEventInterstitialListener, boolean isScrollable, String redirectUrl, String clickthroughUrl) {
        super.init(isScrollable);

        HtmlInterstitialWebViewListener htmlInterstitialWebViewListener = new HtmlInterstitialWebViewListener(customEventInterstitialListener);
        HtmlWebViewClient htmlWebViewClient = new HtmlWebViewClient(htmlInterstitialWebViewListener, this, clickthroughUrl, redirectUrl);
        setWebViewClient(htmlWebViewClient);

        addMoPubUriJavascriptInterface(new MoPubUriJavascriptFireFinishLoadListener() {
            @Override
            public void onInterstitialLoaded() {
                if (!mIsDestroyed) {
                    customEventInterstitialListener.onInterstitialLoaded();
                }
            }
        });
    }

    private void postHandlerRunnable(Runnable r) {
        mHandler.post(r);
    }

    /*
     * XXX (2/15/12): This is a workaround for a problem on ICS devices where
     * WebViews with layout height WRAP_CONTENT can mysteriously render with
     * zero height. This seems to happen when calling loadData() with HTML that
     * sets window.location during its "onload" event. We use loadData() when
     * displaying interstitials, and our creatives use window.location to
     * communicate ad loading status to AdViews. This results in zero-height
     * interstitials. We counteract this by using a Javascript interface object
     * to signal loading status, rather than modifying window.location.
     */
    void addMoPubUriJavascriptInterface(final MoPubUriJavascriptFireFinishLoadListener moPubUriJavascriptFireFinishLoadListener) {
        final class MoPubUriJavascriptInterface {
            // This method appears to be unused, since it will only be called from JavaScript.
            @SuppressWarnings("unused")
            @JavascriptInterface
            public boolean fireFinishLoad() {
                HtmlInterstitialWebView.this.postHandlerRunnable(new Runnable() {
                    @Override
                    public void run() {
                        moPubUriJavascriptFireFinishLoadListener.onInterstitialLoaded();
                    }
                });

                return true;
            }
        }

        addJavascriptInterface(new MoPubUriJavascriptInterface(), MOPUB_JS_INTERFACE_NAME);
    }

    @TargetApi(11)
    @Override
    public void destroy() {
        if (currentApiLevel().isAtLeast(HONEYCOMB)) {
            removeJavascriptInterface(MOPUB_JS_INTERFACE_NAME);
        }

        super.destroy();
    }

    static class HtmlInterstitialWebViewListener implements HtmlWebViewListener {
        private final CustomEventInterstitialListener mCustomEventInterstitialListener;

        public HtmlInterstitialWebViewListener(CustomEventInterstitialListener customEventInterstitialListener) {
            mCustomEventInterstitialListener = customEventInterstitialListener;
        }

        @Override
        public void onLoaded(BaseHtmlWebView mHtmlWebView) {
            mCustomEventInterstitialListener.onInterstitialLoaded();
        }

        @Override
        public void onFailed(MoPubErrorCode errorCode) {
            mCustomEventInterstitialListener.onInterstitialFailed(errorCode);
        }

        @Override
        public void onClicked() {
            mCustomEventInterstitialListener.onInterstitialClicked();
        }

        @Override
        public void onCollapsed() {
            // Ignored
        }
    }
}
