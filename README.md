# MoPub Android SDK

Thanks for taking a look at MoPub! We take pride in having an easy-to-use, flexible monetization solution that works across multiple platforms.

Sign up for an account at [http://app.mopub.com/](http://app.mopub.com/).

## Need Help?

You can find integration documentation on our [wiki](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started) and additional help documentation on our [developer help site](http://dev.twitter.com/mopub).

To file an issue with our team visit the [MoPub Forum](https://twittercommunity.com/c/fabric/mopub) or email [support@mopub.com](mailto:support@mopub.com).

**Please Note: We no longer accept GitHub Issues.**

## New Pull Requests?

Thank you for submitting pull requests to the MoPub Android GitHub repository. Our team regularly monitors and investigates all submissions for inclusion in our official SDK releases. Please note that MoPub does not directly merge these pull requests at this time. Please reach out to your account team or [support@mopub.com](mailto:support@mopub.com) if you have further questions.

## Disclosures

MoPub SDK 4.16 and above integrates technology from our partners Integral Ad Science, Inc. (“IAS”) and Moat, Inc. (“Moat”) in order to support viewability measurement and other proprietary reporting that [IAS](https://integralads.com/capabilities/viewability/) and [Moat](https://moat.com/analytics) provide to their advertiser and publisher clients. You have the option to remove or disable this technology by following the [opt-out instructions](#disableViewability) below.  

If you do not remove or disable IAS's and/or Moat’s technology in accordance with these instructions, you agree that IAS's [privacy policy](https://integralads.com/privacy-policy/) and [license](https://integralads.com/sdk-license-agreement) and Moat’s [privacy policy](https://moat.com/privacy),  [terms](https://moat.com/terms), and [license](https://moat.com/sdklicense.txt), respectively, apply to your integration of these partners' technologies into your application.

## Download

The MoPub SDK is available via:

1. **JCenter AAR**
    
    [ ![Download](https://api.bintray.com/packages/mopub/mopub-android-sdk/mopub-android-sdk/images/download.svg)](https://bintray.com/mopub/mopub-android-sdk/mopub-android-sdk/_latestVersion)  
    The MoPub SDK is available as an AAR via JCenter; to use it, add the following to your `build.gradle`.
    
    ```
    repositories {
        jcenter() // includes the MoPub SDK and AVID library
        maven { url "https://s3.amazonaws.com/moat-sdk-builds" }
        maven { url 'https://maven.google.com' } // necessary for Android API 26
    }

    dependencies {
        implementation('com.mopub:mopub-sdk:5.0.0@aar') {
            transitive = true
        }
    }
    ```

    ***SDK Modularization***

    With the modular SDK, you can choose to include specific formats to decrease overall SDK footprint in your app. To do so, include the line for any combination of components that you want in your `build.gradle` file as follows:

    ```groovy
    repositories {
        // ... other project repositories
        jcenter() // includes the MoPub SDK and AVID library
        maven { url "https://s3.amazonaws.com/moat-sdk-builds" }
        maven { url 'https://maven.google.com' } // necessary for Android API 26
    }

    dependencies {
        // ... other project dependencies

        // For banners
        implementation('com.mopub:mopub-sdk-banner:5.0.0@aar') {
            transitive = true
        }
        
        // For interstitials
        implementation('com.mopub:mopub-sdk-interstitial:5.0.0@aar') {
            transitive = true
        }

        // For rewarded videos. This will automatically also include interstitials
        implementation('com.mopub:mopub-sdk-rewardedvideo:5.0.0@aar') {
            transitive = true
        }

        // For native static (images).
        implementation('com.mopub:mopub-sdk-native-static:5.0.0@aar') {
            transitive = true
        }

        // For native video. This will automatically also include native static
        implementation('com.mopub:mopub-sdk-native-video:5.0.0@aar') {
            transitive = true
        }
    }
    ```

    **To continue integration using the mopub-sdk AAR, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#updating-your-android-manifest).**

2. **Zipped Source**

    The MoPub SDK is also distributed as zipped source code that you can include in your application.  MoPub provides two prepackaged archives of source code:

    **[MoPub Android SDK.zip](http://bit.ly/YUdWhH)**  
    _Includes everything you need to serve MoPub ads.  No third party ad networks are included._
    
    **For additional integration instructions, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#requirements-and-dependencies).**

3. **Cloned GitHub repository**
    
    Alternatively, you can obtain the MoPub SDK source by cloning the git repository:
    
    `git clone git://github.com/mopub/mopub-android-sdk.git`
    
    **For additional integration instructions, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#requirements-and-dependencies).**

## New in this Version
Please view the [changelog](https://github.com/mopub/mopub-android-sdk/blob/master/CHANGELOG.md) for a complete list of additions, fixes, and enhancements in the latest release.

- General Data Protection Regulation (GDPR) update to support a way for publishers to determine GDPR applicability and to obtain and manage consent from users in European Economic Area, the United Kingdom, or Switzerland to serve personalized ads.
- New SDK initialization method to initialize consent management and rewarded video ad networks. Required for receiving personalized ads. In future versions of the SDK, initialization will be required to receive ads.
- Updated network stack to MoPub-Volley-2.0.0.
- Updated ad requests to use POST instead of GET.
- All communication with MoPub servers is now made via HTTPS.

## Requirements

- Android 4.1 (API Version 16) and up (**Updated in 4.12.0**)
- android-support-v4.jar, r26 (**Updated in 4.18.0**)
- android-support-annotations.jar, r26 (**Updated in 4.18.0**)
- android-support-v7-recyclerview.jar, r26 (**Updated in 4.18.0**)
- MoPub Volley Library (mopub-volley-2.0.0.jar - available on JCenter) (**Updated in 5.0.0**)
- **Recommended** Google Play Services 11.4.0

## Upgrading to SDK 5.0

Please see the [Getting Started Guide](https://developers.mopub.com/docs/android/getting-started/) for instructions on upgrading from SDK 4.X to SDK 5.0.

For GDPR-specific upgrading instructions, also see the [GDPR Integration Guide](https://developers.mopub.com/docs/android/gdpr/).

## <a name="upgradeRepositoryViewability"></a>Upgrading from 4.15.0 and Prior
In 4.16.0, dependencies were added to viewability libraries provided by AVID and Moat. Apps upgrading from previous versions must add
`maven { url "https://s3.amazonaws.com/moat-sdk-builds" }`
to their `build.gradle` repositories block for these included dependencies to resolve.

## <a name="disableViewability"></a>Disabling Viewability Measurement
There are a few options for opting out of viewability measurement:  
##### Strip out from JCenter Integration
Normally, to add the MoPub SDK to your app via JCenter, your `build.gradle` would contain:

```	
dependencies {
    implementation('com.mopub:mopub-sdk:5.0.0@aar') {
        transitive = true
    }
}
```
Update to the following to exclude one or both viewability vendors:

```
dependencies {
    implementation('com.mopub:mopub-sdk:4.20.0@aar') {
        transitive = true
        exclude module: 'libAvid-mopub' // To exclude AVID
        exclude module: 'moat-mobile-app-kit' // To exclude Moat
    }
}
```
##### Strip out from GitHub integration
Navigate to the `gradle.properties` file in your home directory (e.g. `~/.gradle/gradle.properties`) and include one or both of these lines to opt out of viewability measurement for AVID and/or Moat.  

```
mopub.avidEnabled=false
mopub.moatEnabled=false
```
##### Disable via API
If you would like to opt out of viewability measurement but do not want to modify the MoPub SDK, a function is provided for your convenience. At any point, call `MoPub.disableViewability(vendor);`. This method can can be called with any of the enum values available in `ExternalViewabilitySessionManager.ViewabilityVendor`: `AVID` will disable AVID but leave Moat enabled, `MOAT` will disable Moat but leave AVID enabled, and `ALL` will disable all viewability measurement.

## Working with Android 6.0 Runtime Permissions
If your app's target SDK is 23 or higher _**and**_ the user's device is running Android 6.0 or higher, you are responsible for supporting [runtime permissions](http://developer.android.com/training/permissions/requesting.html), one of the [changes](http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html) introduced in Android 6.0 (API level 23). In addition to listing any dangerous permissions your app needs in the manifest, your app also has to explicitly request the dangerous permission(s) during runtime by calling method `requestPermissions()` in the [`ActivityCompat`](http://developer.android.com/reference/android/support/v4/app/ActivityCompat.html) class.

### Specifically for the MoPub SDK:
- Dangerous permission [`ACCESS_COARSE_LOCATION`](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_COARSE_LOCATION) is needed to pass network location data to MoPub.
- Dangerous permission [`ACCESS_FINE_LOCATION`](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_FINE_LOCATION) is needed to pass GPS location data to MoPub.
    - Granting `ACCESS_FINE_LOCATION` also allows network location data to be passed to MoPub without the need to also grant `ACCESS_COARSE_LOCATION`.
- Dangerous permission [`WRITE_EXTERNAL_STORAGE`](http://developer.android.com/reference/android/Manifest.permission.html#WRITE_EXTERNAL_STORAGE) is optional and only required for MRAID 2.0 storePicture ads.
- _**Note:** The user can deny granting any dangerous permissions during runtime, so please make sure your app can handle this properly._
- _**Note:** The user can revoke any permissions granted previously by going to your app's Settings screen, so please make sure your app can handle this properly._

### Additional resources:
- [Android 6.0 Changes](http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html)
- [Requesting Permissions at Run Time](http://developer.android.com/training/permissions/requesting.html)
- [Permissions Best Practices](http://developer.android.com/training/permissions/best-practices.html)
- [Normal vs Dangerous Permissions](http://developer.android.com/guide/topics/security/permissions.html#normal-dangerous)
- [Permission Groups](http://developer.android.com/guide/topics/security/permissions.html#perm-groups)

## License

We have launched a new license as of version 3.2.0. To view the full license, visit [http://www.mopub.com/legal/sdk-license-agreement/](http://www.mopub.com/legal/sdk-license-agreement/).
