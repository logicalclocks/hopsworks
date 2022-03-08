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

package io.hops.hopsworks.api.featurestore.query;

import io.hops.hadoop.shaded.javax.ws.rs.core.Context;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiParam;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class QueryResource {

  @GET
  @Path("/batch")
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response constructBatchQuery(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req,
      @ApiParam(value = "Event start time")
      @QueryParam("startTime")
          String startTime,
      @ApiParam(value = "Event end time")
      @QueryParam("endTime")
          String endTime
  ) {
    // return an offline query string
    return Response.ok().entity("").build();
  }

  @GET
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getQuery(
      @Context
          SecurityContext sc,
      @Context
          HttpServletRequest req
  ) {
    // return query originally used to create the feature view without event time filter
    return Response.ok().entity(new QueryDTO()).build();
  }

  public void setFeatureView(String name, Integer version) {
  }
}
