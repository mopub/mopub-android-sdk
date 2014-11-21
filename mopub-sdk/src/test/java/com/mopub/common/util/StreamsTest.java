package com.mopub.common.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

@RunWith(RobolectricTestRunner.class)
public class StreamsTest {
    @Test
    public void copyStream_shouldCopyContentsOfOneStreamToAnother() throws Exception {
        File inFile = new File("etc/expectedFile.jpg");
        FileInputStream in = new FileInputStream(inFile);
        File tempFile = File.createTempFile("foo", "bar");
        FileOutputStream out = new FileOutputStream(tempFile);

        Streams.copyContent(in, out);

        assertThat(inFile.length()).isEqualTo(tempFile.length());
    }

    @Test
    public void copyStream_withMaxBytes_belowThreshold_shouldCopyContentsOfOneStreamToAnother() throws Exception {
        File inFile = new File("etc/expectedFile.jpg");
        FileInputStream in = new FileInputStream(inFile);
        File tempFile = File.createTempFile("foo", "bar");
        FileOutputStream out = new FileOutputStream(tempFile);

        Streams.copyContent(in, out, 1000000);

        assertThat(inFile.length()).isEqualTo(tempFile.length());
    }

    @Test
    public void copyStream_withMaxBytes_aboveThreshold_shouldThrowIOException() throws Exception {
        InputStream in = new ByteArrayInputStream("this is a pretty long stream".getBytes());

        File tempFile = File.createTempFile("foo", "bar");
        FileOutputStream out = new FileOutputStream(tempFile);

        try {
            Streams.copyContent(in, out, 10);
            fail("Expected IOException.");
        } catch (IOException e) {
            // pass
        }
    }
}
