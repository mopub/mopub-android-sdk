package com.mopub.network;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.GpsHelper;
import com.mopub.common.GpsHelperTest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class PlayServicesUrlRewriterTest {

    private Reflection.MethodBuilder methodBuilder;
    private PlayServicesUrlRewriter subject;

    @Before
    public void setUp() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        GpsHelper.setClassNamesForTesting();
        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        reset(methodBuilder);
        subject = new PlayServicesUrlRewriter("sha:testDeviceID", context);
    }

    @Test
    public void rewriteUrl_whenPlayServicesPresent_shouldUsePlayServicesValue() throws Exception {
        GpsHelperTest.TestAdInfo adInfo = new GpsHelperTest.TestAdInfo();
        when(methodBuilder.setStatic(any(Class.class))).thenReturn(methodBuilder);
        when(methodBuilder.addParam(any(Class.class), any())).thenReturn(methodBuilder);
        when(methodBuilder.execute()).thenReturn(
                GpsHelper.GOOGLE_PLAY_SUCCESS_CODE,
                adInfo,
                adInfo.ADVERTISING_ID,
                adInfo.LIMIT_AD_TRACKING_ENABLED
        );

        assertThat(subject.rewriteUrl("https://ads.mopub.com/m/ad?ad_id=abcece&udid=mp_tmpl_advertising_id&dnt=mp_tmpl_do_not_track"))
                .isEqualToIgnoringCase("https://ads.mopub.com/m/ad?ad_id=abcece&udid=ifa%3A38400000-8cf0-11bd-b23e-10b96e40000d&dnt=1");
    }

    @Test
    public void rewriteUrl_whenPlayServicesNotPresent_shouldUseDeviceValue() throws Exception {
        when(methodBuilder.setStatic(any(Class.class))).thenReturn(methodBuilder);
        when(methodBuilder.addParam(any(Class.class), any())).thenReturn(methodBuilder);
        // return error code so it fails
        when(methodBuilder.execute()).thenReturn(GpsHelper.GOOGLE_PLAY_SUCCESS_CODE + 1);

        assertThat(subject.rewriteUrl("https://ads.mopub.com/m/ad?ad_id=abcece&udid=mp_tmpl_advertising_id&dnt=mp_tmpl_do_not_track"))
                .isEqualToIgnoringCase("https://ads.mopub.com/m/ad?ad_id=abcece&udid=sha%3AtestDeviceId&dnt=0");
    }

    @Test
    public void rewriteUrl_noTemplates_shouldReturnIdentical() throws Exception {
        assertThat(subject.rewriteUrl("https://ads.mopub.com/m/ad")).isEqualTo("https://ads.mopub.com/m/ad");
    }
}
