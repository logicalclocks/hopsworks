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
package io.hops.hopsworks.common.git;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.git.GitRepositoryRemotesFacade;
import io.hops.hopsworks.common.dao.git.GitOpExecutionFacade;
import io.hops.hopsworks.common.dao.git.GitRepositoryFacade;
import io.hops.hopsworks.common.dao.git.GitCommitsFacade;
import io.hops.hopsworks.common.dao.git.GitPaths;
import io.hops.hopsworks.common.git.util.GitCommandConfigurationValidator;
import io.hops.hopsworks.common.git.util.GitCommandOperationUtil;
import io.hops.hopsworks.common.git.util.GitContainerArgumentsWriter;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandType;
import io.hops.hopsworks.persistence.entity.git.config.GitOpExecutionState;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.commons.codec.digest.DigestUtils;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class GitExecutionController {
  private static final Logger LOGGER = Logger.getLogger(GitExecutionController.class.getName());
  @EJB
  private Settings settings;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private GitJWTManager gitJWTManager;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private GitOpExecutionFacade gitOpExecutionFacade;
  @EJB
  private AsynchronousGitCommandExecutor gitCommandExecutor;
  @EJB
  private GitCommandOperationUtil gitCommandOperationUtil;
  @EJB
  private GitRepositoryFacade gitRepositoryFacade;
  @EJB
  private GitCommitsFacade gitCommitsFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private GitRepositoryRemotesFacade gitRepositoryRemotesFacade;
  @EJB
  private GitCommandConfigurationValidator commandConfigurationValidator;
  @EJB
  private GitContainerArgumentsWriter argumentsWriter;

  /**
   * initializes the execution of all git commands
   *
   * @param gitCommandConfiguration
   * @param project
   * @param hopsworksUser
   * @param repository
   * @return
   * @throws HopsSecurityException
   * @throws GitOpException
   */
  public GitOpExecution createExecution(GitCommandConfiguration gitCommandConfiguration, Project project,
                                        Users hopsworksUser, GitRepository repository)
      throws HopsSecurityException, GitOpException {
    commandConfigurationValidator.verifyReadOnly(gitCommandConfiguration.getCommandType(), repository);
    gitCommandConfiguration.setReadOnly(settings.getEnableGitReadOnlyRepositories());
    String hdfsUsername = hdfsUsersController.getHdfsUserName(project, hopsworksUser);
    //set the provider to validate secrets for some commands
    gitCommandConfiguration.setProvider(repository.getGitProvider());
    BasicAuthSecrets authSecrets = gitCommandOperationUtil.getAuthenticationSecrets(hopsworksUser,
        repository.getGitProvider());
    commandConfigurationValidator.validateProviderConfiguration(authSecrets, gitCommandConfiguration);
    String configSecret = DigestUtils.sha256Hex(Integer.toString(ThreadLocalRandom.current().nextInt()));
    lockRepository(repository.getId());
    GitOpExecution gitOpExecution = null;
    DistributedFileSystemOps  udfso = null;
    try {
      udfso = dfsService.getDfsOps(hdfsUsername);
      GitPaths gitPaths = prepareCommandExecution(project, hopsworksUser, udfso, configSecret);
      gitOpExecution = gitOpExecutionFacade.create(gitCommandConfiguration, hopsworksUser, repository, configSecret);
      argumentsWriter.createArgumentFile(gitOpExecution, gitPaths, authSecrets);
      gitCommandExecutor.execute(gitOpExecution, gitPaths);
      return gitOpExecution;
    } catch (Exception ex) {
      gitRepositoryFacade.updateRepositoryCid(repository, null);
      gitCommandOperationUtil.cleanUp(project, hopsworksUser, configSecret);
      if (ex instanceof IOException) {
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_MATERIALIZATION_ERROR, Level.SEVERE,
            ex.getMessage(), null, ex);
      }
      throw new GitOpException(RESTCodes.GitOpErrorCode.GIT_OPERATION_ERROR, Level.SEVERE, ex.getMessage());
    } finally {
      if (udfso != null) {
        dfsService.closeDfsClient(udfso);
      }
    }
  }

  /**
   * Generate the git paths, materialize user certificates, and jwt
   *
   * @param project
   * @param hopsworksUser
   * @param dfso
   * @throws GitOpException
   * @throws IOException
   */
  public GitPaths prepareCommandExecution(Project project, Users hopsworksUser, DistributedFileSystemOps dfso,
                                          String configSecret) throws GitOpException, IOException {
    GitPaths gitPaths = new GitPaths(settings.getStagingDir() + settings.PRIVATE_DIRS, configSecret);
    gitCommandOperationUtil.generatePaths(gitPaths);
    HopsUtils.materializeCertificatesForUserCustomDir(project.getName(), hopsworksUser.getUsername(),
        settings.getHdfsTmpCertDir(), dfso, certificateMaterializer, settings, gitPaths.getCertificatesDirPath());
    gitJWTManager.materializeJWT(hopsworksUser, gitPaths.getTokenPath());
    return gitPaths;
  }

  private synchronized void lockRepository(Integer repositoryId) throws GitOpException {
    Optional<GitRepository> optional = gitRepositoryFacade.findById(repositoryId);
    if (!optional.isPresent()) {
      throw new GitOpException(RESTCodes.GitOpErrorCode.REPOSITORY_NOT_FOUND, Level.SEVERE, "Git " +
          "repository with id [" + optional + "] was not found.");
    } else {
      GitRepository repository = optional.get();
      if (repository.getCid() != null) {
        throw new GitOpException(RESTCodes.GitOpErrorCode.GIT_OPERATION_ERROR, Level.WARNING,
            "There is another ongoing operation in the repository.");
      }
      //lock repository
      repository.setCid(String.valueOf(System.currentTimeMillis()));
      gitRepositoryFacade.updateRepository(repository);
    }
  }

  public GitOpExecution getExecutionInRepository(GitRepository repository, Integer executionId) throws GitOpException {
    Optional<GitOpExecution> executionObj = gitOpExecutionFacade.findByIdAndRepository(repository, executionId);
    if (!executionObj.isPresent()) {
      throw new GitOpException(RESTCodes.GitOpErrorCode.EXECUTION_OBJECT_NOT_FOUND, Level.FINE, "Could not find " +
          "execution with id " + executionId );
    }
    return executionObj.get();
  }

  public GitOpExecution updateGitExecutionState(Project project, Users hopsworksUser,
                                                GitCommandExecutionStateUpdateDTO stateDTO, Integer repositoryId,
                                                Integer executionId)
      throws IllegalArgumentException, GitOpException {
    GitOpExecutionState newState = stateDTO.getExecutionState();
    if (newState == null) {
      throw new IllegalArgumentException("Invalid git execution state. Execution state cannot be null.");
    }
    LOGGER.log(Level.INFO, "Updating execution, Id = " + executionId + " to " + newState.getExecutionState());
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, hopsworksUser, repositoryId);
    GitOpExecution exec = getExecutionInRepository(repository, executionId);
    exec.setCommandResultMessage(stateDTO.getMessage());
    if (newState.isFinalState()) {
      if (newState == GitOpExecutionState.SUCCESS) {
        //Every successful operation should update the repository current commit and branch
        repository.setCurrentBranch(stateDTO.getBranch());
        repository.setCurrentCommit(stateDTO.getCommitHash());
        GitCommandConfiguration executedCommandConfig = exec.getGitCommandConfiguration();
        if (executedCommandConfig.getCommandType() == GitCommandType.DELETE_BRANCH) {
          //if we deleted a branch then we should also delete all the commits for this branch
          gitCommitsFacade.deleteAllInBranchAndRepository(executedCommandConfig.getBranchName(), repository);
        }
        if (executedCommandConfig.getCommandType() == GitCommandType.ADD_REMOTE ||
            executedCommandConfig.getCommandType() == GitCommandType.DELETE_REMOTE) {
          //Update the remotes which are in the execution final message
          String remotesJson = exec.getCommandResultMessage();
          if (!Strings.isNullOrEmpty(remotesJson)) {
            gitRepositoryRemotesFacade.updateRepositoryRemotes(gitCommandOperationUtil.convertToRemote(repository,
                remotesJson), repository);
          }
        }
      }
      gitRepositoryFacade.updateRepositoryCid(repository, null);
      gitCommandOperationUtil.cleanUp(project, hopsworksUser, exec.getConfigSecret());
    }
    return gitOpExecutionFacade.updateState(exec, newState, stateDTO.getMessage());
  }
}
