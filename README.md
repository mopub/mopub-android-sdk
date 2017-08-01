# MoPub Android SDK

Thanks for taking a look at MoPub! We take pride in having an easy-to-use, flexible monetization solution that works across multiple platforms.

Sign up for an account at [http://app.mopub.com/](http://app.mopub.com/).

## Need Help?

You can find integration documentation on our [wiki](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started) and additional help documentation on our [developer help site](http://dev.twitter.com/mopub).

To file an issue with our team visit the [MoPub Forum](https://twittercommunity.com/c/fabric/mopub) or email [support@mopub.com](mailto:support@mopub.com).

**Please Note: We no longer accept GitHub Issues.**

## Download

The MoPub SDK is available via:

1. **JCenter AAR**
    
    [ ![Download](https://api.bintray.com/packages/mopub/mopub-android-sdk/mopub-android-sdk/images/download.svg)](https://bintray.com/mopub/mopub-android-sdk/mopub-android-sdk/_latestVersion)  
    The MoPub SDK is available as an AAR via JCenter; to use it, add the following to your `build.gradle`.
    
    ```
    repositories {
        jcenter()
    }

    dependencies {
        compile('com.mopub:mopub-sdk:4.15.0@aar') {
            transitive = true
        }
    }
    ```

    ***SDK Modularization***

    With the modular SDK, you can choose to include specific formats to decrease overall SDK footprint in your app. To do so, include the line for any combination of components that you want in your `build.gradle` file as follows:

    ```groovy
    repositories {
        // ... other project repositories
        jcenter()
    }
    // ...

    dependencies {
        // ... other project dependencies

        // For banners
        compile('com.mopub:mopub-sdk-banner:4.15.0@aar') {
            transitive = true
        }
        
        // For interstitials
        compile('com.mopub:mopub-sdk-interstitial:4.15.0@aar') {
            transitive = true
        }

        // For rewarded videos. This will automatically also include interstitials
        compile('com.mopub:mopub-sdk-rewardedvideo:4.15.0@aar') {
            transitive = true
        }

        // For native static (images).
        compile('com.mopub:mopub-sdk-native-static:4.15.0@aar') {
            transitive = true
        }

        // For native video. This will automatically also include native static
        compile('com.mopub:mopub-sdk-native-video:4.15.0@aar') {
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

- The SDK now sends Advertising ID on Amazon devices when appropriate.
- Fixed issue with Charles proxy in sample app for API 24+.
- Bug fixes.

## Requirements

- Android 4.1 (API Version 16) and up (**Updated in 4.12.0**)
- android-support-v4.jar, r23 (**Updated in 4.4.0**)
- android-support-annotations.jar, r23 (**Updated in 4.4.0**)
- android-support-v7-recyclerview.jar, r23 (**Updated in 4.4.0**)
- MoPub Volley Library (mopub-volley-1.1.0.jar - available on JCenter) (**Updated in 3.6.0**)
- **Recommended** Google Play Services 9.4.0

## Upgrading from 3.2.0 and Prior
In 3.3.0 a dependency on android-support-annotations.jar was added. If you are using Maven or Gradle to include the MoPub SDK, this dependency is included in the build scripts. For instructions on adding dependencies for Eclipse projects, see our [Getting Started Guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#adding-the-support-libraries-to-your-project)

## Important Message About Upgrading to MoPub SDK 4.4.0+

Version 4.4.0 of the MoPub SDK fixes a security issue identified by Google. Note that only publishers who received a message from Google are affected. While not all publishers are impacted, we recommend you upgrade to SDK 4.4.0+ ahead of Google's deadline (July 11, 2016) to avoid any issues submitting updates to your apps after the date. More information can be found in [Google's support article](https://support.google.com/faqs/answer/6345928).

## Working with Android 6.0 Runtime Permissions
If your app's target SDK is 23 or higher _**and**_ the user's device is running Android 6.0 or higher, you are responsible for supporting [runtime permissions](http://developer.android.com/training/permissions/requesting.html), one of the [changes](http://developer.android.com/about/versions/marshmallow/android-6.0-changes.html) introduced in Android 6.0 (API level 23). In addition to listing any dangerous permissions your app needs in the manifest, your app also has to explicitly request the dangerous permission(s) during runtime by calling method `requestPermissions()` in the [`ActivityCompat`](http://developer.android.com/reference/android/support/v4/app/ActivityCompat.html) class.

### Specifically for the MoPub SDK:
- Dangerous permission [`ACCESS_COARSE_LOCATION`](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_COARSE_LOCATION) is needed to pass network location data to MoPub.
- Dangerous permission [`ACCESS_FINE_LOCATION`](http://developer.android.com/reference/android/Manifest.permission.html#ACCESS_FINE_LOCATION) is needed to pass GPS location data to MoPub.
    - Granting `ACCESS_FINE_LOCATION` also allows network location data to be passed to MoPub without the need to also grant `ACCESS_COARSE_LOCATION`.
- Dangerous permission [`WRITE_EXTERNAL_STORAGE`](http://developer.android.com/reference/android/Manifest.permission.html#WRITE_EXTERNAL_STORAGE) is needed for MRAID 2.
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
