package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.iot.GatewayFacade;
import io.hops.hopsworks.common.dao.iot.GatewayState;
import io.hops.hopsworks.common.dao.iot.IoTGateways;
import io.hops.hopsworks.common.dao.iot.LwM2MTopics;
import io.hops.hopsworks.common.dao.kafka.AclDTO;
import io.hops.hopsworks.common.dao.kafka.KafkaFacade;
import io.hops.hopsworks.common.dao.kafka.TopicAcls;
import io.hops.hopsworks.common.dao.kafka.TopicDTO;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.project.ProjectFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GatewayException;
import io.hops.hopsworks.exceptions.KafkaException;
import io.hops.hopsworks.jwt.annotation.JWTRequired;
import io.swagger.annotations.ApiOperation;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class IoTGatewayResource {
  private static final Logger LOGGER = Logger.getLogger(IoTGatewayResource.class.getName());
  
  @EJB
  private GatewaysBuilder gatewaysBuilder;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private GatewayFacade gatewayFacade;
  @EJB
  private GatewayController gatewayController;
  @EJB
  private KafkaFacade kafkaFacade;
  
  private Project project;
  
  
  public IoTGatewayResource setProject(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }
  
  @ApiOperation(value = "Get list of currently connected IoT Gateways")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getGateways(
    @Context
      UriInfo uriInfo
  ) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.GATEWAYS);
    IoTGatewayDTO dto = gatewaysBuilder.build(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get info about a specific IoT Gateway")
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getGatewayById(
    @Context
      UriInfo uriInfo,
    @PathParam("id")
      Integer id
  ) throws GatewayException {
    IoTGateways gateway = gatewayController.getGateway(project, id);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.GATEWAYS);
    IoTGatewayDTO dto = gatewaysBuilder.build(uriInfo, resourceRequest, gateway);
    return Response.ok().entity(dto).build();
  }
  
  @PUT
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_GATEWAY"})
  public Response registerGateway() {
    //TODO: implement
    return Response.ok().build();
  }
  
  @DELETE
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_GATEWAY"})
  public Response unregisterGateway() {
    //TODO: implement
    return Response.ok().build();
  }
  
  @ApiOperation(value = "Start blocking an IoT Gateway")
  @POST
  @Path("{id}/ignored")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response startBlockingGateway(
    @PathParam("id")
      Integer id
  ) {
    
    kafkaFacade
      .findTopicsByProject(project)
      .stream()
      .filter(t -> LwM2MTopics.getNamesAsList().contains(t.getName()))
      .forEach(t -> addBlockingAcl(t, id));
    
    gatewayFacade.updateState(id, GatewayState.BLOCKED);
    return Response.ok().build();
  }
  
  private void addBlockingAcl(TopicDTO t, int gatewayId) {
    IoTGateways ioTGateway = gatewayFacade.findByProjectAndId(project, gatewayId);
    AclDTO acl = new AclDTO(project.getName(),
      Settings.KAFKA_ACL_WILDCARD,
      "deny",
      Settings.KAFKA_ACL_WILDCARD,
      ioTGateway.getHostname(),
      Settings.KAFKA_ACL_WILDCARD);
    
    try {
      kafkaFacade.addAclsToTopic(t.getName(), project.getId(), acl);
    } catch (Exception e){
      e.printStackTrace();
    }
  }
  
  @ApiOperation(value = "Stop blocking an IoT Gateway")
  @DELETE
  @Path("{id}/ignored")
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response stopBlockingGateway(
    @PathParam("id")
      Integer id
  ) {
    kafkaFacade
      .findTopicsByProject(project)
      .stream()
      .filter(t -> LwM2MTopics.getNamesAsList().contains(t.getName()))
      .forEach(t -> removeBlockingAcl(t, id));
    
    gatewayFacade.updateState(id, GatewayState.REGISTERED);
    return Response.ok().build();
  }
  
  private void removeBlockingAcl(TopicDTO t, int gatewayId) {
    String gatewayIp = gatewayFacade.findByProjectAndId(project, gatewayId).getHostname();
    //TODO: make sure that principal is not necessary
    TopicAcls acl = kafkaFacade.getTopicAcl(
      t.getName(),
      "deny",
      Settings.KAFKA_ACL_WILDCARD,
      gatewayIp,
      Settings.KAFKA_ACL_WILDCARD);

    try {
      if (acl != null) {
        kafkaFacade.removeAclFromTopic(t.getName(), acl.getId());
      }
    } catch (KafkaException e) {
      e.printStackTrace();
    }
  }
}
