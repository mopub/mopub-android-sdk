package com.mopub.nativeads;

import android.support.annotation.NonNull;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.nativeads.BaseNativeAd.NativeEventListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class BaseNativeAdTest {

    private BaseNativeAd subject;
    @Mock NativeEventListener mockEventListener;

    @Before
    public void setUp() {
        subject = new BaseNativeAd() {
            @Override
            public void prepare(@NonNull View view) { }

            @Override
            public void clear(@NonNull View view) { }

            @Override
            public void destroy() { }
        };
    }

    @Test
    public void notifyAdImpressed_withListenerSet_shouldCallOnAdImpressed() {
        subject.setNativeEventListener(mockEventListener);
        subject.notifyAdImpressed();

        verify(mockEventListener).onAdImpressed();
    }

    @Test
    public void notifyAdImpressed_withoutListenerSet_shouldNotCallOnAdImpressed() {
        subject.notifyAdImpressed();

        verifyNoMoreInteractions(mockEventListener);
    }

    @Test
    public void notifyAdClicked_withListenerSet_shouldCallOnAdClicked() {
        subject.setNativeEventListener(mockEventListener);
        subject.notifyAdClicked();

        verify(mockEventListener).onAdClicked();
    }

    @Test
    public void notifyAdClicked_withoutListenerSet_shouldNotCallOnAdClicked() {
        subject.notifyAdClicked();

        verifyNoMoreInteractions(mockEventListener);
    }
}