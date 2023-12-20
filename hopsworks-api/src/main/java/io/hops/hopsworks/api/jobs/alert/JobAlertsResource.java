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

package io.hops.hopsworks.api.jobs.alert;

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
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.api.project.alert.ProjectAlertsDTO;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.jobs.description.JobAlertsFacade;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlert;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlertStatus;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
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
@Api(value = "JobAlerts Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class JobAlertsResource {
  
  private static final Logger LOGGER = Logger.getLogger(JobAlertsResource.class.getName());
  
  @EJB
  private JobAlertsBuilder jobalertsBuilder;
  @EJB
  private JobAlertsFacade jobalertsFacade;
  @EJB
  private AlertController alertController;
  @EJB
  private AlertBuilder alertBuilder;
  @EJB
  private AlertReceiverFacade alertReceiverFacade;
  
  private Jobs job;
  
  @Logged(logLevel = LogLevel.OFF)
  public JobAlertsResource setJob(Jobs job) {
    this.job = job;
    return this;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all alerts.", response = JobAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination, @BeanParam JobAlertsBeanParam jobalertsBeanParam,
                      @Context HttpServletRequest req,
                      @Context UriInfo uriInfo, @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(jobalertsBeanParam.getSortBySet());
    resourceRequest.setFilter(jobalertsBeanParam.getFilter());
    JobAlertsDTO dto = jobalertsBuilder.buildItems(uriInfo, resourceRequest, job);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Find alert by Id.", response = JobAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getById(@PathParam("id") Integer id, @Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @Context SecurityContext sc)
      throws JobException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    JobAlertsDTO dto = jobalertsBuilder.build(uriInfo, resourceRequest, job, id);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("values")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get values for job alert.", response = JobAlertValues.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getAvailableServices(@Context UriInfo uriInfo,
                                       @Context HttpServletRequest req,
                                       @Context SecurityContext sc)  {
    JobAlertValues values = new JobAlertValues();
    return Response.ok().entity(values).build();
  }
  
  @PUT
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update an alert.", response = JobAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response createOrUpdate(@PathParam("id") Integer id, JobAlertsDTO jobAlertsDTO,
                                 @Context HttpServletRequest req,
                                 @Context UriInfo uriInfo,
                                 @Context SecurityContext sc) throws JobException {
    JobAlert jobAlert = jobalertsFacade.findByJobAndId(job, id);
    if (jobAlert == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_NOT_FOUND, Level.FINE,
          "Job alert not found. Id=" + id.toString());
    }
    if (jobAlertsDTO == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    if (jobAlertsDTO.getStatus() != null) {
      if (!jobAlertsDTO.getStatus().equals(jobAlert.getStatus()) && jobalertsFacade.findByJobAndStatus(job,
          jobAlertsDTO.getStatus()) != null) {
        throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ALREADY_EXISTS, Level.FINE,
            "Job alert with jobId=" + job.getId() + " status=" + jobAlertsDTO.getStatus() + " already exists.");
      }
      jobAlert.setStatus(jobAlertsDTO.getStatus());
    }
    if (jobAlertsDTO.getSeverity() != null) {
      jobAlert.setSeverity(jobAlertsDTO.getSeverity());
    }
    if (!jobAlert.getReceiver().getName().equals(jobAlertsDTO.getReceiver())) {
      deleteRoute(jobAlert);
      jobAlert.setReceiver(getReceiver(jobAlertsDTO.getReceiver()));
      createRoute(jobAlert);
    }
    jobAlert.setAlertType(alertController.getAlertType(jobAlert.getReceiver()));
    jobAlert = jobalertsFacade.update(jobAlert);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    JobAlertsDTO dto = jobalertsBuilder.build(uriInfo, resourceRequest, jobAlert);
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create an alert.", response = PostableJobAlerts.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response create(PostableJobAlerts jobAlertsDTO,
                         @QueryParam("bulk") @DefaultValue("false") Boolean bulk,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc) throws JobException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    JobAlertsDTO dto = createAlert(jobAlertsDTO, bulk, uriInfo, resourceRequest);
    return Response.created(dto.getHref()).entity(dto).build();
  }
  
  private JobAlertsDTO createAlert(PostableJobAlerts jobAlertsDTO, Boolean bulk, UriInfo uriInfo,
      ResourceRequest resourceRequest) throws JobException {
    JobAlertsDTO dto;
    if (bulk) {
      validateBulk(jobAlertsDTO);
      dto = new JobAlertsDTO();
      for (PostableJobAlerts pa : jobAlertsDTO.getItems()) {
        dto.addItem(createAlert(pa, uriInfo, resourceRequest));
      }
      dto.setCount((long) jobAlertsDTO.getItems().size());
    } else {
      dto = createAlert(jobAlertsDTO, uriInfo, resourceRequest);
    }
    return dto;
  }
  
  private void validateBulk(PostableJobAlerts jobAlertsDTO) throws JobException {
    if (jobAlertsDTO.getItems() == null || jobAlertsDTO.getItems().size() < 1) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Set<JobAlertStatus> statusSet = new HashSet<>();
    for (PostableJobAlerts dto : jobAlertsDTO.getItems()) {
      statusSet.add(dto.getStatus());
    }
    if (statusSet.size() < jobAlertsDTO.getItems().size()) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ILLEGAL_ARGUMENT, Level.FINE, "Duplicate alert.");
    }
  }
  
  private JobAlertsDTO createAlert(PostableJobAlerts jobAlertsDTO, UriInfo uriInfo,
                                   ResourceRequest resourceRequest) throws JobException {
    validate(jobAlertsDTO);
    JobAlert jobAlert = new JobAlert();
    jobAlert.setStatus(jobAlertsDTO.getStatus());
    jobAlert.setSeverity(jobAlertsDTO.getSeverity());
    jobAlert.setCreated(new Date());
    jobAlert.setJobId(job);
    jobAlert.setReceiver(getReceiver(jobAlertsDTO.getReceiver()));
    jobAlert.setAlertType(alertController.getAlertType(jobAlert.getReceiver()));
    createRoute(jobAlert);
    jobalertsFacade.save(jobAlert);
    jobAlert = jobalertsFacade.findByJobAndStatus(job, jobAlertsDTO.getStatus());
    return jobalertsBuilder.buildItems(uriInfo, resourceRequest, jobAlert);
  }
  
  private void validate(PostableJobAlerts jobAlertsDTO) throws JobException {
    if (jobAlertsDTO == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    if (jobAlertsDTO.getStatus() == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ILLEGAL_ARGUMENT, Level.FINE, "Status can not be empty.");
    }
    if (jobAlertsDTO.getSeverity() == null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ILLEGAL_ARGUMENT, Level.FINE, "Severity can not be " +
          "empty.");
    }
    JobAlert jobAlert = jobalertsFacade.findByJobAndStatus(job, jobAlertsDTO.getStatus());
    if (jobAlert != null) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ALREADY_EXISTS, Level.FINE,
          "Job alert with jobId=" + job.getId() + " status=" + jobAlertsDTO.getStatus() + " already exists.");
    }
  }
  
  @POST
  @Path("{id}/test")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Test alert by Id.", response = ProjectAlertsDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getTestById(@PathParam("id") Integer id,
                              @Context UriInfo uriInfo,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc)
      throws AlertException {
    JobAlert jobAlert = jobalertsFacade.findByJobAndId(job, id);
    List<Alert> alerts;
    try {
      alerts = alertController.testAlert(job.getProject(), jobAlert);
    } catch (AlertManagerUnreachableException | AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    AlertDTO alertDTO = alertBuilder.getAlertDTOs(uriInfo, resourceRequest, alerts, job.getProject());
    return Response.ok().entity(alertDTO).build();
  }
  
  @DELETE
  @Path("{id}")
  @ApiOperation(value = "Delete alert by Id.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteById(@PathParam("id") Integer id,
                             @Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc) throws JobException {
    JobAlert jobAlert = jobalertsFacade.findByJobAndId(job, id);
    if (jobAlert != null) {
      deleteRoute(jobAlert);
      jobalertsFacade.remove(jobAlert);
    }
    return Response.noContent().build();
  }
  
  private AlertReceiver getReceiver(String name) throws JobException {
    if (Strings.isNullOrEmpty(name)) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Receiver can not be empty.");
    }
    Optional<AlertReceiver> alertReceiver = alertReceiverFacade.findByName(name);
    if (!alertReceiver.isPresent()) {
      throw new JobException(RESTCodes.JobErrorCode.JOB_ALERT_ILLEGAL_ARGUMENT, Level.FINE,
          "Alert receiver not found " + name);
    }
    return alertReceiver.get();
  }
  
  private void createRoute(JobAlert jobAlert) throws JobException {
    try {
      alertController.createRoute(jobAlert);
    } catch ( AlertManagerClientCreateException | AlertManagerConfigReadException |
        AlertManagerConfigCtrlCreateException | AlertManagerConfigUpdateException | AlertManagerNoSuchElementException |
        AlertManagerAccessControlException | AlertManagerUnreachableException e) {
      throw new JobException(RESTCodes.JobErrorCode.FAILED_TO_CREATE_ROUTE, Level.FINE, e.getMessage());
    }
  }
  
  private void deleteRoute(JobAlert jobAlert) throws JobException {
    try {
      alertController.deleteRoute(jobAlert);
    } catch (AlertManagerUnreachableException | AlertManagerAccessControlException | AlertManagerConfigUpdateException |
        AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException | AlertManagerClientCreateException e) {
      throw new JobException(RESTCodes.JobErrorCode.FAILED_TO_DELETE_ROUTE, Level.FINE, e.getMessage());
    }
  }
  
}