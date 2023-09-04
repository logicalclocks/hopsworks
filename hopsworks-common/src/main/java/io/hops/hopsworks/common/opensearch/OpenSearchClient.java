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
package io.hops.hopsworks.common.opensearch;

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import com.logicalclocks.servicediscoverclient.service.Service;
import io.hops.hopsworks.common.hosts.ServiceDiscoveryController;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.servicediscovery.HopsworksService;
import io.hops.hopsworks.servicediscovery.tags.OpenSearchTags;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.ssl.SSLContexts;
import org.opensearch.action.admin.indices.cache.clear.ClearIndicesCacheRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@DependsOn("Settings")
public class OpenSearchClient {

  @EJB
  private Settings settings;
  @EJB
  private BaseHadoopClientsService clientsService;
  @EJB
  private ServiceDiscoveryController serviceDiscoveryController;

  private static final Logger LOG = Logger.getLogger(OpenSearchClient.class.getName());

  private RestHighLevelClient elasticClient = null;

  @PostConstruct
  private void init() {
    try {
      getClient();
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  @PreDestroy
  private void close() {
    try {
      shutdownClient();
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, null, ex);
    }
  }

  public synchronized void resetClient() {
    elasticClient = null;
  }

  public synchronized RestHighLevelClient getClient() throws OpenSearchException, ServiceDiscoveryException {
    if (elasticClient == null) {
      Service elasticService = serviceDiscoveryController.getAnyAddressOfServiceWithDNS(
              HopsworksService.OPENSEARCH.getNameWithTag(OpenSearchTags.rest));
      HttpHost elasticAddr = new HttpHost(
          elasticService.getName(),
          elasticService.getPort(),
          settings.isOpenSearchHTTPSEnabled() ? "https" : "http");

      final boolean isSecurityEnabled =
        settings.isOpenSearchSecurityEnabled();

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
          throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.OPENSEARCH_CONNECTION_ERROR,
            Level.INFO, "Error while setting up connections to " +
            "opensearch", e.getMessage(), e);
        }

        credentialsProvider =
          new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
          new UsernamePasswordCredentials(settings.getOpenSearchAdminUser(),
            settings.getOpenSearchAdminPassword()));
      }

      final SSLContext finalSslCtx = sslCtx;
      final CredentialsProvider finalCredentialsProvider = credentialsProvider;

      elasticClient = new RestHighLevelClient(
        RestClient.builder(elasticAddr)
          .setHttpClientConfigCallback(httpAsyncClientBuilder -> {
            httpAsyncClientBuilder.setDefaultIOReactorConfig(
              IOReactorConfig.custom().setIoThreadCount(Settings.OPENSEARCH_KIBANA_NO_CONNECTIONS).build());
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

  private void shutdownClient() throws OpenSearchException {
    if (elasticClient != null) {
      try {
        elasticClient.indices().clearCache(new ClearIndicesCacheRequest(
          Settings.META_INDEX), RequestOptions.DEFAULT);
        elasticClient.close();
        elasticClient = null;
      } catch (IOException e) {
        throw new OpenSearchException(RESTCodes.OpenSearchErrorCode.OPENSEARCH_INTERNAL_REQ_ERROR,
          Level.INFO, "Error while shuting down client", e.getMessage(), e);
      }
    }
  }
}
