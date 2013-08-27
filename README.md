# MoPub Android SDK

Thanks for taking a look at MoPub! We take pride in having an easy-to-use, flexible monetization solution that works across multiple platforms.

Sign up for an account at [http://app.mopub.com/](http://app.mopub.com/).

Help is available on the [wiki](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started).

## Download

The MoPub SDK is distributed as source code that you can include in your application.  MoPub provides two prepackaged archives of source code:

- **[MoPub Android Full SDK.zip](http://bit.ly/YUdU9v)**

  Includes everything you need to serve HTML and MRAID MoPub advertisiments *and* built-in support for two major third party ad networks - [Google AdMob](http://www.google.com/ads/admob/) and [Millennial Media](http://www.millennialmedia.com/) - including the required third party binaries.

- **[MoPub Android Base SDK.zip](http://bit.ly/YUdWhH)**

  Includes everything you need to serve HTML and MRAID MoPub advertisements.  No third party ad networks are included.

## Integrate

Integration instructions are available on the [wiki](https://github.com/mopub/mopub-android-sdk/wiki/Getting-Started).


## New in this Version

This release of the Android SDK includes support for ads in the MRAID 2.0 format. We will now allow advertisers to serve ads with rich media functionality, including adding calendar events, storing pictures to the user's devices and playing videos in the native player. To learn more about MRAID 2.0 and our update, read our [help article](http://j.mp/16MKSci). 

To correctly display ads that ask the user to save a picture (storePicture ads), you need to make the following change to AndroidManifest.xml:  
* Add the`WRITE_EXTERNAL_STORAGE` permission. Note: **Adding the permission is optional**. If the permission is not added, we will not deliver any store picture ads to the users' devices. All other features will remain functional without the new permission. 

To allow users to play videos using the native video player:
* Declare activity `com.mopub.mobileads.MraidVideoPlayerActivity` This activity is required to support playing videos in the native player and we strongly recommend adding it.

Please view the [changelog](https://github.com/mopub/mopub-android-sdk/blob/master/CHANGELOG.md) for details. 

## Requirements

Android 1.6 and up

## License

The MoPub Android SDK is open sourced under the New BSD license:

Copyright (c) 2010-2013 MoPub Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of MoPub nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
