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

1. Copy `KiipInterstitial.java` under the `extras/` folder to `mopub-android-sdk/src/com/mopub/mobileads`

2. To use the Kiip, download the Kiip's Android SDK (docs.kiip.me) and Copy `KiipSDK/libs/KiipSDK.jar` into your `mopub-sdk/libs/` directory, modify `mopub-sdk/build.xml` to add `compile fileTree(dir: 'libs', include: '*.jar')` to dependencies. Add Kiip resources from `KiipSDK/src/main/res/*` to `mopub-sdk/res/`

3. Login to app.mopub.com, From Inventory select the app which you like to add.

4. Click "Add an Ad Unit" and choose "Full Screen" and name it as "Kiip Reward Unit". Now click on "Code Integration"  to get MoPub Ad unit Id.

5. Navigate to "Networks" and click on "Add a Network" to add "Custom Native Network". Provide a title as "Kiip Network", make sure the App is listed in "Set Up Your Inventory" section.

6. Add "KiipInterstitial" in "Custom Event Class" section.

7. Add {"appKey":"<KIIP_APP_KEY>",
        "appSecret":"<KIIP_APP_SECRET>",
        "momentId":"<KIIP_MOMENT_ID>",  
        "testMode":<true/false>} in "Custom Event Class Data" section. That's it you all done.

8. Use the MoPub Ad unit Id and follow MoPub code integration guide.
