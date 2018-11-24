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
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.jwt.JWTHelper;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceProperties;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.ActivitiesException;
import io.hops.hopsworks.common.exception.UserException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class UserActivitiesResource {

  @EJB
  private ActivitiesBuilder activitiesBuilder;
  @EJB
  private JWTHelper jWTHelper;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response findAllByProject(
      @BeanParam Pagination pagination,
      @BeanParam ActivitiesBeanParam activitiesBeanParam,
      @QueryParam("expand") String expand,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest req) throws UserException, ActivitiesException {
    Users user = jWTHelper.getUserPrincipal(req);
    ResourceProperties resourceProperties = new ResourceProperties(ResourceProperties.Name.ACTIVITIES, pagination.
        getOffset(), pagination.getLimit(), activitiesBeanParam.getSortBySet(), activitiesBeanParam.getFilter(), 
        expand);
    ActivitiesDTO activitiesDTO = activitiesBuilder.buildItems(uriInfo, resourceProperties, user);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(activitiesDTO).build();
  }

  @GET
  @Path("{activityId}")
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens = {Audience.API},
      allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response findAllById(
      @PathParam("activityId") Integer activityId,
      @QueryParam("expand") String expand,
      @Context UriInfo uriInfo,
      @Context HttpServletRequest req) throws ActivitiesException, UserException {
    Users user = jWTHelper.getUserPrincipal(req);
    ResourceProperties resourceProperties = new ResourceProperties(ResourceProperties.Name.ACTIVITIES, expand);
    ActivitiesDTO activitiesDTO = activitiesBuilder.build(uriInfo, resourceProperties, user, activityId);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(activitiesDTO).build();
  }
}
