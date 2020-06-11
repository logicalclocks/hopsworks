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

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.python.CondaCommandFacade;
import io.hops.hopsworks.persistence.entity.python.CondaCommands;
import io.hops.hopsworks.persistence.entity.python.CondaStatus;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@ManagedBean
@RequestScoped
public class CondaController implements Serializable {

  @EJB
  private CondaCommandFacade condaCommandFacade;

  private List<CondaCommands> failedCommands;
  private List<CondaCommands> ongoingCommands;
  private List<CondaCommands> newCommands;

  private boolean showFailed = true;
  private boolean showNew = true;
  private boolean showOngoing = true;

  private String output;

  private static final Logger logger = Logger.getLogger(CondaController.class.getName());

  public CondaController() {}

  @PostConstruct
  public void init() {
    logger.fine("init CondaController");
    loadCommands();
  }

  public void retryCommand(CondaCommands command) {
    command.setErrorMsg("");
    command.setStatus(CondaStatus.NEW);
    condaCommandFacade.update(command);
    message("Retrying command");
    loadCommands();
  }

  public void deleteAllFailedCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaStatus.FAILED);
    message("Deleting all commands with the state 'failed'");
    loadFailedCommands();
  }

  public void deleteAllOngoingCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaStatus.ONGOING);
    message("Deleting all commands with the state 'ongoing'");
    loadOngoingCommands();
  }

  public void deleteAllNewCommands() {
    condaCommandFacade.deleteAllCommandsByStatus(CondaStatus.NEW);
    message("Deleting all commands with the state 'new'");
    loadNewCommands();
  }

  public void deleteCommand(CondaCommands command) {
    condaCommandFacade.removeCondaCommand(command.getId());
    message("Deleting command");
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

  public String getOutput() {
    if (!isOutput()) {
      return "No Output to show for command executions.";
    }
    return this.output;
  }

  public boolean isOutput() {
    return !Strings.isNullOrEmpty(this.output);
  }

  public void showError(CondaCommands command) {
    if(!Strings.isNullOrEmpty(command.getErrorMsg()))
      this.output = command.getErrorMsg();
  }
}
