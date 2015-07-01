package se.kth.bbc.project;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.UploadedFile;
import se.kth.bbc.activity.Activity;
import se.kth.bbc.activity.ActivityFacade;
import se.kth.bbc.activity.LazyActivityModel;
import se.kth.bbc.activity.UserGroupsController;
import se.kth.bbc.activity.UsersGroups;
import se.kth.bbc.activity.UsersGroupsPK;
import se.kth.bbc.fileoperations.FileOperations;
import se.kth.bbc.lims.ClientSessionState;
import se.kth.bbc.lims.Constants;
import se.kth.bbc.lims.MessagesController;
import se.kth.bbc.project.fb.Inode;
import se.kth.bbc.project.fb.InodeFacade;
import se.kth.bbc.project.privacy.ProjectPrivacyManager;
import se.kth.bbc.project.privacy.model.Consent;
import se.kth.bbc.project.services.ProjectServiceEnum;
import se.kth.bbc.project.services.ProjectServiceFacade;
import se.kth.bbc.security.ua.UserManager;
import se.kth.bbc.security.ua.model.User;

/**
 *
 * @author roshan
 *
 */
@ManagedBean(name = "projectManagedBean",
        eager = true)
@SessionScoped
public class ProjectMB implements Serializable {

  private static final Logger logger = Logger.getLogger(ProjectMB.class.
          getName());
  private static final long serialVersionUID = 1L;
  private static final int TAB_INDEX_ALL_STUDIES = 0, TAB_INDEX_MY_STUDIES = 1, TAB_INDEX_JOINED_STUDIES
          = 2;

  @EJB
  private ProjectFacade projectFacade;

  @EJB
  private ProjectTeamFacade projectTeamController;

  @EJB
  private UserManager userMgr;

  @EJB
  private UserGroupsController userGroupsController;

  @EJB
  private ActivityFacade activityFacade;

  @EJB
  private FileOperations fileOps;

  @EJB
  private ProjectServiceFacade projectServices;

  @EJB
  private InodeFacade inodes;

  @EJB
  private ProjectPrivacyManager privacyManager;

  @ManagedProperty(value = "#{clientSessionState}")
  private ClientSessionState sessionState;

  private List<User> usernames;
  private ProjectTeam projectTeamEntry;
  private List<Theme> selectedUsernames;
  private List<Theme> themes;
  private String sample_Id;

  private Project project;

  private String projectName;
  private String projectCreator;
  private int tabIndex;
  private String loginName;

  private ProjectServiceEnum[] selectedServices;

  private boolean deleteFilesOnRemove = true;

  private LazyActivityModel lazyModel = null;

  private Date date;

  private Date retentionPeriod;
  private Consent activeConset;

  private List<Consent> allConsent;

  private UploadedFile file;

  private String filePath;
  private String ethicalStatus;

  public String getFilePath() {
    return filePath;
  }

  public void setFilePath(String filePath) {
    this.filePath = filePath;
  }

  public void setActiveConset(Consent activeConset) {
    this.activeConset = activeConset;
  }

  public ProjectMB() {
  }

  public UploadedFile getFile() {
    return file;
  }

  public void setFile(UploadedFile file) {
    this.file = file;
  }

  public String getEthicalStatus() {
    this.ethicalStatus = sessionState.getActiveProject().getEthicalStatus();
    return this.ethicalStatus;
  }

  public void setEthicalStatus(String ethicalStatus) {
    this.ethicalStatus = ethicalStatus;
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

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
    this.lazyModel = null;
  }

  public String getCreator() {
    return projectCreator;
  }

  public void setCreator(String projectCreator) {
    this.projectCreator = projectCreator;
  }

  public String getSampleID() {
    return sample_Id;
  }

  public void setSampleID(String sample_Id) {
    this.sample_Id = sample_Id;
  }

  public ProjectTeam getProjectTeamEntry() {
    if (projectTeamEntry == null) {
      projectTeamEntry = new ProjectTeam();
    }
    return projectTeamEntry;
  }

  public void setProjectTeamEntry(ProjectTeam projectTeamEntry) {
    this.projectTeamEntry = projectTeamEntry;
  }

  public List<Project> getProjectList() {
    return projectFacade.findAll();
  }

  public List<Project> getPersonalProject() {
    return projectFacade.findAllPersonalStudies(sessionState.getLoggedInUser());
  }

  public boolean isDeleteFilesOnRemove() {
    return deleteFilesOnRemove;
  }

  public void setDeleteFilesOnRemove(boolean deleteFilesOnRemove) {
    this.deleteFilesOnRemove = deleteFilesOnRemove;
  }

  public List<Theme> addThemes() {
    List<User> list = userMgr.filterUsersBasedOnProject(sessionState.
            getActiveProject());
    themes = new ArrayList<>();
    int i = 0;
    for (User user : list) {
      themes.add(new Theme(i, user.getFname() + " " + user.getLname(), user.
              getEmail()));
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
    return (HttpServletRequest) FacesContext.getCurrentInstance().
            getExternalContext().getRequest();
  }

  private HttpServletResponse getResponse() {
    return (HttpServletResponse) FacesContext.getCurrentInstance().
            getExternalContext().getResponse();
  }

  public String getUsername() {
    return getRequest().getRemoteUser();
  }

  public ProjectRoleTypes[] getTeam() {
    return ProjectRoleTypes.values();
  }

  public int countAllMembersInActiveProject() {
    return projectTeamController.
            countMembersInProject(sessionState.getActiveProject());
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

  /**
   * Get the current role of the logged in user in the current project.
   * <p>
   * @return
   */
  public String currentRoleInProject() {
    return projectTeamController.
            findCurrentRole(sessionState.getActiveProject(),
                    getUsername());
  }

  public List<Project> getAllStudiesPerUser() {
    return projectFacade.findAllMemberStudies(sessionState.getLoggedInUser());
  }

  public List<Project> getJoinedStudies() {
    return projectFacade.findAllJoinedStudies(sessionState.getLoggedInUser());
  }

  private long countAllStudiesPerUser() {
    return projectTeamController.countByMember(sessionState.getLoggedInUser());
  }

  private int countPersonalProject() {
    return projectFacade.findByUser(sessionState.getLoggedInUser()).size();
  }

  private int countJoinedProject() {
    return projectFacade.findAllJoinedStudies(sessionState.getLoggedInUser()).
            size();
  }

  //TODO: change this method to include the Project directly.
  /**
   * @return
   */
  public String fetchProject() {
    FacesContext fc = FacesContext.getCurrentInstance();
    Map<String, String> params = fc.getExternalContext().
            getRequestParameterMap();
    String projectname = params.get("projectname");
    this.projectCreator = params.get("username");
    if (!isProjectPresentInHdfs(projectname)) {
      return null;
    }
    return fetchProject(projectname);
  }

  private boolean isProjectPresentInHdfs(String projectname) {
    Inode root = inodes.getProjectRoot(projectname);
    if (root == null) {
      MessagesController.addErrorMessage("Project not found.",
              "The project's root folder was not found in HDFS. You will be unable to access its contents.",
              "loadError");
      logger.
              log(Level.INFO,
                      "Project folder not found in HDFS for project{0} .",
                      projectname);
      return false;
    }
    return true;
  }

  public String fetchProject(String projectname) {
    setProjectName(projectname);
    Project s = projectFacade.findByNameAndOwnerEmail(projectname, sessionState.
            getLoggedInUsername());
    sessionState.setActiveProject(s);
    return checkAccess();
  }

  public String fetchProject(Project project) {
    setProjectName(project.getName());
    sessionState.setActiveProject(project);
    return checkAccess();
  }

  public String checkAccess() {
    boolean res = projectTeamController.isUserMemberOfProject(sessionState.
            getActiveProject(),
            getUsername());
    boolean rec = userGroupsController.existsEntryForEmail(getUsername());
    if (!res) {
      if (!rec) {
        userGroupsController.persistUserGroups(new UsersGroups(
                new UsersGroupsPK(getUsername(), "GUEST")));
        logger.log(Level.FINE, "Guest role added for: {0}.", getUsername());
        return "projectPage";
      }
    }
    return "projectPage";
  }

  /**
   * Add
   *
   * @return
   */
  public synchronized String addToTeam() {
    try {
      Iterator<Theme> itr = getSelectedUsernames().listIterator();
      while (itr.hasNext()) {
        Theme t = itr.next();
        ProjectTeamPK stp = new ProjectTeamPK(sessionState.getActiveProject().
                getId(), t.getName());
        ProjectTeam st = new ProjectTeam(stp);
        st.setTimestamp(new Date());
        st.setTeamRole(projectTeamEntry.getTeamRole());
        projectTeamController.persistProjectTeam(st);
        logger.log(Level.FINE, "{0} - member added to project : {1}.",
                new Object[]{t.getName(), projectName});
        activityFacade.persistActivity(ActivityFacade.NEW_MEMBER + t.getName()
                + " ", sessionState.getActiveProject(), sessionState.
                getLoggedInUsername());
      }
      if (!getSelectedUsernames().isEmpty()) {
        getSelectedUsernames().clear();
      }
    } catch (EJBException ejb) {
      MessagesController.addErrorMessage("Error: Adding team member failed.");
      logger.log(Level.SEVERE, "Adding members to project failed...{0}", ejb.
              getMessage());
      return null;
    }
    MessagesController.addInfoMessage("New Member Added!");
    return "projectPage";
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
        setTabIndex(TAB_INDEX_ALL_STUDIES);
        break;
      case "Personal":
        setTabIndex(TAB_INDEX_MY_STUDIES);
        break;
      case "Joined":
        setTabIndex(TAB_INDEX_JOINED_STUDIES);
        break;
      default:
        break;
    }
  }

  public int getNumberOfDisplayedStudies() {
    switch (tabIndex) {
      case TAB_INDEX_ALL_STUDIES:
        return (int) countAllStudiesPerUser();
      case TAB_INDEX_JOINED_STUDIES:
        return countJoinedProject();
      case TAB_INDEX_MY_STUDIES:
        return countPersonalProject();
      default:
        throw new IllegalStateException(
                "Tab index can only be contained in the set {0,1,2}.");
    }
  }

  public boolean isAllProjectListEmpty() {
    return countAllStudiesPerUser() == 0;
  }

  public boolean isJoinedProjectListEmpty() {
    return countJoinedProject() == 0;
  }

  public boolean isPersonalProjectListEmpty() {
    return countPersonalProject() == 0;
  }

  public boolean isCurrentOwner() {
    String email = getUsername();
    return email.
            equals(projectFacade.findOwner(sessionState.getActiveProject()));
  }

  /**
   * Remove the currently active project.
   * <p>
   * @return
   */
  public String removeCurrentProject() {
    boolean success = false;
    try {
      projectFacade.removeProject(sessionState.getActiveProject());
      activityFacade.persistActivity(ActivityFacade.REMOVED_PROJECT,
              sessionState.
              getActiveProject(), sessionState.getLoggedInUser());
      if (deleteFilesOnRemove) {
        String path = File.separator + Constants.DIR_ROOT + File.separator
                + projectName;
        success = fileOps.rmRecursive(path);
        if (!success) {
          MessagesController.addErrorMessage(MessagesController.ERROR,
                  "Failed to remove project files.");
        }
      }
      logger.log(Level.FINE, "{0} - project removed.", projectName);
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to remove project " + sessionState.
              getActiveProject().getName() + ".", e);
      MessagesController.addErrorMessage("Error: Project wasn't removed.");
      return null;
    }
    if (success) {
      MessagesController.addInfoMessage("Success", "Project " + projectName
              + " was successfully removed.", "projectRemoved");
      FacesContext context = FacesContext.getCurrentInstance();
      context.getExternalContext().getFlash().setKeepMessages(true);
      deleteFilesOnRemove = true;
    }
    return "indexPage";
  }

  /**
   * Get a lazy datamodel containing activity on the current project.
   * <p>
   * @return
   */
  public LazyDataModel<Activity> getSpecificLazyModel() {
    if (lazyModel == null) {
      try {
        lazyModel = new LazyActivityModel(activityFacade, sessionState.
                getActiveProject());
        lazyModel.setRowCount((int) activityFacade.getProjectCount(sessionState.
                getActiveProject()));
      } catch (IllegalArgumentException e) {
        logger.log(Level.SEVERE, "Error loading lazy model.", e);
        this.lazyModel = null;
      }
    }
    return lazyModel;
  }

  /**
   * Redirect the user to the upload page.
   */
  public void redirectToUploader() {
    try {
      setLoginName(getUsername());
      getResponse().sendRedirect(getRequest().getContextPath()
              + "/bbc/uploader/sampleUploader.jsp");
      FacesContext.getCurrentInstance().responseComplete();
    } catch (IOException ex) {
      Logger.getLogger(ProjectMB.class.getName()).log(Level.SEVERE,
              "Failed to send redirect to uploader page.", ex);
    }
  }

  /**
   * Return a list of UserGroups, which contain the members of this project
   * per role type.
   *
   * @return
   */
  public List<UserGroup> getGroupedMembers() {
    List<UserGroup> groupedUsers = new ArrayList<>();
    ProjectRoleTypes[] roles = ProjectRoleTypes.values();
    for (ProjectRoleTypes role : roles) {
      List<User> mems = projectTeamController.findTeamMembersByProject(
              sessionState.
              getActiveProject(),
              role.getTeam());
      if (!mems.isEmpty()) {
        List<RoledUser> roleMems = new ArrayList<>();
        for (User u : mems) {
          roleMems.add(new RoledUser(u.getEmail(), u.getFname() + " " + u.
                  getLname(), role));
        }
        groupedUsers.add(new UserGroup(role, roleMems));
      }
    }
    return groupedUsers;
  }

  /**
   * Count the number of users with the given role in the current project.
   *
   * @param role
   * @return
   */
  public int countRoleUsers(String role) {
    return projectTeamController.countProjectTeam(sessionState.
            getActiveProject(),
            role);
  }

  public class UserGroup {

    private ProjectRoleTypes groupname;
    private List<RoledUser> members;

    public ProjectRoleTypes getGroupname() {
      return groupname;
    }

    public List<RoledUser> getMembers() {
      return members;
    }

    public UserGroup(ProjectRoleTypes groupname, List<RoledUser> members) {
      this.groupname = groupname;
      this.members = members;
    }

    public void setGroupname(ProjectRoleTypes groupName) {
      this.groupname = groupName;
    }

    public void setMembers(List<RoledUser> members) {
      this.members = members;
    }
  }

  public class RoledUser {

    private String email;
    private String name;
    private ProjectRoleTypes role;

    public RoledUser(String email, String name, ProjectRoleTypes role) {
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

    public ProjectRoleTypes getRole() {
      return role;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public void setName(String name) {
      this.name = name;
    }

    public void setRole(ProjectRoleTypes role) {
      this.role = role;
    }
  }

  /**
   * Check if the current project is owned by the user with given email.
   *
   * @param email
   * @return true if the project is owned by the user with given email
   */
  public boolean projectOwnedBy(String email) {
    Project currentProject = sessionState.getActiveProject();
    if (currentProject == null) {
      return false;
    }
    return currentProject.getOwner().getEmail().equalsIgnoreCase(email);
  }

  /**
   * Get an array of the services selected for the current project.
   * <p>
   * @return
   */
  public ProjectServiceEnum[] getSelectedServices() {
    List<ProjectServiceEnum> services = projectServices.
            findEnabledServicesForProject(
                    sessionState.getActiveProject());
    ProjectServiceEnum[] reArr = new ProjectServiceEnum[services.size()];
    return services.toArray(reArr);
  }

  /**
   * Check if the tab for the given project should be drawn.
   * <p>
   * @param service
   * @return
   */
  public boolean shouldDrawTab(String service) {
    return projectServices.findEnabledServicesForProject(sessionState.
            getActiveProject()).contains(
                    ProjectServiceEnum.valueOf(service));
  }

  /**
   * Set the extra services that have been selected for the current project.
   * <p>
   * @param selectedServices
   */
  public void setSelectedServices(ProjectServiceEnum[] selectedServices) {
    this.selectedServices = selectedServices;
  }

  /**
   * Persist the new selection of project services.
   * <p>
   * @return
   */
  public String updateServices() {
    projectServices.persistServicesForProject(sessionState.getActiveProject(),
            selectedServices);
    return "projectPage";
  }

  /**
   * Archive the current project. Should implement erasure coding on the
   * project folder.
   */
  public void archiveProject() {
    //archive the project
    boolean success = true;
    if (success) {
      projectFacade.archiveProject(projectName);
    }
  }

  /**
   * Unarchive the current project.
   */
  public void unarchiveProject() {
    //unarchive project
    boolean success = true;
    if (success) {
      projectFacade.unarchiveProject(projectName);
    }
  }

  /**
   * Check if the current project has been archived, in which case its
   * functionality is not available.
   * <p>
   * @return
   */
  public boolean isProjectArchived() {
    Project project = sessionState.getActiveProject();
    if (project == null) {
      logger.log(Level.SEVERE,
              "Trying to call if project is archived, but has not been set.");
      throw new IllegalStateException(
              "Cannot check on ProjectMB if project is archived if it has not been set.");
    }
    return project.getArchived();
  }

  public void updateRetentionPeriod() {
    if (projectFacade.updateRetentionPeriod(projectName, this.retentionPeriod)) {

      MessagesController.addInfoMessage("Success: Updated retention period.");

    } else {

      MessagesController.addErrorMessage(
              "Error: Update retention period failed.");
    }
  }

  public Date getDate() {
    return this.date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Date getRetentionPeriod() {
    this.retentionPeriod = projectFacade.getRetentionPeriod(projectName);
    return this.retentionPeriod;
  }

  public void setRetentionPeriod(Date retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  public void uploadConsent(FileUploadEvent event) {
    Consent consent = new Consent();
    consent.setType("CONSENT");
    try {
      consent.setConsentForm(IOUtils.toByteArray(event.getFile().
              getInputstream()));
    } catch (IOException ex) {
      Logger.getLogger(ProjectMB.class.getName()).log(Level.SEVERE, null, ex);
    }
    consent.setDate(new Date());
    consent.setProject(sessionState.getActiveProject());
    consent.setStatus("PENDING");
    consent.setName(event.getFile().getFileName());

    try {
      if (privacyManager.getConsentByName(consent.getName()) != null) {
        MessagesController.addErrorMessage("Error",
                "Select another file name that does not exist.");
        return;
      }
    } catch (ParseException ex) {
      MessagesController.addErrorMessage("Error", "Something went wrong!");
      return;
    }
    if (privacyManager.upload(consent)) {
      MessagesController.addInfoMessage("Success", event.getFile().getFileName()
              + " file uploaded successfully.");

    } else {
      MessagesController.addErrorMessage("Error", "Something went wrong!");

    }
  }

  public void uploadEthicalApproval(FileUploadEvent event) {
    Consent consent = new Consent();
    consent.setType("EAPPROVAL");
    try {
      consent.setConsentForm(IOUtils.toByteArray(event.getFile().
              getInputstream()));
    } catch (IOException ex) {
      Logger.getLogger(ProjectMB.class.getName()).log(Level.SEVERE, null, ex);
    }
    consent.setDate(new Date());
    consent.setProject(sessionState.getActiveProject());
    consent.setStatus("PENDING");
    consent.setName(event.getFile().getFileName());

    try {
      if (privacyManager.getConsentByName(consent.getName()) != null) {
        MessagesController.addErrorMessage("Error",
                "Select another file name that does not exist.");
        return;
      }
    } catch (ParseException ex) {
      MessagesController.addErrorMessage("Error", "Something went wrong!");
      return;
    }
    if (privacyManager.upload(consent)) {
      MessagesController.addInfoMessage("Success", event.getFile().getFileName()
              + " file uploaded successfully.");

    } else {
      MessagesController.addErrorMessage("Error", "Something went wrong!");

    }
  }

  public void uploadEthicaNonConsent(FileUploadEvent event) {
    Consent consent = new Consent();
    consent.setType("NONCONSENT");
    try {
      consent.setConsentForm(IOUtils.toByteArray(event.getFile().
              getInputstream()));
    } catch (IOException ex) {
      Logger.getLogger(ProjectMB.class.getName()).log(Level.SEVERE, null, ex);
    }
    consent.setDate(new Date());
    consent.setProject(sessionState.getActiveProject());
    consent.setStatus("PENDING");
    consent.setName(event.getFile().getFileName());

    try {
      if (privacyManager.getConsentByName(consent.getName()) != null) {
        MessagesController.addErrorMessage("Error",
                "Select another file name that does not exist.");
        return;
      }
    } catch (ParseException ex) {
      MessagesController.addErrorMessage("Error", "Something went wrong!");
      return;
    }
    if (privacyManager.upload(consent)) {
      MessagesController.addInfoMessage("Success", event.getFile().getFileName()
              + " file uploaded successfully.");

    } else {
      MessagesController.addErrorMessage("Error", "Something went wrong!");

    }
  }

  public Consent getActiveConsent() {
    this.activeConset = privacyManager.getActiveConsent(projectName);
    return this.activeConset;
  }

  public List<Consent> getAllConsent() {
    this.allConsent = privacyManager.getAllConsentsByProject(sessionState.
            getActiveProject());
    return this.allConsent;
  }

  public List<Activity> getAllActivitiesOnProject() {
    List<Activity> ad = activityFacade.getAllActivityOnProject(sessionState.
            getActiveProject());
    return ad;
  }

  public void showConsent(String consName) {

    try {
      Consent consent = privacyManager.getConsentByName(consName);
      privacyManager.downloadPDF(consent);
    } catch (ParseException | IOException ex) {
      Logger.getLogger(ProjectMB.class.getName()).log(Level.SEVERE, null, ex);
    }

  }
}
