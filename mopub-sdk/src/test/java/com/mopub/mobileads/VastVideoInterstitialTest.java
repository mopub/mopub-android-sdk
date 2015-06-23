package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.mopub.common.CacheServiceTest;
import com.mopub.common.DataKeys;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;
import com.mopub.mobileads.test.support.TestVastManagerFactory;
import com.mopub.mobileads.test.support.TestVastVideoDownloadTaskFactory;
import com.mopub.mobileads.util.vast.VastCompanionAd;
import com.mopub.mobileads.util.vast.VastManager;
import com.mopub.mobileads.util.vast.VastVideoConfiguration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLocalBroadcastManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.DataKeys.BROADCAST_IDENTIFIER_KEY;
import static com.mopub.common.DataKeys.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.CustomEventInterstitial.CustomEventInterstitialListener;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_DISMISS;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_SHOW;
import static com.mopub.mobileads.EventForwardingBroadcastReceiverTest.getIntentForActionAndIdentifier;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;
import static com.mopub.mobileads.util.vast.VastManager.VastManagerListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

@RunWith(SdkTestRunner.class)
public class VastVideoInterstitialTest extends ResponseBodyInterstitialTest {
    private Context context;
    private CustomEventInterstitialListener customEventInterstitialListener;
    private Map<String, Object> localExtras;
    private Map<String, String> serverExtras;
    private TestHttpResponseWithHeaders response;
    private String expectedResponse;
    private VastManager vastManager;
    private String videoUrl;
    private VastVideoDownloadTask vastVideoDownloadTask;
    private long broadcastIdentifier;

    @Before
    public void setUp() throws Exception {
        subject = new VastVideoInterstitial();

        vastVideoDownloadTask = TestVastVideoDownloadTaskFactory.getSingletonMock();
        vastManager = TestVastManagerFactory.getSingletonMock();
        expectedResponse = "<VAST>hello</VAST>";
        videoUrl = "http://www.video.com";

        context = new Activity();
        customEventInterstitialListener = mock(CustomEventInterstitialListener.class);
        localExtras = new HashMap<String, Object>();
        serverExtras = new HashMap<String, String>();
        serverExtras.put(DataKeys.HTML_RESPONSE_BODY_KEY, expectedResponse);

        response = new TestHttpResponseWithHeaders(200, expectedResponse);

        broadcastIdentifier = 2222;
        localExtras.put(BROADCAST_IDENTIFIER_KEY, broadcastIdentifier);
    }

    @After
    public void tearDown() throws Exception {
        reset(vastVideoDownloadTask);
    }

    @Test
    public void preRenderHtml_whenCreatingVideoCache_butItHasInitializationErrors_shouldSignalOnInterstitialFailedOnError() throws Exception {
        // context is null when loadInterstitial is not called, which causes DiskLruCache to not be created

        subject.preRenderHtml(customEventInterstitialListener);

        verify(customEventInterstitialListener).onInterstitialFailed(eq(MoPubErrorCode.VIDEO_CACHE_ERROR));
        verify(vastManager, never()).prepareVastVideoConfiguration(anyString(), any(VastManagerListener.class));
    }

    @Test
    public void loadInterstitial_shouldParseHtmlResponseBodyServerExtra() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        assertThat(((VastVideoInterstitial) subject).getVastResponse()).isEqualTo(expectedResponse);
    }

    @Test
    public void loadInterstitial_shouldInitializeDiskCache() throws Exception {
        Robolectric.addPendingHttpResponse(response);

        CacheServiceTest.assertDiskCacheIsUninitialized();
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        CacheServiceTest.assertDiskCacheIsEmpty();
    }

    @Test
    public void loadInterstitial_shouldCreateVastManagerAndProcessVast() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        verify(vastManager).prepareVastVideoConfiguration(eq(expectedResponse), eq((VastVideoInterstitial) subject));
    }

    @Test
    public void loadInterstitial_whenServerExtrasDoesNotContainResponse_shouldSignalOnInterstitialFailed() throws Exception {
        serverExtras.remove(HTML_RESPONSE_BODY_KEY);

        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        verify(customEventInterstitialListener).onInterstitialFailed(NETWORK_INVALID_STATE);
        verify(vastManager, never()).prepareVastVideoConfiguration(anyString(), any(VastManagerListener.class));
    }

    @Test
    public void loadInterstitial_shouldConnectListenerToBroadcastReceiver() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        Intent intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_SHOW, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialShown();

        intent = getIntentForActionAndIdentifier(ACTION_INTERSTITIAL_DISMISS, broadcastIdentifier);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener).onInterstitialDismissed();
    }

    @Test
    public void showInterstitial_shouldStartVideoPlayerActivityWithAllValidTrackers() throws Exception {
        VastCompanionAd vastCompanionAd = mock(VastCompanionAd.class, withSettings().serializable());
        VastVideoConfiguration vastVideoConfiguration = new VastVideoConfiguration();
        vastVideoConfiguration.setNetworkMediaFileUrl(videoUrl);
        vastVideoConfiguration.addAbsoluteTrackers(Arrays.asList(new VastAbsoluteProgressTracker("start", 2000)));
        vastVideoConfiguration.addFractionalTrackers(Arrays.asList(new VastFractionalProgressTracker("first", 0.25f),
                new VastFractionalProgressTracker("mid", 0.5f),
                new VastFractionalProgressTracker("third", 0.75f)));
        vastVideoConfiguration.addCompleteTrackers(Arrays.asList("complete"));
        vastVideoConfiguration.addImpressionTrackers(Arrays.asList("imp"));
        vastVideoConfiguration.setClickThroughUrl("clickThrough");
        vastVideoConfiguration.addClickTrackers(Arrays.asList("click"));
        vastVideoConfiguration.setVastCompanionAd(vastCompanionAd);

        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        ((VastVideoInterstitial) subject).onVastVideoConfigurationPrepared(vastVideoConfiguration);

        subject.showInterstitial();
        BaseVideoPlayerActivityTest.assertVastVideoPlayerActivityStarted(
                MraidVideoPlayerActivity.class,
                vastVideoConfiguration,
                broadcastIdentifier
                );
    }

    @Test
    public void onInvalidate_shouldCancelVastManager() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        subject.onInvalidate();

        verify(vastManager).cancel();
    }

    @Test
    public void onInvalidate_whenVastManagerIsNull_shouldNotBlowUp() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);

        ((VastVideoInterstitial) subject).setVastManager(null);

        subject.onInvalidate();

        // pass
    }

    @Test
    public void onInvalidate_shouldDisconnectListenerToBroadcastReceiver() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        subject.onInvalidate();

        Intent intent;
        intent = new Intent(ACTION_INTERSTITIAL_SHOW);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialShown();

        intent = new Intent(ACTION_INTERSTITIAL_DISMISS);
        ShadowLocalBroadcastManager.getInstance(context).sendBroadcast(intent);

        verify(customEventInterstitialListener, never()).onInterstitialDismissed();
    }

    @Test
    public void onVastVideoConfigurationPrepared_withVastVideoConfiguration_shouldSignalOnInterstitialLoaded() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        ((VastVideoInterstitial) subject).onVastVideoConfigurationPrepared(mock(VastVideoConfiguration.class));

        verify(customEventInterstitialListener).onInterstitialLoaded();
    }

    @Test
    public void onVastVideoConfigurationPrepared_withNullVastVideoConfiguration_shouldSignalOnInterstitialFailed() throws Exception {
        subject.loadInterstitial(context, customEventInterstitialListener, localExtras, serverExtras);
        ((VastVideoInterstitial) subject).onVastVideoConfigurationPrepared(null);

        verify(customEventInterstitialListener).onInterstitialFailed(MoPubErrorCode.VIDEO_DOWNLOAD_ERROR);
    }
}
