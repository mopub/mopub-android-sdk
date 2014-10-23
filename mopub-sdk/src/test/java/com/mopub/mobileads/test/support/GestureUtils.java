package com.mopub.mobileads.test.support;

import android.view.MotionEvent;

public class GestureUtils {
    public static MotionEvent createActionMove(float x, float y) {
        return MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, x, y, 0);
    }

    public static MotionEvent createActionDown(float x, float y) {
        return MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0);
    }

    public static MotionEvent createActionUp(float x, float y) {
        return MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, x, y, 0);
    }
}
