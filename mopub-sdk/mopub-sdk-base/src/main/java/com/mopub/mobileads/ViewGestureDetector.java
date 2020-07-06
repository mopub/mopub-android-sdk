// Copyright 2018-2020 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads;

import android.content.Context;
import androidx.annotation.NonNull;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.mopub.common.VisibleForTesting;

public class ViewGestureDetector extends GestureDetector {
    @NonNull
    private GestureListener mGestureListener;

    public ViewGestureDetector(@NonNull Context context)  {
        this(context, new GestureListener());
    }

    private ViewGestureDetector(Context context, @NonNull GestureListener gestureListener) {
        super(context, gestureListener);

        mGestureListener = gestureListener;

        setIsLongpressEnabled(false);
    }

    void onResetUserClick() {
        mGestureListener.onResetUserClick();
    }

    public boolean isClicked() {
        return mGestureListener.isClicked();
    }

    @Deprecated
    @VisibleForTesting
    void setGestureListener(@NonNull GestureListener gestureListener) {
        mGestureListener = gestureListener;
    }

    @VisibleForTesting
    public void setClicked(boolean clicked) {
        mGestureListener.mIsClicked = clicked;
    }

    /**
     * Track user interaction in a separate class
     */
    static class GestureListener extends GestureDetector.SimpleOnGestureListener {
        boolean mIsClicked = false;

        void onResetUserClick() {
            mIsClicked = false;
        }

        boolean isClicked() {
            return mIsClicked;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            mIsClicked = true;
            return super.onSingleTapUp(e);
        }
    }
}
