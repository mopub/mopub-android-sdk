// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import com.mopub.common.util.Json;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

public class JsonUtils {
    // Assert that two shallow jsonStrings representing <String,String> maps are equal
    public static void assertJsonStringMapsEqual(String a, String b) {
        Map<String, String> mapA = Collections.emptyMap();
        Map<String, String> mapB = Collections.emptyMap();

        try {
            mapA = Json.jsonStringToMap(a);
        } catch (Exception e) {
            fail("Unable to turn json into map: " + a);
        }

        try {
            mapB = Json.jsonStringToMap(b);
        } catch (Exception e) {
            fail("Unable to turn json into map: " + b);
        }

        assertThat(mapA.size()).isEqualTo(mapB.size());

        Set<String> keysA = mapA.keySet();
        Set<String> keysB = mapB.keySet();
        assertThat(keysA).isEqualTo(keysB);

        for (final String key : keysA) {
            assertThat(mapA.get(key)).isEqualTo(mapB.get(key));
        }
    }
}
