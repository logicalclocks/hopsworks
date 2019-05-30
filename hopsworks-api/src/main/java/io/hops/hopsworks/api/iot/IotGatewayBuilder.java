package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.iot.IotGatewayFacade;
import io.hops.hopsworks.common.dao.iot.IotGateways;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class IotGatewayBuilder {

  private static final Logger LOGGER = Logger.getLogger(IotGatewayBuilder.class.getName());

  @EJB
  private IotGatewayFacade iotGatewayFacade;
  
  public IotGatewayDetailsDTO buildGatewayDetails(UriInfo uriInfo, ResourceRequest resourceRequest, IotGatewayDetails
    ioTGatewayDetails) {
    IotGatewayDetailsDTO dto = new IotGatewayDetailsDTO();
    URI href = uriIotGateway(uriInfo, ioTGatewayDetails).build();
    dto.setExpand(true);
    dto.setHref(href);
    dto.setId(ioTGatewayDetails.getIotGateway().getId());
    dto.setHostname(ioTGatewayDetails.getIotGateway().getHostname());
    dto.setPort(ioTGatewayDetails.getIotGateway().getPort());
    dto.setState(ioTGatewayDetails.getIotGateway().getState());
    dto.setBlockedDevicesEndpoints(ioTGatewayDetails.getBlockedDevicesEndpoints());
    dto.setCoapHost(ioTGatewayDetails.getCoapHost());
    dto.setCoapPort(ioTGatewayDetails.getCoapPort());
    dto.setCoapsHost(ioTGatewayDetails.getCoapsHost());
    dto.setCoapsPort(ioTGatewayDetails.getCoapsPort());
    dto.setConnectedDevices(ioTGatewayDetails.getConnectedDevices());
    
    return dto;
  }
  
  public IotGatewayDTO buildGateway(UriInfo uriInfo, ResourceRequest resourceRequest, IotGateways ioTGateway) {
    IotGatewayDTO dto = new IotGatewayDTO();
    URI href = uriIotGateway(uriInfo, ioTGateway).build();
    dto.setExpand(true);
    dto.setHref(href);
    dto.setId(ioTGateway.getId());
    dto.setHostname(ioTGateway.getHostname());
    dto.setPort(ioTGateway.getPort());
    dto.setState(ioTGateway.getState());
    
    return dto;
  }

  public IotGatewayDTO buildGateway(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    IotGatewayDTO dto = new IotGatewayDTO();
    URI href = uriIotGateways(uriInfo, project).build();
    dto.setExpand(true);
    dto.setHref(href);
    List<IotGateways> gateways = iotGatewayFacade.findByProject(project);
    dto.setCount(Integer.toUnsignedLong(gateways.size()));
    gateways.forEach((iotGateway) ->
      dto.addItem(buildGateway(uriInfo, resourceRequest, (IotGateways) iotGateway)));
    return dto;
  }
  
  public IotDeviceDTO buildDevice(UriInfo uriInfo, Project project, List<IotDevice> devices, Integer gatewayId) {
    IotDeviceDTO dto = new IotDeviceDTO();
    dto.setHref(uriIotNodes(gatewayId, uriInfo, project));
    dto.setCount(Integer.toUnsignedLong(devices.size()));
    devices.forEach((iotDevice) ->
      dto.addItem(buildDevice(uriInfo, (IotDevice) iotDevice, project)));
    return dto;
  }
  
  public IotDeviceDTO buildDevice(UriInfo uriInfo, IotDevice iotDevice, Project project) {
    IotDeviceDTO dto = new IotDeviceDTO();
    dto.setHref(uriIotNode(iotDevice.getGatewayId(), iotDevice.getEndpoint(), uriInfo, project));
    dto.setEndpoint(iotDevice.getEndpoint());
    dto.setHostname(iotDevice.getHostname());
    dto.setPort(iotDevice.getPort());
    dto.setGatewayId(iotDevice.getGatewayId());
    return dto;
  }
  
  private UriBuilder uriIotGateways(UriInfo uriInfo, Project project) {
    return uriInfo
      .getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.GATEWAYS.toString().toLowerCase());
  }
  
  private UriBuilder uriIotGateway(UriInfo uriInfo, IotGatewayDetails iotGatewayDetails) {
    return uriIotGateways(uriInfo, iotGatewayDetails.getIotGateway().getProject())
      .path(Integer.toString(iotGatewayDetails.getIotGateway().getId()));
  }
  
  private UriBuilder uriIotGateway(UriInfo uriInfo, IotGateways iotGateway) {
    return uriIotGateways(uriInfo, iotGateway.getProject())
      .path(Integer.toString(iotGateway.getId()));
  }
  
  private URI uriIotNodes(Integer gatewayId, UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.GATEWAYS.toString().toLowerCase())
      .path(Integer.toString(gatewayId))
      .path(ResourceRequest.Name.NODES.toString().toLowerCase())
      .build();
  }
  
  private URI uriIotNode(Integer gatewayId, String nodeId, UriInfo uriInfo, Project project) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.GATEWAYS.toString().toLowerCase())
      .path(Integer.toString(gatewayId))
      .path(ResourceRequest.Name.NODES.toString().toLowerCase())
      .path(nodeId)
      .build();
  }
}
