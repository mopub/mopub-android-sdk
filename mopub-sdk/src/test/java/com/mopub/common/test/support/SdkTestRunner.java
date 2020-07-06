// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.test.support;

import androidx.annotation.NonNull;

import com.mopub.common.CacheService;
import com.mopub.common.ClientMetadata;
import com.mopub.common.MoPub;
import com.mopub.common.Preconditions;
import com.mopub.common.factories.MethodBuilderFactory;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.DateAndTime;
import com.mopub.common.util.test.support.ShadowAsyncTasks;
import com.mopub.common.util.test.support.ShadowMoPubHttpUrlConnection;
import com.mopub.common.util.test.support.ShadowReflection;
import com.mopub.common.util.test.support.TestDateAndTime;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.factories.AdViewControllerFactory;
import com.mopub.mobileads.factories.BaseAdFactory;
import com.mopub.mobileads.factories.FullscreenAdAdapterFactory;
import com.mopub.mobileads.factories.MediaPlayerFactory;
import com.mopub.mobileads.factories.MoPubViewFactory;
import com.mopub.mobileads.factories.MraidControllerFactory;
import com.mopub.mobileads.factories.VastManagerFactory;
import com.mopub.mobileads.factories.VideoViewFactory;
import com.mopub.mobileads.test.support.TestAdViewControllerFactory;
import com.mopub.mobileads.test.support.TestBaseAdFactory;
import com.mopub.mobileads.test.support.TestFullscreenAdAdapterFactory;
import com.mopub.mobileads.test.support.TestMediaPlayerFactory;
import com.mopub.mobileads.test.support.TestMoPubViewFactory;
import com.mopub.mobileads.test.support.TestMraidControllerFactory;
import com.mopub.mobileads.test.support.TestVastManagerFactory;
import com.mopub.mobileads.test.support.TestVideoViewFactory;
import com.mopub.nativeads.factories.CustomEventNativeFactory;
import com.mopub.nativeads.test.support.TestCustomEventNativeFactory;

import org.junit.runners.model.InitializationError;
import org.mockito.MockitoAnnotations;
import org.robolectric.DefaultTestLifecycle;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.TestLifecycle;
import org.robolectric.android.util.concurrent.RoboExecutorService;

import static com.mopub.common.MoPub.LocationAwareness;

public class SdkTestRunner extends RobolectricTestRunner {

    public SdkTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    @NonNull
    protected Class<? extends TestLifecycle> getTestLifecycleClass() {
        return TestLifeCycleWithInjection.class;
    }

    public static class TestLifeCycleWithInjection extends DefaultTestLifecycle {
        @Override
        public void prepareTest(Object test) {
            ClientMetadata.clearForTesting();

            // Precondition exceptions should not be thrown during tests so that we can test
            // for unexpected behavior even after failing a precondition.
            Preconditions.NoThrow.setStrictMode(false);

            DateAndTime.setInstance(new TestDateAndTime());
            MoPubViewFactory.setInstance(new TestMoPubViewFactory());
            FullscreenAdAdapterFactory.setInstance(new TestFullscreenAdAdapterFactory());
            BaseAdFactory.setInstance(new TestBaseAdFactory());
            AdViewControllerFactory.setInstance(new TestAdViewControllerFactory());
            VastManagerFactory.setInstance(new TestVastManagerFactory());
            MediaPlayerFactory.Companion.setInstance(new TestMediaPlayerFactory());
            VideoViewFactory.Companion.setInstance(new TestVideoViewFactory());
            MethodBuilderFactory.setInstance(new TestMethodBuilderFactory());
            CustomEventNativeFactory.setInstance(new TestCustomEventNativeFactory());
            MraidControllerFactory.setInstance(new TestMraidControllerFactory());

            ShadowAsyncTasks.reset();
            ShadowMoPubHttpUrlConnection.reset();
            ShadowReflection.reset();

            MoPub.setLocationAwareness(LocationAwareness.NORMAL);
            MoPub.setLocationPrecision(6);

            MockitoAnnotations.initMocks(test);

            AsyncTasks.setExecutor(new RoboExecutorService());
            CacheService.clearAndNullCaches();
        }
    }
}
