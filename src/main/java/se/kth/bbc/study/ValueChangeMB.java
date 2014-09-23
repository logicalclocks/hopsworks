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
import javax.faces.bean.NoneScoped;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.event.ValueChangeEvent;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityMB;


/**
 *
 * @author roshan
 */

@ManagedBean(name="valueChangeMB", eager = true)
@RequestScoped
public class ValueChangeMB implements Serializable { 
    
    @EJB
    private StudyTeamController studyTeamController;
    
    private String newTeamRole;
    
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
    
    public String getNewTeamRole() {
        return newTeamRole;
    }

    public void setNewTeamRole(String newTeamRole) {
        this.newTeamRole = newTeamRole;
    }
    
    public synchronized String updateStudyTeamRole(String email) {

        try {
            studyTeamController.updateTeamRole(studyMB.getStudyName(), email, getNewTeamRole());
            activity.addActivity(ActivityController.CHANGE_ROLE + email + " to " + getNewTeamRole(), studyMB.getStudyName(), "TEAM");
        } catch (Exception ejb) {
            //addErrorMessageToUserAction("Error: Update failed.");
            return "Failed";
        }
            //addMessage("Team role updated successful "+ email + " at "+ studyMB.getStudyName());
            //return "studyPage?faces-redirect=true";
            studyMB.setManTabIndex(StudyMB.TEAM_TAB);
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
            studyMB.setManTabIndex(StudyMB.TEAM_TAB);
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
    
}
