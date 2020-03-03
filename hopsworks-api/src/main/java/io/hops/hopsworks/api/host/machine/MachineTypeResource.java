/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.host.machine;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.python.LibraryFacade;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

@Path("/machines")
@Stateless
@JWTRequired(acceptedTokens = {Audience.API},
  allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Machine Types Resource")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MachineTypeResource {
  
  @EJB
  private MachineTypeBuilder machineTypeBuilder;
  
  @ApiOperation(value = "Get all types of machines for conda", response = MachineTypeDTO.class)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMachineTypes(@Context UriInfo uriInfo, @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MACHINETYPES);
    MachineTypeDTO machineTypeDTO = machineTypeBuilder.buildItems(uriInfo, resourceRequest);
    return Response.ok(machineTypeDTO).build();
  }
  
  @ApiOperation(value = "Get number of machines for a machine type", response = MachineTypeDTO.class)
  @GET
  @Path("/{type}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMachineType(@PathParam("type") LibraryFacade.MachineType type, @Context UriInfo uriInfo,
    @Context SecurityContext sc) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.MACHINETYPES);
    MachineTypeDTO machineTypeDTO = machineTypeBuilder.buildItem(uriInfo, resourceRequest, type);
    return Response.ok(machineTypeDTO).build();
  }
}
