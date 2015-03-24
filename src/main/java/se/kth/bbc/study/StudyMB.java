package se.kth.bbc.study;

import se.kth.bbc.study.services.StudyServiceFacade;
import se.kth.bbc.study.services.StudyServiceEnum;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.primefaces.model.UploadedFile;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.activity.ActivityDetail;
import se.kth.bbc.activity.ActivityDetailFacade;
import se.kth.bbc.activity.LazyActivityModel;
import se.kth.bbc.activity.UserGroupsController;
import se.kth.bbc.activity.UsersGroups;
import se.kth.bbc.activity.UsersGroupsPK;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.security.ua.EmailBean;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;
import se.kth.bbc.study.privacy.StudyPrivacyManager;
import se.kth.bbc.study.privacy.model.Consent;

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
    private StudyFacade studyController;

    @EJB
    private StudyTeamFacade studyTeamController;

    @EJB
    private UserManager userMgr;

    @EJB
    private UserGroupsController userGroupsController;

    @EJB
    private ActivityFacade activityFacade;

    @EJB
    private ActivityDetailFacade activityDetailFacade;

    @EJB
    private FileOperations fileOps;

    @EJB
    private StudyServiceFacade studyServices;
    
    @EJB
    private StudyPrivacyManager privacyManager;
    
    @ManagedProperty(value = "#{clientSessionState}")
    private ClientSessionState sessionState;

    
    private TrackStudy study;
    private List<User> usernames;
    private StudyTeam studyTeamEntry;
    private List<Theme> selectedUsernames;
    private List<Theme> themes;
    private String sample_Id;

    private String studyName;
    private String studyCreator;
    private int tabIndex;
    private String loginName;
    private List<ActivityDetail> allActivities;
    
    private StudyServiceEnum[] selectedServices;

    private boolean deleteFilesOnRemove = true;

    private LazyActivityModel lazyModel = null;

    private Date date;

    private Date retentionPeriod;
    private Consent activeConset;

    private List<Consent> allConsent;
    
    private UploadedFile file;

    public void setActiveConset(Consent activeConset) {
        this.activeConset = activeConset;
    }

    public StudyMB() {
    }

    
    
 
    public UploadedFile getFile() {
        return file;
    }
    
    public void setFile(UploadedFile file) {
        this.file = file;
    }
    
    public void setSessionState(ClientSessionState sessionState) {
        this.sessionState = sessionState;
    }

    public List<User> getUsersname() {
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
            study = studyController.findByName(studyName);
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
        List<User> list = userMgr.filterUsersBasedOnStudy(getStudyName());
        themes = new ArrayList<>();
        int i = 0;
        for (User user : list) {
            themes.add(new Theme(i, user.getFname() + " " + user.getLname(), user.getEmail()));
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
        return getRequest().getRemoteUser();
    }

    public StudyRoleTypes[] getTeam() {
        return StudyRoleTypes.values();
    }

    public int countAllMembersPerStudy() {
        return studyTeamController.countMembersPerStudy(studyName).size();
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
        return getUsername().equals(getCreator());
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
     * @return
     */
    public String fetchStudy() {
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        String studyname = params.get("studyname");
        this.studyCreator = params.get("username");
        return fetchStudy(studyname);
    }

    public String fetchStudy(String studyname) {
        setStudyName(studyname);
        sessionState.setActiveStudyByName(studyName);
        return checkAccess();
    }

    public String checkAccess() {
        boolean res = studyTeamController.findUserForActiveStudy(studyName,
                getUsername());
        boolean rec = userGroupsController.checkForCurrentSession(getUsername());
        if (!res) {
            if (!rec) {
                userGroupsController.persistUserGroups(new UsersGroups(
                        new UsersGroupsPK(getUsername(), "GUEST")));
                logger.log(Level.FINE, "Guest role added for: {0}.", getUsername());
                return "studyPage";
            }
        }
        return "studyPage";
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
                logger.log(Level.FINE, "{0} - member added to study : {1}.", new Object[]{t.getName(), studyName});
                activityFacade.persistActivity(ActivityFacade.NEW_MEMBER + t.getName() + " ", studyName, sessionState.getLoggedInUsername());
            }
            if (!getSelectedUsernames().isEmpty()) {
                getSelectedUsernames().clear();
            }
        } catch (EJBException ejb) {
            MessagesController.addErrorMessage("Error: Adding team member failed.");
            logger.log(Level.SEVERE, "Adding members to study failed...{0}", ejb.getMessage());
            return null;
        }
        MessagesController.addInfoMessage("New Member Added!");
        return "studyPage";
    }

    public void itemSelect(SelectEvent e) {
        if (getSelectedUsernames().isEmpty()) {
            MessagesController.addErrorMessage("Error: People field cannot be empty.");
        }
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public void onTabChange(TabChangeEvent event) {
        switch (event.getTab().getTitle()) {
            case "All":
                setTabIndex(0);
                break;
            case "Personal":
                setTabIndex(1);
                break;
            case "Joined":
                setTabIndex(2);
                break;
            default:
                break;
        }
    }

    public boolean isCurrentOwner() {
        String email = getUsername();
        return email.equals(studyController.findOwner(studyName));
    }

    public String removeByName() {
        boolean success = false;
        try {
            studyController.removeByName(studyName);
            activityFacade.persistActivity(ActivityFacade.REMOVED_STUDY, studyName, sessionState.getLoggedInUsername());
            if (deleteFilesOnRemove) {
                String path = File.separator + Constants.DIR_ROOT + File.separator + studyName;
                success = fileOps.rmRecursive(path);
                if (!success) {
                    MessagesController.addErrorMessage(MessagesController.ERROR, "Failed to remove study files.");
                }
            }
            logger.log(Level.FINE, "{0} - study removed.", studyName);
        } catch (IOException e) {
            MessagesController.addErrorMessage("Error: Study wasn't removed.");
            return null;
        }
        if (success) {
            MessagesController.addInfoMessage("Success", "Study " + studyName + " was successfully removed.", "studyRemoved");
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
            lazyModel = new LazyActivityModel(activityDetailFacade, studyName);
            lazyModel.setRowCount((int) activityFacade.getStudyCount(studyName));
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

    /**
     * Return a list of UserGroups, which contain the members of this study per
     * role type.
     *
     * @return
     */
    public List<UserGroup> getGroupedMembers() {
        List<UserGroup> groupedUsers = new ArrayList<>();
        StudyRoleTypes[] roles = StudyRoleTypes.values();
        for (StudyRoleTypes role : roles) {
            List<User> mems = studyTeamController.findTeamMembersByName(studyName, role.getTeam());
            if (!mems.isEmpty()) {
                List<RoledUser> roleMems = new ArrayList<>();
                for (User u : mems) {
                    roleMems.add(new RoledUser(u.getEmail(), u.getFname() + " " + u.getLname(), role));
                }
                groupedUsers.add(new UserGroup(role, roleMems));
            }
        }
        return groupedUsers;
    }

    /**
     * Count the number of users with the given role in the current study.
     *
     * @param role
     * @return
     */
    public int countRoleUsers(String role) {
        return studyTeamController.countStudyTeam(studyName, role);
    }

    public class UserGroup {

        private StudyRoleTypes groupname;
        private List<RoledUser> members;

        public StudyRoleTypes getGroupname() {
            return groupname;
        }

        public List<RoledUser> getMembers() {
            return members;
        }

        public UserGroup(StudyRoleTypes groupname, List<RoledUser> members) {
            this.groupname = groupname;
            this.members = members;
        }

        public void setGroupname(StudyRoleTypes groupName) {
            this.groupname = groupName;
        }

        public void setMembers(List<RoledUser> members) {
            this.members = members;
        }
    }

    public class RoledUser {

        private String email;
        private String name;
        private StudyRoleTypes role;

        public RoledUser(String email, String name, StudyRoleTypes role) {
            this.email = email;
            this.name = name;
            this.role = role;
        }

        public String getEmail() {
            return email;
        }

        public String getName() {
            return name;
        }

        public StudyRoleTypes getRole() {
            return role;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setRole(StudyRoleTypes role) {
            this.role = role;
        }
    }

    /**
     * Check if the current study is owned by the user with given email.
     *
     * @param email
     * @return true if the study is owned by the user with given email
     */
    public boolean studyOwnedBy(String email) {
        TrackStudy t = studyController.findByName(studyName);
        if (t == null) {
            return false;
        } else {
            return t.getUsername().equalsIgnoreCase(email);
        }
    }

    public StudyServiceEnum[] getSelectedServices() {
        List<StudyServiceEnum> services = studyServices.findEnabledServicesForStudy(studyName);
        StudyServiceEnum[] reArr = new StudyServiceEnum[services.size()];
        return services.toArray(reArr);
    }

    public boolean shouldDrawTab(String service) {
        return studyServices.findEnabledServicesForStudy(studyName).contains(StudyServiceEnum.valueOf(service));
    }

    public void setSelectedServices(StudyServiceEnum[] selectedServices) {
        this.selectedServices = selectedServices;
    }

    public String updateServices() {
        studyServices.persistServicesForStudy(studyName, selectedServices);
        return "studyPage";
    }

    public void updateRetentionPeriod() {
        if (studyController.updateRetentionPeriod(studyName, this.retentionPeriod)) {

            MessagesController.addInfoMessage("Success: Updated retention period.");

        } else {

            MessagesController.addErrorMessage("Error: Update retention period failed.");
        }
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Date getRetentionPeriod() {
        this.retentionPeriod = studyController.getRetentionPeriod(studyName);
        return this.retentionPeriod;
    }

    public void setRetentionPeriod(Date retentionPeriod) {
        this.retentionPeriod = retentionPeriod;
    }

    public String updateConsent() {
        // TODO: send email to user
        //emailBean.sendEmail(studyname, studyname, studyname);

        return "";
    }

    public void uploadConsnet(){
        if (privacyManager.upload(file)) {
            FacesMessage message = new FacesMessage("Succesful", file.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }else {
            FacesMessage message = new FacesMessage("Error", file.getFileName() + " is not uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, message);
        }
    }
    
    public Consent getActiveConent() {
        this.activeConset = privacyManager.getActiveConsent(studyName);
        return this.activeConset;
    }

    public List<Consent> getAllConsent() {

        this.allConsent = privacyManager.getAllConsets(studyName);
        return this.allConsent;
    }



    public List<ActivityDetail> getAllActivities() {
        return allActivities;
    }

    public void showConsent(String path) {
    
        try {
            privacyManager.downloadPDF(path);
        } catch (IOException ex) {
            Logger.getLogger(StudyMB.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
