/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityMB;


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
    
    
    @ManagedProperty(value = "#{studyManagedBean}")
    private StudyMB studyMB;
    
    
    @PostConstruct
    public void init() {
        activity.getActivity();
    }

    public void setActivity(ActivityMB activity) {
        this.activity = activity;
    }
    
    public void setStudyMB(StudyMB studyMB) {
        this.studyMB = studyMB;
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
            studyTeamController.updateTeamRole(studyMB.getStudyName(), email, role.getTeam());
            activity.addActivity(ActivityController.CHANGE_ROLE + email + " to " + role, studyMB.getStudyName(), "TEAM");
        } catch (Exception ejb) {
            //addErrorMessageToUserAction("Error: Update failed.");
            return "Failed";
        }
            //addMessage("Team role updated successful "+ email + " at "+ studyMB.getStudyName());
            //return "studyPage?faces-redirect=true";
            newTeamRole = null;
            return "OK";
    }
    
    public synchronized String deleteMemberFromTeam(String email) {

        try {

            studyTeamController.removeStudyTeam(studyMB.getStudyName(), email);
            activity.addActivity(ActivityController.REMOVED_MEMBER + email, studyMB.getStudyName(), "TEAM");

        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Deleting team member failed.");
            return null;
        }
            addMessage("Team member " + email + " deleted from study " + studyMB.getStudyName());            
            return "studyPage";

    }
    
    
    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }
    
    @Override
    public void processValueChange(ValueChangeEvent event) throws AbortProcessingException {
        setNewTeamRole((StudyRoleTypes)event.getNewValue());
        System.out.println(" new value from Listener ===== "+ event.getNewValue().toString());
        System.out.println(" new value from Listener ===== "+ event.getOldValue().toString());
        
   }
    
}
