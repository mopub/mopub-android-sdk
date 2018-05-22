# Google AdSense SDK

Although the AdSense SDK for Android is deprecated, you can support it by doing the following:

1) Copy AdSenseAdapter.java under the extras/ folder to mopub-android-sdk/src/com/mopub/mobileads
2) Place the Google AdSense SDK in mopub-android-sdk/libs
3) Add GoogleAdView.jar to the Java Build Path in the Properties section of mopub-android-sdk.

*NOTE: At this time, Google does not allow distribution of GoogleAdView.jar so you will need to get that file from Google*

# Integrating Kiip Android SDK

Follow the integration documentation provided by MoPub on Integrating MoPub Android SDK.

1. Copy `KiipInterstitial.java` under the `extras/` folder to `mopub-sdk/mopub-sdk-interstitial/src/main/java/com/mopub/mobileads`

2. Open up the `mopub-sdk/build.gradle` and add `implementation 'me.kiip.sdk:kiip:3.0.0'` inside `dependenies{}` code block. If you want to download the Kiip .aar file, please visit http://docs.kiip.me/en/downloads/ for more information.

3. Login to app.mopub.com, and navigate to "Apps" tab to register your app.

4. When you get to "New ad unit" page, name it as "Kiip Reward Unit" and choose your format as "Fullscreen". After that it will lead you to the Code integration page. Copy the Ad unit ID. 

5. Navigate to "Networks" and click on " New network" to select "Custom SDK network". Provide a title as "Kiip Network", make sure the App is listed in "APP & AD UNIT SETUP" section.

6. Add "KiipInterstitial" with its package location (ex. `com.mopub.mobileads.KiipInterstitial` is where we have added the file) in "Custom Event Class" column.

7. Add {"appKey":"<KIIP_APP_KEY>",
        "appSecret":"<KIIP_APP_SECRET>",
        "momentId":"<KIIP_MOMENT_ID>",  
        "testMode":<true/false>} in "Custom Event Class Data" column. That's it you all done.

8. You can set optional data parameters like "email", "gender", "birthday", and etc. inside "Custom Event Class Data" as well. Please check the KiipInterstitial.java for more detail.

9. Use the MoPub Ad unit ID and follow MoPub code integration guide.
