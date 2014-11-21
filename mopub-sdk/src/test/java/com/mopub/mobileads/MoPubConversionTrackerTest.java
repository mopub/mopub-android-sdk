package com.mopub.mobileads;

import android.app.Activity;
import android.provider.Settings;

import com.mopub.common.GpsHelper;
import com.mopub.common.GpsHelperTest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.Reflection.MethodBuilder;
import com.mopub.common.util.Utils;
import com.mopub.common.util.test.support.TestMethodBuilderFactory;

import org.apache.http.HttpRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.FakeHttpLayer;
import org.robolectric.tester.org.apache.http.HttpRequestInfo;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.application;

@RunWith(SdkTestRunner.class)
public class MoPubConversionTrackerTest {
    private MoPubConversionTracker subject;
    private Activity context;
    private FakeHttpLayer fakeHttpLayer;
    private MethodBuilder methodBuilder;
    private String expectedUdid;
    private boolean dnt = false;
    private static final String TEST_UDID = "20b013c721c";

    @Before
    public void setUp() throws Exception {
        subject = new MoPubConversionTracker();
        context = new Activity();
        fakeHttpLayer = Robolectric.getFakeHttpLayer();
        methodBuilder = TestMethodBuilderFactory.getSingletonMock();
        Settings.Secure.putString(application.getContentResolver(), Settings.Secure.ANDROID_ID, TEST_UDID);
        expectedUdid = "sha%3A" + Utils.sha1(TEST_UDID);
    }

    @After
    public void tearDown() throws Exception {
        reset(methodBuilder);
    }

    @Test
    public void reportAppOpen_onValidHttpResponse_isIdempotent() throws Exception {
        fakeHttpLayer.addPendingHttpResponse(200, "doesn't matter what this is as long as it's not nothing");
        subject.reportAppOpen(context);
        assertRequestMade(true);

        fakeHttpLayer.addPendingHttpResponse(200, "doesn't matter what this is as long as it's not nothing");
        subject.reportAppOpen(context);
        assertRequestMade(false);
    }

    @Test
    public void reportAppOpen_onInvalidStatusCode_shouldMakeSecondRequest() throws Exception {
        fakeHttpLayer.addPendingHttpResponse(404, "doesn't matter what this is as long as it's not nothing");
        subject.reportAppOpen(context);
        assertRequestMade(true);

        fakeHttpLayer.addPendingHttpResponse(404, "doesn't matter what this is as long as it's not nothing");
        subject.reportAppOpen(context);
        assertRequestMade(true);
    }

    @Test
    public void reportAppOpen_onEmptyResponse_shouldMakeSecondRequest() throws Exception {
        fakeHttpLayer.addPendingHttpResponse(200, "");
        subject.reportAppOpen(context);
        assertRequestMade(true);

        fakeHttpLayer.addPendingHttpResponse(200, "");
        subject.reportAppOpen(context);
        assertRequestMade(true);
    }

    @Test
    public void reportAppOpen_whenGooglePlayServicesIsLinkedAndAdInfoIsNotCached_shouldUseAdInfoParams() throws Exception {
        GpsHelper.setClassNamesForTesting();
        GpsHelperTest.verifyCleanClientMetadata(context);
        GpsHelperTest.TestAdInfo adInfo = new GpsHelperTest.TestAdInfo();

        when(methodBuilder.setStatic(any(Class.class))).thenReturn(methodBuilder);
        when(methodBuilder.addParam(any(Class.class), any())).thenReturn(methodBuilder);
        when(methodBuilder.execute()).thenReturn(
                GpsHelper.GOOGLE_PLAY_SUCCESS_CODE,
                adInfo,
                adInfo.mAdId,
                adInfo.mLimitAdTrackingEnabled,
                GpsHelper.GOOGLE_PLAY_SUCCESS_CODE
        );

        expectedUdid = "ifa%3A" + adInfo.mAdId;
        dnt = true;

        fakeHttpLayer.addPendingHttpResponse(200, "doesn't matter what this is as long as it's not nothing");
        subject.reportAppOpen(context);
        Thread.sleep(500); // extra sleep since there are 2 async tasks
        assertRequestMade(true);
    }

    private void assertRequestMade(boolean shouldRequestBeMade) throws Exception {
        StringBuilder stringBuilder = new StringBuilder("http://ads.mopub.com/m/open")
                .append("?v=6")
                .append("&id=").append("com.mopub.mobileads")
                .append("&udid=").append(expectedUdid);

        if (dnt) {
            stringBuilder.append("&dnt=1");
        }

        String expectedUrl = stringBuilder.append("&av=")
                .append("1.0")
                .toString();

        Thread.sleep(500);
        HttpRequestInfo lastSentHttpRequestInfo = fakeHttpLayer.getLastSentHttpRequestInfo();
        if (lastSentHttpRequestInfo == null) {
            if (shouldRequestBeMade) {
                fail("No request info in the http layer");
            }
            return;
        }
        HttpRequest request = lastSentHttpRequestInfo.getHttpRequest();
        fakeHttpLayer.clearRequestInfos();
        String actualUrl = request.getRequestLine().getUri();
        assertThat(actualUrl).isEqualTo(expectedUrl);
    }
}

