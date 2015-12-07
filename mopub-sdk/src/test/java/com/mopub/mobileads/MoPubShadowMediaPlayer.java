package com.mopub.mobileads;

import android.media.MediaPlayer;

import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowMediaPlayer;
import org.robolectric.shadows.util.DataSource;

/**
 */
@Implements(MediaPlayer.class)
public class MoPubShadowMediaPlayer extends ShadowMediaPlayer {

    /*
     * Override to avoid the "no setup extension" that Robo 3.0 ShadowMediaPlayer gives you.
     */
    @Override
    public void doSetDataSource(final DataSource dataSource) {
        MediaInfo stubMediaInfo = new MediaInfo(123, 123);
        if (getMediaInfo(dataSource) == null) {
            addMediaInfo(dataSource, stubMediaInfo);
        }
        super.doSetDataSource(dataSource);
    }
}
