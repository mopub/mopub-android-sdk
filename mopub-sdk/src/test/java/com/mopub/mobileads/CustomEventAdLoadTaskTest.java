package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class CustomEventAdLoadTaskTest {

    private AdViewController adViewController;
    private AdLoadTask.CustomEventAdLoadTask subject;
    private Map<String, String> paramsMap;
    private MoPubView moPubView;

    @Before
    public void setup() {
        moPubView = mock(MoPubView.class);
        adViewController = mock(AdViewController.class);
        stub(adViewController.getMoPubView()).toReturn(moPubView);
        paramsMap = new HashMap<String, String>();
        subject = new AdLoadTask.CustomEventAdLoadTask(adViewController, paramsMap);
    }

    @Test
    public void execute_shouldCallLoadCustomEvent() throws Exception {
        subject.execute();

        verify(adViewController).setNotLoading();
        verify(moPubView).loadCustomEvent(eq(paramsMap));
    }

    @Test
    public void execute_whenAdViewControllerIsNull_shouldDoNothing() throws Exception {
        subject = new AdLoadTask.CustomEventAdLoadTask(null, paramsMap);

        subject.execute();
        // pass
    }

    @Test
    public void execute_whenAdViewControllerIsDestroyed_shouldDoNothing() throws Exception {
        stub(adViewController.isDestroyed()).toReturn(true);

        subject.execute();

        verify(adViewController, never()).setNotLoading();
        verify(moPubView, never()).loadCustomEvent(eq(paramsMap));
    }

    @Test
    public void execute_whenParamsMapIsNull_shouldLoadNullParamsMap() throws Exception {
        subject = new AdLoadTask.CustomEventAdLoadTask(adViewController, null);

        subject.execute();

        verify(adViewController).setNotLoading();
        verify(moPubView).loadCustomEvent((Map<String, String>) eq(null));
    }

    @Test
    public void execute_afterCleanup_shouldLoadNullParamsMap() throws Exception {
        subject.cleanup();
        subject.execute();

        verify(adViewController).setNotLoading();
        verify(moPubView).loadCustomEvent((Map<String, String>) eq(null));
    }
}
