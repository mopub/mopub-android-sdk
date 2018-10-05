// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common;

import android.support.annotation.Nullable;

import com.mopub.volley.Request;

import org.mockito.ArgumentMatcher;

/**
 * A Mockito Request Matcher, used in tests to allow verifying that Volley Requests match a given
 * url.
 *
 * "verify(mock).add(argThat(VolleyRequestMatcher.isUrl("testUrl")));"
 */
public class VolleyRequestMatcher extends ArgumentMatcher<Request> {

     @Nullable private final String mUrl;

     private VolleyRequestMatcher(@Nullable final String url) {
         mUrl = url;
     }

     public static VolleyRequestMatcher isUrl(@Nullable String url) {
         return new VolleyRequestMatcher(url);
     }

     @Override
     public boolean matches(final Object that) {
         return that instanceof Request
                 && ((this.mUrl == null && ((Request) that).getUrl() == null)
                    || ((Request) that).getUrl().equals(mUrl));
     }
 }
