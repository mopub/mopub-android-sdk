// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.ResponseHeader;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
public class HeaderUtilsTest {
    private JSONObject subject;

    @Before
    public void setup() {
        subject = new JSONObject();
    }

    @Test
    public void extractIntegerHeader_shouldReturnIntegerValue() throws JSONException {
        subject.remove(ResponseHeader.HEIGHT.getKey());
        assertThat(HeaderUtils.extractIntegerHeader(null, ResponseHeader.HEIGHT)).isNull();

        subject.put(ResponseHeader.HEIGHT.getKey(), "100");
        assertThat(HeaderUtils.extractIntegerHeader(subject, ResponseHeader.HEIGHT)).isEqualTo(100);

        subject.put(ResponseHeader.HEIGHT.getKey(), "1");
        assertThat(HeaderUtils.extractIntegerHeader(subject, ResponseHeader.HEIGHT)).isEqualTo(1);

        subject.put(ResponseHeader.HEIGHT.getKey(), "0");
        assertThat(HeaderUtils.extractIntegerHeader(subject, ResponseHeader.HEIGHT)).isEqualTo(0);

        subject.put(ResponseHeader.HEIGHT.getKey(), "-1");
        assertThat(HeaderUtils.extractIntegerHeader(subject, ResponseHeader.HEIGHT)).isEqualTo(-1);

        subject.put(ResponseHeader.HEIGHT.getKey(), "");
        assertThat(HeaderUtils.extractIntegerHeader(subject, ResponseHeader.HEIGHT)).isNull();

        subject.put(ResponseHeader.HEIGHT.getKey(), "a");
        assertThat(HeaderUtils.extractIntegerHeader(subject, ResponseHeader.HEIGHT)).isNull();
    }

    @Test
    public void extractBooleanHeader_shouldReturnBooleanValue() throws JSONException {
        subject.remove(ResponseHeader.HEIGHT.getKey());
        assertThat(HeaderUtils.extractBooleanHeader(subject, ResponseHeader.HEIGHT, true)).isFalse();

        subject.put(ResponseHeader.HEIGHT.getKey(), "1");
        assertThat(HeaderUtils.extractBooleanHeader(subject, ResponseHeader.HEIGHT, false)).isTrue();

        subject.put(ResponseHeader.HEIGHT.getKey(), "0");
        assertThat(HeaderUtils.extractBooleanHeader(subject, ResponseHeader.HEIGHT, true)).isFalse();

        subject.put(ResponseHeader.HEIGHT.getKey(), "");
        assertThat(HeaderUtils.extractBooleanHeader(subject, ResponseHeader.HEIGHT, true)).isFalse();

        subject.put(ResponseHeader.HEIGHT.getKey(), "a");
        assertThat(HeaderUtils.extractBooleanHeader(subject, ResponseHeader.HEIGHT, true)).isFalse();
    }

    @Test
    public void extractPercentHeader_shouldReturnPercentValue() throws JSONException {
        subject.remove(ResponseHeader.HEIGHT.getKey());
        assertThat(HeaderUtils.extractPercentHeader(subject, ResponseHeader.HEIGHT)).isNull();

        subject.put(ResponseHeader.HEIGHT.getKey(), "100%");
        assertThat(HeaderUtils.extractPercentHeader(subject, ResponseHeader.HEIGHT)).isEqualTo(100);

        subject.put(ResponseHeader.HEIGHT.getKey(), "10");
        assertThat(HeaderUtils.extractPercentHeader(subject, ResponseHeader.HEIGHT)).isEqualTo(10);

        subject.put(ResponseHeader.HEIGHT.getKey(), "");
        assertThat(HeaderUtils.extractPercentHeader(subject, ResponseHeader.HEIGHT)).isNull();

        subject.put(ResponseHeader.HEIGHT.getKey(), "0%");
        assertThat(HeaderUtils.extractPercentHeader(subject, ResponseHeader.HEIGHT)).isEqualTo(0);

        subject.put(ResponseHeader.HEIGHT.getKey(), "-1%");
        assertThat(HeaderUtils.extractPercentHeader(subject, ResponseHeader.HEIGHT)).isNull();

        subject.put(ResponseHeader.HEIGHT.getKey(), "0");
        assertThat(HeaderUtils.extractPercentHeader(subject, ResponseHeader.HEIGHT)).isEqualTo(0);

        subject.put(ResponseHeader.HEIGHT.getKey(), "a%");
        assertThat(HeaderUtils.extractPercentHeader(subject, ResponseHeader.HEIGHT)).isNull();
    }

    @Test
    public void extractJsonObjectHeader_shouldReturnJsonObject() throws JSONException {
        JSONObject testObject = new JSONObject();

        subject.put(ResponseHeader.IMPRESSION_DATA.getKey(), testObject);
        assertThat(HeaderUtils.extractJsonObjectHeader(subject, ResponseHeader.IMPRESSION_DATA)).isEqualTo(testObject);
        assertThat(HeaderUtils.extractJsonObjectHeader(null, ResponseHeader.IMPRESSION_DATA)).isNull();

        subject.remove(ResponseHeader.IMPRESSION_DATA.getKey());
        assertThat(HeaderUtils.extractJsonObjectHeader(subject, ResponseHeader.IMPRESSION_DATA)).isNull();
    }
}
