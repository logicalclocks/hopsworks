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

package io.hops.hopsworks.api.admin.alert.route;

import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.api.alert.route.PostableRouteDTO;
import io.hops.hopsworks.api.alert.route.RouteBeanParam;
import io.hops.hopsworks.api.alert.route.RouteBuilder;
import io.hops.hopsworks.api.alert.route.RouteDTO;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.audit.logger.annotation.Logged;
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
@Api(value = "Route Resource")
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AdminRouteResource {
  
  private static final Logger LOGGER = Logger.getLogger(AdminRouteResource.class.getName());
  
  @EJB
  private RouteBuilder routeBuilder;
  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;
  
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all routes.", response = RouteDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response get(@BeanParam Pagination pagination, @BeanParam RouteBeanParam routeBeanParam,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc)
      throws AlertException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ROUTES);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    RouteDTO dto = routeBuilder.buildItems(uriInfo, resourceRequest, routeBeanParam, null);
    return Response.ok().entity(dto).build();
  }
  
  @GET
  @Path("{receiver}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get all routes.", response = RouteDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response get(@PathParam("receiver") String receiver,
                      @QueryParam("match") List<String> match,
                      @QueryParam("matchRe") List<String> matchRe,
                      @Context UriInfo uriInfo,
                      @Context HttpServletRequest req,
                      @Context SecurityContext sc)
      throws AlertException {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ROUTES);
    Route route = new Route(receiver).withMatch(routeBuilder.toMap(match)).withMatchRe(routeBuilder.toMap(matchRe));
    RouteDTO dto = routeBuilder.build(uriInfo, resourceRequest, route, null);
    return Response.ok().entity(dto).build();
  }
  
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create a route.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response create(PostableRouteDTO routeDTO, @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws AlertException {
    if (routeDTO == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Route route = routeDTO.toRoute();
    try {
      alertManagerConfiguration.addRoute(route);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerDuplicateEntryException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_EXIST, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerNoSuchElementException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_NOT_FOUND, Level.FINE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ROUTES);
    RouteDTO dto = routeBuilder.build(uriInfo, resourceRequest, route, null);
    return Response.created(dto.getHref()).build();
  }
  
  
  @PUT
  @Path("{receiver}")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update a route.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response update(@PathParam("receiver") String receiver, PostableRouteDTO route,
                         @QueryParam("match") List<String> match,
                         @QueryParam("matchRe") List<String> matchRe,
                         @Context HttpServletRequest req,
                         @Context UriInfo uriInfo,
                         @Context SecurityContext sc) throws AlertException {
    if (route == null) {
      throw new AlertException(RESTCodes.AlertErrorCode.ILLEGAL_ARGUMENT, Level.FINE, "No payload.");
    }
    Route routeToUpdate =
        new Route(receiver).withMatch(routeBuilder.toMap(match)).withMatchRe(routeBuilder.toMap(matchRe));
    Route updatedRoute = route.toRoute();
    try {
      alertManagerConfiguration.updateRoute(routeToUpdate, updatedRoute);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerDuplicateEntryException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.RECEIVER_EXIST, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    } catch (AlertManagerNoSuchElementException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.ROUTE_NOT_FOUND, Level.FINE, e.getMessage());
    }
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ROUTES);
    RouteDTO dto = routeBuilder.build(uriInfo, resourceRequest, updatedRoute, null);
    return Response.ok().entity(dto).build();
  }
  
  @DELETE
  @Path("{receiver}")
  @ApiOperation(value = "Delete route.")
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
  public Response delete(@PathParam("receiver") String receiver,
                         @QueryParam("match") List<String> match,
                         @QueryParam("matchRe") List<String> matchRe,
                         @Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc)
      throws AlertException {
    Route routeToDelete =
        new Route(receiver).withMatch(routeBuilder.toMap(match)).withMatchRe(routeBuilder.toMap(matchRe));
    try {
      alertManagerConfiguration.removeRoute(routeToDelete);
    } catch (AlertManagerConfigCtrlCreateException | AlertManagerConfigReadException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_READ_CONFIGURATION, Level.FINE, e.getMessage());
    } catch (AlertManagerConfigUpdateException e) {
      throw new AlertException(RESTCodes.AlertErrorCode.FAILED_TO_UPDATE_AM_CONFIG, Level.FINE, e.getMessage());
    }
    return Response.noContent().build();
  }
}