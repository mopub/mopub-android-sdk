package com.mopub.common.util;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

public class ImageUtils {

    /**
     * Fast Gaussian blurring algorithm source:
     * https://github.com/patrickfav/BlurTestAndroid/blob/master/BlurBenchmark/src/main/java/at/favre/app/blurbenchmark/blur/algorithms/GaussianFastBlur.java
     *
     */
    @NonNull
    public static Bitmap applyFastGaussianBlurToBitmap(@NonNull Bitmap mutableBitmap, int radius) {
        int w = mutableBitmap.getWidth();
        int h = mutableBitmap.getHeight();
        int[] pixels = new int[w * h];
        mutableBitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int r = radius; r >= 1; r /= 2) {
            for (int i = r; i < h - r; i++) {
                for (int j = r; j < w - r; j++) {
                    int tl = pixels[(i - r) * w + j - r];
                    int tr = pixels[(i - r) * w + j + r];
                    int tc = pixels[(i - r) * w + j];
                    int bl = pixels[(i + r) * w + j - r];
                    int br = pixels[(i + r) * w + j + r];
                    int bc = pixels[(i + r) * w + j];
                    int cl = pixels[i * w + j - r];
                    int cr = pixels[i * w + j + r];

                    pixels[(i * w) + j] = 0xFF000000 |
                            (((tl & 0xFF) + (tr & 0xFF) + (tc & 0xFF) + (bl & 0xFF) + (br & 0xFF) + (bc & 0xFF) + (cl & 0xFF) + (cr & 0xFF)) >> 3) & 0xFF |
                            (((tl & 0xFF00) + (tr & 0xFF00) + (tc & 0xFF00) + (bl & 0xFF00) + (br & 0xFF00) + (bc & 0xFF00) + (cl & 0xFF00) + (cr & 0xFF00)) >> 3) & 0xFF00 |
                            (((tl & 0xFF0000) + (tr & 0xFF0000) + (tc & 0xFF0000) + (bl & 0xFF0000) + (br & 0xFF0000) + (bc & 0xFF0000) + (cl & 0xFF0000) + (cr & 0xFF0000)) >> 3) & 0xFF0000;
                }
            }
        }

        mutableBitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return mutableBitmap;
    }
}
