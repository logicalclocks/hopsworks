package io.hops.hopsworks.api.admin.hosts;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.host.Hosts;
import io.hops.hopsworks.common.dao.host.HostsFacade;
import io.hops.hopsworks.common.hosts.HostsController;
import io.hops.hopsworks.exceptions.ServiceException;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HostsBuilder {
  
  @EJB
  private HostsFacade hostsFacades;
  @EJB
  private HostsController hostsController;
  
  public HostsDTO buildBasic(UriInfo uriInfo, ResourceRequest resourceRequest) {
    return null;
  }
  
  public HostsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest) {
    HostsDTO dto = new HostsDTO();
    dto.setHref(uriBase(uriInfo).build());
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = hostsFacades.findHosts(
        resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter(),
        resourceRequest.getSort());
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach((host) ->
        dto.addItem(build(uriInfo, resourceRequest, (Hosts) host)));
    }
    return dto;
  }
  
  private HostsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Hosts host) {
    HostsDTO dto;
    if (expand(resourceRequest)) {
      dto = new HostsDTO(host);
    } else {
      dto = new HostsDTO();
    }
    dto.setHref(uriHost(uriInfo, host));
    return dto;
  }
  
  private UriBuilder uriBase(UriInfo uriInfo) {
    return uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.HOSTS.toString());
  }
  
  private URI uriHost(UriInfo uriInfo, Hosts host) {
    return uriBase(uriInfo)
      .path(host.getHostname())
      .build();
  }
  
  public HostsDTO expand(HostsDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.HOSTS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public boolean expand(ResourceRequest resourceRequest) {
    return resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.HOSTS);
  }
  
  public HostsDTO buildByHostname(UriInfo uriInfo, String hostname) throws ServiceException {
    Hosts h = hostsController.findByHostname(hostname);
    HostsDTO dto = new HostsDTO(h);
    dto.setHref(uriHost(uriInfo, h));
    return dto;
  }
}
