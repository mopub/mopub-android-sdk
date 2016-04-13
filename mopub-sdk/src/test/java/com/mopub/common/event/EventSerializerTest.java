package com.mopub.common.event;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.ClientMetadata;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class EventSerializerTest {

    private EventSerializer subject;

    @Mock private Event mockEvent;
    @Mock private ErrorEvent mockErrorEvent;

    @Before
    public void setUp() {
        subject = new EventSerializer();

        // initialize client meta data with context
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        ClientMetadata.getInstance(context);

        populateBaseEventFields(mockEvent);
        populateBaseEventFields(mockErrorEvent);

        when(mockErrorEvent.getErrorExceptionClassName()).thenReturn("error_exception_class_name");
        when(mockErrorEvent.getErrorMessage()).thenReturn("error_message");
        when(mockErrorEvent.getErrorStackTrace()).thenReturn("error_stack_trace");
        when(mockErrorEvent.getErrorFileName()).thenReturn("error_file_name");
        when(mockErrorEvent.getErrorClassName()).thenReturn("error_class_name");
        when(mockErrorEvent.getErrorMethodName()).thenReturn("error_method_name");
        when(mockErrorEvent.getErrorLineNumber()).thenReturn(123);
    }

    @Test
    public void serializeAsJson_withAllEventFieldsPopulated_shouldCorrectJsonRepresentation() throws Exception {
        ArrayList<BaseEvent> events = new ArrayList<BaseEvent>();
        events.add(mockEvent);
        JSONArray jsonArray = subject.serializeAsJson(events);
        assertThat(jsonArray.length()).isEqualTo(1);

        JSONObject jsonObject = jsonArray.getJSONObject(0);
        validateBaseEventFields(jsonObject);
    }

    @Test
    public void serializeAsJson_withAllErrorEventFieldsPopulated_shouldCorrectJsonRepresentation() throws Exception {
        ArrayList<BaseEvent> events = new ArrayList<BaseEvent>();
        events.add(mockErrorEvent);
        JSONArray jsonArray = subject.serializeAsJson(events);
        assertThat(jsonArray.length()).isEqualTo(1);

        JSONObject jsonObject = jsonArray.getJSONObject(0);
        validateBaseEventFields(jsonObject);

        assertThat(jsonObject.getString("error_exception_class_name")).isEqualTo("error_exception_class_name");
        assertThat(jsonObject.getString("error_message")).isEqualTo("error_message");
        assertThat(jsonObject.getString("error_stack_trace")).isEqualTo("error_stack_trace");
        assertThat(jsonObject.getString("error_file_name")).isEqualTo("error_file_name");
        assertThat(jsonObject.getString("error_class_name")).isEqualTo("error_class_name");
        assertThat(jsonObject.getString("error_method_name")).isEqualTo("error_method_name");
        assertThat(jsonObject.getInt("error_line_number")).isEqualTo(123);
    }

    @Test
    public void serializeAsJson_shouldReturnJsonArrayOfEvents() throws Exception {
        when(mockEvent.getName()).thenReturn(BaseEvent.Name.AD_REQUEST);
        when(mockEvent.getCategory()).thenReturn(BaseEvent.Category.REQUESTS);
        when(mockErrorEvent.getName()).thenReturn(BaseEvent.Name.IMPRESSION_REQUEST);
        when(mockErrorEvent.getCategory()).thenReturn(BaseEvent.Category.REQUESTS);

        ArrayList<BaseEvent> events = new ArrayList<BaseEvent>();
        events.add(mockEvent);
        events.add(mockErrorEvent);

        JSONArray jsonArray = subject.serializeAsJson(events);
        assertThat(jsonArray.length()).isEqualTo(2);

        JSONObject jsonObject1 = jsonArray.getJSONObject(0);
        assertThat(jsonObject1.getString("name")).isEqualTo(BaseEvent.Name.AD_REQUEST.getName());
        assertThat(jsonObject1.getString("name_category")).isEqualTo("requests");

        JSONObject jsonObject2 = jsonArray.getJSONObject(1);
        assertThat(jsonObject2.getString("name")).isEqualTo("impression_request");
        assertThat(jsonObject2.getString("name_category")).isEqualTo("requests");
    }

    private void populateBaseEventFields(BaseEvent mockBaseEvent) {
        when(mockBaseEvent.getScribeCategory()).thenReturn(BaseEvent.ScribeCategory.EXCHANGE_CLIENT_EVENT);
        when(mockBaseEvent.getName()).thenReturn(BaseEvent.Name.AD_REQUEST);
        when(mockBaseEvent.getCategory()).thenReturn(BaseEvent.Category.REQUESTS);
        when(mockBaseEvent.getSdkProduct()).thenReturn(BaseEvent.SdkProduct.NATIVE);
        when(mockBaseEvent.getSdkVersion()).thenReturn("3.5.0");
        when(mockBaseEvent.getAdUnitId()).thenReturn("8cf00598d3664adaaeccd800e46afaca");
        when(mockBaseEvent.getAdCreativeId()).thenReturn("3c2b887e2c2a4cd0ae6a925440a62f0d");
        when(mockBaseEvent.getAdType()).thenReturn("html");
        when(mockBaseEvent.getAdNetworkType()).thenReturn("admob");
        when(mockBaseEvent.getAdWidthPx()).thenReturn(320.0);
        when(mockBaseEvent.getAdHeightPx()).thenReturn(50.0);
        when(mockBaseEvent.getDspCreativeId()).thenReturn("jack!fake234");
        when(mockBaseEvent.getAppPlatform()).thenReturn(BaseEvent.AppPlatform.ANDROID);
        when(mockBaseEvent.getAppName()).thenReturn("MoPub Sample App");
        when(mockBaseEvent.getAppPackageName()).thenReturn("com.mopub.simpleadsdemo");
        when(mockBaseEvent.getAppVersion()).thenReturn("1.0");
        when(mockBaseEvent.getObfuscatedClientAdvertisingId()).thenCallRealMethod();
        when(mockBaseEvent.getClientAdvertisingId()).thenReturn("38400000-8cf0-11bd-b23e-10b96e40000d");
        when(mockBaseEvent.getClientDoNotTrack()).thenReturn(false);
        when(mockBaseEvent.getDeviceManufacturer()).thenReturn("LGE");
        when(mockBaseEvent.getDeviceModel()).thenReturn("Nexus 5");
        when(mockBaseEvent.getDeviceProduct()).thenReturn("hammerhead");
        when(mockBaseEvent.getDeviceOsVersion()).thenReturn("5.0");
        when(mockBaseEvent.getDeviceScreenWidthDip()).thenReturn(1080);
        when(mockBaseEvent.getDeviceScreenHeightDip()).thenReturn(1920);
        when(mockBaseEvent.getGeoLat()).thenReturn(37.7833);
        when(mockBaseEvent.getGeoLon()).thenReturn(-122.4183333);
        when(mockBaseEvent.getGeoAccuracy()).thenReturn(10.0);
        when(mockBaseEvent.getPerformanceDurationMs()).thenReturn(100.0);
        when(mockBaseEvent.getNetworkType()).thenReturn(ClientMetadata.MoPubNetworkType.WIFI);
        when(mockBaseEvent.getNetworkOperatorCode()).thenReturn("310410");
        when(mockBaseEvent.getNetworkOperatorName()).thenReturn("AT&T");
        when(mockBaseEvent.getNetworkIsoCountryCode()).thenReturn("US");
        when(mockBaseEvent.getNetworkSimCode()).thenReturn("network_sim_code");
        when(mockBaseEvent.getNetworkSimOperatorName()).thenReturn("network_operator_name");
        when(mockBaseEvent.getNetworkSimIsoCountryCode()).thenReturn("US");
        when(mockBaseEvent.getRequestId()).thenReturn("b550796074da4559a27c5072dcba2b27");
        when(mockBaseEvent.getRequestStatusCode()).thenReturn(200);
        when(mockBaseEvent.getRequestUri()).thenReturn("https://ads.mopub.com/m/ad?id=8cf00598d3664adaaeccd800e46afaca");
        when(mockBaseEvent.getRequestRetries()).thenReturn(0);
        when(mockBaseEvent.getTimestampUtcMs()).thenReturn(1416447053472L);
    }

    private void validateBaseEventFields(JSONObject jsonObject) throws Exception {
        assertThat(jsonObject.getString("_category_")).isEqualTo("exchange_client_event");
        assertThat(jsonObject.getLong("ts")).isEqualTo(1416447053472L);

        // Name Details
        assertThat(jsonObject.getString("name")).isEqualTo(BaseEvent.Name.AD_REQUEST.getName());
        assertThat(jsonObject.getString("name_category")).isEqualTo(BaseEvent.Category.REQUESTS.getCategory());

        // SDK Details
        assertThat(jsonObject.getInt("sdk_product")).isEqualTo(BaseEvent.SdkProduct.NATIVE.getType());
        assertThat(jsonObject.getString("sdk_version")).isEqualTo("3.5.0");

        // Ad Details
        assertThat(jsonObject.getString("ad_unit_id")).isEqualTo("8cf00598d3664adaaeccd800e46afaca");
        assertThat(jsonObject.getString("ad_creative_id")).isEqualTo("3c2b887e2c2a4cd0ae6a925440a62f0d");
        assertThat(jsonObject.getString("ad_type")).isEqualTo("html");
        assertThat(jsonObject.getString("ad_network_type")).isEqualTo("admob");
        assertThat(jsonObject.getDouble("ad_width_px")).isEqualTo(320.0);
        assertThat(jsonObject.getDouble("ad_height_px")).isEqualTo(50.0);
        assertThat(jsonObject.getString("dsp_creative_id")).isEqualTo("jack!fake234");

        // App Details
        assertThat(jsonObject.getInt("app_platform")).isEqualTo(2);
        assertThat(jsonObject.getString("app_name")).isEqualTo("MoPub Sample App");
        assertThat(jsonObject.getString("app_package_name")).isEqualTo("com.mopub.simpleadsdemo");
        assertThat(jsonObject.getString("app_version")).isEqualTo("1.0");

        // Client Details
        assertThat(jsonObject.getString("client_advertising_id")).isEqualTo("ifa:XXXX");
        assertThat(jsonObject.getBoolean("client_do_not_track")).isEqualTo(false);

        // Device Details
        assertThat(jsonObject.getString("device_manufacturer")).isEqualTo("LGE");
        assertThat(jsonObject.getString("device_model")).isEqualTo("Nexus 5");
        assertThat(jsonObject.getString("device_product")).isEqualTo("hammerhead");
        assertThat(jsonObject.getString("device_os_version")).isEqualTo("5.0");
        assertThat(jsonObject.getInt("device_screen_width_px")).isEqualTo(1080);
        assertThat(jsonObject.getInt("device_screen_height_px")).isEqualTo(1920);

        // Geo Details
        assertThat(jsonObject.getDouble("geo_lat")).isEqualTo(37.7833);
        assertThat(jsonObject.getDouble("geo_lon")).isEqualTo(-122.4183333);
        assertThat(jsonObject.getDouble("geo_accuracy_radius_meters")).isEqualTo(10.0);

        // Performance Details
        assertThat(jsonObject.getDouble("perf_duration_ms")).isEqualTo(100.0);

        // Network Details
        assertThat(jsonObject.getInt("network_type")).isEqualTo(ClientMetadata.MoPubNetworkType.WIFI.getId());
        assertThat(jsonObject.getString("network_operator_code")).isEqualTo("310410");
        assertThat(jsonObject.getString("network_operator_name")).isEqualTo("AT&T");
        assertThat(jsonObject.getString("network_iso_country_code")).isEqualTo("US");
        assertThat(jsonObject.getString("network_sim_code")).isEqualTo("network_sim_code");
        assertThat(jsonObject.getString("network_sim_operator_name")).isEqualTo("network_operator_name");
        assertThat(jsonObject.getString("network_sim_iso_country_code")).isEqualTo("US");

        // Request Details
        assertThat(jsonObject.getString("req_id")).isEqualTo("b550796074da4559a27c5072dcba2b27");
        assertThat(jsonObject.getInt("req_status_code")).isEqualTo(200);
        assertThat(jsonObject.getString("req_uri")).isEqualTo("https://ads.mopub.com/m/ad?id=8cf00598d3664adaaeccd800e46afaca");
        assertThat(jsonObject.getInt("req_retries")).isEqualTo(0);

        // Timestamp Details
        assertThat(jsonObject.getLong("timestamp_client")).isEqualTo(1416447053472L);
    }
}

