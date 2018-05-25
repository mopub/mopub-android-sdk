package com.mopub.common;

import android.content.Context;

/**
 * Called when Sdk initialization completes from
 * {@link MoPub#initializeSdk(Context, SdkConfiguration, SdkInitializationListener)}
 */
public interface SdkInitializationListener {
    void onInitializationFinished();
}
