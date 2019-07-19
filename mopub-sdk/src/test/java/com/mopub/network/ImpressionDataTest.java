// Copyright 2018-2019 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.network;

import com.mopub.common.test.support.SdkTestRunner;

import org.apache.commons.lang3.SerializationUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(SdkTestRunner.class)
public class ImpressionDataTest {

    private ImpressionData subject;

    @Before
    public void setup() throws JSONException {
        JSONObject json = new JSONObject(SAMPLE_JSON);
        subject = ImpressionData.create(json);
    }

    @Test
    public void create_withNullJson_shouldReturnNull() {
        assertNull(ImpressionData.create(null));
    }

    @Test
    public void create_withValidJson_shouldReturnValidImpressionData() {
        assertTrue(equalsToSampleData(subject));
    }

    @Test
    public void create_withEmptyJson_shouldReturnValidImpressiondata() {
        subject = ImpressionData.create(new JSONObject());

        assertNotNull(subject);
        assertNull(subject.getImpressionId());
        assertNull(subject.getAdUnitId());
        assertNull(subject.getAdUnitName());
        assertNull(subject.getAdUnitFormat());
        assertNull(subject.getAdGroupId());
        assertNull(subject.getAdGroupName());
        assertNull(subject.getAdGroupType());
        assertNull(subject.getAdGroupPriority());
        assertNull(subject.getCurrency());
        assertNull(subject.getCountry());
        assertNull(subject.getNetworkName());
        assertNull(subject.getNetworkPlacementId());
        assertNull(subject.getPublisherRevenue());
        assertNull(subject.getPrecision());
        JSONObject jsonObject = subject.getJsonRepresentation();
        assertFalse(jsonObject.keys().hasNext());
    }

    @Test
    public void serialize_desirialize_shouldReturnObjectCopy() {
        ImpressionData copy = SerializationUtils.clone(subject);
        equalsToSampleData(copy);
    }

    @Test
    public void serialize_deserialize_withUnicodeChars_shouldReturnValidImpressionData() throws JSONException {
        JSONObject jsonObject = new JSONObject(SAMPLE_JSON);
        jsonObject.put("country", "Россия");
        subject = ImpressionData.create(jsonObject);
        ImpressionData copy = SerializationUtils.clone(subject);
        assert copy != null;
        compareObjects(subject, copy);
    }

    private final String SAMPLE_JSON = "{\n" +
            "                \"id\": \"test_ID\",\n" +
            "                \"adunit_id\": \"test_adunit_id\",\n" +
            "                \"adunit_name\": \"test_adunit_name\",\n" +
            "                \"adunit_format\": \"test_adunit_format\",\n" +
            "                \"adgroup_id\": \"test_adgroup_id\",\n" +
            "                \"adgroup_name\": \"test_adgroup_name\",\n" +
            "                \"adgroup_type\": \"test_adgroup_type\",\n" +
            "                \"adgroup_priority\": 9876,\n" +
            "                \"currency\": \"test_currency\",\n" +
            "                \"country\": \"test_country\",\n" +
            "                \"network_name\": \"test_network_name\",\n" +
            "                \"network_placement_id\": \"test_network_placement_id\",\n" +
            "                \"publisher_revenue\": 6789,\n" +
            "                \"precision\": \"test_precision\"\n" +
            "            }";

    private static boolean equalsToSampleData(ImpressionData data) {
        assertEquals("test_ID", data.getImpressionId());
        assertEquals("test_adunit_id", data.getAdUnitId());
        assertEquals("test_adunit_name", data.getAdUnitName());
        assertEquals("test_adunit_format", data.getAdUnitFormat());
        assertEquals("test_adgroup_id", data.getAdGroupId());
        assertEquals("test_adgroup_name", data.getAdGroupName());
        assertEquals("test_adgroup_type", data.getAdGroupType());
        assertEquals(9876, data.getAdGroupPriority().intValue());
        assertEquals("test_currency", data.getCurrency());
        assertEquals("test_country", data.getCountry());
        assertEquals("test_network_name", data.getNetworkName());
        assertEquals("test_network_placement_id", data.getNetworkPlacementId());
        assertEquals(6789d, data.getPublisherRevenue(), 0.001);
        assertEquals("test_precision", data.getPrecision());

        JSONObject json = data.getJsonRepresentation();

        assertEquals("test_ID", json.optString("id"));
        assertEquals("test_adunit_id", json.optString("adunit_id"));
        assertEquals("test_adunit_name", json.optString("adunit_name"));
        assertEquals("test_adunit_format", json.optString("adunit_format"));
        assertEquals("test_adgroup_id", json.optString("adgroup_id"));
        assertEquals("test_adgroup_name", json.optString("adgroup_name"));
        assertEquals("test_adgroup_type", json.optString("adgroup_type"));
        assertEquals(9876, json.optInt("adgroup_priority"));
        assertEquals("test_currency", json.optString("currency"));
        assertEquals("test_country", json.optString("country"));
        assertEquals("test_network_name", json.optString("network_name"));
        assertEquals("test_network_placement_id", json.optString("network_placement_id"));
        assertEquals(6789, json.optDouble("publisher_revenue"), 0.001);
        assertEquals("test_precision", json.optString("precision"));

        return true;
    }

    private static void compareObjects(ImpressionData o1, ImpressionData o2) {
        JSONObject j1 = o1.getJsonRepresentation();
        JSONObject j2 = o2.getJsonRepresentation();
        assertEquals(j1.toString(), j2.toString());
    }
}
