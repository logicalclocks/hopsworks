/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import javax.faces.component.UIInput;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.component.tabview.TabView;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FlowEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import se.kth.bbc.activity.ActivityMB;
import se.kth.kthfsdashboard.user.UserFacade;
import se.kth.kthfsdashboard.user.Username;

/**
 *
 * @author roshan
 *
 */
@ManagedBean(name = "studyManagedBean", eager = true)
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

    @ManagedProperty(value = "#{activityBean}")
    private ActivityMB activity;

    private TrackStudy study;
    private DatasetStudy dsStudy;
    private Dataset dataset;
    private List<Username> usernames;
    private StudyTeam studyTeamEntry;
    private List<Theme> selectedUsernames;
    private List<Theme> themes;

    private String studyName;
    private String studyCreator;
    private String newTeamRole;
    private String newChangedRole;
    private String setTeamRole;
    private String newRole;
    private String owner;
    private int tabIndex;

    //private UIInput newTeamRole;
    
    
    @PostConstruct
    public void init() {
        activity.getActivity();
    }

    public void setActivity(ActivityMB activity) {
        this.activity = activity;
    }

    public List<Username> getUsersNameList() {
        return userFacade.findAllUsers();
    }

    public List<Username> getUsersname() {
        return usernames;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
    }

    public String getCreator() {
        return studyCreator;
    }

    public void setCreator(String studyCreator) {
        this.studyCreator = studyCreator;
    }

    
    public String getOwnerRole(){
        return owner;
    }
    
    public void setOwnerRole(String owner){
        this.owner = owner;
    }
    
    public String getNewTeamRole() {
        return newTeamRole;
    }

    public void setNewTeamRole(String newTeamRole) {
        this.newTeamRole = newTeamRole;
    }

    public String getChangedRole() {
        return newChangedRole;
    }

    public void setChangedRole(String newChangedRole) {
        this.newChangedRole = newChangedRole;
    }
    
    public String getNewRole(){
        return newRole;
    }

    public void setNewRole(String newRole){
        this.newRole = newRole;
    }
    
//    public void teamRoleChanged(ValueChangeEvent e) {
//        setChangedRole(e.getNewValue().toString());
//        System.out.println(" New role selected : " + getChangedRole());
//
////     if(getChangedRole().equals(e.getOldValue().toString()))
//    }

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

    public StudyTeam getStudyTeamEntry() {
        if (studyTeamEntry == null) {
            studyTeamEntry = new StudyTeam();
        }
        return studyTeamEntry;
    }

    public void setStudyTeamEntry(StudyTeam studyTeamEntry) {
        this.studyTeamEntry = studyTeamEntry;
    }

    public String getStudyTeamRole(String email) {
       this.setTeamRole = studyTeamController.findByPrimaryKey(studyName, email).getTeamRole();
       return setTeamRole;
//        if (team != null) {
//            setTeamRole = team.getTeamRole();
//            return setTeamRole;
//        }
//        return null;

    }

    public void setStudyTeamRole(String email, String role) {
//        studyTeamEntry = studyTeamController.findByPrimaryKey(studyName, email);
//        this.setTeamRole = studyTeamEntry.getTeamRole();
          this.setTeamRole = role;
    }
//    public void setStudyTeam(StudyTeam studyTeam) {
////        this.studyTeam = studyTeam;
//    }
    public List<TrackStudy> getStudyList() {
        return studyController.findAll();
    }

    public List<TrackStudy> getPersonalStudy() {
        return studyController.findByUser(getUsername());
    }

    public long getAllStudy() {
        return studyController.getAllStudy(getUsername());
    }

    public long getNOfMembers() {
        return studyController.getMembers(getStudyName());
    }

    public List<TrackStudy> getPersonalStudyList() {
        return studyController.filterPersonalStudy(getUsername());
    }

    public int getLatestStudyListSize() {
        return studyController.filterPersonalStudy(getUsername()).size();
    }

    public List<Theme> addThemes() {
        List<Username> list = userFacade.filterUsersBasedOnStudy(getStudyName());
        themes = new ArrayList<>();
        int i = 0;
        for (Username user : list) {
            themes.add(new Theme(i, user.getName(), user.getEmail()));
            i++;
            //System.out.println("themes - " + user.getName() + " - "+user.getEmail());
        }

        return themes;
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public List<Theme> completeUsername(String query) {
        //List<Theme> allThemes = userFacade.filterUsersBasedOnStudy(studyName);
        List<Theme> allThemes = addThemes();
        List<Theme> filteredThemes = new ArrayList<>();

        for (Theme t : allThemes) {
            if (t.getName().toLowerCase().contains(query)) {
                filteredThemes.add(t);
            }
        }
        return filteredThemes;
    }

//    public void setService(ThemeService service) {
//        this.service = service;
//    }
//    public List<Username> completeUsername(String name) {
//        usernames = getUsersNameList();
//        List<Username> suggestions = new ArrayList<>();
//        for(Username names : usernames) {
//            if(names.getName().toLowerCase().startsWith(name))
//                suggestions.add(names);
//        }
//            return suggestions;
//    }
    public List<Theme> getSelectedUsernames() {
        return this.selectedUsernames;
    }

    public void setSelectedUsernames(List<Theme> selectedUsernames) {
        this.selectedUsernames = selectedUsernames;
    }

    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

    public String getUsername() {
        return getRequest().getUserPrincipal().getName();
    }

    public void gravatarAccess() {
        activity.getGravatar(studyCreator);
    }

    
    
    public StudyRoleTypes[] getTeam() {
        return StudyRoleTypes.values();
    }

    public List<StudyRoleTypes> getTeamForResearchList() {

        List<StudyRoleTypes> reOrder = new ArrayList<>();
        //for(StudyRoleTypes role: StudyRoleTypes.values()){
        reOrder.add(StudyRoleTypes.RESEARCHER);
        reOrder.add(StudyRoleTypes.MASTER);
        reOrder.add(StudyRoleTypes.GUEST);
        //}
        return reOrder;
    }

    public  List<StudyRoleTypes> getTeamForGuestList() {

        List<StudyRoleTypes> reOrder = new ArrayList<>();
        //for(StudyRoleTypes role: StudyRoleTypes.values()){
        reOrder.add(StudyRoleTypes.GUEST);
        reOrder.add(StudyRoleTypes.MASTER);
        reOrder.add(StudyRoleTypes.RESEARCHER);
        //}
        return reOrder;
    }

    public long countAllMembersPerStudy() {
        return studyTeamController.countMembersPerStudy(studyName).size();
    }

    public long countMasters() {
        return studyTeamController.countStudyTeam(studyName, "Master");
    }

    public long countResearchers() {
        return studyTeamController.countStudyTeam(studyName, "Researcher");
    }

    public long countGuests() {
        return studyTeamController.countStudyTeam(studyName, "Guest");
    }

    public List<Username> getMastersList() {
        return studyTeamController.findTeamMembersByName(studyName, "Master");
    }

    public List<Username> getResearchersList() {
        return studyTeamController.findTeamMembersByName(studyName, "Researcher");
    }

    public List<Username> getGuestsList() {
        return studyTeamController.findTeamMembersByName(studyName, "Guest");
    }

    public String checkStudyOwner(String email) {
       
        List<TrackStudy> lst = studyTeamController.findStudyMaster(studyName);
        for (TrackStudy tr : lst) {
            if (tr.getUsername().equals(email)) {
                    return email;
            }
        }
        return null;
    }
    
    public String checkCurrentUser(String email) {
       
        if(email.equals(getUsername()))
                return email;
        
        return null;
    }

    public String renderList(String email) {
//        if (!getNewTeamRole().equals(studyTeamController.findByPrimaryKey(studyName, email).getTeamRole())) {
            //String role = studyTeamController.findByPrimaryKey(studyName, email).getTeamRole();
//        }
            //return role;
        //return null;
        
        String role = studyTeamController.getStudyTeam(studyName, email).getTeamRole();
        return role;
    }

//    public List<StudyRoleTypes> getCurrentRole(String email) {
//
//        String team = studyTeamController.findByPrimaryKey(studyName, email).getTeamRole();
//        List<StudyRoleTypes> reOrder = new ArrayList<>();
//
//        if (team.equals("Researcher")) {
////                for(StudyRoleTypes role: StudyRoleTypes.values()) {
//            reOrder.add(StudyRoleTypes.RESEARCHER);
//            reOrder.add(StudyRoleTypes.MASTER);
//            reOrder.add(StudyRoleTypes.GUEST);
////                }
//            return reOrder;
//
//        } else if (team.equals("Guest")) {
//
//            reOrder.add(StudyRoleTypes.GUEST);
//            reOrder.add(StudyRoleTypes.MASTER);
//            reOrder.add(StudyRoleTypes.RESEARCHER);
//
//            return reOrder;
//        } else {
//            return null;
//        }
//    }

    public int getAllStudyUserTypesListSize() {
        return studyTeamController.findMembersByStudy(studyName).size();
    }

    public List<StudyTeam> getAllStudyUserTypesList() {
        return studyTeamController.findMembersByStudy(studyName);
    }

    public List<TrackStudy> getAllStudiesPerUser() {
        return studyController.findAllStudies(getUsername());
    }

    public List<TrackStudy> getJoinedStudies() {
        return studyController.findJoinedStudies(getUsername());
    }

    public List<StudyTeam> getTeamList() {
        return studyTeamController.findMembersByStudy(studyName);
    }

    public int countAllStudiesPerUser(){
        return studyController.findAllStudies(getUsername()).size();
    }
    
    public int countPersonalStudy(){
        return  studyController.findByUser(getUsername()).size();
    }
    
    
     public int countJoinedStudy(){
        return  studyController.findJoinedStudies(getUsername()).size();
    }
    
    /**
     * Get the current username from session and
     * sets it as the creator of the study,
     * and also adding a record to the StudyTeam table for setting role as master for the study.
     * @return 
     */
    
    public String createStudy() {

        study.setUsername(getUsername());
        study.setTimestamp(new Date());
        try {
            studyController.persistStudy(study);
            activity.addActivity("new study created", study.getName(), "STUDY");
            addStudyMaster(study.getName());
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Failed: Study already exists!");
            return null;
        }
            addMessage("Study created! [" + study.getName() + "] study is owned by " + study.getUsername());
            return "Success!";
    }

    /**
     *  @return  
     */
    public String fetchStudy() {

        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        this.studyName = params.get("studyname");
        this.studyCreator = params.get("username");

        System.out.println("Studyname: " + this.studyName + " - user: " + getUsername() + " - creator: " + this.studyCreator);
        studyTeamController.setRoleForActiveStudy(studyName, getUsername());

        return "studyPage";

    }

    public void addStudyMaster(String study_name) {

        StudyTeamPK stp = new StudyTeamPK(study_name, getUsername());
        StudyTeam st = new StudyTeam(stp);
        st.setTeamRole("Master");
        st.setTimestamp(new Date());

        try {
            studyTeamController.persistStudyTeam(st);
        } catch (EJBException ejb) {
            System.out.println("Add study master failed" + ejb.getMessage());
        }
        System.out.println("Add study master success!");
    }

    //delete a study
    public synchronized String deleteStudy() {
        try {
            studyController.removeStudy(study);
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Study wasn't removed.");
            return null;
        }
            addMessage("Study removed.");
            return "Success!";
    }

    //add member to a team - batch persist 
    public synchronized String addToTeam() {
        try {
            Iterator<Theme> itr = getSelectedUsernames().listIterator();
            while (itr.hasNext()) {
                Theme t = itr.next();
                StudyTeamPK stp = new StudyTeamPK(studyName, t.getName());
                StudyTeam st = new StudyTeam(stp);
                st.setTimestamp(new Date());
                st.setTeamRole(studyTeamEntry.getTeamRole());
                studyTeamController.persistStudyTeam(st);
                activity.addActivity("added new member " + t.getName() + " ", studyName, "STUDY");
            }
                
                 if(!getSelectedUsernames().isEmpty())
                       getSelectedUsernames().clear();
            
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Adding team member failed.");
            return null;
        }
            
            addMessage("New Member Added!");
            return "studyPage";
    }
    
    public void itemSelect(SelectEvent e) {
         if(getSelectedUsernames().isEmpty())
             addErrorMessageToUserAction("Error: People field cannot be empty.");
    }

    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }
    
    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    
    public void onTabChange(TabChangeEvent event) {
        if(event.getTab().getTitle().equals("All")){
            setTabIndex(0);
            //System.out.println("All - " + getTabIndex());
        } else if(event.getTab().getTitle().equals("Personal")){
            setTabIndex(1);
            //System.out.println("Personal - " + getTabIndex());
        } else if(event.getTab().getTitle().equals("Joined")){
            setTabIndex(2);
             //System.out.println("Joined - " + getTabIndex());
        } else {
            //do nothing at the moment
        }

    }
    
    public String onComplete(){
        return "indexPage";
    }
    
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
