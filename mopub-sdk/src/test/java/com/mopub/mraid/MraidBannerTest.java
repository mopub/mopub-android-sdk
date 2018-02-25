package com.mopub.mraid;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.test.support.TestMraidControllerFactory;
import com.mopub.mraid.MraidController.MraidListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.CustomEventBanner.CustomEventBannerListener;
import static com.mopub.mobileads.MoPubErrorCode.MRAID_LOAD_ERROR;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MraidBannerTest {
    private static final String INPUT_HTML_DATA = "%3Chtml%3E%3C%2Fhtml%3E";

    MraidController mockMraidController;
    @Mock CustomEventBannerListener mockBannerListener;

    private Context context;
    private Map<String, Object> localExtras;
    private Map<String, String> serverExtras;

    private MraidBanner subject;

    @Before
    public void setUp() {
        subject = new MraidBanner();
        mockMraidController = TestMraidControllerFactory.getSingletonMock();

        context = new Activity();
        localExtras = new HashMap<String, Object>();
        localExtras.put("broadcastIdentifier", 123L);
        serverExtras = new HashMap<String, String>();
        serverExtras.put(HTML_RESPONSE_BODY_KEY, INPUT_HTML_DATA);
    }

    @Test
    public void loadBanner_whenExtrasAreMalformed_shouldNotifyBannerListenerFailureAndReturn() {
        serverExtras.remove(HTML_RESPONSE_BODY_KEY);

        subject.loadBanner(context, mockBannerListener, localExtras, serverExtras);

        verify(mockBannerListener).onBannerFailed(eq(MRAID_LOAD_ERROR));
    }

    @Test
    public void loadBanner_withTrueFlag_shouldSetBannerImpressionPixelCountEnabledTrue() {
        assertThat(subject.isBannerImpressionPixelCountEnabled()).isFalse();

        localExtras.put(DataKeys.BANNER_IMPRESSION_PIXEL_COUNT_ENABLED, true);

        subject.loadBanner(context, mockBannerListener, localExtras, serverExtras);

        assertThat(subject.isBannerImpressionPixelCountEnabled()).isTrue();
    }

    @Test
    public void loadBanner_withFalseFlag_shouldSetBannerImpressionPixelCountEnabledFalse() {
        assertThat(subject.isBannerImpressionPixelCountEnabled()).isFalse();

        localExtras.put(DataKeys.BANNER_IMPRESSION_PIXEL_COUNT_ENABLED, false);

        subject.loadBanner(context, mockBannerListener, localExtras, serverExtras);

        assertThat(subject.isBannerImpressionPixelCountEnabled()).isFalse();
    }

    @Test
    public void invalidate_shouldDestroyMraidController() {
        subject.loadBanner(context, mockBannerListener, localExtras, serverExtras);
        subject.onInvalidate();

        verify(mockMraidController).destroy();
    }

    @Test
    public void bannerMraidListener_onReady_shouldNotifyBannerLoaded() {
        MraidListener mraidListener = captureMraidListener();
        mraidListener.onLoaded(null);

        verify(mockBannerListener).onBannerLoaded(any(View.class));
    }

    @Test
    public void bannerMraidListener_onFailure_shouldNotifyBannerFailed() {
        MraidListener mraidListener = captureMraidListener();
        mraidListener.onFailedToLoad();

        verify(mockBannerListener).onBannerFailed(eq(MRAID_LOAD_ERROR));
    }

    @Test
    public void bannerMraidListener_onExpand_shouldNotifyBannerExpandedAndClicked() {
        MraidListener mraidListener = captureMraidListener();
        mraidListener.onExpand();

        verify(mockBannerListener).onBannerExpanded();
        verify(mockBannerListener).onBannerClicked();
    }

    @Test
    public void bannerMraidListener_onOpen_shouldNotifyBannerClicked() {
        MraidListener mraidListener = captureMraidListener();
        mraidListener.onOpen();

        verify(mockBannerListener).onBannerClicked();
    }

    @Test
    public void bannerMraidListener_onClose_shouldNotifyBannerCollapsed() {
        MraidListener mraidListener = captureMraidListener();
        mraidListener.onClose();

        verify(mockBannerListener).onBannerCollapsed();
    }

    @Test
    public void trackMpxAndThirdPartyImpressions_shouldFireJavascriptWebViewDidAppear() {
        MraidListener mraidListener = captureMraidListener();
        mraidListener.onLoaded(null);
        verify(mockBannerListener).onBannerLoaded(any(View.class));

        subject.trackMpxAndThirdPartyImpressions();
        verify(mockMraidController).loadJavascript(eq("webviewDidAppear();"));
    }

    private MraidListener captureMraidListener() {
        subject.loadBanner(context, mockBannerListener, localExtras, serverExtras);
        ArgumentCaptor<MraidListener> listenerCaptor = ArgumentCaptor.forClass(MraidListener.class);
        verify(mockMraidController).setMraidListener(listenerCaptor.capture());

        return listenerCaptor.getValue();
    }
}
