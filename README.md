# MoPub Android SDK

Thanks for taking a look at MoPub! We take pride in having an easy-to-use, flexible monetization solution that works across multiple platforms.

Sign up for an account at [http://app.mopub.com/](http://app.mopub.com/).

## Need Help?

You can find integration documentation on our [wiki](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started) and additional help documentation on our [developer help site](http://dev.twitter.com/mopub).

To file an issue with our team visit the [MoPub Forum](https://twittercommunity.com/c/fabric/mopub) or email [support@mopub.com](mailto:support@mopub.com).

**Please Note: We no longer accept GitHub Issues.**

## Download

The MoPub SDK is available via:

1. **jCenter AAR**
    
    [ ![Download](https://api.bintray.com/packages/mopub/mopub-android-sdk/mopub-android-sdk/images/download.svg)](https://bintray.com/mopub/mopub-android-sdk/mopub-android-sdk/_latestVersion)  
    The MoPub SDK is available as an AAR via jCenter; to use it, add the following to your `build.gradle`.
    
    ```
    repositories {
        jcenter()
    }

    dependencies {
        compile('com.mopub:mopub-sdk:4.1.0@aar') {
            transitive = true
        }
    }
    ```

    **To continue integration using the mopub-sdk AAR, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#updating-your-android-manifest).**

2. **Zipped Source**

    The MoPub SDK is also distributed as zipped source code that you can include in your application.  MoPub provides two prepackaged archives of source code:

    **[MoPub Android Full SDK.zip](http://bit.ly/YUdU9v)**  
    _Includes everything you need to serve MoPub ads *and* built-in support for Millennial Media third party ad network - [Millennial Media](http://www.millennialmedia.com/) - including the required third party binaries._
    
    _**Note:** Millennial Media has updated their minimum supported version of Android to 4.1 (API level 16+). The MoPub Android Full SDK manifest and build.gradle files have been updated accordingly._
    
    _**Note:** Millennial Media has deprecated support for Ant/Eclipse; migrating users should use the MoPub Android base SDK and follow [Millennial's Ant/Eclipse Integration Guide](http://docs.millennialmedia.com/android-ad-sdk/ant-eclipse-integration-guide.html)._
    
    **[MoPub Android Base SDK.zip](http://bit.ly/YUdWhH)**  
    _Includes everything you need to serve MoPub ads.  No third party ad networks are included._
    
    **For additional integration instructions, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#requirements-and-dependencies).**

3. **Cloned GitHub repository**
    
    Alternatively, you can obtain the MoPub SDK source by cloning the git repository:
    
    `git clone git://github.com/mopub/mopub-android-sdk.git`
    
    **For additional integration instructions, please see the [Getting Started guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#requirements-and-dependencies).**

## New in this Version
Please view the [changelog](https://github.com/mopub/mopub-android-sdk/blob/master/CHANGELOG.md) for a complete list of additions, fixes, and enhancements in the latest release.

- Support for mediating Facebook Native Video ads.
- Mediated Facebook Native Ads now display the AdChoices icon.
- Support for Facebook SDK 8.4.1

## Requirements

- Android 2.3.1 (API Version 9) and up
- android-support-v4.jar, r22 (**Updated in 3.7.0**)
- android-support-annotations.jar, r22 (**Updated in 3.7.0**)
- android-support-v7-recyclerview.jar, r22 (**Updated in 3.9.0**)
- MoPub Volley Library (mopub-volley-1.1.0.jar - available on JCenter) (**Updated in 3.6.0**)
- **Recommended** Google Play Services 7.8.0

## Upgrading from 3.2.0 and Prior
In 3.3.0 a dependency on android-support-annotations.jar was added. If you are using Maven or Gradle to include the MoPub SDK, this dependency is included in the build scripts. For instructions on adding dependencies for Eclipse projects, see our [Getting Started Guide](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started#adding-the-support-libraries-to-your-project)

## License

We have launched a new license as of version 3.2.0. To view the full license, visit [http://www.mopub.com/legal/sdk-license-agreement/](http://www.mopub.com/legal/sdk-license-agreement/).
