package com.mopub.mobileads;

import android.content.Context;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.mopub.common.util.VersionCode;
import com.mopub.common.util.Views;
import com.mopub.mobileads.util.WebViews;

public class BaseWebView extends WebView {
    protected boolean mIsDestroyed;

    public BaseWebView(Context context) {
        /*
         * Important: don't allow any WebView subclass to be instantiated using
         * an Activity context, as it will leak on Froyo devices and earlier.
         */
        super(context.getApplicationContext());
        enablePlugins(false);

        WebViews.setDisableJSChromeClient(this);
    }

    protected void enablePlugins(final boolean enabled) {
        // Android 4.3 and above has no concept of plugin states
        if (VersionCode.currentApiLevel().isAtLeast(VersionCode.JELLY_BEAN_MR2)) {
            return;
        }

        if (enabled) {
            getSettings().setPluginState(WebSettings.PluginState.ON);
        } else {
            getSettings().setPluginState(WebSettings.PluginState.OFF);
        }
    }

    @Override
    public void destroy() {
        mIsDestroyed = true;

        Views.removeFromParent(this);
        super.destroy();
    }

    @Deprecated // for testing
    void setIsDestroyed(boolean isDestroyed) {
        mIsDestroyed = isDestroyed;
    }
}
