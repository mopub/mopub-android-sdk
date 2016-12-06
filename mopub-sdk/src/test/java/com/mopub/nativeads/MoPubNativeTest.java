package com.mopub.nativeads;

import android.app.Activity;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.ShadowAsyncTasks;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.BuildConfig;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.nativeads.MoPubNative.MoPubNativeNetworkListener;
import com.mopub.network.MoPubNetworkError;
import com.mopub.network.MoPubRequestQueue;
import com.mopub.network.Networking;
import com.mopub.volley.NoConnectionError;
import com.mopub.volley.Request;
import com.mopub.volley.VolleyError;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.INTERNET;
import static com.mopub.common.VolleyRequestMatcher.isUrl;
import static com.mopub.common.util.Reflection.MethodBuilder;
import static com.mopub.nativeads.MoPubNative.EMPTY_NETWORK_LISTENER;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowAsyncTasks.class})
public class MoPubNativeTest {
    private MoPubNative subject;
    private MethodBuilder methodBuilder;
    private Activity context;
    private static final String adUnitId = "test_adunit_id";

    @Mock private MoPubNativeNetworkListener mockNetworkListener;
    @Mock private MoPubRequestQueue mockRequestQueue;
    @Mock private AdRendererRegistry mockAdRendererRegistry;
    @Mock private MoPubStaticNativeAdRenderer mockRenderer;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        Shadows.shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);
        Shadows.shadowOf(context).grantPermissions(INTERNET);
        subject = new MoPubNative(context, adUnitId, mockAdRendererRegistry, mockNetworkListener);
        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        Networking.setRequestQueueForTesting(mockRequestQueue);
    }

    @After
    public void tearDown() {
        reset(methodBuilder);
    }

    @Test
    public void registerAdRenderer_shouldCallAdRednererRegistryRegisterAdRenderer() throws Exception {
        subject.registerAdRenderer(mockRenderer);

        verify(mockAdRendererRegistry).registerAdRenderer(mockRenderer);
    }

    @Test
    public void destroy_shouldSetListenersToEmptyAndClearContext() {
        assertThat(subject.getContextOrDestroy()).isSameAs(context);
        assertThat(subject.getMoPubNativeNetworkListener()).isSameAs(mockNetworkListener);

        subject.destroy();

        assertThat(subject.getContextOrDestroy()).isNull();
        assertThat(subject.getMoPubNativeNetworkListener()).isSameAs(EMPTY_NETWORK_LISTENER);
    }

    @Test
    public void loadNativeAd_shouldReturnFast() {
        Robolectric.getForegroundThreadScheduler().pause();

        subject.destroy();
        subject.makeRequest();

        assertThat(Robolectric.getForegroundThreadScheduler().size()).isEqualTo(0);
    }

    @Test
    public void requestNativeAd_shouldFireNetworkRequest() {
        subject.requestNativeAd("https://www.mopub.com");

        verify(mockNetworkListener, never()).onNativeFail(any(NativeErrorCode.class));
        verify(mockRequestQueue).add(argThat(isUrl("https://www.mopub.com")));
    }

    @Test
    public void requestNativeAd_whenReqeustQueueDeliversUnknownError_shouldFireNativeFail() {

        when(mockRequestQueue.add(any(Request.class)))
                .then(new Answer<Void>() {
                    @Override
                    public Void answer(final InvocationOnMock invocationOnMock) throws Throwable {
                        ((Request) invocationOnMock.getArguments()[0]).deliverError(new VolleyError(new MalformedURLException()));
                        return null;
                    }
                });
        subject.requestNativeAd("//\\//\\::::");

        verify(mockNetworkListener).onNativeFail(any(NativeErrorCode.class));
    }

    @Test
    public void requestNativeAd_withNullUrl_shouldFireNativeFail() {
        Robolectric.getForegroundThreadScheduler().pause();

        subject.requestNativeAd(null);

        verify(mockNetworkListener).onNativeFail(any(NativeErrorCode.class));
        verify(mockRequestQueue, never()).add(any(Request.class));
    }

    @Test
    public void onAdError_shouldNotifyListener() {
        subject.onAdError(new MoPubNetworkError(MoPubNetworkError.Reason.BAD_BODY));

        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.INVALID_RESPONSE));
    }

    @Test
    public void onAdError_whenNotMoPubError_shouldNotifyListener() {
        subject.onAdError(new VolleyError("generic"));

        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.UNSPECIFIED));
    }

    @Test
    public void onAdError_withVolleyErrorWarmingUp_shouldLogMoPubErrorCodeWarmup_shouldNotifyListener() {
        MoPubLog.setSdkHandlerLevel(Level.ALL);

        subject.onAdError(new MoPubNetworkError(MoPubNetworkError.Reason.WARMING_UP));

        final List<ShadowLog.LogItem> allLogMessages = ShadowLog.getLogs();
        final ShadowLog.LogItem latestLogMessage = allLogMessages.get(allLogMessages.size() - 1);

        // All log messages end with a newline character.
        assertThat(latestLogMessage.msg.trim()).isEqualTo(MoPubErrorCode.WARMUP.toString());
        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.EMPTY_AD_RESPONSE));
    }

    @Test
    public void onAdError_withNoConnection_shouldLogMoPubErrorCodeNoConnection_shouldNotifyListener() {
        MoPubLog.setSdkHandlerLevel(Level.ALL);
        Shadows.shadowOf(context).denyPermissions(INTERNET);

        subject.onAdError(new NoConnectionError());

        final List<ShadowLog.LogItem> allLogMessages = ShadowLog.getLogs();
        final ShadowLog.LogItem latestLogMessage = allLogMessages.get(allLogMessages.size() - 1);

        // All log messages end with a newline character.
        assertThat(latestLogMessage.msg.trim()).isEqualTo(MoPubErrorCode.NO_CONNECTION.toString());
        verify(mockNetworkListener).onNativeFail(eq(NativeErrorCode.CONNECTION_ERROR));
    }
}
