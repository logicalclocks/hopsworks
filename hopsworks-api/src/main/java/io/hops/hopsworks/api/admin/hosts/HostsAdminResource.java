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
package io.hops.hopsworks.api.admin.hosts;

import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.api.util.Pagination;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.host.HostDTO;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

@Path("/hosts")
@Stateless
@JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN"})
@Api(value = "Admin Hosts")
@Produces(MediaType.APPLICATION_JSON)
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HostsAdminResource {
  
  private static final Logger LOGGER = Logger.getLogger(HostsAdminResource.class.getName());
  @EJB
  private HostsController hostsController;
  @EJB
  private HostsBuilder hostsBuilder;
  
  @ApiParam(value = "Get all cluster nodes.")
  @GET
  public Response getAllClusterNodes(@Context SecurityContext sc,
    @Context UriInfo uriInfo,
    @BeanParam Pagination pagination,
    @BeanParam HostsBeanParam hostsBeanParam
  ) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.HOSTS);
    resourceRequest.setOffset(pagination.getOffset());
    resourceRequest.setLimit(pagination.getLimit());
    resourceRequest.setSort(hostsBeanParam.getSortBySet());
    resourceRequest.setFilter(hostsBeanParam.getFilter());
    HostsDTO dto = hostsBuilder.build(uriInfo, resourceRequest);
    return Response.ok().entity(dto).build();
  }
  
  @ApiParam(value = "Get cluster node by hostname.")
  @GET
  @Path("/{hostname}")
  public Response getClusterNode(@Context SecurityContext sc, @Context UriInfo uriInfo,
    @PathParam("hostname") String hostname) throws ServiceException {
    HostsDTO dto = hostsBuilder.buildByHostname(uriInfo, hostname);
    return Response.ok().entity(dto).build();
  }
  
  @ApiParam(value = "Delete cluster node by hostname.")
  @DELETE
  @Path("/{hostname}")
  public Response deleteNodeByHostname(@PathParam("hostname") String hostname, @Context SecurityContext sc) {
    if (hostsController.removeByHostname(hostname)) {
      return Response.noContent().build();
    } else {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }
  
  @ApiParam(value = "Add new cluster node or update existing by hostname.")
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{hostname}")
  public Response updateClusterNode(@Context UriInfo uriInfo, @Context SecurityContext sc,
    @PathParam("hostname") String hostname, HostDTO nodeToUpdate) {
    
    return hostsController.addOrUpdateClusterNode(uriInfo, hostname, nodeToUpdate);
  }
}
