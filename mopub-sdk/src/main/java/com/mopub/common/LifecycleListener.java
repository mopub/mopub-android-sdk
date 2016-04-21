package com.mopub.common;

import android.app.Activity;
import android.support.annotation.NonNull;

/**
 * This is a uniform interface to 3rd party SDKs that need to know when Activity lifecycle events
 * occur.
 */
public interface LifecycleListener {
    void onCreate(@NonNull Activity activity);
    void onStart(@NonNull Activity activity);
    void onPause(@NonNull Activity activity);
    void onResume(@NonNull Activity activity);

    void onRestart(@NonNull Activity activity);
    void onStop(@NonNull Activity activity);
    void onDestroy(@NonNull Activity activity);
    void onBackPressed(@NonNull Activity activity);
}
