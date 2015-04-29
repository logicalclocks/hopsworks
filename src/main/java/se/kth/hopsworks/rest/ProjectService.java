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
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import se.kth.bbc.study.Study;
import se.kth.bbc.study.StudyTeam;
import se.kth.bbc.study.services.StudyServiceEnum;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.controller.ProjectDTO;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.hopsworks.users.UserFacade;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@Path("/project")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectService {

  @EJB
  private ProjectController projectController;
  @EJB
  private UserFacade userBean;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.ALL})
  public Response findAllByUser(@Context SecurityContext sc,
          @Context HttpServletRequest req) {

    // Get the user according to current session and then get all its projects
    String eamil = sc.getUserPrincipal().getName();
    List<StudyTeam> list = projectController.findStudyByUser(eamil);
    GenericEntity<List<StudyTeam>> projects
            = new GenericEntity<List<StudyTeam>>(list) {
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
    ProjectDTO proj = projectController.getStudyByID(id);

    GenericEntity<ProjectDTO> projs = new GenericEntity<ProjectDTO>(proj) {
    };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            proj).build();
  }

  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response updateProject(
          ProjectDTO projectDTO,
          @PathParam("id") String id,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    JsonResponse json = new JsonResponse();

    return noCacheResponse.getNoCacheResponseBuilder(
            Response.Status.NOT_IMPLEMENTED).entity(json).build();
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

    Logger.getLogger(ProjectService.class.getName()).log(Level.SEVERE,
            projectDTO.toString());
    String owner = sc.getUserPrincipal().getName();
    try {
      List<StudyServiceEnum> studyServices = new ArrayList<>();
      for (String s : projectDTO.getServices()) {
        StudyServiceEnum se = StudyServiceEnum.valueOf(s);
        se.toString();
        studyServices.add(se);
      }
      projectDTO.setOwner(owner);
      projectDTO.setCreated(new Date());
      //save the project
      Study study = projectController.createStudy(projectDTO.getProjectName(),
              owner);
      //add the services for the project
      projectController.addServices(study, studyServices);
      //add members of the project
      failedMembers = projectController.addMembers(study, owner, projectDTO.
              getProjectTeam());
    } catch (IOException ex) {
      Logger.getLogger(ProjectService.class.getName()).log(Level.SEVERE,
              ResponseMessages.PROJECT_FOLDER_NOT_CREATED, ex);
      json.setErrorMsg(ResponseMessages.PROJECT_FOLDER_NOT_CREATED);
    } catch (IllegalArgumentException iex) {
      Logger.getLogger(ProjectService.class.getName()).log(Level.SEVERE,
              ResponseMessages.PROJECT_SERVICE_NOT_FOUND, iex);
      json.setErrorMsg(ResponseMessages.PROJECT_SERVICE_NOT_FOUND +"\n"+ json.getErrorMsg());
    } catch (EJBException ex) {
      Logger.getLogger(ProjectService.class.getName()).log(Level.SEVERE,
              ResponseMessages.PROJECT_INODE_NOT_CREATED, ex);
      json.setErrorMsg(ResponseMessages.PROJECT_INODE_NOT_CREATED +"\n"+ json.getErrorMsg());
    }

    json.setStatus("201");// Created  
    json.setSuccessMessage(ResponseMessages.PROJECT_CREATED +"\n"+ json.getErrorMsg());
    if (failedMembers != null) {
      json.setFieldErrors(failedMembers);
    }
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED).
            entity(json).build();
  }

  @DELETE
  @Path("{id}/query")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response removeProjectAndFiles(
          @PathParam("id") Integer id,
          @QueryParam("wipeData") boolean wipeData,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {
    String user = sc.getUserPrincipal().getName();
    JsonResponse json = new JsonResponse();
    boolean success = !wipeData;
    try {
      success = projectController.removeByName(id, user, wipeData);
    } catch (IOException ex) {
      Logger.getLogger(ProjectService.class.getName()).log(Level.SEVERE,
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_FOLDER_NOT_REMOVED);
    }
    json.setStatus("OK");
    if (success) {
      json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED);
    } else {
      json.setSuccessMessage(ResponseMessages.PROJECT_REMOVED_NOT_FOLDER);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

}
