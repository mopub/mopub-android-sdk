package com.mopub.common.util;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class JsonTest {
    private Map<String,String> expectedMap;

    @Before
    public void setUp() throws Exception {
        expectedMap = new HashMap<String, String>();
    }

    @Test
    public void jsonStringToMap_shouldParseJson() throws Exception {
        expectedMap.put("key", "value");
        expectedMap.put("other_key", "other_value");

        String json = "{\"key\":\"value\",\"other_key\":\"other_value\"}";
        Map<String, String> map = Json.jsonStringToMap(json);
        assertThat(map).isEqualTo(expectedMap);
    }

    @Test
    public void jsonStringToMap_whenStringIsNull_shouldReturnEmptyMap() throws Exception {
        Map<String, String> map = Json.jsonStringToMap(null);
        assertThat(map).isEqualTo(expectedMap);
    }

    @Test
    public void jsonStringToMap_whenStringIsEmpty_shouldReturnEmptyMap() throws Exception {
        Map<String, String> map = Json.jsonStringToMap("");
        assertThat(map).isEqualTo(expectedMap);
    }

    @Test
    public void mapToJsonString_followedByJsonStringToMap_shouldReturnSameMap() throws Exception {
        Map<String, String> inputMap = new HashMap<String, String>();
        inputMap.put("key", "value");
        inputMap.put("other_key", "other_value");

        Map<String, String> outputMap = Json.jsonStringToMap(Json.mapToJsonString(inputMap));
        assertThat(outputMap).isEqualTo(inputMap);
    }

    @Test
    public void mapToJsonString_shouldReturnValidMap() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        map.put("key", "value");

        String expectedJson = "{\"key\":\"value\"}";
        String actualJson = Json.mapToJsonString(map);
        assertThat(actualJson).isEqualTo(expectedJson);
    }

    @Test
    public void mapToJsonString_whenMapIsEmpty_shouldReturnEmptyJson() throws Exception {
        String expectedJson = "{}";
        assertThat(Json.mapToJsonString(new HashMap<String, String>())).isEqualTo(expectedJson);
    }

    @Test
    public void mapToJsonString_whenMapIsNull_shouldReturnEmptyJson() throws Exception {
        String expectedJson = "{}";
        assertThat(Json.mapToJsonString(null)).isEqualTo(expectedJson);
    }

    @Test
    public void jsonArrayToStringArray_withMultipleValidParameters_shouldReturnCorrespondingStringArray() throws Exception {
        String jsonString = "[\"hi\",\"dog\",\"goat\"]";

        String[] expected = {"hi", "dog", "goat"};

        assertThat(Json.jsonArrayToStringArray(jsonString)).isEqualTo(expected);
    }

    @Test
    public void jsonArrayToStringArray_withMultipleValidParameters_withSingleQuotes_shouldReturnCorrespondingStringArray() throws Exception {
        String jsonString = "['hi','dog','goat']";

        String[] expected = {"hi", "dog", "goat"};

        assertThat(Json.jsonArrayToStringArray(jsonString)).isEqualTo(expected);
    }

    @Test
    public void jsonArrayToStringArray_withMultipleValidParameters_withNoQuotes_shouldReturnCorrespondingStringArray() throws Exception {
        String jsonString = "[hi,dog,goat]";

        String[] expected = {"hi", "dog", "goat"};

        assertThat(Json.jsonArrayToStringArray(jsonString)).isEqualTo(expected);
    }

    @Test
    public void jsonArrayToStringArray_withNullInput_shouldReturnEmptyStringArray() throws Exception {
        String[] result = Json.jsonArrayToStringArray(null);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void jsonArrayToStringArray_withEmptyJsonArray_shouldReturnEmptyStringArray() throws Exception {
        String[] result = Json.jsonArrayToStringArray("[]");

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void jsonArrayToStringArray_withEmptyString_shouldReturnEmptyStringArray() throws Exception {
        String[] result = Json.jsonArrayToStringArray("");

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void jsonArrayToStringArray_withMalformedMalicousString_shouldReturnEmptyStringArray() throws Exception {
        String[] result = Json.jsonArrayToStringArray("} die");

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void jsonArrayToStringArray_whenMalformed_shouldReturnEmptyStringArray() throws Exception {
        String jsonString = "[cool,guy,crew";

        String[] result = Json.jsonArrayToStringArray(jsonString);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    public void jsonArrayToStringArray_withLotsOfEmptySpace_shouldReturnStringArrayWithoutSpaces() throws Exception {
        String jsonString = "        [    \"  hi\",\"do g\",\"goat  \"]";
        String[] expected = {"  hi", "do g", "goat  "};

        String[] result = Json.jsonArrayToStringArray(jsonString);

        assertThat(result).isEqualTo(expected);
    }
}
