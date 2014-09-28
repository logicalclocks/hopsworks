/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study;

import se.kth.bbc.study.filebrowser.FileSummary;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.TreeNode;
import se.kth.bbc.activity.ActivityController;
import se.kth.bbc.activity.ActivityMB;
import se.kth.bbc.activity.UserGroupsController;
import se.kth.bbc.activity.UsersGroups;
import se.kth.bbc.activity.UsersGroupsPK;
import se.kth.bbc.study.filebrowser.FileStructureListener;
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
    private List<Username> usernames;
    private StudyTeam studyTeamEntry;
    private List<Theme> selectedUsernames;
    private List<Theme> themes;
    private String sample_Id;
    List<SampleIdDisplay> filteredSampleIds;
    
    private String studyName;
    private String studyCreator;
    private int tabIndex;
    private String loginName;
    
    private StreamedContent file;

    private int manTabIndex = SHOW_TAB;
    
    private FileSummary selectedFile;
    private TreeNode selectedNode;
    
    private final List<FileStructureListener> fileListeners = new ArrayList<>();
    
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
        for (FileStructureListener l : fileListeners) {
            l.changeStudy(studyName);
        }
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
        try {
            if(!studyController.findStudy(study.getName())){
                study.setUsername(getUsername());
                study.setTimestamp(new Date());
                studyController.persistStudy(study);
                activity.addActivity(ActivityController.NEW_STUDY, study.getName(), "STUDY");
                addStudyMaster(study.getName());
                mkStudyDIR(study.getName());
                logger.log(Level.INFO, "{0} - study was created successfully.", study.getName());
                
                //addMessage("Study created! [" + study.getName() + "] study is owned by " + study.getUsername());
                setStudyName(study.getName());
                this.studyCreator = study.getUsername();
//                FacesContext context = FacesContext.getCurrentInstance();
//                context.getExternalContext().getFlash().setKeepMessages(true);
                return "studyPage";
            }
            
        } catch (IOException | EJBException | URISyntaxException exp) {
            addErrorMessageToUserAction("Failed: Study already exists!");
            logger.log(Level.SEVERE, "Study was not created!");
            return null;
        }
            addErrorMessageToUserAction("Failed: Study already exists!");
            logger.log(Level.SEVERE, "Study exists!");
            return null;
    }

    //create study on HDFS
    public void mkStudyDIR(String study_name) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        String rootDir = "Projects";

        String buildPath = File.separator + rootDir + File.separator + study_name;
        FileSystem fs = FileSystem.get(conf);
        Path path = new Path(buildPath);

        if (fs.exists(path)) {
            logger.log(Level.SEVERE, "Study directory exists : {0}", study_name);
            return;
        }
        fs.mkdirs(path, null);
        logger.log(Level.INFO, "Study directory was created on HDFS: {0}.", path.toString());
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

    //delete a study - only owner can perform the deletion
    public String deleteStudy() {
        
        String StudyOwner = studyController.filterByName(studyName);
        
        try {
            if (getUsername().equals(StudyOwner)) {
                studyController.removeStudy(studyName);
            }
            delStudyDIR(studyName);
            logger.log(Level.INFO, "{0} - study removed.", studyName);
        } catch (IOException | URISyntaxException | EJBException exp) {
            addErrorMessageToUserAction("Error: Study wasn't removed.");
            logger.log(Level.SEVERE, "{0} - study was not removed.", exp.getMessage());
            return null;
        }
        addMessage("Study removed.");
        FacesContext context = FacesContext.getCurrentInstance();
        context.getExternalContext().getFlash().setKeepMessages(true);
        return "indexPage";
    }

    //delete study dir from HDFS
    public void delStudyDIR(String study_name) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        String rootDir = "Projects";

        String buildPath = File.separator + rootDir + File.separator + study_name;
        FileSystem fs = FileSystem.get(conf);
        Path path = new Path(buildPath);

        if (!fs.exists(path)) {
            logger.log(Level.SEVERE, "Study directory does not exist : {0}", study_name);
            return;
        }
        fs.delete(path, true);
        logger.log(Level.INFO, "Directory was deleted on HDFS: {0}.", path.toString());
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

    //adding a record to sample id table
    public void addSample() throws IOException {
        
        boolean rec = sampleIDController.checkForExistingIDs(getSampleID(), studyName);
        
        try {
            if (!rec) {
                
                SampleIdsPK idPK = new SampleIdsPK(getSampleID(), studyName);
                SampleIds samId = new SampleIds(idPK);
                sampleIDController.persistSample(samId);
                logger.log(Level.INFO, "{0} - sample ID was created for : {1}.", new Object[]{getSampleID(), studyName});
                activity.addActivity(ActivityController.NEW_SAMPLE + getSampleID() + " ", studyName, "DATA");
                setLoginName(getUsername());
                getResponse().sendRedirect(getRequest().getContextPath() + "/bbc/uploader/sampleUploader.jsp");
                FacesContext.getCurrentInstance().responseComplete();                
                for (FileStructureListener l : fileListeners) {
                    l.newSample(idPK.getId());
                }
            } else {
                
                setLoginName(getUsername());
                getResponse().sendRedirect(getRequest().getContextPath() + "/bbc/uploader/sampleUploader.jsp");
                FacesContext.getCurrentInstance().responseComplete();
            }
            
        } catch (EJBException ejb) {
            
            addErrorMessageToUserAction("Error: Add sample transaction failed");
            logger.log(Level.SEVERE, "Error: Add sample transaction failed.");
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
                logger.log(Level.INFO, "{0} - sample file was created for : {1}.", new Object[]{getSampleID(), fileName});
                activity.addSampleActivity(ActivityController.NEW_SAMPLE + "[" + fileName + "]" + " file ", studyName, "DATA", getLoginName());
                for (FileStructureListener l : fileListeners) {
                    l.newFile(getSampleID(), fileType, fileName, SampleFileStatus.COPYING_TO_HDFS.getFileStatus());
                }
            }

        } catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Sample file wasn't created.");
            logger.log(Level.SEVERE, "Sample files were not created.");
            return;
        }
        mkDIRS(getSampleID(), fileType, fileName);
    }

    //Creating directory structure in HDFS
    public void mkDIRS(String sampleID, String fileType, String fileName) throws IOException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        String rootDir = "Projects";
        
        String buildPath = File.separator + rootDir + File.separator + studyName;
        FileSystem fs = FileSystem.get(conf);
        Path path = new Path(buildPath);
        
        try {

            if (fs.exists(path)) {
                Path.mergePaths(path, new Path(File.separator + sampleID + File.separator + fileType.toUpperCase().trim()));
                copyFromLocal(fileType, fileName, sampleID);
            }
                fs.mkdirs(path.suffix(File.separator + sampleID + File.separator + fileType.toUpperCase().trim()), null);
                copyFromLocal(fileType, fileName, sampleID);

        } catch (URISyntaxException uri) {
                logger.log(Level.SEVERE, "Directories were not created in HDFS...{0}", uri.getMessage());
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
        Path build = new Path(buildPath + File.separator + sampleId + File.separator + fileType.toUpperCase().trim() + File.separator + filename);
        
        InputStream is = new FileInputStream(fileFromLocal);
        if (!fs.exists(build)) {
            FSDataOutputStream os = fs.create(build, false);
            IOUtils.copyBytes(is, os, 131072, true);
        }
            logger.log(Level.INFO, "File, {0}, copied to HDFS: {1}", new Object[]{filename, build.toString()});
            logger.log(Level.INFO, "File size: {0} bytes", fs.getFileStatus(build).getLen());

        //Status update in SampleFiles
            updateFileStatus(sampleId, filename);
    }
    
    public void updateFileStatus(String id, String filename) {
        //TODO: update when finished uploading, when copying to hdfs
        try {
            sampleFilesController.update(id, filename);
            logger.log(Level.INFO, "{0} - sample file status was updated to: {1}", new Object[]{filename, "available"});
            for (FileStructureListener l : fileListeners) {
                l.updateStatus(id, filename.substring(filename.lastIndexOf('.') + 1), filename, "available");
            }
        } catch (EJBException ejb) {
            logger.log(Level.SEVERE, "Sample file status update failed.");
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
            logger.log(Level.INFO, "{0} - Sample was deleted from {1} in HDFS", new Object[]{sampleId, studyName});
        } else {
            logger.log(Level.SEVERE, "Sample id {0} does not exist", sampleId);
        }

            //remove the sample from SampleIds
            deleteSamples(sampleId);
    }
    
    public void deleteSamples(String id) {
        try {
            sampleIDController.removeSample(id, studyName);
            activity.addSampleActivity(ActivityController.REMOVED_SAMPLE + "[" + id + "]" + " ", studyName, "DATA", getUsername());
            logger.log(Level.INFO, "{0} - Sample was deleted from {1} in SampleFiles table", new Object[]{id, studyName});
        } catch (EJBException ejb) {
            logger.log(Level.SEVERE, "Sample deletion was failed.");
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
            logger.log(Level.INFO, "{0} - File type folder was deleted from {1} study in HDFS", new Object[]{fileType.toUpperCase(), studyName});
        } else {
            logger.log(Level.SEVERE, "{0} - File type folder does not exist", fileType.toUpperCase());
        }

            //remove file type records
            deleteFileTypes(sampleId, fileType);
    }
    
    public void deleteFileTypes(String sampleId, String fileType) {
        
        try {
            sampleFilesController.deleteFileTypeRecords(sampleId, studyName, fileType);
            activity.addSampleActivity(" removed " + "[" + fileType + "]" + " files " + " ", studyName, "DATA", getUsername());
        } catch (EJBException ejb) {
             logger.log(Level.SEVERE, "File type folder deletion was failed.");
        }
        
    }

    //Delete a file from a sample study
    public void deleteFileFromHDFS(String sampleId, String filename, String fileType) throws IOException, URISyntaxException {

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", this.nameNodeURI);
        FileSystem fs = FileSystem.get(conf);

        String rootDir = "Projects";
        String buildPath = File.separator + rootDir + File.separator + studyName;

        Path build = new Path(buildPath + File.separator + sampleId + File.separator + fileType.toUpperCase().trim());
        //check the file count inside a directory, if it is only one then recursive delete
        if (fs.exists(build)) {
            if(fileCount(fs, build) == 1) {
                fs.delete(build, true);
            } else{
                fs.delete(build.suffix(File.separator+filename), false);
            }
        } else {
            logger.log(Level.SEVERE, "File does not exist on path: {0}", build.toString());
        }

        logger.log(Level.INFO,"{0} - file was deleted from path: {1}", new Object[]{filename, build.toString()});
        //remove file record from SampleFiles
        deleteFileFromSampleFiles(sampleId, filename);
    }
    
    //count the number of files in a directory for a given path on HDFS
    public int fileCount(FileSystem fs, Path path) throws IOException, URISyntaxException{
    
            int count = 0;
            FileStatus[] files = fs.listStatus(path);
            for(FileStatus f: files){
                if(!f.getPath().getName().isEmpty())
                    count++;
            }
            //logger.log(Level.INFO, "file count: {0}", count);
        return count;
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

        try {

            if (!fs.exists(outputPath)) {
                System.out.println("Error: File does not exist for downloading" + fileName);
                logger.log(Level.SEVERE, "File does not exist on this [{0}] path of HDFS", outputPath.toString());
                return;
            }

            InputStream inStream = fs.open(outputPath, 1048576);
            file = new DefaultStreamedContent(inStream, "fastq/fasta/bam/sam/vcf", fileName);
            logger.log(Level.INFO, "{0} - file was downloaded from HDFS path: {1}", new Object[]{fileName, outputPath.toString()});

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
            delStudyDIR(studyName);
            logger.log(Level.INFO, "{0} - study removed.", studyName);
        } catch (IOException | URISyntaxException | EJBException exp) {
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
        System.out.println("CALLED REMOVE");
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
            for (FileStructureListener l : fileListeners) {
                l.removeFile(sampleId, type, filename);
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
            for (FileStructureListener l : fileListeners) {
                l.removeType(sampleId, foldername);
            }
            message = "Successfully removed folder " + foldername + ".";
        } else if (selectedFile.isSample()) {
            try {
                deleteSampleFromHDFS(selectedFile.getSampleID());
            } catch (IOException | URISyntaxException ex) {
                addErrorMessageToUserAction("Error", "Failed to remove sample.", "remove");
                return;
            }
            for (FileStructureListener l : fileListeners) {
                l.removeSample(selectedFile.getSampleID());
            }
            message = "Successfully removed sample " + selectedFile.getSampleID() + ".";
        } else {
            logger.log(Level.SEVERE, "Trying to remove a file that is not according to the hierarchy.");
            return;
        }
        addMessage("Success", message, "remove");
    }
    
    public void registerFileListener(FileStructureListener listener) {
        if (!fileListeners.contains(listener)) {
            fileListeners.add(listener);
        }
    }
}
