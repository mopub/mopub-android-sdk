// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.View;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.test.support.ShadowReflection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(shadows = {ShadowReflection.class})
public class MoPubViewTest {
    private MoPubView subject;
    private Map<String,String> paramsMap = new HashMap<String, String>();
    @Mock
    private AdViewController adViewController;
    private Activity context;

    @Before
    public void setup() {
        context = spy(Robolectric.buildActivity(Activity.class).create().get());
        subject = new MoPubView(context);
        subject.setAdViewController(adViewController);
    }

    @Test
    public void screenStateBroadcastReceiver_withActionUserPresent_shouldUnpauseRefresh() throws Exception {
        broadcastIntent(new Intent(Intent.ACTION_USER_PRESENT));

        verify(adViewController).resumeRefresh();
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
        verify(adViewController, never()).resumeRefresh();
    }

    @Test
    public void screenStateBroadcastReceiver_withRandomIntent_shouldDoNothing() throws Exception {
        broadcastIntent(new Intent(Intent.ACTION_BATTERY_LOW));

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).resumeRefresh();
    }

    @Test
    public void screenStateBroadcastReceiver_whenAdInBackground_shouldDoNothing() throws Exception {
        subject.onWindowVisibilityChanged(View.INVISIBLE);
        reset(adViewController);

        broadcastIntent(new Intent(Intent.ACTION_USER_PRESENT));
        verify(adViewController, never()).resumeRefresh();

        broadcastIntent(new Intent(Intent.ACTION_SCREEN_OFF));
        verify(adViewController, never()).pauseRefresh();
    }

    @Test
    public void screenStateBroadcastReceiver_afterOnDestroy_shouldDoNothing() throws Exception {
        subject.destroy();

        broadcastIntent(new Intent(Intent.ACTION_USER_PRESENT));
        verify(adViewController, never()).resumeRefresh();

        broadcastIntent(new Intent(Intent.ACTION_SCREEN_OFF));
        verify(adViewController, never()).pauseRefresh();
    }

    @Test
    public void onWindowVisibilityChanged_fromVisibleToInvisible_shouldPauseRefresh() throws Exception {
        // Default visibility is View.VISIBLE
        subject.onWindowVisibilityChanged(View.INVISIBLE);

        verify(adViewController).pauseRefresh();
        verify(adViewController, never()).resumeRefresh();
    }


    @Test
    public void onWindowVisibilityChanged_fromInvisibleToVisible_shouldUnpauseRefresh() throws Exception {
        subject.onWindowVisibilityChanged(View.INVISIBLE);
        reset(adViewController);

        subject.onWindowVisibilityChanged(View.VISIBLE);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController).resumeRefresh();
    }

    @Test
    public void onWindowVisibilityChanged_fromVisibleToVisible_shouldDoNothing() throws Exception {
        // Default visibility is View.VISIBLE
        subject.onWindowVisibilityChanged(View.VISIBLE);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).resumeRefresh();
    }

    @Test
    public void onWindowVisibilityChanged_fromInvisibleToGone_shouldDoNothing() throws Exception {
        subject.onWindowVisibilityChanged(View.INVISIBLE);
        reset(adViewController);

        subject.onWindowVisibilityChanged(View.GONE);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).resumeRefresh();
    }

    @Test
    public void onWindowVisibilityChanged_fromGoneToInvisible_shouldDoNothing() throws Exception {
        subject.onWindowVisibilityChanged(View.GONE);
        reset(adViewController);

        subject.onWindowVisibilityChanged(View.INVISIBLE);

        verify(adViewController, never()).pauseRefresh();
        verify(adViewController, never()).resumeRefresh();
    }

    @Test
    public void setAutorefreshEnabled_withRefreshTrue_shouldForwardToAdViewController() throws Exception {
        subject.setAutorefreshEnabled(true);

        verify(adViewController).setShouldAllowAutoRefresh(true);
    }

    @Test
    public void setAutorefreshEnabled_withRefreshFalse_shouldForwardToAdViewController() throws Exception {
        subject.setAutorefreshEnabled(false);

        verify(adViewController).setShouldAllowAutoRefresh(false);
    }

    @Test
    public void invalidateAdapter_withReflection_shouldExist() throws Exception {
        assertThat(Reflection.getDeclaredMethodWithTraversal(InlineAdAdapter.class,
                "invalidate")).isNotNull();
    }

    @Test
    public void loadAd_withoutRequestedAdSize_shouldSetRequestedAdSizeToZeroZero() throws Exception {
        subject.loadAd();
        final Point point = new Point(0, 0);
        verify(adViewController).setRequestedAdSize(point);
    }

    @Test
    public void loadAd_withAdSize50Height_withFhdScreen_withDensityOf1_shouldResolveWithHeight50() {
        final float density = 1.0f;
        final MoPubView.MoPubAdSize adSize = MoPubView.MoPubAdSize.HEIGHT_50;

        // Set the expected screen dimensions
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = 1080;
        mockDisplayMetrics.heightPixels = 1920;
        mockDisplayMetrics.density = density;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);

        final Point point = new Point(0, (int)(adSize.toInt() * density));
        subject.loadAd(adSize);
        verify(adViewController).setRequestedAdSize(point);
    }

    @Test
    public void loadAd_withAdSize90Height_withFhdScreen_withDensityOf1_shouldResolveWithHeight90() {
        final float density = 1.0f;
        final MoPubView.MoPubAdSize adSize = MoPubView.MoPubAdSize.HEIGHT_90;

        // Set the expected screen dimensions
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = 1080;
        mockDisplayMetrics.heightPixels = 1920;
        mockDisplayMetrics.density = density;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);

        final Point point = new Point(0, (int)(adSize.toInt() * density));
        subject.loadAd(adSize);
        verify(adViewController).setRequestedAdSize(point);
    }

    @Test
    public void loadAd_withAdSize250Height_withFhdScreen_withDensityOf2_shouldResolveWithHeight500() {
        final float density = 2.0f;
        final MoPubView.MoPubAdSize adSize = MoPubView.MoPubAdSize.HEIGHT_250;

        // Set the expected screen dimensions
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = 1080;
        mockDisplayMetrics.heightPixels = 1920;
        mockDisplayMetrics.density = density;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);

        final Point point = new Point(0, (int)(adSize.toInt() * density));
        subject.loadAd(adSize);
        verify(adViewController).setRequestedAdSize(point);
    }

    @Test
    public void loadAd_withAdSize280Height_withFhdScreen_withDensityOf3_shouldResolveWithHeight840() {
        final float density = 3.0f;
        final MoPubView.MoPubAdSize adSize = MoPubView.MoPubAdSize.HEIGHT_280;

        // Set the expected screen dimensions
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = 1080;
        mockDisplayMetrics.heightPixels = 1920;
        mockDisplayMetrics.density = density;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);

        final Point point = new Point(0, (int)(adSize.toInt() * density));
        subject.loadAd(adSize);
        verify(adViewController).setRequestedAdSize(point);
    }

    private void broadcastIntent(final Intent intent) {
        final List<ShadowApplication.Wrapper> wrappers = ShadowApplication.getInstance().getRegisteredReceivers();

        for (final ShadowApplication.Wrapper wrapper : wrappers) {
            wrapper.broadcastReceiver.onReceive(context, intent);
        }
    }

    @Test
    public void loadAd_withIrrationalDensity_shouldRoundhHeightUp_1() {
        final float density = 1.9800001f;
        final MoPubView.MoPubAdSize adSize = MoPubView.MoPubAdSize.HEIGHT_50;

        // Set the expected screen dimensions
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = 1080;
        mockDisplayMetrics.heightPixels = 1920;
        mockDisplayMetrics.density = density;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);

        final Point point = new Point(0, adSize.toInt() * 2);
        subject.loadAd(adSize);
        verify(adViewController).setRequestedAdSize(point);
    }

    @Test
    public void loadAd_withIrrationalDensity_shouldRoundhHeightUp_2() {
        final float density = 1.98f;
        final MoPubView.MoPubAdSize adSize = MoPubView.MoPubAdSize.HEIGHT_50;

        // Set the expected screen dimensions
        final Resources spyResources = spy(context.getResources());
        final DisplayMetrics mockDisplayMetrics = mock(DisplayMetrics.class);
        mockDisplayMetrics.widthPixels = 1080;
        mockDisplayMetrics.heightPixels = 1920;
        mockDisplayMetrics.density = density;
        when(spyResources.getDisplayMetrics()).thenReturn(mockDisplayMetrics);
        when(context.getResources()).thenReturn(spyResources);

        final Point point = new Point(0, adSize.toInt() * 2 - 1);
        subject.loadAd(adSize);
        verify(adViewController).setRequestedAdSize(point);
    }

}
