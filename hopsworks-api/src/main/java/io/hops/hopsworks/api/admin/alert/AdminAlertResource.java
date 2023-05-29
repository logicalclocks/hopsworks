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

package io.hops.hopsworks.api.admin.alert;

import io.hops.hopsworks.alert.AMClient;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.api.activities.ActivitiesDTO;
import io.hops.hopsworks.api.admin.alert.management.ManagementResource;
import io.hops.hopsworks.api.admin.alert.receiver.AdminReceiverResource;
import io.hops.hopsworks.api.admin.alert.route.AdminRouteResource;
import io.hops.hopsworks.api.admin.alert.silence.AdminSilenceResource;
import io.hops.hopsworks.api.alert.AlertBeanParam;
import io.hops.hopsworks.api.alert.AlertBuilder;
import io.hops.hopsworks.api.alert.AlertDTO;
import io.hops.hopsworks.api.alert.AlertFilterBy;
import io.hops.hopsworks.api.alert.AlertGroupDTO;
import io.hops.hopsworks.api.alert.PostableAlertDTOs;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.exceptions.AlertException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.restutils.RESTCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Logged
@Stateless
@Path("/admin/alerts")
@Api(value = "Alert Resource")
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AdminAlertResource {

  private static final Logger LOGGER = Logger.getLogger(AdminAlertResource.class.getName());
  @EJB
  private AlertBuilder alertBuilder;
  @EJB
  private AMClient alertManager;
  @Inject
  private ManagementResource managementResource;
  @Inject
  private AdminSilenceResource adminSilenceResource;
  @Inject
  private AdminReceiverResource adminReceiverResource;
  @Inject
  private AdminRouteResource adminRouteResource;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get alerts.", response = ActivitiesDTO.class)
  public Response getAlerts(@BeanParam Pagination pagination, @BeanParam AlertBeanParam alertBeanParam,
                            @Context HttpServletRequest req,
                            @Context UriInfo uriInfo, @Context SecurityContext sc) throws AlertException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    AlertDTO alertDTO = alertBuilder.buildItems(uriInfo, resourceRequest, alertBeanParam, null);
    return Response.ok().entity(alertDTO).build();
  }

  @GET
  @Path("groups")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get alerts groups.", response = ActivitiesDTO.class)
  public Response getAlertGroups(@BeanParam Pagination pagination, @BeanParam AlertFilterBy alertFilterBy,
                                 @QueryParam("expand_alert") Boolean expand, @Context UriInfo uriInfo,
                                 @Context HttpServletRequest req,
                                 @Context SecurityContext sc) throws AlertException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ALERTGROUPS);
    if (expand != null && expand) {
      Set<ResourceRequest> expansions = new HashSet<>();
      expansions.add(new ResourceRequest(ResourceRequest.Name.ALERTS));
      resourceRequest.setExpansions(expansions);
    }
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    AlertGroupDTO alertGroupDTO = alertBuilder.buildAlertGroupItems(uriInfo, resourceRequest, alertFilterBy, null);
    return Response.ok().entity(alertGroupDTO).build();
  }

  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create alert.", response = ActivitiesDTO.class)
  public Response createAlerts(PostableAlertDTOs alerts, @Context UriInfo uriInfo,
                               @Context HttpServletRequest req,
                               @Context SecurityContext sc) throws AlertException {
    if (alerts == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    try {
      return alertManager.postAlerts(alertBuilder.buildItems(alerts.getAlerts()));
    } catch (AlertManagerClientCreateException | AlertManagerUnreachableException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_CONNECT, Level.FINE, e.getMessage());
    } catch (AlertManagerResponseException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RESPONSE_ERROR, Level.FINE, e.getMessage());
    } catch (IllegalArgumentException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, e.getMessage());
    }
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("management")
  public ManagementResource management() {
    return managementResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("silences")
  public AdminSilenceResource silence() {
    return adminSilenceResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("receivers")
  public AdminReceiverResource receiver() {
    return adminReceiverResource;
  }

  @Logged(logLevel = LogLevel.OFF)
  @Path("routes")
  public AdminRouteResource route() {
    return adminRouteResource;
  }
}