package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestAdViewControllerFactory;
import com.mopub.mobileads.test.support.TestCustomEventBannerAdapterFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowApplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mopub.common.util.ResponseHeader.CUSTOM_EVENT_DATA;
import static com.mopub.common.util.ResponseHeader.CUSTOM_EVENT_HTML_DATA;
import static com.mopub.common.util.ResponseHeader.CUSTOM_EVENT_NAME;
import static com.mopub.mobileads.MoPubErrorCode.ADAPTER_NOT_FOUND;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class MoPubViewTest {
    private MoPubView subject;
    private Map<String,String> paramsMap = new HashMap<String, String>();
    private CustomEventBannerAdapter customEventBannerAdapter;
    private AdViewController adViewController;
    private Context context;

    @Before
    public void setup() {
        context = new Activity();
        subject = new MoPubView(context);
        customEventBannerAdapter = TestCustomEventBannerAdapterFactory.getSingletonMock();
        reset(customEventBannerAdapter);
        adViewController = TestAdViewControllerFactory.getSingletonMock();
    }

    @Test
    public void screenStateBroadcastReceiver_withActionUserPresent_shouldUnpauseRefresh() throws Exception {
        broadcastIntent(new Intent(Intent.ACTION_USER_PRESENT));

        verify(adViewController).unpauseRefresh();
    }

    @Test
    public void screenStateBroadcastReceiver_withActionScreenOff_shouldPauseRefersh() throws Exception {
        broadcastIntent(new Intent(Intent.ACTION_SCREEN_OFF));

        verify(adViewController).pauseRefresh();
    }

    @Test
    public void screenStateBroadcastReceiver_withNullIntent_shouldDoNothing() throws Exception {
        broadcastIntent(null);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).unpauseRefresh();
    }

    @Test
    public void screenStateBroadcastReceiver_withRandomIntent_shouldDoNothing() throws Exception {
        broadcastIntent(new Intent(Intent.ACTION_BATTERY_LOW));

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).unpauseRefresh();
    }

    @Test
    public void screenStateBroadcastReceiver_whenAdInBackground_shouldDoNothing() throws Exception {
        subject.onWindowVisibilityChanged(View.INVISIBLE);
        reset(adViewController);

        broadcastIntent(new Intent(Intent.ACTION_USER_PRESENT));
        verify(adViewController, never()).unpauseRefresh();

        broadcastIntent(new Intent(Intent.ACTION_SCREEN_OFF));
        verify(adViewController, never()).pauseRefresh();
    }

    @Test
    public void screenStateBroadcastReceiver_afterOnDestroy_shouldDoNothing() throws Exception {
        subject.destroy();

        broadcastIntent(new Intent(Intent.ACTION_USER_PRESENT));
        verify(adViewController, never()).unpauseRefresh();

        broadcastIntent(new Intent(Intent.ACTION_SCREEN_OFF));
        verify(adViewController, never()).pauseRefresh();
    }

    @Test
    public void onWindowVisibilityChanged_fromVisibleToInvisible_shouldPauseRefresh() throws Exception {
        // Default visibility is View.VISIBLE
        subject.onWindowVisibilityChanged(View.INVISIBLE);

        verify(adViewController).pauseRefresh();
        verify(adViewController, never()).unpauseRefresh();
    }


    @Test
    public void onWindowVisibilityChanged_fromInvisibleToVisible_shouldUnpauseRefresh() throws Exception {
        subject.onWindowVisibilityChanged(View.INVISIBLE);
        reset(adViewController);

        subject.onWindowVisibilityChanged(View.VISIBLE);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController).unpauseRefresh();
    }

    @Test
    public void onWindowVisibilityChanged_fromVisibleToVisible_shouldDoNothing() throws Exception {
        // Default visibility is View.VISIBLE
        subject.onWindowVisibilityChanged(View.VISIBLE);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).unpauseRefresh();
    }

    @Test
    public void onWindowVisibilityChanged_fromInvisibleToGone_shouldDoNothing() throws Exception {
        subject.onWindowVisibilityChanged(View.INVISIBLE);
        reset(adViewController);

        subject.onWindowVisibilityChanged(View.GONE);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).unpauseRefresh();
    }

    @Test
    public void onWindowVisibilityChanged_fromGoneToInvisible_shouldDoNothing() throws Exception {
        subject.onWindowVisibilityChanged(View.GONE);
        reset(adViewController);

        subject.onWindowVisibilityChanged(View.INVISIBLE);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).unpauseRefresh();
    }

    @Test
    public void setAutorefreshEnabled_withRefreshTrue_shouldForwardToAdViewController() throws Exception {
        subject.setAutorefreshEnabled(true);

        verify(adViewController).forceSetAutorefreshEnabled(true);
    }

    @Test
    public void setAutorefreshEnabled_withRefreshFalse_shouldForwardToAdViewController() throws Exception {
        subject.setAutorefreshEnabled(false);

        verify(adViewController).forceSetAutorefreshEnabled(false);
    }
    
    @Test
    public void nativeAdLoaded_shouldScheduleRefreshTimer() throws Exception {
        subject.nativeAdLoaded();

        verify(adViewController).scheduleRefreshTimerIfEnabled();
    }

    @Test
    public void loadCustomEvent_shouldInitializeCustomEventBannerAdapter() throws Exception {
        paramsMap.put(CUSTOM_EVENT_NAME.getKey(), "name");
        paramsMap.put(CUSTOM_EVENT_DATA.getKey(), "data");
        paramsMap.put(CUSTOM_EVENT_HTML_DATA.getKey(), "html");
        subject.loadCustomEvent(paramsMap);

        assertThat(TestCustomEventBannerAdapterFactory.getLatestMoPubView()).isEqualTo(subject);
        assertThat(TestCustomEventBannerAdapterFactory.getLatestClassName()).isEqualTo("name");
        assertThat(TestCustomEventBannerAdapterFactory.getLatestClassData()).isEqualTo("data");

        verify(customEventBannerAdapter).loadAd();
    }

    @Test
    public void loadCustomEvent_whenParamsMapIsNull_shouldCallLoadFailUrl() throws Exception {
        subject.loadCustomEvent(null);

        verify(adViewController).loadFailUrl(eq(ADAPTER_NOT_FOUND));
        verify(customEventBannerAdapter, never()).invalidate();
        verify(customEventBannerAdapter, never()).loadAd();
    }

    @Test
    public void setLocationAwarenss_shouldChangeGlobalSetting() {
        assertThat(MoPub.getLocationAwareness()).isEqualTo(MoPub.LocationAwareness.NORMAL);
        subject.setLocationAwareness(LocationService.LocationAwareness.DISABLED);
        assertThat(MoPub.getLocationAwareness()).isEqualTo(MoPub.LocationAwareness.DISABLED);
    }

    private void broadcastIntent(final Intent intent) {
        final List<ShadowApplication.Wrapper> wrappers = Robolectric.getShadowApplication().getRegisteredReceivers();

        for (final ShadowApplication.Wrapper wrapper : wrappers) {
            wrapper.broadcastReceiver.onReceive(context, intent);
        }
    }
}
