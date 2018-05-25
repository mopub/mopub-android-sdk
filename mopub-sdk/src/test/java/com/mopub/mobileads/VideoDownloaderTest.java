package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;

import com.mopub.common.CacheService;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.common.util.test.support.ShadowAsyncTasks;
import com.mopub.common.util.test.support.ShadowMoPubHttpUrlConnection;
import com.mopub.mobileads.VideoDownloader.VideoDownloaderListener;
import com.mopub.mobileads.VideoDownloader.VideoDownloaderTask;

import org.fest.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.lang.ref.WeakReference;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class, shadows = {ShadowAsyncTasks.class, ShadowMoPubHttpUrlConnection.class})
public class VideoDownloaderTest {
    @Mock VideoDownloaderListener mockListener;
    private final static String expectedUrl1 = "https://video_url";
    private final static String expectedUrl2 = "https://video_url2";

    @Before
    public void setUp() {
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        CacheService.initialize(context);
    }

    @After
    public void tearDown() {
        VideoDownloader.clearDownloaderTasks();
        CacheService.clearAndNullCaches();
    }

    @Test
    public void cache_shouldExecuteVideoDownloaderTask() {
        VideoDownloader.cache(expectedUrl1, mockListener);

        assertThat(ShadowAsyncTasks.wasCalled()).isTrue();
        assertThat(ShadowAsyncTasks.getLatestAsyncTask()).isInstanceOf(VideoDownloaderTask.class);
        assertThat(ShadowAsyncTasks.getLatestParams()).hasSize(1);
        assertThat(ShadowAsyncTasks.getLatestParams().contains(expectedUrl1)).isTrue();

        // In the success case, the listener will not be modified until after the AsyncTask is
        // actually executed
        verify(mockListener, never()).onComplete(anyBoolean());
    }

    @Test(expected = NullPointerException.class)
    public void cache_withNullListener_shouldThrowNullPointerException() {
        VideoDownloader.cache(expectedUrl1, null);
    }

    @Test
    public void cache_withNullUrl_shouldCallOnCompleteFalse_shouldNotExecuteAsyncTask() {
        VideoDownloader.cache(null, mockListener);

        verify(mockListener).onComplete(false);
        assertThat(ShadowAsyncTasks.wasCalled()).isFalse();
    }

    @Test
    public void cache_shouldAddVideoDownloaderTaskToStaticCollection() {
        assertThat(VideoDownloader.getDownloaderTasks()).isEmpty();

        VideoDownloader.cache(expectedUrl1, mockListener);

        VideoDownloaderTask expectedTask =
                (VideoDownloaderTask) ShadowAsyncTasks.getLatestAsyncTask();

        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(1);
        assertThat(VideoDownloader.getDownloaderTasks().pop().get()).isEqualTo(expectedTask);
    }

    @Test
    public void cache_shouldAddMultipleVideoDownloaderTasksToStaticCollection() {
        VideoDownloader.cache(expectedUrl1, mockListener);
        VideoDownloaderTask expectedTask1 =
                (VideoDownloaderTask) ShadowAsyncTasks.getLatestAsyncTask();

        VideoDownloader.cache(expectedUrl2, mockListener);
        VideoDownloaderTask expectedTask2 =
                (VideoDownloaderTask) ShadowAsyncTasks.getLatestAsyncTask();

        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(2);
        assertThat(VideoDownloader.getDownloaderTasks().pop().get()).isEqualTo(expectedTask1);
        assertThat(VideoDownloader.getDownloaderTasks().pop().get()).isEqualTo(expectedTask2);
    }

    @Test
    public void cancelAllDownloaderTasks_shouldCancelAllTasksAndRemoveFromStaticCollection() {
        final VideoDownloaderTask task1 = mock(VideoDownloaderTask.class);
        final VideoDownloaderTask task2 = mock(VideoDownloaderTask.class);
        VideoDownloader.getDownloaderTasks().add(new WeakReference<VideoDownloaderTask>(task1));
        VideoDownloader.getDownloaderTasks().add(new WeakReference<VideoDownloaderTask>(task2));

        VideoDownloader.cancelAllDownloaderTasks();

        verify(task1).cancel(true);
        verify(task2).cancel(true);
        assertThat(VideoDownloader.getDownloaderTasks()).isEmpty();
    }

    @Test
    public void cancelLastDownloaderTasks_shouldCancelTasksAndRemoveFromStaticCollection() {
        final VideoDownloaderTask task1 = mock(VideoDownloaderTask.class);
        final VideoDownloaderTask task2 = mock(VideoDownloaderTask.class);
        VideoDownloader.getDownloaderTasks().add(new WeakReference<VideoDownloaderTask>(task1));
        VideoDownloader.getDownloaderTasks().add(new WeakReference<VideoDownloaderTask>(task2));

        VideoDownloader.cancelLastDownloadTask();

        verify(task1, never()).cancel(anyBoolean());
        verify(task2).cancel(true);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(1);
        assertThat(VideoDownloader.getDownloaderTasks().pop().get()).isEqualTo(task1);
    }

    @Test
    public void doInBackground_shouldReturnTrue_shouldUpdateCache() throws Exception {
        String expectedResponse = "response";
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, expectedResponse);
        final VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);

        final Boolean result = videoDownloaderTask.doInBackground(expectedUrl1);

        assertThat(result).isTrue();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(expectedResponse.length());
        assertThat(CacheService.getFromDiskCache(expectedUrl1)).isEqualTo(expectedResponse.getBytes());
    }

    @Test
    public void doInBackground_withNullArguments_shouldReturnFalse_shouldNotUpdateCache() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, "response");
        final VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);

        final Boolean result = videoDownloaderTask.doInBackground((String) null);

        assertThat(result).isFalse();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(0);
    }

    @Test
    public void doInBackground_withEmptyArrayArguments_shouldReturnFalse_shouldNotUpdateCache() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, "response");
        final VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);

        final Boolean result = videoDownloaderTask.doInBackground(Arrays.<String>array());

        assertThat(result).isFalse();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(0);
    }

    @Test
    public void doInBackground_withArrayStartingWithNull_shouldReturnFalse_shouldNotUpdateCache() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, "response");
        final VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);

        final String[] parameters = {null};
        final Boolean result = videoDownloaderTask.doInBackground(parameters);

        assertThat(result).isFalse();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(0);
    }

    @Test
    public void doInBackground_withStatusCodeLessThan200_shouldReturnFalse_shouldNotUpdateCache() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(199, "response");
        VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);

        final Boolean result = videoDownloaderTask.doInBackground(expectedUrl1);

        assertThat(result).isFalse();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(0);
    }

    @Test
    public void doInBackground_withStatusCodeGreaterThan299_shouldReturnFalse_shouldNotUpdateCache() throws Exception {
        ShadowMoPubHttpUrlConnection.addPendingResponse(300, "response");
        VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);

        final Boolean result = videoDownloaderTask.doInBackground(expectedUrl1);

        assertThat(result).isFalse();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(0);
    }

    @Test
    public void doInBackground_withResponseGreaterThan25Mb_shouldReturnFalse_shouldNotUpdateCache() throws Exception {
        String longString = createLongString(25 * 1024 * 1024 + 1);
        ShadowMoPubHttpUrlConnection.addPendingResponse(200, longString);
        VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);

        final Boolean result = videoDownloaderTask.doInBackground(expectedUrl1);

        assertThat(result).isFalse();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(0);
    }

    @Test
    public void onPostExecute_withSuccessTrue_shouldCallOnCompleteTrue_shouldRemoveDownloadTaskFromQueue() {
        VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(1);

        videoDownloaderTask.onPostExecute(true);

        verify(mockListener).onComplete(true);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(0);
    }

    @Test
    public void onPostExecute_withSuccessFalse_shouldCallOnCompleteFalse_shouldRemoveDownloadTaskFromQueue() {
        VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(1);

        videoDownloaderTask.onPostExecute(false);

        verify(mockListener).onComplete(false);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(0);
    }

    @Test
    public void onPostExecute_withSuccessNull_shouldCallOnCompleteFalse_shouldRemoveDownloadTaskFromQueue() {
        VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(1);

        videoDownloaderTask.onPostExecute(null);

        verify(mockListener).onComplete(false);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(0);
    }

    @Test
    public void cancelledTask_shouldCallOnCompleteFalse_shouldRemoveDownloadTaskFromQueue() {
        VideoDownloaderTask videoDownloaderTask = new VideoDownloaderTask(mockListener);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(1);

        videoDownloaderTask.cancel(true);

        verify(mockListener).onComplete(false);
        assertThat(VideoDownloader.getDownloaderTasks()).hasSize(0);
    }

    private static String createLongString(int size) {
        return new String(new char[size]).replace("\0", "*");
    }
}
