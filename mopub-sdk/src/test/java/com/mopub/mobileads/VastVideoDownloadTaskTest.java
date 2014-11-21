package com.mopub.mobileads;

import android.app.Activity;

import com.mopub.common.CacheService;
import com.mopub.common.CacheServiceTest;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.test.support.TestHttpResponseWithHeaders;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import java.util.Random;
import java.util.concurrent.Semaphore;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(SdkTestRunner.class)
public class VastVideoDownloadTaskTest {
    private VastVideoDownloadTask.VastVideoDownloadTaskListener mVastVideoDownloadTaskListener;
    private VastVideoDownloadTask subject;
    private String videoUrl;
    private TestHttpResponseWithHeaders response;
    private Semaphore semaphore;

    @Before
    public void setUp() throws Exception {
        Activity context = new Activity();
        CacheService.initializeDiskCache(context);

        videoUrl = "http://www.video.com";
        response = new TestHttpResponseWithHeaders(200, "responseBody");
        Robolectric.addPendingHttpResponse(response);

        semaphore = new Semaphore(0);
        mVastVideoDownloadTaskListener = mock(VastVideoDownloadTask.VastVideoDownloadTaskListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                semaphore.release();
                return null;
            }
        }).when(mVastVideoDownloadTaskListener).onComplete(anyBoolean());

        subject = new VastVideoDownloadTask(mVastVideoDownloadTaskListener);
    }

    @After
    public void tearDown() throws Exception {
        CacheService.clearAndNullCaches();
    }

    @Test
    public void execute_shouldAddToCacheAndSignalDownloadSuccess() throws Exception {
        subject.execute(videoUrl);

        semaphore.acquire();
        verify(mVastVideoDownloadTaskListener).onComplete(true);

        final byte[] data = CacheService.getFromDiskCache(videoUrl);
        assertThat(data).isEqualTo("responseBody".getBytes());
    }

    @Test
    public void execute_withMultipleUrls_shouldParseTheFirstOne() throws Exception {
        String ignoredUrl = "ignored";
        subject.execute(videoUrl, ignoredUrl);

        semaphore.acquire();
        verify(mVastVideoDownloadTaskListener).onComplete(true);

        assertThat(CacheService.getFromDiskCache(videoUrl)).isEqualTo("responseBody".getBytes());
        assertThat(CacheService.getFromDiskCache(ignoredUrl)).isNull();
    }

    @Test
    public void execute_whenUrlArrayIsNull_shouldSignalDownloadFailed() throws Exception {
        subject.execute((String) null);

        semaphore.acquire();
        verify(mVastVideoDownloadTaskListener).onComplete(false);
    }

    @Test
    public void execute_whenFirstElementOfUrlArrayIsNull_shouldSignalDownloadFailed() throws Exception {
        subject.execute(null, "ignored");

        semaphore.acquire();
        verify(mVastVideoDownloadTaskListener).onComplete(false);
    }

    @Test
    public void execute_whenDiskCacheIsNotInitialized_shouldNotPutDataInCacheAndShouldSignalDownloadFailed() throws Exception {
        CacheService.clearAndNullCaches();
        CacheServiceTest.assertDiskCacheIsUninitialized();
        subject.execute(videoUrl);

        semaphore.acquire();
        CacheServiceTest.assertDiskCacheIsUninitialized();
        verify(mVastVideoDownloadTaskListener).onComplete(false);
    }

    @Test
    public void execute_whenResponseContentLengthIsLargerThan25MiB_shouldNotPutDataInCacheAndShouldSignalDownloadFailed() throws Exception {
        Robolectric.clearPendingHttpResponses();
        final String randomString = createRandomString(25 * 1024 * 1024 + 1);
        Robolectric.addPendingHttpResponse(new TestHttpResponse(200, randomString));
        subject.execute(videoUrl);

        semaphore.acquire();
        CacheServiceTest.assertDiskCacheIsEmpty();
        verify(mVastVideoDownloadTaskListener).onComplete(false);
    }

    @Test
    public void onPostExecute_whenOnDownloadCompleteListenerIsNull_shouldNotBlowUp() throws Exception {
        subject = new VastVideoDownloadTask(null);

        subject.onPostExecute(true);
        subject.onPostExecute(false);

        // pass
    }

    private static String createRandomString(int size) {
        byte[] buffer = new byte[size];
        new Random().nextBytes(buffer);
        return new String(buffer);
    }
}
