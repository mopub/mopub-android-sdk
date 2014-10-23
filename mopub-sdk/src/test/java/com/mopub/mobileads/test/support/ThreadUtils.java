package com.mopub.mobileads.test.support;

// note: keep this in test/support folder. this is not intended to be of Utility usage
public class ThreadUtils {
    public static final long NETWORK_DELAY = 500;

    public static void pause(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie){
            // Ignore interrupts on this Thread.
        }
    }
}
