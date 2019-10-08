/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.project;

import io.hops.hopsworks.common.airflow.AirflowManager;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetType;
import io.hops.hopsworks.common.dao.featurestore.Featurestore;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreDTO;
import io.hops.hopsworks.common.dao.featurestore.online_featurestore.OnlineFeaturestoreController;
import io.hops.hopsworks.common.dao.hdfs.HdfsDirectoryWithQuotaFeature;
import io.hops.hopsworks.common.dao.hdfs.HdfsDirectoryWithQuotaFeatureFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeView;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsGroups;
import io.hops.hopsworks.common.dao.hdfsUser.HdfsUsers;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.jupyter.JupyterProject;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationType;
import io.hops.hopsworks.common.dao.log.operation.OperationsLog;
import io.hops.hopsworks.common.dao.log.operation.OperationsLogFacade;
import io.hops.hopsworks.common.dao.project.PaymentType;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectRoleTypes;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamPK;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FolderNameValidator;
import io.hops.hopsworks.common.elastic.ElasticController;
import io.hops.hopsworks.common.experiments.TensorBoardController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.FsPermissions;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import io.hops.hopsworks.common.jobs.yarn.LocalResourceDTO;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.message.MessageController;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.security.CertificatesController;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.inference.logger.KafkaInferenceLogger;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.TensorBoardException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.FinalApplicationStatus;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.json.JSONObject;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.mail.Message;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectController {

  private static final Logger LOGGER = Logger.getLogger(ProjectController.class.
    getName());
  @EJB
  protected UsersController usersController;
  @EJB
  protected JobController jobController;
  @EJB
  protected ExecutionController executionController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private YarnProjectsQuotaFacade yarnProjectsQuotaFacade;
  @EJB
  private UserFacade userFacade;
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
  private EnvironmentController environmentController;
  @EJB
  private JobFacade jobFacade;
  @EJB
  private KafkaFacade kafkaFacade;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private TensorBoardController tensorBoardController;
  @EJB
  private ElasticController elasticController;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private HiveController hiveController;
  @EJB
  private HdfsUsersController hdfsUsersController;
  @EJB
  private CertificatesController certificatesController;
  @EJB
  private MessageController messageController;
  @EJB
  private HdfsDirectoryWithQuotaFeatureFacade hdfsDirectoryWithQuotaFeatureFacade;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @Inject
  private ServingController servingController;
  @Inject
  @Any
  private Instance<ProjectHandler> projectHandlers;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private EmailBean emailBean;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private AirflowManager airflowManager;
  @EJB
  private ProjectServiceFacade projectServiceFacade;


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
   */
  public Project createProject(ProjectDTO projectDTO, Users owner,
    List<String> failedMembers, String sessionId)
    throws DatasetException, GenericException, KafkaException, ProjectException, UserException, HopsSecurityException,
    ServiceException, FeaturestoreException {

    Long startTime = System.currentTimeMillis();

    //check that the project name is ok
    String projectName = projectDTO.getProjectName();
    FolderNameValidator.isValidProjectName(projectUtils, projectName);

    List<ProjectServiceEnum> projectServices = new ArrayList<>();
    if (projectDTO.getServices() != null) {
      for (String s : projectDTO.getServices()) {
        ProjectServiceEnum se = ProjectServiceEnum.valueOf(s.toUpperCase());
        projectServices.add(se);
      }
    }
    LOGGER.log(Level.FINE, () -> "PROJECT CREATION TIME. Step 1: " + (System.currentTimeMillis() - startTime));

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
        project = createProject(projectName, owner, projectDTO.getDescription(), dfso);
      } catch (EJBException ex) {
        LOGGER.log(Level.WARNING, null, ex);
        Path dummy = new Path("/tmp/" + projectName);
        try {
          dfso.rm(dummy, true);
        } catch (IOException e) {
          LOGGER.log(Level.SEVERE, null, e);
        }
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_EXISTS, Level.SEVERE, "project: " + projectName,
          ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 2 (hdfs): {0}", System.currentTimeMillis() - startTime);

      verifyProject(project, dfso, sessionId);

      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 3 (verify): {0}", System.currentTimeMillis() - startTime);


      // Run the handlers.
      for (ProjectHandler projectHandler : projectHandlers) {
        try {
          projectHandler.preCreate(project);
        } catch (Exception e) {
          cleanup(project, sessionId, null, true, owner);
          throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HANDLER_PRECREATE_ERROR, Level.SEVERE,
            "project: " + project.getName() + ", handler: " + projectHandler.getClassName(), e.getMessage(), e);
        }
      }

      //create certificate for this user
      // User's certificates should be created before making any call to
      // Hadoop clients. Otherwise the client will fail if RPC TLS is enabled
      // This is an async call
      List<Future<?>> projectCreationFutures = new ArrayList<>();
      try {
        projectCreationFutures.add(certificatesController.generateCertificates(project, owner));
      } catch (Exception ex) {
        cleanup(project, sessionId, projectCreationFutures, true, owner);
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.SEVERE,
          "project: " + project.getName() +
            "owner: " + owner.getUsername(), ex.getMessage(), ex);
      }

      String username = hdfsUsersController.getHdfsUserName(project, owner);
      if (username == null || username.isEmpty()) {
        cleanup(project, sessionId, projectCreationFutures, true, owner);
        throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.SEVERE,
          "project: " + project.getName() + "owner: " + owner.getUsername());
      }

      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 4 (certs): {0}", System.currentTimeMillis() - startTime);

      //all the verifications have passed, we can now create the project
      //create the project folder
      try {
        mkProjectDIR(projectName, dfso);
      } catch (IOException | EJBException ex) {
        cleanup(project, sessionId, projectCreationFutures, true, owner);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_FOLDER_NOT_CREATED, Level.SEVERE,
          "project: " + projectName, ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 5 (folders): {0}", System.currentTimeMillis() - startTime);
      //update the project with the project folder inode
      try {
        setProjectInode(project, dfso);
      } catch (IOException | EJBException ex) {
        cleanup(project, sessionId, projectCreationFutures, true, owner);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_INODE_CREATION_ERROR,
          Level.SEVERE, "project: " + projectName, ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 6 (inodes): {0}", System.currentTimeMillis() - startTime);

      //set payment and quotas
      try {
        setProjectOwnerAndQuotas(project, settings.getHdfsDefaultQuotaInMBs(),
          dfso, owner);
      } catch (IOException | EJBException ex) {
        cleanup(project, sessionId, projectCreationFutures, true, owner);
        throw new ProjectException(RESTCodes.ProjectErrorCode.QUOTA_ERROR, Level.SEVERE,
          "project: " + project.getName(), ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 7 (quotas): {0}", System.currentTimeMillis() - startTime);

      try {
        hdfsUsersController.addProjectFolderOwner(project, dfso);
        createProjectLogResources(owner, project, dfso);
      } catch (IOException | EJBException ex) {
        cleanup(project, sessionId, projectCreationFutures);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_SET_PERMISSIONS_ERROR, Level.SEVERE,
          "project: " + projectName, ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 8 (logs): {0}", System.currentTimeMillis() - startTime);

      if (environmentController.condaEnabledHosts()) {
        try {
          environmentController.createEnv("3.6", project);//TODO: use variables for version
        } catch (PythonException | EJBException ex) {
          cleanup(project, sessionId, projectCreationFutures);
          throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_ANACONDA_ENABLE_ERROR, Level.SEVERE,
            "project: " + projectName, ex.getMessage(), ex);
        }
        LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 9 (env): {0}", System.currentTimeMillis() - startTime);
      }

      logProject(project, OperationType.Add);

      // enable services
      for (ProjectServiceEnum service : projectServices) {
        try {
          projectCreationFutures.addAll(addService(project, service, owner, dfso));
        } catch (RESTException ex) {
          cleanup(project, sessionId, projectCreationFutures);
          throw ex;
        }
      }

      try {
        //add members of the project
        failedMembers = new ArrayList<>();
        failedMembers.addAll(addMembers(project, owner, projectDTO.getProjectTeam()));
      } catch (KafkaException | UserException | ProjectException | EJBException ex) {
        cleanup(project, sessionId, projectCreationFutures);
        throw ex;
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 9 (members): {0}", System.currentTimeMillis() - startTime);

      try {
        for (Future f : projectCreationFutures) {
          if (f != null) {
            f.get();
          }
        }
      } catch (InterruptedException | ExecutionException ex) {
        LOGGER.log(Level.SEVERE, "Error while waiting for the certificate "
          + "generation thread to finish. Will try to cleanup...", ex);
        cleanup(project, sessionId, projectCreationFutures);
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.SEVERE);
      }

      // Run the handlers.
      for (ProjectHandler projectHandler : projectHandlers) {
        try {
          projectHandler.postCreate(project);
        } catch (Exception e) {
          cleanup(project, sessionId, projectCreationFutures);
          throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HANDLER_POSTCREATE_ERROR, Level.SEVERE,
            "project: " + projectName, e.getMessage(), e);
        }
      }

      return project;

    } finally {
      if (dfso != null) {
        dfso.close();
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 10 (close): {0}", System.currentTimeMillis() - startTime);
    }

  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void verifyProject(Project project, DistributedFileSystemOps dfso,
    String sessionId)
    throws ProjectException, GenericException {
    //proceed to all the verifications and set up local variable
    //  verify that the project folder does not exist
    //  verify that users and groups corresponding to this project name does not already exist in HDFS
    //  verify that Quota for this project name does not already exists in YARN
    //  verify that There is no logs folders corresponding to this project name
    //  verify that There is no certificates corresponding to this project name in the certificate generator
    final String severity = "Possible inconsistency,  Please contact the administrator.";
    try {
      if (existingProjectFolder(project)) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_FOLDER_EXISTS, Level.INFO, severity,
          project.getName());
      } else if (!noExistingUser(project.getName())) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_USER_EXISTS, Level.INFO, severity,
          project.getName());
      } else if (!noExistingGroup(project.getName())) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_GROUP_EXISTS, Level.INFO, severity,
          project.getName());
      } else if (!verifyQuota(project.getName())) {
        cleanup(project, sessionId, true);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_QUOTA_EXISTS, Level.INFO, project.getName());
      } else if (!verifyLogs(dfso, project.getName())) {
        cleanup(project, sessionId, true);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_LOGS_EXIST, Level.INFO, severity,
          project.getName());
      }
    } catch (IOException | EJBException ex) {
      LOGGER.log(Level.SEVERE, RESTCodes.ProjectErrorCode.PROJECT_VERIFICATIONS_FAILED.toString(), ex);
      cleanup(project, sessionId, true);
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_VERIFICATIONS_FAILED, Level.SEVERE);
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Project createProject(String projectName, Users user,
    String projectDescription, DistributedFileSystemOps dfso) throws ProjectException {
    if (user == null) {
      throw new IllegalArgumentException("User was not provided.");
    }
    if (projectFacade.numProjectsLimitReached(user)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.NUM_PROJECTS_LIMIT_REACHED,
        Level.FINE, "projects already created: " + user.getNumCreatedProjects() + ", out of a maximum: " +
          user.getMaxNumProjects());
    } else if (projectFacade.projectExists(projectName)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_EXISTS, Level.FINE, "project: " + projectName);
    }
    //Create a new project object
    Date now = new Date();
    Project project = new Project(projectName, user, now, PaymentType.PREPAID);
    project.setKafkaMaxNumTopics(settings.getKafkaMaxNumTopics());
    project.setDescription(projectDescription);

    // set retention period to next 10 years by default
    Calendar cal = Calendar.getInstance();
    cal.setTime(now);
    cal.add(Calendar.YEAR, 10);
    project.setRetentionPeriod(cal.getTime());

    //set a dumy node in the project until the creation of the project folder
    Path dummy = new Path("/tmp/" + projectName);
    try {
      dfso.touchz(dummy);
      project.setInode(inodes.getInodeAtPath(dummy.toString()));
    } catch (IOException ex) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_INODE_CREATION_ERROR, Level.SEVERE,
        "Couldn't get the dummy Inode at: /tmp/" + projectName, ex.getMessage(), ex);
    }

    //Persist project object
    this.projectFacade.persistProject(project);
    this.projectFacade.flushEm();
    usersController.increaseNumCreatedProjects(user.getUid());
    return project;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void setProjectInode(Project project, DistributedFileSystemOps dfso) throws IOException {
    Inode projectInode = inodes.getProjectRoot(project.getName());
    project.setInode(projectInode);
    this.projectFacade.mergeProject(project);
    this.projectFacade.flushEm();
    Path dumy = new Path("/tmp/" + project.getName());
    dfso.rm(dumy, true);
  }

  private boolean existingProjectFolder(Project project) {
    Inode projectInode = inodes.getProjectRoot(project.getName());
    if (projectInode != null) {
      LOGGER.log(Level.WARNING, "project folder existing for project {0}",
        project.getName());
      return true;
    }
    return false;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private boolean noExistingUser(String projectName) {
    List<HdfsUsers> hdfsUsers = hdfsUsersController.getAllProjectHdfsUsers(projectName);
    if (hdfsUsers != null && !hdfsUsers.isEmpty()) {
      LOGGER.log(Level.WARNING, "hdfs users exist for project {0}",
        projectName);
      return false;
    }
    return true;
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private boolean noExistingGroup(String projectName) {
    List<HdfsGroups> hdfsGroups = hdfsUsersController.getAllProjectHdfsGroups(projectName);
    if (hdfsGroups != null && !hdfsGroups.isEmpty()) {
      LOGGER.log(Level.WARNING, () -> "hdfs group(s) exist for project: " + projectName + ", group(s): "
        + Arrays.toString(hdfsGroups.toArray()));
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

  /**
   * Project default datasets Logs and Resources need to be created in a
   * separate transaction after the project creation
   * is complete.
   *
   * @param user
   * @param project
   * @param dfso
   * @throws java.io.IOException
   */
  public void createProjectLogResources(Users user, Project project,
    DistributedFileSystemOps dfso) throws IOException, DatasetException, HopsSecurityException {

    for (Settings.BaseDataset ds : Settings.BaseDataset.values()) {
      datasetController.createDataset(user, project, ds.getName(), ds.
        getDescription(), -1, false, true, true, dfso);

      Path dsPath = new Path(Utils.getProjectPath(project.getName()) + ds.getName());

      FileStatus fstatus = dfso.getFileStatus(dsPath);

      // create subdirectories for the resource dataset
      if (ds.equals(Settings.BaseDataset.RESOURCES)) {
        String[] subResources = settings.getResourceDirs().split(";");
        for (String sub : subResources) {
          Path resourceDir = new Path(Utils.getProjectPath(project.getName()),
            ds.getName());
          Path subDirPath = new Path(resourceDir, sub);
          datasetController.createSubDirectory(project, subDirPath, -1,
            "", false, dfso);
          dfso.setOwner(subDirPath, fstatus.getOwner(), fstatus.getGroup());
        }
      } else if (ds.equals(Settings.BaseDataset.LOGS)) {
        // To not fill the SSDs with Logs files that nobody access frequently
        // We set the StoragePolicy for the LOGS dir to be DEFAULT
        dfso.setStoragePolicy(dsPath, DistributedFileSystemOps.StoragePolicy.DEFAULT);
      }

      //Persist README.md to hdfs for Default Datasets
      datasetController.generateReadme(dfso, ds.getName(),
        ds.getDescription(), project.getName());
      Path readmePath = new Path(dsPath, Settings.README_FILE);
      dfso.setOwner(readmePath, fstatus.getOwner(), fstatus.getGroup());
    }
  }

  /**
   * Returns a Project
   *
   * @param id the identifier for a Project
   * @return Project
   */
  public Project findProjectById(Integer id) throws ProjectException {
    Project project = projectFacade.find(id);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + id);
    }
    return project;
  }

  // Used only during project creation
  private List<Future<?>> addService(Project project, ProjectServiceEnum service,
    Users user, DistributedFileSystemOps dfso)
    throws ProjectException, ServiceException, DatasetException, HopsSecurityException,
    UserException, FeaturestoreException {
    return addService(project, service, user, dfso, dfso);
  }

  public List<Future<?>> addService(Project project, ProjectServiceEnum service,
    Users user, DistributedFileSystemOps dfso, DistributedFileSystemOps udfso)
    throws ProjectException, ServiceException, DatasetException, HopsSecurityException,
    UserException, FeaturestoreException {

    List<Future<?>> futureList = new ArrayList<>();

    if (projectServicesFacade.isServiceEnabledForProject(project, service)) {
      // Service already enabled for the current project. Nothing to do
      return null;
    }

    switch (service) {
      case JUPYTER:
        addServiceDataset(project, user, Settings.ServiceDataset.JUPYTER, dfso, udfso);
        if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.JOBS)) {
          addKibana(project);
          addServiceDataset(project, user, Settings.ServiceDataset.EXPERIMENTS, dfso, udfso);
        }
        break;
      case HIVE:
        addServiceHive(project, user, dfso);
        break;
      case SERVING:
        futureList.add(addServiceServing(project, user, dfso, udfso));
        break;
      case JOBS:
        if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.JUPYTER)) {
          addServiceDataset(project, user, Settings.ServiceDataset.EXPERIMENTS, dfso, udfso);
          addKibana(project);
        }
        break;
      case FEATURESTORE:
        //Note: Order matters here. Training Dataset should be created before the Featurestore
        addServiceDataset(project, user, Settings.ServiceDataset.TRAININGDATASETS, dfso, udfso);
        addServiceFeaturestore(project, user, dfso, udfso);
        addServiceDataset(project, user, Settings.ServiceDataset.DATAVALIDATION, dfso, udfso);
        //Enable Jobs service at the same time as featurestore
        if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.JOBS)) {
          if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.JUPYTER)) {
            addServiceDataset(project, user, Settings.ServiceDataset.EXPERIMENTS, dfso, udfso);
            addKibana(project);
          }
        }
        break;
    }

    // Persist enabled service in the database
    projectServicesFacade.addServiceForProject(project, service);
    logActivity(ActivityFacade.ADDED_SERVICE + service.toString(), user, project, ActivityFlag.SERVICE);
    return futureList;
  }

  private void addServiceDataset(Project project, Users user,
    Settings.ServiceDataset ds, DistributedFileSystemOps dfso,
    DistributedFileSystemOps udfso) throws DatasetException, HopsSecurityException, ProjectException {
    try {
      String datasetName = ds.getName();
      //Training Datasets should be shareable, prefix with project name to avoid naming conflicts when sharing
      if (ds == Settings.ServiceDataset.TRAININGDATASETS) {
        datasetName = project.getName() + "_" + datasetName;
      }
      datasetController.createDataset(user, project, datasetName, ds.
        getDescription(), -1, false, false, true, dfso);
      datasetController.generateReadme(udfso, datasetName,
        ds.getDescription(), project.getName());

      // This should only happen in project creation
      // Create dataset and corresponding README file as superuser
      // to postpone waiting for the certificates generation thread when
      // RPC TLS is enabled
      if (dfso == udfso && udfso.getEffectiveUser()
        .equals(settings.getHdfsSuperUser())) {
        StringBuilder dsStrBuilder = new StringBuilder();
        dsStrBuilder.append(Utils.getProjectPath(project.getName())).append(datasetName);
        Path dsPath = new Path(dsStrBuilder.toString());
        FileStatus fstatus = dfso.getFileStatus(dsPath);
        Path readmePath = new Path(dsPath, Settings.README_FILE);
        dfso.setOwner(readmePath, fstatus.getOwner(), fstatus.getGroup());
      }
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Could not create dir: " + ds.getName(), ex);
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_SERVICE_ADD_FAILURE,
        Level.SEVERE, "service: " + ds.toString(), ex.getMessage(), ex);
    }
  }

  private void addServiceHive(Project project, Users user, DistributedFileSystemOps dfso) throws ProjectException {
    try {
      hiveController.createDatabase(project.getName(), "Project general-purpose Hive database");
      hiveController.createDatasetDb(project, user, dfso, project.getName());
    } catch (SQLException | IOException ex) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HIVEDB_CREATE_ERROR, Level.SEVERE,
        "project: " + project.getName(), ex.getMessage(), ex);
    }
  }

  private Future<CertificatesController.CertsResult> addServiceServing(Project project, Users user,
    DistributedFileSystemOps dfso, DistributedFileSystemOps udfso)
    throws ProjectException, DatasetException, HopsSecurityException, UserException {

    addServiceDataset(project, user, Settings.ServiceDataset.SERVING, dfso, udfso);
    elasticController.createIndexPattern(project, project.getName().toLowerCase() + "_serving-*");
    // If Kafka is not enabled for the project, enable it
    if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.KAFKA)) {
      projectServicesFacade.addServiceForProject(project, ProjectServiceEnum.KAFKA);
    }
    return addServingManager(project);
  }

  /**
   * Add to the project the serving manager. The user responsible of writing the inference logs to kafka
   * @param project
   */
  private Future<CertificatesController.CertsResult> addServingManager(Project project) throws HopsSecurityException,
    UserException {
    // Add the Serving Manager user to the project team
    Users servingManagerUser = userFacade.findByUsername(KafkaInferenceLogger.SERVING_MANAGER_USERNAME);
    ProjectTeamPK stp = new ProjectTeamPK(project.getId(), servingManagerUser.getEmail());
    ProjectTeam st = new ProjectTeam(stp);
    st.setTeamRole(ProjectRoleTypes.DATA_SCIENTIST.getRole());
    st.setTimestamp(new Date());
    projectTeamFacade.persistProjectTeam(st);
    // Create the Hdfs user
    hdfsUsersController.addNewProjectMember(project, st);
    // Create the certificate for this project user
    Future<CertificatesController.CertsResult> certsResultFuture = null;
    try {
      certsResultFuture = certificatesController.generateCertificates(project, servingManagerUser);
    } catch (Exception e) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.SEVERE,
        "project: " + project.getName() + "owner: servingmanager", e.getMessage(), e);
    }

    return certsResultFuture;
  }
  
  /**
   * Add the featurestore service to the project,
   * 1. create the hive database for the featurestore
   * 2. insert featurestore metadata in the hopsworks db
   * 3. create a hopsworks dataset for the featurestore
   * 4. create a directory in resources to store json configurations for feature import jobs.
   *
   * @param project the project to add the featurestore service for
   * @param user the user adding the service
   * @param dfso dfso
   */
  private void addServiceFeaturestore(Project project, Users user,
    DistributedFileSystemOps dfso, DistributedFileSystemOps udfso)
    throws FeaturestoreException {
    String featurestoreName = featurestoreController.getOfflineFeaturestoreDbName(project);
    try {
      //Create HiveDB for the featurestore
      hiveController.createDatabase(featurestoreName,
        "Featurestore database for project: " + project.getName());
      //Store featurestore metadata in Hopsworks
      Dataset trainingDatasets = datasetController.getByProjectAndDsName(project,
        null, project.getName() + "_" + Settings.ServiceDataset.TRAININGDATASETS.getName());
      Featurestore featurestore = featurestoreController.createProjectFeatureStore(project, user, featurestoreName,
        trainingDatasets);
      //Create Hopsworks Dataset of the HiveDb
      hiveController.createDatasetDb(project, user, dfso, featurestoreName, DatasetType.FEATURESTORE, featurestore);
    } catch (SQLException | IOException ex) {
      LOGGER.log(Level.SEVERE, RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATURESTORE.getMessage(), ex);
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.COULD_NOT_CREATE_FEATURESTORE, Level.SEVERE,
        "project: " + project.getName(), ex.getMessage(), ex);
    }
  }


  /**
   * Change, if necessary, the project description
   * <p/>
   *
   * @param project the project to change
   * @param projectDescr the description
   * @param user the user making the change
   * @return
   */
  public boolean updateProjectDescription(Project project, String projectDescr,
    Users user) {
    if (project.getDescription() == null || !project.getDescription().equals(projectDescr)) {
      project.setDescription(projectDescr);
      projectFacade.mergeProject(project);
      logProject(project, OperationType.Update);
      logActivity(ActivityFacade.PROJECT_DESC_CHANGED + projectDescr, user, project, ActivityFlag.
        PROJECT);
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
   */
  public boolean updateProjectRetention(Project project, Date projectRetention,
    Users user) {
    if (project.getRetentionPeriod() == null || !project.getRetentionPeriod().equals(projectRetention)) {
      project.setRetentionPeriod(projectRetention);
      projectFacade.mergeProject(project);
      logProject(project, OperationType.Update);
      logActivity(ActivityFacade.PROJECT_RETENTION_CHANGED + projectRetention, user, project,
        ActivityFlag.PROJECT);
      return true;
    }
    return false;
  }

  //Set the project owner as project master in ProjectTeam table
  private void addProjectOwner(Project project, Users user) {
    ProjectTeamPK stp = new ProjectTeamPK(project.getId(), user.getEmail());
    ProjectTeam st = new ProjectTeam(stp);
    st.setTeamRole(ProjectRoleTypes.DATA_OWNER.getRole());
    st.setTimestamp(new Date());
    st.setProject(project);
    st.setUser(user);
    project.getProjectTeamCollection().add(st);
    projectFacade.update(project);
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
      rootDirCreated = dfso.mkdir(location, FsPermission.getDirDefault());
      dfso.setPermission(location, FsPermissions.rwxrwxr_x);

      // Set the DIR_ROOT (/Projects) to have DB storage policy, i.e. - small files stored on db
      dfso.setStoragePolicy(location, DistributedFileSystemOps.StoragePolicy.SMALL_FILES);
    } else {
      rootDirCreated = true;
    }

    /*
     * Marking a project path as meta enabled means that all child folders/files
     * that'll be created down this directory tree will have as a parent this
     * inode.
     */
    String projectPath = Utils.getProjectPath(projectName);
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
   */
  public void removeProject(String userMail, int projectId, String sessionId) throws ProjectException,
    GenericException {

    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    //Only project owner and admin is able to delete a project
    Users user = userFacade.findByEmail(userMail);
    if (!project.getOwner().equals(user) && !usersController.isUserInRole(user, "HOPS_ADMIN")) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_REMOVAL_NOT_ALLOWED, Level.FINE);
    }

    kafkaFacade.removeAclForProject(projectId);

    cleanup(project, sessionId);
  }

  public String[] forceCleanup(String projectName, String userEmail, String sessionId) {
    CleanupLogger cleanupLogger = new CleanupLogger(projectName);
    DistributedFileSystemOps dfso = null;
    YarnClientWrapper yarnClientWrapper = null;
    try {
      dfso = dfs.getDfsOps();
      yarnClientWrapper = ycs.getYarnClientSuper(settings.getConfiguration());
      Project project = projectFacade.findByName(projectName);
      if (project != null) {
        cleanupLogger.logSuccess("Project found in the database");

        // Run custom handler for project deletion
        for (ProjectHandler projectHandler : projectHandlers) {
          try {
            projectHandler.preDelete(project);
            cleanupLogger.logSuccess("Handler " + projectHandler.getClassName() + " successfully run");
          } catch (Exception e) {
            cleanupLogger.logError("Error running handler: " + projectHandler.getClassName()
              + " during project cleanup");
            cleanupLogger.logError(e.getMessage());
          }
        }

        // Remove from Project team
        try {
          updateProjectTeamRole(project, ProjectRoleTypes.UNDER_REMOVAL);
          cleanupLogger.logSuccess("Updated team role");
        } catch (Exception ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Get Yarn applications
        List<ApplicationReport> projectApps = null;
        try {
          Collection<ProjectTeam> team = project.getProjectTeamCollection();
          Set<String> hdfsUsers = new HashSet<>();
          for (ProjectTeam pt : team) {
            String hdfsUsername = hdfsUsersController.getHdfsUserName(project, pt.
              getUser());
            hdfsUsers.add(hdfsUsername);
          }

          projectApps = getYarnApplications(hdfsUsers, yarnClientWrapper.getYarnClient());
          cleanupLogger.logSuccess("Gotten Yarn applications");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when reading YARN apps during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Kill Yarn Jobs
        try {
          killYarnJobs(project);
          cleanupLogger.logSuccess("Killed Yarn jobs");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when killing YARN jobs during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // jupyter notebook server and sessions
        try {
          removeJupyter(project);
          cleanupLogger.logSuccess("Removed Jupyter");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing Anaconda during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Wait for Yarn logs
        try {
          waitForJobLogs(projectApps, yarnClientWrapper.getYarnClient());
          cleanupLogger.logSuccess("Gotten logs for jobs");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when getting Yarn logs during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Log removal
        try {
          logProject(project, OperationType.Delete);
          cleanupLogger.logSuccess("Logged project removal");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when logging project removal during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Change ownership of root dir
        try {
          Path path = new Path(Utils.getProjectPath(project.getName()));
          changeOwnershipToSuperuser(path, dfso);
          cleanupLogger.logSuccess("Changed ownership of root Project dir");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when changing ownership of root Project dir during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Change ownership of tmp file
        Path dummy = new Path("/tmp/" + project.getName());
        try {
          changeOwnershipToSuperuser(dummy, dfso);
          cleanupLogger.logSuccess("Changed ownership of dummy inode");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when changing ownership of dummy inode during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove Kafka
        try {
          removeKafkaTopics(project);
          cleanupLogger.logSuccess("Removed Kafka topics");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing kafka topics during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove certificates
        try {
          certificatesController.revokeProjectCertificates(project);
          cleanupLogger.logSuccess("Removed certificates");
        } catch (HopsSecurityException ex) {
          if (ex.getErrorCode() != RESTCodes.SecurityErrorCode.CERTIFICATE_NOT_FOUND) {
            cleanupLogger.logError("Error when removing certificates during project cleanup");
            cleanupLogger.logError(ex.getMessage());
          }
        } catch (IOException | GenericException ex) {
          cleanupLogger.logError("Error when removing certificates during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        List<HdfsUsers> usersToClean = getUsersToClean(project);
        List<HdfsGroups> groupsToClean = getGroupsToClean(project);

        // Remove project related files
        try {
          removeProjectRelatedFiles(usersToClean, dfso);
          cleanupLogger.logSuccess("Removed project related files");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing project-related files during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove quotas
        try {
          removeQuotas(project);
          cleanupLogger.logSuccess("Removed quotas");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing quota during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Change owner for files in shared datasets
        try {
          fixSharedDatasets(project, dfso);
          cleanupLogger.logSuccess("Fixed shared datasets");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when changing ownership during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // 16) Delete Hive database - will automatically cleanup all the Hive's metadata
        try {
          hiveController.dropDatabases(project, dfso, true);
          cleanupLogger.logSuccess("Removed Hive db");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing hive db during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Delete elasticsearch template for this project
        try {
          removeElasticsearch(project);
          cleanupLogger.logSuccess("Removed ElasticSearch");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing elastic during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // delete project group and users
        try {
          removeGroupAndUsers(groupsToClean, usersToClean);
          cleanupLogger.logSuccess("Removed HDFS Groups and Users");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing HDFS groups/users during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // remove running tensorboards repos
        try {
          removeTensorBoard(project);
          cleanupLogger.logSuccess("Removed local TensorBoards");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing running TensorBoards during project cleanup");
        }

        try {
          servingController.deleteServings(project);
          cleanupLogger.logSuccess("Removed servings");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing serving instances");
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove project DAGs, JWT monitors and free X.509 certificates
        try {
          airflowManager.onProjectRemoval(project);
          cleanupLogger.logSuccess("Removed Airflow DAGs and security references");
        } catch (Exception ex) {
          cleanupLogger.logError("Error while cleaning Airflow DAGs and security references");
          cleanupLogger.logError(ex.getMessage());
        }

        try {
          removeCertificatesFromMaterializer(project);
          cleanupLogger.logSuccess("Removed all X.509 certificates related to the Project from " +
            "CertificateMaterializer");
        } catch (Exception ex) {
          cleanupLogger.logError("Error while force removing Project certificates from CertificateMaterializer");
          cleanupLogger.logError(ex.getMessage());
        }

        // remove conda envs
        try {
          removeAnacondaEnv(project);
          cleanupLogger.logSuccess("Removed conda envs");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing conda envs during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // remove dumy Inode
        try {
          dfso.rm(dummy, true);
          cleanupLogger.logSuccess("Removed dummy Inode");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing dummy Inode during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // remove folder
        try {
          removeProjectFolder(project.getName(), dfso);
          cleanupLogger.logSuccess("Removed root Project folder");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing root Project dir during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Run custom handler for project deletion
        for (ProjectHandler projectHandler : projectHandlers) {
          try {
            projectHandler.postDelete(project);
            cleanupLogger.logSuccess("Handler " + projectHandler.getClassName() + " successfully run");
          } catch (Exception e) {
            cleanupLogger.logError("Error running handler: " + projectHandler.getClassName()
              + " during project cleanup");
            cleanupLogger.logError(e.getMessage());
          }
        }
      } else {
        // Create /tmp/Project and add to database so we lock in case someone tries to create a Project
        // with the same name at the same time
        cleanupLogger.logSuccess("Project is *NOT* in the database, going to remove as much as possible");
        Date now = DateUtils.localDateTime2Date(DateUtils.getNow());
        Users user = userFacade.findByEmail(userEmail);
        Project toDeleteProject = new Project(projectName, user, now, PaymentType.PREPAID);
        toDeleteProject.setKafkaMaxNumTopics(settings.getKafkaMaxNumTopics());
        Path tmpInodePath = new Path(File.separator + "tmp" + File.separator + projectName);
        try {
          if (!dfso.exists(tmpInodePath.toString())) {
            dfso.touchz(tmpInodePath);
          }
          Inode tmpInode = inodes.getInodeAtPath(tmpInodePath.toString());
          if (tmpInode != null) {
            toDeleteProject.setInode(tmpInode);
            projectFacade.persistProject(toDeleteProject);
            projectFacade.flushEm();
            cleanupLogger.logSuccess("Created dummy Inode");
          }
        } catch (IOException ex) {
          cleanupLogger.logError("Could not create dummy Inode, moving on unsafe");
        }

        // Kill jobs
        List<HdfsUsers> projectHdfsUsers = hdfsUsersController.getAllProjectHdfsUsers(projectName);
        try {
          Set<String> hdfsUsersStr = new HashSet<>();
          for (HdfsUsers hdfsUser : projectHdfsUsers) {
            hdfsUsersStr.add(hdfsUser.getName());
          }

          List<ApplicationReport> projectApps = getYarnApplications(hdfsUsersStr, yarnClientWrapper.getYarnClient());
          waitForJobLogs(projectApps, yarnClientWrapper.getYarnClient());
          cleanupLogger.logSuccess("Killed all Yarn Applications");
        } catch (Exception ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove project related files
        try {
          removeProjectRelatedFiles(projectHdfsUsers, dfso);
          cleanupLogger.logSuccess("Removed project related files from HDFS");
        } catch (IOException ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove Hive database
        try {
          hiveController.dropDatabases(toDeleteProject, dfso, true);
          cleanupLogger.logSuccess("Dropped Hive database");
        } catch (IOException ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove ElasticSearch index
        try {
          removeElasticsearch(toDeleteProject);
          cleanupLogger.logSuccess("Removed ElasticSearch");
        } catch (Exception ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove HDFS Groups and Users
        try {
          List<HdfsGroups> projectHdfsGroups = hdfsUsersController.getAllProjectHdfsGroups(projectName);
          removeGroupAndUsers(projectHdfsGroups, projectHdfsUsers);
          cleanupLogger.logSuccess("Removed HDFS Groups and Users");
        } catch (IOException ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove Yarn project quota
        try {
          removeQuotas(toDeleteProject);
          cleanupLogger.logSuccess("Removed project quota");
        } catch (Exception ex) {
          cleanupLogger.logError(ex.getMessage());
        }


        List<ProjectTeam> reconstructedProjectTeam = new ArrayList<>();
        try {
          for (HdfsUsers hdfsUser : hdfsUsersController.getAllProjectHdfsUsers(projectName)) {
            Users foundUser = userFacade.findByUsername(hdfsUser.getUsername());
            if (foundUser != null) {
              reconstructedProjectTeam.add(new ProjectTeam(toDeleteProject, foundUser));
            }
          }
        } catch (Exception ex) {
          // NOOP
        }
        toDeleteProject.setProjectTeamCollection(reconstructedProjectTeam);

        try {
          airflowManager.onProjectRemoval(toDeleteProject);
          cleanupLogger.logSuccess("Removed Airflow DAGs and security references");
        } catch (Exception ex) {
          cleanupLogger.logError("Failed to remove Airflow DAGs and security references");
          cleanupLogger.logError(ex.getMessage());
        }

        try {
          removeCertificatesFromMaterializer(toDeleteProject);
          cleanupLogger.logSuccess("Freed all x.509 references from CertificateMaterializer");
        } catch (Exception ex) {
          cleanupLogger.logError("Failed to free all X.509 references from CertificateMaterializer");
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove Certificates
        try {
          certificatesController.revokeProjectCertificates(project);
          userCertsFacade.removeAllCertsOfAProject(projectName);
          cleanupLogger.logSuccess("Deleted certificates");
        } catch (HopsSecurityException | GenericException | IOException ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove root project directory
        try {
          removeProjectFolder(projectName, dfso);
          cleanupLogger.logSuccess("Removed root project directory");
        } catch (IOException ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove /tmp/project
        try {
          dfso.rm(new Path(File.separator + "tmp" + File.separator + projectName), true);
          cleanupLogger.logSuccess("Removed /tmp");
        } catch (IOException ex) {
          cleanupLogger.logError(ex.getMessage());
        }
      }
    } finally {
      dfs.closeDfsClient(dfso);
      ycs.closeYarnClient(yarnClientWrapper);
      LOGGER.log(Level.INFO, cleanupLogger.getSuccessLog().toString());
      String errorLog = cleanupLogger.getErrorLog().toString();
      if (!errorLog.isEmpty()) {
        LOGGER.log(Level.SEVERE, errorLog);
      }
      sendInbox(cleanupLogger.getSuccessLog().append("\n")
        .append(cleanupLogger.getErrorLog()).append("\n").toString(), userEmail);
    }
    String[] logs = new String[2];
    logs[0] = cleanupLogger.getSuccessLog().toString();
    logs[1] = cleanupLogger.getErrorLog().toString();
    return logs;
  }

  private void sendInbox(String message, String userRequested) {
    Users to = userFacade.findByEmail(userRequested);
    Users from = userFacade.findByEmail(settings.getAdminEmail());
    messageController.send(to, from, "Force project cleanup", "Status", message, "");
  }

  private void removeProjectRelatedFiles(List<HdfsUsers> hdfsUsers, DistributedFileSystemOps dfso)
    throws IOException {
    String logPath = getYarnAgregationLogPath();
    for (HdfsUsers user : hdfsUsers) {
      // Remove logs
      dfso.rm(new Path(logPath + File.separator + user.getName()), true);

      // Change owner of history files
      List<Inode> historyInodes = inodeFacade.findHistoryFileByHdfsUser(user);
      for (Inode inode : historyInodes) {
        dfso.setOwner(new Path(inodeFacade.getPath(inode)),
          UserGroupInformation.getLoginUser().getUserName(), "hadoop");
      }

      Path certsHdfsDir = new Path(settings.getHdfsTmpCertDir() + File.separator + user.getName());
      if (dfso.exists(certsHdfsDir.toString())) {
        dfso.rm(certsHdfsDir, true);
      }
    }
  }

  private List<ApplicationReport> getYarnApplications(Set<String> hdfsUsers, YarnClient yarnClient)
    throws YarnException, IOException {
    return yarnClient.getApplications(null, hdfsUsers, null,
      EnumSet.of(
        YarnApplicationState.ACCEPTED, YarnApplicationState.NEW, YarnApplicationState.NEW_SAVING,
        YarnApplicationState.RUNNING, YarnApplicationState.SUBMITTED));
  }

  private void killYarnJobs(Project project) throws JobException {
    List<Jobs> running = jobFacade.getRunningJobs(project);
    if (running != null && !running.isEmpty()) {
      for (Jobs job : running) {
        //Get the appId of the running app
        executionController.stop(job);
      }
    }
  }

  private void waitForJobLogs(List<ApplicationReport> projectsApps, YarnClient client)
    throws YarnException, IOException, InterruptedException {
    for (ApplicationReport appReport : projectsApps) {
      FinalApplicationStatus finalState = appReport.getFinalApplicationStatus();
      while (finalState.equals(FinalApplicationStatus.UNDEFINED)) {
        client.killApplication(appReport.getApplicationId());
        appReport = client.getApplicationReport(appReport.getApplicationId());
        finalState = appReport.getFinalApplicationStatus();
      }
      YarnLogUtil.waitForLogAggregation(client, appReport.getApplicationId());
    }
  }

  /**
   * Safety-net method to delete certificate references from {@link CertificateMaterializer}
   * Individual services who request material should de-reference them during cleanup, but just
   * to be on the safe side force remove them too
   *
   * @param project Project to be deleted
   */
  private void removeCertificatesFromMaterializer(Project project) {
    for (ProjectTeam team : project.getProjectTeamCollection()) {
      certificateMaterializer.forceRemoveLocalMaterial(team.getUser().getUsername(), project.getName(), null, true);
      String remoteCertsDirectory = settings.getHdfsTmpCertDir() + Path.SEPARATOR +
        hdfsUsersController.getHdfsUserName(project, team.getUser());
      if (remoteCertsDirectory.equals(settings.getHdfsTmpCertDir())) {
        LOGGER.log(Level.WARNING, "Programming error! Tried to delete " + settings.getHdfsTmpCertDir()
          + " while deleting project " + project + " but this operation is not allowed.");
      } else {
        certificateMaterializer.forceRemoveRemoteMaterial(team.getUser().getUsername(), project.getName(),
          remoteCertsDirectory, false);
      }
    }
  }

  private static class InsecureHostnameVerifier implements HostnameVerifier {
    static InsecureHostnameVerifier INSTANCE = new InsecureHostnameVerifier();

    InsecureHostnameVerifier() {
    }

    @Override
    public boolean verify(String string, SSLSession ssls) {
      return true;
    }
  }

  public void cleanup(Project project, String sessionId) throws GenericException {
    cleanup(project, sessionId, false);
  }

  public void cleanup(Project project, String sessionId, boolean decreaseCreatedProj) throws GenericException {
    cleanup(project, sessionId, null, decreaseCreatedProj);
  }

  public void cleanup(Project project, String sessionId,
    List<Future<?>> projectCreationFutures) throws GenericException {
    cleanup(project, sessionId, projectCreationFutures, true);
  }

  public void cleanup(Project project, String sessionId,
      List<Future<?>> projectCreationFutures, boolean decreaseCreatedProj) throws GenericException {
    cleanup(project, sessionId, projectCreationFutures, true, null);
  }

  public void cleanup(Project project, String sessionId,
      List<Future<?>> projectCreationFutures, boolean decreaseCreatedProj, Users owner)
    throws GenericException {

    if (project == null) {
      return;
    }
    int nbTry = 0;
    while (nbTry < 2) {
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
        List<ApplicationReport> projectsApps = getYarnApplications(hdfsUsers, client);

        // try and close all the jupyter jobs
        removeJupyter(project);

        removeAnacondaEnv(project);

        //kill jobs
        killYarnJobs(project);

        waitForJobLogs(projectsApps, client);

        List<HdfsUsers> usersToClean = getUsersToClean(project);
        List<HdfsGroups> groupsToClean = getGroupsToClean(project);
        removeProjectInt(project, usersToClean, groupsToClean, projectCreationFutures, decreaseCreatedProj, owner);
        removeCertificatesFromMaterializer(project);
        //Delete online featurestore database
        onlineFeaturestoreController.removeOnlineFeatureStore(project);

        break;
      } catch (Exception ex) {
        nbTry++;
        if (nbTry < 2) {
          try {
            Thread.sleep(nbTry * 1000);
          } catch (InterruptedException ex1) {
            LOGGER.log(Level.SEVERE, null, ex1);
          }
        } else {
          throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, null, ex.getMessage(), ex);
        }
      } finally {
        ycs.closeYarnClient(yarnClientWrapper);
      }
    }
  }

  private void removeProjectInt(Project project, List<HdfsUsers> usersToClean,
      List<HdfsGroups> groupsToClean, List<Future<?>> projectCreationFutures,
      boolean decreaseCreatedProj, Users owner)
    throws IOException, InterruptedException, ExecutionException,
    HopsSecurityException, ServiceException, ProjectException, GenericException, TensorBoardException,
    FeaturestoreException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();

      // Run custom handler for project deletion
      for (ProjectHandler projectHandler : projectHandlers) {
        try {
          projectHandler.preDelete(project);
        } catch (Exception e) {
          throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HANDLER_PREDELETE_ERROR, Level.SEVERE,
            "project: " + project.getName() + ", handler: " + projectHandler.getClassName(), e.getMessage(), e);
        }
      }

      datasetController.unsetMetaEnabledForAllDatasets(dfso, project);

      //log removal to notify elastic search
      logProject(project, OperationType.Delete);
      //change the owner and group of the project folder to hdfs super user
      Path location = new Path(Utils.getProjectPath(project.getName()));
      changeOwnershipToSuperuser(location, dfso);

      Path dumy = new Path("/tmp/" + project.getName());
      changeOwnershipToSuperuser(dumy, dfso);

      //remove kafka topics
      removeKafkaTopics(project);

      // remove user certificate from local node
      // (they will be removed from db when the project folder is deleted)
      // projectCreationFutures will be null during project deletion.
      if (projectCreationFutures != null) {
        for (Future f : projectCreationFutures) {
          if (f != null) {
            try {
              f.get();
            } catch (ExecutionException ex) {
              LOGGER.log(Level.SEVERE, "Error while waiting for ProjectCreationFutures to finish for Project "
                  + project.getName(), ex);
            }
          }
        }
      }

      try {
        certificatesController.revokeProjectCertificates(project, owner);
      } catch (HopsSecurityException ex) {
        if (ex.getErrorCode() != RESTCodes.SecurityErrorCode.CERTIFICATE_NOT_FOUND) {
          LOGGER.log(Level.SEVERE, "Could not delete certificates during cleanup for project " + project.getName()
            + ". Manual cleanup is needed!!!", ex);
          throw ex;
        }
      } catch (IOException | GenericException ex) {
        LOGGER.log(Level.SEVERE, "Could not delete certificates during cleanup for project " + project.getName()
          + ". Manual cleanup is needed!!!", ex);
        throw ex;
      }

      //remove running tensorboards
      removeTensorBoard(project);

      //remove jupyter
      removeJupyter(project);

      removeProjectRelatedFiles(usersToClean, dfso);

      //remove quota
      removeQuotas(project);

      //change owner for files in shared datasets
      fixSharedDatasets(project, dfso);

      //Delete online featurestore database
      onlineFeaturestoreController.removeOnlineFeatureStore(project);

      //Delete Hive database - will automatically cleanup all the Hive's metadata
      hiveController.dropDatabases(project, dfso, false);

      //Delete elasticsearch template for this project
      removeElasticsearch(project);

      //delete project group and users
      removeGroupAndUsers(groupsToClean, usersToClean);

      //remove dumy Inode
      dfso.rm(dumy, true);

      // Remove servings
      try {
        servingController.deleteServings(project);
      } catch (ServingException e) {
        throw new IOException(e);
      }

      // Remove Airflow DAGs from local filesystem,
      // JWT renewal monitors and materialized X.509
      airflowManager.onProjectRemoval(project);

      //remove folder
      removeProjectFolder(project.getName(), dfso);

      if (decreaseCreatedProj) {
        usersController.decrementNumProjectsCreated(project.getOwner().getUid());
      }

      usersController.decrementNumActiveProjects(project.getOwner().getUid());

      // Run custom handler for project deletion
      for (ProjectHandler projectHandler : projectHandlers) {
        try {
          projectHandler.postDelete(project);
        } catch (Exception e) {
          throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HANDLER_POSTDELETE_ERROR, Level.SEVERE,
            "project: " + project.getName() + ", handler: " + projectHandler.getClassName(), e.getMessage(), e);
        }
      }

      LOGGER.log(Level.INFO, "{0} - project removed.", project.getName());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  private void changeOwnershipToSuperuser(Path path, DistributedFileSystemOps dfso) throws IOException {
    if (dfso.exists(path.toString())) {
      dfso.setOwner(path, settings.getHdfsSuperUser(), settings.
        getHdfsSuperUser());
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
    return hdfsUsersController.getAllProjectHdfsUsers(project.getName());
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private List<HdfsGroups> getGroupsToClean(Project project) {

    return hdfsUsersController.getAllProjectHdfsGroups(project.getName());

  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void removeKafkaTopics(Project project) throws ServiceException, InterruptedException {
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
    hdfsUsersController.deleteGroups(groups);
    hdfsUsersController.deleteUsers(users);
  }

  private void removeProjectFolder(String projectName, DistributedFileSystemOps dfso) throws IOException {
    final Path location = new Path(Utils.getProjectPath(projectName));
    dfso.rm(location, true);
  }

  /**
   * Adds new team members to a project(project) - bulk persist if team role not
   * specified or not in (Data owner or Data
   * scientist)defaults to Data scientist
   *
   * @param project
   * @param owner
   * @param projectTeams
   * @return a list of user names that could not be added to the project team
   * list.
   * @throws KafkaException
   * @throws ProjectException
   * @throws UserException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<String> addMembers(Project project, Users owner, List<ProjectTeam> projectTeams) throws KafkaException,
    ProjectException, UserException, FeaturestoreException {
    List<String> failedList = new ArrayList<>();
    if (projectTeams == null) {
      return failedList;
    }

    Users newMember;
    for (ProjectTeam projectTeam : projectTeams) {
      try {
        if (!projectTeam.getProjectTeamPK().getTeamMember().equals(owner.getEmail())) {

          //if the role is not properly set set it to the default role (Data Scientist).
          if (projectTeam.getTeamRole() == null || (!projectTeam.getTeamRole().
            equals(ProjectRoleTypes.DATA_SCIENTIST.getRole())
            && !projectTeam.
            getTeamRole().equals(ProjectRoleTypes.DATA_OWNER.getRole()))) {
            projectTeam.setTeamRole(ProjectRoleTypes.DATA_SCIENTIST.getRole());
          }

          projectTeam.setTimestamp(new Date());
          newMember = userFacade.findByEmail(projectTeam.getProjectTeamPK().
            getTeamMember());
          if (newMember != null && !projectTeamFacade.isUserMemberOfProject(
            project, newMember)) {
            //this makes sure that the member is added to the project sent as the
            //first param b/c the securty check was made on the parameter sent as path.
            projectTeam.getProjectTeamPK().setProjectId(project.getId());
            projectTeam.setProject(project);
            projectTeam.setUser(newMember);
            project.getProjectTeamCollection().add(projectTeam);
            projectFacade.update(project);
            hdfsUsersController.addNewProjectMember(project, projectTeam);

            //Add user to kafka topics ACLs by default
            if (projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.KAFKA)) {
              kafkaController.addProjectMemberToTopics(project, newMember.getEmail());
            }
  
            //if online-featurestore service is enabled in the project, give new member access to it
            if (projectServiceFacade.isServiceEnabledForProject(project, ProjectServiceEnum.FEATURESTORE) &&
                settings.isOnlineFeaturestore()) {
              onlineFeaturestoreController.createDatabaseUser(projectTeam.getUser(), project);
              FeaturestoreDTO featurestoreDTO = featurestoreController.getFeaturestoreForProjectWithName(project,
                  featurestoreController.getOfflineFeaturestoreDbName(project));
              onlineFeaturestoreController.updateUserOnlineFeatureStoreDB(project, projectTeam.getUser(),
                  featurestoreController.getFeaturestoreWithId(featurestoreDTO.getFeaturestoreId()));
            }

            // TODO: This should now be a REST call
            Future<CertificatesController.CertsResult> certsResultFuture = null;
            try {
              certsResultFuture = certificatesController.generateCertificates(project, newMember);
              certsResultFuture.get();
            } catch (Exception ex) {
              try {
                if (certsResultFuture != null) {
                  certsResultFuture.get();
                }
                certificatesController.revokeUserSpecificCertificates(project, newMember);
              } catch (IOException | InterruptedException | ExecutionException |
                HopsSecurityException | GenericException e) {
                String failedUser = project.getName() + HdfsUsersController.USER_NAME_DELIMITER + newMember.
                  getUsername();
                LOGGER.log(Level.SEVERE,
                  "Could not delete user certificates for user " + failedUser + ". Manual cleanup is needed!!! ", e);
              }
              LOGGER.log(Level.SEVERE, "error while creating certificates, jupyter kernel: " + ex.getMessage(), ex);
              projectTeamFacade.removeProjectTeam(project, newMember);
              hdfsUsersController.removeProjectMember(newMember, project);
              throw new EJBException("Could not create certificates for user");
            }

            LOGGER.log(Level.FINE, "{0} - member added to project : {1}.",
              new Object[]{newMember.getEmail(),
                project.getName()});

            logActivity(ActivityFacade.NEW_MEMBER + projectTeam.getProjectTeamPK().getTeamMember(), owner,
              project, ActivityFlag.MEMBER);
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
   * @param projectID of the project
   * @return project DTO that contains team members and services
   */
  public ProjectDTO getProjectByID(Integer projectID) throws ProjectException {
    Project project = projectFacade.find(projectID);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectID);
    }
    //find the project as an inode from hops database
    Inode inode = inodes.getInodeAtPath(Utils.getProjectPath(project.getName()));

    List<ProjectTeam> projectTeam = projectTeamFacade.findMembersByProject(
      project);
    List<ProjectServiceEnum> projectServices = projectServicesFacade.
      findEnabledServicesForProject(project);
    List<String> services = new ArrayList<>();
    for (ProjectServiceEnum s : projectServices) {
      services.add(s.toString());
    }

    QuotasDTO quotas = getQuotasInternal(project);

    return new ProjectDTO(project, inode.getId(), services, projectTeam, quotas,
      settings.getHopsExamplesSparkFilename());
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   *
   * @param name
   * @return project DTO that contains team members and services
   */
  public ProjectDTO getProjectByName(String name) throws ProjectException {
    //find the project entity from hopsworks database
    Project project = projectFacade.findByName(name);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "project: " + name);
    }
    //find the project as an inode from hops database
    Inode inode = inodes.getInodeAtPath(Utils.getProjectPath(name));

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
    return new ProjectDTO(project, inode.getId(), services, projectTeam, kids);
  }

  public void setProjectOwnerAndQuotas(Project project, long diskspaceQuotaInMB,
    DistributedFileSystemOps dfso, Users user)
    throws IOException {
    this.yarnProjectsQuotaFacade.persistYarnProjectsQuota(
      new YarnProjectsQuota(project.getName(), settings.getYarnDefaultQuota(), 0));
    this.yarnProjectsQuotaFacade.flushEm();
    setHdfsSpaceQuotasInMBs(project, diskspaceQuotaInMB, null, null, dfso);
    projectFacade.setTimestampQuotaUpdate(project, new Date());
    //Add the activity information
    logActivity(ActivityFacade.NEW_PROJECT + project.getName(), user, project, ActivityFlag.PROJECT);
    //update role information in project
    addProjectOwner(project, user);
    LOGGER.log(Level.FINE, "{0} - project created successfully.", project.
      getName());
  }

  public void setHdfsSpaceQuotasInMBs(Project project, Long diskspaceQuotaInMB,
    Long hiveDbSpaceQuotaInMb, Long featurestoreDbSpaceQuotaInMb,
    DistributedFileSystemOps dfso)
    throws IOException {

    dfso.setHdfsSpaceQuotaInMBs(new Path(Utils.getProjectPath(project.getName())), diskspaceQuotaInMB);

    if (hiveDbSpaceQuotaInMb != null && projectServicesFacade.isServiceEnabledForProject(project,
      ProjectServiceEnum.HIVE)) {
      dfso.setHdfsSpaceQuotaInMBs(hiveController.getDbPath(project.getName()),
        hiveDbSpaceQuotaInMb);
    }

    if (featurestoreDbSpaceQuotaInMb != null && projectServicesFacade.isServiceEnabledForProject(project,
      ProjectServiceEnum.FEATURESTORE)) {
      dfso.setHdfsSpaceQuotaInMBs(hiveController.getDbPath(
        featurestoreController.getOfflineFeaturestoreDbName(project)),
        featurestoreDbSpaceQuotaInMb);
    }
  }

  public void setPaymentType(Project project, PaymentType paymentType) {
    project.setPaymentType(paymentType);
    this.projectFacade.mergeProject(project);
    this.projectFacade.flushEm();
  }

  /**
   * @param projectId
   * @return
   */
  public QuotasDTO getQuotas(Integer projectId) {
    Project project = projectFacade.find(projectId);
    return getQuotasInternal(project);
  }

  public QuotasDTO getQuotasInternal(Project project) {
    Long hdfsQuota = -1L, hdfsUsage = -1L, hdfsNsQuota = -1L, hdfsNsCount = -1L,
      dbhdfsQuota = -1L, dbhdfsUsage = -1L, dbhdfsNsQuota = -1L, dbhdfsNsCount = -1L,
      fshdfsQuota = -1L, fshdfsUsage = -1L, fshdfsNsQuota = -1L, fshdfsNsCount = -1L;
    Integer kafkaQuota = project.getKafkaMaxNumTopics();
    Float yarnRemainingQuota = 0f, yarnTotalQuota = 0f;

    // Yarn Quota
    YarnProjectsQuota yarnQuota = yarnProjectsQuotaFacade.
      findByProjectName(project.getName());
    if (yarnQuota == null) {
      LOGGER.log(Level.SEVERE, "Cannot find YARN quota information for project: " + project.getName());
    } else {
      yarnRemainingQuota = yarnQuota.getQuotaRemaining();
      yarnTotalQuota = yarnQuota.getTotal();
    }

    // HDFS project directory quota
    HdfsDirectoryWithQuotaFeature projectInodeAttrs = hdfsDirectoryWithQuotaFeatureFacade.
      getByInodeId(project.getInode().getId());
    if (projectInodeAttrs == null) {
      LOGGER.log(Level.SEVERE, "Cannot find HDFS quota information for project: " + project.getName());
    } else {
      hdfsQuota = projectInodeAttrs.getSsquota().longValue();
      hdfsUsage = projectInodeAttrs.getStorageSpace().longValue();
      hdfsNsQuota = projectInodeAttrs.getNsquota().longValue();
      hdfsNsCount = projectInodeAttrs.getNscount().longValue();
    }

    // If the Hive service is enabled, get the quota information for the db directory
    if (projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.HIVE)) {
      List<Dataset> datasets = (List<Dataset>) project.getDatasetCollection();
      for (Dataset ds : datasets) {
        if (ds.getType() == DatasetType.HIVEDB) {
          HdfsDirectoryWithQuotaFeature dbInodeAttrs = hdfsDirectoryWithQuotaFeatureFacade
            .getByInodeId(ds.getInodeId());
          if (dbInodeAttrs == null) {
            LOGGER.log(Level.SEVERE, "Cannot find HiveDB quota information for project: " + project.getName());
          } else {
            dbhdfsQuota = dbInodeAttrs.getSsquota().longValue();
            dbhdfsUsage = dbInodeAttrs.getStorageSpace().longValue();
            dbhdfsNsQuota = dbInodeAttrs.getNsquota().longValue();
            dbhdfsNsCount = dbInodeAttrs.getNscount().longValue();
          }
        }
      }
    }

    // If the Featurestore service is enabled, get the quota information for the featurestore db directory
    if (projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.FEATURESTORE)) {
      List<Dataset> datasets = (List<Dataset>) project.getDatasetCollection();
      for (Dataset ds : datasets) {
        if (ds.getType() == DatasetType.FEATURESTORE) {
          HdfsDirectoryWithQuotaFeature dbInodeAttrs = hdfsDirectoryWithQuotaFeatureFacade
            .getByInodeId(ds.getInodeId());
          if (dbInodeAttrs == null) {
            LOGGER.log(Level.SEVERE, "Cannot find FeaturestoreDb quota information for project: " + project.getName());
          } else {
            fshdfsQuota = dbInodeAttrs.getSsquota().longValue();
            fshdfsUsage = dbInodeAttrs.getStorageSpace().longValue();
            fshdfsNsQuota = dbInodeAttrs.getNsquota().longValue();
            fshdfsNsCount = dbInodeAttrs.getNscount().longValue();
          }
        }
      }
    }

    return new QuotasDTO(yarnRemainingQuota, yarnTotalQuota, hdfsQuota, hdfsUsage, hdfsNsQuota, hdfsNsCount,
      dbhdfsQuota, dbhdfsUsage, dbhdfsNsQuota, dbhdfsNsCount, fshdfsQuota, fshdfsUsage, fshdfsNsQuota,
      fshdfsNsCount, kafkaQuota);
  }

  public void removeMemberFromTeam(Project project, Users user, String toRemoveEmail) throws UserException,
    ProjectException, ServiceException, IOException, GenericException, JobException, HopsSecurityException,
    TensorBoardException {
    Users userToBeRemoved = userFacade.findByEmail(toRemoveEmail);
    if (userToBeRemoved == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "user: " + user.getEmail());
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project, userToBeRemoved);
    if (projectTeam == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.TEAM_MEMBER_NOT_FOUND, Level.FINE,
        "project: " + project + ", user: " + user.getEmail());
    }
    projectTeamFacade.removeProjectTeam(project, userToBeRemoved);
    String hdfsUser = hdfsUsersController.getHdfsUserName(project, userToBeRemoved);

    YarnClientWrapper yarnClientWrapper = ycs.getYarnClientSuper(settings.getConfiguration());
    YarnClient client = yarnClientWrapper.getYarnClient();
    try {
      Set<String> hdfsUsers = new HashSet<>();
      hdfsUsers.add(hdfsUser);
      List<ApplicationReport> projectsApps = client.getApplications(null, hdfsUsers, null, EnumSet.of(
        YarnApplicationState.ACCEPTED, YarnApplicationState.NEW, YarnApplicationState.NEW_SAVING,
        YarnApplicationState.RUNNING, YarnApplicationState.SUBMITTED));
      //kill jupyter for this user

      JupyterProject jupyterProject = jupyterFacade.findByUser(hdfsUser);
      if (jupyterProject != null) {
        jupyterController.shutdown(project, hdfsUser, user, jupyterProject.getSecret(), jupyterProject.getPid(),
          jupyterProject.getPort());
      }

      //kill running TB if any
      tensorBoardController.cleanup(project, user);

      //kill all jobs run by this user.
      //kill jobs
      List<Jobs> running = jobFacade.getRunningJobs(project, hdfsUser);
      if (running != null && !running.isEmpty()) {
        for (Jobs job : running) {
          executionController.stop(job);
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
        YarnLogUtil.waitForLogAggregation(client, appReport.getApplicationId());
      }
    } catch (YarnException | IOException | InterruptedException e) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.KILL_MEMBER_JOBS, Level.SEVERE,
        "project: " + project + ", user: " + userToBeRemoved, e.getMessage(), e);
    } finally {
      ycs.closeYarnClient(yarnClientWrapper);
    }

    kafkaController.removeProjectMemberFromTopics(project, userToBeRemoved);

    logActivity(ActivityFacade.REMOVED_MEMBER + userToBeRemoved.getEmail(), user, project, ActivityFlag.
      MEMBER);

    certificateMaterializer.forceRemoveLocalMaterial(userToBeRemoved.getUsername(), project.getName(), null, false);
    try {
      certificatesController.revokeUserSpecificCertificates(project, userToBeRemoved);
    } catch (HopsSecurityException ex) {
      if (ex.getErrorCode() != RESTCodes.SecurityErrorCode.CERTIFICATE_NOT_FOUND) {
        LOGGER.log(Level.SEVERE, "Could not delete certificates when removing member "
            + userToBeRemoved.getUsername() + " from project " + project.getName()
            + ". Manual cleanup is needed!!!", ex);
        throw ex;
      }
    } catch (IOException | GenericException ex) {
      LOGGER.log(Level.SEVERE, "Could not delete certificates when removing member "
          + userToBeRemoved.getUsername() + " from project " + project.getName()
          + ". Manual cleanup is needed!!!", ex);
      throw ex;
    }
  }

  /**
   * Updates the role of a member
   *
   * @param project
   * @param opsOwner
   * @param toUpdateEmail
   * @param newRole
   * @throws UserException
   * @throws ProjectException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateMemberRole(Project project, Users opsOwner, String toUpdateEmail, String newRole) throws
    UserException, ProjectException {
    Users projOwner = project.getOwner();
    Users user = userFacade.findByEmail(toUpdateEmail);
    if (projOwner.equals(user)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_OWNER_ROLE_NOT_ALLOWED, Level.FINE,
        "project: " + project.getName());
    }
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "user: " + toUpdateEmail);
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project, user);
    if (projectTeam == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.TEAM_MEMBER_NOT_FOUND, Level.FINE,
        "project: " + project.getName() + ", user: " + user.getUsername());
      //member not found
    }
    if (!projectTeam.getTeamRole().equals(newRole)) {
      projectTeam.setTeamRole(newRole);
      projectTeam.setTimestamp(new Date());
      projectTeamFacade.update(projectTeam);

      if (newRole.equals(AllowedRoles.DATA_OWNER)) {
        hdfsUsersController.addUserToProjectGroup(project, projectTeam);
      } else {
        hdfsUsersController.modifyProjectMembership(user, project);
      }

      logActivity(ActivityFacade.CHANGE_ROLE + toUpdateEmail, opsOwner, project, ActivityFlag.MEMBER);
    }

  }

  /**
   * Retrieves all the project teams that a user have a role
   *
   * @param email of the user
   * @return a list of project team
   */
  public List<ProjectTeam> findProjectByUser(String email) {
    Users user = userFacade.findByEmail(email);
    return projectTeamFacade.findActiveByMember(user);
  }

  /**
   * Retrieves all the project teams that a user have a role.
   *
   * @param email of the user
   * @param ignoreCase
   * @return a list of project names
   */
  public List<String> findProjectNamesByUser(String email, boolean ignoreCase) {
    Users user = userFacade.findByEmail(email);
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

  public List<String> findProjectNames() {
    List<Project> projects = projectFacade.findAll();
    List<String> projectNames = null;
    if (projects != null && !projects.isEmpty()) {
      projectNames = new ArrayList(projects.size());
      for (Project project : projects) {
        projectNames.add(project.getName());
      }
    }
    return projectNames;
  }

  /**
   * Retrieves all the project teams for a project
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
   * @param activityPerformed the description of the operation performed
   * @param performedBy the user that performed the operation
   * @param performedOn the project the operation was performed on.
   * @param flag
   */
  public void logActivity(String activityPerformed, Users performedBy, Project performedOn, ActivityFlag flag) {
    activityFacade.persistActivity(activityPerformed, performedOn, performedBy, flag);
  }

  public void addTourFilesToProject(String username, Project project,
    DistributedFileSystemOps dfso, DistributedFileSystemOps udfso,
    TourProjectType projectType) throws DatasetException, HopsSecurityException, ProjectException,
    JobException, GenericException, ServiceException {

    Users user = userFacade.findByEmail(username);
    if (null != projectType) {
      String projectPath = Utils.getProjectPath(project.getName());

      switch (projectType) {
        case SPARK:
          datasetController.createDataset(user, project, Settings.HOPS_TOUR_DATASET,
            "files for guide projects", -1, false, true, true, dfso);
          String exampleDir = settings.getSparkDir() + Settings.SPARK_EXAMPLES_DIR + "/";
          try {
            File dir = new File(exampleDir);
            File[] file = dir.listFiles((File dir1, String name) ->
              name.matches("spark-examples(.*).jar"));
            if (file.length == 0) {
              throw new IllegalStateException("No spark-examples*.jar was found in "
                + dir.getAbsolutePath());
            }
            if (file.length > 1) {
              LOGGER.log(Level.WARNING,
                "More than one spark-examples*.jar found in {0}.", dir.
                  getAbsolutePath());
            }
            String hdfsJarPath = projectPath + Settings.HOPS_TOUR_DATASET
              + "/spark-examples.jar";
            udfso.copyToHDFSFromLocal(false, file[0].getAbsolutePath(), hdfsJarPath);
            String datasetGroup = hdfsUsersController.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET);
            String userHdfsName = hdfsUsersController.getHdfsUserName(project, user);
            udfso.setPermission(new Path(hdfsJarPath), udfso.getParentPermission(new Path(hdfsJarPath)));
            udfso.setOwner(new Path(projectPath + Settings.HOPS_TOUR_DATASET + "/spark-examples.jar"),
              userHdfsName, datasetGroup);

          } catch (IOException ex) {
            throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_TOUR_FILES_ERROR, Level.SEVERE,
              "project: " + project.getName(), ex.getMessage(), ex);
          }
          break;
        case KAFKA:
          datasetController.createDataset(user, project, Settings.HOPS_TOUR_DATASET,
            "files for guide projects", -1, false, true, true, dfso);
          // Get the JAR from /user/<super user>
          String kafkaExampleSrc = "/user/" + settings.getSparkUser() + "/"
            + settings.getHopsExamplesSparkFilename();
          String kafkaExampleDst = projectPath + Settings.HOPS_TOUR_DATASET +
            "/" + settings.getHopsExamplesSparkFilename();
          try {
            udfso.copyInHdfs(new Path(kafkaExampleSrc), new Path(kafkaExampleDst));
            String datasetGroup = hdfsUsersController.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET);
            String userHdfsName = hdfsUsersController.getHdfsUserName(project, user);
            udfso.setPermission(new Path(kafkaExampleDst), udfso.getParentPermission(new Path(kafkaExampleDst)));
            udfso.setOwner(new Path(kafkaExampleDst), userHdfsName, datasetGroup);

          } catch (IOException ex) {
            throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_TOUR_FILES_ERROR, Level.SEVERE,
              "project: " + project.getName(), ex.getMessage(), ex);
          }
          break;
        case DEEP_LEARNING:
          datasetController.createDataset(user, project, Settings.HOPS_DL_TOUR_DATASET,
            "sample training data for notebooks", -1, false, true, true, dfso);
          String DLDataSrc = "/user/" + settings.getHdfsSuperUser() + "/" + Settings.HOPS_DEEP_LEARNING_TOUR_DATA
            + "/*";
          String DLDataDst = projectPath + Settings.HOPS_DL_TOUR_DATASET;
          String DLNotebooksSrc = "/user/" + settings.getHdfsSuperUser() + "/" +
            Settings.HOPS_DEEP_LEARNING_TOUR_NOTEBOOKS;
          String DLNotebooksDst = projectPath + Settings.HOPS_TOUR_DATASET_JUPYTER;
          try {
            udfso.copyInHdfs(new Path(DLDataSrc), new Path(DLDataDst));
            String datasetGroup = hdfsUsersController.getHdfsGroupName(project, Settings.HOPS_DL_TOUR_DATASET);
            String userHdfsName = hdfsUsersController.getHdfsUserName(project, user);
            Inode tourDs = inodes.getInodeAtPath(DLDataDst);
            datasetController.recChangeOwnershipAndPermission(new Path(DLDataDst),
              FsPermission.createImmutable(tourDs.getPermission()),
              userHdfsName, datasetGroup, dfso, udfso);
            udfso.copyInHdfs(new Path(DLNotebooksSrc + "/*"), new Path(DLNotebooksDst));
            datasetGroup = hdfsUsersController.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET_JUPYTER);
            Inode jupyterDS = inodes.getInodeAtPath(DLNotebooksDst);
            datasetController.recChangeOwnershipAndPermission(new Path(DLNotebooksDst),
              FsPermission.createImmutable(jupyterDS.getPermission()),
              userHdfsName, datasetGroup, dfso, udfso);
          } catch (IOException ex) {
            throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_TOUR_FILES_ERROR, Level.SEVERE,
              "project: " + project.getName(), ex.getMessage(), ex);
          }
          break;
        case FEATURESTORE:
          datasetController.createDataset(user, project, Settings.HOPS_TOUR_DATASET,
            "files for guide projects", -1, false, true, true, dfso);
          // Get the JAR from /user/<super user>
          String featurestoreExampleJarSrc = "/user/" + settings.getSparkUser() + "/"
            + settings.getHopsExamplesFeaturestoreTourFilename();
          String featurestoreExampleJarDst = projectPath
            + Settings.HOPS_TOUR_DATASET + "/" + settings.getHopsExamplesFeaturestoreTourFilename();
          // Get the sample data and notebooks from /user/<super user>/featurestore_demo/
          String featurestoreExampleDataSrc = "/user/" + settings.getHdfsSuperUser() + "/" +
            Settings.HOPS_FEATURESTORE_TOUR_DATA + "/data";
          String featurestoreExampleDataDst = projectPath + Settings.HOPS_TOUR_DATASET;

          try {
            //Move example .jar file to HDFS
            udfso.copyInHdfs(new Path(featurestoreExampleJarSrc), new Path(featurestoreExampleJarDst));
            String datasetGroup = hdfsUsersController.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET);
            String userHdfsName = hdfsUsersController.getHdfsUserName(project, user);
            udfso.setPermission(new Path(featurestoreExampleJarDst),
              udfso.getParentPermission(new Path(featurestoreExampleJarDst)));
            udfso.setOwner(new Path(featurestoreExampleJarDst), userHdfsName, datasetGroup);
            //Move example data and notebooks to HDFS
            udfso.copyInHdfs(new Path(featurestoreExampleDataSrc), new Path(featurestoreExampleDataDst));
            datasetGroup = hdfsUsersController.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET);
            userHdfsName = hdfsUsersController.getHdfsUserName(project, user);
            Inode featurestoreDataDst = inodes.getInodeAtPath(featurestoreExampleDataDst);
            datasetController.recChangeOwnershipAndPermission(new Path(featurestoreExampleDataDst),
              FsPermission.createImmutable(featurestoreDataDst.getPermission()),
              userHdfsName, datasetGroup, dfso, udfso);
            //Move example notebooks to Jupyter dataset
            String featurestoreExampleNotebooksSrc = "/user/" + settings.getHdfsSuperUser() + "/" +
              Settings.HOPS_FEATURESTORE_TOUR_DATA + "/notebooks";
            String featurestoreExampleNotebooksDst = projectPath + Settings.HOPS_TOUR_DATASET_JUPYTER;
            udfso.copyInHdfs(new Path(featurestoreExampleNotebooksSrc + "/*"),
              new Path(featurestoreExampleNotebooksDst));
            datasetGroup = hdfsUsersController.getHdfsGroupName(project, Settings.HOPS_TOUR_DATASET_JUPYTER);
            Inode featurestoreNotebooksDst = inodes.getInodeAtPath(featurestoreExampleNotebooksDst);
            datasetController.recChangeOwnershipAndPermission(new Path(featurestoreExampleNotebooksDst),
              FsPermission.createImmutable(featurestoreNotebooksDst.getPermission()),
              userHdfsName, datasetGroup, dfso, udfso);
          } catch (IOException ex) {
            throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_TOUR_FILES_ERROR, Level.SEVERE,
              "project: " + project.getName(), ex.getMessage(), ex);
          }
          SparkJobConfiguration sparkJobConfiguration = new SparkJobConfiguration();
          sparkJobConfiguration.setAmQueue("default");
          sparkJobConfiguration.setAmMemory(1024);
          sparkJobConfiguration.setAmVCores(1);
          sparkJobConfiguration.setAppPath("hdfs://" + featurestoreExampleJarDst);
          sparkJobConfiguration.setMainClass(Settings.HOPS_FEATURESTORE_TOUR_JOB_CLASS);
          sparkJobConfiguration.setExecutorInstances(1);
          sparkJobConfiguration.setExecutorCores(1);
          sparkJobConfiguration.setExecutorMemory(2024);
          sparkJobConfiguration.setExecutorGpus(0);
          sparkJobConfiguration.setDynamicAllocationEnabled(true);
          sparkJobConfiguration.setDynamicAllocationMinExecutors(1);
          sparkJobConfiguration.setDynamicAllocationMaxExecutors(3);
          sparkJobConfiguration.setDynamicAllocationInitialExecutors(1);
          sparkJobConfiguration.setArgs(Settings.HOPS_FEATURESTORE_TOUR_JOB_INPUT_PARAM + Settings.HOPS_TOUR_DATASET +
            "/data");
          sparkJobConfiguration.setAppName(Settings.HOPS_FEATURESTORE_TOUR_JOB_NAME);
          sparkJobConfiguration.setLocalResources(new LocalResourceDTO[0]);
          Jobs job = jobController.putJob(user, project, null, sparkJobConfiguration);
          activityFacade.persistActivity(ActivityFacade.CREATED_JOB + job.getName(), project, user,
            ActivityFlag.SERVICE);
          executionController.start(job, user);
          activityFacade.persistActivity(ActivityFacade.RAN_JOB + job.getName(), project, user,
            ActivityFlag.SERVICE);
          break;
        default:
          break;
      }
    }
  }

  public List<YarnPriceMultiplicator> getYarnMultiplicators() {
    List<YarnPriceMultiplicator> multiplicators = new ArrayList<>(yarnProjectsQuotaFacade.getMultiplicators());
    if (multiplicators.isEmpty()) {
      YarnPriceMultiplicator multiplicator = new YarnPriceMultiplicator();
      multiplicator.setMultiplicator(Settings.DEFAULT_YARN_MULTIPLICATOR);
      multiplicator.setId("-1");
      multiplicators.add(multiplicator);
    }
    return multiplicators;
  }

  public void logProject(Project project, OperationType type) {
    operationsLogFacade.persist(new OperationsLog(project, type));
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeAnacondaEnv(Project project) throws ServiceException {
    environmentController.removeEnvironment(project);
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeJupyter(Project project) throws ServiceException {
    jupyterController.removeJupyter(project);
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeTensorBoard(Project project) throws TensorBoardException {
    tensorBoardController.removeProject(project);
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void cloneAnacondaEnv(Project srcProj, Project destProj) throws ServiceException {
    environmentController.cloneProject(srcProj, destProj);
  }

  /**
   * Handles Kibana related indices and templates for projects.
   *
   * @param project project
   * @throws ProjectException ProjectException
   * @throws ServiceException ServiceException
   */
  public void addKibana(Project project) throws ProjectException, ServiceException {

    String projectName = project.getName().toLowerCase();

    elasticController.createIndexPattern(project, projectName + Settings.ELASTIC_LOGS_INDEX_PATTERN);
    elasticController.createIndexPattern(project,
      project.getName().toLowerCase() + Settings.ELASTIC_BEAMJOBSERVER_INDEX_PATTERN);
    elasticController.createIndexPattern(project,
      project.getName().toLowerCase() + Settings.ELASTIC_BEAMSDKWORKER_INDEX_PATTERN);
    // Create index and index-pattern for experiment service
    String indexName = projectName + "_" + Settings.ELASTIC_EXPERIMENTS_INDEX;
    if (!elasticController.indexExists(indexName)) {
      elasticController.createIndex(indexName);
    }

    elasticController.createIndexPattern(project, indexName);

    String savedSummarySearch =
      "{\"attributes\":{\"title\":\"Experiments summary\",\"description\":\"\",\"hits\":0,\"columns\"" +
        ":[\"_id\",\"user\",\"name\",\"start\",\"finished\",\"status\",\"module\",\"function\"" +
        ",\"hyperparameter\"" +
        ",\"metric\"],\"sort\":[\"start\"" +
        ",\"desc\"],\"version\":1,\"kibanaSavedObjectMeta\":{\"searchSourceJSON\":\"" +
        "{\\\"index\\\":\\\"" + indexName + "\\\",\\\"highlightAll\\\":true,\\\"version\\\":true" +
        ",\\\"query\\\":{\\\"language\\\":\\\"lucene\\\",\\\"query\\\":\\\"\\\"},\\\"filter\\\":" +
        "[]}\"}}}";

    Map<String, String> params = new HashMap<>();
    params.put("op", "POST");
    params.put("data", savedSummarySearch);
    JSONObject resp = elasticController.sendKibanaReq(params, "search", indexName + "_summary-search", true);

    if (!(resp.has("updated_at") || (resp.has("statusCode") && resp.get("statusCode").toString().equals("409")))) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_KIBANA_CREATE_SEARCH_ERROR, Level.SEVERE,
        "project: " + projectName + ", resp: " + resp.toString(2));
    }

    String savedSummaryDashboard =
      "{\"attributes\":{\"title\":\"Experiments summary dashboard\",\"hits\":0,\"description\":\"" +
        "A summary of all experiments run in this project\",\"panelsJSON\":\"[{\\\"gridData\\\"" +
        ":{\\\"h\\\":9,\\\"i\\\":\\\"1\\\",\\\"w\\\":12,\\\"x\\\":0,\\\"y\\\":0},\\\"id\\\"" +
        ":\\\"" + indexName + "_summary-search" + "\\\",\\\"panelIndex\\\":\\\"1\\\"" +
        ",\\\"type\\\":\\\"search\\\"" +
        ",\\\"version\\\":\\\"" + settings.getKibanaVersion() +
        "\\\"}]\",\"optionsJSON\":\"{\\\"darkTheme\\\":false" +
        ",\\\"hidePanelTitles\\\":false,\\\"useMargins\\\":true}\",\"version\":1,\"timeRestore\":" +
        "false" +
        ",\"kibanaSavedObjectMeta\":{\"searchSourceJSON\":\"{\\\"query\\\":{\\\"language\\\"" +
        ":\\\"lucene\\\",\\\"query\\\":\\\"\\\"},\\\"filter\\\":[],\\\"highlightAll\\\":" +
        "true,\\\"version\\\":true}\"}}}";
    params.clear();
    params.put("op", "POST");
    params.put("data", savedSummaryDashboard);
    resp = elasticController.sendKibanaReq(params, "dashboard", indexName + "_summary-dashboard", true);

    if (!(resp.has("updated_at") || (resp.has("statusCode") && resp.get("statusCode").toString().equals("409")))) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_KIBANA_CREATE_DASHBOARD_ERROR, Level.SEVERE,
        "project: " + projectName + ", resp: " + resp.toString(2));
    }
  }

  public void removeElasticsearch(Project project) throws ServiceException {
    Map<String, String> params = new HashMap<>();
    params.put("op", "DELETE");

    List<ProjectServiceEnum> projectServices = projectServicesFacade.
      findEnabledServicesForProject(project);

    String projectName = project.getName().toLowerCase();

    if (projectServices.contains(ProjectServiceEnum.JOBS)
      || projectServices.contains(ProjectServiceEnum.JUPYTER)
      || projectServices.contains(ProjectServiceEnum.SERVING)) {
      elasticController.deleteProjectIndices(project);
    }

    if (projectServices.contains(ProjectServiceEnum.JOBS) || projectServices.contains(ProjectServiceEnum.JUPYTER)) {
      //1. Delete visualizations, saved searches, dashboards
      List<String> projectNames = new ArrayList<>();
      projectNames.add(project.getName());
      LOGGER.log(Level.FINE, "removeElasticsearch-1:{0}", projectNames);
      elasticController.deleteProjectSavedObjects(projectNames);

      //2. Delete logs Kibana Index
      JSONObject resp = elasticController.sendKibanaReq(params, "index-pattern", projectName + "_logs-*");
      LOGGER.log(Level.FINE, resp.toString(4));
      resp = elasticController.sendKibanaReq(params, "index-pattern",
        projectName + Settings.ELASTIC_KAGENT_INDEX_PATTERN);
      LOGGER.log(Level.FINE, resp.toString(4));
      LOGGER.log(Level.FINE, "removeElasticsearch-2:{0}", projectName);

      //3. Delete beam job service and sdk worker Kibana indices
      String beamjobserviceIndex = projectName + Settings.ELASTIC_BEAMJOBSERVER_INDEX_PATTERN;
      String beamsdkharnessIndex = projectName + Settings.ELASTIC_BEAMSDKWORKER_INDEX_PATTERN;
      resp = elasticController.sendKibanaReq(params, "index-pattern", beamjobserviceIndex, false);
      LOGGER.log(Level.FINE, "Deleting beamjobserviceIndex resp:" + resp.toString(4));
      resp = elasticController.sendKibanaReq(params, "index-pattern", beamsdkharnessIndex, false);
      LOGGER.log(Level.FINE, "Deleting beamsdkharnessIndex resp:" + resp.toString(4));
      LOGGER.log(Level.FINE, "removeElasticsearch-3:{0}", projectName);
    }

    if (projectServices.contains(ProjectServiceEnum.SERVING)) {
      JSONObject resp = elasticController.sendKibanaReq(params, "index-pattern",
        projectName + Settings.ELASTIC_SERVING_INDEX_PATTERN);
      LOGGER.log(Level.FINE, resp.toString(4));
    }
  }

  private AccessCredentialsDTO getAccessCredentials(Project project, Users user)
        throws IOException, CryptoPasswordNotFoundException {
    //Read certs from database and stream them out
    certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
    CertificateMaterializer.CryptoMaterial material = certificateMaterializer.getUserMaterial(user.getUsername(),
      project.getName());
    String keyStore = org.apache.commons.net.util.Base64.encodeBase64String(material.getKeyStore().array());
    String trustStore = org.apache.commons.net.util.Base64.encodeBase64String(material.getTrustStore().array());
    String certPwd = new String(material.getPassword());
    return new AccessCredentialsDTO("jks", keyStore, trustStore, certPwd);
  }

  public CertsDTO downloadCert(Integer projectId, Users user) throws ProjectException, DatasetException {
    Project project = findProjectById(projectId);
    try {
      AccessCredentialsDTO accessCredentials = getAccessCredentials(project, user);
      //Pop-up a message from admin
      messageController.send(user, userFacade.findByEmail(settings.getAdminEmail()), "Certificate Info", "",
          "An email was sent with the password for your project's certificates. If an email does not arrive shortly, "
          + "please check spam first and then contact the administrator.", "");
      emailBean.sendEmail(user.getEmail(), Message.RecipientType.TO, "Hopsworks certificate information",
          "The password for keystore and truststore is:" + accessCredentials.getPassword());
      return new CertsDTO("jks", accessCredentials.getkStore(), accessCredentials.gettStore());
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new DatasetException(RESTCodes.DatasetErrorCode.DOWNLOAD_ERROR, Level.SEVERE, "projectId: " + projectId,
          ex.getMessage(), ex);
    } finally {
      certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
    }
  }

  public AccessCredentialsDTO credentials(Integer projectId, Users user) throws ProjectException, DatasetException {
    Project project = findProjectById(projectId);
    try {
      return getAccessCredentials(project, user);
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      throw new DatasetException(RESTCodes.DatasetErrorCode.DOWNLOAD_ERROR, Level.SEVERE, "projectId: " + projectId,
        ex.getMessage(), ex);
    } finally {
      certificateMaterializer.removeCertificatesLocal(user.getUsername(), project.getName());
    }
  }

  /**
   * Helper class to log force cleanup operations
   *
   * @see ProjectController#forceCleanup(String, String, String)
   */
  private class CleanupLogger {

    private final String projectName;
    private final StringBuilder successLog;
    private final StringBuilder errorLog;

    private CleanupLogger(String projectName) {
      this.projectName = projectName;
      successLog = new StringBuilder();
      errorLog = new StringBuilder();
    }

    private void logError(String message) {
      log(errorLog, "*** ERROR ***", message);
    }

    private void logSuccess(String message) {
      log(successLog, "*** SUCCESS ***", message);
    }

    private void log(StringBuilder log, String summary, String message) {
      LocalDateTime now = DateUtils.getNow();
      log.append("<").append(now.format(DateTimeFormatter.ISO_DATE_TIME)).append(">")
          .append(summary)
          .append(message)
          .append(" *").append(projectName).append("*")
          .append("\n");
    }

    private StringBuilder getSuccessLog() {
      return successLog;
    }

    private StringBuilder getErrorLog() {
      return errorLog;
    }
  }

  /**
   * For HOPSFS quotas, both the namespace and the space quotas should be not null
   * at the same time.
   *
   * @param newProjectState
   * @param quotas
   * @throws IOException
   */
  public void adminProjectUpdate(Project newProjectState, QuotasDTO quotas) throws ProjectException {
    Project currentProject = projectFacade.findByName(newProjectState.getName());
    if (currentProject == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE);
    }

    // Set (un)archived status only if changed
    if (newProjectState.getArchived() != null && (currentProject.getArchived() != newProjectState.getArchived())) {
      if (newProjectState.getArchived()) {
        projectFacade.archiveProject(currentProject);
      } else {
        projectFacade.unarchiveProject(currentProject);
      }
    }

    // Set payment type information
    if (newProjectState.getPaymentType() != null
      && (newProjectState.getPaymentType() != currentProject.getPaymentType())) {
      setPaymentType(currentProject, newProjectState.getPaymentType());
    }

    // Set the quotas information
    if (quotas != null) {
      QuotasDTO currentQuotas = getQuotasInternal(currentProject);

      DistributedFileSystemOps dfso = dfs.getDfsOps();
      boolean quotaChanged = false;
      try {
        // If Hdfs quotas has changed, persist the changes in the database.
        if (quotas.getHdfsQuotaInBytes() != null && quotas.getHdfsNsQuota() != null && (!quotas.getHdfsQuotaInBytes().
          equals(currentQuotas.getHdfsQuotaInBytes()) || !quotas.getHdfsNsQuota().equals(currentQuotas.
          getHdfsNsQuota()))) {

          dfso.setHdfsQuotaBytes(new Path(Utils.getProjectPath(currentProject.getName())),
            quotas.getHdfsNsQuota(), quotas.getHdfsQuotaInBytes());
          quotaChanged = true;

        }

        // If Hive quota has changed and the Hive service is enabled, persist the changes in the database.
        if (quotas.getHiveHdfsQuotaInBytes() != null && quotas.getHiveHdfsNsQuota() != null && projectServicesFacade.
          isServiceEnabledForProject(currentProject, ProjectServiceEnum.HIVE) && (!quotas.getHiveHdfsQuotaInBytes().
          equals(currentQuotas.getHiveHdfsQuotaInBytes()) || !quotas.getHiveHdfsNsQuota().equals(currentQuotas.
          getHiveHdfsNsQuota()))) {

          dfso.setHdfsQuotaBytes(hiveController.getDbPath(currentProject.getName()),
            quotas.getHiveHdfsNsQuota(), quotas.getHiveHdfsQuotaInBytes());
          quotaChanged = true;
        }

        // If Featurestore quota has changed and the Featurestore service is enabled, persist the changes in the db.
        if (quotas.getFeaturestoreHdfsQuotaInBytes() != null && quotas.getFeaturestoreHdfsNsQuota() != null
          && projectServicesFacade.isServiceEnabledForProject(currentProject, ProjectServiceEnum.FEATURESTORE) &&
          (!quotas.getFeaturestoreHdfsQuotaInBytes().equals(currentQuotas.getFeaturestoreHdfsQuotaInBytes()) ||
            !quotas.getFeaturestoreHdfsNsQuota().equals(currentQuotas.getFeaturestoreHdfsNsQuota()))) {

          dfso.setHdfsQuotaBytes(hiveController.getDbPath(
            featurestoreController.getOfflineFeaturestoreDbName(newProjectState))
            , quotas.getFeaturestoreHdfsNsQuota(), quotas.getFeaturestoreHdfsQuotaInBytes());
          quotaChanged = true;
        }
      } catch (IOException e) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.QUOTA_ERROR,
          Level.SEVERE, "project: " + currentProject.getName(), e.getMessage(), e);
      } finally {
        if (dfso != null) {
          dfso.close();
        }
      }

      // If the yarn quota has changed, persist the change in the database
      if (quotas.getYarnQuotaInSecs() != null &&
        !quotas.getYarnQuotaInSecs().equals(currentQuotas.getYarnQuotaInSecs())) {
        yarnProjectsQuotaFacade.changeYarnQuota(currentProject.getName(), quotas.getYarnQuotaInSecs());
        quotaChanged = true;
      }
      if (quotas.getKafkaMaxNumTopics() != null) {
        projectFacade.changeKafkaQuota(currentProject, quotas.getKafkaMaxNumTopics());
        quotaChanged = true;
      }

      // Register time of last quota change in the project entry
      if (quotaChanged) {
        projectFacade.setTimestampQuotaUpdate(currentProject, new Date());
      }
    }
  }
}
