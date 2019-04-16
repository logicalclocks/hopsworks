package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.iot.GatewayFacade;
import io.hops.hopsworks.common.dao.iot.IoTGateways;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class GatewaysBuilder {

  private static final Logger LOGGER = Logger.getLogger(GatewaysBuilder.class.getName());

  @EJB
  private GatewayFacade gatewayFacade;

  public IoTGatewayDTO uriGateway(IoTGatewayDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.GATEWAYS.toString().toLowerCase())
      .build());
    return dto;
  }

  //FIXME: imho to be removed
  private IoTGatewayDTO expandGateway(IoTGatewayDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.GATEWAYS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public IoTGatewayDTO uri(IoTGatewayDTO dto, UriInfo uriInfo, IoTGateways ioTGateway) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(ioTGateway.getProject().getId()))
      .path(ResourceRequest.Name.GATEWAYS.toString().toLowerCase())
      .path(Integer.toString(ioTGateway.getId()))
      .build());
    return dto;
  }
  
  public IoTDeviceDTO uriDevice(IoTDeviceDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.GATEWAYS.toString().toLowerCase())
      .path(Integer.toString(dto.getGatewayId()))
      .path(ResourceRequest.Name.NODES.toString().toLowerCase())
      .build());
    return dto;
  }
  
  public IoTDeviceDTO uriDeviceNode(IoTDeviceDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.GATEWAYS.toString().toLowerCase())
      .path(Integer.toString(dto.getGatewayId()))
      .path(ResourceRequest.Name.NODES.toString().toLowerCase())
      .path(dto.getHostname())
      .build());
    return dto;
  }

  public IoTGatewayDTO buildGateway(UriInfo uriInfo, ResourceRequest resourceRequest, IoTGateways ioTGateway) {
    IoTGatewayDTO dto = new IoTGatewayDTO();
    uri(dto, uriInfo, ioTGateway);
    expandGateway(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(ioTGateway.getId());
      dto.setHostname(ioTGateway.getHostname());
      dto.setPort(ioTGateway.getPort());
      dto.setState(ioTGateway.getState());
    }
    return dto;
  }

  public IoTGatewayDTO buildGateway(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    IoTGatewayDTO dto = new IoTGatewayDTO();
    uriGateway(dto, uriInfo, project);
    expandGateway(dto, resourceRequest);
    if (dto.isExpand()) {
      List<IoTGateways> gateways = gatewayFacade.findByProject(project);
      dto.setCount(Integer.toUnsignedLong(gateways.size()));
      gateways.forEach((iotGateway) ->
        dto.addItem(buildGateway(uriInfo, resourceRequest, (IoTGateways) iotGateway)));
    }
    return dto;
  }
  
  public IoTDeviceDTO buildDevice(UriInfo uriInfo, Project project, List<IoTDevice> devices) {
    IoTDeviceDTO dto = new IoTDeviceDTO();
    uriDevice(dto, uriInfo, project);
    dto.setCount(Integer.toUnsignedLong(devices.size()));
    devices.forEach((ioTDevice) ->
      dto.addItem(buildDevice(uriInfo, (IoTDevice) ioTDevice, project)));
    return dto;
  }
  
  private IoTDeviceDTO buildDevice(UriInfo uriInfo, IoTDevice ioTDevice, Project project) {
    IoTDeviceDTO dto = new IoTDeviceDTO();
    uriDeviceNode(dto, uriInfo, project);
    dto.setEndpoint(ioTDevice.getEndpoint());
    dto.setHostname(ioTDevice.getHostname());
    dto.setPort(ioTDevice.getPort());
    return dto;
  }
}
