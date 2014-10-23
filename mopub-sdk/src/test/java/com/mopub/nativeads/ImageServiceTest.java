package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.mopub.common.CacheService;
import com.mopub.common.CacheServiceTest;
import com.mopub.common.DownloadResponse;
import com.mopub.nativeads.test.support.MoPubShadowBitmap;
import com.mopub.nativeads.test.support.MoPubShadowDisplay;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.tester.org.apache.http.FakeHttpLayer;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import static com.mopub.nativeads.ImageService.ImageServiceListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(shadows={MoPubShadowBitmap.class, MoPubShadowDisplay.class})
public class ImageServiceTest {
    private ImageServiceListener imageServiceListener;
    private Semaphore semaphore;
    private Map<String, Bitmap> bitmaps;
    private FakeHttpLayer fakeHttpLayer;
    private String url1;
    private String url2;
    private String url3;
    private String imageData1;
    private String imageData2;
    private String imageData3;
    private Context context;
    private Bitmap image2;
    private Bitmap image1;
    private DownloadResponse downloadResponse;

    @Before
    public void setUp() throws Exception {
        semaphore = new Semaphore(0);
        imageServiceListener = mock(ImageServiceListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                Map<String, Bitmap> bitmaps = (Map)args[0];
                ImageServiceTest.this.bitmaps = bitmaps;
                semaphore.release();
                return null;
            }
        }).when(imageServiceListener).onSuccess(anyMap());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                semaphore.release();
                return null;
            }
        }).when(imageServiceListener).onFail();

        downloadResponse = mock(DownloadResponse.class);
        fakeHttpLayer = Robolectric.getFakeHttpLayer();
        url1 = "http://www.mopub.com/";
        url2 = "http://www.twitter.com";
        url3 = "http://www.guydot.com";
        imageData1 = "image_data_1";
        imageData2 = "image_data_2";
        imageData3 = "image_data_3";
        image1 = BitmapFactory.decodeByteArray(imageData1.getBytes(), 0, imageData1.getBytes().length);
        image2 = BitmapFactory.decodeByteArray(imageData2.getBytes(), 0, imageData2.getBytes().length);
        context = new Activity();

        ImageService.initialize(context);
    }

    @After
    public void tearDown() throws Exception {
        CacheService.clearAndNullCaches();
    }

    @Test
    public void get_shouldInitializeCaches() throws Exception {
        CacheService.clearAndNullCaches();
        assertThat(CacheService.getBitmapLruCache()).isNull();
        assertThat(CacheService.getDiskLruCache()).isNull();

        ImageService.get(context, new ArrayList<String>(), imageServiceListener);

        assertThat(CacheService.getBitmapLruCache()).isNotNull();
        assertThat(CacheService.getDiskLruCache()).isNotNull();
    }

    @Test
    public void get_shouldGetDisplaySize() {
        ImageService.clear();
        assertThat(ImageService.getTargetWidth()).isEqualTo(-1);

        ImageService.get(context, new ArrayList<String>(), imageServiceListener);
        assertThat(ImageService.getTargetWidth()).isGreaterThan(-1);
    }

    @Test
    public void get_withImageInMemoryCache_shouldReturnImage() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();
        CacheService.putToBitmapCache(url1, image1);

        ImageService.get(context, Arrays.asList(url1), imageServiceListener);
        // no need for semaphore since memory cache is synchronous

        assertThat(shadowOf(bitmaps.get(url1)).getDescription())
                .isEqualTo("Bitmap for image_data_1");
    }

    @Test
    public void get_withImageInDiskCache_shouldReturnImage() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();
        CacheService.putToDiskCache(url1, imageData1.getBytes());

        ImageService.get(context, Arrays.asList(url1), imageServiceListener);
        semaphore.acquire();

        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo()).isNull();
        assertThat(shadowOf(bitmaps.get(url1)).getDescription())
                .isEqualTo("Bitmap for image_data_1");
    }

    @Test
    public void get_withEmptyCaches_shouldGetImageFromNetwork() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        fakeHttpLayer.addPendingHttpResponse(200, imageData1);

        ImageService.get(context, Arrays.asList(url1), imageServiceListener);
        semaphore.acquire();

        assertThat(shadowOf(bitmaps.get(url1)).getDescription())
                .isEqualTo("Bitmap for image_data_1");
    }

    @Test
    public void get_withImagesInMemoryCacheAndDiskCache_shouldReturnBothImages() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        CacheService.putToBitmapCache(url1, image1);
        CacheService.putToDiskCache(url2, imageData2.getBytes());

        ImageService.get(context, Arrays.asList(url1, url2), imageServiceListener);
        semaphore.acquire();

        assertThat(bitmaps.get(url1)).isEqualTo(image1);
        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo()).isNull();
        assertThat(shadowOf(bitmaps.get(url2)).getDescription())
                .isEqualTo("Bitmap for image_data_2");
    }

    @Test
    public void get_withImagesInMemoryAndNetwork_shouldReturnBothImages() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        CacheService.putToBitmapCache(url1, image1);
        fakeHttpLayer.addPendingHttpResponse(200, imageData2);

        ImageService.get(context, Arrays.asList(url1, url2), imageServiceListener);
        semaphore.acquire();

        assertThat(shadowOf(bitmaps.get(url1)).getCreatedFromBytes()).isEqualTo(imageData1.getBytes());
        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo().getHttpHost().toString()).isEqualTo(url2);
        assertThat(shadowOf(bitmaps.get(url2)).getDescription())
                .isEqualTo("Bitmap for image_data_2");
    }

    @Test
    public void get_withImagesInDiskAndNetwork_shouldReturnBothImages() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        CacheService.putToDiskCache(url1, imageData1.getBytes());
        fakeHttpLayer.addPendingHttpResponse(200, imageData2);

        ImageService.get(context, Arrays.asList(url1, url2), imageServiceListener);
        semaphore.acquire();

        assertThat(shadowOf(bitmaps.get(url1)).getDescription())
                .isEqualTo("Bitmap for image_data_1");
        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo().getHttpHost().toString()).isEqualTo(url2);
        assertThat(shadowOf(bitmaps.get(url2)).getDescription())
                .isEqualTo("Bitmap for image_data_2");
    }

    @Test
    public void get_withImagesInMemoryAndDiskAndNetwork_shouldReturnAllImages() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        CacheService.putToBitmapCache(url1, image1);
        CacheService.putToDiskCache(url2, imageData2.getBytes());
        fakeHttpLayer.addPendingHttpResponse(200, imageData3);

        ImageService.get(context, Arrays.asList(url1, url2, url3), imageServiceListener);
        semaphore.acquire();

        assertThat(shadowOf(bitmaps.get(url1)).getCreatedFromBytes()).isEqualTo(imageData1.getBytes());
        assertThat(shadowOf(bitmaps.get(url2)).getDescription())
                .isEqualTo("Bitmap for image_data_2");
        assertThat(shadowOf(bitmaps.get(url3)).getDescription())
                .isEqualTo("Bitmap for image_data_3");
    }

    @Test
    public void get_withSameKeysInMemoryAndDiskCache_shouldReturnValueFromMemoryCache() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        CacheService.putToBitmapCache(url1, image2);
        CacheService.putToDiskCache(url1, imageData1.getBytes());

        ImageService.get(context, Arrays.asList(url1), imageServiceListener);
        semaphore.acquire();

        assertThat(shadowOf(bitmaps.get(url1)).getCreatedFromBytes()).isEqualTo(imageData2.getBytes());
    }

    @Test
    public void get_withSameKeysInMemoryAndNetwork_shouldReturnValueFromMemoryCache() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        CacheService.putToBitmapCache(url1, image2);
        fakeHttpLayer.addPendingHttpResponse(200, imageData1);

        ImageService.get(context, Arrays.asList(url1), imageServiceListener);
        semaphore.acquire();

        assertThat(shadowOf(bitmaps.get(url1)).getCreatedFromBytes()).isEqualTo(imageData2.getBytes());
    }

    @Test
    public void get_withSameKeysInDiskAndNetwork_shouldReturnValueFromDiskCache() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        CacheService.putToDiskCache(url1, imageData2.getBytes());
        fakeHttpLayer.addPendingHttpResponse(200, imageData1);

        ImageService.get(context, Arrays.asList(url1), imageServiceListener);
        semaphore.acquire();

        assertThat(fakeHttpLayer.getLastSentHttpRequestInfo()).isNull();
        assertThat(shadowOf(bitmaps.get(url1)).getDescription())
                .isEqualTo("Bitmap for image_data_2");
    }

    @Test
    public void get_withNetworkFailure_shouldFail() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        CacheService.putToBitmapCache(url1, image1);
        CacheService.putToDiskCache(url2, imageData2.getBytes());
        fakeHttpLayer.addPendingHttpResponse(500, imageData3);

        ImageService.get(context, Arrays.asList(url1, url2, url3), imageServiceListener);
        semaphore.acquire();

        assertThat(bitmaps).isNull();
    }

    @Test
    public void get_withMultipleNetworkSuccessAndOneFailure_shouldFail() throws Exception {
        CacheService.initialize(context);
        CacheServiceTest.assertCachesAreEmpty();

        fakeHttpLayer.addPendingHttpResponse(200, imageData1);
        fakeHttpLayer.addPendingHttpResponse(200, imageData2);
        fakeHttpLayer.addPendingHttpResponse(500, imageData3);

        ImageService.get(context, Arrays.asList(url1, url2, url3), imageServiceListener);
        semaphore.acquire();

        assertThat(bitmaps).isNull();
    }

    @Test
    public void putDataInCache_populatesCaches() throws Exception {
        CacheService.initialize(context);

        Bitmap bitmap1 = BitmapFactory.decodeStream(getInputStreamFromString(imageData1));
        Bitmap bitmap2 = BitmapFactory.decodeStream(getInputStreamFromString(imageData2));

        assertThat(ImageService.getBitmapFromDiskCache(url1)).isNull();
        assertThat(ImageService.getBitmapFromDiskCache(url2)).isNull();
        assertThat(ImageService.getBitmapFromMemoryCache(url1)).isNull();
        assertThat(ImageService.getBitmapFromMemoryCache(url2)).isNull();

        ImageService.putDataInCache(url1, bitmap1, imageData1.getBytes());
        ImageService.putDataInCache(url2, bitmap2, imageData2.getBytes());

        Thread.sleep(500); // disk cache put is async

        assertThat(shadowOf(ImageService.getBitmapFromDiskCache(url1)).getDescription())
                .isEqualTo("Bitmap for image_data_1");
        assertThat(shadowOf(ImageService.getBitmapFromDiskCache(url2)).getDescription())
                .isEqualTo("Bitmap for image_data_2");
        assertThat(ImageService.getBitmapFromMemoryCache(url1)).isEqualTo(bitmap1);
        assertThat(ImageService.getBitmapFromMemoryCache(url2)).isEqualTo(bitmap2);
    }

    @Test
    public void getBitmapsFromMemoryCache_withEmptyCacheAndTwoUrls_returnsNoCacheHitsAndTwoCacheMisses() throws Exception {
        CacheService.initialize(context);
        assertThat(CacheService.getBitmapLruCache().size()).isEqualTo(0);

        Map<String, Bitmap> cacheHits = new HashMap<String, Bitmap>(2);
        List<String> cacheMisses =
                ImageService.getBitmapsFromMemoryCache(Arrays.asList(url1, url2), cacheHits);

        assertThat(cacheHits).isEmpty();
        assertThat(cacheMisses).containsOnly(url1, url2);
    }

    @Test
    public void getBitmapsFromMemoryCache_withOneCacheEntryAndTwoUrls_returnsOneCacheHitAndOneCacheMiss() throws Exception {
        CacheService.initialize(context);

        assertThat(CacheService.getBitmapLruCache().size()).isEqualTo(0);

        CacheService.putToBitmapCache(url1, image1);

        Map<String, Bitmap> cacheHits = new HashMap<String, Bitmap>(2);
        List<String> cacheMisses =
                ImageService.getBitmapsFromMemoryCache(Arrays.asList(url1, url2), cacheHits);

        assertThat(cacheHits.keySet()).containsOnly(url1);
        assertThat(cacheMisses).containsOnly(url2);
    }

    @Test
    public void asBitmap_withMaxSize_shouldReturnBitmap() {

        String imageData = "fake_bitmap_data";
        when(downloadResponse.getByteArray()).thenReturn(imageData.getBytes());

        final Bitmap bitmap = ImageService.asBitmap(downloadResponse, 30);

        assertThat(bitmap).isNotNull();
        assertThat(bitmap).isInstanceOf(Bitmap.class);
    }

    @Test
    public void asBitmap_withNullResponse_shouldReturnNull() throws Exception {
        final Bitmap bitmap = ImageService.asBitmap(null, 30);

        assertThat(bitmap).isNull();
    }

    @Test
    public void calculateInSampleSize_withImageSmallerThanRequested_shouldBe1() {
        int nativeWidth = 1024;
        assertThat(ImageService.calculateInSampleSize(nativeWidth, 2046)).isEqualTo(1);
    }

    @Test
    public void calculateInSampleSize_withImageSlightlyBiggerThanRequest_shouldBe1() {
        int nativeWidth = 1024;
        assertThat(ImageService.calculateInSampleSize(nativeWidth, 800)).isEqualTo(1);

    }

    @Test
    public void calculateInSampleSize_withImageMuchBiggerThanRequest_shouldBe4() {
        int nativeWidth = 2048;
        int nativeHeight = 1024;
        assertThat(ImageService.calculateInSampleSize(nativeWidth, 512)).isEqualTo(4);
    }

    private static InputStream getInputStreamFromString(final String string) {
        return spy(new ByteArrayInputStream(string.getBytes()));
    }
}
