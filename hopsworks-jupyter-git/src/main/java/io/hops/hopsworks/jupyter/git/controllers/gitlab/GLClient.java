/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.jupyter.git.controllers.gitlab;

import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jupyter.git.controllers.RemoteGitClient;
import io.hops.hopsworks.jupyter.git.controllers.qualifiers.GitLab;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.RepositoryApi;
import org.gitlab4j.api.models.Branch;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
@GitLab
public class GLClient implements RemoteGitClient {
  private static final Logger LOG = Logger.getLogger(GLClient.class.getName());
  @EJB
  private GLClientCache glClientCache;
  
  @Override
  public Set<String> fetchBranches(SecretPlaintext apiKey, String repository) throws ServiceException, IOException {
    URL url = getUrl(repository);
    GitLabApi gitLabApi = glClientCache.getClient(getHostUrl(url), apiKey);
    RepositoryApi repositoryApi = new RepositoryApi(gitLabApi);
    List<Branch> branches = getBranches(repositoryApi, getPath(url));
    Set<String> flatBranches = new LinkedHashSet<>(branches.size());
    branches.stream().map(Branch::getName).forEach(flatBranches::add);
    return flatBranches;
  }

  @Override
  public boolean hasWriteAccess(SecretPlaintext apiKey, String repository) throws ServiceException, IOException {
    return true;
  }

  private List<Branch> getBranches(RepositoryApi repositoryApi, String path) throws IOException {
    List<Branch> branches;
    try {
      branches = repositoryApi.getBranches(path);
    } catch (GitLabApiException e) {
      throw new IOException(e);
    }
    return branches;
  }
  
  private URL getUrl(String repo) throws IOException {
    URL url;
    try {
      url = new URL(repo);
    } catch (MalformedURLException e) {
      throw new IOException(e);
    }
    return url;
  }
  
  private String getHostUrl(URL url) {
    return String.format("%s://%s", url.getProtocol(), url.getAuthority());
  }
  
  private String getPath(URL url) {
    String path = url.getPath();
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    return path.endsWith(".git")? path.substring(0, path.length() - 4) : path;
  }
}
