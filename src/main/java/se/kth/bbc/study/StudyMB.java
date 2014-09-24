/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.sql.Timestamp;
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
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.primefaces.component.tabview.TabView;
import org.primefaces.context.RequestContext;
import org.primefaces.event.FlowEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityMB;
import se.kth.bbc.activity.UserGroupsController;
import se.kth.bbc.activity.UsersGroups;
import se.kth.bbc.activity.UsersGroupsPK;
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

    public final String nameNodeURI = "hdfs://localhost:9999";

    @EJB
    private StudyController studyController;

    @EJB
    private StudyTeamController studyTeamController;

    @EJB
    private UserFacade userFacade;

    @EJB
    private UserGroupsController userGroupsController;

    @EJB
    private SampleIdController sampleIDController;

    @EJB
    private SampleFilesController sampleFilesController;

    @ManagedProperty(value = "#{activityBean}")
    private ActivityMB activity;

    private TrackStudy study;
    private DatasetStudy dsStudy;
    private Dataset dataset;
    private List<Username> usernames;
    private StudyTeam studyTeamEntry;
    private List<Theme> selectedUsernames;
    private List<Theme> themes;
    private List<SampleIds> sampleIds;
    private String sample_Id;
    List<SampleIdDisplay> filteredSampleIds;

    private String studyName;
    private String studyCreator;
    private String newTeamRole;
    private String newChangedRole;
    private String setTeamRole;
    private String newRole;
    private String owner;
    private int tabIndex;
    private String loginName;

    private StreamedContent file;

    //private UIInput newTeamRole;
    private int manTabIndex = SHOW_TAB;

    private TreeNode root;
    private FileSummary selectedFile;
    private TreeNode selectedNode;

    public StudyMB() {
    }

    @PostConstruct
    public void init() {
        initTreeTable();
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

    public TreeNode getRoot() {
        return root;
    }

    public void setRoot(TreeNode t) {
        this.root = t;
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

    public String getOwnerRole() {
        return owner;
    }

    public void setOwnerRole(String owner) {
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

    public String getNewRole() {
        return newRole;
    }

    public void setNewRole(String newRole) {
        this.newRole = newRole;
    }

    public String getSampleID() {
        return sample_Id;
    }

    public void setSampleID(String sample_Id) {
        this.sample_Id = sample_Id;
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

    public List<StudyDetail> getPersonalStudy() {
        return studyController.findAllPersonalStudyDetails(getUsername());
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

    public List<SampleIdDisplay> completeSampleIDs(String query) {
        List<SampleIds> allSampleIds = sampleIDController.getExistingSampleIDs(studyName, getUsername());
        filteredSampleIds = new ArrayList<>();

        for (SampleIds t : allSampleIds) {
            if (t.getSampleIdsPK().getId().toLowerCase().contains(query)) {
                filteredSampleIds.add(new SampleIdDisplay(t.getSampleIdsPK().getId(), studyName));
            }
        }
        return filteredSampleIds;
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
        reOrder.add(StudyRoleTypes.AUDITOR);
        //}
        return reOrder;
    }

    public List<StudyRoleTypes> getTeamForGuestList() {

        List<StudyRoleTypes> reOrder = new ArrayList<>();
        //for(StudyRoleTypes role: StudyRoleTypes.values()){
        reOrder.add(StudyRoleTypes.AUDITOR);
        reOrder.add(StudyRoleTypes.MASTER);
        reOrder.add(StudyRoleTypes.RESEARCHER);
        //}
        return reOrder;
    }

    public SampleFileTypes[] getFileType() {
        return SampleFileTypes.values();
    }

    public SampleFileStatus[] getFileStatus() {
        return SampleFileStatus.values();
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

    public long countResearchAdmins() {
        return studyTeamController.countStudyTeam(studyName, "Research Admin");
    }

    public long countAuditors() {
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
//                for(StudyRoleTypes role: StudyRoleTypes.values()) {
            reOrder.add(StudyRoleTypes.RESEARCHER);
            reOrder.add(StudyRoleTypes.AUDITOR);
            reOrder.add(StudyRoleTypes.MASTER);

//                }
            return reOrder;

//        } else if (team.equals("Research Admin")) {
//
//            reOrder.add(StudyRoleTypes.RESEARCH_ADMIN);
//            reOrder.add(StudyRoleTypes.AUDITOR);
//            reOrder.add(StudyRoleTypes.RESEARCHER);
//            reOrder.add(StudyRoleTypes.MASTER);
//
//            return reOrder;
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

    public int countAllStudiesPerUser() {
        return studyController.findAllStudies(getUsername()).size();
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
     * study, and also adding a record to the StudyTeam table for setting role
     * as master for the study.
     *
     * @return
     */
    public String createStudy() {

        study.setUsername(getUsername());
        study.setTimestamp(new Date());
        try {
            studyController.persistStudy(study);
            activity.addActivity(ActivityController.NEW_STUDY, study.getName(), "STUDY");
            addStudyMaster(study.getName());
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Failed: Study already exists!");
            return null;
        }
        addMessage("Study created! [" + study.getName() + "] study is owned by " + study.getUsername());
        this.studyName = study.getName();
        this.studyCreator = study.getUsername();
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getFlash().setKeepMessages(true);
        return "studyPage";
    }

    /**
     * @return
     */
    public String fetchStudy() {

        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
        this.studyName = params.get("studyname");
        this.studyCreator = params.get("username");

        //System.out.println("Studyname: " + this.studyName + " - user: " + getUsername() + " - creator: " + this.studyCreator);
//        FacesContext facesContext = FacesContext.getCurrentInstance(); 
//        Map<String,Object> sessionMap = facesContext.getExternalContext().getSessionMap();
//        sessionMap.put("currentStudy", studyName);
        boolean res = studyTeamController.findUserForActiveStudy(studyName, getUsername());
        boolean rec = userGroupsController.checkForCurrentSession(getUsername());

        if (!res) {
            if (!rec) {
                userGroupsController.persistUserGroups(new UsersGroups(new UsersGroupsPK(getUsername(), "GUEST")));
                return "studyPage";
            }
        }
        //initialize the file browser
        initTreeTable();

        return "studyPage";
//        if (stList.iterator().hasNext()){
//            StudyTeam t = stList.iterator().next();
////            if(getRequest().getRequestedSessionId() != null && getRequest().isRequestedSessionIdValid()){
//                if(sessionMap.get("currentStudy").equals(t.getStudyTeamPK().getName())){
//                    System.out.println("Session set - "+ sessionMap.get("currentStudy").toString());
//                    userGroupsController.persistUserGroups(new UsersGroups(new UsersGroupsPK(newRole, studyName)));
//                    return "studyPage";
//            }
//        }
//                    studyTeamController.clearGroups(getUsername(), sessionMap.get("currentStudy").toString());
//                    sessionMap.remove("currentStudy");
//                    return null;
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

    //delete a study - only owner can perform the deletion
    public String deleteStudy() {

        String StudyOwner = studyController.filterByName(studyName);

        try {
            if (getUsername().equals(StudyOwner)) {
                studyController.removeStudy(study);
            }
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Study wasn't removed.");
            return null;
        }
        addMessage("Study removed.");
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getFlash().setKeepMessages(true);
        return "indexPage";
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
                activity.addActivity(ActivityController.NEW_MEMBER + t.getName() + " ", studyName, "STUDY");
            }

            if (!getSelectedUsernames().isEmpty()) {
                getSelectedUsernames().clear();
            }

        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Adding team member failed.");
            return null;
        }

        addMessage("New Member Added!");
        manTabIndex = TEAM_TAB;
        return "studyPage";
    }

    //adding a record to sample id table
    public void addSample() throws IOException {

        boolean rec = sampleIDController.checkForExistingIDs(getSampleID(), studyName);

        try {
            if (!rec) {

                SampleIdsPK idPK = new SampleIdsPK(getSampleID(), studyName);
                SampleIds samId = new SampleIds(idPK);
                sampleIDController.persistSample(samId);
                activity.addActivity(ActivityController.NEW_SAMPLE + getSampleID() + " ", studyName, "DATA");
                //System.out.println("Session id from bean "+getRequest().getSession().getId());
                setLoginName(getUsername());
                getResponse().sendRedirect(getRequest().getContextPath() + "/bbc/uploader/sampleUploader.jsp");
                FacesContext.getCurrentInstance().responseComplete();

            } else {

                setLoginName(getUsername());
                getResponse().sendRedirect(getRequest().getContextPath() + "/bbc/uploader/sampleUploader.jsp");
                FacesContext.getCurrentInstance().responseComplete();
            }

        } catch (EJBException ejb) {

            addErrorMessageToUserAction("Error: Transaction failed");
        }
    }

    public void createSampleFiles(String fileName, String fileType) throws IOException, URISyntaxException {
        boolean rec = sampleFilesController.checkForExistingSampleFiles(getSampleID(), fileName);

        try {
            if (!rec) {
                SampleFilesPK smPK = new SampleFilesPK(getSampleID(), fileName);
                SampleFiles sf = new SampleFiles(smPK);
                sf.setFileType(fileType);
                sf.setStatus(SampleFileStatus.COPYING_TO_HDFS.getFileStatus());
                sampleFilesController.persistSampleFiles(sf);
                System.out.println("loging name after set " + getLoginName());
                activity.addSampleActivity(ActivityController.NEW_SAMPLE + "[" + fileName + "]" + " file ", studyName, "DATA", getLoginName());
            }
            
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Sample file wasn't created.");
            return;
        }
        mkDIRS(fileType, fileName, getSampleID());
    }

    //Creating directory structure in HDFS
    public void mkDIRS(String fileType, String fileName, String sampleID) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        String rootDir = "Projects";

        String buildPath = File.separator + rootDir + File.separator + studyName;
        FileSystem fs = FileSystem.get(conf);
        Path path = new Path(buildPath);

        try {
            if (fs.exists(path)) {
                Path.mergePaths(path, new Path(File.separator + sampleID + File.separator + fileType.toUpperCase().trim()));
                //addMessage("Dataset directory created!" + "/" + getSampleID());
                //Path fullPath = new Path(path, new Path(File.separator+getSampleID()+File.separator+fileType.toUpperCase().trim()));
                copyFromLocal(fileType, fileName, sampleID);
            }
            fs.mkdirs(path.suffix(File.separator + sampleID + File.separator + fileType.toUpperCase().trim()), null);
            copyFromLocal(fileType, fileName, sampleID);

        } catch (IOException ioe) {
            System.err.println("IOException during operation" + ioe.getMessage());
        } finally {
            fs.close();
        }

    }

    //Copy file to HDFS 
    public void copyFromLocal(String fileType, String filename, String sampleId) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);

        String rootDir = "Projects";
        String buildPath = File.separator + rootDir + File.separator + studyName;

        File fileFromLocal = new File("/home/glassfish/data" + File.separator + filename);
        System.out.println("Path to read " + fileFromLocal.toString());
        Path build = new Path(buildPath + File.separator + sampleId + File.separator + fileType.toUpperCase().trim() + File.separator + filename);

        InputStream is = new FileInputStream(fileFromLocal);
        if (!fs.exists(build)) {
            FSDataOutputStream os = fs.create(build, false);
            IOUtils.copyBytes(is, os, 131072, true);
        } else {
            System.out.println("Error: File exist.");
        }
        System.out.println("Copied to hdfs " + build.toString());

        //Status update in SampleFiles
        updateFileStatus(sampleId, filename);
    }

    public void updateFileStatus(String id, String filename) {
        try {
            sampleFilesController.update(id, filename);
        } catch (EJBException ejb) {
            System.out.println("Status update failed");
        }
    }

    //Delete a sample from HDFS
    public void deleteSampleFromHDFS(String sampleId) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);

        String rootDir = "Projects";
        String buildPath = File.separator + rootDir + File.separator + studyName;

        Path build = new Path(buildPath + File.separator + sampleId);
        if (fs.exists(build)) {
            fs.delete(build, true);
        } else {
            System.out.println("Sample ID does not exist");
        }

        //remove the sample from SampleIds
        deleteSamples(sampleId);
    }

    public void deleteSamples(String id) {
        try {
            sampleIDController.removeSample(id, studyName);
            activity.addSampleActivity(ActivityController.REMOVED_SAMPLE + "[" + id + "]" + " ", studyName, "DATA", getUsername());
            System.out.println("loging name after set from delete " + getLoginName());
        } catch (EJBException ejb) {
            System.out.println("Sample deletion failed");
        }
    }

    //Delete a file type folder
    public void deleteFileTypeFromHDFS(String sampleId, String fileType) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);

        String rootDir = "Projects";
        String buildPath = File.separator + rootDir + File.separator + studyName;
        Path build = new Path(buildPath + File.separator + sampleId + File.separator + fileType.toUpperCase().trim());
        if (fs.exists(build)) {
            fs.delete(build, true);
        } else {
            System.out.println("File Type folder does not exist");
        }

        //remove file type records
        deleteFileTypes(sampleId, fileType);
    }

    public void deleteFileTypes(String sampleId, String fileType) {

        try {
            sampleFilesController.deleteFileTypeRecords(sampleId, studyName, fileType);
            activity.addSampleActivity(" removed " + "[" + fileType + "]" + " files " + " ", studyName, "DATA", getUsername());
        } catch (EJBException ejb) {
            System.out.println("Sample file type deletion failed");
        }

    }

    //Delete a file from a sample study
    public void deleteFileFromHDFS(String sampleId, String filename, String fileType) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);

        String rootDir = "Projects";
        String buildPath = File.separator + rootDir + File.separator + studyName;

        Path build = new Path(buildPath + File.separator + sampleId + File.separator + fileType.toUpperCase().trim() + File.separator + filename);
        if (fs.exists(build)) {
            fs.delete(build, false);
        } else {
            System.out.println("File does not exist");
        }

        //remove file record from SampleFiles
        deleteFileFromSampleFiles(sampleId, filename);
    }

    public void deleteFileFromSampleFiles(String id, String filename) {
        try {
            sampleFilesController.deleteFile(id, filename);
            activity.addSampleActivity(ActivityController.REMOVED_FILE + "[" + filename + "]" + " from sample " + "[" + id + "]" + " ", studyName, "DATA", getUsername());
        } catch (EJBException ejb) {
            System.out.println("Sample file deletion failed");
        }
    }

    //Download a file from HDFS
    public void fileDownloadEvent() throws IOException, URISyntaxException {

        String sampleId = selectedFile.getSampleID();
        String fileType = selectedFile.getType();
        String fileName = selectedFile.getFilename();

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);
        String rootDir = "Projects";
        String buildPath = File.separator + rootDir + File.separator + studyName;
        Path outputPath = new Path(buildPath + File.separator + sampleId + File.separator + fileType.toUpperCase().trim() + File.separator + fileName.trim());
        //System.out.println("download path "+outputPath.toString());

        try {

            if (!fs.exists(outputPath)) {
                System.out.println("Error: File does not exist for downloading" + fileName);
                return;
            }

            InputStream inStream = fs.open(outputPath, 1048576);
            file = new DefaultStreamedContent(inStream, "fastq/fasta/bam/sam/vcf", fileName);

        } finally {
            //inStream.close();
        }

    }

    public StreamedContent getFile() {
        return file;
    }

    public void setFile(StreamedContent file) {
        this.file = file;
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
            //System.out.println("All - " + getTabIndex());
        } else if (event.getTab().getTitle().equals("Personal")) {
            setTabIndex(1);
            //System.out.println("Personal - " + getTabIndex());
        } else if (event.getTab().getTitle().equals("Joined")) {
            setTabIndex(2);
            //System.out.println("Joined - " + getTabIndex());
        } else {
            //do nothing at the moment
        }

    }

    public String onComplete() {
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
        List<TrackStudy> lst = studyTeamController.findStudyMaster(studyName);
        for (TrackStudy tr : lst) {
            if (tr.getUsername().equals(email)) {
                return true;
            }
        }
        return false;
    }

    public String removeByName() {
        try {
            studyController.removeByName(studyName);
            activity.addActivity(ActivityController.REMOVED_STUDY, studyName, ActivityController.CTE_FLAG_STUDY);
        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Study wasn't removed.");
            return null;
        }
        addMessage("Success", "Study " + studyName + " was successfully removed.", "studyRemoved");
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getFlash().setKeepMessages(true);
        return "indexPage";
    }

    public boolean isRemoved(String studyName) {
        TrackStudy item = studyController.findByName(studyName);
        return item == null;
    }

    private void initTreeTable() {
        //root node
        root = new DefaultTreeNode(new FileSummary("", "", "", "", ""), null);
        //level 1: study
        TreeNode studyRoot = new DefaultTreeNode(new FileSummary(studyName, "", "", "", ""), root);
        //level 2: all samples
        List<SampleIds> samples = sampleIDController.findAllByStudy(studyName);
        for (SampleIds sam : samples) {
            TreeNode node = new DefaultTreeNode(new FileSummary(studyName, sam.getSampleIdsPK().getId(), "", "", ""), studyRoot);
            //level 3: all extensions
            List<String> extensions = sampleFilesController.findAllExtensionsForSample(sam.getSampleIdsPK().getId());
            for (String s : extensions) {
                TreeNode typeFolder = new DefaultTreeNode(new FileSummary(studyName, sam.getSampleIdsPK().getId(), s, "", ""), node);
                //level 4: all files
                List<SampleFiles> files = sampleFilesController.findAllByIdType(sam.getSampleIdsPK().getId(), s);
                for (SampleFiles file : files) {
                    TreeNode leaf = new DefaultTreeNode(new FileSummary(studyName, sam.getSampleIdsPK().getId(), s, file.getSampleFilesPK().getFilename(), file.getStatus()), typeFolder);
                }
            }
        }
    }

//    public void download() {
//        try {
//            fileDownloadEvent(selectedFile.getSampleID(), selectedFile.getType(), selectedFile.getFilename());
//        } catch (IOException | URISyntaxException ex) {
//            logger.log(Level.SEVERE,"FAILED DOWNLOAD");
//            //addErrorMessageToUserAction("Error", "Failed to download "+selectedFile.getFilename(), "remove");
//        }
//    }
    public FileSummary getSelectedFile() {
        return selectedFile;
    }

    public void setSelectedNode(TreeNode selectedNode) {
        if (selectedNode != null) {
            this.selectedFile = (FileSummary) selectedNode.getData();
        }
        this.selectedNode = selectedNode;
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedFile(FileSummary file) {
        this.selectedFile = file;
    }

    public void removeFile() {
        //check if sample or file
        String message;
        if (selectedFile.isFile()) {
            String sampleId = selectedFile.getSampleID();
            String type = selectedFile.getType();
            String filename = selectedFile.getFilename();
            try {
                deleteFileFromHDFS(sampleId, filename, type);
            } catch (IOException | URISyntaxException ex) {
                addErrorMessageToUserAction("Error", "Failed to remove file.", "remove");
                return;
            }
            message = "Successfully removed file " + filename + ".";
        } else if (selectedFile.isTypeFolder()) {
            String sampleId = selectedFile.getSampleID();
            String foldername = selectedFile.getType();
            try {
                deleteFileTypeFromHDFS(sampleId, foldername);
            } catch (IOException | URISyntaxException ex) {
                addErrorMessageToUserAction("Error", "Failed to remove folder.", "remove");
                return;
            }
            message = "Successfully removed folder " + foldername + ".";
        } else if (selectedFile.isSample()) {
            try {
                deleteSampleFromHDFS(selectedFile.getSampleID());
            } catch (IOException | URISyntaxException ex) {
                addErrorMessageToUserAction("Error", "Failed to remove sample.", "remove");
                return;
            }
            message = "Successfully removed sample " + selectedFile.getSampleID() + ".";
        } else {
            logger.log(Level.SEVERE, "Trying to remove a file that is not according to the hierarchy.");
            return;
        }
        addMessage("Success", message, "remove");
    }
}
