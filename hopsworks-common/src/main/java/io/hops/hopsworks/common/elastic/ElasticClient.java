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
package io.hops.hopsworks.common.elastic;

import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Ip;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@DependsOn("Settings")
public class ElasticClient {
  
  @EJB
  private Settings settings;
  @EJB
  private BaseHadoopClientsService clientsService;
  
  private static final Logger LOG =
      Logger.getLogger(ElasticClient.class.getName());
  
  private RestHighLevelClient elasticClient = null;
  
  @PostConstruct
  private void init() {
    try {
      getClient();
    } catch (ElasticException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }
  
  @PreDestroy
  private void close() {
    try {
      shutdownClient();
    } catch (ElasticException ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }
  
  
  public synchronized RestHighLevelClient getClient() throws ElasticException {
    if (elasticClient == null) {
      HttpHost[] elasticAddrs = getElasticIps();
      final boolean isSecurityEnabled =
          settings.isElasticOpenDistroSecurityEnabled();
      
      SSLContext sslCtx = null;
      CredentialsProvider credentialsProvider = null;
      if (isSecurityEnabled) {
        Path trustStore = Paths
            .get(clientsService.getSuperTrustStorePath());
        char[] trustStorePassword =
            clientsService.getSuperTrustStorePassword().toCharArray();
        try {
          sslCtx = SSLContexts.custom()
              .loadTrustMaterial(trustStore.toFile(), trustStorePassword)
              .build();
        } catch (GeneralSecurityException | IOException e) {
          throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_CONNECTION_ERROR,
              Level.INFO, "Error while setting up connections to " +
              "elasticsearch", e.getMessage(), e);
        }
        
        credentialsProvider =
            new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
            new UsernamePasswordCredentials(settings.getElasticAdminUser(),
                settings.getElasticAdminPassword()));
      }
      
      final SSLContext finalSslCtx = sslCtx;
      final CredentialsProvider finalCredentialsProvider = credentialsProvider;
  
      elasticClient = new RestHighLevelClient(
          RestClient.builder(elasticAddrs)
              .setHttpClientConfigCallback(httpAsyncClientBuilder -> {
                httpAsyncClientBuilder.setDefaultIOReactorConfig(
                    IOReactorConfig.custom().setIoThreadCount(Settings.ELASTIC_KIBANA_NO_CONNECTIONS).build());
                if (isSecurityEnabled) {
                  return httpAsyncClientBuilder.setSSLContext(finalSslCtx)
                      .setDefaultCredentialsProvider(
                          finalCredentialsProvider)
                      .setSSLHostnameVerifier(
                          NoopHostnameVerifier.INSTANCE);
                }
                return httpAsyncClientBuilder;
              }));
    }
    return elasticClient;
  }
  
  private HttpHost[] getElasticIps() throws ElasticException {
    boolean isHTTPS = settings.isElasticHTTPSEnabled();
    List<String> addrs = settings.getElasticIps();
    HttpHost[] hosts = new HttpHost[addrs.size()];
    int index = 0;
    for (String addr : addrs) {
      // Validate the ip address pulled from the variables
      if (!Ip.validIp(addr)) {
        try {
          InetAddress.getByName(addr);
        } catch (UnknownHostException e) {
          throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_CONNECTION_ERROR,
              Level.INFO,  "Error while parsing elasticsearch ips", e.getMessage(), e);
        }
      }
      hosts[index] = new HttpHost(addr,
          settings.getElasticRESTPort(),
          isHTTPS ? "https" : "http");
      index++;
    }
    
    return hosts;
  }
  
  public void shutdownClient() throws ElasticException {
    if (elasticClient != null) {
      try {
        elasticClient.indices().clearCache(new ClearIndicesCacheRequest(
            Settings.META_INDEX), RequestOptions.DEFAULT);
        elasticClient.close();
        elasticClient = null;
      } catch (IOException e) {
        throw new ElasticException(RESTCodes.ElasticErrorCode.ELASTIC_INTERNAL_REQ_ERROR,
            Level.INFO,  "Error while shuting down client", e.getMessage(), e);
      }
    }
  }
}
