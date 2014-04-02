/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.study;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FlowEvent;


/**
 *
 * @author roshan
 */
@ManagedBean
@SessionScoped
public class StudyMB implements Serializable{
    
    @EJB
    private StudyController studyController;
    private TrackStudy study;
    private DatasetStudy dsStudy;
    private Dataset dataset;
       
    
//    private boolean skip;
    private static final Logger logger = Logger.getLogger(StudyMB.class.getName());
    
    
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
    
    
    public List<TrackStudy> getPersonalStudy(){
        return studyController.findByUser(getUsername());
    }
    
    public List<String> getAllStudyList(){
        return studyController.findOwner(getUsername());
    }
//    
//    public List<Dataset> getDatasetId(){
//        return studyController.findById();
//    }
    
    
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
    
    
    public String getUsername(){
    
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        Principal principal = request.getUserPrincipal();
        
        return principal.getName();
    
    
    }
    
    
        
    public String createStudy(){
        
        study.setId(Integer.SIZE);
        study.setUsername(getUsername());
        
        try{
            studyController.persistStudy(study);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Failed: Study name might have been duplicated!");
            return null;
        }
        addMessage("Study created! ["+ study.getName() + "] study is owned by " + study.getUsername());
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
    
    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }
    
    
    //Study View Controller
    
    public void save(ActionEvent actionEvent) {
               createStudy();               
    }
   
    public String onFlowProcess(FlowEvent event) {
		logger.info(event.getOldStep());
		logger.info(event.getNewStep());

                return event.getNewStep();	
    }    
    
    public void showNewStudyDialog() {
        
        RequestContext.getCurrentInstance().update("formNewStudy");
        RequestContext.getCurrentInstance().reset("formNewStudy");
        RequestContext.getCurrentInstance().execute("dlgNewStudy.show()");
    }
    
    
    
    
}
