package io.hops.hopsworks.common.project;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.HdfsInodeAttributes;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroups;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstate;
import io.hops.hopsworks.common.dao.jobhistory.YarnApplicationstateFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobDescription;
import io.hops.hopsworks.common.dao.jobs.description.JobDescriptionFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.log.operation.OperationsLog;
import io.hops.hopsworks.common.dao.log.operation.OperationsLogFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.payment.ProjectPaymentAction;
import io.hops.hopsworks.common.dao.project.payment.ProjectPaymentsHistory;
import io.hops.hopsworks.common.dao.project.payment.ProjectPaymentsHistoryFacade;
import io.hops.hopsworks.common.dao.project.payment.ProjectPaymentsHistoryPK;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectRoleTypes;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamPK;
import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.consent.ConsentStatus;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.dao.user.sshkey.SshKeys;
import io.hops.hopsworks.common.dao.user.sshkey.SshkeysFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FolderNameValidator;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.exception.ProjectInternalFoldersFailedException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.LocalhostServices;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ValidationException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.json.JSONObject;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectController {

  private final static Logger logger = Logger.getLogger(ProjectController.class.
          getName());
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private ProjectPaymentsHistoryFacade projectPaymentsHistoryFacade;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private UserManager userBean;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private ProjectServiceFacade projectServicesFacade;
  @EJB
  private InodeFacade inodes;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private SshkeysFacade sshKeysBean;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private Settings settings;
  @EJB
  private CertsFacade userCertsFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private HdfsUsersFacade hdfsUsersFacade;
  @EJB
  private OperationsLogFacade operationsLogFacade;
  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private JobDescriptionFacade jobFacade;
  @EJB
  private YarnApplicationstateFacade yarnApplicationstateFacade;
  @EJB
  private KafkaFacade kafkaFacade;
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

   /**
   * Creates a new project(project), the related DIR, the different services in
   * the project, and the master of the
   * project.
   * <p>
   * This needs to be an atomic operation (all or nothing) REQUIRES_NEW will
   * make sure a new transaction is created even
   * if this method is called from within a transaction.
   * <p/>
   * @param newProject
   * @param email
   * @param dfso
   * @return
   * @throws IllegalArgumentException if the project name already exists.
   * @throws io.hops.hopsworks.common.exception.AppException
   * @throws IOException if the DIR associated with the project could not be
   * created. For whatever reason.
   */
  public Project createProject(ProjectDTO projectDTO, Users owner,
          List<String> failedMembers) throws AppException {

    //check that the project name is ok
    String projectName = projectDTO.getProjectName();
    try {
      FolderNameValidator.isValidName(projectName);
    } catch (ValidationException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.INVALID_PROJECT_NAME);
    }

    List<ProjectServiceEnum> projectServices = new ArrayList<>();
    if (projectDTO.getServices() != null) {
      for (String s : projectDTO.getServices()) {
        try {
          ProjectServiceEnum se = ProjectServiceEnum.valueOf(s.toUpperCase());
          se.toString();
          projectServices.add(se);
        } catch (IllegalArgumentException iex) {
          logger.log(Level.SEVERE,
              ResponseMessages.PROJECT_SERVICE_NOT_FOUND, iex);
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), s
              + ResponseMessages.PROJECT_SERVICE_NOT_FOUND);
        }
      }
    }
    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      dfso = dfs.getDfsOps();
      /*
       * create a project in the database
       * if the creation go through it means that there is no other project with
       * the same name.
       * this project creation act like a lock, no other project can be created
       * with the same name
       * until this project is removed from the database
       */
      Project project = null;
      try {
        try {
          project = createProject(projectName, owner, projectDTO.
                  getDescription(), dfso);
        } catch (EJBException ex) {
          logger.log(Level.WARNING, null, ex);
          Path dumy = new Path("/tmp/" + project.getName());
          dfso.rm(dumy, true);
          throw new AppException(Response.Status.CONFLICT.
                  getStatusCode(), "A project with this name already exist");
        }
      }catch (AppException ex) {
        throw ex;
      }catch (Exception ex) {
        logger.log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "An error occurend when creating the project");
      }

      verifyProject(project, dfso);
      
      String username = hdfsUsersBean.getHdfsUserName(project, owner);
      if (username == null || username.isEmpty()) {
        cleanup(project);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "wrong user name");
      }
      udfso = dfs.getDfsOps(username);
      if (udfso == null) {
        cleanup(project);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "error geting access to the file system");
      }

      //all the verifications have passed, we can now create the project  
      //create the project folder
      String projectPath = null;
      try {
        projectPath = mkProjectDIR(projectName, dfso);
      } catch (IOException | EJBException ex) {
        cleanup(project);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "problem creating project folder");
      }
      if (projectPath == null) {
        cleanup(project);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "problem creating project folder");
      }
      //update the project with the project folder inode
      try {
        setProjectInode(project, dfso);
      } catch (AppException |EJBException ex) {
        cleanup(project);
        throw ex;
      } catch (IOException ex){
        cleanup(project);
        logger.log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "An error occurend when creating the project");
      }

      //set payment and quotas
      try {

        setProjectOwnerAndQuotas(project, settings.getHdfsDefaultQuotaInMBs(),
                dfso, owner);
        
        
      } catch (IOException | EJBException ex) {
        cleanup(project);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "could not set folder quota");
      }

      try {
        //create certificate for this user
        createCertificates(project, owner);
      } catch (IOException | EJBException ex) {
        Logger.getLogger(ProjectController.class.getName()).log(Level.SEVERE,
                null,
                ex);
        cleanup(project);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "error while creating certificats");
      }

      //add the services for the project
      addServices(project, projectServices, owner.getEmail());

      try {
        hdfsUsersBean.addProjectFolderOwner(project, dfso);
        createProjectLogResources(owner, project, dfso, udfso);
      } catch (IOException | EJBException ex) {
        Logger.getLogger(ProjectController.class.getName()).log(Level.SEVERE,null,ex);
        cleanup(project);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "error while creating project sub folders");
      } catch (AppException ex) {
        cleanup(project);
        throw ex;
      }

      //add members of the project   
      try {
        failedMembers = addMembers(project, owner.getEmail(), projectDTO.
                getProjectTeam());
      } catch (AppException | EJBException ex) {
        cleanup(project);
        throw ex;
      }

      //Create Template for this project in elasticsearch
      try{
        addElasticsearch(project.getName());
      }catch(IOException ex){
        cleanup(project);
      }
      return project;

    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        udfso.close();
      }
    }

  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void verifyProject(Project project, DistributedFileSystemOps dfso) throws AppException {
    //proceed to all the verrifications and set up local variable    
    //  verify that the project folder does not exist
    //  verify that users and groups corresponding to this project name does not already exist in HDFS
    //  verify that Quota for this project name does not already exist in YARN
    //  verify that There is no logs folders corresponding to this project name
    //  verify that There is no certificates corresponding to this project name in the certificate generator
    try {
      if (existingProjectFolder(project) || !noExistingUser(project.getName())
          || !noExistingGroup(project.getName())
          || !verifyQuota(project.getName()) || !verifyLogs(dfso, project.getName())
          || !noExistingCertificates(project.getName())) {
        logger.log(Level.WARNING,
            "some elements of project {0} already exist in the system "
            + "Possible inconsistency!",
            project.getName());
        cleanup(project);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(),
            "some elements of project already exist in the system");
      }
    } catch (IOException | EJBException ex) {
      cleanup(project);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "error while running verifications");
    }
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Project createProject(String projectName, Users user,
          String projectDescription, DistributedFileSystemOps dfso) throws AppException, IOException {
    if (projectFacade.numProjectsLimitReached(user)) {
      logger.log(Level.SEVERE,
              "You have reached the maximum number of allowed projects.");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.NUM_PROJECTS_LIMIT_REACHED);
    } else if (projectFacade.projectExists(projectName)) {
      logger.log(Level.INFO, "Project with name {0} already exists!",
             projectName);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_EXISTS);
    }
    //Create a new project object
    Date now = new Date();
    Project project = new Project(projectName, user, now);
    project.setDescription(projectDescription);

    // make ethical status pending
    project.setEthicalStatus(ConsentStatus.PENDING.name());

    // set retention period to next 10 years by default
    Calendar cal = Calendar.getInstance();
    cal.setTime(now);
    cal.add(Calendar.YEAR, 10);
    project.setRetentionPeriod(cal.getTime());

    //set a dumy node in the project until the creation of the project folder
    Path dumy = new Path("/tmp/" + projectName);
    dfso.touchz(dumy);
    Inode dumyInode = this.inodes.getInodeAtPath(dumy.toString());
    if (dumyInode == null) {
      logger.log(Level.SEVERE, "Couldn't get the dumy Inode");
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Couldn't create project properly");
    }
    project.setInode(dumyInode);

    //Persist project object
    this.projectFacade.persistProject(project);
    this.projectFacade.flushEm();
    logProject(project, OperationType.Add);
    return project;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void setProjectInode(Project project, DistributedFileSystemOps dfso) throws AppException, IOException{
    Inode projectInode = this.inodes.getProjectRoot(project.getName());
    if (projectInode == null) {
      logger.log(Level.SEVERE, "Couldn't get Inode for the project: {0}",
              project.getName());
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Couldn't get Inode for the project: " +
              project.getName());
    }
    project.setInode(projectInode);
    this.projectFacade.mergeProject(project);
    this.projectFacade.flushEm();
    Path dumy = new Path("/tmp/" + project.getName());
    dfso.rm(dumy, true);
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void createCertificates(Project project, Users owner) throws IOException {
    LocalhostServices.
            createUserCertificates(settings.getIntermediateCaDir(), project.
                    getName(), owner.getUsername());
    userCertsFacade.putUserCerts(project.getName(), owner.getUsername());
  }
  
  private boolean existingProjectFolder(Project project){
    Inode projectInode = this.inodes.getProjectRoot(project.getName());
    if(projectInode!=null){
      return true;
    }
    return false;
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private boolean noExistingUser(String projectName) {
    List<HdfsUsers> hdfsUsers = hdfsUsersBean.getAllProjectHdfsUsers(projectName);
    if (hdfsUsers != null && !hdfsUsers.isEmpty()) {

      return false;
    }
    return true;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private boolean noExistingGroup(String projectName) {
    List<HdfsGroups> hdfsGroups = hdfsUsersBean.
            getAllProjectHdfsGroups(projectName);
    if (hdfsGroups != null && !hdfsGroups.isEmpty()) {

      return false;
    }
    return true;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private boolean verifyQuota(String projectName) {
    YarnProjectsQuota projectsQuota = yarnProjectsQuotaFacade.findByProjectName(
            projectName);
    if (projectsQuota != null) {
      return false;
    }
    return true;
  }

  private boolean verifyLogs(DistributedFileSystemOps dfso, String projectName)
          throws IOException {
    Path logPath = new Path(getYarnAgregationLogPath());

    FileStatus[] logs = dfso.listStatus(logPath);
    for (FileStatus log : logs) {
      if (log.getPath().getName().startsWith(projectName + "__")) {
        return false;
      }
    }
    return true;
  }

  private String getYarnAgregationLogPath() {
    File yarnConfFile = new File(settings.getHadoopConfDir(),
            Settings.DEFAULT_YARN_CONFFILE_NAME);
    if (!yarnConfFile.exists()) {
      logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
              yarnConfFile);
      throw new IllegalStateException("No yarn conf file: yarn-site.xml");
    }
    Configuration conf = new Configuration();
    conf.addResource(new Path(yarnConfFile.getAbsolutePath()));
    return conf.get(YarnConfiguration.NM_REMOTE_APP_LOG_DIR,
            YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR);
  }

  private boolean noExistingCertificates(String projectName) {
    return !LocalhostServices.isPresentProjectCertificates(settings.
            getIntermediateCaDir(),
            projectName);
  }
  
  /**
   * Project default datasets Logs and Resources need to be created in a
   * separate transaction after the project creation
   * is complete.
   * <p/>
   * @param username
   * @param project
   * @param dfso
   * @param udfso
   * @throws ProjectInternalFoldersFailedException
   */
  public void createProjectLogResources(Users user, Project project,
          DistributedFileSystemOps dfso, DistributedFileSystemOps udfso) throws
          AppException, IOException {

    List<ProjectServiceEnum> services = projectServicesFacade.
            findEnabledServicesForProject(project);
    String[] subResources = settings.getResourceDirs().split(";");

    for (Settings.DefaultDataset ds : Settings.DefaultDataset.values()) {
      boolean globallyVisible = (ds.equals(Settings.DefaultDataset.RESOURCES)
              || ds.equals(Settings.DefaultDataset.LOGS)
              || ds.equals(Settings.DefaultDataset.NOTEBOOKS));
      if (!services.contains(ProjectServiceEnum.ZEPPELIN) && ds.equals(
              Settings.DefaultDataset.NOTEBOOKS)) {
        continue;
      }

      //Only mark the resources dataset as searchable
      boolean searchableResources = ds.equals(
              Settings.DefaultDataset.RESOURCES);

      datasetController.createDataset(user, project, ds.getName(), ds.
              getDescription(), -1, searchableResources, globallyVisible, dfso,
              udfso);
      if (ds.equals(Settings.DefaultDataset.RESOURCES) && subResources != null) {
        for (String sub : subResources) {
          datasetController.createSubDirectory(user, project, ds.getName(),
                  sub, -1, "", false, dfso, udfso);
        }
      }

      if (searchableResources) {
        logDataSet(project, ds);
      }

      //Persist README.md to hdfs for Default Datasets
      datasetController.generateReadme(udfso, ds.getName(),
              ds.getDescription(), project.getName());

    }

  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void logDataSet(Project project, Settings.DefaultDataset ds) {
    Dataset dataset = datasetFacade.findByNameAndProjectId(project, ds.getName());
    datasetController.logDataset(dataset, OperationType.Add);
  }
  
  /**
   *
   * @param username
   * @param project
   * @param dfso
   * @param udfso
   * @throws ProjectInternalFoldersFailedException
   * @throws AppException
   */
  public void copySparkStreamingResources(String username, Project project,
          DistributedFileSystemOps dfso, DistributedFileSystemOps udfso) throws
          ProjectInternalFoldersFailedException, AppException {
    try {
      udfso.copyInHdfs(new Path(Settings.getSparkLog4JPath(settings.
              getHdfsSuperUser())), new Path("/Projects/" + project.getName()
              + "/" + Settings.DefaultDataset.RESOURCES));
      udfso.copyInHdfs(new Path(Settings.getSparkMetricsPath(settings.
              getHdfsSuperUser())), new Path("/Projects/" + project.getName()
              + "/" + Settings.DefaultDataset.RESOURCES));
    } catch (IOException e) {
      throw new ProjectInternalFoldersFailedException(
              "Could not create project resources ", e);
    }
  }

  public void createProjectConsentFolder(String username, Project project,
          DistributedFileSystemOps dfso, DistributedFileSystemOps udfso)
          throws
          ProjectInternalFoldersFailedException, AppException {

    Users user = userBean.getUserByEmail(username);

    try {
      datasetController.createDataset(user, project, "consents",
              "Biobanking consent forms", -1, false, false, dfso, udfso);
    } catch (IOException | EJBException e) {
      throw new ProjectInternalFoldersFailedException(
              "Could not create project consents folder ", e);
    }
  }

  /**
   * Returns a Project
   * <p/>
   *
   * @param id the identifier for a Project
   * @return Project
   * @throws se.kth.hopsworks.rest.AppException if the project could not be
   * found.
   */
  public Project findProjectById(Integer id) throws AppException {

    Project project = projectFacade.find(id);
    if (project != null) {
      return project;
    } else {
      throw new AppException(Response.Status.NOT_FOUND.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
  }

  public boolean addServices(Project project, List<ProjectServiceEnum> services,
          String userEmail) {
    boolean addedService = false;
    //Add the desired services
    for (ProjectServiceEnum se : services) {
      if (!projectServicesFacade.findEnabledServicesForProject(project).
              contains(se)) {
        projectServicesFacade.addServiceForProject(project, se);
        addedService = true;
      }
    }

    if (addedService) {
      Users user = userBean.getUserByEmail(userEmail);
      String servicesString = "";
      for (int i = 0; i < services.size(); i++) {
        servicesString = servicesString + services.get(i).name() + " ";
      }
      logActivity(ActivityFacade.ADDED_SERVICES + servicesString,
              ActivityFacade.FLAG_PROJECT,
              user, project);

    }
    return addedService;
  }

  /**
   * Change the project description
   * <p/>
   *
   * @param project
   * @param proj
   * @param userEmail of the user making the change
   */
  public void updateProject(Project project, ProjectDTO proj,
          String userEmail) {
    Users user = userBean.getUserByEmail(userEmail);

    project.setDescription(proj.getDescription());
    project.setRetentionPeriod(proj.getRetentionPeriod());

    projectFacade.mergeProject(project);
    logProject(project, OperationType.Update);
    logActivity(ActivityFacade.PROJECT_DESC_CHANGED, ActivityFacade.FLAG_PROJECT,
            user, project);
  }

  //Set the project owner as project master in ProjectTeam table
  private void addProjectOwner(Integer project_id, String userName) {
    ProjectTeamPK stp = new ProjectTeamPK(project_id, userName);
    ProjectTeam st = new ProjectTeam(stp);
    st.setTeamRole(ProjectRoleTypes.DATA_OWNER.getRole());
    st.setTimestamp(new Date());
    projectTeamFacade.persistProjectTeam(st);
  }

  //create project in HDFS
  private String mkProjectDIR(String projectName, DistributedFileSystemOps dfso) throws IOException{

    String rootDir = settings.DIR_ROOT;

    boolean rootDirCreated = false;
    boolean projectDirCreated = false;

    if (!dfso.isDir(File.separator + rootDir)) {
      /*
       * if the base path does not exist in the file system, create it first
       * and set it metaEnabled so that other folders down the dir tree
       * are getting registered in hdfs_metadata_log table
       */
      Path location = new Path(File.separator + rootDir);
      FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
              FsAction.ALL); // permission 777 so any one can creat a project.
      rootDirCreated = dfso.mkdir(location, fsPermission);
    } else {
      rootDirCreated = true;
    }

    /*
     * Marking a project path as meta enabled means that all child folders/files
     * that'll be created down this directory tree will have as a parent this
     * inode.
     */
    String projectPath = File.separator + rootDir + File.separator
            + projectName;
    //Create first the projectPath
    projectDirCreated = dfso.mkdir(projectPath);

    if (rootDirCreated && projectDirCreated) {
      return projectPath;
    }
    return null;
  }

  
  
  /**
   * Remove a project and optionally all associated files.
   *
   * @param projectID to be removed
   * @param email
   * @param deleteFilesOnRemove if the associated files should be deleted
   * @param udfso
   * @param dfso
   * @return true if the project and the associated files are removed
   * successfully, and false if the associated files
   * could not be removed.
   * @throws IOException if the hole operation failed. i.e the project is not
   * removed.
   * @throws AppException if the project could not be found.
   */
  public void removeProject(String userMail, int projectId) throws AppException {
    
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new AppException(Response.Status.FORBIDDEN.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    //Only project owner is able to delete a project
    Users user = userBean.getUserByEmail(userMail);
    if (!project.getOwner().equals(user)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_REMOVAL_NOT_ALLOWED);
    }
    
    
    cleanup(project);
  }
  
  public void cleanup(Project project) throws AppException{
    if(project==null){
      return;
    }
    int nbTry=0;
    while(nbTry<3){
      nbTry++;
      try {
        //remove from project_team so that nobody can see the project anymore
        List<ProjectTeam> projectTeam = updateProjectTeamRole(project,
                ProjectRoleTypes.UNDER_REMOVAL);

        //kill jobs
        List<JobDescription> running = jobFacade.getRunningJobs(project);
        if (running != null && !running.isEmpty()) {
          Runtime rt = Runtime.getRuntime();
          for (JobDescription job : running) {
            List<YarnApplicationstate> apps = yarnApplicationstateFacade.
                    findByAppname(job.getName());
            if (apps != null && !apps.isEmpty()) {
              String appid = apps.get(0).getApplicationid();
              rt.exec(settings.getHadoopDir() + "/bin/yarn application -kill "
                      + appid);
            }
          }
        }

        List<HdfsUsers> usersToClean = getUsersToClean(project);

        List<HdfsGroups> groupsToClean = getGroupsToClean(project);
        
        removeProjectInt(project, usersToClean, groupsToClean);

      } catch (Exception ex) {
        if (nbTry < 3) {
          try {
            Thread.sleep(nbTry * 1000);
          } catch (InterruptedException ex1) {
            Logger.getLogger(ProjectController.class.getName()).
                    log(Level.SEVERE, null, ex1);
          }
        }else{
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                  getStatusCode(), "error while removing project");
        }
      }
    }
    return;
  }

  private void removeProjectInt(Project project, List<HdfsUsers> usersToClean,
          List<HdfsGroups> groupsToClean) throws IOException,
          InterruptedException, AppException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();

      //log removal to notify elastic search
      logProject(project, OperationType.Delete);

      //change the owner and group of the project folder to glassfish
      String path = File.separator + settings.DIR_ROOT + File.separator
              + project.getName();
      Path location = new Path(path);
      if (dfso.exists(path)) {
        dfso.setOwner(location, "glassfish", "glassfish");
      }

      Path dumy = new Path("/tmp/" + project.getName());
      if (dfso.exists(dumy.toString())) {
        dfso.setOwner(dumy, "glassfish", "glassfish");
      }
      
      //remove kafka topics
      removeKafkaTopics(project);

      //remove user certificate from local node 
      //(they will be removed from db when the project folder is deleted)
      LocalhostServices.deleteProjectCertificates(settings.
              getIntermediateCaDir(),
              project.getName());

      String logPath = getYarnAgregationLogPath();

      for (HdfsUsers hdfsUser : usersToClean) {
        //remove jobs log associated with project
        location = new Path(logPath + "/" + hdfsUser.getName());
        dfso.rm(location, true);

        //change owner of history files
        List<Inode> inodes = inodeFacade.findHistoryFileByHdfsUser(hdfsUser);
        for (Inode inode : inodes) {
          location = new Path(inodeFacade.getPath(inode));
          dfso.setOwner(location, UserGroupInformation.getLoginUser().
                  getUserName(), "hadoop");
        }

        //Clean up tmp certificates dir from hdfs
        String tmpCertsDir = Settings.TMP_CERT_STORE_REMOTE + File.separator
                + hdfsUser.getName();
        if (dfso.exists(tmpCertsDir)) {
          dfso.rm(new Path(tmpCertsDir), true);
        }

      }

      //remove folder created by zeppelin in /user
      dfso.rm(new Path("/user/" + project.getName()), true);

      //remove quota
      removeQuotas(project);

      //change owner for files in shared datasets
      fixSharedDatasets(project, dfso);

      //Delete elasticsearch template for this project
      removeElasticsearch(project.getName());

      //delete project group and users
      removeGroupAndUsers(groupsToClean, usersToClean);
      
      //remove dumy Inode
      dfso.rm(dumy, true);

      //remove folder
      removeProjectFolder(project.getName(), dfso);

      logger.log(Level.INFO, "{0} - project removed.", project.getName());
    } finally {
      dfso.close();
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private List<ProjectTeam> updateProjectTeamRole(Project project,
          ProjectRoleTypes teamRole) {
    return projectTeamFacade.updateTeamRole(project, teamRole);
  }
  
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private List<HdfsUsers> getUsersToClean(Project project) {
    return hdfsUsersBean.getAllProjectHdfsUsers(project.getName());
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private List<HdfsGroups> getGroupsToClean(Project project) {

    return hdfsUsersBean.getAllProjectHdfsGroups(project.getName());

  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void removeKafkaTopics(Project project) throws InterruptedException, AppException {
    kafkaFacade.removeAllTopicsFromProject(project);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void removeQuotas(Project project) {
    YarnProjectsQuota yarnProjectsQuota = yarnProjectsQuotaFacade.
            findByProjectName(project.getName());
    yarnProjectsQuotaFacade.remove(yarnProjectsQuota);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void fixSharedDatasets(Project project, DistributedFileSystemOps dfso)
          throws IOException {
    List<Dataset> sharedDataSets = datasetFacade.findSharedWithProject(project);
    for (Dataset dataSet : sharedDataSets) {
      String owner = dataSet.getInode().getHdfsUser().getName();
      String group = dataSet.getInode().getHdfsGroup().getName();
      List<Inode> children = new ArrayList<>();
      inodeFacade.getAllChildren(dataSet.getInode(), children);
      for (Inode child : children) {
        if (child.getHdfsUser().getName().startsWith(project.getName() + "__")) {
          Path childPath = new Path(inodeFacade.getPath(child));
          dfso.setOwner(childPath, owner, group);
        }
      }
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void removeGroupAndUsers(List<HdfsGroups> groups,
          List<HdfsUsers> users) throws IOException {
    hdfsUsersBean.deleteGroups(groups);
    hdfsUsersBean.deleteUsers(users);
  }

  private void removeProjectFolder(String projectName,
          DistributedFileSystemOps dfso) throws IOException {
    String path = File.separator + settings.DIR_ROOT + File.separator
            + projectName;
    final Path location = new Path(path);
    dfso.rm(location, true);
  }
 
  /**
   * Adds new team members to a project(project) - bulk persist if team role not
   * specified or not in (Data owner or Data
   * scientist)defaults to Data scientist
   * <p/>
   *
   * @param project
   * @param email
   * @param projectTeams
   * @return a list of user names that could not be added to the project team
   * list.
   */
  @TransactionAttribute(
          TransactionAttributeType.NEVER)
  public List<String> addMembers(Project project, String email,
          List<ProjectTeam> projectTeams) throws AppException {
    List<String> failedList = new ArrayList<>();
    if(projectTeams==null){
      return failedList;
    }
    
    Users user = userBean.getUserByEmail(email);
    Users newMember;
    for (ProjectTeam projectTeam : projectTeams) {
      try {
        if (!projectTeam.getProjectTeamPK().getTeamMember().equals(user.
                getEmail())) {

          //if the role is not properly set set it to the default resercher.
          if (projectTeam.getTeamRole() == null || (!projectTeam.getTeamRole().
                  equals(ProjectRoleTypes.DATA_SCIENTIST.getRole())
                  && !projectTeam.
                  getTeamRole().equals(ProjectRoleTypes.DATA_OWNER.getRole()))) {
            projectTeam.setTeamRole(ProjectRoleTypes.DATA_SCIENTIST.getRole());
          }

          projectTeam.setTimestamp(new Date());
          newMember = userBean.getUserByEmail(projectTeam.getProjectTeamPK().
                  getTeamMember());
          if (newMember != null && !projectTeamFacade.isUserMemberOfProject(
                  project, newMember)) {
            //this makes sure that the member is added to the project sent as the
            //first param b/c the securty check was made on the parameter sent as path.
            projectTeam.getProjectTeamPK().setProjectId(project.getId());
            projectTeamFacade.persistProjectTeam(projectTeam);
            try {
              hdfsUsersBean.addNewProjectMember(project, projectTeam);
            } catch (IOException ex) {
              projectTeamFacade.removeProjectTeam(project, newMember);
              throw new EJBException("Could not add member to HDFS.");
            }
            try {
              LocalhostServices.createUserCertificates(settings.
                      getIntermediateCaDir(),
                      project.getName(), newMember.getUsername());
              userCertsFacade.putUserCerts(project.getName(), newMember.
                    getUsername());
            } catch (IOException ex) {
              projectTeamFacade.removeProjectTeam(project, newMember);
              
              try {
                hdfsUsersBean.removeProjectMember(projectTeam.getUser(), project);
              } catch (IOException ex1) {
                Logger.getLogger(ProjectController.class.getName()).
                        log(Level.SEVERE, null, ex1);
                throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "error while creating a user");
              }
              
              throw new EJBException("Could not creat certificates for user");
            }

            logger.log(Level.FINE, "{0} - member added to project : {1}.",
                    new Object[]{newMember.getEmail(),
                      project.getName()});
            List<SshKeys> keys = sshKeysBean.findAllById(newMember.getUid());
            List<String> publicKeys = new ArrayList<>();
            for (SshKeys k : keys) {
              publicKeys.add(k.getPublicKey());
            }

            logActivity(ActivityFacade.NEW_MEMBER + projectTeam.
                    getProjectTeamPK().getTeamMember(),
                    ActivityFacade.FLAG_PROJECT, user, project);
//            createUserAccount(project, projectTeam, publicKeys, failedList);
          } else if (newMember == null) {
            failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
                    + " was not found in the system.");
          } else {
            failedList.add(newMember.getEmail()
                    + " is already a member in this project.");
          }

        } else {
          failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
                  + " is already a member in this project.");
        }
      } catch (EJBException ejb) {
        failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
                + "could not be added. Try again later.");
        logger.log(Level.SEVERE, "Adding  team member {0} to members failed",
                projectTeam.getProjectTeamPK().getTeamMember());

      } 
    }
    return failedList;
  }

  // Create Account for user on localhost if the SSH service is enabled
//  private void createUserAccount(Project project, ProjectTeam projectTeam,
//          List<String> publicKeys, List<String> failedList) {
//    for (ProjectServices ps : project.getProjectServicesCollection()) {
//      if (ps.getProjectServicesPK().getService().compareTo(
//              ProjectServiceEnum.SSH) == 0) {
//        try {
//          String email = projectTeam.getProjectTeamPK().getTeamMember();
//          Users user = userBean.getUserByEmail(email);
//          LocalhostServices.createUserAccount(user.getUsername(), project.
//                  getName(), publicKeys);
//        } catch (IOException e) {
//          failedList.add(projectTeam.getProjectTeamPK().getTeamMember()
//                  + "could not create the account on localhost. Try again later.");
//          logger.log(Level.SEVERE,
//                  "Create account on localhost for team member {0} failed",
//                  projectTeam.getProjectTeamPK().getTeamMember());
//        }
//      }
//    }
//  }
  /**
   * Project info as data transfer object that can be sent to the user.
   * <p/>
   *
   * @param projectID of the project
   * @return project DTO that contains team members and services
   * @throws se.kth.hopsworks.rest.AppException
   */
  public ProjectDTO getProjectByID(Integer projectID) throws AppException {
    Project project = projectFacade.find(projectID);
    String name = project.getName();

    //find the project as an inode from hops database
    Inode inode = inodes.getInodeAtPath(File.separator + settings.DIR_ROOT
            + File.separator + name);

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    List<ProjectTeam> projectTeam = projectTeamFacade.findMembersByProject(
            project);
    List<ProjectServiceEnum> projectServices = projectServicesFacade.
            findEnabledServicesForProject(project);
    List<String> services = new ArrayList<>();
    for (ProjectServiceEnum s : projectServices) {
      services.add(s.toString());
    }
    return new ProjectDTO(project, inode.getId(), services, projectTeam,
            getYarnQuota(name));
//    ,getHdfsSpaceQuotaInBytes(name), getHdfsSpaceUsageInBytes(name));
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   * <p/>
   *
   * @param name
   * @return project DTO that contains team members and services
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  public ProjectDTO getProjectByName(String name) throws AppException {
    //find the project entity from hopsworks database
    Project project = projectFacade.findByName(name);

    //find the project as an inode from hops database
    String path = File.separator + settings.DIR_ROOT + File.separator + name;
    Inode inode = inodes.getInodeAtPath(path);

    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }
    List<ProjectTeam> projectTeam = projectTeamFacade.findMembersByProject(
            project);
    List<ProjectServiceEnum> projectServices = projectServicesFacade.
            findEnabledServicesForProject(project);
    List<String> services = new ArrayList<>();
    for (ProjectServiceEnum s : projectServices) {
      services.add(s.toString());
    }
    Inode parent;
    List<InodeView> kids = new ArrayList<>();

    Collection<Dataset> dsInProject = project.getDatasetCollection();
    for (Dataset ds : dsInProject) {
      parent = inodes.findParent(ds.getInode());
      kids.add(new InodeView(parent, ds, inodes.getPath(ds.getInode())));
    }

    //send the project back to client
    String quota = getYarnQuota(name);
    return new ProjectDTO(project, inode.getId(), services, projectTeam, kids,
            quota);
  }

  public String getYarnQuota(String name) {
    YarnProjectsQuota yarnQuota = yarnProjectsQuotaFacade.
            findByProjectName(name);
    if (yarnQuota != null) {
      return Float.toString(yarnQuota.getQuotaRemaining());
    }
    return "";
  }

  public void setProjectOwnerAndQuotas(Project project, long diskspaceQuotaInMB,
          DistributedFileSystemOps dfso, Users user)
          throws IOException {
    this.projectPaymentsHistoryFacade.persistProjectPaymentsHistory(
            new ProjectPaymentsHistory(new ProjectPaymentsHistoryPK(project
                    .getName(), project.getCreated()), project.
                    getOwner().getEmail(),
                    ProjectPaymentAction.DEPOSIT_MONEY, 0));
    this.projectPaymentsHistoryFacade.flushEm();
    this.yarnProjectsQuotaFacade.persistYarnProjectsQuota(
            new YarnProjectsQuota(project.getName(), Integer.parseInt(
                    settings
                    .getYarnDefaultQuota()), 0));
    this.yarnProjectsQuotaFacade.flushEm();
    setHdfsSpaceQuotaInMBs(project.getName(), diskspaceQuotaInMB, dfso);
    //Add the activity information
    logActivity(ActivityFacade.NEW_PROJECT + project.getName(),
            ActivityFacade.FLAG_PROJECT, user, project);
    //update role information in project
    addProjectOwner(project.getId(), user.getEmail());
    logger.log(Level.FINE, "{0} - project created successfully.", project.
            getName());
  }
  
  public void setHdfsSpaceQuotaInMBs(String projectname, long diskspaceQuotaInMB,
          DistributedFileSystemOps dfso)
          throws IOException {
    dfso.setHdfsSpaceQuotaInMBs(new Path(Settings.getProjectPath(projectname)),
            diskspaceQuotaInMB);
  }

//  public Long getHdfsSpaceQuotaInBytes(String name) throws AppException {
//    String path = settings.getProjectPath(name);
//    try {
//      long quota = dfs.getDfsOps().getHdfsSpaceQuotaInMbs(new Path(path));
//      logger.log(Level.INFO, "HDFS Quota for {0} is {1}", new Object[]{path, quota});
//      return quota;
//    } catch (IOException ex) {
//      logger.severe(ex.getMessage());
//      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
//          ". Cannot find quota for the project: " + path);
//    }
  public HdfsInodeAttributes
          getHdfsQuotas(int inodeId) throws AppException {

    HdfsInodeAttributes res = em.find(HdfsInodeAttributes.class, inodeId);
    if (res == null) {
      return new HdfsInodeAttributes(inodeId);
    }

    return res;
  }

//  public Long getHdfsSpaceUsageInBytes(String name) throws AppException {
//    String path = settings.getProjectPath(name);
//
//    try {
//      long usedQuota = dfs.getDfsOps().getUsedQuotaInMbs(new Path(path));
//      logger.log(Level.INFO, "HDFS Quota for {0} is {1}", new Object[]{path, usedQuota});
//      return usedQuota;
//    } catch (IOException ex) {
//      logger.severe(ex.getMessage());
//      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
//          ". Cannot find quota for the project: " + path);
//    }
//  }  
  /**
   * Deletes a member from a project
   *
   * @param project
   * @param email
   * @param toRemoveEmail
   * @throws AppException
   */
  public void deleteMemberFromTeam(Project project, String email,
          String toRemoveEmail) throws AppException, IOException {
    Users userToBeRemoved = userBean.getUserByEmail(toRemoveEmail);
    if (userToBeRemoved == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.USER_DOES_NOT_EXIST);
      //user not found
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project,
            userToBeRemoved);
    if (projectTeam == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.TEAM_MEMBER_NOT_FOUND);
    }
    projectTeamFacade.removeProjectTeam(project, userToBeRemoved);
    Users user = userBean.getUserByEmail(email);
    //remove the user name from HDFS
    hdfsUsersBean.removeProjectMember(projectTeam.getUser(), project);
    logActivity(ActivityFacade.REMOVED_MEMBER + toRemoveEmail,
            ActivityFacade.FLAG_PROJECT, user, project);

//    try {
//      LocalhostServices.deleteUserAccount(email, project.getName());
//    } catch (IOException e) {
//      String errMsg = "Could not delete user account: " + LocalhostServices.
//              getUsernameInProject(email, project.getName()) + " ";
//      logger.warning(errMsg + e.getMessage());
//      //  TODO: Should this be rethrown to give a HTTP Response to client??
//    }
  }

  /**
   * Updates the role of a member
   * <p/>
   *
   * @param project
   * @param owner that is performing the update
   * @param toUpdateEmail
   * @param newRole
   * @throws AppException
   */
  @TransactionAttribute(
          TransactionAttributeType.REQUIRES_NEW)
  public void updateMemberRole(Project project, String owner,
          String toUpdateEmail, String newRole) throws AppException {
    Users projOwner = project.getOwner();
    Users opsOwner = userBean.getUserByEmail(owner);
    Users user = userBean.getUserByEmail(toUpdateEmail);
    if (projOwner.equals(user)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Can not change the role of a project owner.");
    }
    if (user == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.USER_DOES_NOT_EXIST);
      //user not found
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project, user);
    if (projectTeam == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.TEAM_MEMBER_NOT_FOUND);
      //member not found
    }
    if (!projectTeam.getTeamRole().equals(newRole)) {
      projectTeam.setTeamRole(newRole);
      projectTeam.setTimestamp(new Date());
      projectTeamFacade.update(projectTeam);

      if (newRole.equals(AllowedRoles.DATA_OWNER)) {
        hdfsUsersBean.addUserToProjectGroup(project, projectTeam);
      } else {
        hdfsUsersBean.modifyProjectMembership(user, project);
      }

      logActivity(ActivityFacade.CHANGE_ROLE + toUpdateEmail,
              ActivityFacade.FLAG_PROJECT, opsOwner, project);
    }

  }

  /**
   * Retrieves all the project teams that a user have a role
   * <p/>
   *
   * @param email of the user
   * @return a list of project team
   */
  public List<ProjectTeam> findProjectByUser(String email) {
    Users user = userBean.getUserByEmail(email);
    return projectTeamFacade.findActiveByMember(user);
  }

  /**
   * Retrieves all the project teams that a user have a role.
   * <p/>
   *
   * @param email of the user
   * @param ignoreCase
   * @return a list of project names
   */
  public List<String> findProjectNamesByUser(String email, boolean ignoreCase) {
    Users user = userBean.getUserByEmail(email);
    List<ProjectTeam> projectTeams = projectTeamFacade.findActiveByMember(user);
    List<String> projects = null;
    if (projectTeams != null && projectTeams.size() > 0) {
      projects = new ArrayList<>();
      for (ProjectTeam team : projectTeams) {
        if (ignoreCase) {
          projects.add(team.getProject().getName().toLowerCase());
        } else {
          projects.add(team.getProject().getName());
        }
      }
    }
    return projects;
  }

  /**
   * Retrieves all the project teams for a project
   * <p/>
   *
   * @param projectID
   * @return a list of project team
   */
  public List<ProjectTeam> findProjectTeamById(Integer projectID) {
    Project project = projectFacade.find(projectID);
    return projectTeamFacade.findMembersByProject(project);
  }

  /**
   * Logs activity
   * <p/>
   *
   * @param activityPerformed the description of the operation performed
   * @param flag on what the operation was performed(FLAG_PROJECT, FLAG_USER)
   * @param performedBy the user that performed the operation
   * @param performedOn the project the operation was performed on.
   */
  public void logActivity(String activityPerformed, String flag,
          Users performedBy, Project performedOn) {
    Date now = new Date();
    Activity activity = new Activity();
    activity.setActivity(activityPerformed);
    activity.setFlag(flag);
    activity.setProject(performedOn);
    activity.setTimestamp(now);
    activity.setUser(performedBy);

    activityFacade.persistActivity(activity);
  }

  /**
   * Extracts the project name out of the given path. The project name is the
   * second part of this path.
   * <p/>
   * @param path
   * @return
   */
  private String extractProjectName(String path) {

    int startIndex = path.indexOf('/', 1);
    int endIndex = path.indexOf('/', startIndex + 1);

    return path.substring(startIndex + 1, endIndex);
  }

  public void addExampleJarToExampleProject(String username, Project project,
          DistributedFileSystemOps dfso, DistributedFileSystemOps udfso,
          TourProjectType projectType) throws
          AppException {

    Users user = userBean.getUserByEmail(username);
    try {
      datasetController.createDataset(user, project, "TestJob",
              "jar files for guide projects", -1, false, true, dfso, udfso);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "something went wrong when adding the example jar to the project");
    }
    
    if (TourProjectType.SPARK.equals(projectType)) {
      String exampleDir = settings.getSparkDir() + Settings.SPARK_EXAMPLES_DIR
          + "/";
      try {
        File dir = new File(exampleDir);
        File[] file = dir.listFiles((File dir1, String name)
            -> name.matches("spark-examples(.*).jar"));
        if (file.length == 0) {
          throw new IllegalStateException("No spark-examples*.jar was found in "
              + dir.getAbsolutePath());
        }
        if (file.length > 1) {
          logger.log(Level.WARNING,
              "More than one spark-examples*.jar found in {0}.", dir.
                  getAbsolutePath());
        }
        udfso.copyToHDFSFromLocal(false, file[0].getAbsolutePath(),
            File.separator + Settings.DIR_ROOT + File.separator + project.
                getName() + "/TestJob/spark-examples.jar");
    
      } catch (IOException ex) {
        logger.log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "something went wrong when adding the example jar to the project");
      }
    } else if (TourProjectType.KAFKA.equals(projectType)) {
      // Get the JAR from /user/glassfish
      String kafkaExampleSrc = "/user/glassfish/" + Settings
          .HOPS_KAFKA_TOUR_JAR;
      String kafkaExampleDst = "/" + Settings.DIR_ROOT + "/" + project.getName()
          + "/TestJob/" + Settings.HOPS_KAFKA_TOUR_JAR;
      try {
        udfso.copyInHdfs(new Path(kafkaExampleSrc), new Path(kafkaExampleDst));
      } catch (IOException ex) {
        logger.log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
          "something went wrong when adding the example jar to the project");
      }
    }
  }

  public YarnPriceMultiplicator getYarnMultiplicator() {
    YarnPriceMultiplicator multiplicator = yarnProjectsQuotaFacade.
            getMultiplicator();
    if (multiplicator == null) {
      multiplicator = new YarnPriceMultiplicator();
      multiplicator.setMultiplicator(Settings.DEFAULT_YARN_MULTIPLICATOR);
      multiplicator.setId("-1");
    }
    return multiplicator;
  }

  public void logProject(Project project, OperationType type) {
    operationsLogFacade.persist(new OperationsLog(project, type));
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void createAnacondaEnv(Project project) throws AppException {
    pythonDepsFacade.getPreInstalledLibs(project);

  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeAnacondaEnv(Project project) throws AppException {
    pythonDepsFacade.removeProject(project);
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void cloneAnacondaEnv(Project srcProj, Project destProj) throws
          AppException {
    pythonDepsFacade.cloneProject(srcProj, destProj.getName());
  }

  /**
   * Handles Kibana related indices and templates for projects.
   *
   * @param project
   * @param create creation or deletion stage of project
   * @return
   * @throws java.io.IOException
   */
  public boolean addElasticsearch(String project) throws IOException {
    project = project.toLowerCase();
    Map<String, String> params = new HashMap<>();

    params.put("op", "PUT");
    params.put("project", project);
    params.put("resource", "_template");
    params.put("data", "{\"template\":\"" + project
            + "\",\"mappings\":{\"logs\":{\"properties\":{\"application\":"
            + "{\"type\":\"string\",\"index\":\"not_analyzed\"},\"host"
            + "\":{\"type\":\"string\",\"index\":\"not_analyzed\"},\""
            + "jobname\":{\"type\":\"string\",\"index\":\"not_analyzed\"},"
            + "\"timestamp\":{\"type\":\"date\",\"index\":\"not_analyzed\"},"
            + "\"project\":{\"type\":\"string\",\"index\":\"not_analyzed\"}}}}}");
    JSONObject resp = sendElasticsearchReq(params);
    boolean templateCreated = false;
    if (resp.has("acknowledged")) {
      templateCreated = (Boolean) resp.get("acknowledged");
    }

    //Create Kibana index
    params.clear();
    params.put("op", "PUT");
    params.put("project", project);
    params.put("resource", ".kibana/index-pattern");
    params.put("data", "{\"title\" : \"" + project
            + "\", \"fields\" : \"[{\\\"name\\\":\\\"_index\\\",\\\"type\\\":"
            + "\\\"string\\\",\\\"count\\\":0,\\\"scripted\\\":false,"
            + "\\\"indexed\\\":false,\\\"analyzed\\\":false,\\\""
            + "doc_values\\\":false},{\\\"name\\\":\\\"project\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":false,"
            + "\\\"doc_values\\\":true},{\\\"name\\\":\\\"path\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"file\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"@version\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"host\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":false,"
            + "\\\"doc_values\\\":true},{\\\"name\\\":\\\"logger_name\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"class\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"jobname\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":false,"
            + "\\\"doc_values\\\":true},{\\\"name\\\":\\\"timestamp\\\","
            + "\\\"type\\\":\\\"date\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":false,"
            + "\\\"doc_values\\\":true},{\\\"name\\\":\\\"method\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"thread\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"message\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"priority\\\","
            + "\\\"type\\\":\\\"string\\\",\\\"count\\\":0,\\\"scripted"
            + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":true,"
            + "\\\"doc_values\\\":false},{\\\"name\\\":\\\"@timestamp"
            + "\\\",\\\"type\\\":\\\"date\\\",\\\"count\\\":0,"
            + "\\\"scripted\\\":false,\\\"indexed\\\":true,\\\"analyzed"
            + "\\\":false,\\\"doc_values\\\":true},{\\\"name\\\":"
            + "\\\"application\\\",\\\"type\\\":\\\"string\\\",\\\"count"
            + "\\\":0,\\\"scripted\\\":false,\\\"indexed\\\":true,"
            + "\\\"analyzed\\\":false,\\\"doc_values\\\":true},{"
            + "\\\"name\\\":\\\"_source\\\",\\\"type\\\":\\\"_source"
            + "\\\",\\\"count\\\":0,\\\"scripted\\\":false,\\\"indexed"
            + "\\\":false,\\\"analyzed\\\":false,\\\"doc_values\\\":false},"
            + "{\\\"name\\\":\\\"_id\\\",\\\"type\\\":\\\"string\\\","
            + "\\\"count\\\":0,\\\"scripted\\\":false,\\\"indexed\\\":false,"
            + "\\\"analyzed\\\":false,\\\"doc_values\\\":false},{\\\"name\\\":"
            + "\\\"_type\\\",\\\"type\\\":\\\"string\\\",\\\"count"
            + "\\\":0,\\\"scripted\\\":false,\\\"indexed\\\":false,"
            + "\\\"analyzed\\\":false,\\\"doc_values\\\":false},{"
            + "\\\"name\\\":\\\"_score\\\",\\\"type\\\":\\\"number\\\","
            + "\\\"count\\\":0,\\\"scripted\\\":false,\\\"indexed"
            + "\\\":false,\\\"analyzed\\\":false,\\\"doc_values"
            + "\\\":false}]\"}");
    resp = sendElasticsearchReq(params);
    boolean kibanaIndexCreated = false;
    if (resp.has("acknowledged")) {
      kibanaIndexCreated = (Boolean) resp.get("acknowledged");
    }

    if (kibanaIndexCreated && templateCreated) {
      return true;
    }

    return false;
  }

  public boolean removeElasticsearch(String project) throws IOException {
    project = project.toLowerCase();
    Map<String, String> params = new HashMap<>();
    //1. Delete Kibana index
    params.put("project", project);
    params.put("op", "DELETE");
    params.put("resource", ".kibana/index-pattern");
    JSONObject resp = sendElasticsearchReq(params);
    boolean kibanaIndexDeleted = false;
    if (resp != null && resp.has("acknowledged")) {
      kibanaIndexDeleted = (Boolean) resp.get("acknowledged");
    }
    params.clear();
    //2. Delete Elasticsearch Index
    params.put("project", project);
    params.put("op", "DELETE");
    params.put("resource", "");
    resp = sendElasticsearchReq(params);
    boolean elasticIndexDeleted = false;
    if (resp != null && resp.has("acknowledged")) {
      elasticIndexDeleted = (Boolean) resp.get("acknowledged");
    }
    //3. Delete Elasticsearch Template
    params.put("resource", "_template");
    boolean templateDeleted = false;
    resp = sendElasticsearchReq(params);
    if (resp != null && resp.has("acknowledged")) {
      templateDeleted = (Boolean) resp.get("acknowledged");
    }

    if (elasticIndexDeleted && templateDeleted && kibanaIndexDeleted) {
      return true;
    }
    return false;
  }
  
  /**
   *
   * @param params
   * @return
   * @throws MalformedURLException
   * @throws IOException
   */
  private JSONObject sendElasticsearchReq(Map<String, String> params) throws
          MalformedURLException, IOException {
    String templateUrl;
    if (!params.containsKey("url")) {
      templateUrl = "http://" + settings.getElasticIp() + ":" + "9200/"
              + params.get("resource") + "/" + params.get("project");
    } else {
      templateUrl = params.get("url");
    }
    URL obj = new URL(templateUrl);
    HttpURLConnection conn = (HttpURLConnection) obj.openConnection();

    conn.setDoOutput(true);
    conn.setRequestMethod(params.get("op"));
    if (params.get("op").equalsIgnoreCase("PUT")) {
      String data = params.get("data");
      try (OutputStreamWriter out
              = new OutputStreamWriter(conn.getOutputStream())) {
        out.write(data);
      }
    }
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(
              (conn.getInputStream())));

      String output;
      StringBuilder outputBuilder = new StringBuilder();
      while ((output = br.readLine()) != null) {
        outputBuilder.append(output);
      }

      conn.disconnect();
      return new JSONObject(outputBuilder.toString());

    } catch (FileNotFoundException ex) {
      logger.log(Level.WARNING, "Elasticsearch resource " + params.get(
              "resource") + " was not found");
    } catch (IOException ex) {
      if (ex.getMessage().contains("kibana")) {
        logger.log(Level.WARNING, "error", ex);
        logger.log(Level.WARNING, "Kibana index could not be deleted for "
                + params.get("project"));
      } else {
        throw new IOException(ex);
      }
    }
    return null;
  }

}
