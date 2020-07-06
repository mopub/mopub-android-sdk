// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support

import android.content.Context
import androidx.media2.common.SessionPlayer
import androidx.media2.player.AudioFocusHandler
import androidx.media2.player.MediaPlayer
import com.google.common.util.concurrent.ListenableFuture
import com.mopub.mobileads.factories.MediaPlayerFactory
import org.mockito.Matchers
import org.mockito.Mockito.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

class TestMediaPlayerFactory : MediaPlayerFactory() {

    companion object {
        var instance = TestMediaPlayerFactory()
        val mockMediaPlayer: MediaPlayer = mock(MediaPlayer::class.java)

        fun getLatestContext(): Context? { return instance.context }
    }
    private var context: Context? = null

    override fun internalCreate(context: Context): MediaPlayer {
        this.context = context

        `when`(mockMediaPlayer.prepare()).thenReturn(PrepareFuture())

        `when`(mockMediaPlayer.seekTo(Matchers.anyLong(), Matchers.anyInt())).thenReturn(PrepareFuture())

        `when`(mockMediaPlayer.audioFocusHandler).thenReturn(mock(AudioFocusHandler::class.java))

        return mockMediaPlayer
    }

    open class PrepareFuture : ListenableFuture<SessionPlayer.PlayerResult> {
        val listeners = ArrayList<Runnable>()

        override fun addListener(listener: Runnable, executor: Executor) {
            listeners.add(listener)
        }

        override fun isDone(): Boolean {
            listeners.forEach {
                it.run()
            }
            listeners.clear()
            return true
        }

        override fun get(): SessionPlayer.PlayerResult {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun get(timeout: Long, unit: TimeUnit): SessionPlayer.PlayerResult {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun isCancelled(): Boolean {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }
}
