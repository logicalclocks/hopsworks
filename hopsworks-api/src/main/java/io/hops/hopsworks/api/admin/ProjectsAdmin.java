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
package io.hops.hopsworks.api.admin;

import io.hops.hopsworks.api.admin.dto.ProjectAdminInfoDTO;
import io.hops.hopsworks.api.admin.dto.ProjectDeletionLog;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.RESTApiJsonResponse;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.exceptions.ElasticException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.project.QuotasDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/admin")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API},
    allowedUserRoles = {"HOPS_ADMIN"})
@Api(value = "Admin")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsAdmin {

  private static final Logger LOGGER = Logger.getLogger(ProjectsAdmin.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private UserFacade userFacade;
  @EJB
  private Settings settings;
  @EJB
  private JWTHelper jWTHelper;

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects/{id}")
  public Response deleteProject(@Context HttpServletRequest req, @PathParam("id") Integer id) throws ProjectException,
      GenericException {
    Project project = projectFacade.find(id);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + id);
    }

    String sessionId = req.getSession().getId();
    projectController.removeProject(project.getOwner().getEmail(), id, sessionId);
    LOGGER.log(Level.INFO, "Deleted project with id: " + id);

    RESTApiJsonResponse response = new RESTApiJsonResponse();
    response.setSuccessMessage(ResponseMessages.PROJECT_REMOVED);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects/createas")
  public Response createProjectAsUser(@Context HttpServletRequest request, @Context SecurityContext sc,
      ProjectDTO projectDTO) throws DatasetException, GenericException, KafkaException, ProjectException, UserException,
      ServiceException, HopsSecurityException, FeaturestoreException, ElasticException, SchemaException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (user == null || !user.getEmail().equals(settings.getAdminEmail())) {
      throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.WARNING,
          "Unauthorized or unknown user tried to create a Project as another user");
    }

    String ownerEmail = projectDTO.getOwner();
    if (ownerEmail == null) {
      LOGGER.log(Level.WARNING, "Owner username is null");
      throw new IllegalArgumentException("Owner email cannot be null");
    }

    Users owner = userFacade.findByEmail(ownerEmail);
    if (owner == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE, "user:" + ownerEmail);
    }

    List<String> failedMembers = null;
    projectController.createProject(projectDTO, owner, failedMembers, request.getSession().getId());

    RESTApiJsonResponse response = new RESTApiJsonResponse();
    response.setSuccessMessage(ResponseMessages.PROJECT_CREATED);

    if (failedMembers != null && !failedMembers.isEmpty()) {
      response.setFieldErrors(failedMembers);
    }

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.CREATED)
        .entity(response).build();
  }

  /**
   * Returns admin information about all the projects
   *
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects")
  public Response getProjectsAdminInfo(@Context SecurityContext sc) {

    List<Project> projects = projectFacade.findAll();
    List<ProjectAdminInfoDTO> projectAdminInfoDTOList = new ArrayList<>();
    for (Project project : projects) {
      projectAdminInfoDTOList.add(new ProjectAdminInfoDTO(project,
          projectController.getQuotasInternal(project)));
    }

    GenericEntity<List<ProjectAdminInfoDTO>> projectsEntity = new GenericEntity<List<ProjectAdminInfoDTO>>(
        projectAdminInfoDTOList) {
    };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectsEntity).build();
  }

  /**
   * Returns admin information about the requested project
   *
   * @param projectId
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/projects/{id}")
  public Response getProjectAdminInfo(@PathParam("id") Integer projectId, @Context SecurityContext sc)
    throws ProjectException {
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    ProjectAdminInfoDTO projectAdminInfoDTO = new ProjectAdminInfoDTO(project,
        projectController.getQuotasInternal(project));

    GenericEntity<ProjectAdminInfoDTO> projectEntity = new GenericEntity<ProjectAdminInfoDTO>(projectAdminInfoDTO) {
    };

    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(projectEntity).build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/projects")
  public Response setProjectAdminInfo(ProjectAdminInfoDTO projectAdminInfoDTO, @Context SecurityContext sc)
    throws ProjectException {
    // for changes in space quotas we need to check that both space and ns options are not null
    QuotasDTO quotasDTO = projectAdminInfoDTO.getProjectQuotas();
    if (quotasDTO != null && (((quotasDTO.getHdfsQuotaInBytes() == null) != (quotasDTO.getHdfsNsQuota() == null))
        || ((quotasDTO.getHiveHdfsQuotaInBytes() == null) != (quotasDTO.getHiveHdfsNsQuota() == null))
        || ((quotasDTO.getFeaturestoreHdfsQuotaInBytes() == null) !=
        (quotasDTO.getFeaturestoreHdfsNsQuota() == null)))) {
      throw new IllegalArgumentException("projectAdminInfoDTO did not provide quotasDTO or the latter was incomplete.");
    }

    // Build the new project state as Project object
    Project project = new Project();
    project.setKafkaMaxNumTopics(settings.getKafkaMaxNumTopics());
    project.setName(projectAdminInfoDTO.getProjectName());
    project.setArchived(projectAdminInfoDTO.getArchived());
    project.setPaymentType(projectAdminInfoDTO.getPaymentType());

    projectController.adminProjectUpdate(project, quotasDTO);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).build();
  }

  @DELETE
  @Path("/projects/{name}/force")
  @Produces(MediaType.APPLICATION_JSON)
  public Response forceDeleteProject(@Context HttpServletRequest request, @Context SecurityContext sc,
      @PathParam("name") String projectName) {
    Users user = jWTHelper.getUserPrincipal(sc);
    String[] logs = projectController.forceCleanup(projectName, user.getEmail(), request.getSession().getId());
    ProjectDeletionLog deletionLog = new ProjectDeletionLog(logs[0], logs[1]);

    GenericEntity<ProjectDeletionLog> response = new GenericEntity<ProjectDeletionLog>(deletionLog) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(response).build();
  }
}
