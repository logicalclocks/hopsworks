package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class IoTGatewayResource {
  private static final Logger LOGGER = Logger.getLogger(IoTGatewayResource.class.getName());

  @EJB
  private ProjectFacade projectFacade;

  private Project project;


  public IoTGatewayResource setProject(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }

  @ApiOperation(value = "Get list of currently connected IoT Gateways")
  @GET
  @Path("gateways")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getGateways() {
    //TODO: implement endpoint
    String json = "{\"ids\":[\"id1\",\"id2\",\"id3\"]}";
    return Response.ok().entity(json).build();
  }

  @ApiOperation(value = "Get info about a specific IoT Gateway")
  @GET
  @Path("gateway/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getGatewayById(
          @PathParam("id") String id
  ) {
    //TODO: implement endpoint
    String json = "{\"message\":\"Get info about gateway " + id +"\"}";
    return Response.ok().entity(json).build();
  }

  @ApiOperation(value = "Get list of all IoT Nodes of a gateway")
  @GET
  @Path("gateway/{id}/nodes")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response getNodes(
          @PathParam("id") String id
  ) {
    //TODO: implement endpoint
    String json = "{\"message\":\"Get list of nodes of gateway " + id +"\"}";
    return Response.ok().entity(json).build();
  }

  @ApiOperation(value = "Start/stop blocking an IoT Gateway")
  @POST
  @Path("gateway/{id}/ignored")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response startBlockingGateway(
          @PathParam("id") String id
  ) {
    //TODO: implement endpoint
    String json = "{\"message\":\"Start blocking gateway " + id +"\"}";
    return Response.ok().entity(json).build();
  }

  @ApiOperation(value = "Start/stop blocking an IoT Gateway")
  @DELETE
  @Path("gateway/{id}/ignored")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens={Audience.API}, allowedUserRoles={"HOPS_ADMIN", "HOPS_USER"})
  public Response stopBlockingGateway(
          @PathParam("id") String id
  ) {
    //TODO: implement endpoint
    String json = "{\"message\":\"Stop blocking gateway " + id +"\"}";
    return Response.ok().entity(json).build();
  }
}
