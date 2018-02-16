/*
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
 *
 */

package io.hops.hopsworks.apiV2.projects;

import io.hops.hopsworks.apiV2.ErrorResponse;
import io.hops.hopsworks.apiV2.filter.AllowedProjectRoles;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.project.service.ProjectServiceEnum;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.activity.Activity;
import io.hops.hopsworks.common.dao.user.activity.ActivityFacade;
import io.hops.hopsworks.common.dataset.DatasetController;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.project.TourProjectType;
import io.hops.hopsworks.common.user.UsersController;
import io.hops.hopsworks.common.util.Settings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.rpc.ServiceException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/projects")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Projects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsResource {
  
  private final static Logger logger = Logger.getLogger(ProjectsResource.class.getName());
  
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private ActivityFacade activityFacade;
  @EJB
  private DistributedFsService dfs;
  @EJB
  private HdfsUsersController hdfsUsersBean;
  @EJB
  private UserFacade userFacade;
  @EJB
  private UsersController usersController;
  @EJB
  private DatasetController datasetController;
  
  // SUB-RESOURCES
  @Inject
  private MembersResource members;
  @Inject
  private DatasetsResource dataSets;

  
  @GET
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  @ApiOperation(value= "Get a list of projects")
  public Response getProjects(@Context SecurityContext sc, @Context HttpServletRequest req){
    
    if(sc.isUserInRole("HOPS_ADMIN")){
      //Create full project views for admins
      List<ProjectView> projectDTOViews = new ArrayList<>();
      for (Project project : projectFacade.findAll()){
        projectDTOViews.add(new ProjectView(project));
      }
      GenericEntity<List<ProjectView>> projects = new GenericEntity<List<ProjectView>>(projectDTOViews){};
      return Response.ok(projects,MediaType.APPLICATION_JSON_TYPE).build();
    } else {
      //Create limited project views for everyone else
      List<LimitedProjectView> limitedProjectDTOS = new ArrayList<>();
      for (io.hops.hopsworks.common.dao.project.Project project : projectFacade.findAll()) {
        limitedProjectDTOS.add(new LimitedProjectView(project));
      }
      GenericEntity<List<LimitedProjectView>> projects =
          new GenericEntity<List<LimitedProjectView>>(limitedProjectDTOS){};
      return Response.ok(projects, MediaType.APPLICATION_JSON_TYPE).build();
    }
  }
  
  
  @POST
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  @ApiOperation(value= "Create a project")
  public Response createProject(ProjectView projectView, @Context SecurityContext sc, @Context HttpServletRequest req,
      @QueryParam("template") String starterType, @Context UriInfo uriInfo) throws AppException {
    
    if (starterType != null){
      return createStarterProject(starterType, sc, req);
    }
    
    //check the user
    String owner = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(owner);
    if (user == null) {
      logger.log(Level.SEVERE, "Problem finding the user {} ", owner);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
    }
    
    List<String> failedMembers = new ArrayList<>();
    ProjectDTO projectDTO = new ProjectDTO();
    projectDTO.setProjectName(projectView.getName());
    projectDTO.setDescription(projectView.getDescription());
    projectDTO.setServices(projectView.getServices());
    Project project = projectController.createProject(projectDTO, user, failedMembers, req.getSession().getId());
  
  
    if (!failedMembers.isEmpty()) {
      //json.setFieldErrors(failedMembers);
    }
  
  
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    builder.path(Integer.toString(project.getId()));
  
    return Response.created(builder.build()).entity(project).build();
  }
  
  private void populateActiveServices(List<String> projectServices,
      TourProjectType tourType) {
    for (ProjectServiceEnum service : tourType.getActiveServices()) {
      projectServices.add(service.name());
    }
  }
  

  private Response createStarterProject(
      @QueryParam("template") String type,
      @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
  
    io.hops.hopsworks.common.project.ProjectDTO projectDTO = new io.hops.hopsworks.common.project.ProjectDTO();
    io.hops.hopsworks.common.dao.project.Project project = null;
    projectDTO.setDescription("A demo project for getting started with " + type);
  
    String owner = sc.getUserPrincipal().getName();
    String username = usersController.generateUsername(owner);
    List<String> projectServices = new ArrayList<>();
    Users user = userFacade.findByEmail(owner);
    if (user == null) {
      logger.log(Level.SEVERE, "Problem finding the user {} ", owner);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
    }
    //save the project
    List<String> failedMembers = new ArrayList<>();
  
    TourProjectType demoType;
    String readMeMessage;
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
    } else if (TourProjectType.DISTRIBUTED_TENSORFLOW.getTourName().replace("_", " ").equalsIgnoreCase(type)) {
      // It's a Distributed TensorFlow guide
      demoType = TourProjectType.DISTRIBUTED_TENSORFLOW;
      projectDTO.setProjectName("demo_" + TourProjectType.DISTRIBUTED_TENSORFLOW.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.DISTRIBUTED_TENSORFLOW);
      readMeMessage = "Mnist data to demonstrate the creation of a distributed TensorFlow job";
    } else if (TourProjectType.TENSORFLOW.getTourName().equalsIgnoreCase(type)) {
      // It's a TensorFlow guide
      demoType = TourProjectType.TENSORFLOW;
      projectDTO.setProjectName("demo_" + TourProjectType.TENSORFLOW.getTourName() + "_" + username);
      populateActiveServices(projectServices, TourProjectType.TENSORFLOW);
      readMeMessage = "Mnist data and python files to demonstrate running TensorFlow noteooks";
    } else {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.STARTER_PROJECT_BAD_REQUEST);
    }
    projectDTO.setServices(projectServices);
  
    DistributedFileSystemOps dfso = null;
    DistributedFileSystemOps udfso = null;
    try {
      project = projectController.createProject(projectDTO, user, failedMembers, req.getSession().getId());
      dfso = dfs.getDfsOps();
      username = hdfsUsersBean.getHdfsUserName(project, user);
      udfso = dfs.getDfsOps(username);
      projectController.addTourFilesToProject(owner, project, dfso, dfso, demoType);
      //TestJob dataset
      datasetController.generateReadme(udfso, "TestJob", readMeMessage, project.getName());
      //Activate Anaconda and install numppy
      //      if (TourProjectType.TENSORFLOW.getTourName().equalsIgnoreCase(type)){
      //        projectController.initAnacondaForTFDemo(project, req.getSession().getId());
      //      }
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
    URI uri = UriBuilder.fromResource(ProjectsResource.class).path("{projectId}").build(project.getId());
    logger.info("Created uri: " + uri.toString());
  
    return Response.created(uri).entity(new ProjectView(project)).build();
  }
 
  @GET
  @Path("/{projectId}")
  @ApiOperation(value = "Get project metadata")
  @AllowedProjectRoles({AllowedProjectRoles.ANYONE})
  public Response getProject(@PathParam("projectId") Integer id, @Context SecurityContext sc) throws AppException {
    Project project = projectController.findProjectById(id);
    return Response.ok(new ProjectView(project),MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @POST
  @Path("/{projectId}")
  @ApiOperation(value= "Update project metadata")
  public Response updateProject(ProjectView update, @PathParam("projectId") Integer id, @Context SecurityContext sc)
      throws AppException {
    String userEmail = sc.getUserPrincipal().getName();
    Users user = userFacade.findByEmail(userEmail);
    Project project = projectController.findProjectById(id);
  
    boolean updated = false;
  
    if (projectController.updateProjectDescription(project,
        update.getDescription(), user)){
      updated = true;
    }
  
    if (projectController.updateProjectRetention(project,
        update.getRetentionPeriod(), user)){
      updated = true;
    }
  
    ErrorResponse errorResponse = new ErrorResponse();
    
    if (!update.getServices().isEmpty()) {
      // Create dfso here and pass them to the different controllers
      DistributedFileSystemOps dfso = dfs.getDfsOps();
      DistributedFileSystemOps udfso = dfs.getDfsOps(hdfsUsersBean.getHdfsUserName(project, user));
  
      for (String projectServices : update.getServices()) {
        ProjectServiceEnum se = ProjectServiceEnum.valueOf(projectServices.toUpperCase());
        try {
          if (projectController.addService(project, se, user, dfso, udfso)) {
            // Service successfully enabled
            updated = true;
          }
        } catch (ServiceException sex) {
          // Error enabling the service
          String error;
          switch (se) {
            case ZEPPELIN:
              error = ResponseMessages.ZEPPELIN_ADD_FAILURE + Settings.ServiceDataset.ZEPPELIN.getName();
              break;
            case JUPYTER:
              error = ResponseMessages.JUPYTER_ADD_FAILURE + Settings.ServiceDataset.JUPYTER.getName();
              break;
            default:
              error = ResponseMessages.PROJECT_SERVICE_ADD_FAILURE;
          }
          errorResponse.setDescription(errorResponse.getDescription() + "\n" + error);
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
  
    if (errorResponse.getDescription() != null) {
      return Response.serverError().entity(errorResponse).build();
    }
    
    if (!updated){
      return Response.notModified().build();
    }
    
    return Response.noContent().build();
  }
  
  @ApiOperation(value= "Delete project")
  @DELETE
  @Path("/{projectId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteProject(@PathParam("projectId") Integer id, @Context SecurityContext sc,
      @Context HttpServletRequest req) throws AppException {
    String userMail = sc.getUserPrincipal().getName();
    projectController.removeProject(userMail, id, req.getSession().getId());
    
    return Response.noContent().build();
  }
  
  @Path("/{projectId}/datasets")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public DatasetsResource getDataSets(@PathParam("projectId") Integer id, @Context SecurityContext sc)
      throws AppException {
    dataSets.setProject(id);
    return dataSets;
  }
  
  @ApiOperation(value = "Members sub-resource", tags = {"Members"})
  @Path("/{projectId}/members")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public MembersResource getMembers(@PathParam("projectId") Integer id, @Context SecurityContext sc)
      throws AppException {
    members.setProject(id);
    return members;
  }
  
  @Path("/{projectId}/activity")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getActivityForProject(@PathParam("projectId") Integer projectId, @Context SecurityContext sc)
      throws AppException {
    List<Activity> activities = new ArrayList<>();
    if (projectId != null){
      Project project = projectFacade.find(projectId);
      if (project == null){
        throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "No such project");
      }
      activities.addAll(activityFacade.getAllActivityOnProject(project));
    }
    GenericEntity<List<Activity>> result = new GenericEntity<List<Activity>>(activities){};
    return Response.ok(result).type(MediaType.APPLICATION_JSON_TYPE).build();
  }
}
