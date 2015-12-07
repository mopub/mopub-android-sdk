package com.mopub.common;

import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class HttpResponsesTest {

    private DownloadResponse downloadResponse1;

    @Before
    public void setUp() throws Exception {
        downloadResponse1 = mock(DownloadResponse.class);
    }

    @Test
    public void asBitmap_shouldReturnBitmap() throws Exception {
        String imageData = "fake_bitmap_data";
        when(downloadResponse1.getByteArray()).thenReturn(imageData.getBytes());

        final Bitmap bitmap = HttpResponses.asBitmap(downloadResponse1);

        assertThat(bitmap).isNotNull();
        assertThat(bitmap).isInstanceOf(Bitmap.class);
        assertThat(Shadows.shadowOf(bitmap).getCreatedFromBytes()).isEqualTo(imageData.getBytes());
    }

    @Test
    public void asJsonObject_withEmptyJsonString_shouldReturnEmptyJsonObjectAndCloseStream() throws Exception {
        String jsonData = "{}";
        when(downloadResponse1.getByteArray()).thenReturn(jsonData.getBytes());

        final JSONObject expectedJsonObject = new JSONObject();

        final JSONObject actualJsonObject = HttpResponses.asJsonObject(downloadResponse1);

        assertThat(actualJsonObject).isEqualsToByComparingFields(expectedJsonObject);
    }

    @Test
    public void asJsonObject_withShallowJsonString_shouldReturnPopulatedJsonObjectAndCloseStream() throws Exception {
        String jsonData = "{\"key1\":\"value1\",\"key2\":\"2\",\"key3\":\"null\"}";
        when(downloadResponse1.getByteArray()).thenReturn(jsonData.getBytes());

        JSONObject expectedJsonObject = new JSONObject();
        expectedJsonObject.put("key1", "value1");
        expectedJsonObject.put("key2", 2);
        expectedJsonObject.put("key3", JSONObject.NULL);

        final JSONObject actualJsonObject = HttpResponses.asJsonObject(downloadResponse1);

        assertThat(actualJsonObject).isEqualsToByComparingFields(expectedJsonObject);
    }

    @Test
    public void asJsonObject_withDeepJsonString_shouldReturnPopulatedJsonObjectAndCloseStream() throws Exception {
        String jsonData = "{\"key1\":\"value1\",\"key2\":[\"a\",\"b\"]}";
        when(downloadResponse1.getByteArray()).thenReturn(jsonData.getBytes());

        JSONObject expectedJsonObject = new JSONObject();
        expectedJsonObject.put("key1", "value1");
        final JSONArray jsonArray = new JSONArray();
        jsonArray.put("a");
        jsonArray.put("b");
        expectedJsonObject.put("key2", jsonArray);

        final JSONObject actualJsonObject = HttpResponses.asJsonObject(downloadResponse1);

        assertThat(actualJsonObject).isEqualsToByComparingFields(expectedJsonObject);
    }

    @Test
    public void asJsonObject_withMalformedJsonString_shouldReturnNullAndCloseStream() throws Exception {
        String jsonData = "{whoops, forgot closing brace";
        when(downloadResponse1.getByteArray()).thenReturn(jsonData.getBytes());

        final JSONObject jsonObject = HttpResponses.asJsonObject(downloadResponse1);

        assertThat(jsonObject).isNull();
    }

    @Test
    public void asJsonObject_asResponseStringReturnsNull_shouldReturnNull() throws Exception {
        when(downloadResponse1.getByteArray()).thenReturn(null);
        assertThat(HttpResponses.asJsonObject(downloadResponse1)).isNull();
    }

    @Test
    public void asJsonObject_withNullResponse_shouldReturnNull() throws Exception {
        final JSONObject jsonObject = HttpResponses.asJsonObject(null);

        assertThat(jsonObject).isNull();
    }

    @Test
    public void asResponseString_withMultipleLines_shouldReturnResponseAndCloseStream() throws Exception {
        String responseData = "1\n2\n3\n4";
        when(downloadResponse1.getByteArray()).thenReturn(responseData.getBytes());

        final String responseString = HttpResponses.asResponseString(downloadResponse1);

        assertThat(responseString).isEqualTo(responseData);
    }

    @Test
    public void asResponseString_shouldReturnResponseAndCloseStream() throws Exception {
        String responseData = "response_string";
        when(downloadResponse1.getByteArray()).thenReturn(responseData.getBytes());

        final String responseString = HttpResponses.asResponseString(downloadResponse1);

        assertThat(responseString).isEqualTo(responseData);
    }

    @Test
    public void asResponseString_newStringThrowsException_shouldReturnNull() throws Exception {
        when(downloadResponse1.getByteArray()).thenReturn(null);
        assertThat(HttpResponses.asResponseString(downloadResponse1)).isNull();
    }

    @Test
    public void asResponseString_withNullResponse_shouldReturnNull() throws Exception {
        final String responseString = HttpResponses.asResponseString(null);

        assertThat(responseString).isNull();
    }
}
