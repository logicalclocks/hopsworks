/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
 */
package io.hops.hopsworks.api.admin.services;

import io.hops.hopsworks.common.admin.services.HostServicesController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.kagent.HostServicesFacade;
import io.hops.hopsworks.exceptions.ServiceException;
import io.hops.hopsworks.persistence.entity.kagent.HostServices;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ServicesBuilder {
  
  @EJB
  private HostServicesFacade hostServicesFacade;
  @EJB
  private HostServicesController hostServicesController;
  
  private ServiceDTO uri(ServiceDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.SERVICES.toString())
      .build());
    return dto;
  }
  
  private ServiceDTO uriHosts(ServiceDTO dto, UriInfo uriInfo, String hostname) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.HOSTS.toString())
      .path(hostname)
      .build());
    return dto;
  }
  
  private ServiceDTO uri(ServiceDTO dto, UriInfo uriInfo, String serviceName, String hostname) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.HOSTS.toString())
      .path(hostname)
      .path(ResourceRequest.Name.SERVICES.toString())
      .path(serviceName)
      .build());
    return dto;
  }
  
  private ServiceDTO uriServices(ServiceDTO dto, UriInfo uriInfo, String serviceName) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.SERVICES.toString())
      .path(serviceName)
      .build());
    return dto;
  }
  
  private ServiceDTO uri(ServiceDTO dto, UriInfo uriInfo, HostServices service) {
    return uri(dto, uriInfo, service.getName(), service.getHost().getHostname());
  }
  
  private ServiceDTO expandServices(ServiceDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.SERVICES)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  private ServiceDTO expandHosts(ServiceDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.HOSTS)) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public ServiceDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest) {
    ServiceDTO dto = new ServiceDTO();
    uri(dto, uriInfo);
    expandServices(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = hostServicesFacade.findAll(resourceRequest.getOffset(),
        resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort());
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach((service) -> dto.addItem(build(uriInfo, (HostServices)
        service)));
    }
    return dto;
  }
  
  public ServiceDTO buildItems(UriInfo uriInfo, String hostname, ResourceRequest resourceRequest) {
    ServiceDTO dto = new ServiceDTO();
    uriHosts(dto, uriInfo, hostname);
    expandHosts(dto, resourceRequest);
    if (dto.isExpand()) {
      AbstractFacade.CollectionInfo collectionInfo = hostServicesFacade.findByHostname(
        hostname,
        resourceRequest.getOffset(),
        resourceRequest.getLimit(),
        resourceRequest.getFilter(),
        resourceRequest.getSort());
      dto.setCount(collectionInfo.getCount());
      collectionInfo.getItems().forEach((service) -> dto.addItem(build(uriInfo, (HostServices)
        service)));
    }
    return dto;
  }
  
  public ServiceDTO build(UriInfo uriInfo, HostServices service) {
    ServiceDTO dto = new ServiceDTO();
    uri(dto, uriInfo, service);
    dto.setId(service.getId());
    dto.setHostId(service.getHost().getId());
    dto.setPid(service.getPid());
    dto.setGroup(service.getGroup());
    dto.setName(service.getName());
    dto.setStatus(service.getStatus());
    dto.setUptime(service.getUptime());
    dto.setStartTime(service.getStartTime());
    dto.setStopTime(service.getStopTime());
    return dto;
  }
  
  public ServiceDTO buildItems(UriInfo uriInfo, String serviceName) {
    ServiceDTO dto = new ServiceDTO();
    uriServices(dto, uriInfo, serviceName);
    List<HostServices> services = hostServicesFacade.findServices(serviceName);
    dto.setCount(Integer.toUnsignedLong(services.size()));
    services.forEach((service) -> dto.addItem(build(uriInfo, (HostServices) service)));
    return  dto;
  }
  
  public ServiceDTO buildItem(UriInfo uriInfo, String hostname, String name) throws ServiceException {
    HostServices service = hostServicesController.findByName(name, hostname);
    ServiceDTO dto = new ServiceDTO(service);
    uri(dto, uriInfo, name, hostname);
    return dto;
  }
  
  
}
