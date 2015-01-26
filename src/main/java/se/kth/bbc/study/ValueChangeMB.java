package se.kth.bbc.study;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityMB;
import se.kth.bbc.lims.ClientSessionState;


/**
 *
 * @author roshan
 */

@ManagedBean(name="valueChangeMB", eager = true)
@RequestScoped
public class ValueChangeMB implements Serializable, ValueChangeListener { 
    
    @EJB
    private StudyTeamController studyTeamController;
    
    private StudyRoleTypes newTeamRole;
    
    @ManagedProperty(value = "#{activityBean}")
    private ActivityMB activity;
    
    
    @ManagedProperty(value = "#{clientSessionState}")
    private ClientSessionState sessionState;
    
    
    @PostConstruct
    public void init() {
        activity.getActivity();
    }

    public void setActivity(ActivityMB activity) {
        this.activity = activity;
    }

  public void setSessionState(ClientSessionState sessionState) {
    this.sessionState = sessionState;
  }
    
    public StudyRoleTypes getNewTeamRole() {
        return newTeamRole;
    }

    public void setNewTeamRole(StudyRoleTypes newTeamRole) {
        this.newTeamRole = newTeamRole;
    }
    
    public synchronized String updateStudyTeamRole(String email, StudyRoleTypes role) {
        System.out.println("Update "+email+" to "+role);
        try {
            studyTeamController.updateTeamRole(sessionState.getActiveStudyname(), email, role.getTeam());
            activity.addActivity(ActivityController.CHANGE_ROLE + email + " to " + role, sessionState.getActiveStudyname(), "TEAM");
        } catch (Exception ejb) {
            //addErrorMessageToUserAction("Error: Update failed.");
            return "Failed";
        }
            //addMessage("Team role updated successful "+ email + " at "+ studyMB.getStudyName());
            //return "studyPage?faces-redirect=true";
            newTeamRole = null;
            return "OK";
    }
    
    @Override
    public void processValueChange(ValueChangeEvent event) throws AbortProcessingException {
        setNewTeamRole((StudyRoleTypes)event.getNewValue());
        System.out.println(" new value from Listener ===== "+ event.getNewValue().toString());
        System.out.println(" new value from Listener ===== "+ event.getOldValue().toString());
        
   }
    
}
