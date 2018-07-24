/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.cluster;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.dao.kagent.ServiceStatusDTO;
import io.hops.hopsworks.common.dao.kagent.HostServices;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.common.exception.AppException;
import io.swagger.annotations.Api;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.security.RolesAllowed;
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
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@Path("/kmon")
@RolesAllowed({"HOPS_ADMIN", "HOPS_USER"})
@Api(value = "Monitor Cluster Service")
@Produces(MediaType.APPLICATION_JSON)
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class Monitor {

  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private HostsFacade hostEjb;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Path("/services")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllRoles(@Context SecurityContext sc, @Context HttpServletRequest req) {
    List<HostServices> list = hostServicesFacade.findAll();
    GenericEntity<List<HostServices>> services = new GenericEntity<List<HostServices>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(services).build();
  }

  @GET
  @Path("/groups/{groupName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getServiceRoles(@PathParam("groupName") String groupName, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    List<HostServices> list = hostServicesFacade.findGroupServices(groupName);
    // Do not leak Host data back to clients!
    List<ServiceStatusDTO> groupStatus = new ArrayList<>();
    for (HostServices h : list) {
      groupStatus.add(new ServiceStatusDTO(h.getGroup(), h.getService(), h.getStatus()));
    }
    GenericEntity<List<ServiceStatusDTO>> services = new GenericEntity<List<ServiceStatusDTO>>(groupStatus) { };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(services).build();
  }

  @GET
  @Path("/hosts/{hostId}/services")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHostRoles(@PathParam("hostId") String hostname, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    List<HostServices> list = hostServicesFacade.findHostServiceByHostname(hostname);
    GenericEntity<List<HostServices>> services = new GenericEntity<List<HostServices>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(services).build();
  }

  @GET
  @Path("/groups/{groupName}/services/{serviceName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRoles(@PathParam("groupName") String groupName, @PathParam("serviceName") String serviceName,
      @Context SecurityContext sc, @Context HttpServletRequest req) {
    List<HostServices> list = hostServicesFacade.findGroups(groupName, serviceName);
    GenericEntity<List<HostServices>> services = new GenericEntity<List<HostServices>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(services).build();
  }

  @GET
  @Path("/hosts")
  @RolesAllowed({"HOPS_ADMIN"}) //return the password in the host object
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHosts(@Context SecurityContext sc, @Context HttpServletRequest req) {
    List<Hosts> list = hostEjb.find();
    GenericEntity<List<Hosts>> hosts = new GenericEntity<List<Hosts>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(hosts).build();
  }

  @GET
  @Path("/hosts/{hostId}")
  @RolesAllowed({"HOPS_ADMIN"}) //return the password in the host object
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHosts(@PathParam("hostId") String hostId, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    Hosts h = hostEjb.findByHostname(hostId);
    if (h != null) {
      GenericEntity<Hosts> host = new GenericEntity<Hosts>(h) {
      };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(host).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }

  }

  @POST
  @Path("/groups/{groupName}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response serviceOp(@PathParam("groupName") String groupName, @Context SecurityContext sc,
      @Context HttpServletRequest req, ServicesActionDTO action) throws AppException {
    String result = hostServicesFacade.serviceOp(groupName, action.getAction());
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(result);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("/groups/{groupName}/services/{serviceName}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response serviceOp(@PathParam("groupName") String groupName, @PathParam("serviceName") String serviceName,
      @Context SecurityContext sc, @Context HttpServletRequest req, ServicesActionDTO action) throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(hostServicesFacade.serviceOp(groupName, serviceName, action.getAction()));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }

  @POST
  @Path("/groups/{groupName}/services/{serviceName}/hosts/{hostId}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response serviceOnHostOp(@PathParam("groupName") String groupName,
      @PathParam("serviceName") String serviceName,
      @PathParam("hostId") String hostId, @Context SecurityContext sc, @Context HttpServletRequest req,
      ServicesActionDTO action) throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(hostServicesFacade.serviceOnHostOp(groupName, serviceName, hostId, action.getAction()));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
}
