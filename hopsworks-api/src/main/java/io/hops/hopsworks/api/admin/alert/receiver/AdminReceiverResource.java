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

package io.hops.hopsworks.api.admin.alert.receiver;

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.api.alert.receiver.GlobalReceiverDefaults;
import io.hops.hopsworks.api.alert.receiver.PostableReceiverDTO;
import io.hops.hopsworks.api.alert.receiver.ReceiverBeanParam;
import io.hops.hopsworks.api.alert.receiver.ReceiverBuilder;
import io.hops.hopsworks.api.alert.receiver.ReceiverDTO;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
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
import java.util.logging.Level;

@Logged
@Api(value = "Receiver Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AdminReceiverResource {
  @EJB
  private ReceiverBuilder receiverBuilder;
  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;
  @EJB
  private AlertController alertController;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all receivers.", response = ReceiverDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response get(@BeanParam Pagination pagination,
                      @BeanParam ReceiverBeanParam receiverBeanParam,
                      @QueryParam("expand") @DefaultValue("false") Boolean expand,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc) throws AlertException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    ReceiverDTO dto = receiverBuilder.buildItems(uriInfo, resourceRequest, receiverBeanParam, expand);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get receiver by name.", response = ReceiverDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response getByName(@PathParam("name") String name,
                            @Context UriInfo uriInfo,
                            @Context HttpServletRequest req,
                            @Context SecurityContext sc) throws AlertException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    ReceiverDTO dto = receiverBuilder.build(uriInfo, resourceRequest, name, null);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("default")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Check if default receiver configured.", response = GlobalReceiverDefaults.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response getDefaults(@Context UriInfo uriInfo,
                              @Context HttpServletRequest req,
                              @Context SecurityContext sc)
      throws AlertManagerConfigReadException {
    return Response.ok().entity(receiverBuilder.build()).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create a receiver.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response create(PostableReceiverDTO postableReceiverDTO,
                         @QueryParam("defaultName") @DefaultValue("false") Boolean defaultName,
                         @QueryParam("defaultTemplate") @DefaultValue("false") Boolean defaultTemplate,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc) throws AlertException {
    if (postableReceiverDTO == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Receiver receiver;
    try {
      AlertType alertType = null;
      if (defaultName || Strings.isNullOrEmpty(postableReceiverDTO.getName()) ||
          AlertType.fromReceiverName(postableReceiverDTO.getName()) != null) {
        alertType = getDefaultName(postableReceiverDTO);
        postableReceiverDTO.setName(alertType.getReceiverName());
      }
      receiver = receiverBuilder.build(postableReceiverDTO, defaultTemplate, true);
      alertManagerConfiguration.addReceiver(receiver);
      if (alertType != null) {
        alertController.createRoute(alertType); // to create a single route for global receivers
      }
      ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
      ReceiverDTO dto = receiverBuilder.build(uriInfo, resourceRequest, receiver.getName(), null);
      dto.setHref(uriInfo.getAbsolutePathBuilder().path(receiver.getName()).build());
      return Response.created(dto.getHref()).entity(dto).build();
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerUnreachableException |
        AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerDuplicateEntryException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_EXIST, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerAccessControlException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ACCESS_CONTROL_EXCEPTION, Level.FINE, e.getMessage());
    } catch (AlertManagerNoSuchElementException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_NOT_FOUND, Level.FINE, e.getMessage());
    }
  }
  
  private AlertType getDefaultName(PostableReceiverDTO postableReceiverDTO) throws AlertException {
    AlertType alertType = null;
    int configs = 0;
    if (postableReceiverDTO.getEmailConfigs() != null && postableReceiverDTO.getEmailConfigs().size() > 0) {
      alertType = AlertType.GLOBAL_ALERT_EMAIL;
      configs++;
    }
    if (postableReceiverDTO.getSlackConfigs() != null && postableReceiverDTO.getSlackConfigs().size() > 0) {
      alertType = AlertType.GLOBAL_ALERT_SLACK;
      configs++;
    }
    if (postableReceiverDTO.getPagerdutyConfigs() != null && postableReceiverDTO.getPagerdutyConfigs().size() > 0) {
      alertType = AlertType.GLOBAL_ALERT_PAGERDUTY;
      configs++;
    }
    if (postableReceiverDTO.getPushoverConfigs() != null && postableReceiverDTO.getPushoverConfigs().size() > 0) {
      alertType = AlertType.GLOBAL_ALERT_PUSHOVER;
      configs++;
    }
    if (postableReceiverDTO.getOpsgenieConfigs() != null && postableReceiverDTO.getOpsgenieConfigs().size() > 0) {
      alertType = AlertType.GLOBAL_ALERT_OPSGENIE;
      configs++;
    }
    if (postableReceiverDTO.getWebhookConfigs() != null && postableReceiverDTO.getWebhookConfigs().size() > 0) {
      alertType = AlertType.GLOBAL_ALERT_WEBHOOK;
      configs++;
    }
    if (postableReceiverDTO.getVictoropsConfigs() != null && postableReceiverDTO.getVictoropsConfigs().size() > 0) {
      alertType = AlertType.GLOBAL_ALERT_VICTOROPS;
      configs++;
    }
    if (postableReceiverDTO.getWechatConfigs() != null && postableReceiverDTO.getWechatConfigs().size() > 0) {
      alertType = AlertType.GLOBAL_ALERT_WEBCHAT;
      configs++;
    }
    if (configs > 1 || alertType == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "Receiver name not set.");
    }
    return alertType;
  }
  
  @PUT
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a receiver.", response = ReceiverDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response update(@PathParam("name") String name,
                         @QueryParam("defaultTemplate") @DefaultValue("false") Boolean defaultTemplate,
                         PostableReceiverDTO postableReceiverDTO,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws AlertException {
    if (postableReceiverDTO == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Receiver receiver = receiverBuilder.build(postableReceiverDTO, defaultTemplate, true);
    try {
      alertManagerConfiguration.updateReceiver(name, receiver);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerDuplicateEntryException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_EXIST, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerNoSuchElementException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_NOT_FOUND, Level.FINE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    ReceiverDTO dto = receiverBuilder.build(uriInfo, resourceRequest, receiver.getName(), null);
    return Response.ok().entity(dto).build();
  }
  
  
  @DELETE
  @Path("{name}")
  @ApiOperation(value = "Delete receiver by name.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response deleteById(@PathParam("name") String name,
                             @QueryParam("cascade") @DefaultValue("false") Boolean cascade,
                             @Context UriInfo uriInfo,
                             @Context HttpServletRequest req,
                             @Context SecurityContext sc) throws AlertException {
    try {
      alertManagerConfiguration.removeReceiver(name, cascade);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    }
    return Response.noContent().build();
  }
}