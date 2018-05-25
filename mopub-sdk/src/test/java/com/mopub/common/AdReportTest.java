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
import static org.mockito.Mockito.stub;

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

        stub(mockClientMetadata.getSdkVersion()).toReturn("1.15.2.2");
        stub(mockAdResponse.getDspCreativeId()).toReturn("");
        stub(mockClientMetadata.getDeviceModel()).toReturn("android");
        stub(mockClientMetadata.getDeviceLocale()).toReturn(Locale.US);
        stub(mockClientMetadata.getMoPubIdentifier()).toReturn(new MoPubIdentifier(context));

        stub(mockAdResponse.getNetworkType()).toReturn("unknown");
        stub(mockAdResponse.getTimestamp()).toReturn(now.getTime());
        stub(mockAdResponse.getAdType()).toReturn("interstitial");
        stub(mockAdResponse.getWidth()).toReturn(480);
        stub(mockAdResponse.getHeight()).toReturn(320);

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

        stub(mockClientMetadata.getSdkVersion()).toReturn(null);
        stub(mockAdResponse.getDspCreativeId()).toReturn(null);
        stub(mockClientMetadata.getDeviceLocale()).toReturn(null);
        stub(mockAdResponse.getNetworkType()).toReturn(null);
        stub(mockClientMetadata.getMoPubIdentifier()).toReturn(new MoPubIdentifier(context));

        stub(mockAdResponse.getTimestamp()).toReturn(-1L);
        stub(mockAdResponse.getAdType()).toReturn(null);
        stub(mockAdResponse.getWidth()).toReturn(null);
        stub(mockAdResponse.getHeight()).toReturn(null);

        subject = new AdReport("testAdUnit", mockClientMetadata, mockAdResponse);
        assertThat(subject.toString()).isEqualTo(expectedParameters);
    }

    private String getCurrentDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d/yy hh:mm:ss a z", Locale.US);
        return dateFormat.format(now);
    }
}
