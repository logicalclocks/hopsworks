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

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.dao.AlertReceiverFacade;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.alert.AlertBuilder;
import io.hops.hopsworks.api.alert.AlertDTO;
import io.hops.hopsworks.api.featurestore.datavalidation.alert.FeatureGroupAlertBuilder;
import io.hops.hopsworks.api.featurestore.datavalidation.alert.FeatureGroupAlertDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.api.jobs.alert.JobAlertsBuilder;
import io.hops.hopsworks.api.jobs.alert.JobAlertsDTO;
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
import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlertStatus;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
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
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  @EJB
  private JobAlertsBuilder jobAlertsBuilder;
  @EJB
  private FeatureGroupAlertBuilder featureGroupAlertBuilder;
  @EJB
  private AlertReceiverFacade alertReceiverFacade;
  
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
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response get(@BeanParam Pagination pagination, @BeanParam ProjectAlertsBeanParam projectAlertsBeanParam,
                      @Context HttpServletRequest req,
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
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getById(@PathParam("id") Integer id, @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @Context SecurityContext sc)
      throws ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    ProjectAlertsDTO dto = projectAlertsBuilder.build(uriInfo, resourceRequest, getProject(), id);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("values")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get values for services alert.", response = ProjectServiceAlertValues.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAvailableServices(@Context UriInfo uriInfo,
                                       @Context HttpServletRequest req,
                                       @Context SecurityContext sc)  {
    ProjectServiceAlertValues values = new ProjectServiceAlertValues();
    return Response.ok().entity(values).build();
  }
  
  @GET
  @Path("all")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get project, job and feature group alerts.", response = ProjectAllAlertsDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response getAllAlerts(@Context UriInfo uriInfo,
                               @Context HttpServletRequest req,
                               @Context SecurityContext sc) throws ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    ProjectAlertsDTO projectAlertsDTO = projectAlertsBuilder.buildItemsAll(uriInfo, resourceRequest, getProject());
    JobAlertsDTO jobAlertsDTO = jobAlertsBuilder.buildItems(uriInfo, resourceRequest, getProject());
    FeatureGroupAlertDTO featureGroupAlertDTO =
        featureGroupAlertBuilder.buildItems(uriInfo, resourceRequest, getProject());
    ProjectAllAlertsDTO projectAllAlertsDTO = new ProjectAllAlertsDTO();
    projectAllAlertsDTO.setProjectAlerts(projectAlertsDTO);
    projectAllAlertsDTO.setJobAlerts(jobAlertsDTO);
    projectAllAlertsDTO.setFeatureGroupAlerts(featureGroupAlertDTO);
    return Response.ok().entity(projectAllAlertsDTO).build();
  }
  
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update an alert.", response = ProjectAlertsDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response createOrUpdate(@PathParam("id") Integer id, ProjectAlertsDTO projectAlertsDTO,
                                 @Context HttpServletRequest req,
                                 @Context UriInfo uriInfo, @Context SecurityContext sc) throws ProjectException {
    ProjectServiceAlert projectServiceAlert = projectServiceAlertsFacade.findByProjectAndId(getProject(), id);
    if (projectServiceAlert == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_NOT_FOUND, Level.FINE,
          "Alert not found. Id=" + id.toString());
    }
    if (projectAlertsDTO == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
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
    if (!projectServiceAlert.getReceiver().getName().equals(projectAlertsDTO.getReceiver())) {
      deleteRoute(projectServiceAlert);
      projectServiceAlert.setReceiver(getReceiver(projectAlertsDTO.getReceiver()));
      projectServiceAlert.setAlertType(alertController.getAlertType(projectServiceAlert.getReceiver()));
      createRoute(projectServiceAlert);
    }
    projectServiceAlert = projectServiceAlertsFacade.update(projectServiceAlert);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    ProjectAlertsDTO dto = projectAlertsBuilder.build(uriInfo, resourceRequest, projectServiceAlert);
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create an alert.", response = PostableProjectAlerts.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  public Response create(PostableProjectAlerts projectAlertsDTO,
                         @QueryParam("bulk") @DefaultValue("false") Boolean bulk,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc) throws ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    ProjectAlertsDTO dto = createAlert(projectAlertsDTO, bulk, uriInfo, resourceRequest);
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  private ProjectAlertsDTO createAlert(PostableProjectAlerts projectAlertsDTO, Boolean bulk, UriInfo uriInfo,
      ResourceRequest resourceRequest) throws ProjectException {
    ProjectAlertsDTO dto;
    Project project = getProject();
    if (bulk) {
      validateBulk(projectAlertsDTO);
      dto = new ProjectAlertsDTO();
      for (PostableProjectAlerts pa : projectAlertsDTO.getItems()) {
        dto.addItem(createAlert(pa, uriInfo, project, resourceRequest));
      }
      dto.setCount((long) projectAlertsDTO.getItems().size());
    } else {
      dto = createAlert(projectAlertsDTO, uriInfo, project, resourceRequest);
    }
    return dto;
  }
  
  private void validateBulk(PostableProjectAlerts projectAlertsDTO) throws ProjectException {
    if (projectAlertsDTO.getItems() == null || projectAlertsDTO.getItems().size() < 1) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Set<ProjectServiceAlertStatus> statusSet = new HashSet<>();
    for (PostableProjectAlerts dto : projectAlertsDTO.getItems()) {
      statusSet.add(dto.getStatus());
    }
    if (statusSet.size() < projectAlertsDTO.getItems().size()) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE, "Duplicate alert.");
    }
  }
  
  private ProjectAlertsDTO createAlert(PostableProjectAlerts projectAlertsDTO, UriInfo uriInfo, Project project,
      ResourceRequest resourceRequest) throws ProjectException {
    validate(projectAlertsDTO);
    ProjectServiceAlert projectServiceAlert = new ProjectServiceAlert();
    projectServiceAlert.setStatus(projectAlertsDTO.getStatus());
    projectServiceAlert.setSeverity(projectAlertsDTO.getSeverity());
    projectServiceAlert.setService(projectAlertsDTO.getService());
    projectServiceAlert.setCreated(new Date());
    projectServiceAlert.setProject(project);
    projectServiceAlert.setReceiver(getReceiver(projectAlertsDTO.getReceiver()));
    projectServiceAlert.setAlertType(alertController.getAlertType(projectServiceAlert.getReceiver()));
    createRoute(projectServiceAlert);
    projectServiceAlertsFacade.save(projectServiceAlert);
    projectServiceAlert = projectServiceAlertsFacade.findByProjectAndStatus(project, projectAlertsDTO.getStatus());
    return projectAlertsBuilder.buildItems(uriInfo, resourceRequest, projectServiceAlert);
  }
  
  private AlertReceiver getReceiver(String name) throws ProjectException {
    if (Strings.isNullOrEmpty(name)) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Receiver can not be empty.");
    }
    Optional<AlertReceiver> alertReceiver = alertReceiverFacade.findByName(name);
    if (alertReceiver.isPresent()) {
      return alertReceiver.get();
    }
    throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        "Alert receiver not found " + name);
  }
  
  private void createRoute(ProjectServiceAlert projectServiceAlert) throws ProjectException {
    try {
      alertController.createRoute(projectServiceAlert);
    } catch ( AlertManagerClientCreateException | AlertManagerConfigReadException |
        AlertManagerConfigCtrlCreateException | AlertManagerConfigUpdateException | AlertManagerNoSuchElementException |
        AlertManagerAccessControlException | AlertManagerUnreachableException e) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.FAILED_TO_CREATE_ROUTE, Level.FINE, e.getMessage());
    }
  }
  
  private void validate(PostableProjectAlerts projectAlertsDTO) throws ProjectException {
    if (projectAlertsDTO == null) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
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
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTestById(@PathParam("id") Integer id, @Context UriInfo uriInfo,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc)
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
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteById(@PathParam("id") Integer id,
                             @Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc)
      throws ProjectException {
    ProjectServiceAlert projectServiceAlert = projectServiceAlertsFacade.findByProjectAndId(getProject(), id);
    if (projectServiceAlert != null) {
      deleteRoute(projectServiceAlert);
      projectServiceAlertsFacade.remove(projectServiceAlert);
    }
    return Response.noContent().build();
  }
  
  private void deleteRoute(ProjectServiceAlert projectServiceAlert) throws ProjectException {
    try {
      alertController.deleteRoute(projectServiceAlert);
    } catch (AlertManagerUnreachableException | AlertManagerAccessControlException | AlertManagerConfigUpdateException |
        AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException | AlertManagerClientCreateException e) {
      throw new ProjectException(RESTCodes.ProjectErrorCode.FAILED_TO_DELETE_ROUTE, Level.FINE, e.getMessage());
    }
  }
  
}