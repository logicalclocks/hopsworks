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
import io.hops.hopsworks.common.dao.git.GitRepositoryFacade;
import io.hops.hopsworks.common.git.util.GitCommandOperationUtil;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.util.templates.git.GitContainerLaunchScriptArgumentsTemplate;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.common.dao.git.GitOpExecutionFacade;
import io.hops.hopsworks.common.dao.git.GitPaths;

import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.config.GitOpExecutionState;

import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
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
@LocalBean
public class AsynchronousGitCommandExecutor {
  private static final Logger LOGGER = Logger.getLogger(AsynchronousGitCommandExecutor.class.getName());
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

  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void execute(GitOpExecution gitOpExecution, GitPaths gitPaths) {
    int maxTries = 5;
    String pid = "";
    String gitCommand = gitOpExecution.getGitCommandConfiguration().getCommandType().getGitCommand();
    String prog = settings.getSudoersDir() + "/git.sh";
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
              "Could not start git service to execute command " + gitCommand +  " . " + "Exit code: " +
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

  @Asynchronous
  @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
  public void cancelGitExecution(GitOpExecution execution, String message) {
    if (execution.getState().isFinalState()) {
      return;
    }
    try {
      gitOpExecutionFacade.updateState(execution, GitOpExecutionState.CANCELLED, message);
      GitRepository repository = execution.getRepository();
      int maxTries = 10; // wait time if the container is not launched
      while (maxTries > 0 && org.opensearch.common.Strings.isNullOrEmpty(repository.getCid())) {
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
      gitCommandOperationUtil.shutdownCommandService(repository, execution);
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error when cancelling git execution with ID: " + execution.getId(), e);
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
    LOGGER.log(Level.SEVERE, "Problem executing shell script to start git command service." );
  }
}
