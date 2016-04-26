package com.mopub.common;

import android.app.Activity;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.Semaphore;

import static com.mopub.common.CacheService.DiskLruCacheGetListener;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class CacheServiceTest {

    private Activity context;
    private String key1;
    private String data1;
    private String getKey;
    private byte[] getBytes;
    private DiskLruCacheGetListener diskCacheGetListener;
    private Semaphore semaphore;

    @Before
    public void setUp() throws Exception {
        context = Robolectric.buildActivity(Activity.class).create().get();
        key1 = "https://www.mopub.com/";
        data1 = "image_data_1";

        semaphore = new Semaphore(0);
        diskCacheGetListener = mock(DiskLruCacheGetListener.class);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Object[] args = invocationOnMock.getArguments();
                CacheServiceTest.this.getKey = (String)args[0];
                CacheServiceTest.this.getBytes = (byte[])args[1];
                semaphore.release();
                return null;
            }
        }).when(diskCacheGetListener).onComplete(anyString(), any(byte[].class));
    }

    @Test
    public void initializeDiskCache_withNullCacheDirectory_shouldNotThrowNpe_shouldReturnFalse() {
        Activity mockContext = mock(Activity.class);
        when(mockContext.getCacheDir()).thenReturn(null);

        assertThat(CacheService.initializeDiskCache(mockContext)).isFalse();
    }

    @Test
    public void initializeCache_withValidContext_shouldCreateNewCachesIdempotently() throws Exception {
        assertThat(CacheService.getDiskLruCache()).isNull();

        CacheService.initialize(context);
        DiskLruCache diskLruCache = CacheService.getDiskLruCache();
        assertThat(diskLruCache).isNotNull();

        CacheService.initialize(context);
        assertThat(diskLruCache).isEqualTo(CacheService.getDiskLruCache());
    }
    
    @Test
    public void getDiskLruCacheDirectory_shouldReturnValidCacheDirectory() throws Exception {
        File file = CacheService.getDiskCacheDirectory(context);
        String expectedPath = context.getCacheDir().toString() + "/mopub-cache";
        assertThat(file.getAbsolutePath()).isEqualTo(expectedPath);
    }

    @Test
    public void diskLruCacheGet_whenPopulated_shouldReturnValue() throws Exception {
        CacheService.initialize(context);
        CacheService.putToDiskCache(key1, data1.getBytes());
        assertThat(CacheService.getFromDiskCache(key1)).isEqualTo(data1.getBytes());
    }

    @Test
    public void diskLruCacheGet_whenEmpty_shouldReturnNull() throws Exception {
        CacheService.initialize(context);
        assertCachesAreEmpty();
        assertThat(CacheService.getFromDiskCache(key1)).isNull();
    }

    @Test
    public void diskLruCacheAsyncGet_whenPopulated_shouldReturnValue() throws Exception {
        CacheService.initialize(context);
        assertCachesAreEmpty();
        CacheService.putToDiskCache(key1, data1.getBytes());
        CacheService.getFromDiskCacheAsync(key1, diskCacheGetListener);
        semaphore.acquire();
        assertThat(getKey).isEqualTo(key1);
        assertThat(getBytes).isEqualTo(data1.getBytes());
    }

    @Test
    public void diskLruCacheAsyncGet_whenEmpty_shouldReturnNull() throws Exception {
        CacheService.initialize(context);
        CacheService.getFromDiskCacheAsync(key1, diskCacheGetListener);
        semaphore.acquire();
        assertThat(getKey).isEqualTo(key1);
        assertThat(getBytes).isNull();
    }

    @Test
    public void diskLruCachePut_withEmptyStringKey_shouldPutCorrectly() throws Exception {
        // this works because an empty string sha1 hashes to a valid key
        CacheService.initialize(context);
        CacheService.putToDiskCache("", data1.getBytes());
        assertThat(CacheService.getFromDiskCache("")).isEqualTo(data1.getBytes());
    }

    @Test
    public void diskLruCachePut_withNullKey_shouldNotPut() throws Exception {
        // null value produces empty string key which is invalid for disk lru cache
        CacheService.initialize(context);
        assertCachesAreEmpty();
        CacheService.putToDiskCache(null, data1.getBytes());
        assertCachesAreEmpty();
    }

    @Test
    public void createValidDiskLruCacheKey_withNullValue_shouldReturnEmptyString() throws Exception {
        CacheService.initialize(context);
        assertThat(CacheService.createValidDiskCacheKey(null)).isEqualTo("");
    }

    @Test
    public void diskLruCacheAsyncPut_whenEmpty_shouldReturnNull() throws Exception {
        CacheService.initialize(context);
        CacheService.putToDiskCacheAsync(key1, data1.getBytes());
        Thread.sleep(500);
        assertThat(CacheService.getFromDiskCache(key1)).isEqualTo(data1.getBytes());
    }

    private static InputStream getInputStreamFromString(final String string) {
        return spy(new ByteArrayInputStream(string.getBytes()));
    }

    public static void assertDiskCacheIsUninitialized() {
        assertThat(CacheService.getDiskLruCache()).isNull();
    }

    public static void assertDiskCacheIsEmpty() {
        assertThat(CacheService.getDiskLruCache()).isNotNull();
        assertThat(CacheService.getDiskLruCache().size()).isEqualTo(0);
    }

    public static void assertCachesAreEmpty() {
        assertDiskCacheIsEmpty();
    }
}
