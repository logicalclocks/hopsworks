/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.controllers;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.jupyter.RepositoryStatus;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jupyter.git.dao.CommitParameters;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterServerProxy {
  
  private static final Gson GSON_SERIALIZER = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();
  private static final String JUPYTER_HOST = "http://%s:%d";
  
  private static final String BASE_PATH = "/hopsworks-api/jupyter/%d/git";
  private static final String CLONE_PULL_PATH = BASE_PATH + "/init";
  private static final String COMMIT_PATH = BASE_PATH + "/commit";
  private static final String PUSH_PATH = BASE_PATH + "/push";
  private static final String STATUS_PATH = BASE_PATH + "/status";
  
  private final ResponseHandler JUPYTER_RESPONSE_HANDLER;
  
  @EJB
  private HttpClient client;
  @EJB
  private Settings settings;
  
  private String jupyterHost;
  
  public JupyterServerProxy() {
    JUPYTER_RESPONSE_HANDLER = new JupyterProxyHandler();
  }
  
  @PostConstruct
  public void init() {
    jupyterHost = settings.getJupyterHost();
  }
  
  public RepositoryStatus cloneOrPullFromRemote(JupyterProject jupyterProject) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST, jupyterHost, jupyterPort));
    String path = String.format(CLONE_PULL_PATH, jupyterPort);
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost httpRequest = new HttpPost(uri);
      return client.<RepositoryStatus>execute(host, httpRequest, JUPYTER_RESPONSE_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public RepositoryStatus commit(JupyterProject jupyterProject, CommitParameters parameters) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST, jupyterHost, jupyterPort));
    String path = String.format(COMMIT_PATH, jupyterPort);
    
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      String jsonParameters = GSON_SERIALIZER.toJson(parameters);
      request.setEntity(new StringEntity(jsonParameters));
      return client.<RepositoryStatus>execute(host, request, JUPYTER_RESPONSE_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public RepositoryStatus push(JupyterProject jupyterProject) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
  
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST, jupyterHost, jupyterPort));
    String path = String.format(PUSH_PATH, jupyterPort);
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      return client.<RepositoryStatus>execute(host, request, JUPYTER_RESPONSE_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public RepositoryStatus status(JupyterProject jupyterProject) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST, jupyterHost, jupyterPort));
    String path = String.format(STATUS_PATH, jupyterPort);
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpGet request = new HttpGet(uri);
      return client.<RepositoryStatus>execute(host, request, JUPYTER_RESPONSE_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  private URI addTokenParameter(JupyterProject jupyterProject, String path) throws URISyntaxException {
    return new URIBuilder(path)
        .addParameter("token", jupyterProject.getToken())
        .build();
  }
  
  private class JupyterProxyHandler implements ResponseHandler<RepositoryStatus> {
  
    @Override
    public RepositoryStatus handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      if (HttpStatus.SC_OK == status) {
        return GSON_SERIALIZER.fromJson(EntityUtils.toString(response.getEntity(), Charset.defaultCharset()),
            RepositoryStatus.ModifiableRepositoryStatus.class);
      }
      ResponseErrorContainer errorMessage = GSON_SERIALIZER.fromJson(EntityUtils.toString(response.getEntity(),
          Charset.defaultCharset()),
          ResponseErrorContainer.class);
      throw new ClientProtocolException(errorMessage.toString());
    }
  }
  
  private class ResponseErrorContainer {
    private Integer code;
    private String reason;
  
    public Integer getCode() {
      return code;
    }
  
    public void setCode(Integer code) {
      this.code = code;
    }
  
    public String getReason() {
      return reason;
    }
  
    public void setReason(String reason) {
      this.reason = reason;
    }
  
    @Override
    public String toString() {
      return "Status code: " + code + " Reason: " + reason;
    }
  }
}
