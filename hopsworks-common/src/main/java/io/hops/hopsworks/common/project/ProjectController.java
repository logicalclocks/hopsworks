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
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsersFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.ValidationException;
import org.apache.hadoop.conf.Configuration;
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
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public Project createProject(ProjectDTO newProject, String email,
          DistributedFileSystemOps dfso) throws
          IOException, AppException {
    Users user = userBean.getUserByEmail(email);
    try {
      FolderNameValidator.isValidName(newProject.getProjectName());
    } catch (ValidationException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.INVALID_PROJECT_NAME);
    }
    if (projectFacade.numProjectsLimitReached(user)) {
      logger.log(Level.SEVERE,
              "You have reached the maximum number of allowed projects.");
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.NUM_PROJECTS_LIMIT_REACHED);
    } else if (projectFacade.projectExists(newProject.getProjectName())) {
      logger.log(Level.INFO, "Project with name {0} already exists!",
              newProject.getProjectName());
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_EXISTS);
    } else if (dfso.exists(File.separator + settings.DIR_ROOT
            + File.separator + newProject.getProjectName())) {
      logger.log(Level.WARNING, "Project with name {0} already exists in hdfs. "
              + "Possible inconsistency! project name not in database.",
              newProject.getProjectName());
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_EXISTS);
    } else { // create the project!


      /*
       * first create the folder structure in hdfs. If it is successful move on
       * to create the project in hopsworks database
       */
      String projectPath = mkProjectDIR(newProject.getProjectName(), dfso);
      if (projectPath != null) {

        //Create a new project object
        Date now = new Date();
        Project project = new Project(newProject.getProjectName(), user, now);
        project.setDescription(newProject.getDescription());

        // make ethical status pending
        project.setEthicalStatus(ConsentStatus.PENDING.name());

        // set retention period to next 10 years by default
        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        cal.add(Calendar.YEAR, 10);
        project.setRetentionPeriod(cal.getTime());

        Inode projectInode = this.inodes.getProjectRoot(project.getName());
        if (projectInode == null) {
          // delete the project if there's an error/
          DistributedFileSystemOps udfso = dfs.getDfsOps(project.getOwner().
                  getUsername());
          try {
            removeByID(project.getId(), project.getOwner().getEmail(), true,
                    udfso, dfs.getDfsOps());
          } catch (IOException | AppException t) {
            // do nothing
          } finally {
            udfso.close();
          }
          logger.log(Level.SEVERE, "Couldn't get Inode for the project: {0}",
                  project.getName());
          throw new AppException(Response.Status.INTERNAL_SERVER_ERROR.
                  getStatusCode(),
                  ResponseMessages.INTERNAL_SERVER_ERROR);
        }
        project.setInode(projectInode);

        //Persist project object
        this.projectFacade.persistProject(project);
        this.projectFacade.flushEm();
        logProject(project, OperationType.Add);
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
        //Add the activity information
        logActivity(ActivityFacade.NEW_PROJECT + project.getName(),
                ActivityFacade.FLAG_PROJECT, user, project);
        //update role information in project
        addProjectOwner(project.getId(), user.getEmail());
        logger.log(Level.FINE, "{0} - project created successfully.", project.
                getName());

        //Create default DataSets
        return project;
      }
    }
    return null;
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
  public void createProjectLogResources(String username, Project project,
          DistributedFileSystemOps dfso, DistributedFileSystemOps udfso) throws
          ProjectInternalFoldersFailedException, AppException {

    Users user = userBean.getUserByEmail(username);
    List<ProjectServiceEnum> services = projectServicesFacade.
            findEnabledServicesForProject(project);
    try {
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

        if (searchableResources) {
          Dataset dataset = datasetFacade.findByNameAndProjectId(project, ds.
                  getName());
          datasetController.logDataset(dataset, OperationType.Add);
        }

        //Persist README.md to hdfs for Default Datasets
        datasetController.generateReadme(udfso, ds.getName(),
                ds.getDescription(), project.getName());

        // Add the metrics.properties file to the /Resources dataset
//        if (ds.equals(Settings.DefaultDataset.RESOURCES)) {
//          StringBuilder metrics_props;
//          FSDataOutputStream fsOut = null;
//          try {
//            metrics_props
//                    = ConfigFileGenerator.
//                            instantiateFromTemplate(
//                                    ConfigFileGenerator.METRICS_TEMPLATE
//                            //              "spark_dir", settings.getSparkDir(),
//                            );
//            String metricsFilePath = "/Projects/" + project + "/"
//                    + ds.name() + "/" + Settings.SPARK_METRICS_PROPS;
//
//            fsOut = udfso.create(metricsFilePath);
//            fsOut.writeBytes(metrics_props.toString());
//            fsOut.flush();
//            udfso.setPermission(new org.apache.hadoop.fs.Path(metricsFilePath),
//                    new FsPermission(FsAction.ALL,
//                            FsAction.READ_EXECUTE,
//                            FsAction.NONE));
//          } catch (IOException ex) {
//            logger.log(Level.WARNING,
//                    "metrics.properties could not be generated for project"
//                    + " {0} and dataset {1}.", new Object[]{project, ds.name()});
//          } finally {
//            if (fsOut != null) {
//              fsOut.close();
//            }
//          }
//        }
      }
    } catch (IOException | EJBException e) {
      throw new ProjectInternalFoldersFailedException(
              "Could not create project resources ", e);
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
    boolean sshAdded = false;
    for (ProjectServiceEnum se : services) {
      if (!projectServicesFacade.findEnabledServicesForProject(project).
              contains(se)) {
        projectServicesFacade.addServiceForProject(project, se);
        addedService = true;
        if (se == ProjectServiceEnum.SSH) {
          sshAdded = true;
        }
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
//      if (sshAdded == true) {
//        try {
//
//          // For all members of the project, create an account for them and 
//          //copy their public keys to ~/.ssh/authorized_keys
//          List<ProjectTeam> members = projectTeamFacade.findMembersByProject(
//                  project);
//          for (ProjectTeam pt : members) {
//            Users myUser = pt.getUser();
//            List<SshKeys> keys = sshKeysBean.findAllById(myUser.getUid());
//            List<String> publicKeys = new ArrayList<>();
//            for (SshKeys k : keys) {
//              publicKeys.add(k.getPublicKey());
//            }
//            LocalhostServices.createUserAccount(myUser.getUsername(), project.
//                    getName(), publicKeys);
//          }
//
//        } catch (IOException e) {
//          // TODO - propagate exception?
//          logger.warning("Could not create user account: " + e.getMessage());
//        }
//      }

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
    st.setTeamRole(ProjectRoleTypes.DATA_OWNER.getTeam());
    st.setTimestamp(new Date());
    projectTeamFacade.persistProjectTeam(st);
  }

  //create project in HDFS
  private String mkProjectDIR(String projectName, DistributedFileSystemOps dfso)
          throws IOException {

    String rootDir = settings.DIR_ROOT;

    boolean rootDirCreated = false;
    boolean projectDirCreated = false;
    boolean childDirCreated = false;

    if (!dfso.isDir(rootDir)) {
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
    String fullProjectPath = File.separator + rootDir + File.separator
            + projectName;
    String project = this.extractProjectName(fullProjectPath + File.separator);
    String projectPath = File.separator + rootDir + File.separator + project;
    //Create first the projectPath
    projectDirCreated = dfso.mkdir(projectPath); //fails here

    ProjectController.this.setHdfsSpaceQuotaInMBs(projectName, settings.
            getHdfsDefaultQuotaInMBs(), dfso);

    //create the rest of the child folders if any
    if (projectDirCreated && !fullProjectPath.equals(projectPath)) {
      childDirCreated = dfso.mkdir(fullProjectPath);
    } else if (projectDirCreated) {
      childDirCreated = true;
    }

    if (rootDirCreated && projectDirCreated && childDirCreated) {
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
  @TransactionAttribute(
          TransactionAttributeType.REQUIRES_NEW)
  public boolean removeByID(Integer projectID, String email,
          boolean deleteFilesOnRemove, DistributedFileSystemOps udfso,
          DistributedFileSystemOps dfso) throws
          IOException, AppException {
    boolean success = !deleteFilesOnRemove;

    Project project = projectFacade.find(projectID);
    Users user = userBean.findByEmail(email);
    if (project == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_NOT_FOUND);
    }

    YarnProjectsQuota yarnProjectsQuota = yarnProjectsQuotaFacade.
            findByProjectName(project.getName());
    List<Dataset> dsInProject = datasetFacade.findByProject(project);
    Collection<ProjectTeam> projectTeam = projectTeamFacade.
            findMembersByProject(project);
    //if we remove the project we cant store activity that has a reference to it!!
    //logActivity(ActivityFacade.REMOVED_PROJECT,
    //ActivityFacade.FLAG_PROJECT, user, project);
    if (deleteFilesOnRemove) {
      //clean up log aggregation folder
      File yarnConfFile = new File(settings.getHadoopConfDir(),
              Settings.DEFAULT_YARN_CONFFILE_NAME);
      if (!yarnConfFile.exists()) {
        logger.log(Level.SEVERE, "Unable to locate configuration file in {0}",
                yarnConfFile);
        throw new IllegalStateException("No yarn conf file: yarn-site.xml");
      }

      Configuration conf = new Configuration();
      conf.addResource(new Path(yarnConfFile.getAbsolutePath()));
      String logPath = conf.get(YarnConfiguration.NM_REMOTE_APP_LOG_DIR,
              YarnConfiguration.DEFAULT_NM_REMOTE_APP_LOG_DIR);

      Properties sparkProperties = new Properties();
      InputStream is = null;
      String historyPath = "";
      try {
        is = new FileInputStream(settings.getSparkDir() + "/"
                + Settings.SPARK_CONFIG_FILE);
        sparkProperties.load(is);

        //For every property that is in the spark configuration file but is not
        //already set, create a java system property.
        historyPath = sparkProperties.getProperty("spark.eventLog.dir");
      } finally {
        if (is != null) {
          is.close();
        }
      }

      String hdfsUsername;
      List<String> usersToClean = new ArrayList<>();
      List<Integer> userIdToClean = new ArrayList<>();

      for (ProjectTeam member : projectTeam) {
        hdfsUsername = hdfsUsersBean.getHdfsUserName(project, member.getUser());
        usersToClean.add(hdfsUsername);

        int userId = hdfsUsersFacade.findByName(hdfsUsername).getId();
        userIdToClean.add(userId);
      }

      //clean up project
      String path = File.separator + settings.DIR_ROOT + File.separator
              + project.getName();
      Path location = new Path(path);
      success = udfso.rm(location, true);
      //if the files are removed the group should also go.
      if (success) {
        for (String hdfsUserName : usersToClean) {
          location = new Path(logPath + "/" + hdfsUserName);
          dfso.rm(location, true);
        }
        for (int userId : userIdToClean) {
          List<Inode> inodes = inodeFacade.findByHdfsUser(new HdfsUsers(userId));
          for (Inode inode : inodes) {
            if (inode.getInodePK().getName().contains("snappy")) {
              location = new Path(historyPath + "/" + inode.getInodePK().
                      getName());
              logger.log(Level.SEVERE, "chown " + location.toString());
              dfso.setOwner(location, UserGroupInformation.getLoginUser().
                      getUserName(), "hadoop");

            }
          }
        }
        //Clean up tmp certificates dir from hdfs
        String tmpCertsDir = Settings.TMP_CERT_STORE_REMOTE + File.separator
                + hdfsUsersBean.getHdfsUserName(project, user);
        if (dfso.exists(tmpCertsDir)) {
          dfso.rm(new Path(tmpCertsDir), true);
        }

        hdfsUsersBean.deleteProjectGroupsRecursive(project, dsInProject);
        hdfsUsersBean.deleteProjectUsers(project, projectTeam);
        //ZeppelinConfigFactory.deleteZeppelinConfDir(project);
        //projectPaymentsHistoryFacade.remove(projectPaymentsHistory);
        yarnProjectsQuotaFacade.remove(yarnProjectsQuota);
      }
    } else {
      projectFacade.remove(project);
      //projectPaymentsHistoryFacade.remove(projectPaymentsHistory);
      yarnProjectsQuotaFacade.remove(yarnProjectsQuota);
    }

    logProject(project, OperationType.Delete);

    // TODO: DELETE THE KAFKA TOPICS
    userCertsFacade.removeAllCertsOfAProject(project.getName());

    //Delete elasticsearch template for this project
    manageElasticsearch(project.getName(), false);

    LocalhostServices.deleteProjectCertificates(settings.getIntermediateCaDir(),
            project.getName());
    logger.log(Level.INFO, "{0} - project removed.", project.getName());

    return success;
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
          List<ProjectTeam> projectTeams) {
    List<String> failedList = new ArrayList<>();
    Users user = userBean.getUserByEmail(email);
    Users newMember;
    for (ProjectTeam projectTeam : projectTeams) {
      try {
        if (!projectTeam.getProjectTeamPK().getTeamMember().equals(user.
                getEmail())) {

          //if the role is not properly set set it to the default resercher.
          if (projectTeam.getTeamRole() == null || (!projectTeam.getTeamRole().
                  equals(ProjectRoleTypes.DATA_SCIENTIST.getTeam())
                  && !projectTeam.
                  getTeamRole().equals(ProjectRoleTypes.DATA_OWNER.
                          getTeam()))) {
            projectTeam.setTeamRole(ProjectRoleTypes.DATA_SCIENTIST.getTeam());
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
            LocalhostServices.createUserCertificates(settings.
                    getIntermediateCaDir(),
                    project.getName(), newMember.getUsername());

            userCertsFacade.putUserCerts(project.getName(), newMember.
                    getUsername());

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

      } catch (IOException ex) {
        Logger.getLogger(ProjectController.class
                .getName()).log(Level.SEVERE,
                        null, ex);
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

  public void setHdfsSpaceQuotaInMBs(String projectname, long diskspaceQuotaInMB,
          DistributedFileSystemOps dfso)
          throws IOException {
    dfso.setHdfsSpaceQuotaInMBs(new Path(settings.getProjectPath(projectname)),
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
    return projectTeamFacade.findByMember(user);
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
    List<ProjectTeam> projectTeams = projectTeamFacade.findByMember(user);
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
          DistributedFileSystemOps dfso, DistributedFileSystemOps udfso) throws
          AppException {

    Users user = userBean.getUserByEmail(username);
    try {
      datasetController.createDataset(user, project, "TestJob",
              "jar file to calculate pi", -1, false, true, dfso, udfso);
    } catch (IOException ex) {
      logger.log(Level.SEVERE, null, ex);
    }
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
  public boolean manageElasticsearch(String project, boolean create)
          throws IOException {
    project = project.toLowerCase();
    Map<String, String> params = new HashMap<>();
    if (create) {
      params.put("op", "PUT");
      params.put("project", project);
      params.put("resource", "_template");
      params.put("data", "{\"template\":\"" + project
              + "\",\"mappings\":{\"logs\":{\"properties\":{\"application\":"
              + "{\"type\":\"string\",\"index\":\"not_analyzed\"},\"host"
              + "\":{\"type\":\"string\",\"index\":\"not_analyzed\"},\""
              + "jobname\":{\"type\":\"string\",\"index\":\"not_analyzed\"},"
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
              + "\\\"type\\\":\\\"number\\\",\\\"count\\\":0,\\\"scripted"
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
    } else {
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
