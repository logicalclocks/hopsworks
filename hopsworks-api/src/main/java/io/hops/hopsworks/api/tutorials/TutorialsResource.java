/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.api.tutorials;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Logged(logLevel = LogLevel.OFF)
@Path("/tutorials")
@Stateless
@Api(value = "Tutorials Resource", description = "A resource to retrieve all available tutorials")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TutorialsResource {

  @EJB
  TutorialBuilder tutorialBuilder;

  @ApiOperation(value = "Fetch all tutorials", response = TutorialDTO.class, responseContainer = "List")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @JWTRequired(acceptedTokens={Audience.API, Audience.JOB}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER",
    "HOPS_SERVICE_USER"})
  @ApiKeyRequired(acceptedScopes = {ApiScope.PROJECT},
    allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER", "HOPS_SERVICE_USER"})
  public Response getAll(@Context SecurityContext sc, @Context HttpServletRequest req, @Context UriInfo uriInfo)
      throws FeaturestoreException {

    TutorialDTO dtos = tutorialBuilder.build(uriInfo);
    return Response.ok().entity(dtos).build();
  }
}
