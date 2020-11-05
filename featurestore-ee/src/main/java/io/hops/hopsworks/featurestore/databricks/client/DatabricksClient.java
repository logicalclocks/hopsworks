/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.featurestore.databricks.client;

import com.damnhandy.uri.template.UriTemplate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.bijection.codec.Base64;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.proxies.client.HttpRetryableAction;
import io.hops.hopsworks.common.proxies.client.NotRetryableClientProtocolException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class DatabricksClient {

  private static final String API_ENDPOINT = "/api/2.0";
  private static final String CLUSTERS_ENDPOINT = API_ENDPOINT + "/clusters";
  private static final String LIST_ENDPOINT = CLUSTERS_ENDPOINT + "/list";
  private static final String EDIT_ENDPOINT = CLUSTERS_ENDPOINT + "/edit";
  private static final String START_ENDPOINT = CLUSTERS_ENDPOINT + "/start";
  private static final String GET_CLUSTER_ENDPOINT = CLUSTERS_ENDPOINT + "/get{?cluster_id}";

  private static final String LIBRARIES_INSTALL = API_ENDPOINT + "/libraries/install";

  private static final String DBFS_PUT = API_ENDPOINT + "/dbfs/put";
  private static final String DBFS_CREATE = API_ENDPOINT + "/dbfs/create";
  private static final String DBFS_CLOSE = API_ENDPOINT + "/dbfs/close";
  private static final String DBFS_ADD_BLOCK = API_ENDPOINT + "/dbfs/add-block";
  private static final String DBFS_STATUS = API_ENDPOINT + "/dbfs/get-status{?path}";

  private static class DatabricksResponseHandler<T> implements ResponseHandler<T> {

    private Class<T> cls;
    private ObjectMapper objectMapper;

    public DatabricksResponseHandler(Class<T> cls, ObjectMapper objectMapper) {
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

  private static class DatabricksNoResponseHandler<T> implements ResponseHandler<T> {

    public DatabricksNoResponseHandler() {
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

  @EJB
  private HttpClient httpClient;

  private ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
  }

  public List<DbCluster> listClusters(String dbInstance, String token) throws FeaturestoreException {
    HttpHost dbInstanceHost = getDbInstanceHost(dbInstance);
    HttpGet listRequest = new HttpGet(LIST_ENDPOINT);
    listRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    return wrapException(new HttpRetryableAction<List<DbCluster>>() {
      @Override
      public List<DbCluster> performAction() throws ClientProtocolException, IOException {
        return httpClient.execute(dbInstanceHost, listRequest,
            new DatabricksResponseHandler<>(DbClusterListResponse.class, objectMapper)).getClusters();
      }
    });
  }

  public DbCluster getCluster(String dbInstance, String clusterId, String token) throws FeaturestoreException {
    HttpHost dbInstanceHost = getDbInstanceHost(dbInstance);
    String uri = UriTemplate.fromTemplate(GET_CLUSTER_ENDPOINT)
        .set("cluster_id", clusterId)
        .expand();
    HttpGet getClusterRequest = new HttpGet(uri);
    getClusterRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    return wrapException(new HttpRetryableAction<DbCluster>() {
      @Override
      public DbCluster performAction() throws ClientProtocolException, IOException {
        return httpClient.execute(dbInstanceHost, getClusterRequest,
            new DatabricksResponseHandler<>(DbCluster.class, objectMapper));
      }
    });
  }

  public void editCluster(String dbInstance, DbCluster dbCluster, String token)
      throws FeaturestoreException, IOException {
    HttpHost dbInstanceHost = getDbInstanceHost(dbInstance);
    HttpPost editRequest = new HttpPost(EDIT_ENDPOINT);
    editRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    editRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    editRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(dbCluster)));

    wrapException(new HttpRetryableAction<Object>() {
      @Override
      public Object performAction() throws ClientProtocolException, IOException {
        httpClient.execute(dbInstanceHost, editRequest, new DatabricksNoResponseHandler<>());
        return null;
      }
    });
  }

  public void installLibraries(String dbInstance, DbLibraryInstall dbLibraryInstall, String token)
      throws FeaturestoreException, IOException {
    HttpHost dbInstanceHost = getDbInstanceHost(dbInstance);
    HttpPost libraryRequest = new HttpPost(LIBRARIES_INSTALL);
    libraryRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    libraryRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    libraryRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(dbLibraryInstall)));

    wrapException(new HttpRetryableAction<Object>() {
      @Override
      public Object performAction() throws ClientProtocolException, IOException {
        httpClient.execute(dbInstanceHost, libraryRequest, new DatabricksNoResponseHandler<>());
        return null;
      }
    });
  }

  public boolean fileExists(String dbInstance, String path, String token) throws IOException {
    HttpHost dbInstanceHost = getDbInstanceHost(dbInstance);
    String uri = UriTemplate.fromTemplate(DBFS_STATUS)
        .set("path", path)
        .expand();
    HttpGet statusRequest = new HttpGet(uri);
    statusRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    try {
      new HttpRetryableAction<Object>() {
        @Override
        public Object performAction() throws ClientProtocolException, IOException {
          httpClient.execute(dbInstanceHost, statusRequest, new DatabricksNoResponseHandler<>());
          return null;
        }
      }.performAction();
      return true;
    } catch (NotRetryableClientProtocolException e) {
      return false;
    }
  }

  public void uploadOneShot(String dbInstance, DbfsPut dbfsPut, String token)
      throws FeaturestoreException, IOException {
    HttpHost dbInstanceHost = getDbInstanceHost(dbInstance);
    HttpPost uploadRequest = new HttpPost(DBFS_PUT);
    uploadRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    uploadRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    uploadRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(dbfsPut)));

    wrapException(new HttpRetryableAction<Object>() {
      @Override
      public Object performAction() throws ClientProtocolException, IOException {
        httpClient.execute(dbInstanceHost, uploadRequest, new DatabricksNoResponseHandler<>());
        return null;
      }
    });
  }

  public void uploadLarge(String dbInstance, DbfsCreate dbfsCreate, InputStream inputStream, String token)
      throws FeaturestoreException, IOException {
    HttpHost dbInstanceHost = getDbInstanceHost(dbInstance);
    DbfsClose dbfsClose = uploadCreateRequest(dbInstanceHost, dbfsCreate, token);
    uploadStream(dbInstanceHost, dbfsClose, inputStream, token);
    uploadCloseRequest(dbInstanceHost, dbfsClose, token);
  }

  public void startCluster(String dbInstance, DbClusterStart dbClusterStart, String token)
      throws FeaturestoreException, IOException {
    HttpHost dbInstanceHost = getDbInstanceHost(dbInstance);
    HttpPost startRequest = new HttpPost(START_ENDPOINT);
    startRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    startRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    startRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(dbClusterStart)));

    wrapException(new HttpRetryableAction<Object>() {
      @Override
      public Object performAction() throws ClientProtocolException, IOException {
        httpClient.execute(dbInstanceHost, startRequest, new DatabricksNoResponseHandler<>());
        return null;
      }
    });
  }

  private DbfsClose uploadCreateRequest(HttpHost dbInstanceHost, DbfsCreate dbfsCreate, String token)
      throws FeaturestoreException, IOException {
    HttpPost createRequest = new HttpPost(DBFS_CREATE);
    createRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    createRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    createRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(dbfsCreate)));

    return wrapException(new HttpRetryableAction<DbfsClose>() {
      @Override
      public DbfsClose performAction() throws ClientProtocolException, IOException {
        return httpClient.execute(dbInstanceHost, createRequest,
            new DatabricksResponseHandler<>(DbfsClose.class, objectMapper));
      }
    });
  }

  private void uploadStream(HttpHost dbInstanceHost, DbfsClose dbfsClose, InputStream inputStream, String token)
      throws FeaturestoreException, IOException {
    // Databricks request limit is 1 MB per block
    byte[] data = new byte[1024*1024];
    int read = 0;
    while ((read = inputStream.read(data)) > -1) {
      // Send any pending block data
      sendBlock(dbInstanceHost, token, dbfsClose, Base64.encodeBase64String(Arrays.copyOf(data, read)));
    }
  }

  private void sendBlock(HttpHost dbInstanceHost, String token,
                         DbfsClose dbfsClose, String data) throws FeaturestoreException, IOException {
    HttpPost uploadRequest = new HttpPost(DBFS_ADD_BLOCK);
    uploadRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    uploadRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

    DbfsAddBlock addBlock = new DbfsAddBlock(dbfsClose.getHandle(), data);
    uploadRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(addBlock)));

    wrapException(new HttpRetryableAction<Object>() {
      @Override
      public Object performAction() throws ClientProtocolException, IOException {
        httpClient.execute(dbInstanceHost, uploadRequest, new DatabricksNoResponseHandler<>());
        return null;
      }
    });
  }

  private DbfsClose uploadCloseRequest(HttpHost dbInstanceHost, DbfsClose dbfsClose, String token)
      throws FeaturestoreException, IOException {
    HttpPost closeRequest = new HttpPost(DBFS_CLOSE);
    closeRequest.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
    closeRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    closeRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(dbfsClose)));

    return wrapException(new HttpRetryableAction<DbfsClose>() {
      @Override
      public DbfsClose performAction() throws ClientProtocolException, IOException {
        return httpClient.execute(dbInstanceHost, closeRequest, new DatabricksNoResponseHandler<>());
      }
    });
  }

  private <T> T wrapException(HttpRetryableAction<T> action) throws FeaturestoreException {
    try {
      return action.performAction();
    } catch (IOException e) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.DATABRICKS_ERROR, Level.SEVERE,
          e.getMessage(), e.getMessage(), e);
    }
  }

  private HttpHost getDbInstanceHost(String dbInstance) {
    return new HttpHost(dbInstance, 443, "https");
  }
}
