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
package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.activities.ProjectActivitiesResource;
import io.hops.hopsworks.api.airflow.AirflowService;
import io.hops.hopsworks.api.dataset.DatasetResource;
import io.hops.hopsworks.api.dela.DelaClusterProjectService;
import io.hops.hopsworks.api.dela.DelaProjectService;
import io.hops.hopsworks.api.featurestore.FeaturestoreService;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.JobsResource;
import io.hops.hopsworks.api.kafka.KafkaService;
import io.hops.hopsworks.api.jupyter.JupyterService;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.python.PythonResource;
import io.hops.hopsworks.api.serving.ServingService;
import io.hops.hopsworks.api.serving.inference.InferenceResource;
import io.hops.hopsworks.api.tensorflow.TensorBoardService;
import io.hops.hopsworks.api.util.LocalFsService;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.DataSetDTO;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWithFacade;
import io.hops.hopsworks.common.dao.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.dao.dataset.DatasetSharedWith;
import io.hops.hopsworks.common.dao.hdfs.inode.Inode;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.pia.Pia;
import io.hops.hopsworks.common.dao.project.pia.PiaFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dao.user.activity.ActivityFlag;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiScope;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.project.CertsDTO;
import io.hops.hopsworks.common.project.AccessCredentialsDTO;
import io.hops.hopsworks.common.project.MoreInfoDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.project.QuotasDTO;
import io.hops.hopsworks.common.project.TourProjectType;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/project")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Project Service",
    description = "Project Service")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectService {

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @Inject
  private ProjectMembersService projectMembers;
  @Inject
  private KafkaService kafka;
  @Inject
  private JupyterService jupyter;
  @Inject
  private AirflowService airflow;
  @Inject
  private TensorBoardService tensorboard;
  @Inject
  private ServingService servingService;
  @Inject
  private DatasetResource datasetResource;
  @Inject
  private LocalFsService localFs;
  @Inject
  private JobsResource jobs;
  @Inject
  private PythonResource pythonResource;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetSharedWithFacade datasetSharedWithFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private InodeFacade inodes;
  @EJB
  private InodeController inodeController;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private UsersController usersController;
  @EJB
  private UserFacade userFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private AuthController authController;
  @EJB
  private PiaFacade piaFacade;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private ProjectServiceFacade projectServiceFacade;
  @Inject
  private DelaProjectService delaService;
  @Inject
  private DelaClusterProjectService delaclusterService;
  @Inject
  private InferenceResource inference;
  @Inject
  private ProjectActivitiesResource activitiesResource;
  @Inject
  private FeaturestoreService featurestoreService;

  private final static Logger LOGGER = Logger.getLogger(ProjectService.class.getName());

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response findAllByUser(@Context SecurityContext sc) {
    // Get the user according to current session and then get all its projects
    Users user = jWTHelper.getUserPrincipal(sc);
    List<ProjectTeam> list = projectController.findProjectByUser(user.getEmail());
    GenericEntity<List<ProjectTeam>> projects = new GenericEntity<List<ProjectTeam>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projects).build();
  }

  @GET
  @Path("/getAll")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllProjects() {
    List<Project> list = projectFacade.findAll();
    GenericEntity<List<Project>> projects = new GenericEntity<List<Project>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projects).build();
  }

  @GET
  @Path("/getProjectInfo/{projectName}")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProjectByName(@PathParam("projectName") String projectName) throws ProjectException {
    ProjectDTO proj = projectController.getProjectByName(projectName);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(proj).build();
  }

  @GET
  @Path("/getMoreInfo/proj/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMoreInfo(@PathParam("projectId") Integer projectId) throws
      ProjectException {
    MoreInfoDTO info = null;

    if (projectId != null) {
      Project proj = projectFacade.find(projectId);
      if (proj == null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
      }
      info = new MoreInfoDTO(proj);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        info).build();
  }

  @GET
  @Path("/getMoreInfo/{type: (ds|inode)}/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMoreInfo(@PathParam("inodeId") Long inodeId) throws
      DatasetException {
    MoreInfoDTO info = null;

    if (inodeId != null) {
      info = datasetInfo(inodeId);
      if (info == null) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "datasetId: " + inodeId);
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        info).build();
  }

  @GET
  @Path("{projectId}/getMoreInfo/{type: (ds|inode)}/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMoreInfo(@PathParam("projectId") Integer projectId, @PathParam("inodeId") Long id)
      throws DatasetException {
    MoreInfoDTO info = null;
    if (id != null) {
      info = inodeInfo(id, projectId);
      if (info == null) {
        throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "datasetId: " + id);
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        info).build();
  }

  @GET
  @Path("/readme/byInodeId/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getReadmeByInodeId(@PathParam("inodeId") Long inodeId) throws DatasetException {
    if (inodeId == null) {
      throw new IllegalArgumentException("No inodeId provided.");
    }
    Inode inode = inodes.findById(inodeId);
    Inode parent = inodes.findParent(inode);
    Project proj = projectFacade.findByName(parent.getInodePK().getName());
    Dataset ds = datasetFacade.findByProjectAndInode(proj, inode);
    if (ds != null && !ds.isSearchable()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.README_NOT_ACCESSIBLE, Level.FINE);
    }
    DistributedFileSystemOps dfso = dfs.getDfsOps();
    FilePreviewDTO filePreviewDTO;
    String path = inodeController.getPath(inode);
    try {
      filePreviewDTO = datasetController.getReadme(path + "/README.md", dfso);
    } catch (IOException ex) {
      filePreviewDTO = new FilePreviewDTO();
      filePreviewDTO.setContent("No README file found for this dataset.");
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
          entity(
              filePreviewDTO).build();
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        filePreviewDTO).build();
  }

  private MoreInfoDTO datasetInfo(Long inodeId) {
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      return null;
    }
    Dataset ds = datasetFacade.findByInode(inode);
    if (ds != null && !ds.isSearchable()) {
      return null;
    }
    MoreInfoDTO info = new MoreInfoDTO(inode);
    Users user = userFacade.findByUsername(info.getUser());
    info.setUser(user.getFname() + " " + user.getLname());
    info.setSize(inodeController.getSize(inode));
    info.setPath(inodeController.getPath(inode));
    return info;
  }

  private MoreInfoDTO inodeInfo(Long inodeId, Integer projectId) {
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      return null;
    }
    String group = inode.getHdfsGroup().getName();
    Project project = projectFacade.find(projectId);
    if (project != null && !project.getName().equals(hdfsUsersBean.getProjectName(group))) {
      return null;
    }
    MoreInfoDTO info = new MoreInfoDTO(inode);
    Users user = userFacade.findByUsername(info.getUser());
    info.setUser(user.getFname() + " " + user.getLname());
    info.setSize(inodeController.getSize(inode));
    info.setPath(inodeController.getPath(inode));
    return info;
  }

  @GET
  @Path("getDatasetInfo/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDatasetInfo(@PathParam("inodeId") Long inodeId) throws DatasetException {
    Inode inode = inodes.findById(inodeId);
    Project proj = datasetController.getOwningProject(inode);
    Dataset ds = datasetFacade.findByProjectAndInode(proj, inode);
    if (ds == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "inodeId: " + inodeId);
    }

    Collection<DatasetSharedWith> projectsContainingInode = proj.getDatasetSharedWithCollectionCollection();
    List<String> sharedWith = new ArrayList<>();
    for (DatasetSharedWith d : projectsContainingInode) {
      if (!d.getProject().getId().equals(proj.getId())) {
        sharedWith.add(d.getProject().getName());
      }
    }
    DataSetDTO dataset = new DataSetDTO(ds, proj, sharedWith);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(dataset).build();
  }

  @GET
  @Path("{projectId}/getInodeInfo/{inodeId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response getDatasetInfo(@PathParam("projectId") Integer projectId, @PathParam("inodeId") Long inodeId)
      throws ProjectException, DatasetException {
    Inode inode = inodes.findById(inodeId);
    if (inode == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.INODE_NOT_FOUND, Level.FINE, "inodeId: " + inodeId);
    }
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }

    DataSetDTO dataset = new DataSetDTO(inode.getInodePK().getName(), inodeId, project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        dataset).build();
  }

  @GET
  @Path("{projectId}/check")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response checkProjectAccess(@PathParam("projectId") Integer id) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setData(id);
    return Response.ok(json).build();
  }

  @GET
  @Path("{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response findByProjectID(@PathParam("projectId") Integer id) throws ProjectException {

    // Get a specific project based on the id, Annotated so that
    // only the user with the allowed role is able to see it
    ProjectDTO proj = projectController.getProjectByID(id);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        proj).build();
  }

  @PUT
  @Path("{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response updateProject(
      ProjectDTO projectDTO, @PathParam("projectId") Integer id,
      @Context SecurityContext sc) throws ProjectException, DatasetException, HopsSecurityException,
      ServiceException, FeaturestoreException, UserException {

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = projectController.findProjectById(id);

    boolean updated = false;

    if (projectController.updateProjectDescription(project,
        projectDTO.getDescription(), user)) {
      json.setSuccessMessage(ResponseMessages.PROJECT_DESCRIPTION_CHANGED);
      updated = true;
    }

    if (projectController.updateProjectRetention(project,
        projectDTO.getRetentionPeriod(), user)) {
      json.setSuccessMessage(json.getSuccessMessage() + "\n" + ResponseMessages.PROJECT_RETENTON_CHANGED);
      updated = true;
    }

    if (!projectDTO.getServices().isEmpty()) {
      // Create dfso here and pass them to the different controllers
      DistributedFileSystemOps dfso = dfs.getDfsOps();
      DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersBean.getHdfsUserName(project, user));

      for (String s : projectDTO.getServices()) {
        ProjectServiceEnum se = null;
        se = ProjectServiceEnum.valueOf(s.toUpperCase());
        List<Future<?>> serviceFutureList = projectController.addService(project, se, user, dfso, udfso);
        if (serviceFutureList != null) {
          // Wait for the futures
          for (Future f : serviceFutureList) {
            try {
              f.get();
            } catch (InterruptedException | ExecutionException e) {
              throw new ServiceException(RESTCodes.ServiceErrorCode.SERVICE_GENERIC_ERROR,
                  Level.SEVERE, "service: " + s, e.getMessage(), e);
            }
          }

          // Service successfully enabled
          json.setSuccessMessage(json.getSuccessMessage() + "\n"
              + ResponseMessages.PROJECT_SERVICE_ADDED
              + s
          );
          updated = true;
        }
      }

      // close dfsos
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }

    if (!updated) {
      json.setSuccessMessage(ResponseMessages.NOTHING_TO_UPDATE);
    }

    return noCacheResponse.getNoCacheResponseBuilder(
        Response.Status.CREATED).entity(json).build();
  }

  private void populateActiveServices(List<String> projectServices,
      TourProjectType tourType) {
    for (ProjectServiceEnum service : tourType.getActiveServices()) {
      projectServices.add(service.name());
    }
  }

  @POST
  @Path("starterProject/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response example(@PathParam("type") String type, @Context HttpServletRequest req, @Context SecurityContext sc)
      throws DatasetException,
      GenericException, KafkaException, ProjectException, UserException, ServiceException, HopsSecurityException,
      FeaturestoreException, JobException, UnsupportedEncodingException {
    if (!Arrays.asList(TourProjectType.values()).contains(TourProjectType.valueOf(type.toUpperCase()))) {
      throw new IllegalArgumentException("Type must be one of: " + Arrays.toString(TourProjectType.values()));
    }

    ProjectDTO projectDTO = new ProjectDTO();
    Project project = null;
    projectDTO.setDescription("A demo project for getting started with " + type);

    Users user = jWTHelper.getUserPrincipal(sc);
    String username = usersController.generateUsername(user.getEmail());
    List<String> projectServices = new ArrayList<>();
    //save the project
    List<String> failedMembers = new ArrayList<>();

    TourProjectType demoType = null;
    String readMeMessage = null;
    if (TourProjectType.SPARK.getTourName().equalsIgnoreCase(type)) {
      // It's a Spark guide
      demoType = TourProjectType.SPARK;
      projectDTO.setProjectName("demo_" + TourProjectType.SPARK.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.SPARK);
      readMeMessage = "jar file to demonstrate the creation of a spark batch job";
    } else if (TourProjectType.KAFKA.getTourName().equalsIgnoreCase(type)) {
      // It's a Kafka guide
      demoType = TourProjectType.KAFKA;
      projectDTO.setProjectName("demo_" + TourProjectType.KAFKA.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.KAFKA);
      readMeMessage = "jar file to demonstrate Kafka streaming";
    } else if (TourProjectType.DEEP_LEARNING.getTourName().equalsIgnoreCase(type)) {
      // It's a TensorFlow guide
      demoType = TourProjectType.DEEP_LEARNING;
      projectDTO.setProjectName("demo_" + TourProjectType.DEEP_LEARNING.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.DEEP_LEARNING);
      readMeMessage = "Jupyter notebooks and training data for demonstrating how to run Deep Learning";
    } else if (TourProjectType.FEATURESTORE.getTourName().equalsIgnoreCase(type)) {
      // It's a Featurestore guide
      demoType = TourProjectType.FEATURESTORE;
      projectDTO.setProjectName("demo_" + TourProjectType.FEATURESTORE.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.FEATURESTORE);
      readMeMessage = "Dataset containing a jar file and data that can be used to run a sample spark-job for "
          + "inserting data in the feature store.";
    }
    projectDTO.setServices(projectServices);

    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      project = projectController.createProject(projectDTO, user, failedMembers, req.getSession().getId());
      dfso = dfs.getDfsOps();
      username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      projectController.addTourFilesToProject(user.getEmail(), project, dfso, dfso, demoType);
      //TestJob dataset
      datasetController.generateReadme(udfso, "TestJob", readMeMessage, project.getName());
    } catch (Exception ex) {
      projectController.cleanup(project, req.getSession().getId());
      throw ex;
    } finally {
      if (dfso != null) {
        dfso.close();
      }
      if (udfso != null) {
        dfs.closeDfsClient(udfso);
      }
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).
        entity(project).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createProject(ProjectDTO projectDTO, @Context HttpServletRequest req, @Context SecurityContext sc)
      throws DatasetException,
      GenericException, KafkaException, ProjectException, UserException, ServiceException, HopsSecurityException,
      FeaturestoreException {

    Users user = jWTHelper.getUserPrincipal(sc);
    List<String> failedMembers = null;
    projectController.createProject(projectDTO, user, failedMembers, req.getSession().getId());

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    StringBuilder message = new StringBuilder();
    message.append(ResponseMessages.PROJECT_CREATED);
    message.append("<br>You have ").append(user.getMaxNumProjects() - user.getNumCreatedProjects()).
        append(" project(s) left that you can create");
    json.setSuccessMessage(message.toString());

    if (failedMembers != null && !failedMembers.isEmpty()) {
      json.setFieldErrors(failedMembers);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).
        entity(json).build();
  }

  @POST
  @Path("{projectId}/delete")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response removeProjectAndFiles(@PathParam("projectId") Integer id, @Context HttpServletRequest req,
      @Context SecurityContext sc) throws ProjectException, GenericException {

    Users user = jWTHelper.getUserPrincipal(sc);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    projectController.removeProject(user.getEmail(), id, req.getSession().getId());
    json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();

  }

  @Path("{projectId}/projectMembers")
  public ProjectMembersService projectMembers(@PathParam("projectId") Integer id) {
    this.projectMembers.setProjectId(id);
    return this.projectMembers;
  }
  
  @Path("{projectId}/dataset")
  public DatasetResource datasetResource(@PathParam("projectId") Integer id) {
    this.datasetResource.setProjectId(id);
    return this.datasetResource;
  }

  @Path("{projectId}/localfs")
  public LocalFsService localFs(@PathParam("projectId") Integer id) {
    this.localFs.setProjectId(id);
    return this.localFs;
  }

  @Path("{projectId}/jobs")
  public JobsResource jobs(@PathParam("projectId") Integer projectId) {
    return this.jobs.setProject(projectId);
  }

  @GET
  @Path("{projectId}/quotas")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response quotasByProjectID(@PathParam("projectId") Integer id) throws ProjectException {

    QuotasDTO quotas = projectController.getQuotas(id);

    // If YARN quota or HDFS quota for project directory is null, something is wrong with the project
    // throw a ProjectException
    if (quotas.getHdfsQuotaInBytes() == null || quotas.getYarnQuotaInSecs() == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.QUOTA_NOT_FOUND, Level.FINE, "projectId: " + id);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(quotas).build();
  }

  @GET
  @Path("{projectId}/multiplicators")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getCurrentMultiplicator(@PathParam("projectId") Integer id) {

    List<YarnPriceMultiplicator> multiplicatorsList = projectController.getYarnMultiplicators();

    GenericEntity<List<YarnPriceMultiplicator>> multiplicators = new GenericEntity<List<YarnPriceMultiplicator>>(
        multiplicatorsList) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(multiplicators).build();
  }

  @GET
  @Path("{projectId}/importPublicDataset/{projectName}/{inodeId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response quotasByProjectID(
      @PathParam("projectId") Integer id,
      @PathParam("projectName") String projectName,
      @PathParam("inodeId") Long dsId,
      @Context SecurityContext sc) throws ProjectException, DatasetException {

    Project destProj = projectController.findProjectById(id);
    Project dsProject = projectFacade.findByName(projectName);

    Inode inode = inodes.findById(dsId);
    Dataset ds = datasetFacade.findByProjectAndInode(dsProject, inode);
    if (ds == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "project: " + projectName
          + ", inodeId:" + dsId);
    }

    if (!ds.isPublicDs()) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_PUBLIC, Level.FINE, "datasetId: " + ds.getId());
    }
  
    DatasetSharedWith datasetSharedWith = datasetSharedWithFacade.findByProjectAndDataset(destProj, ds);
    if (datasetSharedWith != null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DESTINATION_EXISTS, Level.FINE,
        "Dataset already in " + destProj.getName());
    }
    // Create the new Dataset entry
    datasetSharedWith = new DatasetSharedWith(destProj, ds, true);
    datasetSharedWithFacade.save(datasetSharedWith);
    Users user = jWTHelper.getUserPrincipal(sc);
    activityFacade.
        persistActivity(ActivityFacade.SHARED_DATA + ds.getName() + " with project " + destProj.getName(),
             destProj, user, ActivityFlag.DATASET);

    hdfsUsersBean.shareDataset(destProj, ds);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @POST
  @Path("{projectId}/downloadCert")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response downloadCerts(@PathParam("projectId") Integer id, @FormParam("password") String password,
      @Context HttpServletRequest req, @Context SecurityContext sc) throws ProjectException, HopsSecurityException,
      DatasetException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (user.getEmail().equals(Settings.AGENT_EMAIL) || !authController.validatePassword(user, password, req)) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_ACCESS_DENIED, Level.FINE);
    }
    CertsDTO certsDTO = projectController.downloadCert(id, user);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(certsDTO).build();
  }

  @GET
  @Path("{projectId}/credentials")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response credentials(@PathParam("projectId") Integer id, @Context HttpServletRequest req,
        @Context SecurityContext sc) throws ProjectException, DatasetException {
    Users user = jWTHelper.getUserPrincipal(sc);
    AccessCredentialsDTO certsDTO = projectController.credentials(id, user);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(certsDTO).build();
  }

  @Path("{projectId}/kafka")
  public KafkaService kafka(@PathParam("projectId") Integer id) {
    this.kafka.setProjectId(id);
    return this.kafka;
  }

  @Path("{projectId}/jupyter")
  public JupyterService jupyter(@PathParam("projectId") Integer id) {
    this.jupyter.setProjectId(id);
    return this.jupyter;
  }

  @Path("{projectId}/tensorboard")
  public TensorBoardService tensorboard(@PathParam("projectId") Integer id) {
    this.tensorboard.setProjectId(id);
    return this.tensorboard;
  }

  @Path("{projectId}/airflow")
  public AirflowService airflow(@PathParam("projectId") Integer id) {
    this.airflow.setProjectId(id);
    return this.airflow;
  }

  @Path("{projectId}/serving")
  public ServingService servingService(@PathParam("projectId") Integer id) {
    this.servingService.setProjectId(id);
    return this.servingService;
  }

  @Path("{projectId}/python")
  public PythonResource python(@PathParam("projectId") Integer id) {
    this.pythonResource.setProjectId(id);
    return this.pythonResource;
  }

  @Path("{projectId}/dela")
  public DelaProjectService dela(@PathParam("projectId") Integer id) {
    this.delaService.setProjectId(id);
    return this.delaService;
  }

  @Path("{projectId}/delacluster")
  public DelaClusterProjectService delacluster(@PathParam("projectId") Integer id) {
    this.delaclusterService.setProjectId(id);
    return this.delaclusterService;
  }

  @Path("{projectId}/activities")
  public ProjectActivitiesResource activities(@PathParam("projectId") Integer id) {
    this.activitiesResource.setProjectId(id);
    return this.activitiesResource;
  }

  @PUT
  @Path("{projectId}/pia")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response updatePia(Pia pia, @PathParam("projectId") Integer projectId) {
    piaFacade.mergeUpdate(pia, projectId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("{projectId}/pia")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getPia(@PathParam("projectId") Integer projectId) throws ProjectException {
    Project project = projectController.findProjectById(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    Pia pia = piaFacade.findByProject(projectId);
    GenericEntity<Pia> genericPia = new GenericEntity<Pia>(pia) {
    };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(genericPia).build();
  }

  @ApiOperation(value = "Model inference sub-resource",
      tags = {"Inference"})
  @Path("/{projectId}/inference")
  public InferenceResource infer(@PathParam("projectId") Integer projectId) {
    inference.setProjectId(projectId);
    return inference;
  }

  @Path("{projectId}/featurestores")
  public FeaturestoreService featurestoreService(@PathParam("projectId") Integer projectId) {
    featurestoreService.setProjectId(projectId);
    return featurestoreService;
  }

}
