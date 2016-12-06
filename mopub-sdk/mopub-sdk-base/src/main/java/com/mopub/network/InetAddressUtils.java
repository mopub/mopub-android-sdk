package com.mopub.network;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This class exists to wrap InetAddress static calls since java.net classes cannot be mocked
 */
public class InetAddressUtils {
    @NonNull
    public static InetAddress getInetAddressByName(@Nullable final String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }

    private InetAddressUtils() {
    }
}
