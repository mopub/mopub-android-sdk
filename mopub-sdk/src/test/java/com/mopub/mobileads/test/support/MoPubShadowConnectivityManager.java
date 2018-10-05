// Copyright 2018 Twitter, Inc.
// Licensed under the MoPub SDK License Agreement
// http://www.mopub.com/legal/sdk-license-agreement/

package com.mopub.mobileads.test.support;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import org.robolectric.Shadows;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowNetwork;

import static android.net.ConnectivityManager.TYPE_ETHERNET;
import static android.net.ConnectivityManager.TYPE_WIFI;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Implements(ConnectivityManager.class)
public class MoPubShadowConnectivityManager extends ShadowConnectivityManager {

    private final NetworkCapabilities capabilitiesEthernet;

    public MoPubShadowConnectivityManager() {
        super();
        capabilitiesEthernet = mock(NetworkCapabilities.class);
        when(capabilitiesEthernet.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(true);
    }

    @Implementation(minSdk = LOLLIPOP)
    public NetworkCapabilities getNetworkCapabilities(Network network) {
        ShadowNetwork shadowNetwork = Shadows.shadowOf(network);
        if (shadowNetwork.getNetId() == TYPE_ETHERNET) {
            return capabilitiesEthernet;
        }
        return null;
    }
}
