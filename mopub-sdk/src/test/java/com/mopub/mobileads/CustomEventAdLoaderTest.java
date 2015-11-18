package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class CustomEventAdLoaderTest {

    private AdViewController adViewController;
    private AdLoader.CustomEventAdLoader subject;
    private String customEventName;
    private Map<String, String> serverExtras;
    private MoPubView moPubView;

    @Before
    public void setup() {
        moPubView = mock(MoPubView.class);
        adViewController = mock(AdViewController.class);
        stub(adViewController.getMoPubView()).toReturn(moPubView);
        customEventName = "testCustomEvent";
        serverExtras = new HashMap<String, String>();
        subject = new AdLoader.CustomEventAdLoader(adViewController,
                customEventName, serverExtras);
    }

    @Test
    public void execute_shouldCallLoadCustomEvent() throws Exception {
        subject.load();

        verify(adViewController).setNotLoading();
        verify(moPubView).loadCustomEvent(eq(customEventName), eq(serverExtras));
    }

    @Test
    public void execute_whenAdViewControllerIsNull_shouldDoNothing() throws Exception {
        subject = new AdLoader.CustomEventAdLoader(null, customEventName, serverExtras);

        subject.load();
        // pass
    }

    @Test
    public void execute_whenAdViewControllerIsDestroyed_shouldDoNothing() throws Exception {
        stub(adViewController.isDestroyed()).toReturn(true);

        subject.load();

        verify(adViewController, never()).setNotLoading();
        verify(moPubView, never()).loadCustomEvent(eq(customEventName), eq(serverExtras));
    }

    @Test
    public void execute_whenCustomEventIsNull_shouldDoNothing() {
        subject = new AdLoader.CustomEventAdLoader(adViewController, null, serverExtras);

        subject.load();

        verify(adViewController, never()).setNotLoading();
        verify(moPubView, never()).loadCustomEvent((String) eq(null), eq(serverExtras));
    }

    @Test
    public void execute_whenParamsMapIsNull_shouldLoadNullParamsMap() throws Exception {
        subject = new AdLoader.CustomEventAdLoader(adViewController, customEventName, null);

        subject.load();

        verify(adViewController).setNotLoading();
        verify(moPubView).loadCustomEvent(eq(customEventName), (Map<String, String>) eq(null));
    }
}
