/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;


import java.io.IOException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.primefaces.component.tabview.TabView;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FlowEvent;
import org.primefaces.event.TabChangeEvent;
import se.kth.bbc.activity.ActivityMB;
import se.kth.bbc.activity.UserActivity;
import se.kth.kthfsdashboard.user.AutocompleteMB;
import se.kth.kthfsdashboard.user.UserFacade;
import se.kth.kthfsdashboard.user.Username;



/**
 *
 * @author roshan
 */
@ManagedBean(name="studyManagedBean", eager = true)
@SessionScoped
public class StudyMB implements Serializable {
    
    private static final Logger logger = Logger.getLogger(StudyMB.class.getName());
    private static final long serialVersionUID = 1L;
    
    @EJB
    private StudyController studyController;
    
    
    @EJB
    private StudyTeamController studyTeamController;
    
    @EJB
    private UserFacade userFacade;

    
    
    @ManagedProperty(value="#{activityBean}")
    private ActivityMB activity;
    
//    @ManagedProperty(value="#{autoCompleteBean}")
//    private AutocompleteMB autoComplete;
    
    
    @ManagedProperty("#{themeService}")
    private ThemeService service;
    
    
    private TrackStudy study;
    private DatasetStudy dsStudy;
    private Dataset dataset;
    private TeamMembers studyTeam;
    private List<Username> usernames;
    private List<Theme> selectedUsername;
    private StudyTeam teamem;
    
    private String studyName;   
    private String studyCreator;
    
    
    @PostConstruct
    public void init(){
       activity.getActivity();
    }
    
    public void setActivity(ActivityMB activity) {
        this.activity = activity;
    } 

//    public AutocompleteMB getAutocompleteMB(){
//        return autoComplete;
//    }
//    
//    public void setAutocompleteMB(AutocompleteMB autoComplete) {
//        this.autoComplete = autoComplete;
//     }
    
    
    public List<Username> getUsersNameList() {
        return userFacade.findAllUsers();
    }
    
    public List<Username> getUsersname() {
        return usernames;
    }
    
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

    public TeamMembers getStudyGroups() {
        if (studyTeam == null) {
            studyTeam = new TeamMembers();
        }
        return studyTeam;
    }
        
    public void setStudyGroup(TeamMembers studyTeam){
        this.studyTeam = studyTeam;
    }
    
    public List<TrackStudy> getStudyList(){
        return studyController.findAll();
    }
    
    
    public List<TrackStudy> getPersonalStudy(){
        return studyController.findByUser(getUsername());
    }
    
    public long getAllStudy(){
        return studyController.getAllStudy(getUsername());
    }

    public long getNOfMembers(){
        return studyController.getMembers(getStudyName());
    }

    public List<TrackStudy> getLatestStudyList(){
        return studyController.filterLatestStudy(getUsername());
    }
    
    public List<Theme> completeUsername(String query) {
         List<Theme> allThemes = service.getThemes();
         List<Theme> filteredThemes = new ArrayList<Theme>();
         
        for (int i = 0; i < allThemes.size(); i++) {
            Theme skin = allThemes.get(i);
            if(skin.getName().toLowerCase().contains(query)) {
                filteredThemes.add(skin);
            }
        }
            return filteredThemes;
    }   
    
    public void setService(ThemeService service) {
        this.service = service;
    }
    
    
//    public List<Username> completeUsername(String name) {
//        usernames = getUsersNameList();
//        List<Username> suggestions = new ArrayList<>();
//        for(Username names : usernames) {
//            if(names.getName().toLowerCase().startsWith(name))
//                suggestions.add(names);
//        }
//            return suggestions;
//    }
    
    public List<Theme> getSelectedUsersname() {
        return selectedUsername;
    }
 
    public List<Theme> getSelectedTheme() {
        return new ArrayList<Theme>();
    }
    public void setSelectedTheme(List<Theme> t) {
        return;
    }
 
    public void setSelectedUsersname(List<Theme> selectedUsername) {
        this.selectedUsername = selectedUsername;
    }
    
      
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
    
    public void gravatarAccess(){
        activity.getGravatar(studyCreator);
    }
    
//    public void setTeam(StudyTeam teamem){
//        this.teamem = teamem;
//    }
    
    public StudyTeam[] getTeam() {
        return StudyTeam.values();
    }
    
    
    public long countTeamMembers(){
        return studyTeamController.countTeamMembers(studyName);
    }
    
    //create a study       
    public String createStudy(){
        
        study.setUsername(getUsername());
        study.setTimestamp(new Date());
        
        try{
            studyController.persistStudy(study);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Failed: Study name might have been duplicated!");
            return null;
        }
        addMessage("Study created! ["+ study.getName() + "] study is owned by " + study.getUsername());
        activity.addActivity("new study created", study.getName(),"STUDY");
        return "Success!";
    }
    
    public String fetchStudy(){
    
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String,String> params = fc.getExternalContext().getRequestParameterMap();
        this.studyName =  params.get("studyname"); 
        this.studyCreator =  params.get("username"); 
        
        return "studyPage";
    
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
    
    
    //add member to a team 
    public String addToTeam(){
           
           studyTeam.setName(studyName);
           studyTeam.setTeamMember(getSelectedUsersname().toString());
           studyTeam.setTimestamp(new Date());
//           studyTeam.setTeamRole(studyTeam.setTeamRole(s));
       
       try{
           studyTeamController.persistStudyTeam(studyTeam);
       }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Study wasn't removed.");
            return null;
        }
            addMessage("added.");
            activity.addActivity("new team member added", studyName,"STUDY");
            return "Success!";
    }
    
    
    
    
    
    //add members to study
    
//    public String addMembers(){
//    
//        studyMember.setTimeadded(new Date());
//        studyMember.setAddedBy(getUsername());
//        studyMember.studyGroupMembersPK.setStudyname(this.studyName);
//        
//        try{
//            studyController.addMember(studyMember);
//        }catch(EJBException ejb){
//            addErrorMessageToUserAction("Error: New Member adding failed!");
//            return null;
//        
//        }
//            addMessage("added member successfully!");
//            return "studyMgmt";
//    }
    
    
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
