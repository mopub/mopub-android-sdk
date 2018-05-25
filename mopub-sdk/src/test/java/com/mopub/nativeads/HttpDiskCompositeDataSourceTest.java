package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.mopub.common.CacheService;
import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
@Config(constants = BuildConfig.class)
public class HttpDiskCompositeDataSourceTest {

    private static final int BASE_SEED = 1234567890;
    @Mock HttpDataSource mockHttpDataSource;
    private HttpDiskCompositeDataSource subject;
    private DataSpec dataSpec;
    private Uri uri;


    @Before
    public void setUp() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        CacheService.initialize(context);
        subject = new HttpDiskCompositeDataSource(context, "userAgent", mockHttpDataSource);
        uri = new Uri.Builder().scheme("https").path("www.someurl").build();
        dataSpec = new DataSpec(uri, 0, -1, null);
    }

    @After
    public void tearDown() throws Exception {
        CacheService.clearAndNullCaches();
    }

    @Test
    public void open_withNullDataSpecUri_shouldReturnLengthUnbounded() throws Exception {
        DataSpec dataSpecWithNullUri = new DataSpec(null);

        final long result = subject.open(dataSpecWithNullUri);

        assertThat(result).isEqualTo(HttpDiskCompositeDataSource.LENGTH_UNBOUNDED);
        verifyZeroInteractions(mockHttpDataSource);
    }

    @Test
    public void open_withNoCachedData_shouldOpenHttpDataSource() throws Exception {
        when(mockHttpDataSource.open(any(DataSpec.class))).thenReturn(200000L);

        long result = subject.open(dataSpec);

        assertThat(result).isEqualTo(200000L);
        verify(mockHttpDataSource).open(refEq(dataSpec));
        verifyNoMoreInteractions(mockHttpDataSource);
    }

    @Test
    public void open_withAllDataCached_shouldNotOpenHttpDataSource() throws Exception {
        // When the entire file is cached, there's no need to open the network.
        when(mockHttpDataSource.open(any(DataSpec.class))).thenReturn(100000L);
        byte[] data = generateRandomByteArray(100000, 0);
        byte[] expectedFileSize = "100000".getBytes();
        byte[] intervals = "[\"{start : 0, length : 100000}\"]".getBytes();
        CacheService.putToDiskCache("0" + uri.toString(), data);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.EXPECTED_FILE_SIZE_KEY_PREFIX + uri.toString(),
                expectedFileSize);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.INTERVALS_KEY_PREFIX + uri.toString(), intervals);

        long result = subject.open(dataSpec);

        assertThat(result).isEqualTo(100000);
        verifyZeroInteractions(mockHttpDataSource);
    }

    @Test
    public void open_withSomeDataCached_shouldOpenHttpDataSourceAtAppropriateStartPoint() throws Exception {
        // The idea behind this test is that we think we have the first 33333 bytes and need to ask
        // the network for byte 33333 and onward.
        when(mockHttpDataSource.open(any(DataSpec.class))).thenReturn(100000L);
        byte[] data = generateRandomByteArray(100000, 0);
        byte[] expectedFileSize = "100000".getBytes();
        byte[] intervals = "[\"{start : 0, length : 33333}\"]".getBytes();
        CacheService.putToDiskCache("0" + uri.toString(), data);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.EXPECTED_FILE_SIZE_KEY_PREFIX + uri.toString(),
                expectedFileSize);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.INTERVALS_KEY_PREFIX + uri.toString(), intervals);

        long result = subject.open(dataSpec);

        assertThat(result).isEqualTo(100000);
        DataSpec expectedDataSpec = new DataSpec(dataSpec.uri, 33333, -1, null);
        // Using refEq because equals() is just the Java default, and they're not the same object.
        verify(mockHttpDataSource).open(refEq(expectedDataSpec));
    }

    @Test
    public void read_with2048ExpectedBytes_shouldFillBufferWith2048Bytes_shouldReturnNumberOfBytesRead2048() throws Exception {
        // This is the case where we're reading from a cache that has all the necessary bytes
        byte[] data = generateRandomByteArray(100000, 0);
        byte[] expectedFileSize = "100000".getBytes();
        byte[] intervals = "[\"{start : 0, length : 100000}\"]".getBytes();
        // Pretend we have all the bytes in the cache, complete with the expected file size and
        // a valid intervals set.
        CacheService.putToDiskCache("0" + uri.toString(), data);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.EXPECTED_FILE_SIZE_KEY_PREFIX + uri.toString(),
                expectedFileSize);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.INTERVALS_KEY_PREFIX + uri.toString(), intervals);
        byte[] readBuffer = new byte[2048];

        subject.open(dataSpec);
        int bytesRead = subject.read(readBuffer, 0, 2048);

        // Verify that all 2048 bytes are expected and that the return value of read() is 2048
        assertThat(bytesRead).isEqualTo(2048);
        byte[] expectedData = new byte[2048];
        System.arraycopy(data, 0, expectedData, 0, 2048);
        assertThat(readBuffer).isEqualTo(expectedData);
        verifyZeroInteractions(mockHttpDataSource);
    }

    @Test
    public void read_withAllDataCached_whenReadingAcrossSegmentBoundaries_shouldReturnAllBytes() throws Exception {
        // This is the case where we're reading from a cache that has all the necessary bytes, but
        // some of it is in the next block. We need to finish reading from the current block, set
        // up reading from the next block, and then do it.
        byte[] data1 = generateRandomByteArray(HttpDiskCompositeDataSource.BLOCK_SIZE, 0);
        byte[] data2 = generateRandomByteArray(HttpDiskCompositeDataSource.BLOCK_SIZE, 1);
        String expectedFileLengthString = String.valueOf(
                2 * HttpDiskCompositeDataSource.BLOCK_SIZE);
        byte[] intervals = ("[\"{start : 0, length : " + expectedFileLengthString + "}\"]").getBytes();
        CacheService.putToDiskCache("0" + uri.toString(), data1);
        CacheService.putToDiskCache("1" + uri.toString(), data2);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.EXPECTED_FILE_SIZE_KEY_PREFIX + uri.toString(),
                expectedFileLengthString.getBytes());
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.INTERVALS_KEY_PREFIX + uri.toString(), intervals);
        // Expect to read the last 1337 bytes from the first block and the rest from the second block
        DataSpec modifiedDataSpec = new DataSpec(dataSpec.uri,
                HttpDiskCompositeDataSource.BLOCK_SIZE - 1337, -1, null);
        byte[] readBuffer = new byte[4096];

        subject.open(modifiedDataSpec);
        int bytesRead = subject.read(readBuffer, 0, 4096);

        assertThat(bytesRead).isEqualTo(4096);
        byte[] expectedBytes = new byte[4096];
        System.arraycopy(data1, HttpDiskCompositeDataSource.BLOCK_SIZE - 1337, expectedBytes, 0,
                1337);
        System.arraycopy(data2, 0, expectedBytes, 1337, 4096 - 1337);
        assertThat(readBuffer).isEqualTo(expectedBytes);
        verifyZeroInteractions(mockHttpDataSource);
    }

    @Test
    public void read_withEmptyCache_shouldReadBytesFromNetwork() throws Exception {
        // Sets up an empty cache and read directly from the network
        final byte[] bytesFromNetwork = generateRandomByteArray(2048, 0);
        when(mockHttpDataSource.open(any(DataSpec.class))).thenReturn(100000L);
        setUpMockHttpDataSourceToReturnBytesFromNetwork(bytesFromNetwork, mockHttpDataSource);
        byte[] readBuffer = new byte[2048];

        subject.open(dataSpec);
        int bytesRead = subject.read(readBuffer, 0, 2048);

        assertThat(bytesRead).isEqualTo(2048);
        assertThat(readBuffer).isEqualTo(bytesFromNetwork);
        verify(mockHttpDataSource).open(refEq(dataSpec));
        verify(mockHttpDataSource).read(any(byte[].class), eq(0), eq(2048));
        verifyNoMoreInteractions(mockHttpDataSource);
    }

    @Test
    public void read_withSomeDataInCache_shouldReadRestOfBytesFromNetwork() throws Exception {
        // This is the case where there is some data in the cache, and we run out of cached data.
        // The rest of the bytes are from the network.
        final byte[] bytesFromNetwork = generateRandomByteArray(2048, 0);
        when(mockHttpDataSource.open(any(DataSpec.class))).thenReturn(100000L);
        setUpMockHttpDataSourceToReturnBytesFromNetwork(bytesFromNetwork, mockHttpDataSource);
        byte[] data = generateRandomByteArray(2048, 1);
        byte[] segment0 = new byte[HttpDiskCompositeDataSource.BLOCK_SIZE];
        byte[] expectedFileSize = "100000".getBytes();
        byte[] intervals = ("[\"{start : 0, length : 2048}\"]").getBytes();
        // So we have the first 2048 bytes in the cache
        System.arraycopy(data, 0, segment0, 0, 2048);
        CacheService.putToDiskCache("0" + uri.toString(), segment0);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.EXPECTED_FILE_SIZE_KEY_PREFIX + uri.toString(),
                expectedFileSize);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.INTERVALS_KEY_PREFIX + uri.toString(), intervals);
        // However, we want to read 4096 bytes (We have to go to the network for the rest).
        byte[] readBuffer = new byte[4096];

        subject.open(dataSpec);
        int bytesRead = subject.read(readBuffer, 0, 4096);

        // Make sure that the bytes read have both the first 2048 bytes from the cache and the 2048
        // bytes from the network.
        assertThat(bytesRead).isEqualTo(4096);
        byte[] expectedBytes = new byte[4096];
        System.arraycopy(data, 0, expectedBytes, 0, 2048);
        System.arraycopy(bytesFromNetwork, 0, expectedBytes, 2048, 2048);
        assertThat(readBuffer).isEqualTo(expectedBytes);
        DataSpec modifiedDataSpec = new DataSpec(dataSpec.uri, 2048, -1, null);
        verify(mockHttpDataSource).open(refEq(modifiedDataSpec));
        // Verify that we stored the bytes from network starting from index 2048 for 2048 bytes
        verify(mockHttpDataSource).read(any(byte[].class), eq(2048), eq(2048));
        verifyNoMoreInteractions(mockHttpDataSource);
    }

    @Test
    public void read_withSomeDataInCacheBeforeBlockBoundary_whenApproachingBlockBoundary_shouldReadBytesFromDisk_shouldReadBytesFromNetwork_shouldSetUpNextBlock() throws Exception {
        // This is the situation where the cache ends 1024 bytes before the second block boundary.
        // We need to read the first 1024 bytes from the cache, read the next 3072 bytes, store
        // the first 1024 bytes in the current block, close it out, flush it, set up the next block,
        // and store the next 2048 bytes into that block.
        final byte[] bytesFromNetwork = generateRandomByteArray(3072, 0);
        when(mockHttpDataSource.open(any(DataSpec.class))).thenReturn(
                (long) (HttpDiskCompositeDataSource.BLOCK_SIZE * 4));
        setUpMockHttpDataSourceToReturnBytesFromNetwork(bytesFromNetwork, mockHttpDataSource);
        byte[] data = generateRandomByteArray(HttpDiskCompositeDataSource.BLOCK_SIZE, 1);
        byte[] expectedFileSize = String.valueOf(
                HttpDiskCompositeDataSource.BLOCK_SIZE * 4).getBytes();
        // Typical access patterns of video is to request for the first 44 bytes, the last 3886
        // bytes, and then start at byte 44 and request till the end of the video. We are pretending
        // that we did that, and stopped 1024 bytes before the end of the 2nd block.
        byte[] intervals = ("[\"{start : 0, length : 44}\", \"{start : " +
                (HttpDiskCompositeDataSource.BLOCK_SIZE * 4 - 3886) +
                ", length : 3886}\", \"{start : 44, length : " +
                (HttpDiskCompositeDataSource.BLOCK_SIZE * 2 - 1024 - 44) + "}\"]")
                .getBytes();
        CacheService.putToDiskCache("1" + uri.toString(), data);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.EXPECTED_FILE_SIZE_KEY_PREFIX + uri.toString(),
                expectedFileSize);
        CacheService.putToDiskCache(
                HttpDiskCompositeDataSource.INTERVALS_KEY_PREFIX + uri.toString(), intervals);
        byte[] readBuffer = new byte[4096];
        dataSpec = new DataSpec(dataSpec.uri, 2 * HttpDiskCompositeDataSource.BLOCK_SIZE - 2048, -1,
                dataSpec.key, dataSpec.flags);

        subject.open(dataSpec);
        int bytesRead = subject.read(readBuffer, 0, 4096);

        assertThat(bytesRead).isEqualTo(4096);
        byte[] expectedBytes = new byte[4096];
        System.arraycopy(data, HttpDiskCompositeDataSource.BLOCK_SIZE - 2048, expectedBytes, 0,
                1024);
        System.arraycopy(bytesFromNetwork, 0, expectedBytes, 1024, 3072);
        assertThat(readBuffer).isEqualTo(expectedBytes);
        DataSpec modifiedDataSpec = new DataSpec(dataSpec.uri,
                2 * HttpDiskCompositeDataSource.BLOCK_SIZE - 1024, -1, null);
        verify(mockHttpDataSource).open(refEq(modifiedDataSpec));
        verify(mockHttpDataSource).read(any(byte[].class), eq(1024), eq(3072));
        // Also verify that the 2nd block was written to disk correctly.
        byte[] expectedBlock = new byte[HttpDiskCompositeDataSource.BLOCK_SIZE];
        System.arraycopy(data, 0, expectedBlock, 0, HttpDiskCompositeDataSource.BLOCK_SIZE - 1024);
        System.arraycopy(bytesFromNetwork, 0, expectedBlock,
                HttpDiskCompositeDataSource.BLOCK_SIZE - 1024, 1024);
        assertThat(CacheService.getFromDiskCache("1" + uri.toString())).isEqualTo(
                expectedBlock);
    }

    @Test
    public void close_withoutFirstCallingOpen_shouldNotWriteToDisk() throws Exception {
        subject.close();

        assertThat(CacheService.containsKeyDiskCache("0" + uri.toString())).isFalse();
        assertThat(CacheService.containsKeyDiskCache(
                HttpDiskCompositeDataSource.EXPECTED_FILE_SIZE_KEY_PREFIX + uri.toString())).isFalse();
        assertThat(CacheService.containsKeyDiskCache(
                HttpDiskCompositeDataSource.INTERVALS_KEY_PREFIX + uri.toString())).isFalse();
    }

    @Test
    public void close_withCurrentActiveBlock_shouldWriteToDisk() throws Exception {
        // Verifying that close() actually writes the current data to disk
        final byte[] bytesFromNetwork = generateRandomByteArray(1000, 0);
        when(mockHttpDataSource.open(any(DataSpec.class))).thenReturn(5000L);
        setUpMockHttpDataSourceToReturnBytesFromNetwork(bytesFromNetwork, mockHttpDataSource);
        byte[] readBuffer = new byte[1000];

        subject.open(dataSpec);
        subject.read(readBuffer, 0, 1000);
        subject.close();

        assertThat(readBuffer).isEqualTo(bytesFromNetwork);
        byte[] expectedBlock = new byte[HttpDiskCompositeDataSource.BLOCK_SIZE];
        System.arraycopy(bytesFromNetwork, 0, expectedBlock, 0, 1000);
        assertThat(CacheService.getFromDiskCache("0" + uri.toString())).isEqualTo(expectedBlock);
        assertThat(CacheService.getFromDiskCache(
                HttpDiskCompositeDataSource.EXPECTED_FILE_SIZE_KEY_PREFIX + uri.toString())).isEqualTo(
                "5000".getBytes());
        assertThat(CacheService.getFromDiskCache(
                HttpDiskCompositeDataSource.INTERVALS_KEY_PREFIX + uri.toString())).isEqualTo(
                "[\"{start : 0, length : 1000}\"]".getBytes());
    }

    @Test
    public void addNewInterval_shouldAddNewIntervalToIntervals() {
        IntInterval interval = new IntInterval(5, 42);
        TreeSet<IntInterval> intervalList = new TreeSet<IntInterval>();

        HttpDiskCompositeDataSource.addNewInterval(intervalList, 5, 42);

        assertThat(intervalList).containsOnly(interval);
    }

    @Test
    public void addNewInterval_withExistingInterval_shouldNotAddNewInterval() {
        IntInterval interval1 = new IntInterval(5, 42);
        IntInterval interval2 = new IntInterval(500, 200);
        TreeSet<IntInterval> intervalList = new TreeSet<IntInterval>();
        intervalList.add(interval1);
        intervalList.add(interval2);

        HttpDiskCompositeDataSource.addNewInterval(intervalList, 5, 42);

        assertThat(intervalList).containsOnly(interval1, interval2);
    }

    @Test
    public void addNewInterval_withExistingIntervalInParts_shouldNotAddNewInterval() {
        // The union of these two intervals is 5 with a length of 45 (ie. from 5 to 50).
        // An interval from 7 to 50 (start at 7, length 43) should not be added to the interval set.
        IntInterval interval1 = new IntInterval(10, 40);
        IntInterval interval2 = new IntInterval(5, 20);
        TreeSet<IntInterval> intervalList = new TreeSet<IntInterval>();
        intervalList.add(interval1);
        intervalList.add(interval2);

        HttpDiskCompositeDataSource.addNewInterval(intervalList, 7, 43);

        assertThat(intervalList).containsOnly(interval1, interval2);
    }

    @Test
    public void addNewInterval_withNonExistingIntervals_shouldAddNewInterval() {
        // The existing intervals here are from 10 to 100 and 150 to 250. 7 to 50 is not part of
        // that, even though the first interval already has part of that covered, 7 to 9 is not
        // covered by anything, so this interval should be added.
        IntInterval interval1 = new IntInterval(10, 90);
        IntInterval interval2 = new IntInterval(150, 100);
        TreeSet<IntInterval> intervalList = new TreeSet<IntInterval>();
        intervalList.add(interval1);
        intervalList.add(interval2);

        HttpDiskCompositeDataSource.addNewInterval(intervalList, 7, 43);

        assertThat(intervalList).containsExactly(new IntInterval(7, 43), interval1, interval2);
    }

    @Test
    public void getFirstContiguousPointAfter_withNoIntervals_shouldReturnInput() {
        TreeSet<IntInterval> intervalList = new TreeSet<IntInterval>();
        int startPoint = 12345;

        int firstContiguousPointAfterStartPoint = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(
                startPoint, intervalList);

        assertThat(firstContiguousPointAfterStartPoint).isEqualTo(startPoint);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getFirstContiguousPointAfter_withMultipleConnectedAndDisconnectedIntervals_shouldReturnFirstContiguousPointAfterStartPoint() {
        IntInterval[] intervalArray = new IntInterval[8];

        // Goes from 500 to 1250
        intervalArray[0] = new IntInterval(500, 200);
        intervalArray[1] = new IntInterval(700, 300);
        intervalArray[2] = new IntInterval(1000, 250);

        // Separate section that goes from 2000 to 3000
        intervalArray[3] = new IntInterval(2000, 1000);

        // Goes from 3500 to 4500 but with various sections that overlap
        intervalArray[4] = new IntInterval(3500, 750);
        intervalArray[5] = new IntInterval(4000, 100);
        intervalArray[6] = new IntInterval(4050, 200);
        intervalArray[7] = new IntInterval(3999, 501);

        TreeSet<IntInterval> intervalList = new TreeSet(Arrays.asList(intervalArray));

        // Before the first section
        int result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(499, intervalList);
        assertThat(result).isEqualTo(499);

        // At the start of the first section
        result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(500, intervalList);
        assertThat(result).isEqualTo(1250);

        // In the first section
        result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(567, intervalList);
        assertThat(result).isEqualTo(1250);

        // Between the first section and the second section
        result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(1337, intervalList);
        assertThat(result).isEqualTo(1337);

        // In the second section
        result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(2222, intervalList);
        assertThat(result).isEqualTo(3000);

        // Between the second section and the third section
        result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(3232, intervalList);
        assertThat(result).isEqualTo(3232);

        // At the start of the third section
        result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(3500, intervalList);
        assertThat(result).isEqualTo(4500);

        // In the third section
        result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(3789, intervalList);
        assertThat(result).isEqualTo(4500);

        // After the third section
        result = HttpDiskCompositeDataSource.getFirstContiguousPointAfter(4501, intervalList);
        assertThat(result).isEqualTo(4501);
    }

    /**
     * Creates a byte array and fills it with random data. Use the seed offset to generate
     * subsequent random byte arrays that are not the same random byte array.
     *
     * @param length     How many bytes in the byte array
     * @param seedOffset Offsets the seed so that different, random byte arrays can be created
     * @return byte array of specified length filled with random bytes\
     */
    private byte[] generateRandomByteArray(int length, int seedOffset) {
        byte[] byteArray = new byte[length];
        new Random(BASE_SEED + seedOffset).nextBytes(byteArray);
        return byteArray;
    }

    /**
     * Sets up the mock http data source to return the specified bytes when queried.
     *
     * @param bytesFromNetwork   The bytes to write to the buffer.
     * @param mockHttpDataSource The mock object that does this.
     * @throws HttpDataSource.HttpDataSourceException This should never happen since this is a
     *                                                mock.
     */
    private static void setUpMockHttpDataSourceToReturnBytesFromNetwork(
            final byte[] bytesFromNetwork,
            final HttpDataSource mockHttpDataSource) throws HttpDataSource.HttpDataSourceException {
        when(mockHttpDataSource.read(any(byte[].class), anyInt(), anyInt())).thenAnswer(
                new Answer<Long>() {
                    @Override
                    public Long answer(final InvocationOnMock invocation) throws Throwable {
                        final Object[] args = invocation.getArguments();
                        final byte[] byteBuffer = (byte[]) args[0];
                        final Integer offset = (Integer) args[1];
                        final Integer length = (Integer) args[2];
                        System.arraycopy(bytesFromNetwork, 0, byteBuffer, offset, length);
                        return (long) length;
                    }
                });
    }
}
