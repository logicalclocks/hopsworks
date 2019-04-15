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

  public IoTGatewayDTO uri(IoTGatewayDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.GATEWAYS.toString().toLowerCase())
      .build());
    return dto;
  }

  //FIXME: imho to be removed
  private IoTGatewayDTO expand(IoTGatewayDTO dto, ResourceRequest resourceRequest) {
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

  public IoTGatewayDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, IoTGateways ioTGateway) {
    IoTGatewayDTO dto = new IoTGatewayDTO();
    uri(dto, uriInfo, ioTGateway);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(ioTGateway.getId());
      dto.setHostname(ioTGateway.getHostname());
      dto.setPort(ioTGateway.getPort());
      dto.setState(ioTGateway.getState());
    }
    return dto;
  }

  public IoTGatewayDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    IoTGatewayDTO dto = new IoTGatewayDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      List<IoTGateways> gateways = gatewayFacade.findByProject(project);
      dto.setCount(Integer.toUnsignedLong(gateways.size()));
      LOGGER.info("Collected IoTGateway items: " + dto.getCount());
      gateways.forEach((iotGateway) ->
        dto.addItem(build(uriInfo, resourceRequest, (IoTGateways) iotGateway)));
    }
    return dto;
  }
}
