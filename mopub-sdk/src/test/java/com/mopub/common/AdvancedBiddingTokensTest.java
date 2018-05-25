package com.mopub.common;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdvancedBiddingTokensTest {

    private Context context;
    private AdvancedBiddingTokens subject;
    private SdkInitializationListener mockInitializationListener;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        mockInitializationListener = mock(SdkInitializationListenerMockClass.class);
        subject = new AdvancedBiddingTokens(mockInitializationListener);

    }

    @Test
    public void addAdvancedBidders_getTokensAsJsonString_withAdvancedBiddingClass_shouldAddOneMoPubAdvancedBidder() throws Exception {
        List<Class<? extends MoPubAdvancedBidder>> list = new ArrayList<>();
        list.add(AdvancedBiddingTokensAdvancedBidder.class);

        subject.addAdvancedBidders(list);

        assertThat(subject.getTokensAsJsonString(context)).isEqualTo(
                "{\"AdvancedBiddingTokensTest\":{\"token\":\"AdvancedBiddingTokensToken\"}}");
        verify(mockInitializationListener).onInitializationFinished();
    }

    @Test
    public void addAdvancedBidder_getTokensAsJsonString_withNoAdvancedBidders_shouldReturnNull() throws Exception {
        List<Class<? extends MoPubAdvancedBidder>> list = new ArrayList<>();

        subject.addAdvancedBidders(list);

        assertThat(subject.getTokensAsJsonString(context)).isNull();
        verify(mockInitializationListener).onInitializationFinished();
    }

    private static class AdvancedBiddingTokensAdvancedBidder implements MoPubAdvancedBidder {

        @Override
        public String getToken(final Context context) {
            return "AdvancedBiddingTokensToken";
        }

        @Override
        public String getCreativeNetworkName() {
            return "AdvancedBiddingTokensTest";
        }
    }

    // Creating extra class to prevent roboelectric cache conflict with
    // MoPub.initializeSdk_withCallbackSet_shouldCallCallback
    private abstract class SdkInitializationListenerMockClass implements SdkInitializationListener{};

}
