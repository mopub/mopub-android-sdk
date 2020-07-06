// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mopub.volley.Request;

import org.mockito.ArgumentMatcher;

/**
 * A Mockito Request Matcher, used in tests to allow verifying that Volley Requests match a given
 * url.
 * <p>
 * "verify(mock).add(argThat(VolleyRequestMatcher.isUrl("testUrl")));"
 */
public class VolleyRequestMatcher extends ArgumentMatcher<Request> {

    enum MatchType {
        EXACT,
        STARTS_WITH
    }

    @Nullable private final String mUrl;
    @NonNull private final MatchType mMatchType;

    private VolleyRequestMatcher(@Nullable final String url, @NonNull final MatchType matchType) {
        mUrl = url;
        mMatchType = matchType;
    }

    public static VolleyRequestMatcher isUrl(@Nullable String url) {
        return new VolleyRequestMatcher(url, MatchType.EXACT);
    }

    public static VolleyRequestMatcher isUrlStartingWith(@Nullable String url) {
        return new VolleyRequestMatcher(url, MatchType.STARTS_WITH);
    }

    @Override
    public boolean matches(final Object that) {
        switch (this.mMatchType) {
            case STARTS_WITH:
                return that instanceof Request
                        && ((this.mUrl == null && ((Request) that).getUrl() == null)
                        || ((Request) that).getUrl().startsWith(mUrl));
            case EXACT:
            default: // deliberate fallthrough
                return that instanceof Request
                        && ((this.mUrl == null && ((Request) that).getUrl() == null)
                        || ((Request) that).getUrl().equals(mUrl));
        }
    }
}
