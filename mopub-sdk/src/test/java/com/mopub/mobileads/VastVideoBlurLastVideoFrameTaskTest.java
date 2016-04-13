package com.mopub.mobileads;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.widget.ImageView;

import com.mopub.TestSdkHelper;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class VastVideoBlurLastVideoFrameTaskTest {
    @Mock private MediaMetadataRetriever mockMediaMetadataRetriever;
    @Mock private ImageView mockBlurredLastVideoFrameImageView;
    @Mock private Bitmap mockBitmap;

    private VastVideoBlurLastVideoFrameTask subject;
    private String videoPath;
    private int videoDuration;

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    @Before
    public void setUp() throws Exception {
        videoPath = "disk_video_path";
        videoDuration = 10000;

        when(mockMediaMetadataRetriever.getFrameAtTime(anyLong(), anyInt())).thenReturn(mockBitmap);

        subject = new VastVideoBlurLastVideoFrameTask(mockMediaMetadataRetriever,
                mockBlurredLastVideoFrameImageView, videoDuration);
    }

    @Test
    public void doInBackground_beforeGingerbreadMr1_shouldReturnFalse() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.GINGERBREAD);
        assertThat(subject.doInBackground(videoPath)).isFalse();
        verifyNoMoreInteractions(mockMediaMetadataRetriever);
        assertThat(subject.getBlurredLastVideoFrame()).isNull();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    @Test
    public void doInBackground_atLeastGingerbreadMr1_shouldSetVideoPath_shouldUseVideoDurationMinusOffset_shouldReturnTrue() throws Exception {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.GINGERBREAD_MR1);
        assertThat(subject.doInBackground(videoPath)).isTrue();
        verify(mockMediaMetadataRetriever).setDataSource(videoPath);
        verify(mockMediaMetadataRetriever).getFrameAtTime(9800000,
                MediaMetadataRetriever.OPTION_CLOSEST);
        verifyNoMoreInteractions(mockMediaMetadataRetriever);
        assertThat(subject.getBlurredLastVideoFrame()).isEqualTo(mockBitmap);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    @Test
    public void doInBackground_whenSetDataSourceThrowsRuntimeException_shouldCatchExceptionAndReturnFalse() throws Exception {
        doThrow(new RuntimeException()).when(mockMediaMetadataRetriever).setDataSource(anyString());

        assertThat(subject.doInBackground(videoPath)).isFalse();
        assertThat(subject.getBlurredLastVideoFrame()).isNull();
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
    @Test
    public void doInBackground_whenGetLastFrameReturnsNull_shouldReturnFalse() throws Exception {
        when(mockMediaMetadataRetriever.getFrameAtTime(anyLong(), anyInt())).thenReturn(null);

        assertThat(subject.doInBackground(videoPath)).isFalse();
        assertThat(subject.getBlurredLastVideoFrame()).isNull();
    }

    @Test
    public void doInBackground_whenVideoPathIsNull_shouldReturnFalse() throws Exception {
        assertThat(subject.doInBackground((String) null)).isFalse();
        assertThat(subject.getBlurredLastVideoFrame()).isNull();
    }

    @Test
    public void doInBackground_whenVideoPathsArrayIsNull_shouldReturnFalse() throws Exception {
        assertThat(subject.doInBackground((String[]) null)).isFalse();
        assertThat(subject.getBlurredLastVideoFrame()).isNull();
    }

    @Test
    public void doInBackground_whenVideoPathsArrayIsEmpty_shouldReturnFalse() throws Exception {
        assertThat(subject.doInBackground(new String[0])).isFalse();
        assertThat(subject.getBlurredLastVideoFrame()).isNull();
    }

    @Test
    public void doInBackground_whenVideoPathsArrayHasMultipleElements_shouldParseFirstElement() throws Exception {
        assertThat(subject.doInBackground(videoPath, null)).isTrue();
        assertThat(subject.getBlurredLastVideoFrame()).isEqualTo(mockBitmap);
    }

    @Test
    public void doInBackground_whenFirstElementOfVideoPathsArrayIsNull_shouldReturnFalse() throws Exception {
        assertThat(subject.doInBackground(null, videoPath)).isFalse();
        assertThat(subject.getBlurredLastVideoFrame()).isNull();
    }

    @Test
    public void onPostExecute_whenBlurringSucceeded_shouldSetImageBitmap() throws Exception {
        subject.onPostExecute(true);

        verify(mockBlurredLastVideoFrameImageView).setImageBitmap(subject.getBlurredLastVideoFrame());
    }

    @Test
    public void onPostExecute_whenBlurringFailed_shouldNotSetImageBitmap() throws Exception {
        subject.onPostExecute(false);

        verify(mockBlurredLastVideoFrameImageView, never()).setImageBitmap(any(Bitmap.class));
    }

    @Test
    public void onPostExecute_whenResultIsNull_shouldNotSetImageBitmap() throws Exception {
        subject.onPostExecute(null);

        verify(mockBlurredLastVideoFrameImageView, never()).setImageBitmap(any(Bitmap.class));
    }

    @Test
    public void onPostExecute_whenTaskIsAlreadyCancelled_shouldNotSetImageBitmap() throws Exception {
        subject.cancel(true);

        subject.onPostExecute(true);

        verify(mockBlurredLastVideoFrameImageView, never()).setImageBitmap(any(Bitmap.class));
    }
}
