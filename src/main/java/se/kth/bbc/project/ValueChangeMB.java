package se.kth.bbc.project;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.lims.ClientSessionState;

/**
 *
 * @author roshan
 */
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
    System.out.println("Update " + email + " to " + role);
    try {
      projectTeamController.updateTeamRole(sessionState.getActiveProject(),
              email, role.getTeam());
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
