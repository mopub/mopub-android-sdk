package com.mopub.nativeads;

import android.support.annotation.Nullable;

/**
 * Stores an integer interval in the form of a start and a length.
 */
public class IntInterval implements Comparable<IntInterval>{
    private int start;
    private int length;

    public IntInterval(int start, int length) {
        this.start = start;
        this.length = length;
    }

    public int getStart() {
        return start;
    }

    public int getLength() {
        return length;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /**
     * For comparing intervals directly to this object.
     * @param start The start of the interval
     * @param length The length of the interval
     * @return Whether or not the given interval and this object have the same values.
     */
    public boolean equals(int start, int length) {
        return this.start == start && this.length == length;
    }

    @Override
    public String toString() {
        return "{start : " + start + ", length : " + length + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IntInterval)) {
            return false;
        }

        final IntInterval other = (IntInterval) o;
        return this.start == other.start && this.length == other.length;
    }

    @Override
    public int hashCode() {
        int result = 29;
        result = 31 * result + start;
        result = 31 * result + length;
        return result;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public int compareTo(@Nullable final IntInterval another) {
        if (start == another.start) {
            return length - another.length;
        }
        return start - another.start;
    }
}
