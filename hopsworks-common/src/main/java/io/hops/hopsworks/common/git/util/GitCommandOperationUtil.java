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

import io.hops.hopsworks.common.dao.git.GitPaths;
import io.hops.hopsworks.common.git.BasicAuthSecrets;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.security.secrets.SecretsController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.GitRepositoryRemote;
import io.hops.hopsworks.persistence.entity.git.config.GitProvider;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.secrets.VisibilityType;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class GitCommandOperationUtil {
  private static final Logger LOGGER = Logger.getLogger(GitCommandOperationUtil.class.getName());

  public static final String COMMAND_LOG_FILE_NAME = "command_output.log";
  public static final String HOPSFS_MOUNT_LOG_FILE_NAME = "hopsfs_mount.log";

  //we want to put some information to elastic via the logfile name. This is seperator for the attributes.
  // Will work if the repository name will not contain this pattern
  private static final String LOG_ATTRIBUTES_SEPERATOR = "--s--";

  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private Settings settings;
  @EJB
  private SecretsController secretsController;


  public void cleanUp(Project project, Users user, String configSecret) {
    String gitHomePath = getGitHome(configSecret);
    try {
      String certificatesDir = Paths.get(gitHomePath, "certificates").toString();
      HopsUtils.cleanupCertificatesForUserCustomDir(user.getUsername(), project
          .getName(), settings.getHdfsTmpCertDir(), certificateMaterializer, certificatesDir, settings);
    } catch (Exception e) {
      //Fail silently
      LOGGER.log(Level.SEVERE, "Failed to clean up git execution for user " + user.getUsername(), e);
    }
    FileUtils.deleteQuietly(new File(gitHomePath));
  }

  public String getLogFileFullPath(GitOpExecution execution, String logDirPath, String hdfsUsername,
                                   String logFileName) {
    return logDirPath + File.separator + hdfsUsername
        + LOG_ATTRIBUTES_SEPERATOR  + execution.getId()
        + LOG_ATTRIBUTES_SEPERATOR + execution.getRepository().getName()
        + LOG_ATTRIBUTES_SEPERATOR + execution.getGitCommandConfiguration().getCommandType()
        + LOG_ATTRIBUTES_SEPERATOR + logFileName;
  }

  public void generatePaths(GitPaths gitPaths) throws GitOpException {
    try {
      File baseDir = new File(gitPaths.getGitPath());
      baseDir.mkdirs();
      // Set owner persmissions
      Set<PosixFilePermission> xOnly = new HashSet<>();
      xOnly.add(PosixFilePermission.OWNER_WRITE);
      xOnly.add(PosixFilePermission.OWNER_READ);
      xOnly.add(PosixFilePermission.OWNER_EXECUTE);
      xOnly.add(PosixFilePermission.GROUP_WRITE);
      xOnly.add(PosixFilePermission.GROUP_EXECUTE);

      Set<PosixFilePermission> perms = new HashSet<>();
      //add owners permission
      perms.add(PosixFilePermission.OWNER_READ);
      perms.add(PosixFilePermission.OWNER_WRITE);
      perms.add(PosixFilePermission.OWNER_EXECUTE);
      //add group permissions
      perms.add(PosixFilePermission.GROUP_READ);
      perms.add(PosixFilePermission.GROUP_WRITE);
      perms.add(PosixFilePermission.GROUP_EXECUTE);
      //add others permissions
      perms.add(PosixFilePermission.OTHERS_READ);
      perms.add(PosixFilePermission.OTHERS_EXECUTE);

      Files.setPosixFilePermissions(Paths.get(gitPaths.getGitPath()), perms);

      new File(gitPaths.getLogDirPath()).mkdirs();
      new File(gitPaths.getCertificatesDirPath()).mkdirs();
      new File(gitPaths.getRunDirPath()).mkdirs();
      new File(gitPaths.getTokenPath()).mkdirs();
      new File(gitPaths.getConfDirPath()).mkdirs();

      Files.setPosixFilePermissions(Paths.get(gitPaths.getLogDirPath()), perms);
    } catch (IOException ex) {
      removeProjectUserDirRecursive(gitPaths);
      throw new GitOpException(RESTCodes.GitOpErrorCode.GIT_PATHS_CREATION_ERROR, Level.SEVERE, "Failed to " +
          "create git paths", ex.getMessage(), ex);
    }
  }

  private void removeProjectUserDirRecursive(GitPaths gitPaths) {
    try {
      FileUtils.deleteDirectory(new File(gitPaths.getGitPath()));
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Could not delete Git directory: " + gitPaths.getGitPath(), e);
    }
  }


  /**
   * Requires both username and password/token secrets for a git provider to be configured
   *
   * @param user
   * @param gitProvider
   * @throws IllegalArgumentException
   */
  public BasicAuthSecrets getAuthenticationSecrets(Users user, GitProvider gitProvider)
      throws IllegalArgumentException {
    String token = "";
    String username = "";
    try {
      switch (gitProvider) {
        case BITBUCKET:
          token = secretsController.get(user, Constants.BITBUCKET_TOKEN_SECRET_NAME).getPlaintext();
          username = secretsController.get(user, Constants.BITBUCKET_USERNAME_SECRET_NAME).getPlaintext();
          return new BasicAuthSecrets(username, token);
        case GIT_HUB:
          token  = secretsController.get(user, Constants.GITHUB_TOKEN_SECRET_NAME).getPlaintext();
          username = secretsController.get(user, Constants.GITHUB_USERNAME_SECRET_NAME).getPlaintext();
          return new BasicAuthSecrets(username, token);
        case GIT_LAB:
          token  = secretsController.get(user, Constants.GITLAB_TOKEN_SECRET_NAME).getPlaintext();
          username = secretsController.get(user, Constants.GITLAB_USERNAME_SECRET_NAME).getPlaintext();
          return new BasicAuthSecrets(username, token);
        default:
          throw new IllegalArgumentException("Unsupported git provider");
      }
    } catch (UserException e) {
      return new BasicAuthSecrets(username, token);
    }
  }

  public void createAuthenticationSecret(Users user, String username, String token, GitProvider gitProvider)
      throws UserException {
    String tokenSecretName;
    String usernameSecretName;

    switch (gitProvider) {
      case BITBUCKET:
        tokenSecretName = Constants.BITBUCKET_TOKEN_SECRET_NAME;
        usernameSecretName= Constants.BITBUCKET_USERNAME_SECRET_NAME;
        break;
      case GIT_HUB:
        tokenSecretName = Constants.GITHUB_TOKEN_SECRET_NAME;
        usernameSecretName= Constants.GITHUB_USERNAME_SECRET_NAME;
        break;
      case GIT_LAB:
        tokenSecretName = Constants.GITLAB_TOKEN_SECRET_NAME;
        usernameSecretName= Constants.GITLAB_USERNAME_SECRET_NAME;
        break;
      default:
        throw new IllegalArgumentException("Unsupported git provider");
    }

    secretsController.addOrUpdate(user, usernameSecretName, username, VisibilityType.PRIVATE, null);
    secretsController.addOrUpdate(user, tokenSecretName, token, VisibilityType.PRIVATE, null);
  }

  /**
   * Converts [{"remoteName":"gibson","url":"https://github.com/logicalclocks/hops-hadoop-chef.git"},
   * {"remoteName":"origin","url":"https://github.com/logicalclocks/hops-hadoop-chef.git"}] to List<GitRepositoryRemote>
   */
  public List<GitRepositoryRemote> convertToRemote(GitRepository repository, String remotesJson) {
    List<GitRepositoryRemote> remotes = new ArrayList<>();
    try {
      JSONArray array = new JSONArray(remotesJson);
      for(int i = 0; i < array.length(); i++) {
        JSONObject object = array.getJSONObject(i);
        remotes.add(new GitRepositoryRemote(repository, object.getString("name"), object.getString("url")));
      }
    } catch (JSONException e) {
      LOGGER.log(Level.SEVERE, "Could not convert json string ", e);
    }
    return remotes;
  }

  public String getGitHome(String secret) {
    return settings.getStagingDir() + Settings.PRIVATE_DIRS + secret;
  }
}
