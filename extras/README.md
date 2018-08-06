# MoPub Android SDK

We are formally separating network adapters from our MoPub SDK. This is to enable an independent release cadence resulting in faster updates and certification cycles. New mediation location is accessible [here](https://github.com/mopub/mopub-android-mediation).  

We have also added an additional tool, making it easy for publishers to get up and running with the mediation integration. Check out https://developers.mopub.com/docs/mediation/integrate/ and integration instructions at https://developers.mopub.com/docs/android/integrating-networks/.

# Google AdSense SDK

Although the AdSense SDK for Android is deprecated, you can support it by doing the following:

1) Copy AdSenseAdapter.java under the extras/ folder to mopub-android-sdk/src/com/mopub/mobileads
2) Place the Google AdSense SDK in mopub-android-sdk/libs
3) Add GoogleAdView.jar to the Java Build Path in the Properties section of mopub-android-sdk.

*NOTE: At this time, Google does not allow distribution of GoogleAdView.jar so you will need to get that file from Google*

# Integrating Kiip Android SDK

Follow the integration documentation provided by MoPub on Integrating MoPub Android SDK.

Kiip Interstitial Instruction

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


Kiip Native(Mrect) Instruction

1. Copy `KiipMrecBanner.java` under the `extras/src/com/mopub/mobileads` folder to `mopub-sdk/mopub-sdk-banner/src/main/java/com/mopub/mobileads`

2. Open up the `mopub-sdk/build.gradle (Module: mopub-sdk-banner)` and add `implementation 'me.kiip.sdk:kiip:3.0.0'` inside `dependenies{}` code block. If you want to download the Kiip .aar file, please visit http://docs.kiip.me/en/downloads/ for more information.

3. Login to app.mopub.com, and navigate to "Apps" tab to register your app.

4. When you get to "New ad unit" page, name it as "Kiip Reward Unit" and choose your format as "Medium (300x250)" and refresh interval to 0 seconds. After that it will lead you to the Code integration page. Copy the Ad unit ID.

5. Navigate to "Networks" and click on " New network" to select "Custom SDK network". Provide a title as "Kiip Network", make sure the App is listed in "APP & AD UNIT SETUP" section.

6. Add "KiipMrecBanner" with its package location (ex. `com.mopub.mobileads.KiipMrecBanner` is where we have added the file) in "Custom Event Class" column.

7. Add {"appKey":"<KIIP_APP_KEY>",
        "appSecret":"<KIIP_APP_SECRET>",
        "momentId":"<KIIP_MOMENT_ID>",  
        "testMode":<true/false>} in "Custom Event Class Data" column. That's it you all done.

8. You can set optional data parameters like "email", "gender", "birthday", and etc. inside "Custom Event Class Data" as well. Please check the KiipMrecBanner.java for more detail.

9. Use the MoPub Ad unit ID and follow MoPub code integration guide.
