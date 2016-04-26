package com.mopub.nativeads;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;
import com.mopub.network.MoPubNetworkError;
import com.mopub.volley.NetworkResponse;
import com.mopub.volley.Response;
import com.mopub.volley.VolleyError;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import java.util.TreeMap;

import static junit.framework.Assert.fail;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class PositioningRequestTest {

    String url = "https://example.com";
    @Mock
    Response.Listener<MoPubNativeAdPositioning.MoPubClientPositioning> mockListener;
    @Mock
    Response.ErrorListener mockErrorListener;

    NetworkResponse mockNetworkResponse;
    PositioningRequest subject;

    @Before
    public void setup() {
        subject = new PositioningRequest(url, mockListener, mockErrorListener);
    }

    @Test
    public void parseNetworkResponse_shouldReturnPositioning() {
        mockNetworkResponse = new NetworkResponse(200, "{fixed: []}".getBytes(), new TreeMap<String, String>(), false);
        assertThat(subject.parseNetworkResponse(mockNetworkResponse).result)
                .isExactlyInstanceOf(MoPubNativeAdPositioning.MoPubClientPositioning.class);
    }
    
    @Test
    public void parseNetworkResponse_shouldReturnError() {
        mockNetworkResponse = new NetworkResponse(200, "garbage".getBytes(), new TreeMap<String, String>(), false);
        assertThat(subject.parseNetworkResponse(mockNetworkResponse).error)
                .isExactlyInstanceOf(VolleyError.class);
    }
    
    @Test
    public void parseJson_noFixedPositions_shouldReturnEmptyPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: []}");
        assertThat(positioning.getFixedPositions()).isEmpty();
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJson_oneFixedPosition_shouldReturnValidPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: [{position: 2}]}");
        assertThat(positioning.getFixedPositions()).containsOnly(2);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJson_twoFixedPositions_shouldReturnValidPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: [{position: 1}, {position: 8}]}");
        assertThat(positioning.getFixedPositions()).containsExactly(1, 8);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJson_twoFixedPositions_shouldIgnoreNonZeroSection() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: [{section: 0, position: 5}, {section: 1, position: 8}]}");
        assertThat(positioning.getFixedPositions()).containsOnly(5);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(MoPubNativeAdPositioning.MoPubClientPositioning.NO_REPEAT);
    }

    @Test
    public void parseJson_invalidFixedPosition_shouldThrowException() throws Exception {
        // Must have either fixed or repeating positions.
        checkException("", "Empty response");
        checkException("{}", "Must contain fixed or repeating positions");
        checkException("{\"error\":\"WARMING_UP\"}", "WARMING_UP");

        // Position is required.
        checkException("{fixed: [{}]}", "JSONObject[\"position\"] not found.");
        checkException("{fixed: [{section: 0}]}", "JSONObject[\"position\"] not found.");

        // Section is optional, but if it exists must be > 0
        checkException("{fixed: [{section: -1, position: 8}]}", "Invalid section -1 in JSON response");

        // Positions must be between [0 and 2 ^ 16).
        checkException("{fixed: [{position: -1}]}", "Invalid position -1 in JSON response");
        checkException("{fixed: [{position: 1}, {position: -8}]}",
                "Invalid position -8 in JSON response");
        checkException("{fixed: [{position: 1}, {position: 66000}]}",
                "Invalid position 66000 in JSON response");
    }

    @Test
    public void parseJson_repeatingInterval_shouldReturnValidPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{repeating: {interval: 2}}");
        assertThat(positioning.getFixedPositions()).isEmpty();
        assertThat(positioning.getRepeatingInterval()).isEqualTo(2);
    }

    @Test
    public void parseJson_invalidRepeating_shouldThrowException() throws Exception {
        checkException("{repeating: }", "Missing value at character 12");
        checkException("{repeating: {}}", "JSONObject[\"interval\"] not found.");

        // Intervals must be between [2 and 2 ^ 16).
        checkException("{repeating: {interval: -1}}", "Invalid interval -1 in JSON response");
        checkException("{repeating: {interval: 0}}", "Invalid interval 0 in JSON response");
        checkException("{repeating: {interval: 1}}", "Invalid interval 1 in JSON response");
        checkException("{repeating: {interval: 66000}}",
                "Invalid interval 66000 in JSON response");
    }

    @Test
    public void parseJson_fixedAndRepeating_shouldReturnValidPositioning() throws Exception {
        MoPubNativeAdPositioning.MoPubClientPositioning positioning = subject.parseJson(
                "{fixed: [{position: 0}, {position: 1}], repeating: {interval: 2}}");
        assertThat(positioning.getFixedPositions()).containsExactly(0, 1);
        assertThat(positioning.getRepeatingInterval()).isEqualTo(2);
    }

    private void checkException(String json, String expectedMessage) throws Exception {
        try {
            subject.parseJson(json);
        } catch (JSONException e) {
            return;
        } catch (MoPubNetworkError e) {
            return;
        }
        fail("Should have received an exception");
    }
}
