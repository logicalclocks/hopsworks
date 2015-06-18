package se.kth.hopsworks.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.project.services.ProjectServiceEnum;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.controller.ProjectDTO;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Path("/project")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectService {

  @EJB
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @Inject
  private ProjectMembers projectMembers;
  @Inject
  private DataSetService dataSet;

  private final static Logger logger = Logger.getLogger(ProjectService.class.
          getName());

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response findAllByUser(@Context SecurityContext sc,
          @Context HttpServletRequest req) {

    // Get the user according to current session and then get all its projects
    String eamil = sc.getUserPrincipal().getName();
    List<ProjectTeam> list = projectController.findProjectByUser(eamil);
    GenericEntity<List<ProjectTeam>> projects
            = new GenericEntity<List<ProjectTeam>>(list) {
            };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projects).build();
  }

  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findByProjectID(
          @PathParam("id") Integer id,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    // Get a specific project based on the id, Annotated so that 
    // only the user with the allowed role is able to see it 
    ProjectDTO proj = projectController.getProjectByID(id);

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            proj).build();
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response updateProject(
          ProjectDTO projectDTO,
          @PathParam("id") Integer id,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    JsonResponse json = new JsonResponse();
    boolean updated = false;

    Project project = projectController.findProjectById(id);
    String userEmail = sc.getUserPrincipal().getName();

    // Update the description if it have been chenged
    if (project.getDescription() == null || !project.getDescription().equals(
            projectDTO.getDescription())) {
      projectController.changeProjectDesc(project, projectDTO.getDescription(),
              userEmail);
      json.setSuccessMessage(ResponseMessages.PROJECT_DESCRIPTION_CHANGED);
      updated = true;
    }

    // Add all the new services
    List<ProjectServiceEnum> projectServices = new ArrayList<>();
    for (String s : projectDTO.getServices()) {
      try {
        ProjectServiceEnum se = ProjectServiceEnum.valueOf(s.toUpperCase());
        se.toString();
        projectServices.add(se);
      } catch (IllegalArgumentException iex) {
        logger.log(Level.SEVERE,
                ResponseMessages.PROJECT_SERVICE_NOT_FOUND);
        json.setErrorMsg(s + ResponseMessages.PROJECT_SERVICE_NOT_FOUND + "\n "
                + json.getErrorMsg());
      }
    }

    if (!projectServices.isEmpty()) {
      boolean added = projectController.addServices(project, projectServices,
              userEmail);
      if (added) {
        json.setSuccessMessage(ResponseMessages.PROJECT_SERVICE_ADDED);
        updated = true;
      }
    }

    if (!updated) {
      json.setSuccessMessage("Nothing to update.");
    }

    return noCacheResponse.getNoCacheResponseBuilder(
            Response.Status.CREATED).entity(json).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response createProject(
          ProjectDTO projectDTO,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    JsonResponse json = new JsonResponse();
    List<String> failedMembers = null;
    Project project = null;

    String owner = sc.getUserPrincipal().getName();
    List<ProjectServiceEnum> projectServices = new ArrayList<>();

    for (String s : projectDTO.getServices()) {
      try {
        ProjectServiceEnum se = ProjectServiceEnum.valueOf(s.toUpperCase());
        se.toString();
        projectServices.add(se);
      } catch (IllegalArgumentException iex) {
        logger.log(Level.SEVERE,
                ResponseMessages.PROJECT_SERVICE_NOT_FOUND, iex);
        json.setErrorMsg(s + ResponseMessages.PROJECT_SERVICE_NOT_FOUND + "\n "
                + json.getErrorMsg());
      }
    }

    projectDTO.setOwner(owner);
    projectDTO.setCreated(new Date());
    try {
      //save the project
      project = projectController.createProject(projectDTO.getProjectName(),
              owner);
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              ResponseMessages.PROJECT_FOLDER_NOT_CREATED, ex);
      json.setErrorMsg(ResponseMessages.PROJECT_FOLDER_NOT_CREATED + "\n "
              + json.getErrorMsg());
    } catch (EJBException ex) {
      logger.log(Level.SEVERE,
              ResponseMessages.FOLDER_INODE_NOT_CREATED, ex);
      json.setErrorMsg(ResponseMessages.FOLDER_INODE_NOT_CREATED + "\n "
              + json.getErrorMsg());
    }

    if (project != null) {
      //add the services for the project
      projectController.addServices(project, projectServices, owner);
      //add members of the project
      failedMembers = projectController.addMembers(project, owner, projectDTO.
              getProjectTeam());
    }

    json.setStatus("201");// Created  
    json.setSuccessMessage(ResponseMessages.PROJECT_CREATED);

    if (failedMembers != null) {
      json.setFieldErrors(failedMembers);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).
            entity(json).build();
  }

  @DELETE
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeProjectAndFiles(
          @PathParam("id") Integer id,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    String user = sc.getUserPrincipal().getName();
    JsonResponse json = new JsonResponse();
    boolean success = true;
    try {
      success = projectController.removeByID(id, user, true);
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED);
    }
    json.setStatus("OK");
    if (success) {
      json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @DELETE
  @Path("{id}/remove")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeProjectNotFiles(
          @PathParam("id") Integer id,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    String user = sc.getUserPrincipal().getName();
    JsonResponse json = new JsonResponse();
    boolean success = true;
    try {
      success = projectController.removeByID(id, user, false);
    } catch (IOException ex) {
      logger.log(Level.SEVERE,
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED);
    }
    json.setStatus("OK");
    if (success) {
      json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED_NOT_FOLDER);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @Path("{id}/projectMembers")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public ProjectMembers projectMembers(
          @PathParam("id") Integer id) throws AppException {
    this.projectMembers.setProjectId(id);

    return this.projectMembers;
  }

  @Path("{id}/dataset")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public DataSetService datasets(
          @PathParam("id") Integer id) throws AppException {
    this.dataSet.setProjectId(id);

    return this.dataSet;
  }
}
