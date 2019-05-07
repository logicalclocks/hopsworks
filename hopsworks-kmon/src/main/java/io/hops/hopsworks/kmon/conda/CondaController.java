/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved

 * Hopsworks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.kmon.conda;

import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.common.dao.python.CondaCommands;
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.LocalhostServices;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.ProcessDescriptor;
import io.hops.hopsworks.common.util.ProcessResult;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.ServiceException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@RequestScoped
public class CondaController implements Serializable {

  @EJB
  private CondaCommandFacade condaCommandFacade;
  @EJB
  private CommandsController commandsController;
  @EJB
  private EnvironmentController environmentController;
  @EJB
  private Settings settings;
  @EJB
  private OSProcessExecutor osProcessExecutor;

  private List<CondaCommands> failedCommands;
  private List<CondaCommands> ongoingCommands;
  private List<CondaCommands> newCommands;

  private String output;
  
  private String diskUsage = "Press button to calculate.";

  private boolean showFailed = true;
  private boolean showNew = true;
  private boolean showOngoing = true;

  private static final Logger logger = Logger.getLogger(CondaController.class.getName());

  public CondaController() {

  }

  @PostConstruct
  public void init() {
    logger.info("init CondaController");
    loadCommands();
  }

  public void deleteAllFailedCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaCommandFacade.CondaStatus.FAILED);
    message("deleting all commands with the state 'failed'");
    loadFailedCommands();
  }

  public void deleteAllOngoingCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaCommandFacade.CondaStatus.ONGOING);
    message("deleting all commands with the state 'ongoing'");
    loadOngoingCommands();
  }

  public void deleteAllNewCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaCommandFacade.CondaStatus.NEW);
    message("deleting all commands with the state 'new'");
    loadNewCommands();
  }

  public void deleteCommand(CondaCommands command) {
    condaCommandFacade.removeCondaCommand(command.getId());
    message("deleting");
    loadCommands();
  }

  public void execCommand(CondaCommands command) throws ServiceException {
    // ssh to the host, run the command, print out the results to the terminal.

    try {
      if (command.getStatus() != CondaCommandFacade.CondaStatus.FAILED) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO,
            "You can only execute failed commands.", "Not executed");
        FacesContext.getCurrentInstance().addMessage(null, message);
        return;
      }

      if (command.getOp() == null) {
        this.output = "Conda command was null. Report a bug.";
      } else {
        message("executing");
        CondaCommandFacade.CondaOp op = command.getOp();
        if (op.isEnvOp()) {
          // anaconda environment command: <host> <op> <proj> <arg> <offline> <hadoop_home>
          String prog = settings.getHopsworksDomainDir() + "/bin/anaconda-command-ssh.sh";
          String hostname = command.getHostId().getHostIp();
          String projectName = command.getProj();
          String arg = command.getArg();
          String offline = "";
          String hadoopHome = settings.getHadoopSymbolicLinkDir();
          ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
              .addCommand(prog)
              .addCommand(hostname)
              .addCommand(op.toString())
              .addCommand(projectName)
              .addCommand(arg)
              .addCommand(offline)
              .addCommand(hadoopHome)
              .redirectErrorStream(true)
              .setWaitTimeout(10L, TimeUnit.MINUTES)
              .build();

          logger.log(Level.FINE, "Executing " + processDescriptor.toString());
  
          ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
          
          if (!processResult.processExited()) {
            this.output = "COMMAND TIMED OUT: \r\n" + processResult.getStdout();
            return;
          }
          
          if (processResult.getExitCode() == 0) {
            // delete from conda_commands tables
            command.setStatus(CondaCommandFacade.CondaStatus.SUCCESS);
            condaCommandFacade.removeCondaCommand(command.getId());


            this.output = "SUCCESS. \r\n" + processResult.getStdout();
            commandsController.updateCondaCommandStatus(command.getId(), CondaCommandFacade.CondaStatus.SUCCESS,
                command.getInstallType(),

              command.getMachineType(), command.getArg(), command.getProj(), command.getOp(), command.getLib(),
              command.getVersion(), command.getChannelUrl());

          } else {
            this.output = "FAILED. \r\n" + processResult.getStdout();
          }
        } else { // Conda operation
          String prog = settings.getHopsworksDomainDir() + "/bin/conda-command-ssh.sh";
          String hostname = command.getHostId().getHostIp();
          String projectName = command.getProj();
          String channelUrl = command.getChannelUrl();
          
          ProcessDescriptor processDescriptor = new ProcessDescriptor.Builder()
              .addCommand(prog)
              .addCommand(hostname)
              .addCommand(op.toString())
              .addCommand(projectName)
              .addCommand(channelUrl)
              .addCommand(command.getInstallType().toString())
              .addCommand(command.getLib())
              .addCommand(command.getVersion())
              .redirectErrorStream(true)
              .build();
          logger.log(Level.FINE, "Executing: {0}", processDescriptor.toString());
          
          ProcessResult processResult = osProcessExecutor.execute(processDescriptor);
          if (!processResult.processExited()) {
            this.output = "COMMAND TIMED OUT: \r\n" + processResult.getStdout();
            return;
          }
          if (processResult.getExitCode() == 0) {
            // delete from conda_commands tables
            command.setStatus(CondaCommandFacade.CondaStatus.SUCCESS);

            condaCommandFacade.removeCondaCommand(command.getId());
            this.output = "SUCCESS. \r\n" + processResult.getStdout();
            commandsController.updateCondaCommandStatus(command.getId(), CondaCommandFacade.CondaStatus.SUCCESS,
                command.getInstallType(),
              command.getMachineType(), command.getArg(), command.getProj(), command.getOp(), command.getLib(),
              command.getVersion(), command.getChannelUrl());
            loadCommands();
          } else {
            this.output = "FAILED. \r\n" + processResult.getStdout();
          }
        }
      }
      logger.log(Level.INFO, "Output: {0}", this.output);

    } catch (IOException ex) {
      Logger.getLogger(HopsUtils.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

  public List<CondaCommands> getFailedCondaCommands() {
    loadFailedCommands();
    return failedCommands;
  }

  public List<CondaCommands> getOngoingCondaCommands() {
    loadOngoingCommands();
    return ongoingCommands;
  }

  public List<CondaCommands> getNewCondaCommands() {
    loadNewCommands();
    return newCommands;
  }

  private void loadFailedCommands() {
    failedCommands = condaCommandFacade.findByStatus(CondaCommandFacade.CondaStatus.FAILED);
    if (failedCommands == null) {
      failedCommands = new ArrayList<>();
    }
  }

  private void loadOngoingCommands() {
    ongoingCommands = condaCommandFacade.findByStatus(CondaCommandFacade.CondaStatus.ONGOING);
    if (ongoingCommands == null) {
      ongoingCommands = new ArrayList<>();
    }
  }

  private void loadNewCommands() {
    newCommands = condaCommandFacade.findByStatus(CondaCommandFacade.CondaStatus.NEW);
    if (newCommands == null) {
      newCommands = new ArrayList<>();
    }
  }

  private void loadCommands() {
    loadNewCommands();
    loadOngoingCommands();
    loadFailedCommands();
  }

  public List<CondaCommands> getFailedCommands() {
    return failedCommands;
  }

  public List<CondaCommands> getOngoingCommands() {
    return ongoingCommands;
  }

  public List<CondaCommands> getNewCommands() {
    return newCommands;
  }

  public String getOutput() {
    if (!isOutput()) {
      return "No Output to show for command executions.";
    }
    return this.output;
  }

  public boolean isOutput() {
    if (this.output == null || this.output.isEmpty()) {
      return false;
    }
    return true;
  }

  public boolean isShowFailed() {
    return showFailed;
  }

  public boolean isShowNew() {
    return showNew;
  }

  public boolean isShowOngoing() {
    return showOngoing;
  }

  public void setShowFailed(boolean showFailed) {
    this.showFailed = showFailed;
  }

  public void setShowNew(boolean showNew) {
    this.showNew = showNew;
  }

  public void setShowOngoing(boolean showOngoing) {
    this.showOngoing = showOngoing;
  }

  public void setShowAll(boolean show) {
    this.showOngoing = show;
    this.showFailed = show;
    this.showNew = show;
  }

  public boolean getShowAll() {
    return (this.showOngoing || this.showFailed || this.showNew);
  }

  public void message(String msg) {
    FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Conda Command " + msg));
  }

  public String calculateDiskUsage() {
    try {
      diskUsage = LocalhostServices.du(osProcessExecutor, new File(settings.getAnacondaDir()));
    } catch (Exception e) {
      logger.warning("Problem calculating disk usage for Anaconda");
      logger.warning(e.getMessage());
      message("Error in calculating disk usage for anaconda");
      diskUsage = "Error calculating disk usage. See Hopsworks logs.";
    }
    return diskUsage;
  }
  
  public void cleanupConda() {
    try {
      environmentController.cleanupConda();
    } catch (Exception e) {
      logger.warning("Problem cleaning up Conda");
      logger.warning(e.getMessage());
      message(e.getMessage());
    }
  }
  
  public void cleanupAnaconda() {
  }

  public String getDiskUsage() {
    return diskUsage;
  }

}
