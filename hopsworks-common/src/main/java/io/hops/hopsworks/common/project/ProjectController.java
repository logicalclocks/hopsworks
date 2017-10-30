package io.hops.hopsworks.common.project;

import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.certificates.ServiceCerts;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.HdfsInodeAttributes;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroups;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.jobhistory.Execution;
import io.hops.hopsworks.common.dao.jobhistory.ExecutionFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterProcessFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.log.operation.OperationsLog;
import io.hops.hopsworks.common.dao.log.operation.OperationsLogFacade;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.cert.CertPwDTO;
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
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FolderNameValidator;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.exception.ProjectInternalFoldersFailedException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.user.CertificateMaterializer;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.LocalhostServices;
import io.hops.hopsworks.common.util.Settings;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ValidationException;
import javax.ws.rs.client.ClientBuilder;
import javax.xml.rpc.ServiceException;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.LogAggregationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.json.JSONObject;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectController {

  private final static Logger LOGGER = Logger.getLogger(ProjectController.class.
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
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private Settings settings;
  @EJB
  private CertsFacade userCertsFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private YarnClientService ycs;
  @EJB
  private InodeFacade inodeFacade;
  @EJB
  private OperationsLogFacade operationsLogFacade;
  @EJB
  private PythonDepsFacade pythonDepsFacade;
  @EJB
  private JupyterProcessFacade jupyterProcessFacade;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private ElasticController elasticController;
  @EJB
  private ExecutionFacade execFacade;
  @EJB
  private CertificateMaterializer certificateMaterializer;

  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private CertificatesController certificatesController;

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
   *
   * @param projectDTO
   * @param owner
   * @param failedMembers
   * @param sessionId
   * @return
   * @throws IllegalArgumentException if the project name already exists.
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  public Project createProject(ProjectDTO projectDTO, Users owner,
      List<String> failedMembers, String sessionId) throws AppException {

    Long startTime = System.currentTimeMillis();

    //check that the project name is ok
    String projectName = projectDTO.getProjectName();
    try {
      FolderNameValidator.isValidName(projectName, false);
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
          LOGGER.log(Level.SEVERE,
              ResponseMessages.PROJECT_SERVICE_NOT_FOUND, iex);
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), s
              + ResponseMessages.PROJECT_SERVICE_NOT_FOUND);
        }
      }
    }
    LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 1: " + (System.currentTimeMillis() - startTime));

    DistributedFileSystemOps dfso = null;
    Project project = null;
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
      try {
        try {
          project = createProject(projectName, owner, projectDTO.
              getDescription(), dfso);
        } catch (EJBException ex) {
          LOGGER.log(Level.WARNING, null, ex);
          Path dummy = new Path("/tmp/" + projectName);
          dfso.rm(dummy, true);
          throw new AppException(Response.Status.CONFLICT.
              getStatusCode(), "A project with this name already exist");
        }
      } catch (AppException ex) {
        throw ex;
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "An error occured when creating the project");
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 2 (hdfs): " + (System.currentTimeMillis() - startTime));
  
      verifyProject(project, dfso, sessionId);
  
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 3 (verify): " + (System.currentTimeMillis() - startTime));
      
      //create certificate for this user
      // User's certificates should be created before making any call to
      // Hadoop clients. Otherwise the client will fail if RPC TLS is enabled
      // This is an async call
      Future<CertificatesController.CertsResult> certsGenerationFuture = null;
      try {
        certsGenerationFuture = certificatesController.generateCertificates
            (project, owner, true);
      } catch (Exception ex) {
        LOGGER.log(Level.SEVERE, "Error while creating certificates: "
            + ex.getMessage(), ex);
        cleanup(project, sessionId, certsGenerationFuture);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR
            .getStatusCode(), "Error while generating certificates");
      }

      String username = hdfsUsersBean.getHdfsUserName(project, owner);
      if (username == null || username.isEmpty()) {
        cleanup(project, sessionId, certsGenerationFuture);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "wrong user name");
      }
      
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 4 (certs): " + (System.currentTimeMillis() - startTime));
      
      //all the verifications have passed, we can now create the project  
      //create the project folder
      String projectPath = null;
      try {
        projectPath = mkProjectDIR(projectName, dfso);
      } catch (IOException | EJBException ex) {
        cleanup(project, sessionId, certsGenerationFuture);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "problem creating project folder");
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 5 (folders): " + (System.currentTimeMillis() - startTime));
      if (projectPath == null) {
        cleanup(project, sessionId, certsGenerationFuture);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "problem creating project folder");
      }
      //update the project with the project folder inode
      try {
        setProjectInode(project, dfso);
      } catch (AppException | EJBException ex) {
        cleanup(project, sessionId, certsGenerationFuture);
        throw ex;
      } catch (IOException ex) {
        cleanup(project, sessionId, certsGenerationFuture);
        LOGGER.log(Level.SEVERE, "An error occured when creating the project: "
            + ex.getMessage(), ex);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "An error occured when creating the project");
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 6 (inodes): " + (System.currentTimeMillis() - startTime));

      //set payment and quotas
      try {

        setProjectOwnerAndQuotas(project, settings.getHdfsDefaultQuotaInMBs(),
            dfso, owner);

      } catch (IOException | EJBException ex) {
        cleanup(project, sessionId, certsGenerationFuture);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "could not set folder quota");
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 7 (quotas): " + (System.currentTimeMillis() - startTime));

      try {
        hdfsUsersBean.addProjectFolderOwner(project, dfso);
        createProjectLogResources(owner, project, dfso);
      } catch (IOException | EJBException ex) {
        LOGGER.log(Level.SEVERE, "Error while creating project sub folders: "
            + ex.getMessage(), ex);
        cleanup(project, sessionId, certsGenerationFuture);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "Error while creating project sub folders");
      } catch (AppException ex) {
        cleanup(project, sessionId, certsGenerationFuture);
        throw ex;
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 8 (logs): " + (System.currentTimeMillis() - startTime));

      // enable services
      for (ProjectServiceEnum service : projectServices) {
        try {
          addService(project, service, owner, dfso);
        } catch (ServiceException sex) {
          cleanup(project, sessionId, certsGenerationFuture);
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), "Error while enabling the services");
        }
      }

      //add members of the project   
      try {
        failedMembers = addMembers(project, owner.getEmail(), projectDTO.
            getProjectTeam());
      } catch (AppException | EJBException ex) {
        cleanup(project, sessionId, certsGenerationFuture);
        throw ex;
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 9 (members): " + (System.currentTimeMillis() - startTime));

      //Create Template for this project in elasticsearch
      try {
        addElasticsearch(project.getName());
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "Error while adding elasticsearch service for project:" + projectName, ex);
        cleanup(project, sessionId, certsGenerationFuture);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 10 (elastic): " + (System.currentTimeMillis() - startTime));
      
      try {
        if (certsGenerationFuture != null) {
          certsGenerationFuture.get();
        }
      } catch (InterruptedException | ExecutionException ex) {
        LOGGER.log(Level.SEVERE, "Error while waiting for the certificate " +
            "generation thread to finish. Will try to cleanup...");
        cleanup(project, sessionId, certsGenerationFuture);
      }
      return project;

    } finally {
      if (dfso != null) {
        dfso.close();
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 11 (close): " + (System.currentTimeMillis() - startTime));
    }

  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void verifyProject(Project project, DistributedFileSystemOps dfso,
      String sessionId)
      throws AppException {
    //proceed to all the verrifications and set up local variable    
    //  verify that the project folder does not exist
    //  verify that users and groups corresponding to this project name does not already exist in HDFS
    //  verify that Quota for this project name does not already exist in YARN
    //  verify that There is no logs folders corresponding to this project name
    //  verify that There is no certificates corresponding to this project name in the certificate generator
    try {
      if (existingProjectFolder(project)) {
        LOGGER.log(Level.WARNING,
            "a folder with name corresponding to project {0} already exists in the system "
            + "Possible inconsistency!", project.getName());
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "a Project folder with name corresponding to this project already exists in the system "
            + "Possible inconsistency! Please contact the admin");
      } else if (!noExistingUser(project.getName())) {
        LOGGER.log(Level.WARNING,
            "a user with name corresponding to this project already exists in the system "
            + "Possible inconsistency!", project.getName());
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "a user with name corresponding to this project already exists in the system "
            + "Possible inconsistency! Please contact the admin");
      } else if (!noExistingGroup(project.getName())) {
        LOGGER.log(Level.WARNING,
            "a group with name corresponding to project {0} already exists in the system "
            + "Possible inconsistency! Please contact the admin", project.getName());
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "a group with name corresponding to this project already exists in the system "
            + "Possible inconsistency! Please contact the admin");
      } else if (!noExistingCertificates(project.getName())) {
        LOGGER.log(Level.WARNING,
            "Certificates corresponding to project {0} already exist in the system "
            + "Possible inconsistency!", project.getName());
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "Certificates corresponding to this project already exist in the system "
            + "Possible inconsistency! Please contact the admin");
      } else if (!verifyQuota(project.getName())) {
        LOGGER.log(Level.WARNING,
            "Quotas corresponding to this project already exist in the system "
            + "Possible inconsistency! Retry.", project.getName());
        cleanup(project, sessionId);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "Quotas corresponding to this project already exist in the system "
            + "Possible inconsistency!");
      } else if (!verifyLogs(dfso, project.getName())) {
        LOGGER.log(Level.WARNING,
            "Logs corresponding to this project already exist in the system "
            + "Possible inconsistency!", project.getName());
        cleanup(project, sessionId);
        throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
            getStatusCode(), "Logs corresponding to this project already exist in the system "
            + "Possible inconsistency! Retry");
      }
    } catch (IOException | EJBException ex) {
      cleanup(project, sessionId);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "error while running verifications");
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Project createProject(String projectName, Users user,
      String projectDescription, DistributedFileSystemOps dfso) throws
      AppException, IOException {
    if (projectFacade.numProjectsLimitReached(user)) {
      LOGGER.log(Level.SEVERE,
          "You have reached the maximum number of allowed projects.");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.NUM_PROJECTS_LIMIT_REACHED);
    } else if (projectFacade.projectExists(projectName)) {
      LOGGER.log(Level.INFO, "Project with name {0} already exists!",
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
      LOGGER.log(Level.SEVERE, "Couldn't get the dumy Inode");
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
  private void setProjectInode(Project project, DistributedFileSystemOps dfso)
      throws AppException, IOException {
    Inode projectInode = this.inodes.getProjectRoot(project.getName());
    if (projectInode == null) {
      LOGGER.log(Level.SEVERE, "Couldn't get Inode for the project: {0}",
          project.getName());
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(), "Couldn't get Inode for the project: " + project.
              getName());
    }
    project.setInode(projectInode);
    this.projectFacade.mergeProject(project);
    this.projectFacade.flushEm();
    Path dumy = new Path("/tmp/" + project.getName());
    dfso.rm(dumy, true);
  }

  private boolean existingProjectFolder(Project project) {
    Inode projectInode = this.inodes.getProjectRoot(project.getName());
    if (projectInode != null) {
      LOGGER.log(Level.WARNING, "project folder existing for project {0}",
          project.getName());
      return true;
    }
    return false;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private boolean noExistingUser(String projectName) {
    List<HdfsUsers> hdfsUsers = hdfsUsersBean.
        getAllProjectHdfsUsers(projectName);
    if (hdfsUsers != null && !hdfsUsers.isEmpty()) {
      LOGGER.log(Level.WARNING, "hdfs user existing for project {0}",
          projectName);
      return false;
    }
    return true;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private boolean noExistingGroup(String projectName) {
    List<HdfsGroups> hdfsGroups = hdfsUsersBean.
        getAllProjectHdfsGroups(projectName);
    if (hdfsGroups != null && !hdfsGroups.isEmpty()) {
      LOGGER.log(Level.WARNING, "hdfs group existing for project {0}",
          projectName);
      return false;
    }
    return true;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private boolean verifyQuota(String projectName) {
    YarnProjectsQuota projectsQuota = yarnProjectsQuotaFacade.findByProjectName(
        projectName);
    if (projectsQuota != null) {
      LOGGER.log(Level.WARNING, "quota existing for project {0}", projectName);
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
        LOGGER.log(Level.WARNING, "logs existing for project {0}", projectName);
        return false;
      }
    }
    return true;
  }

  private String getYarnAgregationLogPath() {
    File yarnConfFile = new File(settings.getHadoopConfDir(),
        Settings.DEFAULT_YARN_CONFFILE_NAME);
    if (!yarnConfFile.exists()) {
      LOGGER.log(Level.SEVERE, "Unable to locate configuration file in {0}",
          yarnConfFile);
      throw new IllegalStateException("No yarn conf file: yarn-site.xml");
    }
    Configuration conf = new Configuration();
    conf.addResource(new Path(yarnConfFile.getAbsolutePath()));
    return conf.get(YarnConfiguration.NM_REMOTE_APP_LOG_DIR,
        YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR);
  }

  private boolean noExistingCertificates(String projectName) {
    boolean result = !LocalhostServices.isPresentProjectCertificates(settings.
        getIntermediateCaDir(),
        projectName);

    if (!result) {
      LOGGER.log(Level.WARNING, "certificates existing for project {0}",
          projectName);
    }
    return result;
  }

  /**
   * Project default datasets Logs and Resources need to be created in a
   * separate transaction after the project creation
   * is complete.
   *
   * @param user
   * @param project
   * @param dfso
   * @throws io.hops.hopsworks.common.exception.AppException
   * @throws java.io.IOException
   */
  public void createProjectLogResources(Users user, Project project,
      DistributedFileSystemOps dfso) throws AppException, IOException {

    for (Settings.BaseDataset ds : Settings.BaseDataset.values()) {
      datasetController.createDataset(user, project, ds.getName(), ds.
          getDescription(), -1, false, true, dfso);
      
      StringBuilder dsStrBuilder = new StringBuilder();
      dsStrBuilder.append(File.separator).append(Settings.DIR_ROOT)
          .append(File.separator).append(project.getName())
          .append(File.separator).append(ds.getName());
      
      Path dsPath = new Path(dsStrBuilder.toString());
      FileStatus fstatus = dfso.getFileStatus(dsPath);
      
      // create subdirectories for the resource dataset
      if (ds.equals(Settings.BaseDataset.RESOURCES)) {
        String[] subResources = settings.getResourceDirs().split(";");
        for (String sub : subResources) {
          Path resourceDir = new Path(Settings.getProjectPath(project.getName()),
              ds.getName());
          Path subDirPath = new Path(resourceDir, sub);
          datasetController.createSubDirectory(project, subDirPath, -1,
              "", false, dfso);
          dfso.setOwner(subDirPath, fstatus.getOwner(), fstatus.getGroup());
        }
      }

      //Persist README.md to hdfs for Default Datasets
      datasetController.generateReadme(dfso, ds.getName(),
          ds.getDescription(), project.getName());
      Path readmePath = new Path(dsPath, Settings.README_FILE);
      dfso.setOwner(readmePath, fstatus.getOwner(), fstatus.getGroup());
    }
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
      udfso.copyInHdfs(new Path(settings.getSparkLog4JPath()), new Path(
          "/Projects/" + project.getName()
          + "/" + Settings.BaseDataset.RESOURCES));
      udfso.copyInHdfs(new Path(settings.getSparkMetricsPath()), new Path(
          "/Projects/" + project.getName()
          + "/" + Settings.BaseDataset.RESOURCES));
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
          "Biobanking consent forms", -1, false, false, dfso);
    } catch (IOException | EJBException e) {
      throw new ProjectInternalFoldersFailedException(
          "Could not create project consents folder ", e);
    }
  }

  /**
   * Returns a Project
   *
   *
   * @param id the identifier for a Project
   * @return Project
   * @throws io.hops.hopsworks.common.exception.AppException
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

  // Used only during project creation
  private boolean addService(Project project, ProjectServiceEnum service,
      Users user, DistributedFileSystemOps dfso) throws ServiceException {
    return addService(project, service, user, dfso, dfso);
  }
  
  public boolean addService(Project project, ProjectServiceEnum service,
      Users user, DistributedFileSystemOps dfso,
      DistributedFileSystemOps udfso) throws ServiceException {
    if (projectServicesFacade.isServiceEnabledForProject(project, service)) {
      // Service already enabled fro the current project. Nothing to do
      return false;
    }

    boolean toPersist;
    switch (service) {
      case ZEPPELIN:
        toPersist = addServiceDataset(project, user,
            Settings.ServiceDataset.ZEPPELIN, dfso, udfso);
        break;
      case JUPYTER:
        toPersist = addServiceDataset(project, user,
            Settings.ServiceDataset.JUPYTER, dfso, udfso);
        break;
      default:
        toPersist = true;
    }

    if (toPersist) {
      // Persist enabled service in the database
      projectServicesFacade.addServiceForProject(project, service);
      logActivity(ActivityFacade.ADDED_SERVICE + service.toString(),
          ActivityFacade.FLAG_PROJECT, user, project);
    } else {
      // either addServiceZeppelin or addServiceHive failed. Throw ServiceException to
      // signal it to the view
      throw new ServiceException();
    }

    return true;
  }

  private boolean addServiceDataset(Project project, Users user,
      Settings.ServiceDataset ds, DistributedFileSystemOps dfso,
      DistributedFileSystemOps udfso) {
    try {
      datasetController.createDataset(user, project, ds.getName(), ds.
          getDescription(), -1, false, true, dfso);
      datasetController.generateReadme(udfso, ds.getName(),
          ds.getDescription(), project.getName());
      
      // This should only happen in project creation
      // Create dataset and corresponding README file as superuser
      // to postpone waiting for the certificates generation thread when
      // RPC TLS is enabled
      if (dfso == udfso && udfso.getEffectiveUser()
          .equals(settings.getHdfsSuperUser())) {
        StringBuilder dsStrBuilder = new StringBuilder();
        dsStrBuilder.append(File.separator).append(Settings.DIR_ROOT)
            .append(File.separator).append(project.getName())
            .append(File.separator).append(ds.getName());
        Path dsPath = new Path(dsStrBuilder.toString());
        FileStatus fstatus = dfso.getFileStatus(dsPath);
        Path readmePath = new Path(dsPath, Settings.README_FILE);
        dfso.setOwner(readmePath, fstatus.getOwner(), fstatus.getGroup());
      }
    } catch (IOException | AppException ex) {
      LOGGER.log(Level.SEVERE, "Could not create dir: " + ds.getName(), ex);
      return false;
    }

    return true;
  }

  /**
   * Change, if necessary, the project description
   * <p/>
   *
   * @param project the project to change
   * @param projectDescr the description
   * @param user the user making the change
   * @return
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  public boolean updateProjectDescription(Project project, String projectDescr,
      Users user) throws AppException {
    if (project.getDescription() == null || !project.getDescription().equals(projectDescr)) {
      project.setDescription(projectDescr);
      projectFacade.mergeProject(project);
      logProject(project, OperationType.Update);
      logActivity(ActivityFacade.PROJECT_DESC_CHANGED + projectDescr,
          ActivityFacade.FLAG_PROJECT, user, project);
      return true;
    }
    return false;
  }

  /**
   * Change, if necessary, the project retention period
   * <p/>
   *
   * @param project the project to change
   * @param projectRetention the retention period
   * @param user the user making the change
   * @return
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  public boolean updateProjectRetention(Project project, Date projectRetention,
      Users user) throws AppException {
    if (project.getRetentionPeriod() == null || !project.getRetentionPeriod().equals(projectRetention)) {
      project.setRetentionPeriod(projectRetention);
      projectFacade.mergeProject(project);
      logProject(project, OperationType.Update);
      logActivity(ActivityFacade.PROJECT_RETENTION_CHANGED + projectRetention,
          ActivityFacade.FLAG_PROJECT, user, project);
      return true;
    }
    return false;
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
  private String mkProjectDIR(String projectName, DistributedFileSystemOps dfso)
      throws IOException {

    String rootDir = Settings.DIR_ROOT;

    boolean rootDirCreated;
    boolean projectDirCreated;

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
   * @param userMail
   * @param projectId
   * @param sessionId
   * @throws AppException if the project could not be found.
   */
  public void removeProject(String userMail, int projectId, String sessionId)
      throws AppException {

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

    cleanup(project, sessionId);
    certificateMaterializer.forceRemoveCertificates(user.getUsername(),
        project.getName(), true);

    if (settings.isPythonKernelEnabled()) {
      jupyterProcessFacade.removePythonKernelsForProject(project.getName());
    }

  }

  public void cleanup(Project project, String sessionId) throws AppException {
    cleanup(project, sessionId, null);
  }
  
  public void cleanup(Project project, String sessionId,
      Future<CertificatesController.CertsResult> certsGenerationFuture)
      throws AppException {
    if (project == null) {
      return;
    }
    int nbTry = 0;
    while (nbTry < 3) {
      nbTry++;

      YarnClientWrapper yarnClientWrapper = ycs.getYarnClientSuper(settings
          .getConfiguration());
      YarnClient client = yarnClientWrapper.getYarnClient();
      try {
        //remove from project_team so that nobody can see the project anymore
        updateProjectTeamRole(project, ProjectRoleTypes.UNDER_REMOVAL);

        /*
         * get all running yarn application owned by anny of the project members
         * we will check later if this application have been stoped and their log aggregation have been finished
         * it would be better to check all application (even the ones that have finished running)
         * but the log aggregation status is not recovered when the resource manager restart. As a result
         * we can't know if the status in "NOT_START" because we should wait for it or because the
         * resourcemanager restarted.
         */
        Collection<ProjectTeam> team = project.getProjectTeamCollection();
        Set<String> hdfsUsers = new HashSet<>();
        for (ProjectTeam pt : team) {
          String hdfsUsername = hdfsUsersController.getHdfsUserName(project, pt.
              getUser());
          hdfsUsers.add(hdfsUsername);
        }
        hdfsUsers.add(project.getName());

        List<ApplicationReport> projectsApps = client.getApplications(null, hdfsUsers, null, EnumSet.of(
            YarnApplicationState.ACCEPTED, YarnApplicationState.NEW, YarnApplicationState.NEW_SAVING,
            YarnApplicationState.RUNNING, YarnApplicationState.SUBMITTED));

        //Restart zeppelin so interpreters shut down
        Response resp = ClientBuilder.newClient()
            .target(settings.getRestEndpoint()
                + "/hopsworks-api/api/zeppelin/" + project.getId()
                + "/interpreter/check")
            .request()
            .cookie("SESSION", sessionId)
            .method("GET");
        LOGGER.log(Level.FINE, "Zeppelin check resp:{0}", resp.getStatus());
        if (resp.getStatus() == 200) {
          resp = ClientBuilder.newClient()
              .target(settings.getRestEndpoint()
                  + "/hopsworks-api/api/zeppelin/" + project.getId()
                  + "/interpreter/restart")
              .request()
              .cookie("SESSION", sessionId)
              .method("GET");
          LOGGER.log(Level.FINE, "Zeppelin restart resp:{0}", resp.getStatus());
          if (resp.getStatus() != 200) {
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Could not close zeppelin interpreters, please wait 60 seconds to retry");
          }
        }

        // try and close all the jupyter jobs
        jupyterProcessFacade.stopProject(project);
        try {
          removeAnacondaEnv(project);
        } catch (AppException ex) {
          LOGGER.log(Level.SEVERE, "Problem removing Anaconda Environment:{0}", project.getName());
        }

        //kill jobs
        List<Jobs> running = jobFacade.getRunningJobs(project);
        if (running != null && !running.isEmpty()) {
          Runtime rt = Runtime.getRuntime();
          for (Jobs job : running) {
            //Get the appId of the running app
            List<Execution> jobExecs = execFacade.findForJob(job);
            //Sort descending based on jobId because therie might be two 
            // jobs with the same name and we want the latest
            Collections.sort(jobExecs, new Comparator<Execution>() {
              @Override
              public int compare(Execution lhs, Execution rhs) {
                return lhs.getId() > rhs.getId() ? -1 : (lhs.getId() < rhs.
                    getId()) ? 1 : 0;
              }
            });
            try {
              rt.exec(settings.getHadoopSymbolicLinkDir() + "/bin/yarn application -kill "
                  + jobExecs.get(0).getAppId());
            } catch (IOException ex) {
              Logger.getLogger(ProjectController.class.getName()).
                  log(Level.SEVERE, null, ex);
            }
          }
        }

        for (ApplicationReport appReport : projectsApps) {
          FinalApplicationStatus finalState = appReport.getFinalApplicationStatus();
          while (finalState.equals(FinalApplicationStatus.UNDEFINED)) {
            client.killApplication(appReport.getApplicationId());
            appReport = client.getApplicationReport(appReport.getApplicationId());
            finalState = appReport.getFinalApplicationStatus();
          }
          LogAggregationStatus logAggregationState = appReport.getLogAggregationStatus();
          while (!YarnLogUtil.isFinal(logAggregationState)) {
            Thread.sleep(500);
            appReport = client.getApplicationReport(appReport.getApplicationId());
            logAggregationState = appReport.getLogAggregationStatus();
          }
        }

        List<HdfsUsers> usersToClean = getUsersToClean(project);
        List<HdfsGroups> groupsToClean = getGroupsToClean(project);
        removeProjectInt(project, usersToClean, groupsToClean, certsGenerationFuture);
        return;
      } catch (Exception ex) {
        if (nbTry < 3) {
          try {
            Thread.sleep(nbTry * 1000);
          } catch (InterruptedException ex1) {
            LOGGER.log(Level.SEVERE, null, ex1);
          }
        } else {
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(), ex.getMessage());
        }
      } finally {
        ycs.closeYarnClient(yarnClientWrapper);
      }
    }
  }

  private void removeProjectInt(Project project, List<HdfsUsers> usersToClean,
      List<HdfsGroups> groupsToClean, Future<CertificatesController
      .CertsResult> certsGenerationFuture)
      throws IOException, InterruptedException, ExecutionException,
      AppException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();

      //log removal to notify elastic search
      logProject(project, OperationType.Delete);
      //change the owner and group of the project folder to hdfs super user
      String path = File.separator + Settings.DIR_ROOT + File.separator
          + project.getName();
      Path location = new Path(path);
      if (dfso.exists(path)) {
        dfso.setOwner(location, settings.getHdfsSuperUser(), settings.
            getHdfsSuperUser());
      }

      Path dumy = new Path("/tmp/" + project.getName());
      if (dfso.exists(dumy.toString())) {
        dfso.setOwner(dumy, settings.getHdfsSuperUser(), settings.
            getHdfsSuperUser());
      }

      //remove kafka topics
      removeKafkaTopics(project);

      //remove user certificate from local node 
      //(they will be removed from db when the project folder is deleted)
      if (certsGenerationFuture != null) {
        certsGenerationFuture.get();
      }
      certificatesController.deleteProjectCertificates(project);
      
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
        String tmpCertsDir = settings.getHdfsTmpCertDir() + "/" + hdfsUser.
            getName();
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

      //remove anaconda repos
      removeAnacondaEnv(project);

      //remove anaconda repos
      removeJupyter(project);

      //remove folder
      removeProjectFolder(project.getName(), dfso);

      LOGGER.log(Level.INFO, "{0} - project removed.", project.getName());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  @TransactionAttribute(
      TransactionAttributeType.REQUIRES_NEW)
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
  private void removeKafkaTopics(Project project) throws InterruptedException,
      AppException {
    kafkaFacade.removeAllTopicsFromProject(project);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void removeQuotas(Project project) {
    YarnProjectsQuota yarnProjectsQuota = yarnProjectsQuotaFacade.
        findByProjectName(project.getName());
    yarnProjectsQuotaFacade.remove(yarnProjectsQuota);
  }

  @TransactionAttribute(
      TransactionAttributeType.REQUIRES_NEW)
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

  @TransactionAttribute(
      TransactionAttributeType.REQUIRES_NEW)
  private void removeGroupAndUsers(List<HdfsGroups> groups,
      List<HdfsUsers> users) throws IOException {
    hdfsUsersBean.deleteGroups(groups);
    hdfsUsersBean.deleteUsers(users);
  }

  private void removeProjectFolder(String projectName,
      DistributedFileSystemOps dfso) throws IOException {
    String path = File.separator + Settings.DIR_ROOT + File.separator
        + projectName;
    final Path location = new Path(path);
    dfso.rm(location, true);
  }

  /**
   * Adds new team members to a project(project) - bulk persist if team role not
   * specified or not in (Data owner or Data
   * scientist)defaults to Data scientist
   *
   *
   * @param project
   * @param email
   * @param projectTeams
   * @return a list of user names that could not be added to the project team
   * list.
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  @TransactionAttribute(
      TransactionAttributeType.NEVER)
  public List<String> addMembers(Project project, String email,
      List<ProjectTeam> projectTeams) throws AppException {
    List<String> failedList = new ArrayList<>();
    if (projectTeams == null) {
      return failedList;
    }

    Users user = userBean.getUserByEmail(email);
    Users newMember;
    for (ProjectTeam projectTeam : projectTeams) {
      try {
        if (!projectTeam.getProjectTeamPK().getTeamMember().equals(user.
            getEmail())) {

          //if the role is not properly set set it to the default role (Data Scientist).
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
            // TODO: This should now be a REST call
            Future<CertificatesController.CertsResult> certsResultFuture = null;
            try {
              certsResultFuture = certificatesController
                  .generateCertificates(project, newMember, false);
              certsResultFuture.get();
              if (settings.isPythonKernelEnabled()) {
                jupyterProcessFacade.createPythonKernelForProjectUser(project, newMember);
              }
            } catch (Exception ex) {
              try {
                if (certsResultFuture != null) {
                  certsResultFuture.get();
                }
                certificatesController.deleteUserSpecificCertificates(project,
                    newMember);
              } catch (IOException | InterruptedException | ExecutionException e) {
                String hdfsUser = project.getName() + HdfsUsersController
                    .USER_NAME_DELIMITER + user.getUsername();
                LOGGER.log(Level.SEVERE,
                    "Could not delete user certificates for user " +
                        hdfsUser + ". Manual cleanup is needed!!!");
              }
              LOGGER.log(Level.SEVERE, "error while creating certificates, jupyter kernel: " + ex.getMessage(), ex);
              projectTeamFacade.removeProjectTeam(project, newMember);
              try {
                hdfsUsersBean.
                    removeProjectMember(newMember, project);
              } catch (IOException ex1) {
                LOGGER.log(Level.SEVERE, null, ex1);
                throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                    getStatusCode(), "error while creating a user");
              }

              throw new EJBException("Could not creat certificates for user");
            }

            LOGGER.log(Level.FINE, "{0} - member added to project : {1}.",
                new Object[]{newMember.getEmail(),
                  project.getName()});

            logActivity(ActivityFacade.NEW_MEMBER + projectTeam.
                getProjectTeamPK().getTeamMember(),
                ActivityFacade.FLAG_PROJECT, user, project);
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
        LOGGER.log(Level.SEVERE, "Adding  team member {0} to members failed",
            projectTeam.getProjectTeamPK().getTeamMember());

      }
    }
    
    return failedList;
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   *
   *
   * @param projectID of the project
   * @return project DTO that contains team members and services
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  public ProjectDTO getProjectByID(Integer projectID) throws AppException {
    Project project = projectFacade.find(projectID);
    String name = project.getName();

    //find the project as an inode from hops database
    Inode inode = inodes.getInodeAtPath(File.separator + Settings.DIR_ROOT
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
    String yarnQuota = getYarnQuota(project.getName());
    HdfsInodeAttributes inodeAttrs = getHdfsQuotas(inode.getId());

    Long hdfsQuota = inodeAttrs.getDsquota().longValue();
    Long hdfsUsage = inodeAttrs.getDiskspace().longValue();
    Long hdfsNsQuota = inodeAttrs.getNsquota().longValue();
    Long hdfsNsCount = inodeAttrs.getNscount().longValue();
    QuotasDTO quotas = new QuotasDTO(yarnQuota, hdfsQuota, hdfsUsage,
        hdfsNsQuota, hdfsNsCount);
    return new ProjectDTO(project, inode.getId(), services, projectTeam, quotas);
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   *
   *
   * @param name
   * @return project DTO that contains team members and services
   * @throws io.hops.hopsworks.common.exception.AppException
   */
  public ProjectDTO getProjectByName(String name) throws AppException {
    //find the project entity from hopsworks database
    Project project = projectFacade.findByName(name);

    //find the project as an inode from hops database
    String path = File.separator + Settings.DIR_ROOT + File.separator + name;
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
    LOGGER.log(Level.FINE, "{0} - project created successfully.", project.
        getName());
  }

  public void setHdfsSpaceQuotaInMBs(String projectname, long diskspaceQuotaInMB,
      DistributedFileSystemOps dfso)
      throws IOException {
    dfso.setHdfsSpaceQuotaInMBs(new Path(Settings.getProjectPath(projectname)),
        diskspaceQuotaInMB);
  }

  public HdfsInodeAttributes getHdfsQuotas(int inodeId) throws AppException {

    HdfsInodeAttributes res = em.find(HdfsInodeAttributes.class, inodeId);
    if (res == null) {
      return new HdfsInodeAttributes(inodeId);
    }

    return res;
  }

  /**
   *
   * @param id
   * @return
   * @throws AppException
   */
  public QuotasDTO getQuotas(Integer id) throws AppException {
    ProjectDTO proj = getProjectByID(id);
    String yarnQuota = getYarnQuota(proj.getProjectName());
    HdfsInodeAttributes inodeAttrs = getHdfsQuotas(proj.getInodeid());

    Long hdfsQuota = inodeAttrs.getDsquota().longValue();
    Long hdfsUsage = inodeAttrs.getDiskspace().longValue();
    Long hdfsNsQuota = inodeAttrs.getNsquota().longValue();
    Long hdfsNsCount = inodeAttrs.getNscount().longValue();
    QuotasDTO quotas = new QuotasDTO(yarnQuota, hdfsQuota, hdfsUsage,
        hdfsNsQuota, hdfsNsCount);
    return quotas;
  }

  /**
   * Deletes a member from a project
   *
   * @param project
   * @param email
   * @param toRemoveEmail
   * @throws AppException
   */
  public void removeMemberFromTeam(Project project, String email,
      String toRemoveEmail, String sessionId) throws AppException, Exception {
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
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, userToBeRemoved);

    YarnClientWrapper yarnClientWrapper = ycs.getYarnClientSuper(settings
        .getConfiguration());
    YarnClient client = yarnClientWrapper.getYarnClient();
    try {
      Set<String> hdfsUsers = new HashSet<>();
      hdfsUsers.add(hdfsUser);
      List<ApplicationReport> projectsApps = client.getApplications(null, hdfsUsers, null, EnumSet.of(
          YarnApplicationState.ACCEPTED, YarnApplicationState.NEW, YarnApplicationState.NEW_SAVING,
          YarnApplicationState.RUNNING, YarnApplicationState.SUBMITTED));
      //kill jupitter for this user
      jupyterProcessFacade.stopCleanly(hdfsUser);
      if (settings.isPythonKernelEnabled()) {
        jupyterProcessFacade.removePythonKernelForProjectUser(hdfsUser);
      }

      //kill zeppelin for this user
      Response resp = ClientBuilder.newClient()
          .target(settings.getRestEndpoint()
              + "/hopsworks-api/api/zeppelin/" + project.getId()
              + "/interpreter/check")
          .request()
          .cookie("SESSION", sessionId)
          .method("GET");
      LOGGER.log(Level.FINE, "Zeppelin check resp:{0}", resp.getStatus());
      if (resp.getStatus() == 200) {
        resp = ClientBuilder.newClient()
            .target(settings.getRestEndpoint()
                + "/hopsworks-api/api/zeppelin/" + project.getId()
                + "/interpreter/restart/" + hdfsUser)
            .request()
            .cookie("SESSION", sessionId)
            .method("GET");
        LOGGER.log(Level.FINE, "Zeppelin restart resp:{0}", resp.getStatus());
        if (resp.getStatus() != 200) {
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
              getStatusCode(),
              "Could not close zeppelin interpreters, please wait 60 seconds to retry");
        }
      }
      //kill all jobs run by this user.
      //kill jobs
      List<Jobs> running = jobFacade.getUserRunningJobs(project, hdfsUser);
      if (running != null && !running.isEmpty()) {
        Runtime rt = Runtime.getRuntime();
        for (Jobs job : running) {
          //Get the appId of the running app
          List<Execution> jobExecs = execFacade.findForJob(job);
          //Sort descending based on jobId because there might be two 
          // jobs with the same name and we want the latest
          Collections.sort(jobExecs, new Comparator<Execution>() {
            @Override
            public int compare(Execution lhs, Execution rhs) {
              return lhs.getId() > rhs.getId() ? -1 : (lhs.getId() < rhs.
                  getId()) ? 1 : 0;
            }
          });
          try {
            rt.exec(settings.getHadoopSymbolicLinkDir() + "/bin/yarn application -kill "
                + jobExecs.get(0).getAppId());
          } catch (IOException ex) {
            Logger.getLogger(ProjectController.class.getName()).
                log(Level.SEVERE, null, ex);
          }
        }
      }

      //wait that log aggregation for the jobs finish
      for (ApplicationReport appReport : projectsApps) {
        FinalApplicationStatus finalState = appReport.getFinalApplicationStatus();
        while (finalState.equals(FinalApplicationStatus.UNDEFINED)) {
          client.killApplication(appReport.getApplicationId());
          appReport = client.getApplicationReport(appReport.getApplicationId());
          finalState = appReport.getFinalApplicationStatus();
        }
        LogAggregationStatus logAggregationState = appReport.getLogAggregationStatus();
        while (!YarnLogUtil.isFinal(logAggregationState)) {
          Thread.sleep(500);
          appReport = client.getApplicationReport(appReport.getApplicationId());
          logAggregationState = appReport.getLogAggregationStatus();
        }
      }
    } finally {
      ycs.closeYarnClient(yarnClientWrapper);
    }

    logActivity(ActivityFacade.REMOVED_MEMBER + toRemoveEmail,
        ActivityFacade.FLAG_PROJECT, user, project);

    certificateMaterializer.forceRemoveCertificates(userToBeRemoved
        .getUsername(), project.getName(), false);
    certificatesController.deleteUserSpecificCertificates(project, userToBeRemoved);
  }

  /**
   * Updates the role of a member
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
   *
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
   *
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
   *
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
   *
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

  public void addTourFilesToProject(String username, Project project,
      DistributedFileSystemOps dfso, DistributedFileSystemOps udfso,
      TourProjectType projectType) throws
      AppException {

    Users user = userBean.getUserByEmail(username);
    try {
      datasetController.createDataset(user, project, Settings.HOPS_TOUR_DATASET,
          "files for guide projects", -1, false, true, dfso);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
          getStatusCode(),
          "Something went wrong when adding the tour files to the project");
    }

    if (null != projectType) {
      switch (projectType) {
        case SPARK:
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
              LOGGER.log(Level.WARNING,
                  "More than one spark-examples*.jar found in {0}.", dir.
                      getAbsolutePath());
            }
            String hdfsJarPath = "/" + Settings.DIR_ROOT + "/" + project.getName() + "/" + Settings.HOPS_TOUR_DATASET
                + "/spark-examples.jar";
            udfso.copyToHDFSFromLocal(false, file[0].getAbsolutePath(), hdfsJarPath);
            String datasetGroup = hdfsUsersBean.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET);
            String userHdfsName = hdfsUsersBean.getHdfsUserName(project, user);
            udfso.setPermission(new Path(hdfsJarPath), udfso.getParentPermission(new Path(hdfsJarPath)));
            udfso.setOwner(new Path("/" + Settings.DIR_ROOT + "/" + project.getName() + "/" + Settings.HOPS_TOUR_DATASET
                + "/spark-examples.jar"), userHdfsName, datasetGroup);

          } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Something went wrong when adding the tour files to the project");
          }
          break;
        case KAFKA:
          // Get the JAR from /user/<super user>
          String kafkaExampleSrc = "/user/" + settings.getHopsworksUser() + "/"
              + settings.getKafkaTourFilename();
          String kafkaExampleDst = "/" + Settings.DIR_ROOT + "/" + project.getName()
              + "/" + Settings.HOPS_TOUR_DATASET + "/" + settings.getKafkaTourFilename();
          try {
            udfso.copyInHdfs(new Path(kafkaExampleSrc), new Path(kafkaExampleDst));
            String datasetGroup = hdfsUsersBean.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET);
            String userHdfsName = hdfsUsersBean.getHdfsUserName(project, user);
            udfso.setPermission(new Path(kafkaExampleDst), udfso.getParentPermission(new Path(kafkaExampleDst)));
            udfso.setOwner(new Path(kafkaExampleDst), userHdfsName, datasetGroup);

          } catch (IOException ex) {
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(),
                "Something went wrong when adding the tour files to the project");
          }
          break;
        case TENSORFLOW:
        case DISTRIBUTED_TENSORFLOW:
          // Get the mnist.py and tfr records from /user/<super user>/tensorflow_demo
          //Depending on tour type, copy files
          String tensorflowDataSrc = "/user/" + settings.getHdfsSuperUser() + "/" + Settings.HOPS_TENSORFLOW_TOUR_DATA
              + "/*";
          String tensorflowDataDst = "/" + Settings.DIR_ROOT + "/" + project.getName() + "/"
              + Settings.HOPS_TOUR_DATASET;
          try {
            udfso.copyInHdfs(new Path(tensorflowDataSrc), new Path(tensorflowDataDst));
            String datasetGroup = hdfsUsersBean.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET);
            String userHdfsName = hdfsUsersBean.getHdfsUserName(project, user);
            Inode parent = inodes.getInodeAtPath(tensorflowDataDst);
            List<Inode> children = new ArrayList<>();
            inodes.getAllChildren(parent, children);
            for (Inode child : children) {
              if (child.getHdfsUser() != null && child.getHdfsUser().getName().equals(settings.getHdfsSuperUser())) {
                Path path = new Path(inodes.getPath(child));
                udfso.setPermission(path, udfso.getParentPermission(path));
                udfso.setOwner(path, userHdfsName, datasetGroup);
              }
            }
            //Move notebooks to Jupyter Dataset
            if (projectType == TourProjectType.TENSORFLOW) {
              String tensorflowNotebooksSrc = tensorflowDataDst + "/notebooks";
              String tensorflowNotebooksDst = "/" + Settings.DIR_ROOT + "/" + project.getName() + "/"
                  + Settings.HOPS_TOUR_DATASET_JUPYTER;
              udfso.copyInHdfs(new Path(tensorflowNotebooksSrc + "/*"), new Path(tensorflowNotebooksDst));
              datasetGroup = hdfsUsersBean.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET_JUPYTER);
              Inode parentJupyterDs = inodes.getInodeAtPath(tensorflowNotebooksDst);
              List<Inode> childrenJupyterDs = new ArrayList<>();
              inodes.getAllChildren(parentJupyterDs, childrenJupyterDs);
              for (Inode child : childrenJupyterDs) {
                if (child.getHdfsUser() != null) {
                  Path path = new Path(inodes.getPath(child));
                  udfso.setPermission(path, udfso.getParentPermission(path));
                  udfso.setOwner(path, userHdfsName, datasetGroup);
                }
              }
              udfso.rm(new Path(tensorflowNotebooksSrc), true);
            }
          } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Something went wrong when adding the tour files to the project", ex);
            throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                getStatusCode(), "Something went wrong when adding the tour files to the project");
          }
          break;
        default:
          break;
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

  @TransactionAttribute(
      TransactionAttributeType.NEVER)
  public void createAnacondaEnv(Project project) throws AppException {
    pythonDepsFacade.getPreInstalledLibs(project);

  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeAnacondaEnv(Project project) throws AppException {
    pythonDepsFacade.removeProject(project);
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeJupyter(Project project) throws AppException {
    jupyterProcessFacade.removeProject(project);
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
        + "\":{\"type\":\"string\",\"index\":\"not_analyzed\"},"
        + "\"jobname\":{\"type\":\"string\",\"index\":\"not_analyzed\"},"
        + "\"file\":{\"type\":\"string\",\"index\":\"not_analyzed\"},"
        + "\"timestamp\":{\"type\":\"date\",\"index\":\"not_analyzed\"},"
        + "\"project\":{\"type\":\"string\",\"index\":\"not_analyzed\"}},\n"
        + "\"_ttl\": {\n" + "\"enabled\": true,\n" + "\"default\": \""
        + Settings.getJobLogsExpiration() + "s\"\n" + "}}}}");

    JSONObject resp = elasticController.sendElasticsearchReq(params);
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
        + "\\\":false,\\\"indexed\\\":true,\\\"analyzed\\\":false,"
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
    resp = elasticController.sendElasticsearchReq(params);
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
    JSONObject resp = elasticController.sendElasticsearchReq(params);
    boolean kibanaIndexDeleted = false;
    if (resp != null && resp.has("acknowledged")) {
      kibanaIndexDeleted = (Boolean) resp.get("acknowledged");
    }

    //2. Delete Elasticsearch Index
    params.put("resource", "");
    resp = elasticController.sendElasticsearchReq(params);
    boolean elasticIndexDeleted = false;
    if (resp != null && resp.has("acknowledged")) {
      elasticIndexDeleted = (Boolean) resp.get("acknowledged");
    }
    //3. Delete Elasticsearch Template
    params.put("resource", "_template");
    boolean templateDeleted = false;
    resp = elasticController.sendElasticsearchReq(params);
    if (resp != null && resp.has("acknowledged")) {
      templateDeleted = (Boolean) resp.get("acknowledged");
    }

    return elasticIndexDeleted && templateDeleted && kibanaIndexDeleted;
  }

  /**
   *
   * @param params
   * @return
   * @throws MalformedURLException
   * @throws IOException
   */
  private JSONObject sendElasticsearchReq(Map<String, String> params) throws MalformedURLException, IOException {
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

    } catch (IOException ex) {
      if (ex.getMessage().contains("kibana")) {
        LOGGER.log(Level.WARNING, "error", ex);
        LOGGER.log(Level.WARNING, "Kibana index could not be deleted for "
            + params.get("project"));
      } else {
        throw new IOException(ex);
      }
    }
    return null;
  }

  public CertPwDTO getProjectSpecificCertPw(Users user, String projectName,
      String keyStore) throws Exception {
    //Compare the sent certificate with the one in the database
    String keypw = HopsUtils.decrypt(user.getPassword(), settings.getHopsworksMasterPasswordSsl(), userCertsFacade.
        findUserCert(projectName, user.getUsername()).getUserKeyPwd());
    String projectUser = projectName + HdfsUsersController.USER_NAME_DELIMITER
        + user.getUsername();
    validateCert(Base64.decodeBase64(keyStore), keypw.toCharArray(),
        projectUser, true);

    CertPwDTO respDTO = new CertPwDTO();
    respDTO.setKeyPw(keypw);
    respDTO.setTrustPw(keypw);
    return respDTO;
  }

  public CertPwDTO getProjectWideCertPw(Users user, String projectName,
      String keyStore) throws Exception {
    List<ServiceCerts> serviceCerts = userCertsFacade.findServiceCertsByName(projectName);
    if (serviceCerts.isEmpty() || serviceCerts.size() > 1) {
      throw new Exception("Found more than one or none project-wide " + "certificates for project " + projectName);
    }

    String keypw = HopsUtils.decrypt(user.getPassword(), settings
        .getHopsworksMasterPasswordSsl(), serviceCerts.get(0)
            .getCertificatePassword());
    validateCert(Base64.decodeBase64(keyStore), keypw.toCharArray(),
        projectName, false);

    CertPwDTO respDTO = new CertPwDTO();
    respDTO.setKeyPw(keypw);
    respDTO.setTrustPw(keypw);

    return respDTO;
  }

  /**
   * Returns the project user from the keystore and verifies it.
   *
   * @param keyStore
   * @param keyStorePwd
   * @return
   * @throws AppException
   */
  public void validateCert(byte[] keyStore, char[] keyStorePwd, String projectUser, boolean isProjectSpecific)
      throws AppException {
    String commonName = certificatesController.extractCNFromCertificate
        (keyStore, keyStorePwd, projectUser);
  
    if (!projectUser.equals(commonName)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "Certificate CN does not match the username provided");
    }
  
    byte[] userKey;
  
    if (isProjectSpecific) {
      userKey = userCertsFacade.findUserCert(hdfsUsersBean.
              getProjectName(commonName),
          hdfsUsersBean.getUserName(commonName)).getUserKey();
    } else {
      // In that case projectUser is the name of the Project, see Spark
      // interpreter in Zeppelin
      List<ServiceCerts> serviceCerts = userCertsFacade
          .findServiceCertsByName(projectUser);
      if (serviceCerts.isEmpty() || serviceCerts.size() > 1) {
        throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
            "Could not find exactly one certificate for " + projectUser);
      }
      userKey = serviceCerts.get(0).getServiceKey();
    }
  
    if (!Arrays.equals(userKey, keyStore)) {
      throw new AppException(Response.Status.UNAUTHORIZED.getStatusCode(),
          "Certificate error!");
    }
  }
}
