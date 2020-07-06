// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support

import android.content.Context
import androidx.media2.widget.VideoView
import com.mopub.mobileads.factories.VideoViewFactory
import org.mockito.Mockito.mock

class TestVideoViewFactory : VideoViewFactory() {

    companion object {
        var instance = TestVideoViewFactory()
        var mockVideoView: VideoView = mock(VideoView::class.java)

        fun getSingletonMock(): TestVideoViewFactory {
            return instance
        }

        fun getLatestContext(): Context? {
            return instance.context
        }
    }

    private var context: Context? = null

    override fun internalCreate(context: Context): VideoView {
        this.context = context
        return mockVideoView
    }
}
