// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.support.annotation.NonNull;

import java.util.List;

interface AdvancedBiddersInitializedListener {
    void onAdvancedBiddersInitialized(@NonNull final List<MoPubAdvancedBidder> advancedBidders);
}
