/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.featurestore;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.filter.apiKey.ApiKeyRequired;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.security.apiKey.ApiScope;
import io.hops.hopsworks.common.featurestore.query.ConstructorController;
import io.hops.hopsworks.common.featurestore.query.QueryDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
@Api(value = "Query constructor service")
public class FsQueryConstructorResource {

  @EJB
  private ConstructorController constructorController;
  @EJB
  private FsQueryBuilder fsQueryBuilder;

  private Project project;
  public FsQueryConstructorResource setProject(Project project) {
    this.project = project;
    return this;
  }

  /**
   * TODO(Fabio): here probably a GET request is better as we are not changing any entity in the backend system.
   * For the moment as we are in a rush I'll let the PUT method, but I'll try to fix it before the final release
   * is done.
   * When implementing the GET we need to investigate:
   * - What's the maximum length supported by Glassfish when it comes to URL, and make sure we can send a
   * reasonable size query.
   * - Make sure that the clients send the feature name URL encoded.
   * - Make sure Hive does not automatically URL encode column names (as it already does for partitions)
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API, Audience.JOB}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiKeyRequired( acceptedScopes = {ApiScope.FEATURESTORE}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  @ApiOperation(value = "Construct the SQL query to join the requested features",
      response = FsQueryDTO.class)
  public Response constructQuery(@Context SecurityContext sc, @Context UriInfo uriInfo,
                                 QueryDTO queryDto) throws FeaturestoreException {
    if (queryDto == null) {
      throw new IllegalArgumentException("Please submit a query to construct");
    }
    String query = constructorController.construct(queryDto);
    FsQueryDTO fsQueryDTO = fsQueryBuilder.build(uriInfo, project, query);
    return Response.ok().entity(fsQueryDTO).build();
  }
}
