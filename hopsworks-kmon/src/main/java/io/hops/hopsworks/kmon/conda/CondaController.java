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
import io.hops.hopsworks.common.python.commands.CommandsController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.util.LocalhostServices;
import io.hops.hopsworks.common.util.OSProcessExecutor;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
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
    logger.fine("init CondaController");
    loadCommands();
  }

  public void deleteAllFailedCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaStatus.FAILED);
    message("deleting all commands with the state 'failed'");
    loadFailedCommands();
  }

  public void deleteAllOngoingCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaStatus.ONGOING);
    message("deleting all commands with the state 'ongoing'");
    loadOngoingCommands();
  }

  public void deleteAllNewCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaStatus.NEW);
    message("deleting all commands with the state 'new'");
    loadNewCommands();
  }

  public void deleteCommand(CondaCommands command) {
    condaCommandFacade.removeCondaCommand(command.getId());
    message("deleting");
    loadCommands();
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
    failedCommands = condaCommandFacade.findByStatus(CondaStatus.FAILED);
    if (failedCommands == null) {
      failedCommands = new ArrayList<>();
    }
  }

  private void loadOngoingCommands() {
    ongoingCommands = condaCommandFacade.findByStatus(CondaStatus.ONGOING);
    if (ongoingCommands == null) {
      ongoingCommands = new ArrayList<>();
    }
  }

  private void loadNewCommands() {
    newCommands = condaCommandFacade.findByStatus(CondaStatus.NEW);
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
  
  
  public void cleanupAnaconda() {
  }

  public String getDiskUsage() {
    return diskUsage;
  }

}
