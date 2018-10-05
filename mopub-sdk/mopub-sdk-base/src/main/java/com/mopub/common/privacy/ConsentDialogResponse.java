// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.privacy;

import android.support.annotation.NonNull;

import com.mopub.common.Preconditions;

class ConsentDialogResponse {
    @NonNull
    private final String mHtml;

    ConsentDialogResponse(@NonNull final String html) {
        Preconditions.checkNotNull(html);

        mHtml = html;
    }

    @NonNull
    public String getHtml() {
        return mHtml;
    }
}
