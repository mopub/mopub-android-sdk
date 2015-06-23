package com.mopub.network;

import android.net.SSLCertificateSocketFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * An {@link javax.net.ssl.SSLSocketFactory} that supports TLS settings for the MoPub ad servers.
 */
public class CustomSSLSocketFactory extends SSLSocketFactory {

    private SSLSocketFactory mCertificateSocketFactory;

    private CustomSSLSocketFactory() {}

    public static CustomSSLSocketFactory getDefault(final int handshakeTimeoutMillis) {
        CustomSSLSocketFactory factory = new CustomSSLSocketFactory();
        factory.mCertificateSocketFactory = SSLCertificateSocketFactory.getDefault(handshakeTimeoutMillis, null);

        return factory;
    }

    // Forward all methods. Enable TLS 1.1 and 1.2 before returning.

    // SocketFactory overrides
    @Override
    public Socket createSocket() throws IOException {
        final Socket socket = mCertificateSocketFactory.createSocket();
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final String host, final int i) throws IOException, UnknownHostException {
        final Socket socket = mCertificateSocketFactory.createSocket(host, i);
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final String host, final int port, final InetAddress localhost, final int localPort) throws IOException, UnknownHostException {
        final Socket socket = mCertificateSocketFactory.createSocket(host, port, localhost, localPort);
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final InetAddress address, final int port) throws IOException {
        final Socket socket = mCertificateSocketFactory.createSocket(address, port);
        enableTlsIfAvailable(socket);
        return socket;
    }

    @Override
    public Socket createSocket(final InetAddress address, final int port, final InetAddress localhost, final int localPort) throws IOException {
        final Socket socket = mCertificateSocketFactory.createSocket(address, port, localhost, localPort);
        enableTlsIfAvailable(socket);
        return socket;
    }

    // SSLSocketFactory overrides

    @Override
    public String[] getDefaultCipherSuites() {
        return mCertificateSocketFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return mCertificateSocketFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(final Socket socketParam, final String host, final int port, final boolean autoClose) throws IOException {
        Socket socket = mCertificateSocketFactory.createSocket(socketParam, host, port, autoClose);
        enableTlsIfAvailable(socket);
        return socket;
    }

    private void enableTlsIfAvailable(Socket socket) {
        if (socket instanceof SSLSocket) {
            SSLSocket sslSocket = (SSLSocket) socket;
            String[] supportedProtocols = sslSocket.getSupportedProtocols();
            // Make sure all supported protocols are enabled. Android does not enable TLSv1.1 or
            // TLSv1.2 by default.
            sslSocket.setEnabledProtocols(supportedProtocols);
        }
    }
}
