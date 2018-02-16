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
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
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
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response removeMembersByID(
          @PathParam("email") String email,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException, Exception {

    Project project = projectController.findProjectById(this.projectId);
    JsonResponse json = new JsonResponse();
    String owner = sc.getUserPrincipal().getName();
    if (email == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.EMAIL_EMPTY);
    }
    //Data Scientists are only allowed to remove themselves
    if (sc.isUserInRole(AllowedProjectRoles.DATA_SCIENTIST) && !owner.equals(email)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.MEMBER_REMOVAL_NOT_ALLOWED);
    }
    if (project.getOwner().getEmail().equals(email)) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.PROJECT_OWNER_NOT_ALLOWED);
    }
    try {
      projectController.removeMemberFromTeam(project, owner, email, req.getSession().getId());
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
