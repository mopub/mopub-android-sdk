package com.mopub.mobileads;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.mopub.common.logging.MoPubLog;

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
                    MoPubLog.d("View's onUserClick() is not registered.");
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
