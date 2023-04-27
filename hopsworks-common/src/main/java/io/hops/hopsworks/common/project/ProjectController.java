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

import com.logicalclocks.servicediscoverclient.exceptions.ServiceDiscoveryException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.common.airflow.AirflowManager;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.constants.auth.AllowedRoles;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobs.description.JobFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnProjectsQuotaFacade;
import io.hops.hopsworks.common.dao.jupyter.config.JupyterFacade;
import io.hops.hopsworks.common.dao.kafka.HopsKafkaAdminClient;
import io.hops.hopsworks.common.dao.kafka.KafkaConst;
import io.hops.hopsworks.common.dao.kafka.ProjectTopicsFacade;
import io.hops.hopsworks.common.dao.log.operation.OperationsLogFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.jobconfig.DefaultJobConfigurationFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FolderNameValidator;
import io.hops.hopsworks.common.opensearch.OpenSearchController;
import io.hops.hopsworks.common.experiments.tensorboard.TensorBoardController;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.online.OnlineFeaturestoreController;
import io.hops.hopsworks.common.featurestore.transformationFunction.TransformationFunctionController;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.FsPermissions;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.hive.HiveController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.execution.ExecutionController;
import io.hops.hopsworks.common.jobs.yarn.YarnLogUtil;
import io.hops.hopsworks.common.jupyter.JupyterController;
import io.hops.hopsworks.common.kafka.KafkaController;
import io.hops.hopsworks.common.kafka.SubjectsCompatibilityController;
import io.hops.hopsworks.common.kafka.SubjectsController;
import io.hops.hopsworks.common.message.MessageController;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.provenance.core.Provenance;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.common.python.environment.EnvironmentController;
import io.hops.hopsworks.common.security.CertificateMaterializer;
import io.hops.hopsworks.common.security.CertificatesController;
import io.hops.hopsworks.common.serving.ServingController;
import io.hops.hopsworks.common.serving.inference.logger.KafkaInferenceLogger;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.DateUtils;
import io.hops.hopsworks.common.util.ProjectUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.common.yarn.YarnClientService;
import io.hops.hopsworks.common.yarn.YarnClientWrapper;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.exceptions.CryptoPasswordNotFoundException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.PythonException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.ServingException;
import io.hops.hopsworks.exceptions.TensorBoardException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetAccessPermission;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.hdfs.inode.InodeView;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsGroups;
import io.hops.hopsworks.persistence.entity.hdfs.user.HdfsUsers;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobType;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.persistence.entity.jobs.quota.YarnProjectsQuota;
import io.hops.hopsworks.persistence.entity.jupyter.JupyterProject;
import io.hops.hopsworks.persistence.entity.kafka.ProjectTopics;
import io.hops.hopsworks.persistence.entity.kafka.schemas.SchemaCompatibility;
import io.hops.hopsworks.persistence.entity.log.operation.OperationType;
import io.hops.hopsworks.persistence.entity.log.operation.OperationsLog;
import io.hops.hopsworks.persistence.entity.project.CreationStatus;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.jobs.DefaultJobConfiguration;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeamPK;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.activity.ActivityFlag;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.WordUtils;
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

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectController {

  private static final Logger LOGGER = Logger.getLogger(ProjectController.class.getName());

  @EJB
  protected UsersController usersController;
  @EJB
  protected JobController jobController;
  @Inject
  protected ExecutionController executionController;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private DefaultJobConfigurationFacade projectJobConfigurationFacade;
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
  private InodeController inodeController;
  @EJB
  private DatasetController datasetController;
  @EJB
  private DatasetSharedWithFacade datasetSharedWithFacade;
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
  private SubjectsCompatibilityController subjectsCompatibilityController;
  @EJB
  private KafkaController kafkaController;
  @EJB
  private TensorBoardController tensorBoardController;
  @EJB
  private OpenSearchController openSearchController;
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
  private FeaturestoreController featurestoreController;
  @EJB
  private OnlineFeaturestoreController onlineFeaturestoreController;
  @EJB
  private TransformationFunctionController transformationFunctionController;
  @Inject
  private ServingController servingController;
  @Inject
  @Any
  private Instance<ProjectHandler> projectHandlers;
  @EJB
  private ProjectUtils projectUtils;
  @EJB
  private JupyterController jupyterController;
  @EJB
  private JupyterFacade jupyterFacade;
  @EJB
  private AirflowManager airflowManager;
  @EJB
  private ProjectServiceFacade projectServiceFacade;
  @EJB
  private ProjectQuotasController projectQuotasController;
  @EJB
  private HopsKafkaAdminClient hopsKafkaAdminClient;
  @EJB
  private ProjectTopicsFacade projectTopicsFacade;
  @EJB
  private SubjectsController subjectsController;
  @EJB
  private HopsFSProvenanceController fsProvController;
  @EJB
  private AlertController alertController;
  @Inject
  @Any
  private Instance<ProjectTeamRoleHandler> projectTeamRoleHandlers;
  
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
   * @return
   */
  public Project createProject(ProjectDTO projectDTO, Users owner) throws DatasetException,
    GenericException, KafkaException, ProjectException, UserException, HopsSecurityException, ServiceException,
    FeaturestoreException, OpenSearchException, SchemaException, IOException {

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
        project = createProject(projectName, owner, projectDTO.getDescription());
      } catch (EJBException ex) {
        LOGGER.log(Level.WARNING, null, ex);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_EXISTS, Level.SEVERE, "project: " + projectName,
          ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 2 (hdfs): {0}", System.currentTimeMillis() - startTime);

      verifyProject(project, dfso);

      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 3 (verify): {0}", System.currentTimeMillis() - startTime);

      try {
        ProjectHandler.runProjectPreCreateHandlers(projectHandlers, project);
      } catch (ProjectException ex) {
        cleanup(project, null, true, owner);
        throw ex;
      }

      List<Future<?>> projectCreationFutures = new ArrayList<>();

      //create certificate for this user
      // User's certificates should be created before making any call to
      // Hadoop clients. Otherwise, the client will fail if RPC TLS is enabled
      // This is an async call
      Future<CertificatesController.CertsResult> certsResultFuture;
      try {
        certsResultFuture = certificatesController.generateCertificates(project, owner);
        projectCreationFutures.add(certsResultFuture);
      } catch (Exception ex) {
        cleanup(project, projectCreationFutures, true, owner);
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.SEVERE,
          "project: " + project.getName() +
            "owner: " + owner.getUsername(), ex.getMessage(), ex);
      }

      String username = hdfsUsersController.getHdfsUserName(project, owner);
      if (username == null || username.isEmpty()) {
        cleanup(project, projectCreationFutures, true, owner);
        throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.SEVERE,
          "project: " + project.getName() + "owner: " + owner.getUsername());
      }

      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 4 (certs): {0}", System.currentTimeMillis() - startTime);

      //all the verifications have passed, we can now create the project
      //create the project folder
      ProvTypeDTO provType = settings.getProvType().dto;
      try {
        mkProjectDIR(projectName, dfso);
        fsProvController.updateProjectProvType(project, provType, dfso);
      } catch (IOException | EJBException | ProvenanceException ex) {
        cleanup(project, projectCreationFutures, true, owner);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_FOLDER_NOT_CREATED, Level.SEVERE,
          "project: " + projectName, ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 5 (inodes): {0}", System.currentTimeMillis() - startTime);

      //set payment and quotas
      try {
        setProjectOwnerAndQuotas(project, dfso, owner);
      } catch (IOException | EJBException ex) {
        cleanup(project, projectCreationFutures, true, owner);
        throw new ProjectException(RESTCodes.ProjectErrorCode.QUOTA_ERROR, Level.SEVERE, "project: " + project.getName()
          , ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 6 (quotas): {0}", System.currentTimeMillis() - startTime);

      try {
        hdfsUsersController.addProjectFolderOwner(project, dfso);
        createProjectLogResources(owner, project, dfso);
      } catch (IOException | EJBException ex) {
        cleanup(project, projectCreationFutures);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_SET_PERMISSIONS_ERROR, Level.SEVERE,
          "project: " + projectName, ex.getMessage(), ex);
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 7 (logs): {0}", System.currentTimeMillis() - startTime);

      //Delete old project indices and kibana saved objects to avoid
      // inconsistencies
      try {
        openSearchController.deleteProjectIndices(project);
        openSearchController.deleteProjectSavedObjects(projectName);
        LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 8 (opensearch cleanup): {0}",
            System.currentTimeMillis() - startTime);
      } catch (OpenSearchException ex){
        LOGGER.log(Level.FINE, "Error while cleaning old project indices", ex);
      }

      logProject(project, OperationType.Add);

      // wait for certs before adding services
      try {
        if (certsResultFuture != null) {
          certsResultFuture.get();
        }
      } catch (InterruptedException | ExecutionException ex) {
        LOGGER.log(Level.SEVERE, "Error while waiting for the certificate generation thread to finish. Will try to " +
          "cleanup...", ex);
        cleanup(project, projectCreationFutures);
        throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.SEVERE, "Error while " +
          "generating certificates.", ex.getMessage(), ex);
      }
      // enable services
      for (ProjectServiceEnum service : projectServices) {
        try {
          projectCreationFutures.addAll(addService(project, service, owner, dfso, provType));
        } catch (RESTException | IOException ex) {
          LOGGER.log(Level.SEVERE, "Error enabling service {0}: {1}. Will try to cleanup...", new Object[]{service,
            ex.getMessage()});
          cleanup(project, projectCreationFutures);
          throw ex;
        }
      }

      try {
        for (Future f : projectCreationFutures) {
          if (f != null) {
            f.get();
          }
        }
      } catch (InterruptedException | ExecutionException ex) {
        LOGGER.log(Level.SEVERE, "Error while waiting for project creation future thread to finish. Will try to " +
          "cleanup...", ex);
        cleanup(project, projectCreationFutures);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_SERVICE_ADD_FAILURE, Level.SEVERE,
          "Error while adding services.", ex.getMessage(), ex);
      }

      // Run the handlers.
      try {
        ProjectHandler.runProjectPostCreateHandlers(projectHandlers, project);
      } catch (ProjectException ex) {
        LOGGER.log(Level.SEVERE, "Error running Project Post Create Handlers {0}. Will try to cleanup...",
          ex.getMessage());
        cleanup(project, projectCreationFutures);
        throw ex;
      }

      try {
        project = environmentController.createEnv(project, owner);
      } catch (PythonException | EJBException ex) {
        LOGGER.log(Level.SEVERE, "Error creating environment {0}. Will try to cleanup...", ex.getMessage());
        cleanup(project, projectCreationFutures);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_ANACONDA_ENABLE_ERROR, Level.SEVERE,
          "project: " + projectName, ex.getMessage(), ex);
      }
      
      // set project creation status to done
      project.setCreationStatus(CreationStatus.DONE);
      projectFacade.update(project);
      
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 10 (env): {0}", System.currentTimeMillis() - startTime);

      return project;

    } finally {
      if (dfso != null) {
        dfso.close();
      }
      LOGGER.log(Level.FINE, "PROJECT CREATION TIME. Step 11 (close): {0}", System.currentTimeMillis() - startTime);
    }

  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void verifyProject(Project project, DistributedFileSystemOps dfso) throws ProjectException, GenericException {
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
        cleanup(project, true);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_QUOTA_EXISTS, Level.INFO, project.getName());
      } else if (!verifyLogs(dfso, project.getName())) {
        cleanup(project, true);
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_LOGS_EXIST, Level.INFO, severity,
          project.getName());
      }
    } catch (IOException | EJBException ex) {
      LOGGER.log(Level.SEVERE, RESTCodes.ProjectErrorCode.PROJECT_VERIFICATIONS_FAILED.toString(), ex);
      cleanup(project, true);
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_VERIFICATIONS_FAILED, Level.SEVERE);
    }
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private Project createProject(String projectName, Users user,
    String projectDescription) throws ProjectException {
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
    Project project = new Project(projectName, user, now, settings.getDefaultPaymentType());
    project.setKafkaMaxNumTopics(settings.getKafkaMaxNumTopics());
    project.setDescription(projectDescription);
    project.setCreationStatus(CreationStatus.ONGOING);

    //Persist project object
    this.projectFacade.persistProject(project);
    this.projectFacade.flushEm();
    usersController.increaseNumCreatedProjects(user.getUid());
    usersController.updateNumActiveProjects(project.getOwner().getUid());
    return project;
  }

  private boolean existingProjectFolder(Project project) {
    Inode projectInode = inodeController.getProjectRoot(project.getName());
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
    Optional<YarnProjectsQuota> projectsQuota =
        yarnProjectsQuotaFacade.findByProjectName(projectName);
    if (projectsQuota.isPresent()) {
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
  public void createProjectLogResources(Users user, Project project, DistributedFileSystemOps dfso) throws IOException,
    DatasetException, HopsSecurityException {

    for (Settings.BaseDataset ds : Settings.BaseDataset.values()) {
      datasetController.createDataset(user, project, ds.getName(), ds.getDescription(),
        Provenance.Type.DISABLED.dto, false, DatasetAccessPermission.EDITABLE, dfso);

      Path dsPath = new Path(Utils.getProjectPath(project.getName()) + ds.getName());

      FileStatus fstatus = dfso.getFileStatus(dsPath);
      // create subdirectories for the resource dataset
      if (ds.equals(Settings.BaseDataset.RESOURCES)) {
        String[] subResources = settings.getResourceDirs().split(";");
        for (String sub : subResources) {
          Path subDirPath = new Path(dsPath, sub);
          datasetController.createSubDirectory(project, subDirPath, dfso);
          dfso.setOwner(subDirPath, fstatus.getOwner(), fstatus.getGroup());
        }
      } else if (ds.equals(Settings.BaseDataset.LOGS)) {
        dfso.setStoragePolicy(dsPath, settings.getHdfsLogStoragePolicy());
        JobType[] jobTypes = new JobType[]{JobType.SPARK, JobType.PYSPARK, JobType.FLINK};
        for (JobType jobType : jobTypes) {
          Path subDirPath = new Path(dsPath, jobType.getName());
          datasetController.createSubDirectory(project, subDirPath, dfso);
          dfso.setOwner(subDirPath, fstatus.getOwner(), fstatus.getGroup());
        }
      }

      //Persist README.md to hdfs for Default Datasets
      datasetController.generateReadme(dfso, ds.getName(), ds.getDescription(), project.getName());
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
  
  public Project findProjectByName(String projectName) throws ProjectException {
    Project project = projectFacade.findByName(projectName);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectName: " +
        projectName);
    }
    return project;
  }

  // Used only during project creation
  private List<Future<?>> addService(Project project, ProjectServiceEnum service, Users user,
    DistributedFileSystemOps dfso, ProvTypeDTO projectProvCore) throws ProjectException, ServiceException,
    DatasetException, HopsSecurityException, UserException, FeaturestoreException, OpenSearchException, SchemaException,
    KafkaException, IOException {
    return addService(project, service, user, dfso, dfso, projectProvCore);
  }
  
  public List<Future<?>> addService(Project project, ProjectServiceEnum service, Users user,
    DistributedFileSystemOps dfso, DistributedFileSystemOps udfso, ProvTypeDTO projectProvCore) throws ProjectException,
    ServiceException, DatasetException, HopsSecurityException, FeaturestoreException, OpenSearchException,
    SchemaException, KafkaException, IOException, UserException {
    List<Future<?>> futureList = new ArrayList<>();

    if (projectServicesFacade.isServiceEnabledForProject(project, service)) {
      // Service already enabled for the current project. Nothing to do
      return null;
    }
  
    switch (service) {
      case JUPYTER:
        addServiceDataset(project, user, Settings.ServiceDataset.JUPYTER, dfso, udfso,
          Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.DATASET));
        if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.JOBS)) {
          addKibana(project, user);
          addServiceDataset(project, user, Settings.ServiceDataset.EXPERIMENTS, dfso, udfso,
            Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.EXPERIMENT));
        }
        break;
      case HIVE:
        addServiceHive(project, user, dfso, Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.HIVE));
        break;
      case SERVING:
        futureList.add(addServiceServing(project, user, dfso, udfso,
          Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.MODEL)));
        break;
      case JOBS:
        if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.JUPYTER)) {
          addServiceDataset(project, user, Settings.ServiceDataset.EXPERIMENTS, dfso, udfso,
            Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.EXPERIMENT));
          addKibana(project, user);
        }
        break;
      case FEATURESTORE:
        //Note: Order matters here. Training Dataset should be created before the Featurestore
        addServiceDataset(project, user, Settings.ServiceDataset.TRAININGDATASETS, dfso, udfso,
          Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.TRAINING_DATASET));
        addServiceFeaturestore(project, user, dfso,
          Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.FEATURE));
        addServiceDataset(project, user, Settings.ServiceDataset.DATAVALIDATION, dfso, udfso,
          Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.DATASET));
        addServiceDataset(project, user, Settings.ServiceDataset.STATISTICS, dfso, udfso, Provenance.Type.DISABLED.dto);
        // add onlinefs service user to project
        futureList.add(addOnlineFsUser(project));
        //Enable Jobs service at the same time as featurestore
        if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.JOBS)) {
          if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.JUPYTER)) {
            addServiceDataset(project, user, Settings.ServiceDataset.EXPERIMENTS, dfso, udfso,
              Provenance.getDatasetProvCore(projectProvCore, Provenance.MLType.EXPERIMENT));
            addKibana(project, user);
          }
        }
        // might have been enabled as regular service for project already
        if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.KAFKA)) {
          addServiceKafka(project);
        }
        // add storage connector directory in feature store hive warehouse
        featurestoreController.createStorageConnectorResourceDirectory(project, user);
        break;
      case KAFKA:
        // might have been enabled by feature store service already
        if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.KAFKA)) {
          addServiceKafka(project);
        }
        break;
    }

    // Persist enabled service in the database
    projectServicesFacade.addServiceForProject(project, service);
    logActivity(ActivityFacade.ADDED_SERVICE + service.toString(), user, project, ActivityFlag.SERVICE);
    return futureList;
  }
  
  private void addServiceDataset(Project project, Users user, Settings.ServiceDataset ds, DistributedFileSystemOps dfso,
    DistributedFileSystemOps udfso, ProvTypeDTO datasetProvCore) throws DatasetException, HopsSecurityException,
    ProjectException {
    try {
      String datasetName = ds.getName();
      //Training Datasets should be shareable, prefix with project name to avoid naming conflicts when sharing
      if (ds == Settings.ServiceDataset.TRAININGDATASETS) {
        datasetName = project.getName() + "_" + datasetName;
      }
      datasetController.createDataset(user, project, datasetName, ds.getDescription(), datasetProvCore,
        false, DatasetAccessPermission.EDITABLE, dfso);
      datasetController.generateReadme(udfso, datasetName, ds.getDescription(), project.getName());

      // This should only happen in project creation
      // Create dataset and corresponding README file as superuser
      // to postpone waiting for the certificates generation thread when
      // RPC TLS is enabled
      if (dfso == udfso && udfso.getEffectiveUser()
        .equals(dfs.getLoginUser().getUserName())) {
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

  private void addServiceHive(Project project, Users user, DistributedFileSystemOps dfso, ProvTypeDTO datasetProvCore)
    throws ProjectException {
    try {
      hiveController.createDatabase(project.getName(), "Project general-purpose Hive database");
      hiveController.createDatasetDb(project, user, dfso, project.getName(), datasetProvCore);
    } catch (SQLException | IOException | ServiceDiscoveryException ex) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_HIVEDB_CREATE_ERROR, Level.SEVERE,
        "project: " + project.getName(), ex.getMessage(), ex);
    }
  }
  
  private void addServiceKafka(Project project) throws SchemaException, KafkaException {
    subjectsCompatibilityController.setProjectCompatibility(project, SchemaCompatibility.BACKWARD);
    subjectsController.registerNewSubject(project, Settings.INFERENCE_SCHEMANAME,
      KafkaConst.INFERENCE_SCHEMA_VERSION_1, true);
    subjectsCompatibilityController.setSubjectCompatibility(project, Settings.INFERENCE_SCHEMANAME,
      SchemaCompatibility.NONE);
    subjectsController.registerNewSubject(project, Settings.INFERENCE_SCHEMANAME,
      KafkaConst.INFERENCE_SCHEMA_VERSION_2, true);
    subjectsCompatibilityController.setSubjectCompatibility(project, Settings.INFERENCE_SCHEMANAME,
      SchemaCompatibility.NONE);
    subjectsController.registerNewSubject(project, Settings.INFERENCE_SCHEMANAME,
      KafkaConst.INFERENCE_SCHEMA_VERSION_3, true);
  }
  
  /**
   * Add the onlinefs user to the project in order for the onlinefs service to be able to get an API key
   * @param project
   */
  public Future<CertificatesController.CertsResult> addOnlineFsUser(Project project)
      throws HopsSecurityException, IOException, ProjectException {
    return addServiceUser(project, OnlineFeaturestoreController.ONLINEFS_USERNAME, ProjectRoleTypes.DATA_SCIENTIST);
  }

  private Future<CertificatesController.CertsResult> addServiceServing(Project project, Users user,
    DistributedFileSystemOps dfso, DistributedFileSystemOps udfso, ProvTypeDTO datasetProvCore)
    throws ProjectException, DatasetException, HopsSecurityException,
    OpenSearchException, ServiceException, SchemaException, FeaturestoreException, KafkaException,
    IOException, UserException {

    addServiceDataset(project, user, Settings.ServiceDataset.SERVING, dfso, udfso, datasetProvCore);
    openSearchController.createIndexPattern(project, user,
        project.getName().toLowerCase() + "_serving-*");
    // If Kafka is not enabled for the project, enable it
    if (!projectServicesFacade.isServiceEnabledForProject(project, ProjectServiceEnum.KAFKA)) {
      addService(project, ProjectServiceEnum.KAFKA, user, dfso, udfso, datasetProvCore);
    }
    return addServingManager(project);
  }

  /**
   * Add to the project the serving manager. The user responsible for writing the inference logs to kafka
   * @param project
   */
  private Future<CertificatesController.CertsResult> addServingManager(Project project)
      throws IOException, HopsSecurityException, ProjectException {
    return addServiceUser(project, KafkaInferenceLogger.SERVING_MANAGER_USERNAME, ProjectRoleTypes.DATA_OWNER);
  }
  
  private Future<CertificatesController.CertsResult> addServiceUser(Project project, String username,
                                                                    ProjectRoleTypes roleType)
      throws IOException, HopsSecurityException, ProjectException {
    // Add the Serving Manager user to the project team
    Users serviceUser = userFacade.findByUsername(username);
    ProjectTeamPK stp = new ProjectTeamPK(project.getId(), serviceUser.getEmail());
    ProjectTeam st = new ProjectTeam(stp);
    st.setTeamRole(roleType.getRole());
    st.setTimestamp(new Date());
    st.setUser(serviceUser);
    st.setProject(project);//Not fetched by jpa from project id in PK
    projectTeamFacade.persistProjectTeam(st);
    // Create the Hdfs user
    hdfsUsersController.addNewMember(st);
    // Create the certificate for this project user
    Future<CertificatesController.CertsResult> certsResultFuture = null;
    try {
      certsResultFuture = certificatesController.generateCertificates(project, serviceUser);
    } catch (Exception e) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_CREATION_ERROR, Level.SEVERE,
        "failed adding service user to project: " + project.getName() + "owner: " + username, e.getMessage(), e);
    }
    // trigger project team role add handlers
    ProjectTeamRoleHandler.runProjectTeamRoleAddMembersHandlers(projectTeamRoleHandlers, project,
      Collections.singletonList(serviceUser), ProjectRoleTypes.fromString(st.getTeamRole()), true);

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
    DistributedFileSystemOps dfso, ProvTypeDTO datasetProvCore)
      throws FeaturestoreException, ProjectException, UserException {
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
      hiveController.createDatasetDb(project, user, dfso, featurestoreName, DatasetType.FEATURESTORE, featurestore,
        datasetProvCore);
      //Register built-in transformation function.
      transformationFunctionController.registerBuiltInTransformationFunctions(user, project, featurestore);
    } catch (SQLException | IOException | ServiceDiscoveryException ex) {
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

  //Set the project owner as project master in ProjectTeam table
  private void addProjectOwner(Project project, Users user) {
    ProjectTeamPK stp = new ProjectTeamPK(project.getId(), user.getEmail());
    ProjectTeam st = new ProjectTeam(stp);
    st.setTeamRole(ProjectRoleTypes.DATA_OWNER.getRole());
    // We don't trigger ProjectTeamRole handlers here. Owner's project team role must be handled within the project
    // creation handler.
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
      dfso.setStoragePolicy(location, settings.getHdfsBaseStoragePolicy());
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
   */
  public void removeProject(String userMail, int projectId) throws ProjectException, GenericException {
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    //Only project owner and admin is able to delete a project
    Users user = userFacade.findByEmail(userMail);
    if (!project.getOwner().equals(user) && !usersController.isUserInRole(user, "HOPS_ADMIN")) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_REMOVAL_NOT_ALLOWED, Level.FINE);
    }

    cleanup(project);
  }

  public String[] forceCleanup(String projectName, String userEmail) {
    CleanupLogger cleanupLogger = new CleanupLogger(projectName);
    DistributedFileSystemOps dfso = null;
    YarnClientWrapper yarnClientWrapper = null;
    try {
      dfso = dfs.getDfsOps();
      yarnClientWrapper = ycs.getYarnClientSuper(settings.getConfiguration());
      Project project = projectFacade.findByName(projectName);
      if (project != null) {
        cleanupLogger.logSuccess("Project found in the database");

        // Run custom handlers for project deletion
        try {
          ProjectHandler.runProjectPreDeleteHandlers(projectHandlers, project);
          cleanupLogger.logSuccess("Handlers successfully run");
        } catch (ProjectException e) {
          cleanupLogger.logError("Error running handlers during project cleanup");
          cleanupLogger.logError(e.getMessage());
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
          yarnProjectsQuotaFacade.removeQuota(project.getName());
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

        // 17) Delete online featurestore database
        try {
          onlineFeaturestoreController.removeOnlineFeatureStore(project);
          cleanupLogger.logSuccess("Removed featurestore db");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing featurestore db during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Delete OpenSearch template for this project
        try {
          removeOpenSearch(project);
          cleanupLogger.logSuccess("Removed OpenSearch");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing opensearch during project cleanup");
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
          servingController.deleteAll(project);
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

        // remove folder
        try {
          removeProject(project.getName(), dfso);
          cleanupLogger.logSuccess("Removed root Project folder and database entry");
        } catch (Exception ex) {
          cleanupLogger.logError("Error when removing root Project dir and database entry during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        try {
          removeAlertConfigs(project);
          cleanupLogger.logSuccess("Cleaning alert manager config from project");
        } catch (Exception ex) {
          cleanupLogger.logError("Error cleaning alert manager config during project cleanup");
          cleanupLogger.logError(ex.getMessage());
        }

        // Run custom handlers for project deletion
        try {
          ProjectHandler.runProjectPostDeleteHandlers(projectHandlers, project);
          cleanupLogger.logSuccess("Handlers successfully run");
        } catch (ProjectException e) {
          cleanupLogger.logError("Error running handlers during project cleanup");
          cleanupLogger.logError(e.getMessage());
        }

        usersController.updateNumActiveProjects(project.getOwner().getUid());
      } else {
        cleanupLogger.logSuccess("Project is *NOT* in the database, going to remove as much as possible");
        Date now = DateUtils.localDateTime2Date(DateUtils.getNow());
        Users user = userFacade.findByEmail(userEmail);
        Project toDeleteProject = new Project(projectName, user, now, settings.getDefaultPaymentType());
        toDeleteProject.setKafkaMaxNumTopics(settings.getKafkaMaxNumTopics());
        toDeleteProject.setCreationStatus(CreationStatus.ONGOING);
        projectFacade.persistProject(toDeleteProject);
        projectFacade.flushEm();

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

        // Remove OpenSearch index
        try {
          removeOpenSearch(toDeleteProject);
          cleanupLogger.logSuccess("Removed OpenSearch");
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
          yarnProjectsQuotaFacade.removeQuota(toDeleteProject.getName());
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
          certificatesController.revokeProjectCertificates(toDeleteProject);
          userCertsFacade.removeAllCertsOfAProject(projectName);
          cleanupLogger.logSuccess("Deleted certificates");
        } catch (HopsSecurityException | GenericException | IOException ex) {
          cleanupLogger.logError(ex.getMessage());
        }

        // Remove root project directory and database entry
        try {
          removeProject(projectName, dfso);
          cleanupLogger.logSuccess("Removed root project directory and database entry");
        } catch (IOException | ProjectException ex) {
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
    messageController.send(to, from, "Force project cleanup", "ServiceStatus", message, "");
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
        dfso.setOwner(new Path(inodeController.getPath(inode)),
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
  
  public List<String> findProjectNames() {
    List<String> projectNames = new ArrayList<>();
    for (Project project : projectFacade.findAll()) {
      projectNames.add(project.getName());
    }
    return projectNames;
  }

  public void cleanup(Project project) throws GenericException {
    cleanup(project,true);
  }

  public void cleanup(Project project, boolean decreaseCreatedProj) throws GenericException {
    cleanup(project, null, decreaseCreatedProj);
  }

  public void cleanup(Project project, List<Future<?>> projectCreationFutures) throws GenericException {
    cleanup(project, projectCreationFutures, true);
  }

  public void cleanup(Project project, List<Future<?>> projectCreationFutures,
                      boolean decreaseCreatedProj) throws GenericException {
    cleanup(project, projectCreationFutures, decreaseCreatedProj, null);
  }

  public void cleanup(Project project, List<Future<?>> projectCreationFutures,
                      boolean decreaseCreatedProj, Users owner) throws GenericException {

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
          String hdfsUsername = hdfsUsersController.getHdfsUserName(project, pt.getUser());
          hdfsUsers.add(hdfsUsername);
        }
        List<ApplicationReport> projectsApps = getYarnApplications(hdfsUsers, client);

        // try and close all the jupyter jobs
        removeJupyter(project);

        removeAnacondaEnv(project);

        removeAlertConfigs(project);

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
    throws IOException, InterruptedException, HopsSecurityException,
    ServiceException, ProjectException,
    GenericException, TensorBoardException, FeaturestoreException {
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();

      // Run custom handlers for project deletion
      ProjectHandler.runProjectPreDeleteHandlers(projectHandlers, project);
      
      //log removal to notify opensearch search
      logProject(project, OperationType.Delete);
      //change the owner and group of the project folder to hdfs super user
      Path location = new Path(Utils.getProjectPath(project.getName()));
      changeOwnershipToSuperuser(location, dfso);

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
      yarnProjectsQuotaFacade.removeQuota(project.getName());

      //change owner for files in shared datasets
      fixSharedDatasets(project, dfso);

      //Delete online featurestore database
      onlineFeaturestoreController.removeOnlineFeatureStore(project);

      //Delete Hive database - will automatically cleanup all the Hive's metadata
      hiveController.dropDatabases(project, dfso, false);

      try {
        //Delete OpenSearch template for this project
        removeOpenSearch(project);
      }catch (OpenSearchException ex){
        LOGGER.log(Level.WARNING, "Failure while removing OpenSearch indices", ex);
      }

      //delete project group and users
      removeGroupAndUsers(groupsToClean, usersToClean);

      // Remove servings
      try {
        servingController.deleteAll(project);
      } catch (ServingException e) {
        throw new IOException(e);
      }

      // Remove Airflow DAGs from local filesystem,
      // JWT renewal monitors and materialized X.509
      airflowManager.onProjectRemoval(project);

      //remove folder and from the database
      removeProject(project.getName(), dfso);

      if (decreaseCreatedProj) {
        usersController.decrementNumProjectsCreated(project.getOwner().getUid());
      }

      usersController.updateNumActiveProjects(project.getOwner().getUid());

      // Run custom handlers for project deletion
      ProjectHandler.runProjectPostDeleteHandlers(projectHandlers, project);

      LOGGER.log(Level.INFO, "{0} - project removed.", project.getName());
    } finally {
      if (dfso != null) {
        dfso.close();
      }
    }
  }

  private void changeOwnershipToSuperuser(Path path, DistributedFileSystemOps dfso) throws IOException {
    if (dfso.exists(path.toString())) {
      dfso.setOwner(path, dfs.getLoginUser().getUserName(), settings.getHdfsSuperUser());
    }
  }

  @TransactionAttribute(
    TransactionAttributeType.REQUIRES_NEW)
  private List<ProjectTeam> updateProjectTeamRole(Project project, ProjectRoleTypes teamRole) throws ProjectException {
    List<ProjectTeam> projectTeams = projectTeamFacade.updateTeamRole(project, teamRole);
    ProjectTeamRoleHandler.runProjectTeamRoleUpdateMembersHandlers(projectTeamRoleHandlers, project,
      projectTeams.stream().map(ProjectTeam::getUser).collect(Collectors.toList()), teamRole);
    return projectTeams;
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
  private void removeKafkaTopics(Project project) {
    List<ProjectTopics> topics = projectTopicsFacade.findTopicsByProject(project);
  
    List<String> topicNameList = topics.stream()
      .map(ProjectTopics::getTopicName)
      .collect(Collectors.toList());
  
    hopsKafkaAdminClient.deleteTopics(topicNameList);
  }

  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  private void fixSharedDatasets(Project project, DistributedFileSystemOps dfso) throws IOException {
    List<DatasetSharedWith> sharedDataSets = datasetSharedWithFacade.findByProject(project);
    for (DatasetSharedWith dataSet : sharedDataSets) {
      String owner = dataSet.getDataset().getInode().getHdfsUser().getName();
      String group = dataSet.getDataset().getInode().getHdfsGroup().getName();
      List<Inode> children = new ArrayList<>();
      inodeController.getAllChildren(dataSet.getDataset().getInode(), children);
      for (Inode child : children) {
        if (child.getHdfsUser().getName().startsWith(project.getName() + "__")) {
          Path childPath = new Path(inodeController.getPath(child));
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

  private void removeProject(String projectName, DistributedFileSystemOps dfso) throws IOException, ProjectException {
    final Path location = new Path(Utils.getProjectPath(projectName));
    if(dfso.rm(location, true)) {
      //Only proceed removing project from database if Project folder was removed successfully
      removeProjectFromDatabase(projectName);
    } else {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_FOLDER_NOT_REMOVED, Level.FINE,
        "project: " + projectName);
    }
  }

  private void removeProjectFromDatabase(String projectName) {
    projectFacade.removeProject(projectName);
  }

  /**
   * Adds new team members to a project(project) - bulk persist if team role not
   * specified or not in (Data owner or Data
   * scientist)defaults to Data scientist
   *
   * @param project
   * @param owner
   * @param projectTeams
   * @return a list of usernames that could not be added to the project team
   * list.
   * @throws ProjectException
   * @throws FeaturestoreException
   */
  @TransactionAttribute(TransactionAttributeType.NEVER)
  public List<String> addMembers(Project project, Users owner, List<ProjectTeam> projectTeams) throws
    ProjectException, FeaturestoreException {
    List<String> failedList = new ArrayList<>();
    if (projectTeams == null) {
      return failedList;
    }
  
    Users newMember;
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();//use one dfso
      for (ProjectTeam projectTeam : projectTeams) {
        try {
          if (!projectTeam.getProjectTeamPK().getTeamMember().equals(owner.getEmail())) {
            projectTeam.setTimestamp(new Date());
            newMember = userFacade.findByEmail(projectTeam.getProjectTeamPK().getTeamMember());
            boolean added = addMember(projectTeam, project, newMember, owner, dfso);
            if (newMember == null) {
              failedList.add(projectTeam.getProjectTeamPK().getTeamMember() + " was not found in the system.");
            } else if (!added) {
              failedList.add(newMember.getEmail() + " is already a member in this project.");
            }
          } else {
            failedList.add(projectTeam.getProjectTeamPK().getTeamMember() + " is already a member in this project.");
          }
        } catch (EJBException | IOException ejb) {
          failedList.add(projectTeam.getProjectTeamPK().getTeamMember() + "could not be added. Try again later.");
          LOGGER.log(Level.SEVERE, "Adding  team member {0} to members failed",
            projectTeam.getProjectTeamPK().getTeamMember());
        }
      }
    } finally {
      dfs.closeDfsClient(dfso);
    }
    return failedList;
  }
  
  public boolean addMember(ProjectTeam projectTeam, Project project, Users newMember, Users owner,
    DistributedFileSystemOps dfso) throws ProjectException, FeaturestoreException,
    IOException {
    if (projectTeam.getTeamRole() == null ||
      (!projectTeam.getTeamRole().equals(ProjectRoleTypes.DATA_SCIENTIST.getRole()) &&
        !projectTeam.getTeamRole().equals(ProjectRoleTypes.DATA_OWNER.getRole()))) {
      projectTeam.setTeamRole(ProjectRoleTypes.DATA_SCIENTIST.getRole());
    }
    
    projectTeam.setTimestamp(new Date());
    if (newMember != null && !projectTeamFacade.isUserMemberOfProject(project, newMember)) {
      //this makes sure that the member is added to the project sent as the
      //first param b/c the security check was made on the parameter sent as path.
      projectTeam.getProjectTeamPK().setProjectId(project.getId());
      projectTeam.setProject(project);
      projectTeam.setUser(newMember);
      project.getProjectTeamCollection().add(projectTeam);
      projectFacade.update(project);
      hdfsUsersController.addNewProjectMember(projectTeam, dfso);

      //if online-featurestore service is enabled in the project, give new member access to it
      if (projectServiceFacade.isServiceEnabledForProject(project, ProjectServiceEnum.FEATURESTORE) &&
          settings.isOnlineFeaturestore()) {
        Featurestore featurestore = featurestoreController.getProjectFeaturestore(project);
        onlineFeaturestoreController.createDatabaseUser(projectTeam.getUser(),
          featurestore, projectTeam.getTeamRole());

        // give access to the shared online feature stores
        for (DatasetSharedWith sharedDs : project.getDatasetSharedWithCollection()) {
          if (sharedDs.getDataset().getDsType() == DatasetType.FEATURESTORE) {
            onlineFeaturestoreController
                .shareOnlineFeatureStore(project, newMember, projectTeam.getTeamRole(),
                    sharedDs.getDataset().getFeatureStore(), sharedDs.getPermission());
          }
        }
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
        } catch (IOException | InterruptedException | ExecutionException | HopsSecurityException | GenericException e) {
          String failedUser = project.getName() + HdfsUsersController.USER_NAME_DELIMITER + newMember.getUsername();
          LOGGER.log(Level.SEVERE,
            "Could not delete user certificates for user " + failedUser + ". Manual cleanup is needed!!! ", e);
        }
        LOGGER.log(Level.SEVERE, "error while creating certificates, jupyter kernel: " + ex.getMessage(), ex);
        hdfsUsersController.removeMember(projectTeam);
        projectTeamFacade.removeProjectTeam(project, newMember);
        throw new EJBException("Could not create certificates for user");
      }
  
      // trigger project team role update handlers
      ProjectTeamRoleHandler.runProjectTeamRoleAddMembersHandlers(projectTeamRoleHandlers, project,
        Collections.singletonList(newMember), ProjectRoleTypes.fromString(projectTeam.getTeamRole()), false);
      
      String message = "You have been added to project " + project.getName() + " with a role "
        + projectTeam.getTeamRole() + ".";
      messageController.send(newMember, owner, "You have been added to a project.", message, message, "");
      
      LOGGER.log(Level.FINE, "{0} - member added to project : {1}.", new Object[]{newMember.getEmail(),
        project.getName()});
      
      logActivity(ActivityFacade.NEW_MEMBER + projectTeam.getProjectTeamPK().getTeamMember(), owner,
        project, ActivityFlag.MEMBER);
      return true;
    } else {
      return false;
    }
  }
  
  public void addMember(Users user, String role, Project project) throws ProjectException,
    UserException, FeaturestoreException, IOException {
    if (user == null || project == null) {
      throw new IllegalArgumentException("User and project can not be null.");
    }
    ProjectTeam projectTeam = new ProjectTeam(new ProjectTeamPK(project.getId(), user.getEmail()));
    projectTeam.setTeamRole(role);
    Users owner = project.getOwner();
    boolean added;
    DistributedFileSystemOps dfso = null;
    try {
      dfso = dfs.getDfsOps();
      added = addMember(projectTeam, project, user, owner, dfso);
    } finally {
      dfs.closeDfsClient(dfso);
    }
    if (!added) {
      LOGGER.log(Level.FINE, "User {0} is already a member in this project {1}.", new Object[]{user.getUsername(),
        project.getName()});
    }
  }

  /**
   * Project info as data transfer object that can be sent to the user.
   *
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
    Inode inode = inodeController.getInodeAtPath(Utils.getProjectPath(project.getName()));

    List<ProjectTeam> projectTeam = projectTeamFacade.findMembersByProject(project);
    List<ProjectServiceEnum> projectServices = projectServicesFacade.findEnabledServicesForProject(project);
    List<String> services = new ArrayList<>();
    for (ProjectServiceEnum s : projectServices) {
      services.add(s.toString());
    }

    Quotas quotas = projectQuotasController.getQuotas(project);

    return new ProjectDTO(project, inode.getId(), services, projectTeam, quotas,
        projectUtils.dockerImageIsPreinstalled(project.getDockerImage()),
        projectUtils.isOldDockerImage(project.getDockerImage()));
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
    Inode inode = inodeController.getInodeAtPath(Utils.getProjectPath(name));

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
    Collection<DatasetSharedWith> dsSharedWithProject = project.getDatasetSharedWithCollection();
    for (Dataset ds : dsInProject) {
      parent = inodes.findParent(ds.getInode());
      kids.add(new InodeView(parent, ds, inodeController.getPath(ds.getInode())));
    }
    for (DatasetSharedWith ds : dsSharedWithProject) {
      parent = inodes.findParent(ds.getDataset().getInode());
      kids.add(new InodeView(parent, ds, inodeController.getPath(ds.getDataset().getInode())));
    }

    //send the project back to client
    return new ProjectDTO(project, inode.getId(), services, projectTeam, kids,
        projectUtils.dockerImageIsPreinstalled(project.getDockerImage()),
            projectUtils.isOldDockerImage(project.getDockerImage()));
  }

  public void setProjectOwnerAndQuotas(Project project, DistributedFileSystemOps dfso, Users user)
    throws IOException {
    this.yarnProjectsQuotaFacade.persistYarnProjectsQuota(
      new YarnProjectsQuota(project.getName(), settings.getYarnDefaultQuota(), 0));
    this.yarnProjectsQuotaFacade.flushEm();

    // Here we set only the project quota. The HiveDB and Feature Store quotas are set in the HiveController
    dfso.setHdfsSpaceQuota(new Path(Utils.getProjectPath(project.getName())), settings.getHdfsDefaultQuotaInMBs());

    projectFacade.setTimestampQuotaUpdate(project, new Date());
    //Add the activity information
    logActivity(ActivityFacade.NEW_PROJECT + project.getName(), user, project, ActivityFlag.PROJECT);
    //update role information in project
    addProjectOwner(project, user);
    LOGGER.log(Level.FINE, "{0} - project created successfully.", project.getName());
  }

  public void removeMemberFromTeam(Project project, Users user, String toRemoveEmail) throws UserException,
      ProjectException, ServiceException, IOException, GenericException, JobException, HopsSecurityException,
      TensorBoardException, FeaturestoreException {
    Users userToBeRemoved = userFacade.findByEmail(toRemoveEmail);
    if (userToBeRemoved == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "user: " + toRemoveEmail);
    }
    removeMemberFromTeam(project, userToBeRemoved);
    logActivity(ActivityFacade.REMOVED_MEMBER + userToBeRemoved.getEmail(), user, project,
        ActivityFlag.MEMBER);
  }
  
  public void removeMemberFromTeam(Project project, Users userToBeRemoved) throws ProjectException, ServiceException,
    IOException, GenericException, JobException, HopsSecurityException, TensorBoardException, FeaturestoreException {
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project, userToBeRemoved);
    if (projectTeam == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.TEAM_MEMBER_NOT_FOUND, Level.FINE,
        "project: " + project + ", user: " + userToBeRemoved.getEmail());
    }
  
    //Not able to remove project owner regardless of who is trying to remove the member
    if (project.getOwner().equals(userToBeRemoved)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_OWNER_NOT_ALLOWED, Level.FINE);
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
      
      Optional<JupyterProject> jupyterProjectOptional = jupyterFacade.findByProjectUser(project, userToBeRemoved);
      if (jupyterProjectOptional.isPresent()) {
        JupyterProject jupyterProject = jupyterProjectOptional.get();
        jupyterController.shutdown(project, userToBeRemoved, jupyterProject.getSecret(),
            jupyterProject.getCid(), jupyterProject.getPort());
      }

      //kill running TB if any
      tensorBoardController.cleanup(project, userToBeRemoved);
      
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
  
    // trigger project team role remove handlers
    ProjectTeamRoleHandler.runProjectTeamRoleRemoveMembersHandlers(projectTeamRoleHandlers, project,
      Collections.singletonList(userToBeRemoved));
    
    // TODO: (Javier) Should we reuse handlers instead of calling removeOnlineFeaturestoreUser?
    // Revoke privileges for online feature store
    if (projectServiceFacade.isServiceEnabledForProject(project, ProjectServiceEnum.FEATURESTORE)) {
      Featurestore featurestore = featurestoreController.getProjectFeaturestore(project);
      onlineFeaturestoreController.removeOnlineFeaturestoreUser(featurestore, userToBeRemoved);
    }

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
    hdfsUsersController.removeMember(projectTeam);//TODO: projectTeam might be null?
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
  public void updateMemberRole(Project project, Users opsOwner, String toUpdateEmail, String newRole)
      throws UserException, ProjectException, FeaturestoreException, IOException {
    Users user = userFacade.findByEmail(toUpdateEmail);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_WAS_NOT_FOUND, Level.FINE, "user: " + toUpdateEmail);
    }
    updateMemberRole(project, user, newRole);
    logActivity(ActivityFacade.CHANGE_ROLE + toUpdateEmail, opsOwner, project, ActivityFlag.MEMBER);
  }
  
  /**
   * Updates the role of a member
   * No activity log.
   * @param project
   * @param user
   * @param newRole
   * @throws UserException
   * @throws ProjectException
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
  public void updateMemberRole(Project project, Users user, String newRole) throws ProjectException,
      FeaturestoreException, IOException {
    if (project.getOwner().equals(user)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_OWNER_ROLE_NOT_ALLOWED, Level.FINE,
        "project: " + project.getName());
    }
    ProjectTeam projectTeam = projectTeamFacade.findProjectTeam(project, user);
    if (projectTeam == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.TEAM_MEMBER_NOT_FOUND, Level.FINE,
        "project: " + project.getName() + ", user: " + user.getUsername());
    }
    if (newRole == null || newRole.isEmpty() ||
      (!newRole.equals(AllowedRoles.DATA_OWNER) && !newRole.equals(AllowedRoles.DATA_SCIENTIST))) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ROLE_NOT_SET, Level.FINE,
        "Role not set or not supported role=" + newRole);
    }
    if (projectTeam.getTeamRole().equals(newRole)) {
      //nothing to update
      return;
    }
    projectTeam.setTeamRole(newRole);
    projectTeam.setTimestamp(new Date());
    projectTeamFacade.update(projectTeam);
  
    hdfsUsersController.changeMemberRole(projectTeam);

    // trigger project team role update handlers
    ProjectTeamRoleHandler.runProjectTeamRoleUpdateMembersHandlers(projectTeamRoleHandlers, project,
      Collections.singletonList(user), ProjectRoleTypes.fromString(newRole));
    
    // TODO: (Javier) Should we reuse handlers instead of calling updateUserOnlineFeatureStoreDB?
    // Update privileges for online feature store
    if (projectServiceFacade.isServiceEnabledForProject(project, ProjectServiceEnum.FEATURESTORE)) {
      Featurestore featurestore = featurestoreController.getProjectFeaturestore(project);
      onlineFeaturestoreController.updateUserOnlineFeatureStoreDB(user, featurestore, newRole);
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
   * @param activityPerformed the description of the operation performed
   * @param performedBy the user that performed the operation
   * @param performedOn the project the operation was performed on.
   * @param flag
   */
  public void logActivity(String activityPerformed, Users performedBy, Project performedOn, ActivityFlag flag) {
    activityFacade.persistActivity(activityPerformed, performedOn, performedBy, flag);
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
    Inode inode = inodeController.getInodeAtPath(Utils.getProjectPath(project.getName()));
    operationsLogFacade.persist(new OperationsLog(project, inode.getId(), type));
  }

  @TransactionAttribute(TransactionAttributeType.NEVER)
  public void removeAnacondaEnv(Project project) throws PythonException {
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

  public void removeAlertConfigs(Project project) throws AlertException {
    try {
      alertController.cleanProjectAlerts(project);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException | AlertManagerUnreachableException
        | AlertManagerResponseException | AlertManagerClientCreateException | AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CLEAN, Level.FINE, e.getMessage());
    }
  }

  /**
   * Handles Kibana related indices and templates for projects.
   *
   * @param project project
   * @param user user
   * @throws ProjectException ProjectException
   */
  public void addKibana(Project project, Users user) throws ProjectException {

    String projectName = project.getName().toLowerCase();
  
    try {
      openSearchController.createIndexPattern(project, user, projectName + Settings.OPENSEARCH_LOGS_INDEX_PATTERN);
    } catch (OpenSearchException e) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_KIBANA_CREATE_INDEX_ERROR, Level.SEVERE, "Could " +
        "provision project on Kibana. Contact an administrator if problem persists. Reason: " + e.getUsrMsg(),
        e.getDevMsg(), e );
    }
  }

  public void removeOpenSearch(Project project) throws OpenSearchException {
    List<ProjectServiceEnum> projectServices = projectServicesFacade.
      findEnabledServicesForProject(project);

    String projectName = project.getName().toLowerCase();

    if (projectServices.contains(ProjectServiceEnum.JOBS)
      || projectServices.contains(ProjectServiceEnum.JUPYTER)
      || projectServices.contains(ProjectServiceEnum.SERVING)) {
      openSearchController.deleteProjectIndices(project);
      LOGGER.log(Level.FINE, "removeOpenSearch-1:{0}", projectName);
      openSearchController.deleteProjectSavedObjects(projectName);
    }
  }

  private AccessCredentialsDTO getAccessCredentials(Project project, Users user)
      throws IOException, CryptoPasswordNotFoundException, CertificateException, NoSuchAlgorithmException,
      KeyStoreException, UnrecoverableKeyException {
    //Read certs from database and stream them out
    certificateMaterializer.materializeCertificatesLocal(user.getUsername(), project.getName());
    CertificateMaterializer.CryptoMaterial material = certificateMaterializer.getUserMaterial(user.getUsername(),
      project.getName());
    
    StringBuilder clientKey = new StringBuilder();
    StringBuilder clientCert = new StringBuilder();
    
    KeyStore jksKeyStore = KeyStore.getInstance("JKS");
    jksKeyStore.load(new ByteArrayInputStream(material.getKeyStore().array()), material.getPassword());
    Enumeration<String> aliases = jksKeyStore.aliases();
    while (aliases.hasMoreElements()) {
      String next = aliases.nextElement();
  
      jksKeyStore.getKey(next, material.getPassword());
      appendBytesToPemStr(clientKey, jksKeyStore.getKey(next, material.getPassword()).getEncoded(), "PRIVATE KEY");
      
      for (Certificate certificate : jksKeyStore.getCertificateChain(next)) {
        appendBytesToPemStr(clientCert, certificate.getEncoded(), "CERTIFICATE");
      }
    }
    
    StringBuilder caChain = new StringBuilder();
    KeyStore jksTrustStore = KeyStore.getInstance("JKS");
    jksTrustStore.load(new ByteArrayInputStream(material.getTrustStore().array()), material.getPassword());
    aliases = jksTrustStore.aliases();
    while (aliases.hasMoreElements()) {
      String next = aliases.nextElement();
    
      Certificate cert = jksTrustStore.getCertificate(next);
      appendBytesToPemStr(caChain, cert.getEncoded(), "CERTIFICATE");
    }
    
    String keyStore = Base64.encodeBase64String(material.getKeyStore().array());
    String trustStore = Base64.encodeBase64String(material.getTrustStore().array());
    String certPwd = new String(material.getPassword());
    return new AccessCredentialsDTO("jks", keyStore, trustStore, certPwd, caChain.toString(), clientCert.toString(),
      clientKey.toString());
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

  private void appendBytesToPemStr(StringBuilder pemString, byte[] derBytes, String pemType) {
    pemString.append("-----BEGIN ").append(pemType).append("-----\n")
      .append(WordUtils.wrap(Base64.encodeBase64String(derBytes), 64, null, true)).append("\n")
      .append("-----END ").append(pemType).append("-----\n");
  }

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

  public DefaultJobConfiguration getProjectDefaultJobConfiguration(Project project, JobType jobType) {
    JobConfiguration jobConfiguration = jobController.getConfiguration(project, jobType, false);
    if(jobConfiguration != null) {
      return new DefaultJobConfiguration(project, jobType, jobConfiguration);
    } else {
      return null;
    }
  }

  public DefaultJobConfiguration createOrUpdateDefaultJobConfig(Project project, JobConfiguration newConfig,
                                                            JobType jobType, DefaultJobConfiguration currentConfig)
      throws ProjectException {
    return projectJobConfigurationFacade.createOrUpdate(project, newConfig, jobType,
        currentConfig);
  }

  public void removeProjectDefaultJobConfiguration(Project project, JobType type) {
    projectJobConfigurationFacade.removeDefaultJobConfig(project, type);
  }
}
