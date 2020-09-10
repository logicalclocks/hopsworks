/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.controllers;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import io.hops.hopsworks.common.jupyter.JupyterManager;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jupyter.git.dao.JupyterGitArguments;
import io.hops.hopsworks.jupyter.git.dao.JupyterGitConfigResponse;
import io.hops.hopsworks.jupyter.git.dao.JupyterGitResponse;
import io.hops.hopsworks.jupyter.git.dao.JupyterGitStatusResponse;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JupyterServerProxy {
  
  private static final Gson GSON_SERIALIZER = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .create();
  private static final String JUPYTER_HOST = "http://%s:%d";
  
  private static final String BASE_PATH = "/hopsworks-api/jupyter/%d/git";
  
  private static final String CLONE_PATH = BASE_PATH + "/clone";
  private static final String STATUS_PATH = BASE_PATH + "/statusext";
  private static final String CHECKOUT_PATH = BASE_PATH + "/checkout";
  private static final String PULL_PATH = BASE_PATH + "/pull";
  private static final String CONFIG_PATH = BASE_PATH + "/config";
  private static final String ADD_PATH = BASE_PATH + "/add";
  private static final String COMMIT_PATH = BASE_PATH + "/commit";
  private static final String PUSH_PATH = BASE_PATH + "/push";
  private static final String AUTOPUSH_PATH = BASE_PATH + "/autopush";
  
  private static final ResponseHandler JUPYTER_GIT_GENERIC_JSON_HANDLER = new JupyterGitGenericJSONProxyHandler();
  private static final ResponseHandler JUPYTER_GIT_GENERIC_PLAIN_HANDLER = new JupyterGitGenericPlainProxyHandler();
  private static final ResponseHandler JUPYTER_GIT_STATUS_RESPONSE_HANDLER = new JupyterGitStatusProxyHandler();
  private static final ResponseHandler JUPYTER_GIT_CONFIG_RESPONSE_HANDLER = new JupyterGitConfigProxyHandler();
  
  @EJB
  private HttpClient client;
  @EJB
  private Settings settings;
  @Inject
  private JupyterManager jupyterManager;
  
  private static final Logger LOGGER = Logger.getLogger(Settings.class.
      getName());
  
  public JupyterGitResponse clone(JupyterProject jupyterProject, JupyterSettings jupyterSettings) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    if (jupyterSettings == null) {
      throw new IllegalArgumentException("JupyterSettings is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST, jupyterManager.getJupyterHost(), jupyterPort));
    String path = String.format(CLONE_PATH, jupyterPort);
    String notebookDirectory = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
    JupyterGitArguments args = new JupyterGitArguments(notebookDirectory, jupyterSettings.getGitConfig()
        .getRemoteGitURL());
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      request.setEntity(new StringEntity(GSON_SERIALIZER.toJson(args)));
      return client.<JupyterGitResponse>execute(host, request, JUPYTER_GIT_GENERIC_JSON_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public JupyterGitStatusResponse status(JupyterProject jupyterProject, JupyterSettings jupyterSettings,
      String repositoryName) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    if (jupyterSettings == null) {
      throw new IllegalArgumentException("JupyterSettings is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST,
        jupyterManager.getJupyterHost(), jupyterPort));
    String path = String.format(STATUS_PATH, jupyterPort);
    String notebookDirectory = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
    JupyterGitArguments args = new JupyterGitArguments();
    args.setCurrentPath(Paths.get(notebookDirectory, repositoryName).toString());
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      request.setEntity(new StringEntity(GSON_SERIALIZER.toJson(args)));
      return client.<JupyterGitStatusResponse>execute(host, request, JUPYTER_GIT_STATUS_RESPONSE_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public JupyterGitResponse checkout(JupyterProject jupyterProject, JupyterSettings jupyterSettings,
      String repositoryName, String branchName, Boolean newBranch) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    if (jupyterSettings == null) {
      throw new IllegalArgumentException("JupyterSettings is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST,
        jupyterManager.getJupyterHost(), jupyterPort));
    String path = String.format(CHECKOUT_PATH, jupyterPort);
    String notebookDirectory = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
    JupyterGitArguments args = new JupyterGitArguments();
    args.setTopRepoPath(Paths.get(notebookDirectory, repositoryName).toString());
    args.setCheckoutBranch(true);
    args.setBranchname(branchName);
    args.setNewCheck(newBranch);
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      request.setEntity(new StringEntity(GSON_SERIALIZER.toJson(args)));
      return client.<JupyterGitStatusResponse>execute(host, request, JUPYTER_GIT_GENERIC_JSON_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public JupyterGitResponse pull(JupyterProject jupyterProject, JupyterSettings jupyterSettings,
      String repositoryName) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    if (jupyterSettings == null) {
      throw new IllegalArgumentException("JupyterSettings is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST,
        jupyterManager.getJupyterHost(), jupyterPort));
    String path = String.format(PULL_PATH, jupyterPort);
    String notebookDirectory = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
    JupyterGitArguments args = new JupyterGitArguments();
    args.setCurrentPath(Paths.get(notebookDirectory, repositoryName).toString());
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      request.setEntity(new StringEntity(GSON_SERIALIZER.toJson(args)));
      return client.<JupyterGitResponse>execute(host, request, JUPYTER_GIT_GENERIC_JSON_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public JupyterGitConfigResponse config(JupyterProject jupyterProject, JupyterSettings jupyterSettings,
      String repositoryName, Map<String, String> options) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    if (jupyterSettings == null) {
      throw new IllegalArgumentException("JupyterSettings is null");
    }
    if (options == null) {
      throw new IllegalArgumentException("Git config is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST,
        jupyterManager.getJupyterHost(), jupyterPort));
    String path = String.format(CONFIG_PATH, jupyterPort);
    String notebookDirectory = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
    JupyterGitArguments args = new JupyterGitArguments();
    args.setPath(Paths.get(notebookDirectory, repositoryName).toString());
    args.setOptions(options);
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      request.setEntity(new StringEntity(GSON_SERIALIZER.toJson(args)));
      return client.<JupyterGitConfigResponse>execute(host, request, JUPYTER_GIT_CONFIG_RESPONSE_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public void add(JupyterProject jupyterProject, JupyterSettings jupyterSettings,
      String repositoryName, Boolean addAll) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    if (jupyterSettings == null) {
      throw new IllegalArgumentException("JupyterSettings is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST,
        jupyterManager.getJupyterHost(), jupyterPort));
    String path = String.format(ADD_PATH, jupyterPort);
    String notebookDirectory = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
    JupyterGitArguments args = new JupyterGitArguments();
    args.setTopRepoPath(Paths.get(notebookDirectory, repositoryName).toString());
    args.setAddAll(addAll);
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      request.setEntity(new StringEntity(GSON_SERIALIZER.toJson(args)));
      client.<JupyterGitResponse>execute(host, request, JUPYTER_GIT_GENERIC_JSON_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public String commit(JupyterProject jupyterProject, JupyterSettings jupyterSettings,
      String repositoryName, String commitMessage) throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    if (jupyterSettings == null) {
      throw new IllegalArgumentException("JupyterSettings is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST,
        jupyterManager.getJupyterHost(), jupyterPort));
    String path = String.format(COMMIT_PATH, jupyterPort);
    String notebookDirectory = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
    JupyterGitArguments args = new JupyterGitArguments();
    args.setTopRepoPath(Paths.get(notebookDirectory, repositoryName).toString());
    args.setCommitMsg(commitMessage);
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      request.setEntity(new StringEntity(GSON_SERIALIZER.toJson(args)));
      return client.<String>execute(host, request, JUPYTER_GIT_GENERIC_PLAIN_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  public JupyterGitResponse push(JupyterProject jupyterProject, JupyterSettings jupyterSettings,
      String repositoryName, String remote, boolean andTrack)
    throws IOException {
    if (jupyterProject == null) {
      throw new IllegalArgumentException("JupyterProject is null");
    }
    if (jupyterSettings == null) {
      throw new IllegalArgumentException("JupyterSettings is null");
    }
    Integer jupyterPort = jupyterProject.getPort();
    HttpHost host = HttpHost.create(String.format(JUPYTER_HOST,
        jupyterManager.getJupyterHost(), jupyterPort));
    String pushPath = andTrack ? AUTOPUSH_PATH : PUSH_PATH;
    String path = String.format(pushPath, jupyterPort);
    String notebookDirectory = settings.getStagingDir() + Settings.PRIVATE_DIRS + jupyterSettings.getSecret();
    JupyterGitArguments args = new JupyterGitArguments();
    args.setCurrentPath(Paths.get(notebookDirectory, repositoryName).toString());
    if (andTrack) {
      args.setRemote(remote);
    }
    try {
      URI uri = addTokenParameter(jupyterProject, path);
      HttpPost request = new HttpPost(uri);
      request.setEntity(new StringEntity(GSON_SERIALIZER.toJson(args)));
      return client.<JupyterGitResponse>execute(host, request, JUPYTER_GIT_GENERIC_JSON_HANDLER);
    } catch (URISyntaxException ex) {
      throw new IOException(ex);
    }
  }
  
  private URI addTokenParameter(JupyterProject jupyterProject, String path) throws URISyntaxException {
    return new URIBuilder(path)
        .addParameter("token", jupyterProject.getToken())
        .build();
  }
  
  private static class JupyterGitGenericJSONProxyHandler implements ResponseHandler<JupyterGitResponse> {
    
    @Override
    public JupyterGitResponse handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      String responseJSON = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      if (HttpStatus.SC_OK == status) {
        return GSON_SERIALIZER.fromJson(responseJSON, JupyterGitResponse.class);
      }
      try {
        JupyterGitResponse errorResponse = GSON_SERIALIZER.fromJson(responseJSON, JupyterGitResponse.class);
        throw new ClientProtocolException(errorResponse.toString());
      } catch (JsonSyntaxException ex) {
        String errorMessage = "HTTP code: " + status + " Reason: " + responseJSON;
        throw new ClientProtocolException(errorMessage);
      }
    }
  }
  
  private static class JupyterGitGenericPlainProxyHandler implements ResponseHandler<String> {
  
    @Override
    public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      String responseStr = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      if (HttpStatus.SC_OK == status) {
        return responseStr;
      }
      throw new ClientProtocolException(responseStr);
    }
  }
  
  private static class JupyterGitStatusProxyHandler implements ResponseHandler<JupyterGitStatusResponse> {
  
    @Override
    public JupyterGitStatusResponse handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      String responseJSON = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      if (HttpStatus.SC_OK == status) {
        return GSON_SERIALIZER.fromJson(responseJSON, JupyterGitStatusResponse.class);
      }
      try {
        JupyterGitStatusResponse errorResponse = GSON_SERIALIZER.fromJson(responseJSON, JupyterGitStatusResponse.class);
        throw new ClientProtocolException(errorResponse.toString());
      } catch (JsonSyntaxException ex) {
        String errorMessage = "HTTP code: " + status + " Reason: " + responseJSON;
        throw new ClientProtocolException(errorMessage);
      }
    }
  }
  
  private static class JupyterGitConfigProxyHandler implements ResponseHandler<JupyterGitConfigResponse> {
    
    @Override
    public JupyterGitConfigResponse handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      String responseJSON = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      if (HttpStatus.SC_CREATED == status) {
        return GSON_SERIALIZER.fromJson(responseJSON, JupyterGitConfigResponse.class);
      }
      try {
        JupyterGitConfigResponse errorResponse = GSON_SERIALIZER.fromJson(responseJSON, JupyterGitConfigResponse.class);
        throw new ClientProtocolException(errorResponse.toString());
      } catch (JsonSyntaxException ex) {
        String errorMessage = "HTTP code: " + status + " Reason: " + responseJSON;
        throw new ClientProtocolException(errorMessage);
      }
    }
  }
}
