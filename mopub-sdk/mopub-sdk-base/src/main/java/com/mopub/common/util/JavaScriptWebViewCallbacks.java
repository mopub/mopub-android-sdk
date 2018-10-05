// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.util;

public enum JavaScriptWebViewCallbacks {
    // The ad server appends these functions to the MRAID javascript to help with third party
    // impression tracking.
    WEB_VIEW_DID_APPEAR("webviewDidAppear();"),
    WEB_VIEW_DID_CLOSE("webviewDidClose();");

    private String mJavascript;
    
    JavaScriptWebViewCallbacks(String javascript) {
        mJavascript = javascript;
    }

    public String getJavascript() {
        return mJavascript;
    }

    public String getUrl() {
        return "javascript:" + mJavascript;
    }
}
