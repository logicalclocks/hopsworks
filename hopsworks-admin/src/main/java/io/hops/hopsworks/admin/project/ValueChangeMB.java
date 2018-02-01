/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.admin.project;

import io.hops.hopsworks.admin.lims.ClientSessionState;
import io.hops.hopsworks.common.dao.project.team.ProjectRoleTypes;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;

@ManagedBean(name = "valueChangeMB",
        eager = true)
@RequestScoped
public class ValueChangeMB implements Serializable, ValueChangeListener {

  @EJB
  private ProjectTeamFacade projectTeamController;

  @EJB
  private ActivityFacade activityFacade;

  private ProjectRoleTypes newTeamRole;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }

  public ProjectRoleTypes getNewTeamRole() {
    return newTeamRole;
  }

  public void setNewTeamRole(ProjectRoleTypes newTeamRole) {
    this.newTeamRole = newTeamRole;
  }

  public synchronized String updateProjectTeamRole(String email,
          ProjectRoleTypes role) {
    try {
      projectTeamController.updateTeamRole(sessionState.getActiveProject(),
              email, role.getRole());
      activityFacade.persistActivity(ActivityFacade.CHANGE_ROLE + email + " to "
              + role, sessionState.getActiveProject(), sessionState.
              getLoggedInUsername());
    } catch (Exception ejb) {
      //addErrorMessageToUserAction("Error: Update failed.");
      return "Failed";
    }
    //addMessage("Team role updated successful "+ email + " at "+ projectMB.getProjectName());
    //return "projectPage?faces-redirect=true";
    newTeamRole = null;
    return "OK";
  }

  @Override
  public void processValueChange(ValueChangeEvent event) throws
          AbortProcessingException {
    setNewTeamRole((ProjectRoleTypes) event.getNewValue());
  }

}
