/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.jupyter.git.controllers;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.security.secrets.SecretPlaintext;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.jupyter.JupyterContentsManager;
import io.hops.hopsworks.common.jupyter.JupyterNbVCSController;
import io.hops.hopsworks.common.jupyter.RepositoryStatus;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jupyter.git.controllers.qualifiers.GitHubQualifier;
import io.hops.hopsworks.jupyter.git.controllers.qualifiers.GitLabQualifier;
import io.hops.hopsworks.jupyter.git.dao.JupyterGitConfigResponse;
import io.hops.hopsworks.jupyter.git.dao.JupyterGitResponse;
import io.hops.hopsworks.jupyter.git.dao.JupyterGitStatusResponse;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterSettings;
import io.hops.hopsworks.persistence.entity.jupyter.config.GitBackend;
import io.hops.hopsworks.persistence.entity.user.Users;
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
import java.util.HashMap;
import java.util.Map;
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
  private static final String BRANCH_REMOTE_TEMPLATE = "branch.%s.remote";
  private static final String BRANCH_MERGE_TEMPLATE = "branch.%s.merge";
  private static final String BRANCH_REMOTE = "origin";
  private static final String USER_NAME_CONFIG = "user.name";
  private static final String USER_EMAIL_CONFIG = "user.email";
  private static final String BRANCH_REMOTE_REFS_TEMPLATE = "refs/heads/%s";
  
  private enum REMOTE_SERVICE {
    GITHUB("github", new GitHubQualifier()),
    GITLAB("gitlab", new GitLabQualifier());
    
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
  public Set<String> getRemoteBranches(Users user, String apiKeyName, String remoteURI, GitBackend gitBackend)
    throws ServiceException {
    if (Strings.isNullOrEmpty(remoteURI)) {
      return Collections.emptySet();
    }
    try {
      REMOTE_SERVICE remoteService = REMOTE_SERVICE.valueOf(gitBackend.name());
      Instance<RemoteGitClient> clientInstance = remoteRepoClient.select(remoteService.annotation);
      check4instance(clientInstance, "Remote repository client");
      SecretPlaintext apiKey = null;
      if(!Strings.isNullOrEmpty(apiKeyName)) {
        apiKey = secretsController.get(user, apiKeyName);
      }
      return clientInstance.get().fetchBranches(apiKey, remoteURI);
    } catch (IOException ex) {
      Level loggingLevel;
      // If it's a bad request, don't log it as severe
      String userErrorMsg, devErrorMsg;
      if (ex.getClass().isAssignableFrom(RequestException.class)) {
        loggingLevel = Level.FINE;
        if(Strings.isNullOrEmpty(apiKeyName)) {
          userErrorMsg = "Could not find repository, is it public?";
        } else {
          userErrorMsg = "Could not find repository, check URL and API key.";
        }
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
  public boolean hasWriteAccess(Users user, String apiKeyName, String remoteURI, GitBackend gitBackend)
    throws ServiceException {
    try {
      REMOTE_SERVICE remoteService = REMOTE_SERVICE.valueOf(gitBackend.name());
      Instance<RemoteGitClient> clientInstance = remoteRepoClient.select(remoteService.annotation);
      check4instance(clientInstance, "Remote repository client");
      SecretPlaintext apiKey = null;
      if(!Strings.isNullOrEmpty(apiKeyName)) {
        apiKey = secretsController.get(user, apiKeyName);
      }
      return clientInstance.get().hasWriteAccess(apiKey, remoteURI);
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
        "Failed trying to check write permission of API key",
        "Failed trying to check write permission of API key with name "
          + apiKeyName + " for user " + user.getUsername(), ex);
    } catch (UserException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
        "Could not find API key for user",
        "Could not find API key with name " + apiKeyName + " for user " + user.getUsername(), ex);
    } catch (IllegalArgumentException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
        "Could not parse remote Git URI: " + remoteURI, ex.getMessage());
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
  public RepositoryStatus init(JupyterProject jupyterProject, JupyterSettings jupyterSettings) throws ServiceException {
    try {
      JupyterGitResponse response = jupyterServerProxy.clone(jupyterProject, jupyterSettings);
      //{"code": 128, "message": "fatal: destination path 'test_repo' already exists
      // and is not an empty directory."}
      if (response.getCode().equals(0)
        || (response.getCode().equals(128) && response.getMessage().contains("already exists"))) {
        response = jupyterServerProxy.checkout(jupyterProject, jupyterSettings,
            getRepositoryName(jupyterSettings.getGitConfig().getRemoteGitURL()),
            jupyterSettings.getGitConfig().getHeadBranch(), false);
        if (response.getCode().equals(1)) {
          jupyterServerProxy.checkout(jupyterProject, jupyterSettings,
              getRepositoryName(jupyterSettings.getGitConfig().getRemoteGitURL()),
              jupyterSettings.getGitConfig().getHeadBranch(), true);
        }
        return status(jupyterProject, jupyterSettings);
      }
      return new RepositoryStatus.UnmodifiableRepositoryStatus();
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.WARNING,
          "Failed to clone repository.", ex.getMessage(), ex);
    }
  }
  
  @Override
  public RepositoryStatus status(JupyterProject jupyterProject, JupyterSettings jupyterSettings)
      throws ServiceException {
    try {
      JupyterGitStatusResponse statusResponse = jupyterServerProxy.status(jupyterProject,
          jupyterSettings, getRepositoryName(jupyterSettings.getGitConfig().getRemoteGitURL()));
      RepositoryStatus.ModifiableRepositoryStatus status = new RepositoryStatus.ModifiableRepositoryStatus();
      if (statusResponse.getCode().equals(0)) {
        if (statusResponse.getFiles().isEmpty()) {
          status.setStatus(RepositoryStatus.STATUS.CLEAN);
          status.setModifiedFiles(0);
        } else {
          status.setStatus(RepositoryStatus.STATUS.DIRTY);
          status.setModifiedFiles(statusResponse.getFiles().size());
        }
        status.setBranch(statusResponse.getBranch());
      } else {
        status.setStatus(RepositoryStatus.STATUS.UNINITIALIZED);
        status.setModifiedFiles(-1);
        status.setBranch("UNKNOWN");
      }
      status.setRepository(jupyterSettings.getGitConfig().getRemoteGitURL());
      return status;
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.WARNING,
          "Failed to get status of repository.", ex.getMessage(), ex);
    }
  }
  
  @Override
  public RepositoryStatus pull(JupyterProject jupyterProject, JupyterSettings jupyterSettings)
    throws ServiceException {
    try {
      String repositoryName = getRepositoryName(jupyterSettings.getGitConfig().getRemoteGitURL());
      // First check if branch has remote refs configured
      JupyterGitConfigResponse config = jupyterServerProxy.config(jupyterProject, jupyterSettings, repositoryName,
          Collections.emptyMap());
      if (!config.getCode().equals(0)) {
        throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
            "Could not get Git configuration");
      }
      
      Map<String, String> newConfig = new HashMap<>(4);
      boolean control = false;
      if (config.getOptions() == null) {
        newConfig.put(constructBranchRemote(jupyterSettings), BRANCH_REMOTE);
        newConfig.put(constructBranchMerge(jupyterSettings), constructBranchRemoteRefs(jupyterSettings));
        control = true;
      } else {
        String branchRemote = constructBranchRemote(jupyterSettings);
        if (!config.getOptions().containsKey(branchRemote)) {
          newConfig.put(branchRemote, BRANCH_REMOTE);
          control = true;
        }

        String branchMerge = constructBranchMerge(jupyterSettings);
        if (!config.getOptions().containsKey(branchMerge)) {
          newConfig.put(branchMerge, constructBranchRemoteRefs(jupyterSettings));
          control = true;
        }
      }
      if (control) {
        config = jupyterServerProxy.config(jupyterProject, jupyterSettings, repositoryName, newConfig);
        if (!config.getCode().equals(0)) {
          throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
              "Could not set Git configuration to repository for remote refs and user information");
        }
      }
      
      JupyterGitResponse response = jupyterServerProxy.pull(jupyterProject, jupyterSettings, repositoryName);
      if (response.getCode().equals(0)) {
        return status(jupyterProject, jupyterSettings);
      }
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
          "Could not pull for branch " + jupyterSettings.getGitConfig().getHeadBranch() +
              " " + response.getMessage());
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.WARNING,
          "Failed to pull from repository.", ex.getMessage(), ex);
    }
  }
  
  @Override
  public RepositoryStatus push(JupyterProject jupyterProject, JupyterSettings jupyterSettings, Users user)
      throws ServiceException {
    try {
      String repositoryName = getRepositoryName(jupyterSettings.getGitConfig().getRemoteGitURL());
      JupyterGitStatusResponse statusResponse = jupyterServerProxy.status(jupyterProject, jupyterSettings,
          repositoryName);
      if (statusResponse.getCode().equals(0)) {
        if (statusResponse.getFiles() != null && !statusResponse.getFiles().isEmpty()) {
          jupyterServerProxy.add(jupyterProject, jupyterSettings, repositoryName, true);
          
          // First check if user.name, user.email and remote refs are set for this repo
          JupyterGitConfigResponse config = jupyterServerProxy.config(jupyterProject, jupyterSettings, repositoryName,
              Collections.emptyMap());
          
          Map<String, String> newConfig = new HashMap<>(4);
          boolean control = false;
          if (config.getOptions() == null) {
            newConfig.put(USER_NAME_CONFIG, user.getUsername());
            newConfig.put(USER_EMAIL_CONFIG, user.getEmail());
            control = true;
          } else {
            if (!config.getOptions().containsKey(USER_NAME_CONFIG)) {
              newConfig.put(USER_NAME_CONFIG, user.getUsername());
              control = true;
            }
            
            if (!config.getOptions().containsKey(USER_EMAIL_CONFIG)) {
              newConfig.put(USER_EMAIL_CONFIG, user.getEmail());
              control = true;
            }
          }
          
          if (control) {
            config = jupyterServerProxy.config(jupyterProject, jupyterSettings, repositoryName, newConfig);
            if (!config.getCode().equals(0)) {
              throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
                  "Could not set Git configuration to repository for remote refs and user information");
            }
          }
          
          String commitMessage = "Autocommit due to Jupyter shutdown";
          jupyterServerProxy.commit(jupyterProject, jupyterSettings, repositoryName, commitMessage);
          
          // First try to push to configured upstream
          JupyterGitResponse pushResponse = jupyterServerProxy.push(jupyterProject, jupyterSettings, repositoryName,
              BRANCH_REMOTE, false);
          if (!pushResponse.getCode().equals(0)) {
            // If that fails then push to upstream branch with the same name as local branch
            pushResponse = jupyterServerProxy.push(jupyterProject, jupyterSettings, repositoryName,
                BRANCH_REMOTE, true);
          }
          if (!pushResponse.getCode().equals(0)) {
            throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
                "Could not push to remote repository Reason: " + pushResponse.getMessage());
          }
        } else {
          // Nothing to commit, need to push though
          JupyterGitResponse pushResponse = jupyterServerProxy.push(jupyterProject, jupyterSettings, repositoryName,
              BRANCH_REMOTE, false);
          if (!pushResponse.getCode().equals(0)) {
            pushResponse = jupyterServerProxy.push(jupyterProject, jupyterSettings, repositoryName,
                BRANCH_REMOTE, true);
          }
          if (!pushResponse.getCode().equals(0)) {
            throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.FINE,
                "Could not push to remote repository Reason: " + pushResponse.getMessage());
          }
        }
      }
      return status(jupyterProject, jupyterSettings);
    } catch (IOException ex) {
      throw new ServiceException(RESTCodes.ServiceErrorCode.GIT_COMMAND_FAILURE, Level.WARNING,
          "Failed to push to repository.", ex.getMessage(), ex);
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
  
  private String constructBranchRemote(JupyterSettings jupyterSettings) {
    return String.format(BRANCH_REMOTE_TEMPLATE, jupyterSettings.getGitConfig().getHeadBranch());
  }
  
  private String constructBranchMerge(JupyterSettings jupyterSettings) {
    return String.format(BRANCH_MERGE_TEMPLATE, jupyterSettings.getGitConfig().getHeadBranch());
  }
  
  private String constructBranchRemoteRefs(JupyterSettings jupyterSettings) {
    return String.format(BRANCH_REMOTE_REFS_TEMPLATE, jupyterSettings.getGitConfig().getBaseBranch());
  }
}
