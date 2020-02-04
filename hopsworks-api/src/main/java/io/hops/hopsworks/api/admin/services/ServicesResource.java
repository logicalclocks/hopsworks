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

package io.hops.hopsworks.api.admin.services;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/services")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@Api(value = "Admin")
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ServicesResource {
  
  @EJB
  private ServicesBuilder servicesBuilder;

  @ApiOperation(value = "Get metadata of all services.")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllServices(
    @Context UriInfo uriInfo,
    @BeanParam Pagination pagination,
    @BeanParam ServicesBeanParam servicesBeanParam) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.SERVICES);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(servicesBeanParam.getSortBySet());
    resourceRequest.setFilter(servicesBeanParam.getFilter());
    ServiceDTO dto = servicesBuilder.buildItems(uriInfo, resourceRequest);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get metadata of a service.")
  @GET
  @Path("/{name}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getService(@Context UriInfo uriInfo, @PathParam("name") String name) throws ServiceException {
    ServiceDTO dto = servicesBuilder.buildItems(uriInfo, name);
    return Response.ok().entity(dto).build();
  }
}
