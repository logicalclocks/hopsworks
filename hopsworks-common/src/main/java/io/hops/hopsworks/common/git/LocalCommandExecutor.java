/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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
import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.common.dao.git.GitOpExecutionFacade;
import io.hops.hopsworks.common.dao.git.GitPaths;
import io.hops.hopsworks.common.dao.git.GitRepositoryFacade;
import io.hops.hopsworks.common.git.util.GitCommandOperationUtil;
import io.hops.hopsworks.common.git.util.GitContainerArgumentsWriter;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.integrations.LocalhostStereotype;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.templates.git.GitContainerLaunchScriptArgumentsTemplate;
import io.hops.hopsworks.exceptions.GitOpException;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.config.GitOpExecutionState;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@LocalhostStereotype
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalCommandExecutor implements CommandExecutor {
  private static final Logger LOGGER = Logger.getLogger(LocalCommandExecutor.class.getName());
  @EJB
  private Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;
  @EJB
  private GitOpExecutionFacade gitOpExecutionFacade;
  @EJB
  private GitCommandOperationUtil gitCommandOperationUtil;
  @EJB
  private GitRepositoryFacade gitRepositoryFacade;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private GitContainerArgumentsWriter argumentsWriter;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private GitJWTManager gitJWTManager;
  @EJB
  private DistributedFsService dfsService;

  @Override
  public void execute(GitOpExecution gitOpExecution, BasicAuthSecrets authSecrets) throws GitOpException, IOException,
      ServiceDiscoveryException {
    int maxTries = 5;
    String pid = "";
    String gitCommand = gitOpExecution.getGitCommandConfiguration().getCommandType().getGitCommand();
    String prog = settings.getSudoersDir() + "/git.sh";

    GitPaths gitPaths = prepareCommandExecution(gitOpExecution);
    argumentsWriter.createArgumentFile(gitOpExecution, gitPaths, authSecrets);
    String commandArgumentsFile = gitPaths.getConfDirPath() + File.separator
      + GitContainerLaunchScriptArgumentsTemplate.FILE_NAME;

    while (maxTries > 0 && Strings.isNullOrEmpty(pid)) {
      try {
        ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
          .addCommand("/usr/bin/sudo")
          .addCommand(prog)
          .addCommand("start")
          .addCommand(commandArgumentsFile)
          .redirectErrorStream(true)
          .setCurrentWorkingDirectory(new File(gitPaths.getGitPath()))
          .setWaitTimeout(60L, TimeUnit.SECONDS)
          .build();

        String pidFile = gitPaths.getRunDirPath() + "/git.pid";
        ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
        if (processResult.getExitCode() != 0) {
          String errorMsg =
            "Could not start git service to execute command " + gitCommand + " . " + "Exit code: " +
              processResult.getExitCode() + " Error: stdout: " + processResult.getStdout() + " stderr: " +
              processResult.getStderr();
          LOGGER.log(Level.SEVERE, errorMsg);
          throw new IOException(errorMsg);
        } else {
          pid = com.google.common.io.Files.readFirstLine(new File(pidFile), Charset.defaultCharset());
          //Get the updated repository
          Optional<GitRepository> optional = gitRepositoryFacade.findById(gitOpExecution.getRepository().getId());
          gitRepositoryFacade.updateRepositoryCid(optional.get(), pid);
          //gitOpExecutionFacade.updateState(gitOpExecution, GitOpExecutionState.SUBMITTED);
        }
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Problem executing shell script to start git command service", ex);
        maxTries--;
      }
    }
    if (Strings.isNullOrEmpty(pid)) {
      updateExecutionStateToFail(gitOpExecution);
    }
  }

  @Override
  public void cancelGitExecution(GitOpExecution execution, String message) {
    if (execution.getState().isFinalState()) {
      return;
    }
    try {
      gitOpExecutionFacade.updateState(execution, GitOpExecutionState.CANCELLED, message);
      GitRepository repository = execution.getRepository();
      int maxTries = 10; // wait time if the container is not launched
      while (maxTries > 0 && Strings.isNullOrEmpty(repository.getCid())) {
        Optional<GitRepository> optional = gitRepositoryFacade.findById(repository.getId());
        if (optional.isPresent()) {
          repository = optional.get();
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
          LOGGER.log(Level.INFO, "Interrupted while waiting for the git container to start");
        }
        maxTries--;
      }
      shutdownCommandService(repository, execution);
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error when cancelling git execution with ID: " + execution.getId(), e);
    }
  }

  /**
   * Generate the git paths, materialize user certificates, and jwt
   *
   * @throws GitOpException
   * @throws IOException
   */
  private GitPaths prepareCommandExecution(GitOpExecution gitOpExecution) throws GitOpException, IOException {
    GitPaths gitPaths =
      new GitPaths(settings.getStagingDir() + Settings.PRIVATE_DIRS, gitOpExecution.getConfigSecret());
    gitCommandOperationUtil.generatePaths(gitPaths);
    DistributedFileSystemOps udfso = null;
    String hdfsUsername = hdfsUsersController.getHdfsUserName(gitOpExecution.getRepository().getProject(),
      gitOpExecution.getUser());
    try {
      udfso = dfsService.getDfsOps(hdfsUsername);
      HopsUtils.materializeCertificatesForUserCustomDir(gitOpExecution.getRepository().getProject().getName(),
        gitOpExecution.getUser().getUsername(), settings.getHdfsTmpCertDir(), udfso, certificateMaterializer, settings,
        gitPaths.getCertificatesDirPath());
      gitJWTManager.materializeJWT(gitOpExecution.getUser(), gitPaths.getTokenPath());
    } finally {
      if (udfso != null) {
        dfsService.closeDfsClient(udfso);
      }
    }
    return gitPaths;
  }

  private void shutdownCommandService(GitRepository repository, GitOpExecution execution) {
    String cid = repository.getCid();
    try {
      gitRepositoryFacade.updateRepositoryCid(repository, null);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to update repository pid", e);
    }
    killGitContainer(execution, cid);
    gitCommandOperationUtil.cleanUp(repository.getProject(), execution.getUser(), execution.getConfigSecret());
  }

  @Override
  public void monitorCommands(GitOpExecution execution, String localMemberIp, boolean amIPrimary, long waitTime) {
    if (execution.getHostname().equals(localMemberIp)) {
      long timeElapsed = System.currentTimeMillis() - execution.getExecutionStart();
      if (timeElapsed > (settings.getGitJwtExpMs() + waitTime)) {
        //kill this container
        LOGGER.log(Level.INFO, "Killing git execution with Id + [{0}] with state {1}",
          new Object[]{execution.getId(), execution.getState().toString()});
        gitOpExecutionFacade.updateState(execution, GitOpExecutionState.TIMEDOUT, "Timeout");
        shutdownCommandService(execution.getRepository(), execution);
      }
    }
  }

  private void killGitContainer(GitOpExecution execution, String containerId) {
    if (Strings.isNullOrEmpty(containerId)) {
      return;
    }
    String gitHomePath = gitCommandOperationUtil.getGitHome(execution.getConfigSecret());
    String hdfsUsername = hdfsUsersController.getHdfsUserName(execution.getRepository().getProject(),
      execution.getUser());
    String prog = settings.getSudoersDir() + "/git.sh";
    int exitValue = 0;
    ProcessDescriptor.Builder pdBuilder = new ProcessDescriptor.Builder()
      .addCommand("/usr/bin/sudo")
      .addCommand(prog)
      .addCommand("kill")
      .addCommand(gitHomePath)
      .addCommand(containerId)
      .addCommand(hdfsUsername)
      .redirectErrorStream(true)
      .setWaitTimeout(10L, TimeUnit.SECONDS);
    try {
      ProcessResult processResult = osProcessExecutor.execute(pdBuilder.build());
      LOGGER.log(Level.FINE, processResult.getStdout());
      exitValue = processResult.getExitCode();
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE,
        "Failed to shutdown git container executing command for user " + hdfsUsername, ex);
    }
    if (exitValue != 0) {
      LOGGER.log(Level.SEVERE,
        "Exited with " + exitValue + "Failed to shutdown git container executing command for user "
          + hdfsUsername);
    }
  }

  private void updateExecutionStateToFail(GitOpExecution gitOpExecution) {
    // Get latest execution object
    Optional<GitOpExecution> optional = gitOpExecutionFacade.findByIdAndRepository(gitOpExecution.getRepository(),
      gitOpExecution.getId());
    gitOpExecution = optional.get();
    if (gitOpExecution.getState() == GitOpExecutionState.CANCELLED) {
      return;
    }
    gitCommandOperationUtil.cleanUp(gitOpExecution.getRepository().getProject(),
      gitOpExecution.getUser(), gitOpExecution.getConfigSecret());
    gitOpExecutionFacade.updateState(gitOpExecution, GitOpExecutionState.FAILED, "Could not launch " +
      "container to execute git command.");
    gitRepositoryFacade.updateRepositoryCid(gitOpExecution.getRepository(), null);
    LOGGER.log(Level.SEVERE, "Problem executing shell script to start git command service.");
  }
}
