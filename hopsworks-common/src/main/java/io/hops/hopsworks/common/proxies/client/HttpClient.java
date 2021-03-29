/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.proxies.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.util.Settings;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
import org.apache.http.util.EntityUtils;

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
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class HttpClient {
  
  private static final String AUTH_HEADER_CONTENT = "Bearer %s";
  
  @EJB
  private Settings settings;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;
  
  private PoolingHttpClientConnectionManager connectionManager;
  private CloseableHttpClient client;
  private HttpHost host;
  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() throws RuntimeException {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

    try {
      connectionManager = createConnectionManager();
      client = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .setKeepAliveStrategy((httpResponse, httpContext) -> settings.getConnectionKeepAliveTimeout() * 1000)
          .build();
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

  public static class ObjectResponseHandler<T> implements ResponseHandler<T> {

    private Class<T> cls;
    private ObjectMapper objectMapper;

    public ObjectResponseHandler(Class<T> cls, ObjectMapper objectMapper) {
      this.cls = cls;
      this.objectMapper = objectMapper;
    }

    @Override
    public T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      String responseJson = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      if (response.getStatusLine().getStatusCode() / 100 == 2) {
        return objectMapper.readValue(responseJson, cls);
      } else if (response.getStatusLine().getStatusCode() / 100 == 5) {
        throw new IOException(responseJson);
      } else {
        throw new NotRetryableClientProtocolException(responseJson);
      }
    }
  }

  public static class StringResponseHandler implements ResponseHandler<String> {
    @Override
    public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      String responseJson = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      if (response.getStatusLine().getStatusCode() / 100 == 2) {
        return responseJson;
      } else if (response.getStatusLine().getStatusCode() / 100 == 5) {
        throw new IOException(responseJson);
      } else {
        throw new NotRetryableClientProtocolException(responseJson);
      }
    }
  }

  public static class NoBodyResponseHandler<T> implements ResponseHandler<T> {

    public NoBodyResponseHandler() {
    }

    @Override
    public T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      String responseJson = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      if (response.getStatusLine().getStatusCode() / 100 == 5) {
        throw new IOException(responseJson);
      } else if (response.getStatusLine().getStatusCode() / 100 == 4) {
        throw new NotRetryableClientProtocolException(responseJson);
      }
      return null;
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
    connectionManager.setMaxTotal(10);
    connectionManager.setDefaultMaxPerRoute(10);
    return connectionManager;
  }
  
  public void setAuthorizationHeader(HttpRequest httpRequest) {
    httpRequest.setHeader(HttpHeaders.AUTHORIZATION,
        String.format(AUTH_HEADER_CONTENT, settings.getServiceMasterJWT()));
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }
  
  public <T> T execute(HttpRequest request, ResponseHandler<T> handler) throws IOException {
    if(host==null){
      try {
        Service hopsworksService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
            ServiceDiscoveryController.HopsworksService.HOPSWORKS_APP);
        host = new HttpHost(hopsworksService.getName(), hopsworksService.getPort(), "HTTPS");
      } catch (ServiceDiscoveryException ex) {
        throw new IOException(ex);
      }
    }
    return client.execute(host, request, handler);
  }
  
  public <T> T execute(HttpHost host, HttpRequest request, ResponseHandler<T> handler) throws IOException {
    return client.execute(host, request, handler);
  }
}
