// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.app.Activity;
import android.os.Build;

import com.mopub.common.privacy.MoPubIdentifier;
import com.mopub.common.privacy.MoPubIdentifierTest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.TestDateAndTime;
import com.mopub.mobileads.BuildConfig;
import com.mopub.network.AdResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class AdReportTest {

    @Mock
    private ClientMetadata mockClientMetadata;
    @Mock
    private AdResponse mockAdResponse;

    private Date now;
    private Activity context;
    public AdReport subject;

    @Before
    public void setup() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        now = new Date();
        TestDateAndTime.getInstance().setNow(now);
        MoPubIdentifierTest.writeAdvertisingInfoToSharedPreferences(context, true);
    }

    @After
    public void tearDown(){
        MoPubIdentifierTest.clearPreferences(context);
    }

    @Test
    public void testToString_shouldProperlyConstructParametersTextFile() {
        String expectedParameters =
                "sdk_version : 1.15.2.2\n" +
                        "creative_id : \n" +
                        "platform_version : "+ Integer.toString(Build.VERSION.SDK_INT) +"\n" +
                        "device_model : android\n" +
                        "ad_unit_id : testAdUnit\n" +
                        "device_locale : en_US\n" +
                        "device_id : "+MoPubIdentifierTest.TEST_MOPUB_ID+"\n" +
                        "network_type : unknown\n" +
                        "platform : android\n" +
                        "timestamp : " + getCurrentDateTime() + "\n" +
                        "ad_type : interstitial\n" +
                        "ad_size : {480, 320}\n";

        when(mockClientMetadata.getSdkVersion()).thenReturn("1.15.2.2");
        when(mockAdResponse.getDspCreativeId()).thenReturn("");
        when(mockClientMetadata.getDeviceModel()).thenReturn("android");
        when(mockClientMetadata.getDeviceLocale()).thenReturn(Locale.US);
        when(mockClientMetadata.getMoPubIdentifier()).thenReturn(new MoPubIdentifier(context));

        when(mockAdResponse.getNetworkType()).thenReturn("unknown");
        when(mockAdResponse.getTimestamp()).thenReturn(now.getTime());
        when(mockAdResponse.getAdType()).thenReturn("interstitial");
        when(mockAdResponse.getWidth()).thenReturn(480);
        when(mockAdResponse.getHeight()).thenReturn(320);

        subject = new AdReport("testAdUnit", mockClientMetadata, mockAdResponse);
        assertThat(subject.toString()).isEqualTo(expectedParameters);
    }

    @Test
    public void constructor_shouldHandleInvalidAdConfigurationValues() {
        String expectedParameters =
                "sdk_version : null\n" +
                        "creative_id : null\n" +
                        "platform_version : "+ Integer.toString(Build.VERSION.SDK_INT) +"\n" +
                        "device_model : null\n" +
                        "ad_unit_id : testAdUnit\n" +
                        "device_locale : null\n" +
                        "device_id : "+MoPubIdentifierTest.TEST_MOPUB_ID+"\n" +
                        "network_type : null\n" +
                        "platform : android\n" +
                        "timestamp : null" + "\n" +
                        "ad_type : null\n" +
                        "ad_size : {0, 0}\n";

        when(mockClientMetadata.getSdkVersion()).thenReturn(null);
        when(mockAdResponse.getDspCreativeId()).thenReturn(null);
        when(mockClientMetadata.getDeviceLocale()).thenReturn(null);
        when(mockAdResponse.getNetworkType()).thenReturn(null);
        when(mockClientMetadata.getMoPubIdentifier()).thenReturn(new MoPubIdentifier(context));

        when(mockAdResponse.getTimestamp()).thenReturn(-1L);
        when(mockAdResponse.getAdType()).thenReturn(null);
        when(mockAdResponse.getWidth()).thenReturn(null);
        when(mockAdResponse.getHeight()).thenReturn(null);

        subject = new AdReport("testAdUnit", mockClientMetadata, mockAdResponse);
        assertThat(subject.toString()).isEqualTo(expectedParameters);
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy hh:mm:ss a z", Locale.US);
        return dateFormat.format(now);
    }
}
