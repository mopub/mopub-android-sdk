package com.mopub.common.event;

import android.support.annotation.NonNull;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Preconditions;
import com.mopub.common.logging.MoPubLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Serializes events to the data format expected by the Scribe service.
 */
public class EventSerializer {

    /**
     * Serializes a list of events as a JSON array of flattened JSON objects.
     *
     * @param events The events to be serialized.
     *
     * @return Will return a {@code JSONArray} of serialized {@code JSONObject}s.
     */
    @NonNull
    public JSONArray serializeAsJson(@NonNull List<BaseEvent> events) {
        Preconditions.checkNotNull(events);

        JSONArray jsonArray = new JSONArray();
        for (BaseEvent event : events) {
            try {
                jsonArray.put(serializeAsJson(event));
            } catch (JSONException e) {
                MoPubLog.d("Failed to serialize event \"" + event.getName() + "\" to JSON: ", e);
            }
        }
        return jsonArray;
    }

    /**
     * Serializes a single event as a flattened JSON object. Key values are expected by the Scribe
     * service.
     *
     * @param event The event to be serialized.
     *
     * @return Will return a single serialized {@code JSONObject}.
     */
    @NonNull
    public JSONObject serializeAsJson(@NonNull BaseEvent event) throws JSONException {
        Preconditions.checkNotNull(event);

        // Note: adding null values to the JSONObject will remove the key value pair
        JSONObject jsonObject = new JSONObject();

        // Required Scribe Request Keys
        jsonObject.put("_category_", event.getScribeCategory().getCategory());
        jsonObject.put("ts", event.getTimestampUtcMs());

        // Name Details
        jsonObject.put("name",  event.getName().getName());
        jsonObject.put("name_category", event.getCategory().getCategory());

        // SDK Details
        BaseEvent.SdkProduct sdkProduct = event.getSdkProduct();
        jsonObject.put("sdk_product", sdkProduct == null ? null : sdkProduct.getType());
        jsonObject.put("sdk_version", event.getSdkVersion());

        // Ad Details
        jsonObject.put("ad_unit_id", event.getAdUnitId());
        jsonObject.put("ad_creative_id", event.getAdCreativeId());
        jsonObject.put("ad_type", event.getAdType());
        jsonObject.put("ad_network_type", event.getAdNetworkType());
        jsonObject.put("ad_width_px", event.getAdWidthPx());
        jsonObject.put("ad_height_px", event.getAdHeightPx());

        // App Details
        BaseEvent.AppPlatform appPlatform = event.getAppPlatform();
        jsonObject.put("app_platform", appPlatform == null ? null : appPlatform.getType());
        jsonObject.put("app_name", event.getAppName());
        jsonObject.put("app_package_name", event.getAppPackageName());
        jsonObject.put("app_version", event.getAppVersion());

        // Client Details
        // Server side requires these values to be populated to satisfy thrift union
        jsonObject.put("client_advertising_id", event.getObfuscatedClientAdvertisingId());
        jsonObject.put("client_do_not_track", event.getClientDoNotTrack());

        // Device Details
        jsonObject.put("device_manufacturer", event.getDeviceManufacturer());
        jsonObject.put("device_model", event.getDeviceModel());
        jsonObject.put("device_product", event.getDeviceProduct());
        jsonObject.put("device_os_version", event.getDeviceOsVersion());

        // These fields will actually be the dip value until deprecated and new fields
        // added for future releases
        jsonObject.put("device_screen_width_px", event.getDeviceScreenWidthDip());
        jsonObject.put("device_screen_height_px", event.getDeviceScreenHeightDip());

        // Geo Details
        jsonObject.put("geo_lat", event.getGeoLat());
        jsonObject.put("geo_lon", event.getGeoLon());
        jsonObject.put("geo_accuracy_radius_meters", event.getGeoAccuracy());

        // Performance Details
        jsonObject.put("perf_duration_ms", event.getPerformanceDurationMs());

        // Network Details
        ClientMetadata.MoPubNetworkType moPubNetworkType = event.getNetworkType();
        jsonObject.put("network_type", moPubNetworkType == null ? null : moPubNetworkType.getId());
        jsonObject.put("network_operator_code", event.getNetworkOperatorCode());
        jsonObject.put("network_operator_name", event.getNetworkOperatorName());
        jsonObject.put("network_iso_country_code", event.getNetworkIsoCountryCode());
        jsonObject.put("network_sim_code", event.getNetworkSimCode());
        jsonObject.put("network_sim_operator_name", event.getNetworkSimOperatorName());
        jsonObject.put("network_sim_iso_country_code", event.getNetworkSimIsoCountryCode());

        // Request Details
        jsonObject.put("req_id", event.getRequestId());
        jsonObject.put("req_status_code", event.getRequestStatusCode());
        jsonObject.put("req_uri", event.getRequestUri());
        jsonObject.put("req_retries", event.getRequestRetries());

        // Timestamp Details
        jsonObject.put("timestamp_client", event.getTimestampUtcMs());

        if (event instanceof ErrorEvent) {
            ErrorEvent errorEvent = (ErrorEvent) event;
            // Error Details
            jsonObject.put("error_exception_class_name", errorEvent.getErrorExceptionClassName());
            jsonObject.put("error_message", errorEvent.getErrorMessage());
            jsonObject.put("error_stack_trace", errorEvent.getErrorStackTrace());
            jsonObject.put("error_file_name", errorEvent.getErrorFileName());
            jsonObject.put("error_class_name", errorEvent.getErrorClassName());
            jsonObject.put("error_method_name", errorEvent.getErrorMethodName());
            jsonObject.put("error_line_number", errorEvent.getErrorLineNumber());
        }

        return jsonObject;
    }
}
