// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MoPubFullscreenActivityTest {
    private long broadcastIdentifier;
    private MoPubFullscreenActivity subject;
    private AdData adData;

    private static final String HTML_DATA = "TEST HTML DATA";
    private static final int REWARDED_DURATION_IN_SECONDS = 25;

    @Mock
    private FullscreenAdController mockFullscreenAdController;

    @Before
    public void setup() {
        broadcastIdentifier = 3333;

        AdData adData = new AdData.Builder()
                .broadcastIdentifier(broadcastIdentifier)
                .adPayload(HTML_DATA)
                .rewardedDurationSeconds(REWARDED_DURATION_IN_SECONDS)
                .build();

        Context context = Robolectric.buildActivity(Activity.class).create().get();
        Intent intent = MoPubFullscreenActivity.createIntent(context, adData);
        subject = Robolectric.buildActivity(MoPubFullscreenActivity.class, intent)
                .create().get();
        subject.setFullscreenAdController(mockFullscreenAdController);
    }

    @Test
    public void OnPause_shouldCallPauseOnController() {
        subject.onResume();

        subject.onPause();

        verify(mockFullscreenAdController).resume();
        verify(mockFullscreenAdController).pause();
    }

    @Test
    public void onDestroy_shouldCallDestroyOnController() {
        subject.onResume();

        subject.onDestroy();

        verify(mockFullscreenAdController).resume();
        verify(mockFullscreenAdController).destroy();
    }
}
