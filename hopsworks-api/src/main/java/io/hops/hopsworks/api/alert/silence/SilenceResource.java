/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.alert.silence;

import io.hops.hopsworks.alert.AlertManager;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.SilenceID;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Api(value = "Silence Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class SilenceResource {
  
  private static final Logger LOGGER = Logger.getLogger(SilenceResource.class.getName());
  
  @EJB
  private SilenceBuilder silenceBuilder;
  @EJB
  private AlertManager alertManager;
  @EJB
  private ProjectController projectController;
  @EJB
  private JWTHelper jWTHelper;
  
  private Integer projectId;
  private String projectName;
  
  @Logged(logLevel = LogLevel.OFF)
  public void setProjectId(Integer projectId) {
    this.projectId = projectId;
  }
  
  @Logged(logLevel = LogLevel.OFF)
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }
  
  private Project getProject() throws ProjectException {
    if (this.projectId != null) {
      return projectController.findProjectById(this.projectId);
    } else if (this.projectName != null) {
      return projectController.findProjectByName(this.projectName);
    }
    throw new ProjectException(RESTCodes.ProjectErrorCode.PROJECT_NOT_FOUND, Level.FINE);
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all silences.", response = SilenceDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination, @BeanParam SilenceBeanParam silenceBeanParam,
                      @Context HttpServletRequest req,
                      @Context UriInfo uriInfo,
                      @Context SecurityContext sc) throws AlertException, ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.SILENCES);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    SilenceDTO dto = silenceBuilder.buildItems(uriInfo, resourceRequest, silenceBeanParam, getProject());
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("{silenceId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Find silence by Id.", response = SilenceDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getById(@PathParam("silenceId") String silenceId, @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @Context SecurityContext sc) throws AlertException, ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.SILENCES);
    SilenceDTO dto = silenceBuilder.build(uriInfo, resourceRequest, silenceId, getProject());
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create a silence.", response = SilenceDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response create(PostableSilenceDTO postableSilenceDTO, @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc) throws AlertException, ProjectException {
    if (postableSilenceDTO == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    Project project = getProject();
    postableSilenceDTO.setId(null);
    SilenceID silenceID = postSilence(postableSilenceDTO, project, user);
    SilenceDTO dto = silenceBuilder.build(uriInfo,
        new ResourceRequest(ResourceRequest.Name.SILENCES),
        silenceID.getSilenceID(), project);
    dto.setHref(uriInfo.getAbsolutePathBuilder().path(silenceID.getSilenceID()).build());
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  @PUT
  @Path("{silenceId}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a silence.", response = SilenceDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response update(@PathParam("silenceId") String silenceId, PostableSilenceDTO postableSilenceDTO,
                         @Context HttpServletRequest req,
                         @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws AlertException, ProjectException {
    if (postableSilenceDTO == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Users user = jWTHelper.getUserPrincipal(sc);
    postableSilenceDTO.setId(silenceId);
    Project project = getProject();
    SilenceID silenceID = postSilence(postableSilenceDTO, project, user);
    SilenceDTO dto = silenceBuilder.build(uriInfo, new ResourceRequest(ResourceRequest.Name.SILENCES),
        silenceID.getSilenceID(), project);
    return Response.ok().entity(dto).build();
  }
  
  private SilenceID postSilence(PostableSilenceDTO postableSilenceDTO, Project project, Users user)
      throws AlertException {
    try {
      return alertManager.postSilences(silenceBuilder.getPostableSilence(postableSilenceDTO), project, user);
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
  }
  
  @DELETE
  @Path("{silenceId}")
  @ApiOperation(value = "Delete silence by Id.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteById(@PathParam("silenceId") String silenceId, @Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc) throws AlertException, ProjectException {
    try {
      return alertManager.deleteSilence(silenceId, getProject());
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
  }
  
}