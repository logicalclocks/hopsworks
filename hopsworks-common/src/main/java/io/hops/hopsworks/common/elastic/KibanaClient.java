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

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.proxies.client.HttpRetryableAction;
import io.hops.hopsworks.common.proxies.client.NotFoundClientProtocolException;
import io.hops.hopsworks.common.proxies.client.NotRetryableClientProtocolException;
import io.hops.hopsworks.common.security.BaseHadoopClientsService;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.directory.api.util.Strings;
import org.apache.hadoop.util.ExponentialBackOff;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

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
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.logging.Level;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@DependsOn("Settings")
public class KibanaClient {
  @EJB
  private Settings settings;
  @EJB
  private BaseHadoopClientsService clientsService;
  @EJB
  private ElasticJWTController elasticJWTController;
  
  private PoolingHttpClientConnectionManager connectionManager;
  private CloseableHttpClient client;
  private ExponentialBackOff.Builder backOffPolicy;

  public enum KibanaType{
    Visualization("visualization"),
    Search("search"),
    Dashboard("dashboard"),
    IndexPattern("index-pattern"),
    All ("saved_objects");
    
    private String str;
    KibanaType(String str){
      this.str = str;
    }
  
    @Override
    public String toString() {
      return str;
    }
  }
  
  private enum HttpMethod{
    GET,
    POST,
    DELETE
  }

  @PostConstruct
  public void init() throws RuntimeException {
    try {
      connectionManager = createConnectionManager();
      client = HttpClients.custom()
          .setConnectionManager(connectionManager)
          .build();
    } catch (IOException | GeneralSecurityException ex) {
      throw new RuntimeException(ex);
    }
    backOffPolicy = new ExponentialBackOff.Builder()
        .setMaximumRetries(3)
        .setInitialIntervalMillis(500)
        .setMaximumIntervalMillis(3000)
        .setMultiplier(1.5);
  }
  
  @PreDestroy
  public void destroy() {
    if (connectionManager != null) {
      connectionManager.shutdown();
    }
  }
  
  private Registry<ConnectionSocketFactory> createConnectionFactory()
      throws IOException, GeneralSecurityException {
    SSLContext sslCtx = null;
    final boolean isSecurityEnabled =
        settings.isElasticOpenDistroSecurityEnabled();
    if (isSecurityEnabled) {
      Path trustStore = Paths
          .get(clientsService.getSuperTrustStorePath());
      char[] trustStorePassword =
          clientsService.getSuperTrustStorePassword().toCharArray();
      sslCtx = SSLContexts.custom()
          .loadTrustMaterial(trustStore.toFile(), trustStorePassword)
          .build();
    }
    
    SSLConnectionSocketFactory
        sslsf =
        new SSLConnectionSocketFactory(sslCtx, NoopHostnameVerifier.INSTANCE);
    return RegistryBuilder.<ConnectionSocketFactory>create()
        .register("https", sslsf)
        .register("http", PlainConnectionSocketFactory.getSocketFactory())
        .build();
  }
  
  private PoolingHttpClientConnectionManager createConnectionManager() throws IOException, GeneralSecurityException {
    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager(createConnectionFactory());
    connectionManager.setMaxTotal(settings.ELASTIC_KIBANA_NO_CONNECTIONS);
    connectionManager.setDefaultMaxPerRoute(settings.ELASTIC_KIBANA_NO_CONNECTIONS);
    return connectionManager;
  }
  
  public JSONObject createIndexPattern(Users user, Project project,
      KibanaType type, String id) throws ElasticException {
    return post(user, project, type, id,
        "{\"attributes\": {\"title\": \"" + id + "\"}}");
  }
  
  public JSONObject get(Users user, Project project, KibanaType type, String id)
      throws ElasticException {
    return execute(HttpMethod.GET, type, id, null, user, project);
  }
  
  public JSONObject delete(Users user, Project project, KibanaType type, String id)
      throws ElasticException {
    return execute(HttpMethod.DELETE, type, id, null, user, project);
  }
  
  public JSONObject deleteAsDataOwner(Project project,
      KibanaType type, String id) throws ElasticException {
    return execute(HttpMethod.DELETE, type, id, null, null, project, false,
        true);
  }
  
  public JSONObject post(Users user, Project project, KibanaType type, String id, String data)
      throws ElasticException {
    return execute(HttpMethod.POST, type, id, data, user, project);
  }
  
  public JSONObject postWithOverwrite(Users user, Project project,
      KibanaType type, String id, String data) throws ElasticException {
    return execute(HttpMethod.POST, type, id, data, user, project, true, false);
  }
  
  private JSONObject execute(HttpMethod method, KibanaType type, String id,
      String data, Users user, Project project) throws ElasticException {
    return execute(method, type, id, data, user, project, false, false);
  }

  private HttpUriRequest buildHttpRequest(HttpMethod method, String url, String data) throws ElasticException {
    HttpUriRequest httpRequest = null;
    switch (method) {
      case GET:
        httpRequest = new HttpGet(url);
        break;
      case DELETE:
        httpRequest = new HttpDelete(url);
        break;
      case POST:
        httpRequest = new HttpPost(url);
        if (data != null) {
          try {
            ((HttpPost) httpRequest).setEntity(new StringEntity(data));
          } catch (UnsupportedEncodingException e) {
            throw new ElasticException(RESTCodes.ElasticErrorCode.KIBANA_REQ_ERROR,
                Level.INFO, "Failed execute a Kibana request on " + url,
                e.getMessage(), e);
          }
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported method " + method);
    }
    return httpRequest;
  }
  
  private JSONObject execute(HttpMethod method, KibanaType type, String id,
      String data, Users user, Project project,
      boolean overwrite, boolean runAsDataOwner) throws ElasticException {
    
    String url = settings.getKibanaUri() + "/api/saved_objects";
    if (type != KibanaType.All) {
      url += "/" + type.toString();
    }
    
    if (id != null) {
      url += "/" + id;
    }
    
    if (overwrite){
      url+="?overwrite=true";
    }
    
    try {
      final HttpUriRequest httpRequest = buildHttpRequest(method, url, data);

      httpRequest.setHeader("kbn-xsrf", "required");
      httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      
      // authorization
      if (settings.isElasticOpenDistroSecurityEnabled()) {
        if (settings.isElasticJWTEnabled() && project != null && (user != null || runAsDataOwner)) {
          String token = runAsDataOwner ?
              elasticJWTController.createTokenForELKAsDataOwner(project) :
              elasticJWTController.createTokenForELK(user, project);
          httpRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        } else {
          String userPass =
              settings.getElasticAdminUser() + ":" +
                  settings.getElasticAdminPassword();
          httpRequest.setHeader(HttpHeaders.AUTHORIZATION,
              "Basic " +
                  Base64.getEncoder().encodeToString(userPass.getBytes()));
        }
      }

      HttpRetryableAction<JSONObject> retryableAction = new HttpRetryableAction<JSONObject>(backOffPolicy) {
        @Override
        public JSONObject performAction() throws ClientProtocolException, IOException {
          return client.execute(httpRequest, httpResponse -> {
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            if (statusCode / 100 == 2) {
              String response = EntityUtils.toString(httpResponse.getEntity());
              return Strings.isEmpty(response) ? new JSONObject() : new JSONObject(response);
            } else if (statusCode / 100 == 4) {
              if(statusCode == 404){
                throw new NotFoundClientProtocolException(httpResponse.toString());
              }else{
                throw new NotRetryableClientProtocolException(httpResponse.toString());
              }
            } else {
              // Retry
              throw new ClientProtocolException();
            }
          });
        }
      };
      return retryableAction.tryAction();
    } catch (IOException e) {
      throw new ElasticException(RESTCodes.ElasticErrorCode.KIBANA_REQ_ERROR,
          Level.INFO, "Failed execute a Kibana request on " + url,
          e.getMessage(), e);
    }
  }
  
}
