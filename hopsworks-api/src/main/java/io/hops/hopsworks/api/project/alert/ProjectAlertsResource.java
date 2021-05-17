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

package io.hops.hopsworks.api.project.alert;

import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.alert.AlertBuilder;
import io.hops.hopsworks.api.alert.AlertDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.alert.ProjectServiceAlertsFacade;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
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
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Api(value = "Project Alerts Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ProjectAlertsResource {
  
  private static final Logger LOGGER = Logger.getLogger(ProjectAlertsResource.class.getName());
  
  @EJB
  private ProjectAlertsBuilder projectAlertsBuilder;
  @EJB
  private ProjectServiceAlertsFacade projectServiceAlertsFacade;
  @EJB
  private ProjectController projectController;
  @EJB
  private AlertController alertController;
  @EJB
  private AlertBuilder alertBuilder;
  
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
  @ApiOperation(value = "Get all alerts.", response = ProjectAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response get(@BeanParam Pagination pagination, @BeanParam ProjectAlertsBeanParam projectAlertsBeanParam,
      @Context UriInfo uriInfo, @Context SecurityContext sc) throws ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(projectAlertsBeanParam.getSortBySet());
    resourceRequest.setFilter(projectAlertsBeanParam.getFilter());
    ProjectAlertsDTO dto = projectAlertsBuilder.buildItems(uriInfo, resourceRequest, getProject());
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Find alert by Id.", response = ProjectAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getById(@PathParam("id") Integer id, @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    ProjectAlertsDTO dto = projectAlertsBuilder.build(uriInfo, resourceRequest, getProject(), id);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("values")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get values for services alert.", response = ProjectServiceAlertValues.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getAvailableServices(@Context UriInfo uriInfo, @Context SecurityContext sc)  {
    ProjectServiceAlertValues values = new ProjectServiceAlertValues();
    return Response.ok().entity(values).build();
  }
  
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update an alert.", response = ProjectAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response createOrUpdate(@PathParam("id") Integer id, ProjectAlertsDTO projectAlertsDTO,
      @Context UriInfo uriInfo, @Context SecurityContext sc) throws ProjectException {
    ProjectServiceAlert projectServiceAlert = projectServiceAlertsFacade.findByProjectAndId(getProject(), id);
    if (projectServiceAlert == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_NOT_FOUND, Level.FINE,
          "Alert not found. Id=" + id.toString());
    }
    if (projectAlertsDTO == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    if (projectAlertsDTO.getAlertType() != null) {
      if (AlertType.SYSTEM_ALERT.equals(projectAlertsDTO.getAlertType())) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
            "AlertType can not be " + AlertType.SYSTEM_ALERT);
      }
      projectServiceAlert.setAlertType(projectAlertsDTO.getAlertType());
    }
    Project project = getProject();
    if (projectAlertsDTO.getStatus() != null) {
      if (!projectAlertsDTO.getStatus().equals(projectServiceAlert.getStatus()) &&
          projectServiceAlertsFacade.findByProjectAndStatus(project, projectAlertsDTO.getStatus()) != null) {
        throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ALREADY_EXISTS, Level.FINE,
            "Alert with projectId=" + project.getId() + " status=" + projectAlertsDTO.getStatus() + " already exists.");
      }
      projectServiceAlert.setStatus(projectAlertsDTO.getStatus());
    }
    if (projectAlertsDTO.getSeverity() != null) {
      projectServiceAlert.setSeverity(projectAlertsDTO.getSeverity());
    }
    projectServiceAlert = projectServiceAlertsFacade.update(projectServiceAlert);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    ProjectAlertsDTO dto = projectAlertsBuilder.build(uriInfo, resourceRequest, projectServiceAlert);
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create an alert.", response = ProjectAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response create(ProjectAlertsDTO projectAlertsDTO, @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws ProjectException {
    validate(projectAlertsDTO);
    Project project = getProject();
    ProjectServiceAlert projectServiceAlert = new ProjectServiceAlert();
    projectServiceAlert.setAlertType(projectAlertsDTO.getAlertType());
    projectServiceAlert.setStatus(projectAlertsDTO.getStatus());
    projectServiceAlert.setSeverity(projectAlertsDTO.getSeverity());
    projectServiceAlert.setService(projectAlertsDTO.getService());
    projectServiceAlert.setCreated(new Date());
    projectServiceAlert.setProject(project);
    projectServiceAlertsFacade.save(projectServiceAlert);
    projectServiceAlert = projectServiceAlertsFacade.findByProjectAndStatus(project, projectAlertsDTO.getStatus());
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    ProjectAlertsDTO dto = projectAlertsBuilder.buildItems(uriInfo, resourceRequest, projectServiceAlert);
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  private void validate(ProjectAlertsDTO projectAlertsDTO) throws ProjectException {
    if (projectAlertsDTO == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    if (projectAlertsDTO.getAlertType() == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Type can not be empty.");
    }
    if (AlertType.SYSTEM_ALERT.equals(projectAlertsDTO.getAlertType())) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "AlertType can not be " + AlertType.SYSTEM_ALERT);
    }
    if (projectAlertsDTO.getStatus() == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Status can not be empty.");
    }
    if (projectAlertsDTO.getService() == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Service name can not be empty.");
    }
    if (!ProjectServiceEnum.FEATURESTORE.equals(projectAlertsDTO.getService()) &&
        !ProjectServiceEnum.JOBS.equals(projectAlertsDTO.getService())) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Service not supported.");
    }
    if (ProjectServiceEnum.FEATURESTORE.equals(projectAlertsDTO.getService()) &&
        !projectAlertsDTO.getStatus().isFeatureGroupStatus()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Status not applicable for service.");
    }
    if (ProjectServiceEnum.JOBS.equals(projectAlertsDTO.getService()) &&
        !projectAlertsDTO.getStatus().isJobStatus()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Status not applicable for service.");
    }
    if (projectAlertsDTO.getSeverity() == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE, "Severity can not be " +
          "empty.");
    }
    Project project = getProject();
    ProjectServiceAlert projectServiceAlert =
        projectServiceAlertsFacade.findByProjectAndStatus(project, projectAlertsDTO.getStatus());
    if (projectServiceAlert != null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ALREADY_EXISTS, Level.FINE,
          "Alert with projectId=" + project.getId() + " status=" + projectAlertsDTO.getStatus() + " already " +
              "exists.");
    }
  }
  
  @POST
  @Path("{id}/test")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Test alert by Id.", response = ProjectAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getTestById(@PathParam("id") Integer id, @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws ProjectException, AlertException {
    Project project = getProject();
    ProjectServiceAlert projectServiceAlert = projectServiceAlertsFacade.findByProjectAndId(project, id);
    List<Alert> alerts;
    try {
      alerts = alertController.testAlert(project, projectServiceAlert);
    } catch (AlertManagerUnreachableException | AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    AlertDTO alertDTO = alertBuilder.getAlertDTOs(uriInfo, resourceRequest, alerts, project);
    return Response.ok().entity(alertDTO).build();
  }
  
  @DELETE
  @Path("{id}")
  @ApiOperation(value = "Delete an alert by Id.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response deleteById(@PathParam("id") Integer id, @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws ProjectException {
    ProjectServiceAlert projectServiceAlert = projectServiceAlertsFacade.findByProjectAndId(getProject(), id);
    if (projectServiceAlert != null) {
      projectServiceAlertsFacade.remove(projectServiceAlert);
    }
    return Response.noContent().build();
  }
  
}