package com.mopub.mobileads.test.support;

import com.mopub.common.util.Streams;

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
}
