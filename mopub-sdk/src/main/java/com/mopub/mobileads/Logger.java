package com.mopub.mobileads;

/**
 * Part of the Scopely™ Platform
 * © 2013 Scopely, Inc.
 */
public interface Logger {
    public int v(String tag, String msg);

    public int v(String tag, String msg, Throwable tr);

    public int d(String tag, String msg);

    public int d(String tag, String msg, Throwable tr);

    public int i(String tag, String msg);

    public int i(String tag, String msg, Throwable tr);

    public int w(String tag, String msg);

    public int w(String tag, String msg, Throwable tr);

    public int w(String tag, Throwable tr);

    public int e(String tag, String msg);

    public int e(String tag, String msg, Throwable tr);

    public int wtf(String tag, String msg);

    public int wtf(String tag, Throwable tr);

    public int wtf(String tag, String msg, Throwable tr);
}
