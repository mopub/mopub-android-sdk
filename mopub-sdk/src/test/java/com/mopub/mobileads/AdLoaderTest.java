package com.mopub.mobileads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.network.AdResponse;

import org.fest.assertions.core.Condition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdLoaderTest {

    @Mock
    private AdViewController adViewController;
    @Mock
    private MoPubView moPubView;
    private AdResponse adResponse;
    private Map<String, String> serverExtras;

    @Before
    public void setup() {
        adResponse = new AdResponse.Builder()
                .setResponseBody("<html></html>")
                .setClickTrackingUrl("clickthrough")
                .setRedirectUrl("redirect")
                .setScrollable(false)
                .build();
        serverExtras = new HashMap<String, String>();
        serverExtras.put("test", "hi");
        when(adViewController.getMoPubView()).thenReturn(moPubView);
    }

    @Test
    public void fromAdResponse_whenCustomEvent_shouldCreateAdLoadTask() throws Exception {
        adResponse = adResponse.toBuilder()
                .setAdType("custom")
                .setCustomEventClassName("custom event name")
                .setServerExtras(serverExtras)
                .build();


        AdLoader.CustomEventAdLoader customEventTask = (AdLoader.CustomEventAdLoader) AdLoader.fromAdResponse(adResponse, adViewController);
        assertThat(customEventTask).isNotNull();
        assertThat(customEventTask.getServerExtras()).isEqualTo(serverExtras);
    }


    @Test
    public void fromAdResponse_whenHtml_shouldBeModernAdLoadTask() {
        adResponse = adResponse.toBuilder()
                .setAdType("html")
                .setCustomEventClassName("com.mopub.mobileads.HtmlBanner")
                .setServerExtras(serverExtras)
                .build();

        AdLoader.CustomEventAdLoader customEventTask = (AdLoader.CustomEventAdLoader) AdLoader.fromAdResponse(adResponse, adViewController);
        assertThat(customEventTask).isNotNull();
        assertThat(customEventTask.getServerExtras()).has(new Condition<Map<String, String>>() {
            @Override
            public boolean matches(final Map<String, String> stringStringMap) {
                return stringStringMap.containsKey("test");
            }
        });
    }

    @Test
    public void fromAdResponse_whenCustomMethod_shouldReturnNull() {
        adResponse = adResponse.toBuilder()
                .setCustomEventClassName(null)
                .build();

        assertThat(AdLoader.fromAdResponse(adResponse, adViewController)).isNull();
    }

    @Test
    public void load_shouldCallAdViewController() {
        adResponse = adResponse.toBuilder()
                .setAdType("custom")
                .setCustomEventClassName("custom event name")
                .setServerExtras(serverExtras)
                .build();


        AdLoader.CustomEventAdLoader customEventTask = (AdLoader.CustomEventAdLoader) AdLoader.fromAdResponse(adResponse, adViewController);

        customEventTask.load();
        verify(adViewController).getMoPubView();
        verify(moPubView).loadCustomEvent(eq("custom event name"), anyMapOf(String.class, String.class));
    }

    @Test
    public void load_controllerDestroyed_shouldDoNothing() {
        when(adViewController.isDestroyed()).thenReturn(true);

        adResponse = adResponse.toBuilder()
                .setAdType("custom")
                .setCustomEventClassName("custom event name")
                .setServerExtras(serverExtras)
                .build();


        AdLoader.CustomEventAdLoader customEventTask = (AdLoader.CustomEventAdLoader) AdLoader.fromAdResponse(adResponse, adViewController);

        customEventTask.load();
        verify(adViewController).isDestroyed();
        verifyNoMoreInteractions(adViewController);
        verifyZeroInteractions(moPubView);
    }

    @Test
    public void load_withNullMoPubView_shouldDoNothing() throws Exception {
        when(adViewController.getMoPubView()).thenReturn(null);

        adResponse = adResponse.toBuilder()
                .setAdType("custom")
                .setCustomEventClassName("custom event name")
                .setServerExtras(serverExtras)
                .build();

        AdLoader.CustomEventAdLoader customEventTask = (AdLoader.CustomEventAdLoader) AdLoader.fromAdResponse(adResponse, adViewController);

        customEventTask.load();

        verify(adViewController).getMoPubView();
        verifyZeroInteractions(moPubView);
    }
}
