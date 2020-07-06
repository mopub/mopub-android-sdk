// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.factories

import android.content.Context
import androidx.media2.player.MediaPlayer

open class MediaPlayerFactory {

    companion object {
        var instance = MediaPlayerFactory()

        fun create(context: Context): MediaPlayer { return instance.internalCreate(context) }
    }

    open fun internalCreate(context: Context): MediaPlayer { return MediaPlayer(context) }
}
