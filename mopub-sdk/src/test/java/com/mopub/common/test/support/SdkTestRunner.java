package com.mopub.common.test.support;

import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.event.MoPubEvents;
import com.mopub.common.factories.MethodBuilderFactory;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.DateAndTime;
import com.mopub.common.util.test.support.ShadowAsyncTasks;
import com.mopub.common.util.test.support.TestDateAndTime;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.factories.AdFetcherFactory;
import com.mopub.mobileads.factories.AdViewControllerFactory;
import com.mopub.mobileads.factories.CustomEventBannerAdapterFactory;
import com.mopub.mobileads.factories.CustomEventBannerFactory;
import com.mopub.mobileads.factories.CustomEventInterstitialAdapterFactory;
import com.mopub.mobileads.factories.CustomEventInterstitialFactory;
import com.mopub.mobileads.factories.HtmlBannerWebViewFactory;
import com.mopub.mobileads.factories.HtmlInterstitialWebViewFactory;
import com.mopub.mobileads.factories.HttpClientFactory;
import com.mopub.mobileads.factories.MoPubViewFactory;
import com.mopub.mobileads.factories.MraidViewFactory;
import com.mopub.mobileads.factories.VastManagerFactory;
import com.mopub.mobileads.factories.VastVideoDownloadTaskFactory;
import com.mopub.mobileads.factories.ViewGestureDetectorFactory;
import com.mopub.mobileads.test.support.TestAdFetcherFactory;
import com.mopub.mobileads.test.support.TestAdViewControllerFactory;
import com.mopub.mobileads.test.support.TestCustomEventBannerAdapterFactory;
import com.mopub.mobileads.test.support.TestCustomEventBannerFactory;
import com.mopub.mobileads.test.support.TestCustomEventInterstitialAdapterFactory;
import com.mopub.mobileads.test.support.TestCustomEventInterstitialFactory;
import com.mopub.mobileads.test.support.TestHtmlBannerWebViewFactory;
import com.mopub.mobileads.test.support.TestHtmlInterstitialWebViewFactory;
import com.mopub.mobileads.test.support.TestHttpClientFactory;
import com.mopub.mobileads.test.support.TestMoPubViewFactory;
import com.mopub.mobileads.test.support.TestMraidViewFactory;
import com.mopub.mobileads.test.support.TestVastManagerFactory;
import com.mopub.mobileads.test.support.TestVastVideoDownloadTaskFactory;
import com.mopub.mobileads.test.support.TestViewGestureDetectorFactory;
import com.mopub.nativeads.factories.CustomEventNativeFactory;
import com.mopub.nativeads.test.support.TestCustomEventNativeFactory;

import org.junit.runners.model.InitializationError;
import org.mockito.MockitoAnnotations;
import org.robolectric.DefaultTestLifecycle;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.TestLifecycle;
import org.robolectric.bytecode.ClassInfo;
import org.robolectric.bytecode.Setup;
import org.robolectric.util.RobolectricBackgroundExecutorService;

import java.lang.reflect.Method;

import static com.mopub.common.MoPub.LocationAwareness;
import static org.mockito.Mockito.mock;

public class SdkTestRunner extends RobolectricTestRunner {

    public SdkTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    public Setup createSetup() {
        return new Setup() {
            @Override
            public boolean shouldInstrument(ClassInfo classInfo) {
                return classInfo.getName().equals(AsyncTasks.class.getName())
                        || super.shouldInstrument(classInfo);
            }
        };
    }

    @Override
    protected Class<? extends TestLifecycle> getTestLifecycleClass() {
        return TestLifeCycleWithInjection.class;
    }

    public static class TestLifeCycleWithInjection extends DefaultTestLifecycle {
        @Override
        public void prepareTest(Object test) {
            AdFetcherFactory.setInstance(new TestAdFetcherFactory());
            HttpClientFactory.setInstance(new TestHttpClientFactory());
            DateAndTime.setInstance(new TestDateAndTime());
            CustomEventBannerFactory.setInstance(new TestCustomEventBannerFactory());
            CustomEventInterstitialFactory.setInstance(new TestCustomEventInterstitialFactory());
            CustomEventBannerAdapterFactory.setInstance(new TestCustomEventBannerAdapterFactory());
            MraidViewFactory.setInstance(new TestMraidViewFactory());
            MoPubViewFactory.setInstance(new TestMoPubViewFactory());
            CustomEventInterstitialAdapterFactory.setInstance(new TestCustomEventInterstitialAdapterFactory());
            HtmlBannerWebViewFactory.setInstance(new TestHtmlBannerWebViewFactory());
            HtmlInterstitialWebViewFactory.setInstance(new TestHtmlInterstitialWebViewFactory());
            AdViewControllerFactory.setInstance(new TestAdViewControllerFactory());
            ViewGestureDetectorFactory.setInstance(new TestViewGestureDetectorFactory());
            VastManagerFactory.setInstance(new TestVastManagerFactory());
            VastVideoDownloadTaskFactory.setInstance(new TestVastVideoDownloadTaskFactory());
            MethodBuilderFactory.setInstance(new TestMethodBuilderFactory());
            CustomEventNativeFactory.setInstance(new TestCustomEventNativeFactory());
            ShadowAsyncTasks.reset();
            MoPubEvents.setEventDispatcher(mock(MoPubEvents.EventDispatcher.class));
            MoPub.setLocationAwareness(LocationAwareness.NORMAL);
            MoPub.setLocationPrecision(6);

            MockitoAnnotations.initMocks(test);

            AsyncTasks.setExecutor(new RobolectricBackgroundExecutorService());
        }

        @Override
        public void afterTest(final Method method) {
            ClientMetadata.clearForTesting();
        }
    }
}
