/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.context.RequestContext;


/**
 *
 * @author roshan
 */
@ManagedBean
@RequestScoped
public class StudyMB implements Serializable{
    
    @EJB
    private StudyController studyController;
    private TrackStudy study;
    private DatasetStudy dsStudy;
    private Dataset dataset;
    
    
    public TrackStudy getStudy() {
        if (study == null) {
            study = new TrackStudy();
        }
        return study;
    }
    
    public void setStudy(TrackStudy study) {
        this.study = study;
    } 
    
    public DatasetStudy getDatasetStudy() {
        if (dsStudy == null) {
            dsStudy = new DatasetStudy();
        }
        return dsStudy;
    }
        
    public void setDatasetStudy(DatasetStudy dsStudy) {
        this.dsStudy = dsStudy;
    } 
    
    public Dataset getDataset() {
        if (dataset == null) {
            dataset = new Dataset();
        }
        return dataset;
    }
    
    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    } 
    
    public List<TrackStudy> getStudyList(){
        return studyController.findAll();
    }
    
    public List<Dataset> getDatasetList(){
        return studyController.findAllDataSet();
    }
    
    public List<Dataset> getDatasetId(){
        return studyController.findById();
    }
    
    
    public  long getAllStudy(){
        return studyController.getAllStudy();
    }
    
//    public List<DatasetStudy> getNameOwner(){
//        return studyController.findNameOwner();
//    }
//    
//    public List<Dataset> getPersonalStudy(){
//        return studyController.findStudyOwner();
//    } 
//    
//    public List<TrackStudy> getJoinStudy(){
//        return null;
//    } 
//    
//    
//    
//    
//    public List<Dataset> getNamesById(){
//        return studyController.findAllById();
//    }
//    
    public String createStudy(){
        
        try{
            studyController.persistStudy(study);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Study wasn't created.");
            return null;
        }
        addMessage("Study created.");
        return "Success!";
    }
    
    public String deleteStudy(){
        try{
            studyController.removeStudy(study);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Study wasn't removed.");
            return null;
        }
        addMessage("Study removed.");
        return "Success!";
    }
    
    
    public String studyManagement(){
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();
       
       if (request.isUserInRole("BBC_ADMIN") || request.isUserInRole("BBC_RESEARCHER") || request.isUserInRole("ADMIN")){
            addMessage("Switched to the LIMS Study Management Service!");
            return "/bbc/lims/studyMgmt.xml?faces-redirect=true";
        }else{
            addErrorMessageToUserAction("Operation is not allowed: " + principal.getName() + " is not a privileged user to perform this action.");
            return "Failed";
        }
    }
    
    
    
    public String dataManagement(){
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        Principal principal = request.getUserPrincipal();
       
       if (request.isUserInRole("BBC_ADMIN") || request.isUserInRole("BBC_RESEARCHER")|| request.isUserInRole("ADMIN")){
            addMessage("Switched to the LIMS Data Management Service!");
            return "/bbc/lims/create-Datasets.xml?faces-redirect=true";
        }else{
            addErrorMessageToUserAction("Operation is not allowed: " + principal.getName() + " is not a privileged user to perform this action.");
            return "Failed";
        }
    }
    
    
    
    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }
    
    
    //Study View Controller
    
    
    public void showNewStudyDialog() {
        
        RequestContext.getCurrentInstance().update("formNewStudy");
        RequestContext.getCurrentInstance().reset("formNewStudy");
        RequestContext.getCurrentInstance().execute("dlgNewStudy.show()");
    }
    
    
    
    
}
