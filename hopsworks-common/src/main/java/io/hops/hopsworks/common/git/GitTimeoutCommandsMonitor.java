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

import io.hops.hopsworks.common.dao.git.GitOpExecutionFacade;
import io.hops.hopsworks.common.dao.git.GitRepositoryFacade;
import io.hops.hopsworks.common.git.util.GitCommandOperationUtil;
import io.hops.hopsworks.common.util.PayaraClusterManager;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.git.GitOpExecution;
import io.hops.hopsworks.persistence.entity.git.GitRepository;
import io.hops.hopsworks.persistence.entity.git.config.GitOpExecutionState;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@Startup
@DependsOn("Settings")
public class GitTimeoutCommandsMonitor {
  private static final Logger LOGGER = Logger.getLogger(GitTimeoutCommandsMonitor.class.getName());
  //Time to wait to kill the container after it has reached timeout. 90 seconds
  private static final Integer BONUS_TIME = 1000 * 90;
  //Time to wait after the repository is locked and the execution is created. 30s;
  private static final Integer WAIT_TIME_BEFORE_EXECUTION_OBJECT_CREATION = 3000;

  @EJB
  private GitOpExecutionFacade gitOpExecutionFacade;
  @EJB
  private GitRepositoryFacade gitRepositoryFacade;
  @EJB
  private GitCommandOperationUtil gitCommandOperationUtil;
  @EJB
  private Settings settings;
  @EJB
  private PayaraClusterManager payaraClusterManager;

  @Resource
  private TimerService timerService;
  private Timer timer;
  private String localMemberIp;

  @PostConstruct
  public void init() {
    localMemberIp = payaraClusterManager.getLocalIp();
    //number of milliseconds that must elapse between timer expiration notifications
    long intervalDuration = 60000L; // 1 min
    timer = timerService.createIntervalTimer(0, intervalDuration, new TimerConfig("Git Commands Monitor timer",
      false));
  }

  @PreDestroy
  public void destroy() {
    if (timer != null) {
      timer.cancel();
    }
  }

  @Timeout
  public void gitCommandMonitor(Timer timer) {
    //Should run on all
    LOGGER.log(Level.FINE, "Running GitTimeoutCommandsMonitor");
    Collection<GitRepository> repositories = gitRepositoryFacade.findAllWithOngoingOperations();
    for (GitRepository repository : repositories) {
      Optional<GitOpExecution> optional = gitOpExecutionFacade.findRunningInRepository(repository);
      if (optional.isPresent()) {
        GitOpExecution execution = optional.get();
        //we need all GitOpExecution to check for repository with a pid but no execution. So check host here
        if (execution.getHostIp().equals(localMemberIp)) {
          long timeElapsed = System.currentTimeMillis() - execution.getExecutionStart();
          if (timeElapsed > (settings.getGitJwtExpMs() + BONUS_TIME)) {
            //kill this container
            LOGGER.log(Level.INFO, "Killing git execution with Id + [{0}] with state {1}",
              new Object[]{execution.getId(), execution.getState().toString()});
            gitOpExecutionFacade.updateState(execution, GitOpExecutionState.TIMEDOUT, "Timeout");
            gitCommandOperationUtil.shutdownCommandService(repository, execution);
          }
        }
      } else if (payaraClusterManager.amIThePrimary()) {
        //A repository with a pid but no execution object
        try {
          long executionStart = Long.parseLong(repository.getCid());
          long timeElapsed = System.currentTimeMillis() - executionStart;
          if (timeElapsed > WAIT_TIME_BEFORE_EXECUTION_OBJECT_CREATION) {
            LOGGER.log(Level.INFO, "Failed to create execution in repository with Id + [{0}] ", repository.getId());
            gitRepositoryFacade.updateRepositoryCid(repository, null);
          }
        } catch (NumberFormatException e) {
          //It is probably the container id
          gitRepositoryFacade.updateRepositoryCid(repository, null);
        }
      }
    }
  }
}
