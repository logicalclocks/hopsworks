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
