package com.mopub.common.event;

import com.mopub.common.ClientMetadata;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class BaseEventTest {

    private BaseEvent subject;
    @Mock private ClientMetadata mockClientMetaData;

    @Before
    public void setUp() {
        when(mockClientMetaData.getSdkVersion()).thenReturn("sdk_version");
        when(mockClientMetaData.getAppName()).thenReturn("app_name");
        when(mockClientMetaData.getAppPackageName()).thenReturn("app_package_name");
        when(mockClientMetaData.getAppVersion()).thenReturn("app_version");
        when(mockClientMetaData.getDeviceId()).thenReturn("client_device_id");
        when(mockClientMetaData.isDoNotTrackSet()).thenReturn(true);
        when(mockClientMetaData.getDeviceManufacturer()).thenReturn("device_manufacturer");
        when(mockClientMetaData.getDeviceModel()).thenReturn("device_model");
        when(mockClientMetaData.getDeviceProduct()).thenReturn("device_product");
        when(mockClientMetaData.getDeviceOsVersion()).thenReturn("device_os_version");
        when(mockClientMetaData.getDeviceScreenWidthDip()).thenReturn(1337);
        when(mockClientMetaData.getDeviceScreenHeightDip()).thenReturn(70707);
        when(mockClientMetaData.getActiveNetworkType()).thenReturn(ClientMetadata.MoPubNetworkType.WIFI);
        when(mockClientMetaData.getNetworkOperator()).thenReturn("network_operator");
        when(mockClientMetaData.getNetworkOperatorName()).thenReturn("network_operator_name");
        when(mockClientMetaData.getIsoCountryCode()).thenReturn("network_iso_country_code");
        when(mockClientMetaData.getSimOperator()).thenReturn("network_sim_operator");
        when(mockClientMetaData.getSimOperatorName()).thenReturn("network_sim_operator_name");
        when(mockClientMetaData.getSimIsoCountryCode()).thenReturn("network_sim_iso_country_code");
        ClientMetadata.setInstance(mockClientMetaData);

        subject = new Event.Builder(BaseEvent.Name.AD_REQUEST, BaseEvent.Category.REQUESTS, 0.10000123)
                .withSdkProduct(BaseEvent.SdkProduct.NATIVE)
                .withAdUnitId("8cf00598d3664adaaeccd800e46afaca")
                .withAdCreativeId("3c2b887e2c2a4cd0ae6a925440a62f0d")
                .withAdType("html")
                .withAdNetworkType("admob")
                .withAdWidthPx(320.0)
                .withAdHeightPx(50.0)
                .withGeoLat(37.7833)
                .withGeoLon(-122.4183333)
                .withGeoAccuracy(10.0)
                .withPerformanceDurationMs(100.0)
                .withRequestId("b550796074da4559a27c5072dcba2b27")
                .withRequestStatusCode(200)
                .withRequestUri("https://ads.mopub.com/m/ad?id=8cf00598d3664adaaeccd800e46afaca")
                .withRequestRetries(0)
                .build();
    }

    @After
    public void tearDown() {
        ClientMetadata.setInstance(null);
    }

    @Test
    public void ScribeCategory_shouldHaveExpectedValues() throws Exception {
        // We're testing this since our backend data definitions depend on these values matching
        assertThat(BaseEvent.ScribeCategory.EXCHANGE_CLIENT_EVENT.getCategory())
                .isEqualTo("exchange_client_event");
        assertThat(BaseEvent.ScribeCategory.EXCHANGE_CLIENT_ERROR.getCategory())
                .isEqualTo("exchange_client_error");
    }

    @Test
    public void SdkProduct_shouldHaveExpectedValues() throws Exception {
        // We're testing this since our backend data definitions depend on these values matching
        assertThat(BaseEvent.SdkProduct.NONE.getType())
                .isEqualTo(0);
        assertThat(BaseEvent.SdkProduct.WEB_VIEW.getType())
                .isEqualTo(1);
        assertThat(BaseEvent.SdkProduct.NATIVE.getType())
                .isEqualTo(2);
    }

    @Test
    public void AppPlatform_shouldHaveExpectedValues() throws Exception {
        // We're testing this since our backend data definitions depend on these values matching
        assertThat(BaseEvent.AppPlatform.NONE.getType())
                .isEqualTo(0);
        assertThat(BaseEvent.AppPlatform.IOS.getType())
                .isEqualTo(1);
        assertThat(BaseEvent.AppPlatform.ANDROID.getType())
                .isEqualTo(2);
        assertThat(BaseEvent.AppPlatform.MOBILE_WEB.getType())
                .isEqualTo(3);
    }

    @Test
    public void Name_shouldHaveExpectedValues() throws Exception {
        // We're testing this since our backend data definitions depend on these values matching
        assertThat(BaseEvent.Name.AD_REQUEST.getName())
                .isEqualTo("ad_request");
        assertThat(BaseEvent.Name.IMPRESSION_REQUEST.getName())
                .isEqualTo("impression_request");
        assertThat(BaseEvent.Name.CLICK_REQUEST.getName())
                .isEqualTo("click_request");
    }

    @Test
    public void Category_shouldHaveExpectedValues() throws Exception {
        // We're testing this since our backend data definitions depend on these values matching
        assertThat(BaseEvent.Category.REQUESTS.getCategory())
                .isEqualTo("requests");
    }

    @Test
    public void SamplingRate_shouldHaveExpectedValues() throws Exception {
        // We're testing this since our backend data definitions depend on these values matching
        assertThat(BaseEvent.SamplingRate.AD_REQUEST.getSamplingRate()).isEqualTo(0.1);
    }

    @Test
    public void constructor_shouldCorrectlyAssignFieldsFromBuilder() throws Exception {
        assertThat(subject.getSdkProduct()).isEqualTo(BaseEvent.SdkProduct.NATIVE);
        assertThat(subject.getAdUnitId()).isEqualTo("8cf00598d3664adaaeccd800e46afaca");
        assertThat(subject.getAdCreativeId()).isEqualTo("3c2b887e2c2a4cd0ae6a925440a62f0d");
        assertThat(subject.getAdType()).isEqualTo("html");
        assertThat(subject.getAdNetworkType()).isEqualTo("admob");
        assertThat(subject.getAdWidthPx()).isEqualTo(320.0);
        assertThat(subject.getAdHeightPx()).isEqualTo(50.0);
        assertThat(subject.getGeoLat()).isEqualTo(37.7833);
        assertThat(subject.getGeoLon()).isEqualTo(-122.4183333);
        assertThat(subject.getGeoAccuracy()).isEqualTo(10.0);
        assertThat(subject.getPerformanceDurationMs()).isEqualTo(100.0);
        assertThat(subject.getRequestId()).isEqualTo("b550796074da4559a27c5072dcba2b27");
        assertThat(subject.getRequestStatusCode()).isEqualTo(200);
        assertThat(subject.getRequestUri()).isEqualTo("https://ads.mopub.com/m/ad?id=8cf00598d3664adaaeccd800e46afaca");
        assertThat(subject.getRequestRetries()).isEqualTo(0);
        assertThat(subject.getSamplingRate()).isEqualTo(0.10000123);
    }

    @Test
    public void getSdkVersion_shouldReturnClientMetaDataSdkVersion() throws Exception {
        assertThat(subject.getSdkVersion()).isEqualTo("sdk_version");
    }

    @Test
    public void getAppName_shouldReturnClientMetaDataAppName() throws Exception {
        assertThat(subject.getAppName()).isEqualTo("app_name");
    }

    @Test
    public void getAppPackageName_shouldReturnClientMetaDataAppPackageName() throws Exception {
        assertThat(subject.getAppPackageName()).isEqualTo("app_package_name");
    }

    @Test
    public void getAppVersion_shouldReturnClientMetaDataAppVersion() throws Exception {
        assertThat(subject.getAppVersion()).isEqualTo("app_version");
    }

    @Test
    public void getClientAdvertisingId_shouldReturnClientMetaDataDeviceId() throws Exception {
        assertThat(subject.getClientAdvertisingId()).isEqualTo("client_device_id");
    }

    @Test
    public void getObfuscatedClientAdvertisingId_shouldReturnObfuscatedDeviceId() throws Exception {
        assertThat(subject.getObfuscatedClientAdvertisingId()).isEqualTo("ifa:XXXX");
    }

    @Test
    public void getClientDoNotTrack_shouldReturnClientMetaDataDoNotTrack() throws Exception {
        assertThat(subject.getClientDoNotTrack()).isEqualTo(true);
    }

    @Test
    public void getDeviceManufacturer_shouldReturnClientMetaDataDeviceManufacturer() throws Exception {
        assertThat(subject.getDeviceManufacturer()).isEqualTo("device_manufacturer");
    }

    @Test
    public void getDeviceModel_shouldReturnClientMetaDataDeviceModel() throws Exception {
        assertThat(subject.getDeviceModel()).isEqualTo("device_model");
    }

    @Test
    public void getDeviceProduct_shouldReturnClientMetaDataDeviceProduct() throws Exception {
        assertThat(subject.getDeviceProduct()).isEqualTo("device_product");
    }

    @Test
    public void getDeviceOsVersion_shouldReturnClientMetaDataDeviceOsVersion() throws Exception {
        assertThat(subject.getDeviceOsVersion()).isEqualTo("device_os_version");
    }

    @Test
    public void getDeviceScreenWidthDip_shouldReturnClientMetaDataDeviceScreenWidthDip() throws Exception {
        assertThat(subject.getDeviceScreenWidthDip()).isEqualTo(1337);
    }

    @Test
    public void getDeviceScreenHeightDip_shouldReturnClientMetaDataDeviceScreenHeightDip() throws Exception {
        assertThat(subject.getDeviceScreenHeightDip()).isEqualTo(70707);
    }

    @Test
    public void getNetworkType_shouldReturnClientMetaDataActiveNetworkType() throws Exception {
        assertThat(subject.getNetworkType()).isEqualTo(ClientMetadata.MoPubNetworkType.WIFI);
    }

    @Test
    public void getNetworkOperatorCode_shouldReturnClientMetaDataNetworkOperator() throws Exception {
        assertThat(subject.getNetworkOperatorCode()).isEqualTo("network_operator");
    }

    @Test
    public void getNetworkOperatorName_shouldReturnClientMetaDataNetworkOperatorName() throws Exception {
        assertThat(subject.getNetworkOperatorName()).isEqualTo("network_operator_name");
    }

    @Test
    public void getNetworkIsoCountryCode_shouldReturnClientMetaDataNetworkIsoCountryCode() throws Exception {
        assertThat(subject.getNetworkIsoCountryCode()).isEqualTo("network_iso_country_code");
    }

    @Test
    public void getNetworkSimCode_shouldReturnClientMetaDataNetworkSimOperator() throws Exception {
        assertThat(subject.getNetworkSimCode()).isEqualTo("network_sim_operator");
    }

    @Test
    public void getNetworkSimOperatorName_shouldReturnClientMetaDataNetworkSimOperatorName() throws Exception {
        assertThat(subject.getNetworkSimOperatorName()).isEqualTo("network_sim_operator_name");
    }

    @Test
    public void getNetworkSimIsoCountryCode_shouldReturnClientMetaDataNetworkSimIsoCountryCode() throws Exception {
        assertThat(subject.getNetworkSimIsoCountryCode()).isEqualTo("network_sim_iso_country_code");
    }
}
