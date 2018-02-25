package com.mopub.common;

import android.support.annotation.Nullable;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.matchers.Null;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class UrlResolutionTaskTest {
    @Mock private HttpURLConnection mockHttpUrlConnection;
    @Mock private UrlResolutionTask.UrlResolutionListener mockListener;

    private UrlResolutionTask subject;
    private final String BASE_URL =  "https://a.example.com/b/c/d?e=f";

    @Before
    public void setUp() throws Exception {
        subject = new UrlResolutionTask(mockListener);
    }

    @After
    public void tearDown() {
        MoPub.resetBrowserAgent();
    }

    @Test
    public void resolveRedirectLocation_withAbsoluteRedirect_shouldReturnAbsolutePath() throws Exception {
        setupMockHttpUrlConnection(302, "https://www.abc.com");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://www.abc.com");
    }

    @Test
    public void resolveRedirectLocation_withRelativeRedirect_shouldReplaceFileWithRelativePath() throws Exception {
        setupMockHttpUrlConnection(302, "foo/bar");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://a.example.com/b/c/foo/bar");
    }

    @Test
    public void resolveRedirectLocation_withRelativeFromRootRedirect_shouldReturnAmendedPathFromRoot() throws Exception {
        setupMockHttpUrlConnection(302, "/foo/bar");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://a.example.com/foo/bar");
    }

    @Test
    public void resolveRedirectLocation_withRelativeFromOneLevelUpRedirect_shouldReturnAmendedPathFromOneLevelUp() throws Exception {
        setupMockHttpUrlConnection(302, "../foo/bar");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://a.example.com/b/foo/bar");
    }

    @Test
    public void resolveRedirectLocation_withRelativeAndQueryParamsRedirect_shouldReturnAmendedPathWithQueryParams() throws Exception {
        setupMockHttpUrlConnection(302, "../foo/bar?x=y");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://a.example.com/b/foo/bar?x=y");
    }

    @Test
    public void resolveRedirectLocation_withRedirectWithoutScheme_shouldCompleteTheScheme() throws Exception {
        setupMockHttpUrlConnection(302, "//foo.example.com/bar");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://foo.example.com/bar");
    }

    @Test
    public void resolveRedirectLocation_withRedirectDifferentScheme_shouldReturnRedirectScheme() throws Exception {
        setupMockHttpUrlConnection(302, "https://a.example.com/b/c/d?e=f");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://a.example.com/b/c/d?e=f");
    }

    @Test
    public void resolveRedirectLocation_withOnlyQueryParamsRedirect_shouldReturnAmendedPathWithQueryParams() throws Exception {
        setupMockHttpUrlConnection(302, "?x=y");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://a.example.com/b/c/?x=y");
    }

    @Test
    public void resolveRedirectLocation_withOnlyFragmentRedirect_shouldReturnAmendedPathWithFragment() throws Exception {
        setupMockHttpUrlConnection(302, "#x");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://a.example.com/b/c/d?e=f#x");
    }

    @Test
    public void resolveRedirectLocation_withDotRedirect_shouldStripFile() throws Exception {
        setupMockHttpUrlConnection(302, ".");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://a.example.com/b/c/");
    }

    @Test
    public void resolveRedirectLocation_withResponseCode301_shouldResolvePath() throws Exception {
        setupMockHttpUrlConnection(301, "https://www.abc.com");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isEqualTo("https://www.abc.com");
    }

    @Test
    public void resolveRedirectLocation_withResponseCode200_shouldReturnNull() throws Exception {
        setupMockHttpUrlConnection(200, "https://www.abc.com");

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isNull();
    }

    @Test(expected = NullPointerException.class)
    public void resolveRedirectLocation_withResponseCode302_withoutLocation_shouldThrowException() throws Exception {
        when(mockHttpUrlConnection.getResponseCode()).thenReturn(302);

        UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection);
    }

    @Test(expected = URISyntaxException.class)
    public void resolveRedirectLocation_withInvalidUrl_shouldThrowURISyntaxException() throws Exception {
        setupMockHttpUrlConnection(301, "https://a.example.com/b c/d");

        UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection);
    }

    @Test
    public void doInBackground_withInAppBrowserAgent_withHttpScheme_shouldTryToResolveRedirectAndReturnNull() throws Exception {
        // Since BASE_URL is not resolvable, attempting to resolve any redirects would result in
        // catching an IOException and returning null. Hence, a null return value implies that
        // redirect resolution was tried instead of just returning the URL.
        assertThat(subject.doInBackground(BASE_URL)).isEqualTo(null);
    }

    @Test
    public void doInBackground_withNativeBrowserAgent_withHttpScheme_shouldReturnUrlWithoutRedirectResolution() throws Exception {
        MoPub.setBrowserAgent(MoPub.BrowserAgent.NATIVE);
        assertThat(subject.doInBackground(BASE_URL)).isEqualTo(BASE_URL);
    }

    @Test
    public void doInBackground_withInAppBrowserAgent_withNonHttpScheme_shouldReturnUrlWithoutRedirectResolution() throws Exception {
        final String nonHttpUrl = "nonhttps://a.example.com/b/c/d?e=f";
        assertThat(subject.doInBackground(nonHttpUrl)).isEqualTo(nonHttpUrl);
    }

    @Test
    public void doInBackground_withNativeBrowserAgent_withNonHttpScheme_shouldReturnUrlWithoutRedirectResolution() throws Exception {
        MoPub.setBrowserAgent(MoPub.BrowserAgent.NATIVE);
        final String nonHttpUrl = "nonhttps://a.example.com/b/c/d?e=f";
        assertThat(subject.doInBackground(nonHttpUrl)).isEqualTo(nonHttpUrl);
    }

    @Test
    public void doInBackground_withInAppBrowserAgent_withMoPubNativeBrowserScheme_shouldReturnUrlWithoutRedirectResolution() throws Exception {
        final String mopubNativeBrowserUrl = "mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.twitter.com";
        assertThat(subject.doInBackground(mopubNativeBrowserUrl)).isEqualTo(mopubNativeBrowserUrl);
    }

    @Test
    public void doInBackground_withNativeBrowserAgent_withMoPubNativeBrowserScheme_shouldReturnUrlWithoutRedirectResolution() throws Exception {
        MoPub.setBrowserAgent(MoPub.BrowserAgent.NATIVE);
        final String mopubNativeBrowserUrl = "mopubnativebrowser://navigate?url=https%3A%2F%2Fwww.twitter.com";
        assertThat(subject.doInBackground(mopubNativeBrowserUrl)).isEqualTo(mopubNativeBrowserUrl);
    }

    private void setupMockHttpUrlConnection(final int responseCode,
            @Nullable final String absolutePathUrl) throws IOException {
        when(mockHttpUrlConnection.getResponseCode()).thenReturn(responseCode);
        when(mockHttpUrlConnection.getHeaderField("Location")).thenReturn(absolutePathUrl);
    }

}
