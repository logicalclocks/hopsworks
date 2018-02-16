/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
