package io.hops.hopsworks.api.iot;

import com.google.gson.Gson;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.Audience;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.iot.IotGatewayConfiguration;
import io.hops.hopsworks.common.dao.iot.IotGatewayFacade;
import io.hops.hopsworks.common.dao.iot.IotGatewayState;
import io.hops.hopsworks.common.dao.iot.IotGateways;
import io.hops.hopsworks.common.dao.iot.Lwm2mTopics;
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
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.annotation.PostConstruct;
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
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class IotGatewayResource {
  private static final Logger LOGGER = Logger.getLogger(IotGatewayResource.class.getName());
  
  @EJB
  private IotGatewayBuilder iotGatewayBuilder;
  @EJB
  private ProjectFacade projectFacade;
  @EJB
  private IotGatewayFacade iotGatewayFacade;
  @EJB
  private IotGatewayController iotGatewayController;
  @EJB
  private KafkaFacade kafkaFacade;
  
  private Project project;
  
  public IotGatewayResource setProject(Integer projectId) {
    this.project = projectFacade.find(projectId);
    return this;
  }
  
  @PostConstruct
  public void init() {
  
  }
  
  @ApiOperation(value = "Get list of currently connected IoT Gateways")
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getGateways(
    @Context UriInfo uriInfo
  ) {
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.GATEWAYS);
    IotGatewayDTO dto = iotGatewayBuilder.buildGateway(uriInfo, resourceRequest, project);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get info about a specific IoT Gateway")
  @GET
  @Path("{id}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getGatewayById(
    @Context UriInfo uriInfo,
    @PathParam("id") Integer id
  ) throws GatewayException {
    IotGateways gateway = iotGatewayController.getGateway(project, id);
    ResourceRequest resourceRequest = new ResourceRequest(ResourceRequest.Name.GATEWAYS);
    IotGatewayDTO dto = iotGatewayBuilder.buildGateway(uriInfo, resourceRequest, gateway);
    return Response.ok().entity(dto).build();
  }
  
  @ApiOperation(value = "Get list of all IoT Nodes connected to an IoT Gateway")
  @GET
  @Path("{id}/nodes")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_OWNER, AllowedProjectRoles.DATA_SCIENTIST})
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_USER"})
  public Response getNodesOfGateway(
    @Context UriInfo uriInfo,
    @PathParam("id") Integer gatewayId
  ) throws URISyntaxException, IOException {
    CloseableHttpResponse response = sendRequestForNodes(gatewayId);
    List<IotDevice> devices = responseToDevices(response, gatewayId);
    LOGGER.info("Connected " + devices.size() + " devices");
    IotDeviceDTO dto = iotGatewayBuilder.buildDevice(uriInfo, project, devices, gatewayId);
    return Response.ok().entity(dto).build();
  }
  
  private List<IotDevice> responseToDevices(CloseableHttpResponse response, Integer gatewayId)
    throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(response.getEntity().getContent(), writer);
    String json = writer.toString();
  
    Gson gson = new Gson();
    IotDevice[] array = gson.fromJson(json, IotDevice[].class);
    List<IotDevice> list = Arrays.asList(array);
    list.forEach(d -> d.setGatewayId(gatewayId));
    return list;
  }
  
  private CloseableHttpResponse sendRequestForNodes(int gatewayId)
    throws URISyntaxException, IOException {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    IotGateways gateway = iotGatewayFacade.findByProjectAndId(project, gatewayId);
    URI uri = new URIBuilder()
      .setScheme("http")
      .setHost(gateway.getHostname())
      .setPort(gateway.getPort())
      .setPath("/gateway/nodes")
      .build();
    HttpGet httpGet = new HttpGet(uri);
    return httpClient.execute(httpGet);
  }
  
  @ApiOperation(value = "Register an IoT Gateway")
  @PUT
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_GATEWAY"})
  public Response registerGateway(
    IotGatewayConfiguration config,
    @Context UriInfo uriInfo
  ) {
    if (config == null) {
      throw new IllegalArgumentException("Gateway configuration was not provided.");
    }
    IotGateways gateway = iotGatewayController.putGateway(project, config);
    IotGatewayDTO dto =
      iotGatewayBuilder.buildGateway(uriInfo, new ResourceRequest(ResourceRequest.Name.GATEWAYS), gateway);
    return Response.created(dto.getHref()).build();
  }
  
  @ApiOperation(value = "Unregister IoT Gateway")
  @DELETE
  @Path("{id}")
  @Produces(MediaType.TEXT_PLAIN)
  @JWTRequired(acceptedTokens = {Audience.API}, allowedUserRoles = {"HOPS_ADMIN", "HOPS_GATEWAY"})
  public Response unregisterGateway(
    @PathParam("id") Integer gatewayId,
    @Context UriInfo uriInfo
  ) {
    IotGateways gateway = iotGatewayFacade.findByProjectAndId(project, gatewayId);
    if (gateway == null) {
      String message = "Gateway with id " + gatewayId + " not found.";
      return Response
        .status(Response.Status.NOT_FOUND)
        .entity(message)
        .type(MediaType.TEXT_PLAIN)
        .build();
    }
    
    if (gateway.getState() == IotGatewayState.ACTIVE) {
      iotGatewayFacade.removeIotGateway(gateway);
    } else if (gateway.getState() == IotGatewayState.BLOCKED) {
      iotGatewayFacade.updateState(gateway.getId(), IotGatewayState.INACTIVE_BLOCKED);
    }
    
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
      .filter(t -> Lwm2mTopics.getNamesAsList().contains(t.getName()))
      .forEach(t -> addBlockingAcl(t, id));
    
    iotGatewayFacade.updateState(id, IotGatewayState.BLOCKED);
    return Response.ok().build();
  }
  
  private void addBlockingAcl(TopicDTO t, int gatewayId) {
    IotGateways ioTGateway = iotGatewayFacade.findByProjectAndId(project, gatewayId);
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
    @PathParam("id") Integer gatewayId
  ) {
    kafkaFacade
      .findTopicsByProject(project)
      .stream()
      .filter(t -> Lwm2mTopics.getNamesAsList().contains(t.getName()))
      .forEach(t -> removeBlockingAcl(t, gatewayId));
    
    IotGateways gateway = iotGatewayFacade.findByProjectAndId(project, gatewayId);
    if (gateway.getState() == IotGatewayState.INACTIVE_BLOCKED) {
      iotGatewayFacade.removeIotGateway(gateway);
    } else if (gateway.getState() == IotGatewayState.BLOCKED) {
      iotGatewayFacade.updateState(gatewayId, IotGatewayState.ACTIVE);
    }
    return Response.ok().build();
  }
  
  private void removeBlockingAcl(TopicDTO t, int gatewayId) {
    String gatewayIp = iotGatewayFacade.findByProjectAndId(project, gatewayId).getHostname();
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
