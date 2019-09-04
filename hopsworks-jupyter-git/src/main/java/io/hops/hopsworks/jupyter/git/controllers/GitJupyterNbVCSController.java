/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.controllers;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jupyter.JupyterContentsManager;
import io.hops.hopsworks.common.jupyter.JupyterNbVCSController;
import io.hops.hopsworks.common.jupyter.RepositoryStatus;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jupyter.git.controllers.qualifiers.GitHubQualifier;
import io.hops.hopsworks.jupyter.git.dao.CommitParameters;
import io.hops.hopsworks.restutils.RESTCodes;
import org.eclipse.egit.github.core.client.RequestException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@GitJupyterNbVCSStereotype
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GitJupyterNbVCSController implements JupyterNbVCSController {
  private static final Logger LOG = Logger.getLogger(GitJupyterNbVCSController.class.getName());
  
  private static final Pattern REPO_ATTRS = Pattern.compile("http(s)?://(?<type>.+)/(?<username>.+)/(?<repository>"
      + ".+)\\.git");
  
  private enum REMOTE_SERVICE {
    GITHUB("github.com", new GitHubQualifier());
    
    private String uniqueServiceIdentifier;
    private AnnotationLiteral annotation;
    
    REMOTE_SERVICE(String uniqueServiceIdentifier, AnnotationLiteral annotation) {
      this.uniqueServiceIdentifier = uniqueServiceIdentifier;
      this.annotation = annotation;
    }
  }
  
  @EJB
  private SecretsController secretsController;
  @EJB
  private UserFacade userFacade;
  @Inject @Any
  private Instance<RemoteGitClient> remoteRepoClient;
  @EJB
  private JupyterServerProxy jupyterServerProxy;
  
  @Override
  public boolean isGitAvailable() {
    return true;
  }
  
  @Override
  public JupyterContentsManager getJupyterContentsManagerClass(String remoteURI) throws ServiceException {
    if (!Strings.isNullOrEmpty(remoteURI)) {
      Matcher gitMatcher = REPO_ATTRS.matcher(remoteURI);
      if (gitMatcher.matches()) {
        return JupyterContentsManager.LARGE_FILE_CONTENT_MANAGER;
      }
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.WARNING,
          "Could not recognize URI " + remoteURI,
          "URI: " + remoteURI + " could not be parsed by " + REPO_ATTRS.pattern());
    }
    return JupyterContentsManager.HDFS_CONTENTS_MANAGER;
  }
  
  @Override
  public Set<String> getRemoteBranches(Users user, String apiKeyName, String remoteURI) throws ServiceException {
    if (Strings.isNullOrEmpty(remoteURI)) {
      return Collections.emptySet();
    }
    try {
      String repoName = getRepositoryName(remoteURI);
      REMOTE_SERVICE remoteService = getRemoteServiceType(remoteURI);
      Instance<RemoteGitClient> clientInstance = remoteRepoClient.select(remoteService.annotation);
      check4instance(clientInstance, "Remote repository client");
      SecretPlaintext apiKey = secretsController.get(user, apiKeyName);
      return clientInstance.get().fetchBranches(apiKey, repoName);
    } catch (IOException ex) {
      Level loggingLevel;
      // If it's a bad request, don't log it as severe
      String userErrorMsg, devErrorMsg;
      if (ex.getClass().isAssignableFrom(RequestException.class)) {
        loggingLevel = Level.FINE;
        userErrorMsg = "Invalid API key";
        devErrorMsg = "API key " + apiKeyName + " is not valid for " + remoteURI;
      } else {
        loggingLevel = Level.SEVERE;
        userErrorMsg = "Could not fetch branches for repository " + remoteURI;
        devErrorMsg = "Error fetching branches for repository " + remoteURI + " for user " + user.getUsername();
      }
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, loggingLevel,
          userErrorMsg, devErrorMsg, ex);
    } catch (UserException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
          "Could not find API key for user",
          "Could not find API key with name " + apiKeyName + " for user " + user.getUsername(), ex);
    } catch (IllegalArgumentException ex) {
      String msg = "Could not parse remote Git URI: " + remoteURI;
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
          msg, ex.getMessage());
    }
  }
  
  @Override
  public String getGitApiKey(String hdfsUser, String apiKeyName) throws ServiceException {
    String[] tokenized = hdfsUser.split(HdfsUsersController.USER_NAME_DELIMITER, 2);
    if (tokenized.length != 2) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.FINE,
          "Error tokenizing HDFS user " + hdfsUser);
    }
    Users user = userFacade.findByUsername(tokenized[1]);
    try {
      SecretPlaintext apiKeyPlaintext = secretsController.get(user, apiKeyName);
      return apiKeyPlaintext.getPlaintext();
    } catch (UserException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.JUPYTER_START_ERROR, Level.FINE,
          ex.getUsrMsg(), ex.getDevMsg(), ex);
    }
  }
  
  @Override
  public RepositoryStatus cloneOrPullRemoteRepository(JupyterProject jupyterProject) throws ServiceException {
    try {
      return jupyterServerProxy.cloneOrPullFromRemote(jupyterProject);
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.WARNING,
          ex.getMessage(), ex.getMessage(), ex);
    }
  }
  
  @Override
  public RepositoryStatus commit(JupyterProject jupyterProject, Users user, String commitMessage)
      throws ServiceException {
    if (commitMessage == null) {
      throw new IllegalArgumentException("Commit message cannot be null");
    }
    CommitParameters params = new CommitParameters(commitMessage,
        new CommitParameters.Author(user.getUsername(), user.getEmail()));
    try {
      return jupyterServerProxy.commit(jupyterProject, params);
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.WARNING,
          ex.getMessage(), ex.getMessage(), ex);
    }
  }
  
  @Override
  public RepositoryStatus push(JupyterProject jupyterProject) throws ServiceException {
    try {
      return jupyterServerProxy.push(jupyterProject);
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.WARNING,
          ex.getMessage(), ex.getMessage(), ex);
    }
  }
  
  @Override
  public RepositoryStatus status(JupyterProject jupyterProject) throws ServiceException {
    try {
      return jupyterServerProxy.status(jupyterProject);
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.WARNING,
          ex.getMessage(), ex.getMessage(), ex);
    }
  }
  
  private void check4instance(Instance instance, String instanceUsage) throws ServiceException {
    if (instance.isUnsatisfied() || instance.isAmbiguous()) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.SEVERE,
          "Could not find instance for " + instanceUsage,
          instanceUsage + " is either unsatisfied or ambiguous");
    }
  }
  
  private String getRepositoryName(String remoteURI) {
    Matcher matcher = REPO_ATTRS.matcher(remoteURI);
    if (matcher.matches()) {
      return matcher.group("repository");
    }
    throw new IllegalArgumentException("Could not parse remote URI: " + remoteURI);
  }
  
  private REMOTE_SERVICE getRemoteServiceType(String remoteURI) {
    Matcher matcher = REPO_ATTRS.matcher(remoteURI);
    if (matcher.matches()) {
      String remoteService = matcher.group("type");
      for (REMOTE_SERVICE remote_service : REMOTE_SERVICE.values()) {
        if (remoteService.toLowerCase().equals(remote_service.uniqueServiceIdentifier)) {
          return remote_service;
        }
      }
      throw new UnsupportedOperationException("Could not find type of: " + remoteService);
    }
    throw new IllegalArgumentException("Could not parse remote URI: " + remoteURI);
  }
}
