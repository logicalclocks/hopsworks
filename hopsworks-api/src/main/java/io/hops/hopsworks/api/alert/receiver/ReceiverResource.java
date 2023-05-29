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

package io.hops.hopsworks.api.alert.receiver;

import io.hops.hopsworks.alert.AMClient;
import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.api.alert.dto.ReceiverName;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.alert.silence.SilenceDTO;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.project.Project;
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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Api(value = "Alert Receiver Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ReceiverResource {

  private static final Logger LOGGER = Logger.getLogger(ReceiverResource.class.getName());

  @EJB
  private ReceiverBuilder receiverBuilder;
  @EJB
  private ProjectController projectController;
  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;
  @EJB
  private AMClient alertManager;

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
  @ApiOperation(value = "Get all receivers.", response = ReceiverDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response get(@BeanParam Pagination pagination, @BeanParam ReceiverBeanParam receiverBeanParam,
                      @QueryParam("global") @DefaultValue("false") Boolean includeGlobal,
                      @QueryParam("expand") @DefaultValue("false") Boolean expand,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc) throws AlertException, ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    ReceiverDTO dto =
        receiverBuilder.buildItems(uriInfo, resourceRequest, receiverBeanParam, getProject(), includeGlobal, expand);
    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get receiver by name.", response = ReceiverDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getByName(@PathParam("name") String name,
                            @Context UriInfo uriInfo,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc)
      throws AlertException, ProjectException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    ReceiverDTO dto = receiverBuilder.build(uriInfo, resourceRequest, name, getProject());
    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("default")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Check if default receiver configured.", response = GlobalReceiverDefaults.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getDefaults(@Context UriInfo uriInfo,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc)
      throws AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException {
    return Response.ok().entity(receiverBuilder.build()).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create a receiver.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response create(PostableReceiverDTO postableReceiverDTO,
                         @QueryParam("defaultTemplate") @DefaultValue("false") Boolean defaultTemplate,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws AlertException, ProjectException {
    if (postableReceiverDTO == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Receiver receiver = receiverBuilder.build(postableReceiverDTO, defaultTemplate, true);
    validateReceiverOneConfig(receiver);
    try {
      alertManagerConfiguration.addReceiver(receiver, getProject());
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerUnreachableException |
        AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerDuplicateEntryException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_EXIST, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    ReceiverDTO dto = receiverBuilder.build(uriInfo, resourceRequest, receiver.getName(), getProject());
    dto.setHref(uriInfo.getAbsolutePathBuilder().path(receiver.getName()).build());
    return Response.created(dto.getHref()).entity(dto).build();
  }

  @PUT
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a receiver.", response = SilenceDTO.class)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response update(@PathParam("name") String name,
                         @QueryParam("defaultTemplate") @DefaultValue("false") Boolean defaultTemplate,
                         PostableReceiverDTO postableReceiverDTO,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws AlertException, ProjectException {
    if (postableReceiverDTO == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Receiver receiver = receiverBuilder.build(postableReceiverDTO, defaultTemplate, true);
    validateReceiverOneConfig(receiver);
    try {
      alertManagerConfiguration.updateReceiver(name, receiver, getProject());
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerUnreachableException |
        AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerDuplicateEntryException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_EXIST, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerNoSuchElementException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_NOT_FOUND, Level.FINE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    ReceiverDTO dto = receiverBuilder.build(uriInfo, resourceRequest, receiver.getName(), getProject());
    return Response.ok().entity(dto).build();
  }

  @DELETE
  @Path("{name}")
  @ApiOperation(value = "Delete receiver by name.")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response deleteById(@PathParam("name") String name,
                             @QueryParam("cascade") @DefaultValue("false") Boolean cascade,
                             @Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc) throws AlertException, ProjectException {
    Project project = getProject();
    try {
      List<ReceiverName> receiverNameList = alertManager.getReceivers(project, false);
      if (!receiverNameList.isEmpty() && receiverNameList.contains(new ReceiverName(name))) {
        alertManagerConfiguration.removeReceiver(name, project, cascade);
      }
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerUnreachableException |
        AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    }
    return Response.noContent().build();
  }
  
  private void validateReceiverOneConfig(Receiver receiver) throws AlertException {
    int count = 0;
    if (receiver.getEmailConfigs() != null && !receiver.getEmailConfigs().isEmpty()) {
      count++;
    }
    if (receiver.getSlackConfigs() != null && !receiver.getSlackConfigs().isEmpty()) {
      count++;
    }
    if (receiver.getPagerdutyConfigs() != null && !receiver.getPagerdutyConfigs().isEmpty()) {
      count++;
    }
    if (receiver.getOpsgenieConfigs() != null && !receiver.getOpsgenieConfigs().isEmpty()) {
      count++;
    }
    if (receiver.getPushoverConfigs() != null && !receiver.getPushoverConfigs().isEmpty()) {
      count++;
    }
    if (receiver.getVictoropsConfigs() != null && !receiver.getVictoropsConfigs().isEmpty()) {
      count++;
    }
    if (receiver.getWebhookConfigs() != null && !receiver.getWebhookConfigs().isEmpty()) {
      count++;
    }
    if (receiver.getWechatConfigs() != null && !receiver.getWechatConfigs().isEmpty()) {
      count++;
    }
    if (count > 1) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Receiver should set only one " +
          "configuration.");
    }
  }
}