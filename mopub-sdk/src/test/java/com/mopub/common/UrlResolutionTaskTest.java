package com.mopub.common;

import android.support.annotation.Nullable;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class UrlResolutionTaskTest {
    private final String BASE_URL =  "https://a.example.com/b/c/d?e=f";
    @Mock private HttpURLConnection mockHttpUrlConnection;

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

    @Test
    public void resolveRedirectLocation_withoutLocation_shouldReturnNull() throws Exception {
        setupMockHttpUrlConnection(200, null);

        assertThat(UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection))
                .isNull();
    }

    @Test(expected = URISyntaxException.class)
    public void resolveRedirectLocation_withInvalidUrl_shouldThrowURISyntaxException() throws Exception {
        setupMockHttpUrlConnection(301, "https://a.example.com/b c/d");

        UrlResolutionTask.resolveRedirectLocation(BASE_URL, mockHttpUrlConnection);
    }

    private void setupMockHttpUrlConnection(final int responseCode,
            @Nullable final String absolutePathUrl) throws IOException {
        mockHttpUrlConnection = mock(HttpURLConnection.class);
        when(mockHttpUrlConnection.getResponseCode()).thenReturn(responseCode);
        when(mockHttpUrlConnection.getHeaderField("Location")).thenReturn(absolutePathUrl);
    }

}
