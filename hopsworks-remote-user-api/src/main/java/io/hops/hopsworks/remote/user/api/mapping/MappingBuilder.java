/*
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.api.mapping;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.remote.group.RemoteGroupProjectMappingFacade;
import io.hops.hopsworks.exceptions.RemoteAuthException;
import io.hops.hopsworks.persistence.entity.remote.group.RemoteGroupProjectMapping;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MappingBuilder {
  
  @EJB
  private RemoteGroupProjectMappingFacade remoteGroupProjectMappingFacade;
  
  public MappingDTO uri(MappingDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }
  
  public MappingDTO uriItem(MappingDTO dto, UriInfo uriInfo, RemoteGroupProjectMapping remoteGroupProjectMapping) {
    if (uriInfo.getAbsolutePathBuilder().toString().endsWith(Integer.toString(remoteGroupProjectMapping.getId()))) {
      dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    } else{
      dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(Integer.toString(remoteGroupProjectMapping.getId()))
        .build());
    }
    return dto;
  }
  
  public MappingDTO expand(MappingDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public MappingDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Integer id)
    throws RemoteAuthException {
    RemoteGroupProjectMapping remoteGroupProjectMapping = remoteGroupProjectMappingFacade.find(id);
    if (remoteGroupProjectMapping == null) {
      throw new RemoteAuthException(RESTCodes.RemoteAuthErrorCode.NOT_FOUND, Level.FINE, "Mapping not found.");
    }
    return build(new MappingDTO(), uriInfo, resourceRequest, remoteGroupProjectMapping);
  }
  
  public MappingDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest) {
    MappingDTO mappingDTO = new MappingDTO();
    uri(mappingDTO, uriInfo);
    expand(mappingDTO, resourceRequest);
    return items(mappingDTO, uriInfo, resourceRequest);
  }
  
  private MappingDTO items(MappingDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest) {
    AbstractFacade.CollectionInfo<RemoteGroupProjectMapping> collectionInfo =
      remoteGroupProjectMappingFacade.findAll(resourceRequest.getOffset(), resourceRequest.getLimit(),
        resourceRequest.getFilter(), resourceRequest.getSort());
    dto.setCount(collectionInfo.getCount());
    return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }
  
  private MappingDTO items(MappingDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<RemoteGroupProjectMapping> remoteGroupProjectMappings) {
    remoteGroupProjectMappings.forEach(remoteGroupProjectMapping -> dto.addItem(
      build(new MappingDTO(), uriInfo, resourceRequest, remoteGroupProjectMapping)));
    return dto;
  }
  
  public MappingDTO build(MappingDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    RemoteGroupProjectMapping mapping) {
    uriItem(dto, uriInfo, mapping);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(mapping.getId());
      dto.setProjectId(mapping.getProject().getId());
      dto.setProjectName(mapping.getProject().getName());
      dto.setProjectRole(mapping.getProjectRole());
      dto.setRemoteGroup(mapping.getRemoteGroup());
    }
    return dto;
  }
}
