/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;

/**
 *
 * @author roshan
 */
@ManagedBean
@RequestScoped
public class StudyMB implements Serializable{
    
    @EJB
    private StudyController studyController;
    private TrackStudy study = new TrackStudy();

    
    public List<TrackStudy> getStudy(){
        return studyController.findAll();
    }
    
    public String createStudy(){
        try{
            studyController.persistStudy(study);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Study wasn't created.");
            return null;
        }
        addMessage("Study Created Successfully.");
        return "Success!";
    }
    
    public String deleteStudy(){
        try{
            studyController.removeStudy(study);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Study wasn't removed.");
            return null;
        }
        addMessage("Study removed successfully.");
        return "Success!";
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
