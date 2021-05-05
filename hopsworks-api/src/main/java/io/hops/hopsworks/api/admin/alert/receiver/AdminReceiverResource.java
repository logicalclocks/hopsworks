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

import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.config.dto.Receiver;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.api.alert.receiver.ReceiverBeanParam;
import io.hops.hopsworks.api.alert.receiver.ReceiverBuilder;
import io.hops.hopsworks.api.alert.receiver.ReceiverDTO;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@Api(value = "Receiver Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AdminReceiverResource {

  private static final Logger LOGGER = Logger.getLogger(AdminReceiverResource.class.getName());

  @EJB
  private ReceiverBuilder receiverBuilder;
  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all receivers.", response = ReceiverDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response get(@BeanParam Pagination pagination, @BeanParam ReceiverBeanParam receiverBeanParam,
      @Context UriInfo uriInfo, @Context SecurityContext sc) throws AlertException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    ReceiverDTO dto = receiverBuilder.buildItems(uriInfo, resourceRequest, receiverBeanParam);
    return Response.ok().entity(dto).build();
  }

  @GET
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get receiver by name.", response = ReceiverDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response getByName(@PathParam("name") String name, @Context UriInfo uriInfo,
      @Context SecurityContext sc) throws AlertException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    ReceiverDTO dto = receiverBuilder.build(uriInfo, resourceRequest, name, null);
    return Response.ok().entity(dto).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create a receiver.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response create(Receiver receiver, @Context UriInfo uriInfo, @Context SecurityContext sc)
      throws AlertException {
    if (receiver == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    try {
      alertManagerConfiguration.addReceiver(receiver);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerUnreachableException |
        AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerDuplicateEntryException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_EXIST, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    }
    return Response.created(uriInfo.getAbsolutePathBuilder().path(receiver.getName()).build()).build();
  }

  @PUT
  @Path("{name}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a receiver.", response = ReceiverDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response update(@PathParam("name") String name, Receiver receiver, @Context UriInfo uriInfo,
      @Context SecurityContext sc) throws AlertException {
    if (receiver == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    try {
      alertManagerConfiguration.updateReceiver(name, receiver);
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
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.RECEIVERS);
    ReceiverDTO dto = receiverBuilder.build(uriInfo, resourceRequest, receiver.getName(), null);
    return Response.ok().entity(dto).build();
  }


  @DELETE
  @Path("{name}")
  @ApiOperation(value = "Delete receiver by name.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response deleteById(@PathParam("name") String name, @Context UriInfo uriInfo,
      @Context SecurityContext sc) throws AlertException {
    try {
      alertManagerConfiguration.removeReceiver(name);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerUnreachableException |
        AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerClientCreateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    }
    return Response.noContent().build();
  }
}