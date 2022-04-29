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
import io.hops.hopsworks.common.dao.git.GitRepositoryFacade;
import io.hops.hopsworks.common.dao.git.GitCommitsFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.git.util.Constants;
import io.hops.hopsworks.common.git.util.GitCommandConfigurationValidator;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.exceptions.HopsSecurityException;

import io.hops.hopsworks.persistence.entity.git.CommitterSignature;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.GitCommit;
import io.hops.hopsworks.persistence.entity.git.GitRepositoryRemote;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;
import io.hops.hopsworks.persistence.entity.git.config.GitCommandType;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.hadoop.fs.Path;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class GitController {
  private static final Logger LOGGER = Logger.getLogger(GitController.class.getName());

  @EJB
  private InodeController inodeController;
  @EJB
  private DistributedFsService dfsService;
  @EJB
  private GitRepositoryFacade gitRepositoryFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private GitCommitsFacade gitCommitsFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private GitRepositoryRemotesFacade gitRepositoryRemotesFacade;
  @EJB
  private GitCommandConfigurationValidator commandConfigurationValidator;
  @EJB
  private GitExecutionController executionController;

  public GitOpExecution clone(CloneCommandConfiguration cloneConfigurationDTO, Project project, Users hopsworksUser)
      throws IllegalArgumentException, GitOpException, HopsSecurityException, DatasetException {
    commandConfigurationValidator.verifyCloneOptions(cloneConfigurationDTO);
    //create the repository dir. The go-git does not create a directory, so we need to create it before
    String fullRepoDirPath = cloneConfigurationDTO.getPath() + File.separator +
            commandConfigurationValidator.getRepositoryName(cloneConfigurationDTO.getUrl());
    DistributedFileSystemOps udfso = dfsService.getDfsOps(hdfsUsersController.getHdfsUserName(project, hopsworksUser));
    try {
      datasetController.createSubDirectory(project, new Path(fullRepoDirPath), udfso);
    } finally {
      //Close the udfso
      dfsService.closeDfsClient(udfso);
    }
    Inode inode = inodeController.getInodeAtPath(fullRepoDirPath);
    GitRepository repository = gitRepositoryFacade.create(inode, project, cloneConfigurationDTO.getProvider(),
        hopsworksUser);
    //Create the default remote
    gitRepositoryRemotesFacade.save(new GitRepositoryRemote(repository, Constants.REPOSITORY_DEFAULT_REMOTE_NAME,
        cloneConfigurationDTO.getUrl()));
    GitCommandConfiguration configuration = new GitCommandConfigurationBuilder().setCommandType(GitCommandType.CLONE)
        .setUrl(cloneConfigurationDTO.getUrl())
        .setProvider(cloneConfigurationDTO.getProvider())
        .setPath(fullRepoDirPath)
        .setBranchName(cloneConfigurationDTO.getBranch())
        .build();
    return executionController.createExecution(configuration, project, hopsworksUser, repository);
  }

  public GitOpExecution executeRepositoryAction(RepositoryActionCommandConfiguration configurationDTO,
                                                Project project, Users hopsworksUser,
                                                GitRepositoryAction action, Integer repositoryId)
      throws GitOpException, HopsSecurityException {
    switch (action) {
      case PULL:
        return pull((PullCommandConfiguration) configurationDTO, project, hopsworksUser, repositoryId);
      case PUSH:
        return push((PushCommandConfiguration) configurationDTO, project, hopsworksUser, repositoryId);
      case STATUS:
        return status(project, hopsworksUser, repositoryId);
      case COMMIT:
        return commit((CommitCommandConfiguration) configurationDTO, project, hopsworksUser, repositoryId);
      default:
        throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_REPOSITORY_ACTION.getMessage());
    }
  }

  public GitOpExecution commit(CommitCommandConfiguration commitConfigurationDTO, Project project,
                               Users hopsworksUser, Integer repositoryId) throws IllegalArgumentException,
      GitOpException, HopsSecurityException {
    commandConfigurationValidator.verifyCommitOptions(commitConfigurationDTO);
    String userFullName = hopsworksUser.getFname() + " " + hopsworksUser.getLname();
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    String repositoryFullPath = inodeController.getPath(repository.getInode());
    GitCommandConfiguration commandConfiguration =
        new GitCommandConfigurationBuilder().setCommandType(GitCommandType.COMMIT)
            .setMessage(commitConfigurationDTO.getMessage())
            .setFiles(commitConfigurationDTO.getFiles())
            .setAll(commitConfigurationDTO.isAll())
            .setCommitter(new CommitterSignature(userFullName, hopsworksUser.getEmail()))
            .setPath(repositoryFullPath)
            .build();
    return executionController.createExecution(commandConfiguration, project, hopsworksUser, repository);
  }

  public GitOpExecution executeBranchAction(GitBranchAction action, Project project, Users hopsworksUser,
                                            Integer repositoryId, String branchName, String commit) 
      throws GitOpException, HopsSecurityException, IllegalArgumentException {
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    String repositoryFullPath = inodeController.getPath(repository.getInode());
    GitCommandConfigurationBuilder builder = new GitCommandConfigurationBuilder();
    builder.setBranchName(branchName);
    builder.setPath(repositoryFullPath);
    switch (action) {
      case CREATE:
      case CREATE_CHECKOUT:
        if (Strings.isNullOrEmpty(branchName)) {
          throw new GitOpException(RESTCodes.GitOpErrorCode.INVALID_BRANCH_NAME, Level.WARNING, "Branch name is empty" +
              ".");
        }
        builder.setCommandType(GitCommandType.CREATE_BRANCH);
        builder.setCheckout(action == GitBranchAction.CREATE_CHECKOUT);
        return executionController.createExecution(builder.build(), project, hopsworksUser, repository);
      case DELETE:
        if (Strings.isNullOrEmpty(branchName)) {
          throw new GitOpException(RESTCodes.GitOpErrorCode.INVALID_BRANCH_NAME, Level.WARNING, "Branch name is empty" +
              ".");
        }
        builder.setCommandType(GitCommandType.DELETE_BRANCH);
        builder.setDeleteOnRemote(false);
        return executionController.createExecution(builder.build(), project, hopsworksUser, repository);
      case CHECKOUT:
      case CHECKOUT_FORCE:
        if (Strings.isNullOrEmpty(branchName) && Strings.isNullOrEmpty(commit)) {
          throw new GitOpException(RESTCodes.GitOpErrorCode.INVALID_BRANCH_AND_COMMIT_CHECKOUT_COMBINATION,
              Level.WARNING, "Please provide either branch or commit to checkout.");
        } else if (!Strings.isNullOrEmpty(branchName) && !Strings.isNullOrEmpty(commit)) {
          throw new GitOpException(RESTCodes.GitOpErrorCode.INVALID_BRANCH_AND_COMMIT_CHECKOUT_COMBINATION,
              Level.WARNING, "Checkout requires a commit or branch but not both.");
        }
        builder.setCommandType(GitCommandType.CHECKOUT);
        builder.setCommit(commit);
        builder.setForce(action == GitBranchAction.CHECKOUT_FORCE);
        return executionController.createExecution(builder.build(), project, hopsworksUser, repository);
      default:
        throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_BRANCH_ACTION.getMessage());
    }
  }

  public GitOpExecution addOrDeleteRemote(GitRemotesAction action, Project project, Users hopsworksUser,
                                          Integer repositoryId, String remoteName, String remoteUrl)
      throws GitOpException, IllegalArgumentException, HopsSecurityException {
    if (Strings.isNullOrEmpty(remoteName)) {
      throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_REMOTE_NAME.getMessage());
    }
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    String repositoryFullPath = inodeController.getPath(repository.getInode());
    GitCommandConfigurationBuilder builder = new GitCommandConfigurationBuilder().setPath(repositoryFullPath)
        .setRemoteName(remoteName);
    switch (action) {
      case ADD:
        if (Strings.isNullOrEmpty(remoteUrl)) {
          throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_REMOTE_URL_PROVIDED.getMessage());
        }
        builder.setCommandType(GitCommandType.ADD_REMOTE);
        builder.setRemoteUrl(remoteUrl);
        return executionController.createExecution(builder.build(), project, hopsworksUser, repository);
      case DELETE:
        builder.setCommandType(GitCommandType.DELETE_REMOTE);
        return executionController.createExecution(builder.build(), project, hopsworksUser, repository);
      default:
        throw new IllegalArgumentException(RESTCodes.GitOpErrorCode.INVALID_REMOTES_ACTION.getMessage());
    }
  }

  public GitOpExecution push(PushCommandConfiguration configurationDTO, Project project, Users hopsworksUser,
                             Integer repositoryId) throws GitOpException, HopsSecurityException, 
      IllegalArgumentException {
    commandConfigurationValidator.verifyRemoteNameAndBranch(configurationDTO.getRemoteName(),
        configurationDTO.getBranchName());
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    String repositoryFullPath = inodeController.getPath(repository.getInode());
    GitCommandConfiguration pushCommandConfiguration = new GitCommandConfigurationBuilder()
        .setCommandType(GitCommandType.PUSH)
        .setRemoteName(configurationDTO.getRemoteName())
        .setBranchName(configurationDTO.getBranchName())
        .setForce(configurationDTO.isForce())
        .setPath(repositoryFullPath)
        .build();
    return executionController.createExecution(pushCommandConfiguration, project, hopsworksUser, repository);
  }

  public GitOpExecution pull(PullCommandConfiguration configDTO, Project project, Users hopsworksUser,
                             Integer repositoryId) throws GitOpException, HopsSecurityException,
      IllegalArgumentException {
    String userFullName = hopsworksUser.getFname() + " " + hopsworksUser.getLname();
    commandConfigurationValidator.verifyRemoteNameAndBranch(configDTO.getRemoteName(), configDTO.getBranchName());
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    String repositoryFullPath = inodeController.getPath(repository.getInode());
    GitCommandConfiguration pullCommandConfiguration =
        new GitCommandConfigurationBuilder().setCommandType(GitCommandType.PULL)
            .setRemoteName(configDTO.getRemoteName())
            .setForce(configDTO.isForce())
            .setBranchName(configDTO.getBranchName())
            .setPath(repositoryFullPath)
            .setCommitter(new CommitterSignature(userFullName, hopsworksUser.getEmail()))
            .build();
    return executionController.createExecution(pullCommandConfiguration, project, hopsworksUser, repository);
  }

  public GitOpExecution status(Project project, Users hopsworksUser, Integer repositoryId)
      throws GitOpException, HopsSecurityException {
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    String repositoryFullPath = inodeController.getPath(repository.getInode());
    GitCommandConfiguration statusCommandConfig =
        new GitCommandConfigurationBuilder()
            .setCommandType(GitCommandType.STATUS)
            .setPath(repositoryFullPath)
            .build();
    return executionController.createExecution(statusCommandConfig, project, hopsworksUser, repository);
  }

  public GitOpExecution fileCheckout(Project project, Users hopsworksUser, Integer repositoryId,
                                     List<String> filePaths) throws GitOpException, HopsSecurityException {
    if (filePaths.isEmpty()) {
      throw new IllegalArgumentException("File paths are empty.");
    }
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    String repositoryFullPath = inodeController.getPath(repository.getInode());
    GitCommandConfiguration fileCheckoutConfiguration =
        new GitCommandConfigurationBuilder()
            .setCommandType(GitCommandType.FILE_CHECKOUT)
            .setPath(repositoryFullPath)
            .setFiles(filePaths)
            .build();
    return executionController.createExecution(fileCheckoutConfiguration, project, hopsworksUser, repository);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateBranchCommits(Project project, BranchCommits commits, Integer repositoryId,
                                  String branchName) throws GitOpException {
    if (Strings.isNullOrEmpty(branchName)) {
      throw new IllegalArgumentException("Branch name cannot be null");
    }
    GitRepository repository = commandConfigurationValidator.verifyRepository(project, repositoryId);
    //delete all
    gitCommitsFacade.deleteAllInBranchAndRepository(branchName, repository);
    //create new entries
    for (GitCommit commit: commits.getCommits()) {
      commit.setBranch(branchName);
      commit.setRepository(repository);
      gitCommitsFacade.create(commit);
    }
  }
}
