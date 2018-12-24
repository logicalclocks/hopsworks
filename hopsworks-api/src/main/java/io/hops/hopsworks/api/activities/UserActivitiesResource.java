/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.activities;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.ActivitiesException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UserActivitiesResource {

  @EJB
  private ActivitiesBuilder activitiesBuilder;
  @EJB
  private JWTHelper jWTHelper;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Finds all activities for a user.", response = ActivitiesDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response findAllByUser(
      @BeanParam Pagination pagination,
      @BeanParam ActivitiesBeanParam activitiesBeanParam,
      @Context UriInfo uriInfo,
      @Context SecurityContext sc) {
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ACTIVITIES);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(activitiesBeanParam.getSortBySet());
    resourceRequest.setFilter(activitiesBeanParam.getFilter());
    if (activitiesBeanParam.getExpansions() != null) {
      resourceRequest.setExpansions(activitiesBeanParam.getExpansions().getResources());
    }
  
    ActivitiesDTO activitiesDTO = activitiesBuilder.buildItems(uriInfo, resourceRequest, user);
    return Response.ok().entity(activitiesDTO).build();
  }

  @GET
  @Path("{activityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Finds an activity for a user by id.", response = ActivitiesDTO.class)
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response findAllById(
      @PathParam("activityId") Integer activityId,
      @BeanParam ExpansionBeanParam expansions,
      @Context UriInfo uriInfo,
      @Context SecurityContext sc) throws ActivitiesException {
    Users user = jWTHelper.getUserPrincipal(sc);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.ACTIVITIES);
    resourceRequest.setExpansions(expansions.getResources());
    ActivitiesDTO activitiesDTO = activitiesBuilder.build(uriInfo, resourceRequest, user, activityId);
    return Response.ok().entity(activitiesDTO).build();
  }
}
