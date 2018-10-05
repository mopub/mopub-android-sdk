// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.nativeads;

import android.view.View;

/**
 * This interface should be implemented by native ad formats that want to make use of the
 * {@link NativeClickHandler} to track clicks and open click destinations.
 */
public interface ClickInterface {
    void handleClick(View view);
}
