/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.git.util;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.git.GitRepositoryFacade;
import io.hops.hopsworks.common.git.BasicAuthSecrets;
import io.hops.hopsworks.common.git.CloneCommandConfiguration;
import io.hops.hopsworks.common.git.CommitCommandConfiguration;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandType;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Stateless
public class GitCommandConfigurationValidator {
  private static final Logger LOGGER = Logger.getLogger(GitCommandConfigurationValidator.class.getName());
  private static final Pattern REPO_ATTRS = Pattern.compile("http(s)?://(?<type>.+)/(?<username>.+)/(?<repository>"
      + ".+)\\.git");

  @EJB
  private GitRepositoryFacade gitRepositoryFacade;
  @EJB
  private Settings settings;
  @EJB
  private DistributedFsService dfs;

  public void verifyCloneOptions(Project project, Users user, CloneCommandConfiguration config)
      throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(config.getUrl())) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.REPOSITORY_URL_NOT_PROVIDED.getMessage());
    } else if (config.getProvider() == null) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.GIT_PROVIDER_NOT_PROVIDED.getMessage()
          + "Invalid git provider. Git provider should be one of GitHub, GitLab, or BitBucket");
    }
    verifyRepositoryPath(project, user, config.getPath());
  }

  public void verifyCommitOptions(CommitCommandConfiguration commitConfiguration)
      throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(commitConfiguration.getMessage())) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.COMMIT_MESSAGE_IS_EMPTY.getMessage());
    } else if (!commitConfiguration.isAll() && commitConfiguration.getFiles().isEmpty()) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.COMMIT_FILES_EMPTY.getMessage() + ". Please specify" +
          " set the all option to true if you want to add and commit all the files.");
    }
  }

  public void verifyRemoteNameAndBranch(String remoteName, String branchName) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(remoteName)) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_REMOTE_NAME.getMessage());
    } else if (Strings.isNullOrEmpty(branchName)) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_BRANCH_NAME.getMessage());
    }
  }

  public GitRepository verifyRepository(Project project, Users user, Integer repositoryId) throws GitOpException {
    GitRepository repository = verifyRepository(project, repositoryId);
    if (user.equals(repository.getCreator())) {
      return repository;
    }
    throw new GitOpException(RESTCodes.GitOpErrorCode.USER_IS_NOT_REPOSITORY_OWNER, Level.INFO,
            "Git operation forbidden. Only repository owners can execute a git command in repository");
  }

  public GitRepository verifyRepository(Project project, Integer repositoryId) throws GitOpException {
    Optional<GitRepository> optional = gitRepositoryFacade.findByIdAndProject(project, repositoryId);
    return optional.orElseThrow(() -> new GitOpException(RESTCodes.GitOpErrorCode.REPOSITORY_NOT_FOUND, Level.INFO,
            "No repository with id [" + repositoryId + "] in project [" + project.getId() + "] found in database."));
  }

  public void verifyReadOnly(GitCommandType gitCommandType, GitRepository repository) throws GitOpException {
    if (settings.getEnableGitReadOnlyRepositories() && (gitCommandType == GitCommandType.COMMIT
            || gitCommandType == GitCommandType.FILE_CHECKOUT || gitCommandType == GitCommandType.PUSH)) {
      throw new GitOpException(RESTCodes.GitOpErrorCode.READ_ONLY_REPOSITORY, Level.INFO,
              "Git operation forbidden. Repository is read only. Contact your admin to enable this feature.");
    }
  }

  /**
   * Verifies if the path to clone is a dir and is not already a git repository
   *
   * @param gitPath
   * @throws IllegalArgumentException
   */
  private void verifyRepositoryPath(Project project, Users user, String gitPath) throws IllegalArgumentException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps(project, user);
      if (Strings.isNullOrEmpty(gitPath)) {
        throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.DIRECTORY_PATH_NOT_PROVIDED.getMessage());
      } else if (!dfso.exists(gitPath)) {
        throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.DIRECTORY_PATH_DOES_NOT_EXIST.getMessage());
      } else {
        if (!dfso.isDir(gitPath)) {
          throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.PATH_IS_NOT_DIRECTORY.getMessage());
        }
        //Verify if path is not already a git repository
        if (gitRepositoryFacade.findByPath(gitPath).isPresent()) {
          throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.DIRECTORY_IS_ALREADY_GIT_REPO.getMessage());
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.ERROR_VALIDATING_REPOSITORY_PATH.getMessage());
    } finally {
      dfs.closeDfsClient(dfso);
    }
  }

  public void validateProviderConfiguration(BasicAuthSecrets secrets, GitCommandConfiguration gitCommandConfiguration)
      throws GitOpException {
    GitCommandType commandType = gitCommandConfiguration.getCommandType();
    GitProvider gitProvider = gitCommandConfiguration.getProvider();

    //BitBucket always require username and token for clone
    //BitBucket, GitLab always require username and token for pull
    //BitBucket, GitLab, GitHub always require username and token for push
    boolean authSecretsConfigured = authSecretsConfigured(secrets);
    if ((commandType == GitCommandType.PUSH && !authSecretsConfigured)
            || (commandType == GitCommandType.CLONE && gitProvider == GitProvider.BITBUCKET
            && !authSecretsConfigured)
            || (commandType == GitCommandType.PULL && (gitProvider == GitProvider.GIT_LAB ||
            gitProvider == GitProvider.BITBUCKET) && !authSecretsConfigured)) {
      throw new GitOpException(RESTCodes.GitOpErrorCode.GIT_USERNAME_AND_PASSWORD_NOT_SET, Level.WARNING,
              ". You should setup secrets for " + gitProvider.getProvider() + " to be able to perform a "
                      + commandType.getGitCommand() + " operation");
    }
  }

  public String getRepositoryName(String remoteURI) throws IllegalArgumentException {
    Matcher matcher = REPO_ATTRS.matcher(remoteURI);
    if (matcher.matches()) {
      return matcher.group("repository");
    } else {
      throw new IllegalArgumentException("Could not parse remote URI: " + remoteURI);
    }
  }

  private boolean authSecretsConfigured(BasicAuthSecrets basicAuthSecrets) {
    return !Strings.isNullOrEmpty(basicAuthSecrets.getUsername())
            && !Strings.isNullOrEmpty(basicAuthSecrets.getPassword());
  }
}
