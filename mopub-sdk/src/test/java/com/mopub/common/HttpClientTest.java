package com.mopub.common;

import android.app.Activity;
import android.webkit.WebView;

import com.mopub.common.util.ResponseHeader;
import com.mopub.network.Networking;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.tester.org.apache.http.RequestMatcher;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import static com.mopub.common.HttpClient.initializeHttpGet;
import static com.mopub.common.HttpClient.urlEncode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class HttpClientTest {
    static final String url = "http://www.mopub.com";
    private Activity context;
    private String userAgent;

    @Before
    public void setup() {
        context = Robolectric.buildActivity(Activity.class).create().get();
        userAgent = new WebView(context).getSettings().getUserAgentString();

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, "body"));

        Robolectric.getBackgroundScheduler().pause();
        Robolectric.clearPendingHttpResponses();
    }

    @After
    public void tearDown() throws Exception {
        Robolectric.getBackgroundScheduler().reset();
        Robolectric.clearPendingHttpResponses();
    }

    @Test
    public void initializeHttpGet_shouldReturnHttpGetWithWebViewUserAgent() throws Exception {
        Networking.setUserAgentForTesting(null);
        HttpGet httpGet = initializeHttpGet(url, context);

        assertThat(httpGet.getURI().toURL().toString()).isEqualTo(url);
        assertThat(httpGet.getFirstHeader(ResponseHeader.USER_AGENT.getKey()).getValue()).isEqualTo(
                userAgent);
    }

    @Test
    public void initializeHttpGet_withNullContext_shouldUseCachedUserAgent() throws Exception {
        Networking.setUserAgentForTesting("cached");
        HttpGet httpGet = initializeHttpGet("http://www.mopub.com/");
        assertThat(httpGet.getFirstHeader(ResponseHeader.USER_AGENT.getKey()).getValue()).isEqualTo(
                "cached");
    }

    @Test
    public void initializeHttpGet_shouldProperlyEncodeUrl() throws Exception {
        HttpGet httpGet = initializeHttpGet("http://host:80/doc|search?q=green robots#over 6\"");
        assertThat(httpGet.getURI().toString())
                .isEqualTo("http://host:80/doc%7Csearch?q=green%20robots#over%206%22");
    }

    @Test(expected = IllegalArgumentException.class)
    public void initializeHttpGet_withImproperlyEncodedUrl_shouldThrowIllegalArgumentException() throws Exception {
        initializeHttpGet("http://user:passwrd@host:80/doc%7ZZZC");
    }

    @Test(expected = IllegalArgumentException.class)
    public void initializeHttpGet_withMalformedUrl_shouldThrowIllegalArgumentException() throws Exception {
        initializeHttpGet("bad://host:80/doc|search?q=green robots#over 6\"");
    }

    @Test
    public void urlEncode_shouldProperlyEncodeUrls() throws Exception {
        // Example url borrowed from: http://developer.android.com/reference/java/net/URI.html
        assertThat(urlEncode("http://user:passwrd@host:80/doc|search?q=green robots#over 6\""))
                .isEqualTo("http://user:passwrd@host:80/doc%7Csearch?q=green%20robots#over%206%22");

        assertThat(urlEncode("http://www.example.com/?key=value\"\"&key2=value2?"))
                .isEqualTo("http://www.example.com/?key=value%22%22&key2=value2?");

        assertThat(urlEncode("http://user:passwrd@host:80/doc?q=green#robots"))
                .isEqualTo("http://user:passwrd@host:80/doc?q=green#robots");

        assertThat(urlEncode("http://rtr.innovid.com/r1.5460f51c393410.96367393;cb=[timestamp]"))
                .isEqualTo("http://rtr.innovid.com/r1.5460f51c393410.96367393;cb=%5Btimestamp%5D");
    }

    @Test
    public void urlEncode_withProperlyEncodedUrl_shouldReturnUrlWithSameEncoding() throws Exception {
        assertThat(urlEncode("http://user:passwrd@host:80/doc%7Csearch?q=green%20robots#over%206%22"))
                .isEqualTo("http://user:passwrd@host:80/doc%7Csearch?q=green%20robots#over%206%22");

        assertThat(urlEncode("https://www.mywebsite.com%2Fd+ocs%2Fenglish%2Fsite%2Fmybook.do%3Fkey%3Dvalue%3B%23fragment"))
                .isEqualTo(
                        "https://www.mywebsite.com%2Fd+ocs%2Fenglish%2Fsite%2Fmybook.do%3Fkey%3Dvalue%3B%23fragment");
    }

    @Test(expected = Exception.class)
    public void urlEncode_withImproperlyEncodedUrl_shouldThowException() throws Exception {
        urlEncode("http://user:passwrd@host:80/doc%7ZZZC");
    }


    @Test(expected = Exception.class)
    public void urlEncode_withImproperlyEncodedUrlScheme_shouldThowException() throws Exception {
        // From: http://developer.android.com/reference/java/net/URI.html
        // A URI's host, port and scheme are not eligible for encoding and must not contain illegal characters.
        urlEncode("https%3A%2F%2Fwww.mywebsite.com%2Fdocs%2Fenglish%2Fsite%2Fmybook.do%3Fkey%3Dvalue%3B%23fragment");
    }

    @Test(expected = Exception.class)
    public void urlEncode_withMalformedUrl_shouldThrowException() throws Exception {
        urlEncode("derp://www.mopub.com/");
    }

    @Test(expected = NullPointerException.class)
    public void initializeHttpGet_withNullUrl_shouldThrowNullPointerException() throws Exception {
        initializeHttpGet(null, context);
    }

    @Test
    public void initializeHttpGet_withNullContext_shouldPopulateUserAgentHeaderWithCachedValue() throws Exception {
        Networking.setUserAgentForTesting("cached");
        HttpGet httpGet = initializeHttpGet(url, null);

        assertThat(httpGet.getURI().toURL().toString()).isEqualTo(url);
        assertThat(httpGet.getFirstHeader(ResponseHeader.USER_AGENT.getKey()).getValue()).isEqualTo(
                "cached");
    }
}
