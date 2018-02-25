package com.mopub.network;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.net.SSLCertificateSocketFactory;
import android.os.Build;
import android.support.annotation.Nullable;

import com.mopub.TestSdkHelper;
import com.mopub.mobileads.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.robolectric.annotation.Config;

import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest(InetAddressUtils.class)
@RunWith(PowerMockRunner.class)
@Config(constants = BuildConfig.class)
public class CustomSSLSocketFactoryTest {

    private CustomSSLSocketFactory subject;
    private SSLCertificateSocketFactory mockSSLCertificateSocketFactory;
    private SSLSocketWithSetHostname mockSSLSocket;
    private int previousSdkVersion;

    @SuppressLint("SSLCertificateSocketFactoryCreateSocket")
    @Before
    public void setUp() throws Exception {
        mockStatic(InetAddressUtils.class);
        mockSSLCertificateSocketFactory = mock(SSLCertificateSocketFactory.class);
        mockSSLSocket = mock(SSLSocketWithSetHostname.class);
        Mockito.when(mockSSLCertificateSocketFactory.createSocket(any(InetAddress.class),
                anyInt())).thenReturn(mockSSLSocket);
        InetAddress mockInetAddress = mock(InetAddress.class);
        PowerMockito.when(InetAddressUtils.getInetAddressByName(anyString())).thenReturn(
                mockInetAddress);
        subject = CustomSSLSocketFactory.getDefault(0);
        subject.setCertificateSocketFactory(mockSSLCertificateSocketFactory);
        previousSdkVersion = Build.VERSION.SDK_INT;
    }

    @After
    public void tearDown() {
        TestSdkHelper.setReportedSdkLevel(previousSdkVersion);
    }

    @Test
    public void createSocket_withSocketParam_withAutoCloseTrue_shouldCloseOriginalSocket() throws Exception {
        final Socket mockSocket = mock(Socket.class);
        final HostnameVerifier mockHostnameVerifier = mock(HostnameVerifier.class);
        Mockito.when(mockHostnameVerifier.verify(eq("hostname"), any(SSLSession.class))).thenReturn(true);
        HttpsURLConnection.setDefaultHostnameVerifier(mockHostnameVerifier);

        subject.createSocket(mockSocket, "hostname", 443, true);

        verify(mockSocket).close();
        verify(mockSSLSocket).getSupportedProtocols();
        verify(mockSSLSocket).setEnabledProtocols(any(String[].class));
        verify(mockSSLSocket).startHandshake();
        verify(mockSSLSocket).getSession();
        verify(mockSSLSocket).setHostname(any(String.class));
        verifyNoMoreInteractions(mockSocket);
    }

    @Test
    public void createSocket_withSocketParam_withAutoCloseFalse_shouldNotCloseOriginalSocket_shouldCallSetHostname() throws Exception {
        final Socket mockSocket = mock(Socket.class);
        final HostnameVerifier mockHostnameVerifier = mock(HostnameVerifier.class);
        Mockito.when(mockHostnameVerifier.verify(eq("hostname"), any(SSLSession.class))).thenReturn(true);
        HttpsURLConnection.setDefaultHostnameVerifier(mockHostnameVerifier);

        subject.createSocket(mockSocket, "hostname", 443, false);

        verify(mockSocket, never()).close();
        verify(mockSSLSocket).getSupportedProtocols();
        verify(mockSSLSocket).setEnabledProtocols(any(String[].class));
        verify(mockSSLSocket).startHandshake();
        verify(mockSSLSocket).getSession();
        verify(mockSSLSocket).setHostname(any(String.class));
        verifyNoMoreInteractions(mockSSLSocket);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void setHostnameOnSocket_withAtLeastJellyBeanMR1_shouldEnableServerNameIdentification() {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.JELLY_BEAN_MR1);

        final SSLSocket mockSSLSocket = mock(SSLSocket.class);

        CustomSSLSocketFactory.setHostnameOnSocket(mockSSLCertificateSocketFactory, mockSSLSocket,
                "hostname");

        verify(mockSSLCertificateSocketFactory).setHostname(mockSSLSocket, "hostname");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Test
    public void setHostnameOnSocket_withBelowJellyBeanMR1_shouldEnableServerNameIdentification() {
        TestSdkHelper.setReportedSdkLevel(Build.VERSION_CODES.JELLY_BEAN);

        final SSLSocket mockSSLSocket = mock(SSLSocket.class);

        CustomSSLSocketFactory.setHostnameOnSocket(mockSSLCertificateSocketFactory, mockSSLSocket,
                "hostname");

        verify(mockSSLCertificateSocketFactory, never()).setHostname(mockSSLSocket, "hostname");
    }

    @Test
    public void verifyServerName_withValidServerNameIdentification_shouldNotThrowSSLHandshakeException() throws Exception {
        final SSLSocket mockSslSocket = mock(SSLSocket.class);
        final HostnameVerifier mockHostnameVerifier = mock(HostnameVerifier.class);
        Mockito.when(mockHostnameVerifier.verify(eq("hostname"), any(SSLSession.class))).thenReturn(true);
        HttpsURLConnection.setDefaultHostnameVerifier(mockHostnameVerifier);

        CustomSSLSocketFactory.verifyServerName(mockSslSocket, "hostname");
    }

    @Test(expected = SSLHandshakeException.class)
    public void verifyServerName_withInvalidServerNameIdentification_shouldThrowSSLHandshakeException() throws Exception {
        final SSLSocket mockSslSocket = mock(SSLSocket.class);
        final HostnameVerifier mockHostnameVerifier = mock(HostnameVerifier.class);
        Mockito.when(mockHostnameVerifier.verify(eq("hostname"), any(SSLSession.class))).thenReturn(false);
        HttpsURLConnection.setDefaultHostnameVerifier(mockHostnameVerifier);

        CustomSSLSocketFactory.verifyServerName(mockSslSocket, "hostname");
    }

    /**
     * This class has the setHostname() method that CustomSSLSocketFactory#setHostnameOnSocket uses
     * via reflection. This exists so we can Mockito.verify the method setHostname().
     */
    private abstract class SSLSocketWithSetHostname extends SSLSocket {
        public void setHostname(@Nullable final String hostname) {
        }
    }
}
