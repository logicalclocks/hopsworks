package io.hops.hopsworks.api.cluster;

import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.util.JsonResponse;
import io.hops.hopsworks.common.dao.host.Host;
import io.hops.hopsworks.common.dao.host.HostEJB;
import io.hops.hopsworks.common.dao.role.Role;
import io.hops.hopsworks.common.dao.role.RoleEJB;
import io.hops.hopsworks.common.exception.AppException;
import io.swagger.annotations.Api;
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
  private RoleEJB roleEjb;
  @EJB
  private HostEJB hostEjb;
  @EJB
  private NoCacheResponse noCacheResponse;

  @GET
  @Path("/roles")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getAllRoles(@Context SecurityContext sc, @Context HttpServletRequest req) {
    List<Role> list = roleEjb.findAll();
    GenericEntity<List<Role>> roles = new GenericEntity<List<Role>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(roles).build();
  }

  @GET
  @Path("/services/{serviceName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getServiceRoles(@PathParam("serviceName") String serviceName, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    List<Role> list = roleEjb.findServiceRoles(serviceName);
    GenericEntity<List<Role>> roles = new GenericEntity<List<Role>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(roles).build();
  }

  @GET
  @Path("/hosts/{hostId}/roles")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHostRoles(@PathParam("hostId") String hostId, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    List<Role> list = roleEjb.findHostRoles(hostId);
    GenericEntity<List<Role>> roles = new GenericEntity<List<Role>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(roles).build();
  }

  @GET
  @Path("/services/{serviceName}/roles/{roleName}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRoles(@PathParam("serviceName") String serviceName, @PathParam("roleName") String roleName,
      @Context SecurityContext sc, @Context HttpServletRequest req) {
    List<Role> list = roleEjb.findRoles(serviceName, roleName);
    GenericEntity<List<Role>> roles = new GenericEntity<List<Role>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(roles).build();
  }

  @GET
  @Path("/hosts")
  @RolesAllowed({"HOPS_ADMIN"}) //return the password in the host object
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHosts(@Context SecurityContext sc, @Context HttpServletRequest req) {
    List<Host> list = hostEjb.find();
    GenericEntity<List<Host>> hosts = new GenericEntity<List<Host>>(list) {
    };
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(hosts).build();
  }

  @GET
  @Path("/hosts/{hostId}")
  @RolesAllowed({"HOPS_ADMIN"}) //return the password in the host object
  @Produces(MediaType.APPLICATION_JSON)
  public Response getHosts(@PathParam("hostId") String hostId, @Context SecurityContext sc,
      @Context HttpServletRequest req) {
    Host h = hostEjb.findByHostId(hostId);
    if (h != null) {
      GenericEntity<Host> host = new GenericEntity<Host>(h) {
      };
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(host).build();
    } else {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.NOT_FOUND).build();
    }

  }
  
  @POST
  @Path("/services/{serviceName}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response serviceOp(@PathParam("serviceName") String serviceName, @Context SecurityContext sc,
      @Context HttpServletRequest req, RolesActionDTO action) throws AppException {
    String result = roleEjb.serviceOp(serviceName, action.getAction());
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(result);
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
  
  @POST
  @Path("/services/{serviceName}/roles/{roleName}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response roleOp(@PathParam("serviceName") String serviceName, @PathParam("roleName") String roleName,
      @Context SecurityContext sc, @Context HttpServletRequest req, RolesActionDTO action) throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(roleEjb.roleOp(serviceName, roleName, action.getAction()));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
  
  @POST
  @Path("/services/{serviceName}/roles/{roleName}/hosts/{hostId}")
  @RolesAllowed({"HOPS_ADMIN"})
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response roleOnHostOp(@PathParam("serviceName") String serviceName, @PathParam("roleName") String roleName,
      @PathParam("hostId") String hostId, @Context SecurityContext sc, @Context HttpServletRequest req,
      RolesActionDTO action) throws AppException {
    JsonResponse json = new JsonResponse();
    json.setSuccessMessage(roleEjb.roleOnHostOp(serviceName, roleName, hostId, action.getAction()));
    return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).entity(json).build();
  }
}
