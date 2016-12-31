package io.hops.hopsworks.api.project;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import io.hops.hopsworks.api.filter.AllowedRoles;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.MembersDTO;
import io.hops.hopsworks.common.project.ProjectController;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectMembersService {

  @EJB
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  private Integer projectId;

  public ProjectMembersService() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  private final static Logger logger = Logger.getLogger(
          ProjectMembersService.class.
          getName());

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findMembersByProjectID(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    List<ProjectTeam> list = projectController.findProjectTeamById(
            this.projectId);
    GenericEntity<List<ProjectTeam>> projects
            = new GenericEntity<List<ProjectTeam>>(list) {};

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            projects).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response addMembers(
          MembersDTO members,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    Project project = projectController.findProjectById(this.projectId);
    JsonResponse json = new JsonResponse();
    List<String> failedMembers = null;
    String owner = sc.getUserPrincipal().getName();

    if (members.getProjectTeam() == null || members.getProjectTeam().isEmpty()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.NO_MEMBER_TO_ADD);
    }
    if (project != null) {
      //add new members of the project
      failedMembers = projectController.addMembers(project, owner, members.
              getProjectTeam());
    }

    if (members.getProjectTeam().size() > 1) {
      json.setSuccessMessage(ResponseMessages.PROJECT_MEMBERS_ADDED);
    } else {
      json.setSuccessMessage(ResponseMessages.PROJECT_MEMBER_ADDED);
    }

    if (failedMembers != null) {
      json.setFieldErrors(failedMembers);
      if (members.getProjectTeam().size() > failedMembers.size() + 1) {
        json.setSuccessMessage(ResponseMessages.PROJECT_MEMBERS_ADDED);
      } else if (members.getProjectTeam().size() > failedMembers.size()) {
        json.setSuccessMessage(ResponseMessages.PROJECT_MEMBER_ADDED);
      } else {
        json.setSuccessMessage(ResponseMessages.NO_MEMBER_ADD);
      }
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();
  }

  @POST
  @Path("/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
  public Response updateRoleByEmail(
          @PathParam("email") String email,
          @FormParam("role") String role,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    Project project = projectController.findProjectById(this.projectId);
    JsonResponse json = new JsonResponse();
    String owner = sc.getUserPrincipal().getName();
    if (email == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.EMAIL_EMPTY);
    }
    if (role == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.ROLE_NOT_SET);
    }
    if (project.getOwner().getEmail().equals(email)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_OWNER_ROLE_NOT_ALLOWED);
    }
    projectController.updateMemberRole(project, owner, email, role);

    json.setSuccessMessage(ResponseMessages.MEMBER_ROLE_UPDATED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();

  }

  @DELETE
  @Path("/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response removeMembersByID(
          @PathParam("email") String email,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    Project project = projectController.findProjectById(this.projectId);
    JsonResponse json = new JsonResponse();
    String owner = sc.getUserPrincipal().getName();
    if (email == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.EMAIL_EMPTY);
    }
    //Data Scientists are only allowed to remove themselves
    if (sc.isUserInRole(AllowedRoles.DATA_SCIENTIST) && !owner.equals(email)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.MEMBER_REMOVAL_NOT_ALLOWED);
    }
    if (project.getOwner().getEmail().equals(email)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_OWNER_NOT_ALLOWED);
    }
    try {
      projectController.deleteMemberFromTeam(project, owner, email);
    } catch (IOException ex) {
      //FIXME: take an action?
      logger.log(Level.WARNING,
              "Error while trying to delete a member from team", ex);
    }

    json.setSuccessMessage(ResponseMessages.MEMBER_REMOVED_FROM_TEAM);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();

  }

}
