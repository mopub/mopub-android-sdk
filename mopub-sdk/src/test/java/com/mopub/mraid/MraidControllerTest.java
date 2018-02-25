package com.mopub.mraid;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.view.View;
import android.widget.FrameLayout;

import com.mopub.common.AdReport;
import com.mopub.common.CloseableLayout.ClosePosition;
import com.mopub.common.ExternalViewabilitySessionManager;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Utils;
import com.mopub.mobileads.BaseVideoPlayerActivityTest;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.Interstitial;
import com.mopub.mobileads.MraidVideoPlayerActivity;
import com.mopub.mobileads.WebViewCacheService;
import com.mopub.mraid.MraidBridge.MraidBridgeListener;
import com.mopub.mraid.MraidBridge.MraidWebView;
import com.mopub.mraid.MraidController.MraidListener;
import com.mopub.mraid.MraidController.OrientationBroadcastReceiver;
import com.mopub.mraid.MraidController.ScreenMetricsWaiter;
import com.mopub.mraid.MraidController.ScreenMetricsWaiter.WaitRequest;
import com.mopub.mraid.MraidController.UseCustomCloseListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.net.URI;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MraidControllerTest {
    private long broadcastIdentifier = 123;
    @Mock private AdReport mockAdReport;
    @Mock private MraidBridge mockBridge;
    @Mock private MraidBridge mockTwoPartBridge;
    @Mock private ScreenMetricsWaiter mockScreenMetricsWaiter;
    @Mock private WaitRequest mockWaitRequest;
    @Mock private MraidListener mockMraidListener;
    @Mock private UseCustomCloseListener mockUseCustomCloseListener;
    @Mock private OrientationBroadcastReceiver mockOrientationBroadcastReceiver;
    @Mock private MraidWebView mockWebView;
    @Mock private ExternalViewabilitySessionManager mockViewabilityManager;
    @Captor private ArgumentCaptor<MraidBridgeListener> bridgeListenerCaptor;
    @Captor private ArgumentCaptor<MraidBridgeListener> twoPartBridgeListenerCaptor;

    private Activity activity;
    private FrameLayout rootView;

    private MraidController subject;

    @Before
    public void setUp() {
        ShadowApplication.setDisplayMetricsDensity(1.0f);
        WebViewCacheService.clearAll();

        activity = spy(Robolectric.buildActivity(Activity.class).create().get());
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        rootView = new FrameLayout(activity);
        when(mockBridge.isVisible()).thenReturn(true);

        // By default, immediately fulfill a screen metrics wait request. Individual tests can
        // reset this, if desired.
        when(mockScreenMetricsWaiter.waitFor(Mockito.<View>anyVararg()))
                .thenReturn(mockWaitRequest);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return null;
            }
        }).when(mockWaitRequest).start(any(Runnable.class));

        subject = new MraidController(
                activity, mockAdReport, PlacementType.INLINE,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);
        subject.setMraidListener(mockMraidListener);
        subject.setOrientationBroadcastReceiver(mockOrientationBroadcastReceiver);
        subject.setRootView(rootView);
        subject.fillContent(null, "fake_html_data", null);

        verify(mockBridge).setMraidBridgeListener(bridgeListenerCaptor.capture());
        verify(mockTwoPartBridge).setMraidBridgeListener(twoPartBridgeListenerCaptor.capture());
    }

    @Test
    public void constructor_shouldSetStateToLoading() {
        ViewState state = subject.getViewState();

        assertThat(state).isEqualTo(ViewState.LOADING);
    }

    @Test
    public void bridgeOnReady_shouldSetStateToDefault_shouldCallListener() {
        bridgeListenerCaptor.getValue().onPageLoaded();

        ViewState state = subject.getViewState();

        assertThat(state).isEqualTo(ViewState.DEFAULT);
        verify(mockMraidListener).onLoaded(any(View.class));
    }

    @Test
    public void handlePageLoad_shouldNotifyBridgeOfVisibilityPlacementScreenSizeAndSupports() {
        when(mockBridge.isVisible()).thenReturn(true);

        subject.handlePageLoad();

        verify(mockBridge).notifyViewability(true);
        verify(mockBridge).notifyPlacementType(PlacementType.INLINE);
        verify(mockBridge).notifyScreenMetrics(any(MraidScreenMetrics.class));

        // The actual values here are supplied by the Mraids class, which has separate tests.
        verify(mockBridge).notifySupports(false, false, false, false, false);
    }

    @Test
    public void handlePageLoad_shouldCancelLastRequest() {
        subject.handlePageLoad();

        verify(mockScreenMetricsWaiter).cancelLastRequest();
    }

    @Test
    public void handlePageLoad_thenDestroy_shouldCancelLastRequest() {
        subject.handlePageLoad();
        subject.destroy();

        verify(mockScreenMetricsWaiter, times(2)).cancelLastRequest();
    }

    @Test
    public void onPageFailedToLoad_shouldNotifyListener() {
        bridgeListenerCaptor.getValue().onPageFailedToLoad();

        verify(mockMraidListener).onFailedToLoad();
    }

    @Test
    public void onPageFailedToLoad_withTwoPartBridge_shouldNotNotifyListener() {
        twoPartBridgeListenerCaptor.getValue().onPageFailedToLoad();

        verify(mockMraidListener, never()).onFailedToLoad();
    }

    @Test
    public void bridgeOnVisibilityChanged_withTwoPartBridgeAttached_shouldNotNotifyVisibility() {
        when(mockTwoPartBridge.isAttached()).thenReturn(true);

        bridgeListenerCaptor.getValue().onVisibilityChanged(true);
        bridgeListenerCaptor.getValue().onVisibilityChanged(false);

        verify(mockBridge, never()).notifyViewability(anyBoolean());
        verify(mockTwoPartBridge, never()).notifyViewability(anyBoolean());
    }

    @Test
    public void handleResize_shouldBeIgnoredWhenLoadingOrHidden() throws MraidCommandException {
        subject.setViewStateForTesting(ViewState.LOADING);
        subject.handleResize(100, 200, 0, 0, ClosePosition.TOP_RIGHT, true);
        assertThat(subject.getViewState()).isEqualTo(ViewState.LOADING);

        subject.setViewStateForTesting(ViewState.HIDDEN);
        subject.handleResize(100, 200, 0, 0, ClosePosition.TOP_RIGHT, true);
        assertThat(subject.getViewState()).isEqualTo(ViewState.HIDDEN);
    }

    @Test(expected = MraidCommandException.class)
    public void handleResize_shouldThrowExceptionWhenExpanded() throws MraidCommandException {
        subject.setViewStateForTesting(ViewState.EXPANDED);
        subject.handleResize(100, 200, 0, 0, ClosePosition.TOP_RIGHT, true);
    }

    @Test(expected = MraidCommandException.class)
    public void handleResize_shouldThrowExceptionForInterstitial() throws MraidCommandException {
        MraidListener listener = mock(MraidListener.class);
        subject = new MraidController(activity, mockAdReport, PlacementType.INTERSTITIAL,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);
        subject.setMraidListener(listener);
        subject.setRootView(rootView);

        // Move to DEFAULT state
        subject.fillContent(null, "fake_html_data", null);
        subject.handlePageLoad();

        subject.handleResize(100, 200, 0, 0, ClosePosition.TOP_RIGHT, true);
    }

    @Test
    public void handleResize_shouldMoveWebViewToResizedContainer_shouldSetResizedState()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);

        subject.handleResize(100, 100, 0, 0, ClosePosition.TOP_RIGHT, true);
        assertThat(subject.getExpandedAdContainer().getChildCount()).isEqualTo(2);
        assertThat(subject.getAdContainer().getChildCount()).isEqualTo(0);
        assertThat(subject.getViewState()).isEqualTo(ViewState.RESIZED);
    }

    @Test
    public void handleResize_noAllowOffscreen_smallView_shouldResize()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);

        subject.handleResize(50, 50, 0, 0, ClosePosition.TOP_RIGHT, /* allowOffscreen */ false);
        assertThat(subject.getViewState()).isEqualTo(ViewState.RESIZED);
    }

    @Test(expected = MraidCommandException.class)
    public void handleResize_noAllowOffscreen_largeView_shouldThrowException()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);

        subject.handleResize(101, 101, 0, 0, ClosePosition.TOP_RIGHT, /* allowOffscreen */ false);
    }

    @Test(expected = MraidCommandException.class)
    public void handleResize_allowOffscreen_largeView_closeButtonTopRight_shouldThrowException()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);

        subject.handleResize(150, 150, 0, 0, ClosePosition.TOP_RIGHT, /* allowOffscreen */ true);
    }

    @Test
    public void handleResize_allowOffscreen_closeButtonTopLeft_shouldNotThrowException()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);

        subject.handleResize(150, 150, 0, 0, ClosePosition.TOP_LEFT, /* allowOffscreen */ true);
        assertThat(subject.getViewState()).isEqualTo(ViewState.RESIZED);
    }

    @Test(expected = MraidCommandException.class)
    public void handleResize_allowOffscreen_largeOffset_closeButtonBottomRight_shouldThrowException()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();
        subject.setRootViewSize(100, 1000);

        // Throws an exception because the close button overlaps the edge
        subject.handleResize(100, 100, 25, 25, ClosePosition.BOTTOM_RIGHT, /* allowOffscreen */
                true);
    }

    @Test
    public void handleResize_allowOffscreen_largeOffset_closeButtonBottomLeft_shouldNotThrowException()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();
        subject.setRootViewSize(100, 1000);

        subject.handleResize(100, 100, 25, 25, ClosePosition.BOTTOM_LEFT, /* allowOffscreen */
                true);
        assertThat(subject.getViewState()).isEqualTo(ViewState.RESIZED);
    }

    @Test(expected = MraidCommandException.class)
    public void handleResize_heightSmallerThan50Dips_shouldFail() throws MraidCommandException {
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);
        subject.handleResize(100, 49, 25, 25, ClosePosition.BOTTOM_LEFT, /* allowOffscreen */
                false);
    }

    @Test(expected = MraidCommandException.class)
    public void handleResize_widthSmallerThan50Dips_shouldFail() throws MraidCommandException {
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);
        subject.handleResize(49, 100, 25, 25, ClosePosition.BOTTOM_LEFT, /* allowOffscreen */
                false);
    }

    @Test
    public void handleClose_fromResizedState_shouldMoveWebViewToOriginalContainer_shouldNotFireOnClose()
            throws MraidCommandException {
        // Move to RESIZED state
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);
        subject.handleResize(100, 100, 0, 0, ClosePosition.TOP_RIGHT, false);

        subject.handleClose();

        assertThat(subject.getExpandedAdContainer().getChildCount()).isEqualTo(1);
        assertThat(subject.getAdContainer().getChildCount()).isEqualTo(1);
        assertThat(subject.getViewState()).isEqualTo(ViewState.DEFAULT);
        verify(mockMraidListener, never()).onClose();
    }

    @Test(expected = MraidCommandException.class)
    public void handleExpand_afterDestroy_shouldThrowException() throws MraidCommandException {
        subject.destroy();
        subject.handleExpand(null, false);
    }

    @Test
    public void handleExpand_shouldBeIgnoredForInterstitial() throws MraidCommandException {
        MraidListener listener = mock(MraidListener.class);
        subject = new MraidController(activity, mockAdReport, PlacementType.INTERSTITIAL,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);
        subject.setMraidListener(listener);
        subject.setRootView(rootView);

        // Move to DEFAULT state
        subject.fillContent(null, "fake_html_data", null);
        subject.handlePageLoad();

        subject.handleExpand(null, false);

        assertThat(subject.getViewState()).isEqualTo(ViewState.DEFAULT);
        verify(listener, never()).onExpand();
    }

    @Test
    public void handleExpand_shouldBeIgnoredWhenLoadingHiddenOrExpanded()
            throws MraidCommandException {
        subject.setViewStateForTesting(ViewState.LOADING);
        subject.handleExpand(null, false);
        assertThat(subject.getViewState()).isEqualTo(ViewState.LOADING);
        verify(mockMraidListener, never()).onExpand();

        subject.setViewStateForTesting(ViewState.HIDDEN);
        subject.handleExpand(null, false);
        assertThat(subject.getViewState()).isEqualTo(ViewState.HIDDEN);
        verify(mockMraidListener, never()).onExpand();

        subject.setViewStateForTesting(ViewState.EXPANDED);
        subject.handleExpand(null, false);
        assertThat(subject.getViewState()).isEqualTo(ViewState.EXPANDED);
        verify(mockMraidListener, never()).onExpand();
    }

    @Test
    public void handleExpand_withNoUrl_shouldMoveWebViewToExpandedContainer_shouldCallOnExpand()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();

        subject.handleExpand(null, false);

        assertThat(subject.getExpandedAdContainer().getChildCount()).isEqualTo(2);
        assertThat(subject.getAdContainer().getChildCount()).isEqualTo(0);
        verify(mockMraidListener).onExpand();
    }

    @Test
    public void handleExpand_withTwoPartUrl_shouldAttachTwoPartBridge_shouldCallOnExpand()
            throws MraidCommandException {
        // Move to DEFAULT state
        subject.handlePageLoad();

        subject.handleExpand(URI.create("https://two-part-url"), false);

        verify(mockTwoPartBridge).setMraidBridgeListener(any(MraidBridgeListener.class));
        verify(mockTwoPartBridge).attachView(any(MraidWebView.class));
        verify(mockTwoPartBridge).setContentUrl(URI.create("https://two-part-url").toString());

        assertThat(subject.getExpandedAdContainer().getChildCount()).isEqualTo(2);
        assertThat(subject.getAdContainer().getChildCount()).isEqualTo(1);
        verify(mockMraidListener).onExpand();
        assertThat(subject.getViewState()).isEqualTo(ViewState.EXPANDED);
    }

    @Test
    public void handleClose_afterDestroy_shouldNotFireOnClose() {
        subject.destroy();
        subject.handleClose();

        verify(mockMraidListener, never()).onClose();
    }

    @Test
    public void handleClose_fromExpandedState_shouldMoveWebViewToOriginalContainer_shouldFireOnClose() throws MraidCommandException {
        // Move to EXPANDED state
        subject.handlePageLoad();
        subject.handleExpand(null, false);

        subject.handleClose();

        assertThat(subject.getExpandedAdContainer().getChildCount()).isEqualTo(1);
        assertThat(subject.getAdContainer().getChildCount()).isEqualTo(1);
        assertThat(subject.getViewState()).isEqualTo(ViewState.DEFAULT);
        verify(mockMraidListener).onClose();
    }

    @Test
    public void handleClose_fromTwoPartExpandedState_shouldDetachTwoPartBridge_shouldMoveWebViewToOriginalContainer_shouldFireOnClose()
            throws MraidCommandException {
        URI uri = URI.create("https://two-part-url");

        // Move to two part EXPANDED state
        subject.handlePageLoad();
        subject.handleExpand(uri, false);
        when(mockTwoPartBridge.isAttached()).thenReturn(true);

        subject.handleClose();

        verify(mockTwoPartBridge).detach();
        assertThat(subject.getExpandedAdContainer().getChildCount()).isEqualTo(1);
        assertThat(subject.getAdContainer().getChildCount()).isEqualTo(1);
        assertThat(subject.getViewState()).isEqualTo(ViewState.DEFAULT);

        verify(mockMraidListener).onClose();
    }

    @Test
    public void handleClose_fromDefaultState_shouldHideAdContainer_shouldCallOnClose() {
        // Move to DEFAULT state
        subject.handlePageLoad();
        assertThat(subject.getViewState()).isEqualTo(ViewState.DEFAULT);

        subject.handleClose();

        assertThat(subject.getAdContainer().getVisibility()).isEqualTo(View.INVISIBLE);
        assertThat(subject.getViewState()).isEqualTo(ViewState.HIDDEN);

        verify(mockMraidListener).onClose();
    }

    @Test
    public void handleShowVideo_shouldStartVideoPlayerActivity() {
        subject.handleShowVideo("https://video");
        BaseVideoPlayerActivityTest.assertMraidVideoPlayerActivityStarted(
                MraidVideoPlayerActivity.class, "https://video");
    }

    @Test
    public void handleCustomClose_shouldUpdateExpandedContainer() {
        subject.handleCustomClose(true);
        assertThat(subject.getExpandedAdContainer().isCloseVisible()).isFalse();

        subject.handleCustomClose(false);
        assertThat(subject.getExpandedAdContainer().isCloseVisible()).isTrue();
    }

    @Test
    public void handleCustomClose_shouldCallCustomCloseChangedListener() {
        subject.setUseCustomCloseListener(mockUseCustomCloseListener);

        subject.handleCustomClose(true);
        verify(mockUseCustomCloseListener).useCustomCloseChanged(true);

        reset(mockUseCustomCloseListener);
        subject.handleCustomClose(false);
        verify(mockUseCustomCloseListener).useCustomCloseChanged(false);
    }

    @Test
    public void handleOpen_withMoPubNativeBrowserUrl_shouldOpenExternalBrowser() {
        subject.handleOpen("mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.example.com");

        Intent intent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(intent.getDataString()).isEqualTo("https://www.example.com");
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
    }

    @Test
    public void handleOpen_withMalformedMoPubNativeBrowserUrl_shouldNotStartNewActivity() {
        // invalid host parameter 'nav'
        subject.handleOpen("mopubnativebrowser://nav?url=https%3A%2F%2Fwww.example.com");

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void handleOpen_withApplicationUrl_shouldStartNewIntent() {
        String applicationUrl = "amzn://blah";
        shadowOf(activity.getPackageManager()).addResolveInfoForIntent(new Intent(Intent.ACTION_VIEW, Uri
                .parse(applicationUrl)), new ResolveInfo());

        subject.handleOpen(applicationUrl);

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        // Since we are not using an Activity context, we should have FLAG_ACTIVITY_NEW_TASK
        assertThat(Utils.bitMaskContainsFlag(startedIntent.getFlags(),
                Intent.FLAG_ACTIVITY_NEW_TASK)).isTrue();
        assertThat(startedIntent.getComponent()).isNull();

        verify(mockMraidListener).onOpen();
    }

    @Test
    public void handleOpen_withHttpApplicationUrl_shouldStartMoPubBrowser() {
        String applicationUrl = "https://www.mopub.com/";

        subject.handleOpen(applicationUrl);

        Robolectric.flushBackgroundThreadScheduler();
        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        // Since we are not using an Activity context, we should have FLAG_ACTIVITY_NEW_TASK
        assertThat(Utils.bitMaskContainsFlag(startedIntent.getFlags(),
                Intent.FLAG_ACTIVITY_NEW_TASK)).isTrue();
        assertThat(startedIntent.getComponent().getClassName())
                .isEqualTo("com.mopub.common.MoPubBrowser");

        verify(mockMraidListener).onOpen();
    }

    @Test
    public void handleOpen_withApplicationUrlThatCantBeHandled_shouldDefaultToMoPubBrowser()
            throws Exception {
        String applicationUrl = "canthandleme://blah";

        subject.handleOpen(applicationUrl);

        Intent startedIntent = ShadowApplication.getInstance().getNextStartedActivity();
        assertThat(startedIntent).isNull();

        verify(mockMraidListener).onOpen();
    }

    @Test
    public void handleOpen_withAboutBlankUrl_shouldFailSilently() {
        final String url = "about:blank";

        subject.handleOpen(url);

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
    }

    @Test
    public void fillContent_withCacheHit_shouldNotLoadHtmlData_shouldCallMraidListenerOnLoaded() {
        subject = new MraidController(
                activity, mockAdReport, PlacementType.INLINE,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);
        subject.setMraidListener(mockMraidListener);
        reset(mockMraidListener, mockBridge);
        subject.setOrientationBroadcastReceiver(mockOrientationBroadcastReceiver);
        subject.setRootView(rootView);
        WebViewCacheService.storeWebViewConfig(broadcastIdentifier, new Interstitial() {},
                mockWebView, mockViewabilityManager);

        subject.fillContent(broadcastIdentifier, "fake_html_data", null);

        verify(mockBridge, never()).setContentHtml("fake_html_data");
        verify(mockMraidListener).onLoaded(subject.getAdContainer());
    }

    @Test
    public void fillContent_withCacheMiss_shouldLoadHtmlData() {
        subject = new MraidController(
                activity, mockAdReport, PlacementType.INLINE,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);
        subject.setMraidListener(mockMraidListener);
        reset(mockMraidListener, mockBridge);
        subject.setOrientationBroadcastReceiver(mockOrientationBroadcastReceiver);
        subject.setRootView(rootView);

        subject.fillContent(broadcastIdentifier, "fake_html_data", null);

        verify(mockBridge).setContentHtml("fake_html_data");
        verify(mockMraidListener, never()).onLoaded(any(View.class));
    }

    @Test
    public void orientationBroadcastReceiver_whenUnregistered_shouldIgnoreOnReceive() {
        Intent intent = mock(Intent.class);
        when(intent.getAction()).thenReturn("some bogus action which we hope never to see");

        MraidController.OrientationBroadcastReceiver receiver =
                subject.new OrientationBroadcastReceiver();
        receiver.register(activity);
        receiver.unregister();
        receiver.onReceive(activity, intent);

        verify(intent, never()).getAction();
    }

    @Test
    public void orientationProperties_shouldDefaultToAllowChangeTrueAndForceOrientationNone() {
        // These are the default values provided by the MRAID spec
        assertThat(subject.getAllowOrientationChange()).isTrue();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.NONE);
    }

    @Test
    public void handleSetOrientationProperties_withForcedOrientation_shouldUpdateProperties() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject.handleSetOrientationProperties(false, MraidOrientation.LANDSCAPE);

        assertThat(subject.getAllowOrientationChange()).isFalse();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.LANDSCAPE);
    }

    @Test
    public void handleSetOrientationProperties_withOrientationNone_withApplicationContext_shouldUpdateProperties() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject = new MraidController(
                activity.getApplicationContext(), mockAdReport, PlacementType.INLINE,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);

        subject.handleSetOrientationProperties(false, MraidOrientation.NONE);

        assertThat(subject.getAllowOrientationChange()).isFalse();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.NONE);
    }

    @Test
    public void handleSetOrientationProperties_withForcedOrientation_withApplicationContext_shouldThrowMraidCommandExceptionAndNotUpdateProperties() throws PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject = new MraidController(
                activity.getApplicationContext(), mockAdReport, PlacementType.INLINE,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);

        try {
            subject.handleSetOrientationProperties(false, MraidOrientation.LANDSCAPE);
            fail("Expected MraidCommandException");
        } catch (MraidCommandException e) {
            // pass
        }

        assertThat(subject.getAllowOrientationChange()).isTrue();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.NONE);
    }

    @Test
    public void handleSetOrientationProperties_withActivityInfoNotFound_shouldThrowMraidCommandException() throws PackageManager.NameNotFoundException {
        setMockActivityInfo(false, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        try {
            subject.handleSetOrientationProperties(false, MraidOrientation.LANDSCAPE);
            fail("Expected MraidCommandException");
        } catch (MraidCommandException e) {
            // pass
        }

        assertThat(subject.getAllowOrientationChange()).isTrue();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.NONE);
    }

    @Test
    public void handleSetOrientationProperties_whenTryingToSetToOrientationDeclaredInManifest_shouldUpdateProperties() throws Exception {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject.handleSetOrientationProperties(true, MraidOrientation.PORTRAIT);

        assertThat(subject.getAllowOrientationChange()).isTrue();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.PORTRAIT);
    }

    @Test
    public void handleSetOrientationProperties_whenTryingToSetToOrientationDifferentFromManifest_shouldThrowMraidCommandException() throws Exception {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        try {
            subject.handleSetOrientationProperties(true, MraidOrientation.LANDSCAPE);
            fail("Expected MraidCommandException");
        } catch (MraidCommandException e) {
            // pass
        }

        assertThat(subject.getAllowOrientationChange()).isTrue();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.NONE);
    }

    @Test
    public void handleSetOrientationProperties_withForcedOrientation_withMissingConfigChangeOrientation_shouldThrowMraidCommandException() throws Exception {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                /* missing CONFIG_ORIENTATION */ ActivityInfo.CONFIG_SCREEN_SIZE);

        try {
            subject.handleSetOrientationProperties(true, MraidOrientation.PORTRAIT);
            fail("Expected MraidCommandException");
        } catch (MraidCommandException e) {
            // pass
        }

        assertThat(subject.getAllowOrientationChange()).isTrue();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.NONE);
    }

    @Test(expected = MraidCommandException.class)
    public void handleSetOrientationProperties_withMissingConfigChangeScreenSize_shouldThrowMraidCommandException() throws Exception {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION);

        subject.handleSetOrientationProperties(false, MraidOrientation.LANDSCAPE);

        assertThat(subject.getAllowOrientationChange()).isTrue();
        assertThat(subject.getForceOrientation()).isEqualTo(MraidOrientation.NONE);
    }

    @Test
    public void handleSetOrientationProperties_forExpandedBanner_shouldImmediatelyChangeScreenOrientation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject.handlePageLoad();
        subject.handleExpand(null, false);

        subject.handleSetOrientationProperties(true, MraidOrientation.LANDSCAPE);

        assertThat(activity.getRequestedOrientation()).isEqualTo(ActivityInfo
                .SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void handleSetOrientationProperties_forExpandedBanner_beforeExpandIsCalled_shouldChangeScreenOrientationUponExpand() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject.handlePageLoad();
        subject.handleSetOrientationProperties(true, MraidOrientation.LANDSCAPE);

        assertThat(activity.getRequestedOrientation()).isEqualTo(ActivityInfo
                .SCREEN_ORIENTATION_PORTRAIT);

        subject.handleExpand(null, false);

        assertThat(activity.getRequestedOrientation()).isEqualTo(ActivityInfo
                .SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void handleSetOrientationProperties_forDefaultBanner_shouldNotChangeScreenOrientation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject.handlePageLoad();
        // don't expand the banner

        subject.handleSetOrientationProperties(true, MraidOrientation.LANDSCAPE);

        assertThat(activity.getRequestedOrientation()).isEqualTo(ActivityInfo
                .SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void handleSetOrientationProperties_forInterstitial_shouldChangeScreenOrientation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject = new MraidController(
                activity, mockAdReport, PlacementType.INTERSTITIAL,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);

        assertThat(activity.getRequestedOrientation()).isEqualTo(ActivityInfo
                .SCREEN_ORIENTATION_PORTRAIT);

        subject.handlePageLoad();
        subject.handleSetOrientationProperties(true, MraidOrientation.LANDSCAPE);

        assertThat(activity.getRequestedOrientation()).isEqualTo(ActivityInfo
                .SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void shouldAllowForceOrientation_withNoneOrientation_shouldReturnTrue() throws PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        final boolean result = subject.shouldAllowForceOrientation(MraidOrientation.NONE);

        assertThat(result).isTrue();
    }

    @Test
    public void shouldAllowForceOrientation_withApplicationContext_shouldReturnFalse() throws PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject = new MraidController(
                activity.getApplicationContext(), mockAdReport, PlacementType.INLINE,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);

        final boolean result = subject.shouldAllowForceOrientation(MraidOrientation.PORTRAIT);

        assertThat(result).isFalse();
    }

    @Test(expected = MraidCommandException.class)
    public void lockOrientation_withApplicationContext_shouldThrowMraidCommandException() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject = new MraidController(
                activity.getApplicationContext(), mockAdReport, PlacementType.INLINE,
                mockBridge, mockTwoPartBridge, mockScreenMetricsWaiter);

        subject.lockOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void lockOrientation_withActivityContext_shouldInitializeOriginalActivityOrientationAndCallActivitySetOrientation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        assertThat(subject.getOriginalActivityOrientation()).isNull();
        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        subject.lockOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        assertThat(subject.getOriginalActivityOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void lockOrientation_subsequentTimes_shouldNotModifyOriginalActivityOrientation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject.lockOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        assertThat(subject.getOriginalActivityOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        subject.lockOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        assertThat(subject.getOriginalActivityOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void applyOrientation_withLockedOrientation_withForceOrientationNone_withAllowOrientationChangeTrue_shouldResetOrientation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        subject.lockOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        subject.handleSetOrientationProperties(true, MraidOrientation.NONE);
        subject.applyOrientation();

        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void applyOrientation_withNoLockedOrientation_withForceOrientationNone_withAllowOrientationChangeTrue_shouldDoNothing() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        subject.handleSetOrientationProperties(true, MraidOrientation.NONE);
        subject.applyOrientation();

        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void applyOrientation_withForcedOrientationTrue_shouldSetRequestedOrientationToForcedOrienation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject.handleSetOrientationProperties(true, MraidOrientation.LANDSCAPE);
        subject.applyOrientation();

        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void applyOrientation_withForcedOrientationFalse_shouldSetRequestedOrientationToForcedOrienation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        subject.handleSetOrientationProperties(false, MraidOrientation.LANDSCAPE);
        subject.applyOrientation();

        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Test
    public void unapplyOrientation_withALockedOrientation_shouldReturnToOriginalOrientationAndResetOriginalActivityOrientation() throws MraidCommandException, PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        assertThat(subject.getOriginalActivityOrientation()).isNull();
        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        subject.lockOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        assertThat(subject.getOriginalActivityOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        subject.unApplyOrientation();

        assertThat(subject.getOriginalActivityOrientation()).isNull();
        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void unapplyOrientation_withoutLockedOrientation_shouldNotChangeRequestedOrientation()
            throws PackageManager.NameNotFoundException {
        setMockActivityInfo(true, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
                ActivityInfo.CONFIG_ORIENTATION | ActivityInfo.CONFIG_SCREEN_SIZE);

        assertThat(subject.getOriginalActivityOrientation()).isNull();
        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        subject.unApplyOrientation();

        assertThat(subject.getOriginalActivityOrientation()).isNull();
        assertThat(activity.getRequestedOrientation())
                .isEqualTo(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Test
    public void destroy_shouldCancelLastMetricsRequest_shouldUnregisterBroadcastReceiver_shouldDetachAllBridges() {
        subject.destroy();

        verify(mockScreenMetricsWaiter).cancelLastRequest();
        verify(mockOrientationBroadcastReceiver).unregister();
        verify(mockBridge).detach();
        verify(mockTwoPartBridge).detach();
    }

    @Test
    public void destroy_withDefaultState_shouldSetMraidWebViewsToNull() {
        subject.setViewStateForTesting(ViewState.DEFAULT);
        assertThat(subject.getMraidWebView()).isNotNull();
        // The two-part WebView is null by default
        assertThat(subject.getTwoPartWebView()).isNull();

        subject.destroy();

        assertThat(subject.getMraidWebView()).isNull();
        assertThat(subject.getTwoPartWebView()).isNull();
    }

    @Test
    public void destroy_withExpandedState_shouldSetMraidWebViewsToNull()
            throws MraidCommandException {
        // Necessary to set up the webview before expanding. Also moves the state to DEFAULT.
        subject.handlePageLoad();
        assertThat(subject.getViewState()).isEqualTo(ViewState.DEFAULT);
        subject.handleExpand(URI.create("https://two-part-url"), false);

        assertThat(subject.getMraidWebView()).isNotNull();
        assertThat(subject.getTwoPartWebView()).isNotNull();

        subject.destroy();

        assertThat(subject.getMraidWebView()).isNull();
        assertThat(subject.getTwoPartWebView()).isNull();
    }

    @Test
    public void destroy_afterDestroy_shouldNotThrowAnException() {
        subject.destroy();
        subject.destroy();

        assertThat(subject.getMraidWebView()).isNull();
        assertThat(subject.getTwoPartWebView()).isNull();
    }

    @Test
    public void destroy_fromExpandedState_shouldRemoveCloseableAdContainerFromContentView()
            throws MraidCommandException {
        subject.handlePageLoad();
        subject.handleExpand(null, false);

        assertThat(rootView.getChildCount()).isEqualTo(1);

        subject.destroy();

        assertThat(rootView.getChildCount()).isEqualTo(0);
    }

    @Test
    public void destroy_fromResizedState_shouldRemoveCloseableAdContainerFromContentView()
            throws MraidCommandException {
        subject.handlePageLoad();
        subject.setRootViewSize(100, 100);
        subject.handleResize(100, 100, 0, 0, ClosePosition.TOP_RIGHT, true);

        assertThat(rootView.getChildCount()).isEqualTo(1);

        subject.destroy();

        assertThat(rootView.getChildCount()).isEqualTo(0);
    }

    private void setMockActivityInfo(final boolean activityInfoFound, int screenOrientation,
            int configChanges) throws PackageManager.NameNotFoundException {
        final ActivityInfo mockActivityInfo = mock(ActivityInfo.class);

        mockActivityInfo.screenOrientation = screenOrientation;
        mockActivityInfo.configChanges = configChanges;

        final PackageManager mockPackageManager = mock(PackageManager.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                if (!activityInfoFound) {
                    throw new PackageManager.NameNotFoundException("");
                }

                return mockActivityInfo;
            }
        }).when(mockPackageManager).getActivityInfo(any(ComponentName.class), anyInt());

        when(activity.getPackageManager()).thenReturn(mockPackageManager);
    }
}
