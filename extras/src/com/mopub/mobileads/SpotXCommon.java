/*

    SpotX - MoPub Advertising Plugin Integration

    To use the the SpotX Advertising Pluging, copy the SpotX* source files into your application’s
    com.mopub.mobileads package, and add a build dependency to your application's build.gradle
    file (e.g. compile 'com.spotxchange:spotx-sdk-android:2.2.+').

 */

package com.mopub.mobileads;

import com.spotxchange.sdk.android.SpotxAdSettings;

import java.util.HashMap;
import java.util.Map;

public class SpotXCommon
{
    public static final String CHANNEL_ID_KEY           = "channel_id";
    public static final String IAB_CATEGORY_KEY         = "iab_category";
    public static final String APP_STORE_URL_KEY        = "appstore_url";
    public static final String PLAY_STORE_URL_KEY       = "playstore_url";
    public static final String APP_DOMAIN_KEY           = "app_domain";
    public static final String PREFETCH_KEY             = "prefetch";
    public static final String AUTO_INIT_KEY            = "auto_init";
    public static final String IN_APP_BROWSER_KEY       = "in_app_browser";
    public static final String SECURE_CONNECTION_KEY    = "use_https";

    public static SpotxAdSettings constructAdSettings(
            Map<String,Object> localSettings,
            Map<String, String> defaultSettings,
            boolean bIsInterstitial){

        Map<String,String> settings = defaultSettings;
        settings.putAll(
                convertStringObjectMapToStringStringMap(localSettings)
        );

        String channel = (settings.containsKey(CHANNEL_ID_KEY)) ?
                settings.get(CHANNEL_ID_KEY) :
                defaultSettings.get(CHANNEL_ID_KEY);

        String appDomain = (settings.containsKey(APP_DOMAIN_KEY)) ?
                settings.get(APP_DOMAIN_KEY) :
                defaultSettings.get(APP_DOMAIN_KEY);

        SpotxAdSettings adSettings = null;
        if(bIsInterstitial) {
            adSettings = new SpotxAdSettings(Integer.valueOf(channel), appDomain, "interstitial");
        }else{
            adSettings = new SpotxAdSettings(Integer.valueOf(channel), appDomain);
        }

        if(settings.containsKey(APP_STORE_URL_KEY) || settings.containsKey(PLAY_STORE_URL_KEY)) {
            String storeUrl = localSettings.containsKey(APP_STORE_URL_KEY) ?
                    settings.get(APP_STORE_URL_KEY) :
                    settings.get(PLAY_STORE_URL_KEY);
            adSettings.setAppStoreUrl(storeUrl);
        }
        if(settings.containsKey(IAB_CATEGORY_KEY)) {
            adSettings.setIabCategory(settings.get(IAB_CATEGORY_KEY));
        }
        if(settings.containsKey(AUTO_INIT_KEY)) {
            boolean autoInit = Boolean.parseBoolean(settings.get(AUTO_INIT_KEY));
            adSettings.setAutoInit(autoInit);
        }
        if(settings.containsKey(PREFETCH_KEY)){
            boolean prefetch = Boolean.parseBoolean(settings.get(PREFETCH_KEY));
            adSettings.setPrefetch(prefetch);
        }
        if(settings.containsKey(IN_APP_BROWSER_KEY)){
            boolean shouldUse = Boolean.parseBoolean(settings.get(IN_APP_BROWSER_KEY));
            adSettings.setShouldUseInternalBrowser(shouldUse);
        }

        // Optional configurations for SpotXView behavior.
        if(settings.containsKey(SECURE_CONNECTION_KEY)){
            boolean shouldUse = Boolean.parseBoolean(settings.get(SECURE_CONNECTION_KEY));
            adSettings.setUseSecureConnection(shouldUse);
        }

        return adSettings;
    }

    private static Map<String,String> convertStringObjectMapToStringStringMap(Map<String, Object> map) {
        Map<String, String> newMap = new HashMap<String,String>();
        for (Map.Entry<String, Object> item : map.entrySet()) {
            if (item.getValue() == null) {
                newMap.put(item.getKey(), null);
            }
            else {
                newMap.put(item.getKey(), item.getValue().toString());
            }
        }
        return newMap;
    }

}
