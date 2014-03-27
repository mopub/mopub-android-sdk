/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.webkit.WebView;
import com.mopub.mobileads.util.VersionCode;
import com.mopub.mobileads.util.Views;
import com.mopub.mobileads.util.WebViews;

import java.lang.reflect.Method;

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

        if (VersionCode.currentApiLevel().isBelow(VersionCode.FROYO)) {
            // Note: this is needed to compile against api level 18.
            try {
                Method method = Class.forName("android.webkit.WebSettings").getDeclaredMethod("setPluginsEnabled", boolean.class);
                method.invoke(getSettings(), enabled);
            } catch (Exception e) {
                Log.d("MoPub", "Unable to " + (enabled ? "enable" : "disable") + "WebSettings plugins for BaseWebView.");
            }
        } else {

            try {
                Class<Enum> pluginStateClass = (Class<Enum>) Class.forName("android.webkit.WebSettings$PluginState");

                Class<?>[] parameters = {pluginStateClass};
                Method method = getSettings().getClass().getDeclaredMethod("setPluginState", parameters);

                Object pluginState = Enum.valueOf(pluginStateClass, enabled ? "ON" : "OFF");
                method.invoke(getSettings(), pluginState);
            } catch (Exception e) {
                Log.d("MoPub", "Unable to modify WebView plugin state.");
            }
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
