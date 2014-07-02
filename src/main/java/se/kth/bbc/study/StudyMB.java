/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.primefaces.component.tabview.TabView;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FlowEvent;
import org.primefaces.event.TabChangeEvent;


/**
 *
 * @author roshan
 */
@ManagedBean
@SessionScoped
public class StudyMB implements Serializable{
    
    private static final Logger logger = Logger.getLogger(StudyMB.class.getName());
    private static final long serialVersionUID = 1L;
    
    @EJB
    private StudyController studyController;
    
    
    private TrackStudy study;
    private DatasetStudy dsStudy;
    private Dataset dataset;
    private StudyGroupMembers studyMember;
    
    
    private String studyName;   
    private String studyCreator;
    
    
    
    public String getStudyName(){
        return studyName;
    }
    
    public void setStudyName(String studyName){
        this.studyName = studyName;
    }
    
    public String getCreator(){
        return studyCreator;
    }
    
    public void setCreator(String studyCreator){
        this.studyCreator = studyCreator;
    }
    
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
    
    
    public StudyGroupMembers getStudyMember() {
        if (studyMember == null) {
            studyMember = new StudyGroupMembers();
        }
        return studyMember;
    }
    
    public void setStudyMember(StudyGroupMembers studyMember) {
        this.studyMember = studyMember;
    } 
    
        
    public List<TrackStudy> getStudyList(){
        return studyController.findAll();
    }
    
    
    public List<TrackStudy> getPersonalStudy(){
        return studyController.findByUser(getUsername());
    }
    
    public long getAllStudy(){
        return studyController.getAllStudy();
    }

    public long getNOfMembers(){
        return studyController.getMembers(getStudyName());
    }

      
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
    
    //create a study       
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
        return "studyMgmt";
    }
    
    
    public String fetchStudy(){
    
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String,String> params = fc.getExternalContext().getRequestParameterMap();
        this.studyName =  params.get("studyname"); 
        this.studyCreator =  params.get("username"); 
        
        return "studyInfo";
    
    }
    
    //delete a study
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
    
    
    //add members to study
    
    public String addMembers(){
    
        studyMember.setTimeadded(new Date());
        studyMember.setAddedBy(getUsername());
        studyMember.studyGroupMembersPK.setStudyname(this.studyName);
        
        try{
            studyController.addMember(studyMember);
        }catch(EJBException ejb){
            addErrorMessageToUserAction("Error: New Member adding failed!");
            return null;
        
        }
            addMessage("added member successfully!");
            return "studyMgmt";
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
    
    public void showNewStudyMemberDialog() {
        
        RequestContext.getCurrentInstance().update("formNewStudyMember");
        RequestContext.getCurrentInstance().reset("formNewStudyMember");
        RequestContext.getCurrentInstance().execute("dlgNewStudyMember.show()");
    }
    
    
}
