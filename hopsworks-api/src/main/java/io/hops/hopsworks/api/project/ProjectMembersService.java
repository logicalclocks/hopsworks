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

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.team.ProjectTeamFacade;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.hdfs.Utils;
import io.hops.hopsworks.common.project.MembersDTO;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.util.AccessController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.TensorBoardException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.team.ProjectRoleTypes;
import io.hops.hopsworks.persistence.entity.project.team.ProjectTeam;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectMembersService {

  @EJB
  private ProjectController projectController;
  @EJB
  private ProjectTeamFacade projectTeamFacade;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private AccessController accessCtrl;
  @EJB
  private DatasetHelper datasetHelper;

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
  @JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response findMembersByProjectID(@Context HttpServletRequest req, @Context SecurityContext sc) {
    List<ProjectTeam> list = projectController.findProjectTeamById(this.projectId);
    GenericEntity<List<ProjectTeam>> projects = new GenericEntity<List<ProjectTeam>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projects).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response addMembers(MembersDTO members, @Context HttpServletRequest req, @Context SecurityContext sc)
    throws KafkaException, ProjectException, UserException, FeaturestoreException {

    Project project = projectController.findProjectById(this.projectId);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    List<String> failedMembers = null;
    Users user = jWTHelper.getUserPrincipal(sc);

    if (members.getProjectTeam() == null || members.getProjectTeam().isEmpty()) {
      throw new IllegalArgumentException("Member was not provided in MembersDTO");
    }
    if (project != null) {
      //add new members of the project
      failedMembers = projectController.addMembers(project, user, members.getProjectTeam());
    }

    if (members.getProjectTeam().size() > 1) {
      json.setSuccessMessage(ResponseMessages.PROJECT_MEMBERS_ADDED);
    } else {
      json.setSuccessMessage(ResponseMessages.PROJECT_MEMBER_ADDED);
    }

    if (failedMembers != null && !failedMembers.isEmpty()) {
      String msg;
      if (members.getProjectTeam().size() == 1) {
        msg = "Failed to add a member. Member " + failedMembers.get(0) + " was not added.";
      } else if (members.getProjectTeam().size() == failedMembers.size()) {
        msg = "Failed to add all members. Members " + failedMembers + " were not added.";
      } else if (failedMembers.size() == 1) {
        msg = "Failed to add a member. Member " + failedMembers.get(0) + " was not added.";
      } else {
        msg = "Failed to add some members. Members " + failedMembers + " were not added.";
      }
      throw new ProjectException(RESTCodes.ProjectErrorCode.FAILED_TO_ADD_MEMBER, Level.FINE, msg);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(
        json).build();
  }
  
  @POST
  @Path("/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  public Response updateRoleByEmail(@PathParam("email") String email, @FormParam("role") String role,
                                    @Context HttpServletRequest req,
                                    @Context SecurityContext sc)
      throws ProjectException, UserException, FeaturestoreException, IOException {
    Project project = projectController.findProjectById(this.projectId);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users user = jWTHelper.getUserPrincipal(sc);
    if (email == null) {
      throw new IllegalArgumentException("Email was not provided.");
    }
    if (role == null || !ProjectRoleTypes.isAllowedRole(role)) {
      throw new IllegalArgumentException("Role was not provided.");
    }
    if (project.getOwner().getEmail().equals(email)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_OWNER_ROLE_NOT_ALLOWED, Level.FINE);
    }
    projectController.updateMemberRole(project, user, email, ProjectRoleTypes.fromString(role).getRole());
    json.setSuccessMessage(ResponseMessages.MEMBER_ROLE_UPDATED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @DELETE
  @Path("/{email}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response removeMembersByID(@PathParam("email") String email,
                                    @Context HttpServletRequest req,
                                    @Context SecurityContext sc)
      throws ProjectException, ServiceException, HopsSecurityException, UserException, GenericException, IOException,
    JobException, TensorBoardException, FeaturestoreException {
    Project project = projectController.findProjectById(this.projectId);
    RESTApiJsonResponse json = new RESTApiJsonResponse();
    Users reqUser = jWTHelper.getUserPrincipal(sc);
    if (email == null) {
      throw new IllegalArgumentException("Email was not provided");
    }
    //Not able to remove members that do not exist
    String userProjectRole = projectTeamFacade.findCurrentRole(project, email);
    if (userProjectRole == null || userProjectRole.isEmpty()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.TEAM_MEMBER_NOT_FOUND, Level.FINE);
    }

    //Data scientists can only remove themselves
    String reqUserProjectRole = projectTeamFacade.findCurrentRole(project, reqUser.getEmail());
    if (reqUserProjectRole.equals(AllowedProjectRoles.DATA_SCIENTIST) && !reqUser.getEmail().equals(email)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.MEMBER_REMOVAL_NOT_ALLOWED, Level.FINE);
    }

    projectController.removeMemberFromTeam(project, reqUser, email);

    json.setSuccessMessage(ResponseMessages.MEMBER_REMOVED_FROM_TEAM);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
  
  @GET
  @Path("/dataset/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getDatasetMembers(@PathParam("name") String dsName,
                                    @QueryParam("type") DatasetType datasetType,
                                    @Context HttpServletRequest req,
                                    @Context SecurityContext sc)
    throws ProjectException, DatasetException {
    Project project = projectController.findProjectById(this.projectId);
    String path = Utils.getProjectPath(project.getName()) + dsName;
    DatasetPath dp = datasetHelper.getDatasetPath(project, path, datasetType);
    Collection<ProjectTeam> membersCol = accessCtrl.getExtendedMembers(dp.getDataset());
    GenericEntity<Collection<ProjectTeam>> members = new GenericEntity<Collection<ProjectTeam>>(membersCol) {
    };
    return Response.ok().entity(members).build();
  }
}
