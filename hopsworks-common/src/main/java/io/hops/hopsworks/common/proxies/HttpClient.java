/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.proxies;

import io.hops.hopsworks.common.util.Settings;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ResponseHandler;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class HttpClient {
  
  private static final String AUTH_HEADER_CONTENT = "Bearer %s";
  
  @EJB
  private Settings settings;
  
  private PoolingHttpClientConnectionManager connectionManager;
  private CloseableHttpClient client;
  private HttpHost host;
  
  @PostConstruct
  public void init() throws RuntimeException {
    try {
      connectionManager = createConnectionManager();
      client = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .build();
      host = HttpHost.create(settings.getRestEndpoint());
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  @PreDestroy
  public void destroy() {
    if (connectionManager != null) {
      connectionManager.shutdown();
    }
  }
  
  private Registry<ConnectionSocketFactory> createConnectionFactory() throws IOException, GeneralSecurityException {
    Path trustStore = Paths.get(settings.getHopsworksDomainDir(), "config", "cacerts.jks");
    char[] trustStorePassword = settings.getHopsworksMasterPasswordSsl().toCharArray();
    SSLContext sslCtx = SSLContexts.custom()
        .loadTrustMaterial(trustStore.toFile(), trustStorePassword,
            new TrustSelfSignedStrategy())
        .build();
    SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslCtx, NoopHostnameVerifier.INSTANCE);
    return RegistryBuilder.<ConnectionSocketFactory>create()
        .register("https", sslsf)
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .build();
  }
  
  private PoolingHttpClientConnectionManager createConnectionManager() throws IOException, GeneralSecurityException {
    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager(createConnectionFactory());
    connectionManager.setDefaultMaxPerRoute(10);
    return connectionManager;
  }
  
  public void setAuthorizationHeader(HttpRequest httpRequest, String token) {
    httpRequest.setHeader(HttpHeaders.AUTHORIZATION, String.format(AUTH_HEADER_CONTENT, token));
  }
  
  public <T> T execute(HttpRequest request, ResponseHandler<T> handler) throws IOException {
    return client.execute(host, request, handler);
  }
}
