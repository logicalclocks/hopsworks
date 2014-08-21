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
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.component.tabview.TabView;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FlowEvent;
import org.primefaces.event.TabChangeEvent;
import se.kth.bbc.activity.ActivityMB;
import se.kth.kthfsdashboard.user.UserFacade;
import se.kth.kthfsdashboard.user.Username;

/**
 *
 * @author roshan
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

//    @ManagedProperty(value="#{autoCompleteBean}")
//    private AutocompleteMB autoComplete;
    @ManagedProperty("#{themeService}")
    private ThemeService service;

    private TrackStudy study;
    private DatasetStudy dsStudy;
    private Dataset dataset;
    private StudyTeam studyTeam;
    private List<Username> usernames;
    private List<Theme> selectedUsername;
    private List<StudyRoleTypes> study_groups = new ArrayList<>();

    private String studyName;
    private String studyCreator;

    @PostConstruct
    public void init() {
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

    public StudyTeam getStudyGroups() {
        if (studyTeam == null) {
            studyTeam = new StudyTeam();
        }
        return studyTeam;
    }

    public void setStudyGroup(StudyTeam studyTeam) {
        this.studyTeam = studyTeam;
    }

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

    public List<TrackStudy> getLatestStudyList() {
        return studyController.filterLatestStudy(getUsername());
    }

    public int getLatestStudyListSize() {
        return studyController.filterLatestStudy(getUsername()).size();
    }

    public List<Theme> completeUsername(String query) {
        List<Theme> allThemes = service.getThemes();
        List<Theme> filteredThemes = new ArrayList<Theme>();

        for (int i = 0; i < allThemes.size(); i++) {
            Theme skin = allThemes.get(i);
            if (skin.getName().toLowerCase().contains(query)) {
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

    public void setSelectedUsersname(List<Theme> selectedUsername) {
        this.selectedUsername = selectedUsername;
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

    public List<StudyTeam> getMastersList() {
//        List<StudyTeam> list = studyTeamController.findMasterMembersByName(studyName);
//        for(StudyTeam st:list)
//            System.out.println("print - "+ list.size() +" - "+st.studyTeamPK.getName()+" - "+ st.studyTeamPK.getTeamMember() +" - " +st.getTeamRole());

        return studyTeamController.findMasterMembersByName(studyName);
    }

    public List<StudyTeam> getResearchersList() {
//         List<StudyTeam> list = studyTeamController.findResearchMembersByName(studyName);
//        for(StudyTeam st:list)
//            System.out.println("print - "+ list.size() +" - "+st.studyTeamPK.getName()+" - "+ st.studyTeamPK.getTeamMember() +" - " +st.getTeamRole());

        return studyTeamController.findResearchMembersByName(studyName);
    }

    public List<StudyTeam> getGuestsList() {

//        List<StudyTeam> list = studyTeamController.findGuestMembersByName(studyName);
//        for(StudyTeam st:list)
//            System.out.println("print - "+ list.size() +" - "+st.studyTeamPK.getName()+" - "+ st.studyTeamPK.getTeamMember() +" - " +st.getTeamRole());
        return studyTeamController.findGuestMembersByName(studyName);
    }

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

    //create a study       
    public String createStudy() {

        study.setUsername(getUsername());
        study.setTimestamp(new Date());
        try {
            studyController.persistStudy(study);
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Failed: Study name might have been duplicated!");
            return null;
        }
        addMessage("Study created! [" + study.getName() + "] study is owned by " + study.getUsername());
        activity.addActivity("new study created", study.getName(), "STUDY");
        addStudyMaster(study.getName());
        return "Success!";
    }

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
    public String deleteStudy() {
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
    public String addToTeam() {
        try {

            Iterator<Theme> itr = getSelectedUsersname().listIterator();
            while (itr.hasNext()) {
                Theme t = itr.next();
                StudyTeamPK stp = new StudyTeamPK(studyName, t.getName());
                StudyTeam st = new StudyTeam(stp);
                st.setTimestamp(new Date());
                st.setTeamRole(studyTeam.getTeamRole());
                
                studyTeamController.persistStudyTeam(st);
                activity.addActivity("added new member " + t.getName() + " ", studyName, "STUDY");
            }

        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Adding team member failed.");
            return null;
        }
        addMessage("New Member Added!");
        return "studyPage";
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
//    
//    private Integer activeTabIndex = 0;
//    
//    public Integer getActiveTabIndex() {
//       return activeTabIndex;
//    }
//    public void setActiveTabIndex(Integer activeTabIndex) {
//        this.activeTabIndex = activeTabIndex;
//    }
    private TabView messagesTab = new TabView();

    public TabView getMessagesTab() {
        return messagesTab;
    }

    public void setMessagesTab(TabView messagesTab) {
        this.messagesTab = messagesTab;
    }

    public void onTabChange(TabChangeEvent event) {
        TabView tabView = (TabView) event.getComponent();

        int activeIndex = tabView.getChildren().indexOf(event.getTab());

        this.messagesTab.setActiveIndex(activeIndex);

    }

//    public void onTabChange(TabChangeEvent event) {  
//     
//        TabView tabView = (TabView) event.getComponent();
//        currentData.setSP_Index(tabView.getChildren().indexOf(event.getTab())+1);
//    } 
//    public void onTabChange(TabChangeEvent event) 
//    {   
//        TabView tabView = (TabView) event.getComponent();
//        activeTabIndex = tabView.getChildren().indexOf(event.getTab());
//    }
//    public void onTabChange(TabChangeEvent event) {
//        FacesMessage msg = new FacesMessage("Tab Changed", "Active Tab: " + event.getTab().getTitle());
//        FacesContext.getCurrentInstance().addMessage(null, msg);
//    }
//    
    private int tabIndex = 1;

    public boolean handleTabChange() {
        ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
        String index = externalContext.getRequestParameterMap().get("tabIndex");
        setTabIndex(Integer.parseInt(index));
        return true;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
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
