package com.mopub.mobileads;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;

import com.mopub.TestSdkHelper;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastVideoViewTest {

    @Mock private MediaMetadataRetriever mockMediaMetadataRetriever;
    @Mock private Bitmap mockBitmap;

    private Context context;
    private VastVideoView subject;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        subject = new VastVideoView(context);
        subject.setMediaMetadataRetriever(mockMediaMetadataRetriever);
        when(mockMediaMetadataRetriever.getFrameAtTime(anyLong(), anyInt())).thenReturn(
                mockBitmap);
    }

    @Test
    public void onDestroy_withBlurLastVideoFrameTaskStillRunning_shouldCancelTask() throws Exception {
        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(
                VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.RUNNING);
        subject.setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.onDestroy();

        verify(mockBlurLastVideoFrameTask).cancel(true);
    }

    @Test
    public void onDestroy_withBlurLastVideoFrameTaskStillPending_shouldCancelTask() throws Exception {
        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(
                VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.PENDING);
        subject.setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.onDestroy();

        verify(mockBlurLastVideoFrameTask).cancel(true);
    }

    @Test
    public void onDestroy_withBlurLastVideoFrameTaskFinished_shouldNotCancelTask() throws Exception {
        VastVideoBlurLastVideoFrameTask mockBlurLastVideoFrameTask = mock(
                VastVideoBlurLastVideoFrameTask.class);
        when(mockBlurLastVideoFrameTask.getStatus()).thenReturn(AsyncTask.Status.FINISHED);
        subject.setBlurLastVideoFrameTask(mockBlurLastVideoFrameTask);

        subject.onDestroy();

        verify(mockBlurLastVideoFrameTask, never()).cancel(anyBoolean());
    }

    @Test
    @Config(shadows = {MoPubShadowMediaPlayer.class})
    public void retryMediaPlayer_withVideoFilePermissionErrorAndBelowJellyBean_shouldReturnTrue() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
        File file = new File("disk_video_path");
        file.createNewFile();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1,
                Integer.MIN_VALUE, "disk_video_path")).isTrue();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        file.delete();
    }

    @Test
    @Config(shadows = {MoPubShadowMediaPlayer.class})
    public void retryMediaPlayer_shouldNotRunMoreThanOnce() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
        File file = new File("disk_video_path");
        file.createNewFile();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1,
                Integer.MIN_VALUE, "disk_video_path")).isTrue();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1,
                Integer.MIN_VALUE, "disk_video_path")).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        file.delete();
    }

    @Config(sdk= Build.VERSION_CODES.JELLY_BEAN)
    @Test
    public void retryMediaPlayer_withAndroidVersionAboveJellyBean_shouldReturnFalse() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.JELLY_BEAN);
        File file = new File("disk_video_path");
        file.createNewFile();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE,
                "disk_video_path")).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(0);

        file.delete();
    }

    @Test
    public void retryMediaPlayer_withOtherVideoError_shouldReturnFalse() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        File file = new File("disk_video_path");
        file.createNewFile();

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 2, Integer.MIN_VALUE,
                "disk_video_path")).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(0);

        file.delete();
    }

    @Test
    public void retryMediaPlayer_withExceptionThrown_shouldReturnFalseAndIncrementRetryCount() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.ICE_CREAM_SANDWICH);
        File file = new File("disk_video_path");
        if (file.exists()) {
            assertThat(file.delete()).isTrue();
        }

        assertThat(subject.getVideoRetries()).isEqualTo(0);
        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE,
                "disk_video_path")).isFalse();
        assertThat(subject.getVideoRetries()).isEqualTo(1);
    }

    @Test
    @Config(shadows = {MoPubShadowMediaPlayer.class})
    public void onResume_shouldResetVideoRetryCountToZero() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);

        File file = new File("disk_video_path");
        file.createNewFile();

        assertThat(subject.retryMediaPlayer(new MediaPlayer(), 1, Integer.MIN_VALUE,
                "disk_video_path")).isTrue();
        assertThat(subject.getVideoRetries()).isEqualTo(1);

        subject.onResume();
        assertThat(subject.getVideoRetries()).isEqualTo(0);

        file.delete();
    }

    @Test
    public void createMediaMetadataRetriever_beforeGingerbreadMr1_shouldReturnNull() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.GINGERBREAD);
        MediaMetadataRetriever mediaMetadataRetriever = subject.createMediaMetadataRetriever();

        assertThat(mediaMetadataRetriever).isNull();
    }

    @Test
    public void createMediaMetadataRetriever_atLeastGingerbreadMr1_shouldReturnNewMediaMetadataRetriever() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.GINGERBREAD_MR1);
        MediaMetadataRetriever mediaMetadataRetriever = subject.createMediaMetadataRetriever();

        assertThat(mediaMetadataRetriever).isNotNull();
        assertThat(mediaMetadataRetriever).isInstanceOf(MediaMetadataRetriever.class);
    }
}
