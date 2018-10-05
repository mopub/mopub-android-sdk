// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.common.test.support;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

public class UrlSupport {
    public static final String SCHEME_KEY = "$scheme_key";
    public static final String USER_INFO_KEY = "$user_info_key";
    public static final String HOST_KEY = "$host_key";
    public static final String PORT_KEY = "$port_key";
    public static final String PATH_KEY = "$path_key";
    public static final String FRAGMENT_KEY = "$fragment_key";

    public static Map<String, String> urlToMap(final String url) {
        Map<String, String> map = new HashMap<>();
        if (TextUtils.isEmpty(url)) {
            return map;
        }

        final Uri uri = Uri.parse(url);
        addPair(map, SCHEME_KEY, uri.getScheme());
        addPair(map, USER_INFO_KEY, uri.getUserInfo());
        addPair(map, HOST_KEY, uri.getHost());
        addPair(map, PORT_KEY, uri.getPort() != -1 ? String.valueOf(uri.getPort()) : "");
        addPair(map, PATH_KEY, uri.getPath());
        addPair(map, FRAGMENT_KEY, uri.getFragment());

        for (final String queryParam : uri.getQueryParameterNames()) {
            map.put(queryParam, TextUtils.join(",", uri.getQueryParameters(queryParam)));
        }
        return map;
    }

    private static void addPair(@NonNull Map<String, String> map, @Nullable String key, @Nullable String value) {
        if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
            return;
        }

        map.put(key, value);
    }
}
