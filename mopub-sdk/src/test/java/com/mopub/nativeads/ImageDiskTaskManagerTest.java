package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;

import com.mopub.common.CacheService;
import com.mopub.common.CacheServiceTest;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.tester.org.apache.http.FakeHttpLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static com.mopub.nativeads.TaskManager.TaskManagerListener;
import static junit.framework.Assert.fail;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doAnswer;

@RunWith(SdkTestRunner.class)
public class ImageDiskTaskManagerTest {

    @Mock private TaskManagerListener<Bitmap> imageTaskManagerListener;
    private Semaphore semaphore;
    private Map<String, Bitmap> bitmaps;
    private FakeHttpLayer fakeHttpLayer;
    private String url1;
    private String url2;
    private String url3;
    private String imageData1;
    private String imageData2;
    private String imageData3;
    private List<String> list;
    private Context context;
    private static final int TEST_WIDTH = 400;

    @Before
    public void setUp() throws Exception {
        context = new Activity();
        semaphore = new Semaphore(0);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                Map<String, Bitmap> bitmaps = (Map)args[0];
                ImageDiskTaskManagerTest.this.bitmaps = bitmaps;
                semaphore.release();
                return null;
            }
        }).when(imageTaskManagerListener).onSuccess(anyMap());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                semaphore.release();
                return null;
            }
        }).when(imageTaskManagerListener).onFail();

        fakeHttpLayer = Robolectric.getFakeHttpLayer();
        url1 = "http://www.mopub.com/";
        url2 = "http://www.twitter.com";
        url3 = "http://www.guydot.com";
        imageData1 = "image_data_1";
        imageData2 = "image_data_2";
        imageData3 = "image_data_3";

        list = new ArrayList<String>();
        list.add(url1);
        list.add(url2);
    }

    @After
    public void tearDown() throws Exception {
        CacheService.clearAndNullCaches();
    }

    @Test
    public void constructor_withNullUrlsList_shouldThrowIllegalArgumentException() throws Exception {
        try {
            new ImageDiskTaskManager(null, imageTaskManagerListener, TEST_WIDTH);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void constructor_withNullInUrlsList_shouldThrowIllegalArgumentException() throws Exception {
        List<String> myList = new ArrayList<String>();
        myList.add(null);
        try {
            new ImageDiskTaskManager(myList, imageTaskManagerListener, TEST_WIDTH);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void constructor_withNullImageTaskManagerListener_shouldThrowIllegalArgumentException() throws Exception {
        try {
            new ImageDiskTaskManager(list, null, TEST_WIDTH);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void execute_withEmptyDiskCache_shouldReturnNullsInMap() throws Exception {
        new ImageDiskTaskManager(list, imageTaskManagerListener, TEST_WIDTH).execute();
        semaphore.acquire();

        assertThat(bitmaps.size()).isEqualTo(2);
        assertThat(bitmaps.containsKey(url1)).isTrue();
        assertThat(bitmaps.containsKey(url2)).isTrue();
        assertThat(bitmaps.get(url1)).isNull();
        assertThat(bitmaps.get(url2)).isNull();
    }

    @Test
    public void execute_withPopulatedDiskCache_shouldReturnImagesInMap() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();
        CacheService.putToDiskCache(url1, imageData1.getBytes());
        CacheService.putToDiskCache(url2, imageData2.getBytes());

        new ImageDiskTaskManager(list, imageTaskManagerListener, TEST_WIDTH).execute();
        semaphore.acquire();

        assertThat(bitmaps.size()).isEqualTo(2);
        assertThat(bitmaps.get(url1)).isNotNull();
        assertThat(bitmaps.get(url2)).isNotNull();
    }

    @Test
    public void execute_withPartiallyPopulatedDiskCache_shouldReturnSomeImagesInMap() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();
        CacheService.putToDiskCache(url1, imageData1.getBytes());

        new ImageDiskTaskManager(list, imageTaskManagerListener, TEST_WIDTH).execute();
        semaphore.acquire();

        assertThat(bitmaps.size()).isEqualTo(2);
        assertThat(bitmaps.get(url1)).isNotNull();
        assertThat(bitmaps.containsKey(url2)).isTrue();
        assertThat(bitmaps.get(url2)).isNull();
    }
}
