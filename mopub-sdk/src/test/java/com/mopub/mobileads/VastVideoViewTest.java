package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

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
}
