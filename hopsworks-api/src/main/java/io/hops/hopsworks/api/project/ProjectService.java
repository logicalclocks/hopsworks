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
import io.hops.hopsworks.api.alert.AlertResource;
import io.hops.hopsworks.api.cloud.RoleMappingResource;
import io.hops.hopsworks.api.dataset.DatasetResource;
import io.hops.hopsworks.api.opensearch.OpenSearchResource;
import io.hops.hopsworks.api.experiments.ExperimentsResource;
import io.hops.hopsworks.api.featurestore.FeaturestoreService;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.git.GitResource;
import io.hops.hopsworks.api.integrations.IntegrationsResource;
import io.hops.hopsworks.api.jobs.JobsResource;
import io.hops.hopsworks.api.jupyter.JupyterService;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.kafka.KafkaResource;
import io.hops.hopsworks.api.metadata.XAttrsResource;
import io.hops.hopsworks.api.modelregistry.ModelRegistryResource;
import io.hops.hopsworks.api.project.jobconfig.DefaultJobConfigurationResource;
import io.hops.hopsworks.api.project.alert.ProjectAlertsResource;
import io.hops.hopsworks.api.provenance.ProjectProvenanceResource;
import io.hops.hopsworks.api.python.PythonResource;
import io.hops.hopsworks.api.serving.ServingService;
import io.hops.hopsworks.api.serving.inference.InferenceResource;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.audit.logger.annotation.Secret;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.dataset.DataSetDTO;
import io.hops.hopsworks.common.dao.dataset.DatasetFacade;
import io.hops.hopsworks.common.dao.hdfs.inode.InodeFacade;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.pia.PiaFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.dataset.FilePreviewDTO;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.hdfs.inode.InodeController;
import io.hops.hopsworks.common.project.AccessCredentialsDTO;
import io.hops.hopsworks.common.project.MoreInfoDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.project.QuotasDTO;
import io.hops.hopsworks.common.project.TourProjectType;
import io.hops.hopsworks.common.provenance.core.HopsFSProvenanceController;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.common.user.AuthController;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.hdfs.inode.Inode;
import io.hops.hopsworks.persistence.entity.jobs.quota.YarnPriceMultiplicator;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.pia.Pia;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Path("/project")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
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
  private KafkaResource kafka;
  @Inject
  private JupyterService jupyter;
  @Inject
  private AirflowService airflow;
  @Inject
  private ServingService servingService;
  @Inject
  private DatasetResource datasetResource;
  @Inject
  private ExperimentsResource experiments;
  @Inject
  private ModelRegistryResource modelRegistry;
  @Inject
  private JobsResource jobs;
  @Inject
  private PythonResource pythonResource;
  @Inject
  private GitResource gitService;
  @EJB
  private DatasetFacade datasetFacade;
  @EJB
  private DatasetController datasetController;
  @EJB
  private InodeFacade inodes;
  @EJB
  private InodeController inodeController;
  @EJB
  private HdfsUsersController hdfsUsersBean;
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
  private Settings settings;
  @Inject
  private InferenceResource inference;
  @Inject
  private ProjectActivitiesResource activitiesResource;
  @Inject
  private FeaturestoreService featurestoreService;
  @Inject
  private XAttrsResource xattrs;
  @Inject
  private ProjectProvenanceResource provenance;
  @Inject
  private OpenSearchResource openSearch;
  @Inject
  private RoleMappingResource roleMappingResource;
  @EJB
  private HopsFSProvenanceController fsProvenanceController;
  @Inject
  private IntegrationsResource integrationsResource;
  @Inject
  private AlertResource alertResource;
  @EJB
  private AccessController accessCtrl;
  @Inject
  private DefaultJobConfigurationResource defaultJobConfigurationResource;
  @Inject
  private ProjectAlertsResource projectAlertsResource;

  private final static Logger LOGGER = Logger.getLogger(ProjectService.class.getName());

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response findAllByUser(@Context HttpServletRequest req, @Context SecurityContext sc) {
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
  public Response getAllProjects(@Context HttpServletRequest req, @Context SecurityContext sc) {
    List<Project> list = projectFacade.findAll();
    GenericEntity<List<Project>> projects = new GenericEntity<List<Project>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projects).build();
  }

  @GET
  @Path("/getProjectInfo/{projectName}")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProjectByName(@PathParam("projectName") String projectName,
                                   @Context HttpServletRequest req, @Context SecurityContext sc)
      throws ProjectException {
    ProjectDTO proj = projectController.getProjectByName(projectName);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(proj).build();
  }

  @GET
  @Path("/asShared/getProjectInfo/{projectName}")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.PROJECT}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProjectByNameAsShared(@PathParam("projectName") String projectName,
    @Context HttpServletRequest req, @Context SecurityContext sc) throws ProjectException, GenericException {
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = projectController.findProjectByName(projectName);
    if(accessCtrl.hasExtendedAccess(user, project)) {
      ProjectDTO proj = projectController.getProjectByID(project.getId());
      return Response.ok().entity(proj).build();
    } else {
      throw new GenericException(RESTCodes.GenericErrorCode.NOT_AUTHORIZED_TO_ACCESS, Level.INFO);
    }
  }

  @GET
  @Path("/getMoreInfo/proj/{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMoreInfo(@PathParam("projectId") Integer projectId,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc)
    throws ProjectException {
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
  public Response getMoreInfo(@PathParam("inodeId") Long inodeId,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc) throws DatasetException {
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
  public Response getMoreInfo(@PathParam("projectId") Integer projectId, @PathParam("inodeId") Long id,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc) throws DatasetException {
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
  public Response getReadmeByInodeId(@PathParam("inodeId") Long inodeId,
                                     @Context HttpServletRequest req,
                                     @Context SecurityContext sc)
    throws DatasetException {
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
  public Response getDatasetInfo(@PathParam("inodeId") Long inodeId,
                                 @Context HttpServletRequest req,
                                 @Context SecurityContext sc) throws DatasetException {
    Inode inode = inodes.findById(inodeId);
    Project proj = datasetController.getOwningProject(inode);
    Dataset ds = datasetFacade.findByProjectAndInode(proj, inode);
    if (ds == null) {
      throw new DatasetException(RESTCodes.DatasetErrorCode.DATASET_NOT_FOUND, Level.FINE, "inodeId: " + inodeId);
    }

    Collection<DatasetSharedWith> projectsContainingInode = proj.getDatasetSharedWithCollection();
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
  public Response getDatasetInfo(@PathParam("projectId") Integer projectId, @PathParam("inodeId") Long inodeId,
                                 @Context HttpServletRequest req,
                                 @Context SecurityContext sc) throws ProjectException, DatasetException {
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
  public Response checkProjectAccess(@PathParam("projectId") Integer id,
                                     @Context HttpServletRequest req,
                                     @Context SecurityContext sc) {
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    json.setData(id);
    return Response.ok(json).build();
  }

  @GET
  @Path("{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response findByProjectID(@PathParam("projectId") Integer id,
                                  @Context HttpServletRequest req,
                                  @Context SecurityContext sc) throws ProjectException {

    // Get a specific project based on the id, Annotated so that
    // only the user with the allowed role is able to see it
    ProjectDTO proj = projectController.getProjectByID(id);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(proj).build();
  }

  @PUT
  @Path("{projectId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response updateProject(ProjectDTO projectDTO, @PathParam("projectId") Integer id,
                                @Context HttpServletRequest req,
                                @Context SecurityContext sc)
    throws ProjectException, DatasetException, HopsSecurityException, ServiceException, FeaturestoreException,
    OpenSearchException, SchemaException, KafkaException, ProvenanceException, IOException, UserException {

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
        ProvTypeDTO projectMetaStatus = fsProvenanceController.getProjectProvType(user, project);
        List<Future<?>> serviceFutureList
          = projectController.addService(project, se, user, dfso, udfso, projectMetaStatus);
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

  private void populateActiveServices(List<String> projectServices, TourProjectType tourType) {
    for (ProjectServiceEnum service : tourType.getActiveServices()) {
      projectServices.add(service.name());
    }
  }

  @POST
  @Path("starterProject/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response example(@PathParam("type") String type, @Context HttpServletRequest req, @Context SecurityContext sc)
    throws DatasetException, GenericException, KafkaException, ProjectException, UserException, ServiceException,
    HopsSecurityException, FeaturestoreException, JobException, IOException, OpenSearchException, SchemaException,
    ProvenanceException {
    TourProjectType demoType;
    try {
      demoType = TourProjectType.fromString(type);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Type must be one of: " + Arrays.toString(TourProjectType.values()));
    }
    ProjectDTO projectDTO = new ProjectDTO();
    Project project = null;
    projectDTO.setDescription("A demo project for getting started with " + demoType.getDescription());

    Users user = jWTHelper.getUserPrincipal(sc);
    String username = user.getUsername();
    List<String> projectServices = new ArrayList<>();
    //save the project

    String readMeMessage = null;
    switch (demoType) {
      case KAFKA:
        // It's a Kafka guide
        projectDTO.setProjectName("demo_" + TourProjectType.KAFKA.getTourName() + "_" + username);
        populateActiveServices(projectServices, TourProjectType.KAFKA);
        readMeMessage = "jar file to demonstrate Kafka streaming";
        break;
      case SPARK:
        // It's a Spark guide
        projectDTO.setProjectName("demo_" + TourProjectType.SPARK.getTourName() + "_" + username);
        populateActiveServices(projectServices, TourProjectType.SPARK);
        readMeMessage = "jar file to demonstrate the creation of a spark batch job";
        break;
      case ML:
        // It's a TensorFlow guide
        projectDTO.setProjectName("demo_" + TourProjectType.ML.getTourName() + "_" + username);
        populateActiveServices(projectServices, TourProjectType.ML);
        readMeMessage = "Jupyter notebooks and training data for demonstrating how to run Deep Learning";
        break;
      default:
        throw new IllegalArgumentException("Type must be one of: " + Arrays.toString(TourProjectType.values()));
    }
    projectDTO.setServices(projectServices);

    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      project = projectController.createProject(projectDTO, user, req.getSession().getId());
      dfso = dfs.getDfsOps();
      username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      ProvTypeDTO projectMetaStatus = fsProvenanceController.getProjectProvType(user, project);
      String tourFilesDataset
        = projectController.addTourFilesToProject(user.getEmail(), project, dfso, dfso, demoType, projectMetaStatus);
      //TestJob dataset
      datasetController.generateReadme(udfso, tourFilesDataset, readMeMessage, project.getName());
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
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).entity(project).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  public Response createProject(ProjectDTO projectDTO, @Context HttpServletRequest req, @Context SecurityContext sc)
    throws DatasetException, GenericException, KafkaException, ProjectException, UserException, ServiceException,
    HopsSecurityException, FeaturestoreException, OpenSearchException, SchemaException, IOException {

    Users user = jWTHelper.getUserPrincipal(sc);
    projectController.createProject(projectDTO, user, req.getSession().getId());

    RESTApiJsonResponse json = new RESTApiJsonResponse();
    StringBuilder message = new StringBuilder();
    message.append(ResponseMessages.PROJECT_CREATED);
    message.append("<br>You have ").append(user.getMaxNumProjects() - user.getNumCreatedProjects()).
        append(" project(s) left that you can create");
    json.setSuccessMessage(message.toString());

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

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/projectMembers")
  public ProjectMembersService projectMembers(@PathParam("projectId") Integer id) {
    this.projectMembers.setProjectId(id);
    return this.projectMembers;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/dataset")
  public DatasetResource datasetResource(@PathParam("projectId") Integer id) {
    this.datasetResource.setProjectId(id);
    return this.datasetResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/jobs")
  public JobsResource jobs(@PathParam("projectId") Integer projectId) {
    return this.jobs.setProject(projectId);
  }

  @GET
  @Path("{projectId}/quotas")
  @Logged(logLevel = LogLevel.OFF)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response quotasByProjectID(@PathParam("projectId") Integer id) throws ProjectException {
    Project project = projectController.findProjectById(id);
    QuotasDTO quotas = projectController.getQuotasInternal(project);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(quotas).build();
  }

  @GET
  @Path("{projectId}/multiplicators")
  @Logged(logLevel = LogLevel.OFF)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getCurrentMultiplicator(@PathParam("projectId") Integer id) {

    List<YarnPriceMultiplicator> multiplicatorsList = projectController.getYarnMultiplicators();

    GenericEntity<List<YarnPriceMultiplicator>> multiplicators = new GenericEntity<List<YarnPriceMultiplicator>>(
        multiplicatorsList) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(multiplicators).build();
  }

  @POST
  @Path("{projectId}/downloadCert")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response downloadCerts(@PathParam("projectId") Integer id,
                                @Secret @FormParam("password") String password,
                                @Context HttpServletRequest req,
                                @Context SecurityContext sc)
      throws ProjectException, HopsSecurityException, DatasetException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (user.getEmail().equals(Settings.AGENT_EMAIL) || !authController.validatePassword(user, password)) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERT_ACCESS_DENIED, Level.FINE);
    }
    AccessCredentialsDTO certsDTO = projectController.credentials(id, user);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(certsDTO).build();
  }

  @GET
  @Path("{projectId}/credentials")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response credentials(@PathParam("projectId") Integer id, @Context HttpServletRequest req,
    @Context SecurityContext sc) throws ProjectException, DatasetException {
    Users user = jWTHelper.getUserPrincipal(sc);
    AccessCredentialsDTO certsDTO = projectController.credentials(id, user);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(certsDTO).build();
  }

  @GET
  @Path("{projectId}/client")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response client(@PathParam("projectId") Integer id, @Context HttpServletRequest req,
      @Context SecurityContext sc) throws FileNotFoundException {
    File clientFile = Paths.get(settings.getClientPath(), "clients.tar.gz").toFile();
    InputStream stream = new FileInputStream(clientFile);
    Response.ResponseBuilder response = Response.ok(HopsUtils.buildOutputStream(stream));
    response.header("Content-disposition", "attachment; filename=client.tar.gz");
    return response.build();
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/kafka")
  public KafkaResource kafka(@PathParam("projectId") Integer id) {
    this.kafka.setProjectId(id);
    return this.kafka;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/jupyter")
  public JupyterService jupyter(@PathParam("projectId") Integer id) {
    this.jupyter.setProjectId(id);
    return this.jupyter;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/jobconfig")
  public DefaultJobConfigurationResource defaultJobConfig(@PathParam("projectId") Integer id) {
    this.defaultJobConfigurationResource.setProjectId(id);
    return this.defaultJobConfigurationResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/experiments")
  public ExperimentsResource experiments(@PathParam("projectId") Integer id) {
    this.experiments.setProjectId(id);
    return this.experiments;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/modelregistries")
  public ModelRegistryResource modelregistries(@PathParam("projectId") Integer id) {
    this.modelRegistry.setProjectId(id);
    return this.modelRegistry;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/airflow")
  public AirflowService airflow(@PathParam("projectId") Integer id) {
    this.airflow.setProjectId(id);
    return this.airflow;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/serving")
  public ServingService servingService(@PathParam("projectId") Integer id) {
    this.servingService.setProjectId(id);
    return this.servingService;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/python")
  public PythonResource python(@PathParam("projectId") Integer id) {
    this.pythonResource.setProjectId(id);
    return this.pythonResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/activities")
  public ProjectActivitiesResource activities(@PathParam("projectId") Integer id) {
    this.activitiesResource.setProjectId(id);
    return this.activitiesResource;
  }

  @PUT
  @Path("{projectId}/pia")
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response updatePia(Pia pia, @PathParam("projectId") Integer projectId,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) {
    piaFacade.mergeUpdate(pia, projectId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @GET
  @Path("{projectId}/pia")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getPia(@PathParam("projectId") Integer projectId,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws ProjectException {
    Project project = projectController.findProjectById(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    Pia pia = piaFacade.findByProject(projectId);
    GenericEntity<Pia> genericPia = new GenericEntity<Pia>(pia) {
    };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(genericPia).build();
  }

  @Logged(logLevel = LogLevel.OFF)
  @ApiOperation(value = "Model inference sub-resource",
      tags = {"Inference"})
  @Path("/{projectId}/inference")
  public InferenceResource infer(@PathParam("projectId") Integer projectId) {
    inference.setProjectId(projectId);
    return inference;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/featurestores")
  public FeaturestoreService featurestoreService(@PathParam("projectId") Integer projectId) {
    featurestoreService.setProjectId(projectId);
    return featurestoreService;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/xattrs")
  public XAttrsResource xattrs(@PathParam("projectId") Integer projectId) {
    this.xattrs.setProject(projectId);
    return xattrs;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/provenance")
  public ProjectProvenanceResource provenance(@PathParam("projectId") Integer id) {
    this.provenance.setProjectId(id);
    return provenance;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/elastic")
  public OpenSearchResource elastic(@PathParam("projectId") Integer id) {
    this.openSearch.setProjectId(id);
    return openSearch;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/cloud")
  public RoleMappingResource cloud(@PathParam("projectId") Integer id) {
    this.roleMappingResource.setProjectId(id);
    return this.roleMappingResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/integrations")
  public IntegrationsResource integration(@PathParam("projectId") Integer id) {
    this.integrationsResource.setProjectId(id);
    return integrationsResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/alerts")
  public AlertResource alerting(@PathParam("projectId") Integer id) {
    this.alertResource.setProjectId(id);
    return alertResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/service/alerts")
  public ProjectAlertsResource projectAlert(@PathParam("projectId") Integer id) {
    this.projectAlertsResource.setProjectId(id);
    return projectAlertsResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("{projectId}/git")
  public GitResource git(@PathParam("projectId") Integer id) {
    this.gitService.setProjectId(id);
    return gitService;
  }
}
