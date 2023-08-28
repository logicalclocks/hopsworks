/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
 */
package io.hops.hopsworks.api.admin.projects;

import io.hops.hopsworks.api.admin.dto.ProjectAdminInfoDTO;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.dao.user.UserFacade;
import io.hops.hopsworks.common.dataset.acl.PermissionsFixer;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.project.ProjectDTO;
import io.hops.hopsworks.common.project.ProjectQuotasController;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.exceptions.OpenSearchException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.exceptions.SchemaException;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.exceptions.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Path("/admin/projects")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
@ApiKeyRequired(acceptedScopes = {ApiScope.ADMIN}, allowedUserRoles = {"HOPS_ADMIN"})
@Api(value = "Admin")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectsAdminResource {

  private static final Logger LOGGER = Logger.getLogger(ProjectsAdminResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private PermissionsFixer permissionsFixer;
  @EJB
  private ProjectsAdminBuilder projectsAdminBuilder;
  @EJB
  private ProjectQuotasController projectQuotasController;
  @EJB
  private UserFacade userFacade;

  /**
   * Returns admin information about all the projects
   *
   * @return
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProjectsAdminInfo(@BeanParam ExpansionBeanParam expansionBeanParam,
                                       @Context HttpServletRequest req,
                                       @Context UriInfo uriInfo,
                                       @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROJECT_ADMIN);
    resourceRequest.setExpansions(expansionBeanParam.getResources());
    ProjectAdminInfoDTO projectAdminInfoDTO = projectsAdminBuilder.build(uriInfo, resourceRequest);
    return Response.ok().entity(projectAdminInfoDTO).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{projectId}")
  public Response getProjectAdminInfo(@PathParam("projectId") Integer projectId,
                                      @BeanParam ExpansionBeanParam expansionBeanParam,
                                      @Context HttpServletRequest req,
                                      @Context UriInfo uriInfo,
                                      @Context SecurityContext sc) throws ProjectException {
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROJECT_ADMIN);
    resourceRequest.setExpansions(expansionBeanParam.getResources());
    ProjectAdminInfoDTO projectAdminInfoDTO = projectsAdminBuilder.build(uriInfo, project, resourceRequest);
    return Response.ok().entity(projectAdminInfoDTO).build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("{projectId}")
  public Response setProjectAdminInfo(@PathParam("projectId") Integer projectId,
                                      ProjectAdminInfoDTO projectAdminInfoDTO,
                                      @Context HttpServletRequest req,
                                      @Context UriInfo uriInfo,
                                      @Context SecurityContext sc) throws ProjectException {
    Project project = projectController.findProjectById(projectId);
    project = projectQuotasController.updateQuotas(project,
        projectAdminInfoDTO.getProjectQuotas(),
        projectAdminInfoDTO.getPaymentType());

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.PROJECT_ADMIN);
    resourceRequest.setExpansions(Collections.singleton(new ResourceRequest(ResourceRequest.Name.QUOTAS)));

    ProjectAdminInfoDTO newProjectAdminInfoDTO = projectsAdminBuilder.build(uriInfo, project, resourceRequest);
    return Response.ok().entity(newProjectAdminInfoDTO).build();
  }

  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{projectId}")
  public Response deleteProject(@Context HttpServletRequest req,
                                @Context UriInfo uriInfo,
                                @Context SecurityContext sc,
                                @PathParam("projectId") Integer id,
                                @QueryParam("force") @DefaultValue("false") Boolean force)
      throws ProjectException, GenericException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (force) {
      Project project = projectFacade.find(id);
      if (project != null) {
        projectController.forceCleanup(project.getName(), user);
      }
    } else {
      projectController.removeProject(user.getEmail(), id);
    }

    LOGGER.log(Level.INFO, "Deleted project with id: " + id);
    return Response.ok().build();
  }

  @POST
  @Path("{projectId}/fix-permission")
  @Produces(MediaType.APPLICATION_JSON)
  public Response forcePermissionFix(@PathParam("projectId") Integer projectId,
                                     @Context HttpServletRequest req,
                                     @Context SecurityContext sc)
    throws ProjectException {
    Project project = projectFacade.find(projectId);
    if (project == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE, "projectId: " + projectId);
    }
    permissionsFixer.fixPermissions(project);
    return Response.noContent().build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/createas")
  public Response createProjectAsUser(@Context HttpServletRequest request, @Context SecurityContext sc,
                                      ProjectDTO projectDTO)
      throws DatasetException, GenericException, KafkaException, ProjectException, UserException,
      ServiceException, HopsSecurityException, FeaturestoreException,
      OpenSearchException, SchemaException, IOException {
    Users user = jWTHelper.getUserPrincipal(sc);
    if (user == null) {
      throw new UserException(RESTCodes.UserErrorCode.AUTHENTICATION_FAILURE, Level.WARNING,
          "Unauthorized or unknown user tried to create a Project as another user");
    }

    String username = projectDTO.getOwner();
    if (username == null) {
      LOGGER.log(Level.WARNING, "Owner username is null");
      throw new IllegalArgumentException("Owner email cannot be null");
    }

    Users owner = userFacade.findByUsername(username);
    if (owner == null) {
      throw new UserException(RESTCodes.UserErrorCode.USER_DOES_NOT_EXIST, Level.FINE, "user:" + username);
    }

    projectController.createProject(projectDTO, owner);
    return Response.ok().build();
  }
  
  @POST
  @Path("fix-permission")
  @Produces(MediaType.APPLICATION_JSON)
  public Response forcePermissionFix(@Context HttpServletRequest req, @Context SecurityContext sc) {
    permissionsFixer.fixPermissions();
    return Response.accepted().build();
  }
}
