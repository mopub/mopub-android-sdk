package com.mopub.mobileads;

import android.net.Uri;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastMacroHelperTest {

    private static final String ERROR_CODE = "errorcode";
    private static final String CONTENT_PLAY_HEAD = "contentplayhead";
    private static final String CACHE_BUSTING = "cachebusting";
    private static final String ASSET_URI = "asseturi";

    private VastMacroHelper subject;
    private String defaultUri;

    @Before
    @SuppressWarnings("unchecked")
    public void setup() {
        defaultUri = "https://www.derp.com/herp?errorcode=[ERRORCODE]&contentplayhead=[CONTENTPLAYHEAD]&asseturi=[ASSETURI]&cachebusting=[CACHEBUSTING]";
        // Suppressing unchecked cast to List<String> with Collections#singletonList(Object)
        subject = new VastMacroHelper(Collections.singletonList(defaultUri));
    }

    @Test
    public void constructor_shouldSetCacheBusting() throws Exception {
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=&contentplayhead=&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void constructor_shouldCreateRandomCacheBustingValues() throws Exception {
        VastMacroHelper vastMacroHelper = new VastMacroHelper(Collections.singletonList(defaultUri));

        String uriStr = subject.getUris().get(0);
        String uriStr2 = vastMacroHelper.getUris().get(0);

        Uri uri = Uri.parse(uriStr);
        Uri uri2 = Uri.parse(uriStr2);

        String cacheBusting = uri.getQueryParameter(CACHE_BUSTING);
        String cacheBusting2 = uri2.getQueryParameter(CACHE_BUSTING);

        assertThat(cacheBusting).isNotEqualTo(cacheBusting2);
    }

    @Test
    public void withErrorCode_shouldSetErrorCode() throws Exception {
        subject.withErrorCode(VastErrorCode.XML_PARSING_ERROR);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=100&contentplayhead=&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withErrorCode(VastErrorCode.WRAPPER_TIMEOUT);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=301&contentplayhead=&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withErrorCode(VastErrorCode.NO_ADS_VAST_RESPONSE);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=303&contentplayhead=&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withErrorCode(VastErrorCode.GENERAL_LINEAR_AD_ERROR);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=400&contentplayhead=&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withErrorCode(VastErrorCode.GENERAL_COMPANION_AD_ERROR);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=600&contentplayhead=&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withErrorCode(VastErrorCode.UNDEFINED_ERROR);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=900&contentplayhead=&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));
    }

    @Test
    public void withContentPlayHead_shouldFormatTime_shouldSetContentPlayHead() throws Exception {
        subject.withContentPlayHead(3600000);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=&contentplayhead=01:00:00.000&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withContentPlayHead(360000000);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=&contentplayhead=100:00:00.000&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withContentPlayHead(3599999);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=&contentplayhead=00:59:59.999&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withContentPlayHead(59999);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=&contentplayhead=00:00:59.999&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withContentPlayHead(999);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=&contentplayhead=00:00:00.999&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));

        subject.withContentPlayHead(45296789);
        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=&contentplayhead=12:34:56.789&asseturi=&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));
    }

    @Test
    public void withAssetUri_shouldSetAssetUri() throws Exception {
        final String expectedAssetUri = "https://thisIsAnAsset.Uri";
        subject.withAssetUri(expectedAssetUri);

        assertThat(subject.getUris()).containsOnly(
                "https://www.derp.com/herp?errorcode=&contentplayhead=&asseturi=https%3A%2F%2FthisIsAnAsset.Uri&cachebusting=" +
                        getAndCheckCachebusting(subject.getUris().get(0)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withManyMacros_shouldReplaceAllOfThem() throws Exception {
        final String multiUrl = "https://www.someurl.com/dosomething?[ERRORCODE][ERRORCODE][CONTENTPLAYHEAD][ERRORCODE][ASSETURI][CONTENTPLAYHEAD][ERRORCODE]";
        subject = new VastMacroHelper(Collections.singletonList(multiUrl))
                .withAssetUri("asset")
                .withContentPlayHead(100000)
                .withErrorCode(VastErrorCode.UNDEFINED_ERROR);

        assertThat(subject.getUris()).containsOnly(
                "https://www.someurl.com/dosomething?90090000:01:40.000900asset00:01:40.000900");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withSpecialCharactersInAsseturi_shouldUrlEncode() {
        final String specialUrl = "https://www.someurl.com/somedirectory/somemethod?errorcode=[ERRORCODE]&asseturi=[ASSETURI]";
        subject = new VastMacroHelper(Collections.singletonList(specialUrl))
                .withErrorCode(VastErrorCode.UNDEFINED_ERROR)
                .withAssetUri(
                        "https://aaddss.mmooppuubb.ccoomm:123/method?args=one~`!@#$%^&*()_+-[]{}|:,.<>/");

        assertThat(subject.getUris()).containsOnly(
                "https://www.someurl.com/somedirectory/somemethod?errorcode=900&asseturi=" +
                        "https%3A%2F%2Faaddss.mmooppuubb.ccoomm%3A123%2Fmethod%3Fargs%3Done" +
                        "%7E%60%21%40%23%24%25%5E%26*%28%29_%2B-%5B%5D%7B%7D%7C%3A%2C.%3C%3E%2F");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withManyUrls_shouldReturnListOfUrls() {
        final String uriWithNoCacheBusting = defaultUri.replace("&cachebusting=[CACHEBUSTING]", "");
        final String uriWithTwoAssetUri = uriWithNoCacheBusting + "&asset2=[ASSETURI]";
        final String uriWithTwoContentPlayHead = uriWithNoCacheBusting + "&cph2=[CONTENTPLAYHEAD]";
        subject = new VastMacroHelper(Arrays.asList(
                new String[]{uriWithNoCacheBusting, uriWithTwoAssetUri, uriWithTwoContentPlayHead}));
        subject.withAssetUri("https://a.ss.et");
        subject.withErrorCode(VastErrorCode.UNDEFINED_ERROR);
        subject.withContentPlayHead(500);

        assertThat(subject.getUris().size()).isEqualTo(3);
        assertThat(subject.getUris().get(0)).isEqualTo(
                "https://www.derp.com/herp?errorcode=900&contentplayhead=00:00:00.500&asseturi=https%3A%2F%2Fa.ss.et");
        assertThat(subject.getUris().get(1)).isEqualTo(
                "https://www.derp.com/herp?errorcode=900&contentplayhead=00:00:00.500&asseturi=https%3A%2F%2Fa.ss.et&asset2=https%3A%2F%2Fa.ss.et");
        assertThat(subject.getUris().get(2)).isEqualTo(
                "https://www.derp.com/herp?errorcode=900&contentplayhead=00:00:00.500&asseturi=https%3A%2F%2Fa.ss.et&cph2=00:00:00.500");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withMalformedUrl_shouldNotAttemptToUrlEncode() {
        final String malformedUri = "htttttt:////oops [CONTENTPLAYHEAD]this [ERRORCODE]is not [ASSETURI]a url";
        subject = new VastMacroHelper(Collections.singletonList(malformedUri));
        subject.withAssetUri("asset").withErrorCode(
                VastErrorCode.UNDEFINED_ERROR).withContentPlayHead(1);

        assertThat(subject.getUris()).containsOnly(
                "htttttt:////oops 00:00:00.001this 900is not asseta url");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withDeeplink_shouldNotAttemptToReformat() {
        final String deeplink = "thisisadeeplink://reallyreallydeep";
        subject = new VastMacroHelper(Collections.singletonList(deeplink));

        assertThat(subject.getUris()).containsOnly(deeplink);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void withNormalUri_shouldReturnUnchangedUri() {
        final String normalUri = "https://www.thisisanormal.uri/with?some=query";
        subject = new VastMacroHelper(Collections.singletonList(normalUri));

        assertThat(subject.getUris()).containsOnly(normalUri);
    }

    private String getAndCheckCachebusting(final String uriStr) {
        Uri uri = Uri.parse(uriStr);
        String cacheBusting = uri.getQueryParameter(CACHE_BUSTING);
        assertThat(cacheBusting).isNotEmpty();
        assertThat(cacheBusting.length()).isEqualTo(8);

        // Will throw if not an int
        Integer.parseInt(cacheBusting);

        return cacheBusting;
    }
}
