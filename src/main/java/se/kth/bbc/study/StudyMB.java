package se.kth.bbc.study;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.LazyDataModel;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityDetail;
import se.kth.bbc.activity.ActivityMB;
import se.kth.bbc.activity.LazyActivityModel;
import se.kth.bbc.activity.UserGroupsController;
import se.kth.bbc.activity.UsersGroups;
import se.kth.bbc.activity.UsersGroupsPK;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.study.fb.InodeFacade;
import se.kth.bbc.fileoperations.FileSystemOperations;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.yarn.Client;
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
    public static final int TEAM_TAB = 1;
    public static final int SHOW_TAB = 0;

    /**
     * ************************************
     *
     * TODO: isolate file system operations to FileOperations.java (or anywhere
     * really). Remains: invert operation of FileOperationsManagedBean and
     * StudyMB: StudyMB has reference to FileOPMB and calls methods on it with
     * studyname as a parameter, or studyname is passed as parameter through the
     * view. Then move all file operations away (create studyDir e.g.).
     *
     *
     */
    public final String nameNodeURI = "hdfs://snurran.sics.se:9999";

    @EJB
    private StudyController studyController;

    @EJB
    private StudyTeamController studyTeamController;

    @EJB
    private UserFacade userFacade;

    @EJB
    private UserGroupsController userGroupsController;

    @EJB
    private ActivityController activityController;

    @EJB
    private InodeFacade inodes;

    @EJB
    private FileOperations fileOps;

    @ManagedProperty(value = "#{activityBean}")
    private ActivityMB activity;

    private TrackStudy study;
    private List<Username> usernames;
    private StudyTeam studyTeamEntry;
    private List<Theme> selectedUsernames;
    private List<Theme> themes;
    private String sample_Id;

    private String studyName;
    private String studyCreator;
    private int tabIndex;
    private String loginName;

    private int manTabIndex = SHOW_TAB;

    private boolean deleteFilesOnRemove = true;

    private LazyActivityModel lazyModel = null;

    public StudyMB() {
    }

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

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public String getStudyName() {
        return studyName;
    }

    public void setStudyName(String studyName) {
        this.studyName = studyName;
        this.lazyModel = null;
    }

    public String getCreator() {
        return studyCreator;
    }

    public void setCreator(String studyCreator) {
        this.studyCreator = studyCreator;
    }

    public String getSampleID() {
        return sample_Id;
    }

    public void setSampleID(String sample_Id) {
        this.sample_Id = sample_Id;
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

    public StudyTeam getStudyTeamEntry() {
        if (studyTeamEntry == null) {
            studyTeamEntry = new StudyTeam();
        }
        return studyTeamEntry;
    }

    public void setStudyTeamEntry(StudyTeam studyTeamEntry) {
        this.studyTeamEntry = studyTeamEntry;
    }

    public List<TrackStudy> getStudyList() {
        return studyController.findAll();
    }

    public List<StudyDetail> getPersonalStudy() {
        return studyController.findAllPersonalStudyDetails(getUsername());
    }

    public long getAllStudy() {
        return studyController.getAllStudy(getUsername());
    }

    public int getNOfMembers() {
        return studyController.getMembers(getStudyName());
    }

    public boolean isDeleteFilesOnRemove() {
        return deleteFilesOnRemove;
    }

    public void setDeleteFilesOnRemove(boolean deleteFilesOnRemove) {
        this.deleteFilesOnRemove = deleteFilesOnRemove;
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
        }

        return themes;
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public List<Theme> completeUsername(String query) {
        List<Theme> allThemes = addThemes();
        List<Theme> filteredThemes = new ArrayList<>();

        for (Theme t : allThemes) {
            if (t.getName().toLowerCase().contains(query)) {
                filteredThemes.add(t);
            }
        }
        return filteredThemes;
    }

    public List<Theme> getSelectedUsernames() {
        return this.selectedUsernames;
    }

    public void setSelectedUsernames(List<Theme> selectedUsernames) {
        this.selectedUsernames = selectedUsernames;
    }

    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }

    private HttpServletResponse getResponse() {
        return (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
    }

    public String getUsername() {
        return getRequest().getUserPrincipal().getName();
    }

    public StudyRoleTypes[] getTeam() {
        return StudyRoleTypes.values();
    }

    public int countAllMembersPerStudy() {
        return studyTeamController.countMembersPerStudy(studyName).size();
    }

    public int countMasters() {
        return (studyTeamController.countStudyTeam(studyName, "Master"));
    }

    public int countResearchers() {
        return studyTeamController.countStudyTeam(studyName, "Researcher");
    }

    public int countResearchAdmins() {
        return studyTeamController.countStudyTeam(studyName, "Research Admin");
    }

    public int countAuditors() {
        return studyTeamController.countStudyTeam(studyName, "Auditor");
    }

    public List<Username> getMastersList() {
        return studyTeamController.findTeamMembersByName(studyName, "Master");
    }

    public List<Username> getResearchersList() {
        return studyTeamController.findTeamMembersByName(studyName, "Researcher");
    }

    public List<Username> getResearchAdminList() {
        return studyTeamController.findTeamMembersByName(studyName, "Research Admin");
    }

    public List<Username> getAuditorsList() {
        return studyTeamController.findTeamMembersByName(studyName, "Auditor");
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

    public boolean checkOwnerForSamples() {

        if (getUsername().equals(getCreator())) {
            return true;
        } else {
            return false;
        }
    }

    public String checkCurrentUser(String email) {

        if (email.equals(getUsername())) {
            return email;
        }

        return null;
    }

    public String renderComponentList() {
        List<StudyTeam> st = studyTeamController.findCurrentRole(studyName, getUsername());
        if (st.iterator().hasNext()) {
            StudyTeam t = st.iterator().next();
            return t.getTeamRole();
        }
        return null;
    }

    public List<StudyRoleTypes> getListBasedOnCurrentRole(String email) {

        String team = studyTeamController.findByPrimaryKey(studyName, email).getTeamRole();
        List<StudyRoleTypes> reOrder = new ArrayList<>();

        if (team.equals("Researcher")) {
            reOrder.add(StudyRoleTypes.RESEARCHER);
            reOrder.add(StudyRoleTypes.AUDITOR);
            reOrder.add(StudyRoleTypes.MASTER);

            return reOrder;

        } else if (team.equals("Auditor")) {
            reOrder.add(StudyRoleTypes.AUDITOR);
            reOrder.add(StudyRoleTypes.RESEARCHER);
            reOrder.add(StudyRoleTypes.MASTER);
            return reOrder;
        } else {
            return null;
        }
    }

    public int getAllStudyUserTypesListSize() {
        return studyTeamController.findMembersByStudy(studyName).size();
    }

    public List<StudyTeam> getAllStudyUserTypesList() {
        return studyTeamController.findMembersByStudy(studyName);
    }

    public List<StudyDetail> getAllStudiesPerUser() {
        return studyController.findAllStudyDetails(getUsername());
    }

    public List<StudyDetail> getJoinedStudies() {
        return studyController.findJoinedStudyDetails(getUsername());
    }

    public List<StudyTeam> getTeamList() {
        return studyTeamController.findMembersByStudy(studyName);
    }

    public long countAllStudiesPerUser() {
        return studyTeamController.countByMember(getUsername());
    }

    public int countPersonalStudy() {
        return studyController.findByUser(getUsername()).size();
    }

    public int countJoinedStudy() {
        boolean check = studyController.checkForStudyOwnership(getUsername());

        if (check) {
            return studyController.findJoinedStudies(getUsername()).size();
        } else {
            return studyController.QueryForNonRegistered(getUsername()).size();
        }
    }

    /**
     * Get the current username from session and sets it as the creator of the
     * study, and also adding a record to the StudyTeam table for setting the
     * role as master for within study.
     *
     * @return
     */
    public String createStudy() {
        try {
            if (!studyController.findStudy(study.getName())) {
                study.setUsername(getUsername());
                study.setTimestamp(new Date());
                studyController.persistStudy(study);
                activity.addActivity(ActivityController.NEW_STUDY, study.getName(), "STUDY");
                addStudyMaster(study.getName());
                mkStudyDIR(study.getName());
                logger.log(Level.INFO, "{0} - study was created successfully.", study.getName());

                setStudyName(study.getName());
                this.studyCreator = study.getUsername();
                return "studyPage";

            } else {

                addErrorMessageToUserAction("Failed: Study already exists!");
                logger.log(Level.SEVERE, "Study exists!");
                return null;
            }

        } catch (IOException | EJBException | URISyntaxException exp) {
            addErrorMessageToUserAction("Failed: Study already exists!");
            logger.log(Level.SEVERE, "Study was not created!");
            return null;
        }

    }

    //create study on HDFS
    public void mkStudyDIR(String studyName) throws IOException, URISyntaxException {

        String rootDir = FileSystemOperations.DIR_ROOT;
        String studyPath = File.separator + rootDir + File.separator + studyName;
        String resultsPath = studyPath + File.separator + FileSystemOperations.DIR_RESULTS;
        String cuneiformPath = studyPath + File.separator + FileSystemOperations.DIR_CUNEIFORM;
        String samplesPath = studyPath + File.separator + FileSystemOperations.DIR_SAMPLES;

        fileOps.mkDir(studyPath);
        fileOps.mkDir(resultsPath);
        fileOps.mkDir(cuneiformPath);
        fileOps.mkDir(samplesPath);
    }

    /**
     * @return
     */
    public String fetchStudy() {

        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        setStudyName(params.get("studyname"));
        this.studyCreator = params.get("username");

        boolean res = studyTeamController.findUserForActiveStudy(studyName, getUsername());
        boolean rec = userGroupsController.checkForCurrentSession(getUsername());

        if (!res) {
            if (!rec) {
                userGroupsController.persistUserGroups(new UsersGroups(new UsersGroupsPK(getUsername(), "GUEST")));
                logger.log(Level.INFO, "Guest role added for: {0}.", getUsername());
                return "studyPage";
            }
        }

        return "studyPage";
    }

    //Set the study owner as study master in StudyTeam table
    public void addStudyMaster(String study_name) {

        StudyTeamPK stp = new StudyTeamPK(study_name, getUsername());
        StudyTeam st = new StudyTeam(stp);
        st.setTeamRole("Master");
        st.setTimestamp(new Date());

        try {
            studyTeamController.persistStudyTeam(st);
            logger.log(Level.INFO, "{0} - added the study owner as a master.", study.getName());
        } catch (EJBException ejb) {
            System.out.println("Add study master failed" + ejb.getMessage());
            logger.log(Level.SEVERE, "{0} - adding the study owner as a master failed.", ejb.getMessage());
        }

    }

    //add members to a team - bulk persist 
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
                logger.log(Level.INFO, "{0} - member added to study : {1}.", new Object[]{t.getName(), studyName});
                activity.addActivity(ActivityController.NEW_MEMBER + t.getName() + " ", studyName, "STUDY");
            }

            if (!getSelectedUsernames().isEmpty()) {
                getSelectedUsernames().clear();
            }

        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Adding team member failed.");
            logger.log(Level.SEVERE, "Adding members to study failed...{0}", ejb.getMessage());
            return null;
        }

        addMessage("New Member Added!");
        manTabIndex = TEAM_TAB;
        return "studyPage";
    }

    public void itemSelect(SelectEvent e) {
        if (getSelectedUsernames().isEmpty()) {
            addErrorMessageToUserAction("Error: People field cannot be empty.");
        }
    }

    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addMessage(String summary, String mess, String anchor) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, mess);
        FacesContext.getCurrentInstance().addMessage(anchor, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }

    public void addErrorMessageToUserAction(String summary, String message, String anchor) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, summary, message);
        FacesContext.getCurrentInstance().addMessage(anchor, errorMessage);
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public void onTabChange(TabChangeEvent event) {
        if (event.getTab().getTitle().equals("All")) {
            setTabIndex(0);
        } else if (event.getTab().getTitle().equals("Personal")) {
            setTabIndex(1);
        } else if (event.getTab().getTitle().equals("Joined")) {
            setTabIndex(2);
        } else {
            //
        }

    }

    /*
     Used for navigating to the second tab immediately.
     */
    public int getManTabIndex() {
        int val = manTabIndex;
        manTabIndex = SHOW_TAB;
        return val;
    }

    public void setManTabIndex(int mti) {
        manTabIndex = mti;
    }

    public boolean isCurrentOwner() {
        String email = getUsername();
        return email.equals(studyController.findOwner(studyName));
    }

    public String removeByName() {
        boolean success = false;
        try {
            studyController.removeByName(studyName);
            activity.addActivity(ActivityController.REMOVED_STUDY, studyName, ActivityController.CTE_FLAG_STUDY);
            if (deleteFilesOnRemove) {
                String path = File.separator + FileSystemOperations.DIR_ROOT + File.separator + studyName;
                success = fileOps.rmRecursive(path);
                if (!success) {
                    MessagesController.addErrorMessage(MessagesController.ERROR, "Failed to remove study files.");
                }
            }
            logger.log(Level.INFO, "{0} - study removed.", studyName);
        } catch (IOException e) {
            addErrorMessageToUserAction("Error: Study wasn't removed.");
            return null;
        }
        if (success) {
            addMessage("Success", "Study " + studyName + " was successfully removed.", "studyRemoved");
            FacesContext context = FacesContext.getCurrentInstance();
            context.getExternalContext().getFlash().setKeepMessages(true);
            deleteFilesOnRemove = true;
        }
        return "indexPage";
    }

    public boolean isRemoved(String studyName) {
        TrackStudy item = studyController.findByName(studyName);
        return item == null;
    }

    public LazyDataModel<ActivityDetail> getSpecificLazyModel() {
        if (lazyModel == null) {
            lazyModel = new LazyActivityModel(activityController, studyName);
            lazyModel.setRowCount((int) activityController.getStudyCount(studyName));
        }
        return lazyModel;
    }

    public void redirectToUploader() {
        try {
            setLoginName(getUsername());
            getResponse().sendRedirect(getRequest().getContextPath() + "/bbc/uploader/sampleUploader.jsp");
            FacesContext.getCurrentInstance().responseComplete();
        } catch (IOException ex) {
            //TODO: make redirect better...
            Logger.getLogger(StudyMB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void test() {
        try {
            String[] args = {"-w", "/home/glassfish/roshan/hiway-0.2.0-SNAPSHOT/wordcount.cf", "/home/glassfish/roshan/hiway-0.2.0-SNAPSHOT/wordcount/benzko.txt", "/home/glassfish/testRes/out.txt"};
            Client c = Client.getInitiatedClient(args);
            c.run();
        } catch (Exception ex) {
            Logger.getLogger(StudyMB.class.getName()).log(Level.SEVERE, null, ex);
            addErrorMessageToUserAction("Failed.");
        }
    }

}
