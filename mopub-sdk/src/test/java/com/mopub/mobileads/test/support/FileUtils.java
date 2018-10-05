// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import com.mopub.common.util.Streams;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

// note: keep this in test/support folder. this is not intended to be of Utility usage
public class FileUtils {
    public static void copyFile(String sourceFile, String destinationFile) {
        try {
            Streams.copyContent(new FileInputStream(sourceFile), new FileOutputStream(destinationFile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeBytesToFile(byte[] sourceBytes, String destinationFile) {
        try {
            Streams.copyContent(new ByteArrayInputStream(sourceBytes), new FileOutputStream(destinationFile));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
