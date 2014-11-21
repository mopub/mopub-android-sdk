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
