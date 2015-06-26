package se.kth.hopsworks.rest;

import java.util.List;
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
import se.kth.bbc.project.Project;
import se.kth.bbc.project.ProjectTeam;
import se.kth.hopsworks.controller.MembersDTO;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 * @author André<amore@kth.se>
 * @author Ermias<ermiasg@kth.se>
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectMembers {

  @EJB
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  private Integer projectId;

  public ProjectMembers() {
  }

  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }

  public Integer getProjectId() {
    return projectId;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response findMembersByProjectID(
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    List<ProjectTeam> list = projectController.findProjectTeamById(
            this.projectId);
    GenericEntity<List<ProjectTeam>> projects
            = new GenericEntity<List<ProjectTeam>>(list) {
            };

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
    projectController.updateMemberRole(project, owner, email, role);

    json.setSuccessMessage(ResponseMessages.MEMBER_ROLE_UPDATED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();

  }

  @DELETE
  @Path("/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_OWNER})
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
    projectController.deleteMemberFromTeam(project, owner, email);

    json.setSuccessMessage(ResponseMessages.MEMBER_REMOVED_FROM_TEAM);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
            json).build();

  }

}
