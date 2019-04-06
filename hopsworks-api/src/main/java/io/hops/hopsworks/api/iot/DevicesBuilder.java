package io.hops.hopsworks.api.iot;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.iot.DevicesFacade;
import io.hops.hopsworks.common.dao.iot.IoTGateways;
import io.hops.hopsworks.common.dao.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DevicesBuilder {

  private static final Logger LOGGER = Logger.getLogger(DevicesBuilder.class.getName());

  @EJB
  private DevicesFacade devicesFacade;

  public IoTGatewayDTO uri(IoTGatewayDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.IOTGATEWAY.toString().toLowerCase())
      .build());
    return dto;
  }

  //FIXME: imho to be removed
  private IoTGatewayDTO expand(IoTGatewayDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.IOTGATEWAY)) {
      dto.setExpand(true);
    }
    return dto;
  }

  public IoTGatewayDTO uri(IoTGatewayDTO dto, UriInfo uriInfo, IoTGateways ioTGateway) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(ioTGateway.getProject().getId()))
      .path(ResourceRequest.Name.JOBS.toString().toLowerCase())
      .path(ioTGateway.getName())
      .build());
    return dto;
  }

  public IoTGatewayDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, IoTGateways ioTGateway) {
    IoTGatewayDTO dto = new IoTGatewayDTO();
    uri(dto, uriInfo, ioTGateway);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(ioTGateway.getId());
      dto.setName(ioTGateway.getName());
      dto.setIpAddress(ioTGateway.getIpAddress());
      dto.setPort(ioTGateway.getPort());
    }
    return dto;
  }

  public IoTGatewayDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    IoTGatewayDTO dto = new IoTGatewayDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);
    LOGGER.info("Expand IoTGateway: " + dto.isExpand());
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = devicesFacade.findByProject(project);
      dto.setCount(collectionInfo.getCount());
      LOGGER.info("Collected IoTGateway items: " + dto.getCount());
      collectionInfo.getItems().forEach((iotGateway) ->
        dto.addItem(build(uriInfo, resourceRequest, (IoTGateways) iotGateway)));
    }
    return dto;
  }
}
