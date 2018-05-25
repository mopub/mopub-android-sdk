package com.mopub.network;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.Constants;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.volley.Request;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class MoPubRequestUtilsTest {

    private Context context;
    private Map<String, String> params;

    @Before
    public void setUp() {
        context = Robolectric.buildActivity(Activity.class).get();
        params = new HashMap<>();
        params.put("query1", "value1");
        params.put("query2", "value2,value3,value4");
        params.put("query3", "");
        params.put("query4", "value5%20value6");
    }

    @Test
    public void chooseMethod_withMoPubUrl_shouldReturnPost() {
        final int result = MoPubRequestUtils.chooseMethod("https://" + Constants.HOST);

        assertThat(result).isEqualTo(Request.Method.POST);
    }

    @Test
    public void chooseMethod_withNonMoPubUrl_shouldReturnGet() {
        final int result = MoPubRequestUtils.chooseMethod("https://www.someurl.com");

        assertThat(result).isEqualTo(Request.Method.GET);
    }

    @Test
    public void isMoPubRequest_withHttpsMoPubUrl_shouldReturnTrue() {
        final boolean result = MoPubRequestUtils.isMoPubRequest("https://ads.mopub.com/m/ad");

        assertThat(result).isTrue();
    }

    @Test
    public void isMoPubRequest_withHttpMoPubUrl_shouldReturnTrue() {
        final boolean result = MoPubRequestUtils.isMoPubRequest("http://ads.mopub.com/m/imp");

        assertThat(result).isTrue();
    }

    @Test
    public void isMoPubRequest_withHttpsNonMoPubUrl_shouldReturnFalse() {
        final boolean result = MoPubRequestUtils.isMoPubRequest("https://www.abcdefg.com/xyz");

        assertThat(result).isFalse();
    }

    @Test
    public void isMoPubRequest_withHttpNonMoPubUrl_shouldReturnFalse() {
        final boolean result = MoPubRequestUtils.isMoPubRequest("http://www.notmopub.com/hi");

        assertThat(result).isFalse();
    }

    @Test
    public void truncateQueryParamsIfPost_withQueryParams_shouldStripQuery() {
        final String result = MoPubRequestUtils.truncateQueryParamsIfPost(
                "https://ads.mopub.com/m/ad?query1=abc&query2=def%20ghi&query3=jkl#fragment");

        assertThat(result).isEqualTo("https://ads.mopub.com/m/ad");
    }

    @Test
    public void truncateQueryParamsIfPost_withNonMoPubUrl_shouldDoNothing() {
        final String result = MoPubRequestUtils.truncateQueryParamsIfPost(
                "https://www.notmopub.com/m/ad?query1=abc&query2=def%20ghi&query3=jkl#fragment");

        assertThat(result).isEqualTo(
                "https://www.notmopub.com/m/ad?query1=abc&query2=def%20ghi&query3=jkl#fragment");
    }

    @Test
    public void truncateQueryParamsIfPost_withIntent_shouldDoNothing() {
        final String result = MoPubRequestUtils.truncateQueryParamsIfPost("geo:37.777328,-122.416544");

        assertThat(result).isEqualTo("geo:37.777328,-122.416544");
    }

    @Test
    public void convertQueryToMap_withAdRequest_shouldReturnQueryMap() {
        final Map<String, String> result = MoPubRequestUtils.convertQueryToMap(context,
                "https://ads.mopub.com/m/ad?query1=abc&query2=def%20ghi&query3=jkl&query1=mno&query4&query4&query4#fragment");

        assertThat(result.size()).isEqualTo(4);
        assertThat(result.get("query1")).isEqualTo("abc,mno");
        assertThat(result.get("query2")).isEqualTo("def ghi");
        assertThat(result.get("query3")).isEqualTo("jkl");
        assertThat(result.get("query4")).isEqualTo(",,");
    }

    @Test
    public void convertQueryToMap_withNoQueryParams_shouldReturnEmptyMap() {
        final Map<String, String> result = MoPubRequestUtils.convertQueryToMap(context,
                "https://ads.mopub.com/m/ad");

        assertThat(result).isEmpty();
    }

    @Test
    public void generateBodyFromParams_withParamsMap_withMoPubUrl_shouldGenerateJsonString() {
        final String result = MoPubRequestUtils.generateBodyFromParams(params,
                "https://ads.mopub.com/m/ad");

        assertThat(result).contains("\"query1\":\"value1\"");
        assertThat(result).contains("\"query2\":\"value2,value3,value4\"");
        assertThat(result).contains("\"query3\":\"\"");
        // Values have already been decoded and should not be decoded again.
        assertThat(result).contains("\"query4\":\"value5%20value6\"");
        assertThat(result.length()).isEqualTo(90);
    }

    @Test
    public void generateBodyFromParams_withParamsMap_withNotMoPubUrl_shouldReturnNull() {
        final String result = MoPubRequestUtils.generateBodyFromParams(params,
                "https://not.mopub.com");

        assertThat(result).isNull();
    }
}
