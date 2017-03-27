package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.AdReport;
import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.DataKeys.AD_REPORT_KEY;
import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class RewardedVastVideoInterstitialTest {

    private Context context;
    private RewardedVastVideoInterstitial.RewardedVideoInterstitialListener mRewardedVideoInterstitialListener;
    private Map<String, Object> localExtras;
    private Map<String, String> serverExtras;
    private RewardedVastVideoInterstitial subject;

    @Mock AdReport mockAdReport;

    @Before
    public void setUp() throws Exception {
        subject = new RewardedVastVideoInterstitial();

        final String expectedResponse = "<VAST>hello</VAST>";

        context = Robolectric.buildActivity(Activity.class).create().get();
        mRewardedVideoInterstitialListener = mock(
                RewardedVastVideoInterstitial.RewardedVideoInterstitialListener.class);
        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();
        serverExtras.put(DataKeys.HTML_RESPONSE_BODY_KEY, expectedResponse);

        final long broadcastIdentifier = 2222;
        localExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
        when(mockAdReport.getDspCreativeId()).thenReturn("dsp_creative_id");
        localExtras.put(AD_REPORT_KEY, mockAdReport);
    }

    @Test
    public void loadInterstitial_withCustomEventRewardedVideoInterstitialListener_shouldRegisterRewardedVideoBroadcastReceiver() {
        subject.loadInterstitial(context, mRewardedVideoInterstitialListener, localExtras,
                serverExtras);

        assertThat(subject.getRewardedVideoBroadcastReceiver()).isNotNull();
    }

    @Test
    public void onVastVideoConfigurationPrepared_withProperVastConfig_shouldSetRewardedVideoFlag() {
        subject.loadInterstitial(context, mRewardedVideoInterstitialListener, localExtras,
                serverExtras);
        final VastVideoConfig mockVastVideoConfig = mock(VastVideoConfig.class);

        subject.onVastVideoConfigurationPrepared(mockVastVideoConfig);

        verify(mockVastVideoConfig).setIsRewardedVideo(true);
    }

}
