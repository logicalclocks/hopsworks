/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.httpclient;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.HostnameVerifier;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;

public class HttpConnectionManagerBuilder {
  private Path trustStore;
  private char[] trustStorePassword;
  private Path keyStore;
  private char[] keyStorePassword;
  private char[] keyPassword;
  private TrustStrategy trustStrategy;
  private HostnameVerifier hostnameVerifier;

  public HttpConnectionManagerBuilder withTrustStore(Path trustStore, char[] trustStorePassword) {
    this.trustStore = trustStore;
    this.trustStorePassword = trustStorePassword;
    return this;
  }

  public HttpConnectionManagerBuilder withTrustStrategy(TrustStrategy trustStrategy) {
    this.trustStrategy = trustStrategy;
    return this;
  }

  public HttpConnectionManagerBuilder withKeyStore(Path keyStore, char[] keystorePassword, char[] keyPassword) {
    this.keyStore = keyStore;
    this.keyStorePassword = keystorePassword;
    this.keyPassword = keyPassword;
    return this;
  }

  public HttpConnectionManagerBuilder withHostnameVerifier(HostnameVerifier hostnameVerifier) {
    this.hostnameVerifier = hostnameVerifier;
    return this;
  }

  public Registry<ConnectionSocketFactory> build() throws IOException, GeneralSecurityException {
    if (trustStrategy == null) {
      trustStrategy = new TrustSelfSignedStrategy();
    }
    if (hostnameVerifier == null) {
      hostnameVerifier = NoopHostnameVerifier.INSTANCE;
    }

    SSLContextBuilder sslCtxBuilder = SSLContexts.custom();

    if (trustStore != null) {
      sslCtxBuilder.loadTrustMaterial(trustStore.toFile(), trustStorePassword, trustStrategy);
    }
    if (keyStore != null) {
      sslCtxBuilder.loadKeyMaterial(keyStore.toFile(), keyStorePassword, keyPassword);
    }
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslCtxBuilder.build(), hostnameVerifier);
    return RegistryBuilder.<ConnectionSocketFactory>create()
        .register("https", sslsf)
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .build();
  }
}
