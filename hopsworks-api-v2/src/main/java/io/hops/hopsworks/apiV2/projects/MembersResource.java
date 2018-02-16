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
import io.hops.hopsworks.common.dao.project.team.ProjectTeam;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamPK;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.project.ProjectController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Api("Project Members")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MembersResource {
  
  @EJB
  ProjectTeamFacade projectTeamFacade;
  @EJB
  ProjectController projectController;
  @EJB
  UserFacade userFacade;
  
  private Project project;
  
  public void setProject(Integer projectId) throws AppException {
    this.project = projectController.findProjectById(projectId);
  }
  
  
  @ApiOperation("Get a list of project members")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAll(@Context SecurityContext sc){
    List<ProjectTeam> membersByProject = projectTeamFacade.findMembersByProject(project);
    List<MemberView> result = new ArrayList<>();
    for (ProjectTeam projectTeam : membersByProject) {
      result.add(new MemberView(projectTeam));
    }
    GenericEntity<List<MemberView>> entity = new GenericEntity<List<MemberView>>(result){};
    return Response.ok(entity, MediaType.APPLICATION_JSON_TYPE).build();
  }
  
  @ApiOperation("Remove member from project")
  @DELETE
  @Path("/{userId}")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response deleteMember(@PathParam("userId") Integer userId, @Context SecurityContext sc, @Context
      HttpServletRequest req) throws Exception {
    Users user = userFacade.find(userId);
    
    projectController.removeMemberFromTeam(project, sc.getUserPrincipal().getName(), user.getEmail(), req.getSession
        ().getId() );
  
    return Response.noContent().build();
  }
  
  @ApiOperation("Get member information")
  @GET
  @Path("/{userId}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response getMember(@PathParam("userId") Integer userId, @Context SecurityContext sc) throws AppException {
    for (ProjectTeam member : projectTeamFacade.findMembersByProject(project)){
      if (userId.equals(member.getUser().getUid())){
        GenericEntity<MemberView> result = new GenericEntity<MemberView>(new MemberView(member)){};
        Response.ok(result, MediaType.APPLICATION_JSON_TYPE).build();
      }
    }
    throw new AppException(Response.Status.NOT_FOUND, "No such member");
  }
  
  @ApiOperation("Add a member to the project")
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response addMember(MemberView toAdd, @Context SecurityContext sc,
      @Context HttpServletRequest req, @Context UriInfo uriInfo) throws AppException {
    
    if (toAdd == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.NO_MEMBER_TO_ADD);
    }
    
    String ownerEmail = sc.getUserPrincipal().getName();
    Date now = new Date();
    
    Users users = userFacade.find(toAdd.getUser().getUid());
    ProjectTeam member = new ProjectTeam();
    ProjectTeamPK pk = new ProjectTeamPK();
    pk.setProjectId(project.getId());
    pk.setTeamMember(users.getEmail());
    member.setProjectTeamPK(pk);
    member.setUser(users);
    member.setTeamRole(toAdd.getRole());
    member.setTimestamp(now);
    member.setProject(project);
  
    List<ProjectTeam> members = new ArrayList<>();
    members.add(member);
    //add the new member to the project
    List<String> failedMembers = projectController.addMembers(project, ownerEmail, members);
    
    if (failedMembers.size() > 0) {
      ErrorResponse error = new ErrorResponse();
      error.setDescription("Failed to add member to project.");
      return Response.serverError().entity(error).build();
    }
    
    UriBuilder builder = uriInfo.getAbsolutePathBuilder();
    builder.path(Integer.toString(member.getUser().getUid()));
    return Response.created(builder.build()).entity(new MemberView(member)).build();
  }
}
