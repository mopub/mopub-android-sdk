package com.mopub.common;

import com.mopub.common.util.ResponseHeader;
import com.mopub.common.util.Streams;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;

public class DownloadResponse {
    private byte[] mBytes = new byte[0];
    private final int mStatusCode;
    private final long mContentLength;
    private final Header[] mHeaders;

    public DownloadResponse(final HttpResponse httpResponse) throws Exception {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BufferedInputStream inputStream = null;
        try {
            HttpEntity httpEntity = httpResponse.getEntity();
            if (httpEntity != null) {
                inputStream = new BufferedInputStream(httpEntity.getContent());
                Streams.copyContent(inputStream, outputStream);
                mBytes = outputStream.toByteArray();
            }
        } finally {
            Streams.closeStream(inputStream);
            Streams.closeStream(outputStream);
        }

        mStatusCode = httpResponse.getStatusLine().getStatusCode();
        mContentLength = mBytes.length;
        mHeaders = httpResponse.getAllHeaders();
    }

    public byte[] getByteArray() {
        return mBytes;
    }

    public int getStatusCode() {
        return mStatusCode;
    }

    public long getContentLength() {
        return mContentLength;
    }

    public String getFirstHeader(final ResponseHeader responseHeader) {
        for (final Header header : mHeaders) {
            if (header.getName().equalsIgnoreCase(responseHeader.getKey())) {
                return header.getValue();
            }
        }
        return null;
    }
}
