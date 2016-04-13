package com.mopub.common;

import android.app.Activity;
import android.content.Context;
import android.webkit.WebView;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.network.Networking;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.List;

import static com.mopub.common.MoPubHttpUrlConnection.urlEncode;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubHttpUrlConnectionTest {
    private static final String url = "https://www.mopub.com";
    private String userAgent;

    @Before
    public void setUp() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        userAgent = new WebView(context).getSettings().getUserAgentString();
        Networking.setUserAgentForTesting(userAgent);
    }

    @Test
    public void getHttpUrlConnection_shouldReturnHttpUrlConnectionWithUserAgent() throws Exception {
        HttpURLConnection urlConnection = MoPubHttpUrlConnection.getHttpUrlConnection(url);

        List<String> userAgentHeaders = urlConnection.getRequestProperties().get("User-Agent");
        assertThat(userAgentHeaders).containsExactly(userAgent);
    }

    @Test
    public void getHttpUrlConnection_shouldSetConnectAndReadTimeoutTo10Seconds() throws Exception {
        HttpURLConnection urlConnection = MoPubHttpUrlConnection.getHttpUrlConnection(url);

        assertThat(urlConnection.getConnectTimeout()).isEqualTo(10000);
        assertThat(urlConnection.getReadTimeout()).isEqualTo(10000);
    }

    @Test
    public void getHttpUrlConnection_shouldProperlyEncodeUrl() throws Exception {
        HttpURLConnection urlConnection = MoPubHttpUrlConnection.getHttpUrlConnection(
                "https://host:80/doc|search?q=green robots#over 6\"");

        assertThat(urlConnection.getURL().toString())
                .isEqualTo("https://host:80/doc%7Csearch?q=green%20robots#over%206%22");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getHttpUrlConnection_withImproperlyEncodedUrl_shouldThrowIllegalArgumentException() throws Exception {
        MoPubHttpUrlConnection.getHttpUrlConnection("https://user:passwrd@host:80/doc%7ZZZC");
    }

    @Test(expected = MalformedURLException.class)
    public void getHttpUrlConnection_withMalformedUrl_shouldThrowMalformedUrlException() throws Exception {
        MoPubHttpUrlConnection.getHttpUrlConnection("bad://host:80/doc|search?q=green robots#over 6\"");
    }

    @Test(expected = NullPointerException.class)
    public void getHttpUrlConnection_withNullUrl_shouldThrowNullPointerException() throws Exception {
        MoPubHttpUrlConnection.getHttpUrlConnection(null);
    }

    @Test
    public void urlEncode_shouldProperlyEncodeUrls() throws Exception {
        // Example url borrowed from: https://developer.android.com/reference/java/net/URI.html
        assertThat(urlEncode("https://user:passwrd@host:80/doc|search?q=green robots#over 6\""))
                .isEqualTo("https://user:passwrd@host:80/doc%7Csearch?q=green%20robots#over%206%22");

        assertThat(urlEncode("https://www.example.com/?key=value\"\"&key2=value2?"))
                .isEqualTo("https://www.example.com/?key=value%22%22&key2=value2?");

        assertThat(urlEncode("https://user:passwrd@host:80/doc?q=green#robots"))
                .isEqualTo("https://user:passwrd@host:80/doc?q=green#robots");

        assertThat(urlEncode("https://rtr.innovid.com/r1.5460f51c393410.96367393;cb=[timestamp]"))
                .isEqualTo("https://rtr.innovid.com/r1.5460f51c393410.96367393;cb=%5Btimestamp%5D");
    }

    @Test
    public void urlEncode_withProperlyEncodedUrl_shouldReturnUrlWithSameEncoding() throws Exception {
        assertThat(urlEncode("https://user:passwrd@host:80/doc%7Csearch?q=green%20robots#over%206%22"))
                .isEqualTo("https://user:passwrd@host:80/doc%7Csearch?q=green%20robots#over%206%22");

        assertThat(urlEncode("https://www.mywebsite.com%2Fd+ocs%2Fenglish%2Fsite%2Fmybook.do%3Fkey%3Dvalue%3B%23fragment"))
                .isEqualTo(
                        "https://www.mywebsite.com%2Fd+ocs%2Fenglish%2Fsite%2Fmybook.do%3Fkey%3Dvalue%3B%23fragment");
    }

    @Test(expected = Exception.class)
    public void urlEncode_withImproperlyEncodedUrl_shouldThowException() throws Exception {
        urlEncode("https://user:passwrd@host:80/doc%7ZZZC");
    }


    @Test(expected = Exception.class)
    public void urlEncode_withImproperlyEncodedUrlScheme_shouldThowException() throws Exception {
        // From: https://developer.android.com/reference/java/net/URI.html
        // A URI's host, port and scheme are not eligible for encoding and must not contain illegal characters.
        urlEncode("https%3A%2F%2Fwww.mywebsite.com%2Fdocs%2Fenglish%2Fsite%2Fmybook.do%3Fkey%3Dvalue%3B%23fragment");
    }

    @Test(expected = Exception.class)
    public void urlEncode_withMalformedUrl_shouldThrowException() throws Exception {
        urlEncode("derp://www.mopub.com/");
    }
}
