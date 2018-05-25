package com.mopub.nativeads;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.mopub.common.CacheService;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.TreeSet;

/**
 * This data source caches data on disk as it is read from an {@link HttpDataSource}. This expects
 * relatively large files, and this will segment the files. If any segment becomes invalid, the
 * entire file is effectively cleared from the cache.
 */
public class HttpDiskCompositeDataSource implements DataSource {

    // Keys are prefixed since URLs can end basically however they want, and key names could
    // potentially be part of the URL and get the cache confused.
    @VisibleForTesting static final String INTERVALS_KEY_PREFIX = "intervals-sorted-";
    @VisibleForTesting static final String EXPECTED_FILE_SIZE_KEY_PREFIX = "expectedsize-";

    // These are used to serialize/deserialize the intervals list
    private static final String START = "start";
    private static final String LENGTH = "length";

    /**
     * The constant used in {@link DefaultHttpDataSource} is private even though this is a pretty
     * standard constant used in Exoplayer. This represents the constant that tells the HTTP
     * connection to get all remaining bytes available.
     */
    @VisibleForTesting static final int LENGTH_UNBOUNDED = -1;

    /**
     * HTTP response 416 means trying to request for bytes that the server does not have.
     */
    private static final int HTTP_RESPONSE_REQUESTED_RANGE_NOT_SATISFIABLE = 416;

    /**
     * The current block size is arbitrarily set at 500KiB. This has to be bigger than the biggest
     * read request from the consumer of this class's read method. This also has to be reasonably
     * small to accommodate devices that don't have a lot of memory to work with.
     */
    @VisibleForTesting static final int BLOCK_SIZE = 500 * 1024;

    /**
     * The network data source
     */
    @NonNull private final HttpDataSource mHttpDataSource;

    /**
     * Bytes from disk. This is the in-memory working copy.
     */
    @Nullable private byte[] mCachedBytes;

    /**
     * Base key of the cache. This is the URI of the {@link DataSpec}.
     */
    @Nullable private String mKey;

    /**
     * This is the set of intervals that the cache thinks are valid. Intervals have a start and a
     * length.
     */
    @NonNull private final TreeSet<IntInterval> mIntervals;

    /**
     * The absolute index of the first byte that is currently being requested.
     */
    private int mStartInFile;

    /**
     * The total number of bytes read in the current block. This indicates the current cursor
     * position. mDataBlockOffset plus mStartInFile is the cursor position for the file.
     * mDataBlockOffset plus mStartInDataBlock is the cursor position for the current data block.
     */
    private int mDataBlockOffset;

    /**
     * Which segment of the file the current block is on.
     */
    private int mSegment;

    /**
     * The index of the physical byte array for the current block.
     */
    private int mStartInDataBlock;

    /**
     * Whether or not this has an {@link HttpDataSource} that is already open.
     */
    private boolean mIsHttpSourceOpen;

    /**
     * The expected size of the entire file.
     */
    @Nullable private Integer mExpectedFileLength = null;

    /**
     * Data needed to open another {@link HttpDataSource}.
     */
    @Nullable private DataSpec mDataSpec;

    /**
     * Whether or not the cache has been written to during the current session.
     */
    private boolean mIsDirty;

    public HttpDiskCompositeDataSource(@NonNull final Context context,
            @NonNull final String userAgent) {
        this(context, userAgent, new DefaultHttpDataSource(userAgent, null));
    }

    @VisibleForTesting
    HttpDiskCompositeDataSource(@NonNull final Context context,
            @NonNull final String userAgent,
            @NonNull final HttpDataSource httpDataSource) {
        mHttpDataSource = httpDataSource;
        CacheService.initialize(context);
        mIntervals = new TreeSet<IntInterval>();
    }

    @Override
    public long open(@NonNull final DataSpec dataSpec) throws IOException {
        Preconditions.checkNotNull(dataSpec);
        if (dataSpec.uri == null) {
            return LENGTH_UNBOUNDED;
        }

        mIsDirty = false;

        mDataSpec = dataSpec;
        mKey = dataSpec.uri.toString();
        if (mKey == null) {
            return LENGTH_UNBOUNDED;
        }
        mStartInFile = (int) dataSpec.absoluteStreamPosition;
        mSegment = mStartInFile / BLOCK_SIZE;
        mCachedBytes = CacheService.getFromDiskCache(mSegment + mKey);
        mStartInDataBlock = mStartInFile % BLOCK_SIZE;
        mDataBlockOffset = 0;

        mExpectedFileLength = getExpectedFileLengthFromDisk(mKey);

        populateIntervalsFromDisk(mKey, mIntervals);

        int mDataRequestStartPoint = getFirstContiguousPointAfter(mStartInFile, mIntervals);

        // Cache miss
        if (mCachedBytes == null) {
            mCachedBytes = new byte[BLOCK_SIZE];

            // It's not in the cache, but we expected it to be there.
            if (mDataRequestStartPoint > mStartInFile) {
                MoPubLog.d("Cache segment " + mSegment + " was evicted. Invalidating cache");
                mIntervals.clear();
                mDataRequestStartPoint = (int) dataSpec.absoluteStreamPosition;
            }
        }

        long size;
        // If we think there are more bytes left to read from the network
        if (mExpectedFileLength == null || mDataRequestStartPoint != mExpectedFileLength) {
            final long lengthToUse;
            if (mDataSpec.length == LENGTH_UNBOUNDED) {
                lengthToUse = LENGTH_UNBOUNDED;
            } else {
                // Make sure to take into account that the start point is at a later point
                lengthToUse = mDataSpec.length - (mDataRequestStartPoint - mStartInFile);
            }
            // Modify the data spec to include the new params
            DataSpec modifiedDataSpec = new DataSpec(dataSpec.uri, mDataRequestStartPoint,
                    lengthToUse, dataSpec.key, dataSpec.flags);

            try {
                size = mHttpDataSource.open(modifiedDataSpec);
                if (mExpectedFileLength == null && lengthToUse == LENGTH_UNBOUNDED) {
                    // If we don't have an expected file length set, set it if we requested the
                    // rest of the file.
                    mExpectedFileLength = (int) (mStartInFile + size);
                    CacheService.putToDiskCache(EXPECTED_FILE_SIZE_KEY_PREFIX + mKey,
                            String.valueOf(mExpectedFileLength).getBytes());
                }
                mIsHttpSourceOpen = true;
            } catch (HttpDataSource.InvalidResponseCodeException e) {
                // This shouldn't happen anymore, but if we accidentally requested too many bytes
                // because we already had the bytes before that point, then it's still fine.
                if (e.responseCode == HTTP_RESPONSE_REQUESTED_RANGE_NOT_SATISFIABLE) {
                    size = mExpectedFileLength == null ? mDataRequestStartPoint - mStartInFile : mExpectedFileLength - mStartInFile;
                } else {
                    throw e;
                }
                mIsHttpSourceOpen = false;
            }
        } else {
            size = dataSpec.length == LENGTH_UNBOUNDED ? mExpectedFileLength - mStartInFile : dataSpec.length;
        }
        return size;
    }

    private static void populateIntervalsFromDisk(@NonNull final String key,
            @NonNull final TreeSet<IntInterval> intervals) {
        Preconditions.checkNotNull(key);
        Preconditions.checkNotNull(intervals);

        intervals.clear();
        byte[] intervalsFromDisk = CacheService.getFromDiskCache(INTERVALS_KEY_PREFIX + key);
        if (intervalsFromDisk != null) {
            String intervalsStringData = new String(intervalsFromDisk);
            try {
                JSONArray jsonIntervalArray = new JSONArray(intervalsStringData);
                for (int i = 0; i < jsonIntervalArray.length(); i++) {
                    JSONObject jsonInterval = new JSONObject((String) jsonIntervalArray.get(i));
                    intervals.add(new IntInterval(jsonInterval.getInt(START),
                            jsonInterval.getInt(LENGTH)));
                }
            } catch (JSONException e) {
                MoPubLog.d("clearing cache since invalid json intervals found", e);
                intervals.clear();
            } catch (ClassCastException e) {
                MoPubLog.d("clearing cache since unable to read json data");
                intervals.clear();
            }
        }
    }

    private static Integer getExpectedFileLengthFromDisk(@NonNull final String key) {
        Preconditions.checkNotNull(key);

        byte[] maxSizeByteArray = CacheService.getFromDiskCache(
                EXPECTED_FILE_SIZE_KEY_PREFIX + key);
        if (maxSizeByteArray != null) {
            try {
                return Integer.parseInt(new String(maxSizeByteArray));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public Uri getUri() {
        return mDataSpec != null ? mDataSpec.uri : null;
    }

    @Override
    public void close() throws IOException {
        if (!TextUtils.isEmpty(mKey) && mCachedBytes != null) {
            CacheService.putToDiskCache(mSegment + mKey, mCachedBytes);
            addNewInterval(mIntervals, mStartInFile, mDataBlockOffset);
            writeIntervalsToDisk(mIntervals, mKey);
        }
        mCachedBytes = null;

        mHttpDataSource.close();
        mIsHttpSourceOpen = false;
        mStartInFile = 0;
        mDataBlockOffset = 0;
        mStartInDataBlock = 0;
        mExpectedFileLength = null;
        mIsDirty = false;
    }

    private static void writeIntervalsToDisk(@NonNull final TreeSet<IntInterval> intervals,
            @NonNull final String key) {
        Preconditions.checkNotNull(intervals);
        Preconditions.checkNotNull(key);

        final JSONArray jsonIntervals = new JSONArray();
        for (IntInterval interval : intervals) {
            jsonIntervals.put(interval);
        }
        CacheService.putToDiskCache(INTERVALS_KEY_PREFIX + key,
                jsonIntervals.toString().getBytes());
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        if (length > BLOCK_SIZE) {
            MoPubLog.d(
                    "Reading more than the block size (" + BLOCK_SIZE + " bytes) at once is not possible. length = " + length);
            return -1;
        }
        if (mDataSpec == null) {
            MoPubLog.d("Unable to read from data source when no spec provided");
            return -1;
        }
        if (mCachedBytes == null) {
            MoPubLog.d("No cache set up. Call open before read.");
            return -1;
        }

        // Number of bytes available in the current block
        final int bytesAvailableInCurrentBlock = BLOCK_SIZE - mStartInDataBlock - mDataBlockOffset;

        // The position of the next expected break (could be end of file)
        final int farthestContiguousPoint = getFirstContiguousPointAfter(
                mStartInFile + mDataBlockOffset, mIntervals);
        // Amount of data expected to be in the cache
        final int validBytesLeftInCache = farthestContiguousPoint - mStartInFile - mDataBlockOffset;
        // The number of expected bytes to be able to read from the cache
        final int bytesToRead = Math.min(validBytesLeftInCache, length);
        // To keep track of actual bytes read from the cache
        int bytesReadFromDisk = 0;
        // If the data is available at least partially on disk
        if (areBytesAvailableInCache(farthestContiguousPoint, mStartInFile, mDataBlockOffset)) {
            // The case of when all of the bytes are available in the current block
            if (bytesToRead <= bytesAvailableInCurrentBlock) {
                System.arraycopy(mCachedBytes, mStartInDataBlock + mDataBlockOffset, buffer, offset,
                        bytesToRead);
                mDataBlockOffset += bytesToRead;
                bytesReadFromDisk += bytesToRead;
            } else {
                // Read all of the available bytes in the current block
                System.arraycopy(mCachedBytes, mStartInDataBlock + mDataBlockOffset, buffer, offset,
                        bytesAvailableInCurrentBlock);
                mDataBlockOffset += bytesAvailableInCurrentBlock;
                bytesReadFromDisk += bytesAvailableInCurrentBlock;

                // Flush the cache
                writeCacheToDiskAndClearVariables();

                // Read in the next segment from disk
                mCachedBytes = CacheService.getFromDiskCache(mSegment + mKey);
                if (mCachedBytes == null) {
                    // If there is a mismatch between expected bytes available in the cache and what
                    // is actually in the cache, this is an unrecoverable problem. Reset the cache
                    // and clear the data and open a new HTTP connection starting at the current position.
                    MoPubLog.d("Unexpected cache miss. Invalidating cache");
                    mIntervals.clear();
                    mCachedBytes = new byte[BLOCK_SIZE];
                    mHttpDataSource.close();

                    mHttpDataSource.open(
                            new DataSpec(mDataSpec.uri, mStartInFile + mDataBlockOffset,
                                    LENGTH_UNBOUNDED, mDataSpec.key, mDataSpec.flags));
                    mIsHttpSourceOpen = true;
                } else {
                    // If the data is available in the cache, read the remaining bytes into the
                    // buffer, additionally offset by what has already been written to the buffer.
                    System.arraycopy(mCachedBytes, mStartInDataBlock + mDataBlockOffset, buffer,
                            offset + bytesReadFromDisk,
                            bytesToRead - bytesReadFromDisk);
                    mDataBlockOffset += bytesToRead - bytesReadFromDisk;
                    bytesReadFromDisk = bytesToRead;
                }
            }
        }

        // If we have read enough data from disk, don't ask for network data
        final int bytesToReadFromNetwork = length - bytesReadFromDisk;
        if (bytesToReadFromNetwork <= 0) {
            return bytesReadFromDisk;
        }

        mIsDirty = true;

        // This should never happen, but if we lose network or something, this might happen
        if (!mIsHttpSourceOpen) {
            MoPubLog.d("end of cache reached. No http source open");
            return -1;
        }

        // Read from network and store to disk
        int bytesReadFromNetwork = mHttpDataSource.read(buffer, offset + bytesReadFromDisk,
                bytesToReadFromNetwork);

        final int bytesAvailableInCurrentBlockForNetwork =
                BLOCK_SIZE - mStartInDataBlock - mDataBlockOffset;
        if (bytesAvailableInCurrentBlockForNetwork < bytesReadFromNetwork) {
            // If there is not enough room in the current block, write up to the end of the current
            // block, set up a new segment (which may have data in the cache already), and write
            // the rest of the data.
            System.arraycopy(buffer, offset + bytesReadFromDisk, mCachedBytes,
                    mStartInDataBlock + mDataBlockOffset, bytesAvailableInCurrentBlockForNetwork);
            mDataBlockOffset += bytesAvailableInCurrentBlockForNetwork;

            writeCacheToDiskAndClearVariables();

            mCachedBytes = CacheService.getFromDiskCache(mSegment + mKey);
            if (mCachedBytes == null) {
                mCachedBytes = new byte[BLOCK_SIZE];
            }

            System.arraycopy(buffer,
                    offset + bytesAvailableInCurrentBlockForNetwork + bytesReadFromDisk,
                    mCachedBytes, mStartInDataBlock + mDataBlockOffset,
                    bytesReadFromNetwork - bytesAvailableInCurrentBlockForNetwork);
            mDataBlockOffset += bytesReadFromNetwork - bytesAvailableInCurrentBlockForNetwork;
        } else {
            System.arraycopy(buffer, offset + bytesReadFromDisk, mCachedBytes,
                    mStartInDataBlock + mDataBlockOffset, bytesReadFromNetwork);
            mDataBlockOffset += bytesReadFromNetwork;
        }

        return bytesReadFromNetwork + bytesReadFromDisk;
    }

    private static boolean areBytesAvailableInCache(final int farthestContiguousPoint,
            final int startInFile, final int dataBlockOffset) {
        return farthestContiguousPoint > startInFile + dataBlockOffset;
    }

    private void writeCacheToDiskAndClearVariables() {
        CacheService.putToDiskCache(mSegment + mKey, mCachedBytes);
        addNewInterval(mIntervals, mStartInFile, mDataBlockOffset);
        mStartInDataBlock = 0;
        mStartInFile = mStartInFile + mDataBlockOffset;
        mDataBlockOffset = 0;
        mSegment = mStartInFile / BLOCK_SIZE;
    }

    /**
     * Gets the first contiguous point from disk that we have starting from the given point. If
     * there is no segment that contains this point, return that point.
     */
    @VisibleForTesting
    static int getFirstContiguousPointAfter(int point,
            @NonNull final TreeSet<IntInterval> intervals) {
        Preconditions.checkNotNull(intervals);

        int lastContiguousPoint = point;
        for (final IntInterval interval : intervals) {
            if (interval.getStart() <= lastContiguousPoint) {
                lastContiguousPoint = Math.max(lastContiguousPoint,
                        interval.getStart() + interval.getLength());
            }
        }
        return lastContiguousPoint;
    }

    /**
     * Adds the interval if the interval does not already exist.
     *
     * @param intervals The current set of intervals
     * @param start     The starting point of this interval
     * @param length    The length of this interval
     */
    @VisibleForTesting
    static void addNewInterval(@NonNull final TreeSet<IntInterval> intervals, final int start,
            final int length) {
        Preconditions.checkNotNull(intervals);

        if (getFirstContiguousPointAfter(start, intervals) >= start + length) {
            return;
        }
        intervals.add(new IntInterval(start, length));
    }
}
