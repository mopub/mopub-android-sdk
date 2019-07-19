// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class SingleImpressionTest {

    @Test
    public void sendImpression_makesCallToImpressionEmitter() {
        ImpressionData data = mock(ImpressionData.class);
        ImpressionListener listener = mock(ImpressionListener.class);
        ImpressionsEmitter.addListener(listener);
        SingleImpression subject = new SingleImpression("ad_unit_id", data);

        subject.sendImpression();

        verify(listener).onImpression("ad_unit_id", data);
    }

    @Test
    public void sendImpression_withNullAdUnitId_shouldNotCallImpressionEmitter() {
        ImpressionData data = mock(ImpressionData.class);
        ImpressionListener listener = mock(ImpressionListener.class);
        ImpressionsEmitter.addListener(listener);
        SingleImpression subject = new SingleImpression(null, data);

        subject.sendImpression();

        verify(listener, never()).onImpression(anyString(), any(ImpressionData.class));
    }
}
