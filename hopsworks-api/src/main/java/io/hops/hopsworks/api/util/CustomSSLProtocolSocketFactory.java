/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.util;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CustomSSLProtocolSocketFactory implements SecureProtocolSocketFactory {
  private final Logger LOG = Logger.getLogger(CustomSSLProtocolSocketFactory.class.getName());
  
  private final File keyStore;
  private final String keyStorePassword;
  private final String keyPassword;
  private final File trustStore;
  private final String trustStorePassword;
  private SSLContext sslContext;
  
  public CustomSSLProtocolSocketFactory(final File keyStore, final String keyStorePassword, final String keyPassword,
    final File trustStore, final String trustStorePassword) {
    this.keyStore = keyStore;
    this.keyStorePassword = keyStorePassword;
    this.keyPassword = keyPassword;
    this.trustStore = trustStore;
    this.trustStorePassword = trustStorePassword;
  }
  
  @Override
  public Socket createSocket(Socket socket, String host, int port, boolean autoClose)
      throws IOException, UnknownHostException {
    return getSslContext().getSocketFactory().createSocket(socket, host, port, autoClose);
  }
  
  @Override
  public Socket createSocket(String host, int port, InetAddress inetAddress, int clientPort)
      throws IOException, UnknownHostException {
    return getSslContext().getSocketFactory().createSocket(host, port, inetAddress, clientPort);
  }
  
  @Override
  public Socket createSocket(String host, int port, InetAddress localAddress, int localPort,
      HttpConnectionParams httpConnectionParams) throws IOException, UnknownHostException, ConnectTimeoutException {
    if (httpConnectionParams == null) {
      LOG.log(Level.SEVERE, "Creating SSL socket but HTTP connection parameters is null");
      throw new IllegalArgumentException("HTTP connection parameters cannot be null");
    }
    
    Socket socket = getSslContext().getSocketFactory().createSocket();
    SocketAddress localSocketAddress = new InetSocketAddress(localAddress, localPort);
    SocketAddress remoteSocketAddress = new InetSocketAddress(host, port);
    
    socket.setSoTimeout(httpConnectionParams.getSoTimeout());
    if (httpConnectionParams.getLinger() > 0) {
      socket.setSoLinger(true, httpConnectionParams.getLinger());
    } else {
      socket.setSoLinger(false, 0);
    }
    socket.setTcpNoDelay(httpConnectionParams.getTcpNoDelay());
    if (httpConnectionParams.getSendBufferSize() >= 0) {
      socket.setSendBufferSize(httpConnectionParams.getSendBufferSize());
    }
    if (httpConnectionParams.getReceiveBufferSize() >= 0) {
      socket.setReceiveBufferSize(httpConnectionParams.getReceiveBufferSize());
    }
    
    socket.bind(localSocketAddress);
    socket.connect(remoteSocketAddress, httpConnectionParams.getConnectionTimeout());
    return socket;
  }
  
  @Override
  public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
    return getSslContext().getSocketFactory().createSocket(host, port);
  }
  
  private KeyStore createKeystore(final File keyStoreLocation, final String password)
    throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    try (FileInputStream fis = new FileInputStream(keyStoreLocation)) {
      keyStore.load(fis, password != null ? password.toCharArray() : null);
    }
    return keyStore;
  }
  
  private KeyManager[] createKeyManagers(final KeyStore keyStore, final String keyPassword)
    throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
    if (keyStore == null) {
      LOG.log(Level.SEVERE, "Creating SSL socket but key store is null");
      throw new IllegalArgumentException("KeyStore cannot be null");
    }
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, keyPassword != null ? keyPassword.toCharArray() : null);
    
    return kmf.getKeyManagers();
  }
  
  private TrustManager[] createTrustManagers(final KeyStore trustStore)
    throws NoSuchAlgorithmException, KeyStoreException {
    if (trustStore == null) {
      LOG.log(Level.SEVERE, "Creating SSL socket but trust store is null");
      throw new IllegalArgumentException("TrustStore cannot be null");
    }
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(trustStore);
    return tmf.getTrustManagers();
  }
  
  private SSLContext createSSLContext() {
    try {
      KeyStore keyStore = createKeystore(this.keyStore, this.keyStorePassword);
      KeyManager[] keyManagers = createKeyManagers(keyStore, this.keyPassword);
      KeyStore trustStore = createKeystore(this.trustStore, this.trustStorePassword);
      TrustManager[] trustManagers = createTrustManagers(trustStore);
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(keyManagers, trustManagers, new SecureRandom());
      return sslContext;
    } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException |
        UnrecoverableKeyException | KeyManagementException ex) {
      LOG.log(Level.SEVERE, ex.getMessage());
      throw new SSLInitializationError(ex.getMessage());
    }
  }
  
  private SSLContext getSslContext() {
    if (sslContext == null) {
      sslContext = createSSLContext();
    }
    return sslContext;
  }
  
  public class SSLInitializationError extends Error {
    public SSLInitializationError() {
      super();
    }
    
    public SSLInitializationError(String message) {
      super(message);
    }
  }
}
