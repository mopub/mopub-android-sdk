// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ResponseBodyInterstitialTest {
    protected ResponseBodyInterstitial subject;

    @Test
    public void onInvalidate_beforeLoadInterstitialIsCalled_shouldNotBlowUp() throws Exception {
        // Have not called subject.loadInterstitial()

        subject.onInvalidate();

        // pass
    }
}
