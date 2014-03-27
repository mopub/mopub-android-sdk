/*
 * Copyright (c) 2010-2013, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *  Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 *  Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class ViewGestureDetector extends GestureDetector {
    private final View mView;

    interface UserClickListener {
        void onUserClick();
        void onResetUserClick();
        boolean wasClicked();
    }

    private AdAlertGestureListener mAdAlertGestureListener;
    private UserClickListener mUserClickListener;

    public ViewGestureDetector(Context context, View view, AdConfiguration adConfiguration)  {
        this(context, view, new AdAlertGestureListener(view, adConfiguration));
    }

    private ViewGestureDetector(Context context, View view, AdAlertGestureListener adAlertGestureListener) {
        super(context, adAlertGestureListener);

        mAdAlertGestureListener = adAlertGestureListener;
        mView = view;

        setIsLongpressEnabled(false);
    }

    void sendTouchEvent(MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_UP:
                if (mUserClickListener != null) {
                    mUserClickListener.onUserClick();
                } else {
                    Log.d("MoPub", "View's onUserClick() is not registered.");
                }
                mAdAlertGestureListener.finishGestureDetection();
                break;

            case MotionEvent.ACTION_DOWN:
                onTouchEvent(motionEvent);
                break;

            case MotionEvent.ACTION_MOVE:
                if (isMotionEventInView(motionEvent, mView)) {
                    onTouchEvent(motionEvent);
                } else {
                    resetAdFlaggingGesture();
                }
                break;

            default:
                break;
        }
    }

    void setUserClickListener(UserClickListener listener) {
        mUserClickListener = listener;
    }

    void resetAdFlaggingGesture() {
        mAdAlertGestureListener.reset();
    }

    private boolean isMotionEventInView(MotionEvent motionEvent, View view) {
        if (motionEvent == null || view == null) {
            return false;
        }

        float x = motionEvent.getX();
        float y = motionEvent.getY();

        return (x >= 0 && x <= view.getWidth())
                && (y >= 0 && y <= view.getHeight());
    }

    @Deprecated // for testing
    void setAdAlertGestureListener(AdAlertGestureListener adAlertGestureListener) {
        mAdAlertGestureListener = adAlertGestureListener;
    }
}
