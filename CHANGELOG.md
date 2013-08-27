## Version 1.15 (Aug 21, 2013)

Includes support for ads in the MRAID 2.0 format. MRAID 2.0 allows advertisers to create ads with rich media functionality, including adding calendar events, storing pictures and playing videos in the native video player. 

To allow advertisers to show store picture ads, you need to make the following change to AndroidManifest.xml:  
* Add the`WRITE_EXTERNAL_STORAGE` permission. Note: **This permission is optional**. However, if it is not added, we will not be able to display store picture ads. All other features will remain functional with or without the new permission. 

To allow users to play videos using the native video player:
* Declare activity `com.mopub.mobileads.MraidVideoPlayerActivity` This activity is required to support playing videos in the native player.

  - Added the following MRAID 2.0 features: `createCalendarEvent` (Android 4.0 and above), `playVideo`, `storePicture`, and `supports`
  - Hardware Acceleration is now enabled by default for `MraidInterstitial`s on Android 4.0 and above  
  - Ensured that Cursors in `FacebookKeywordProvider` are always closed properly; fixes [GitHub issue #8](https://github.com/mopub/mopub-android-sdk/issues/8)  
  - Added tracking parameter to InMobi ad requests; fixes [GitHub issue #15](https://github.com/mopub/mopub-android-sdk/issues/15)  
  - Banner WebViews are now removed from the view hierarchy before they are destroyed; fixes [GitHub issue #23](https://github.com/mopub/mopub-android-sdk/issues/23)  

## Version 1.14 (May 28, 2013)

  - Provided improved support for Android Unity by moving all project resources (including layouts, javascript, images, and values) into source
  - Removed reference to TYPE_DUMMY in AdUrlGenerator because it is not available in earlier versions of Android; fixes [GitHub issue #3](https://github.com/mopub/mopub-android-sdk/issues/3)
  - Fixed NPE in AdUrlGenerator when WiFi is off and Airplane mode is on; fixes [GitHub issue #5](https://github.com/mopub/mopub-android-sdk/issues/5)
  - `MraidInterstitial`s now properly notify `InterstitialAdListener` when they are shown and dismissed
  
### Version 1.14.1 (June 21, 2013)
  - Wait until after loaded interstitials are shown to report an impression
  - Remove phantom impression tracking from interstitials
  - Remove extra whitespace from Millennial banner ads
  - Added `onInterstitialClicked()` notification to `InterstitialAdListener`
  - Provide default implementations for `BannerAdListener` and `InterstitialAdListener`
  
## Version 1.13 (May 9, 2013)
  - Moved all Android code and documentation to its own repository: [mopub-android-sdk](https://github.com/mopub/mopub-android-sdk)
  - Updated Millennial support to Millennial Media SDK version 5.0
      - Support for Millennial Media SDK 5.0.1 is ready and will be released when the new Milllennial SDK becomes available
  - Added `GoogleAdMobBanner`, `GoogleAdMobInterstitial`, `MillennialBanner`, and `MillennialInterstitial` custom event classes
  - Removed obsolete native network adapters
  - Added timeout for third-party networks (10 seconds for banners and 30 seconds for interstitials)
  - Added more data signals (application version, connection type, and location accuracy)
  
### Version 1.13.1 (May 21, 2013)
  - Updated Millennial support to Millennial Media SDK version 5.0.1

### Version 1.13.0.1 (May 15, 2013)

  - Removed extraneous display call in `MillennialInterstitial` custom event
  - Fixed potential NPE in `AdView`'s loadUrl()
  - Deprecated `HTML5AdView` after fixing some compilation issues
  
### Version 1.13.0.2 (May 17, 2013)

  - Relaxed access modifiers for `CustomEventBanner` and `CustomEventInterstitial`

## Version 1.12 (April 26, 2013)
  - Chartboost custom event now automatically parses server data
  - Added support for Millennial Media SDK 5.0
  - Initial support for data signals (connectivity and carrier)

## Version 1.11 (March 13, 2013)
  - Deprecated multiple `MoPubView` event listener interfaces in favor of a unified `MoPubView.BannerAdListener` interface
  - Deprecated `MoPubInterstitial` listener interface in favor of a new `MoPubInterstitial.InterstitialAdListener` interface
  - Added "shown" and "dismissed" listener methods to `MoPubInterstitial.InterstitialAdListener` interface
  - Fixed a NullPointerException in `MoPubInterstitial` for waterfalls containing multiple custom events
  - Fixed a NullPointerException when tracking impressions for native network SDKs
  - Fixed issue causing `MoPubView` to left-align HTML banners
  - Fixed issue causing incorrect return value for `isReady` when using `MoPubInterstitial` and custom events

## Version 1.10 (February 13, 2013)
  - Introduced custom event classes
  - Improved error logging during `AdFetch`
  - Fixed view resource ID conflicts in `MraidDisplayController`
  - Fixed issue in which un-implemented custom events could disrupt the mediation waterfall
  - Added ability to force refresh ad units
  - Added testing accessors to `MoPubView` and `MoPubInterstitial`
  - Updated to correctly reflect MRAID capabilities in ad request
  - Updated to perform `HttpClient` shutdown on background thread

## Version 1.9 (September 27, 2012)
  - Added support for the Facebook ads test program
  - Updated the Millennial adapters to support SDK version 4.6.0

## Version 1.8 (September 6, 2012)
  - Fixed a crash resulting from following market:// links when Google Play is not installed
  - Added in-app browser support for more Google Play redirect styles
  - Added exponential backoff on ad server failure
  - Included new ad unit IDs for sample ads in SimpleAdsDemo
  - Removed extraneous image files

## Version 1.7 (August 2, 2012)
  - Added support for Millennial Media leaderboard ads

## Version 1.6 (June 29, 2012)
  - Improved click experience by directing clicks to an in-app browser
  - Fixed errors loading mraid.js from disk on Android 4.0+
  - Added `ThreadPoolExecutor` for AsyncTasks on Android 4.0+
  - Fixed incorrect failover behavior for Custom Native Network banners

## Version 1.5 (May 10, 2012)
  - Added support for Millennial Media SDK 4.5.5
  - Fixed ANR relating to synchronization in `LoadUrlTask`
  - Fixed IllegalArgumentExceptions when creating HttpGet objects with malformed URLs 

## Version 1.4 (March 28, 2012)
  - Fixed some NullPointerExceptions in the AdMob and Millennial native adapters
  - Fixed issues in which third-party adapters might not properly fail over
  - Fixed a crash caused by unregistering non-existent broadcast receivers

## Version 1.3 (March 14, 2012)
  - Fixed handling of potential SecurityExceptions from network connectivity checks
  - Exposed keyword APIs for interstitials
  - Fixed click-tracking for custom:// and other non-http intents

## Version 1.2 (February 29, 2012)
  - Added support for custom events
  - Added network connectivity check before loading an ad
  - Added `OnAdPresentedOverlay` listener methods
  - Removed unnecessary permissions from the library's manifest
