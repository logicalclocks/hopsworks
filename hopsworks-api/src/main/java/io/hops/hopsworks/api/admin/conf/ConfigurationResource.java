/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.api.admin.conf;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.auth.key.ApiKeyRequired;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.util.VariablesFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.hops.hopsworks.persistence.entity.user.security.apiKey.ApiScope;
import io.hops.hopsworks.persistence.entity.util.Variables;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Logged
@Stateless
@Path("/admin/configuration")
@JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN"})
@ApiKeyRequired(acceptedScopes = {ApiScope.ADMIN}, allowedUserRoles = {"HOPS_ADMIN"})
@Api(value = "Configuration endpoints")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ConfigurationResource {

  @EJB
  private ConfigurationBuilder configurationBuilder;
  @EJB
  private VariablesFacade variablesFacade;
  @EJB
  private Settings settings;

  @GET
  @ApiOperation(value = "Get all configuration values")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAll(@Context UriInfo uriInfo,
                         @Context HttpServletRequest req,
                         @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CONFIGURATION);
    ConfigurationDTO configurationDTO = configurationBuilder.build(uriInfo, resourceRequest);
    return Response.ok().entity(configurationDTO).build();
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create/Update configuration value")
  @Path("{name}")
  public Response setConf(@Context UriInfo uriInfo,
                          @Context HttpServletRequest req,
                          @Context SecurityContext sc,
                          @PathParam("name") String name,
                          ConfigurationDTO configurationDTO) {
    // Update the value in the database
    Variables confVariable =
        new Variables(name, configurationDTO.getValue(), configurationDTO.getVisibility(), configurationDTO.getHide());
    variablesFacade.update(confVariable);

    // Refresh the settings cache
    settings.refreshCache();

    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.CONFIGURATION);
    ConfigurationDTO updatedConfigurationDTO = configurationBuilder.build(uriInfo, resourceRequest);

    return Response.ok().entity(updatedConfigurationDTO).build();
  }
}
