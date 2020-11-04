/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.api.admin.cloud;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.cloud.CloudRoleMappingController;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.cloud.CloudRoleMappingFacade;
import io.hops.hopsworks.exceptions.CloudException;
import io.hops.hopsworks.persistence.entity.cloud.CloudRoleMapping;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
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
public class CloudRoleMappingBuilder {
  
  @EJB
  private CloudRoleMappingFacade cloudRoleMappingFacade;
  @EJB
  private CloudRoleMappingController cloudRoleMappingController;
  
  private CloudRoleMappingDTO uri(CloudRoleMappingDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .build());
    return dto;
  }
  
  private CloudRoleMappingDTO uriItem(CloudRoleMappingDTO dto, UriInfo uriInfo, CloudRoleMapping cloudRoleMapping) {
    if (uriInfo.getAbsolutePathBuilder().toString().endsWith(Integer.toString(cloudRoleMapping.getId()))) {
      dto.setHref(uriInfo.getAbsolutePathBuilder()
        .build());
    } else{
      dto.setHref(uriInfo.getAbsolutePathBuilder()
        .path(Integer.toString(cloudRoleMapping.getId()))
        .build());
    }
    return dto;
  }
  
  public CloudRoleMappingDTO expand(CloudRoleMappingDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public CloudRoleMappingDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest) {
    return items(new CloudRoleMappingDTO(), uriInfo, resourceRequest);
  }
  
  public CloudRoleMappingDTO buildItems(UriInfo uriInfo, ResourceRequest resourceRequest, Project project) {
    return items(new CloudRoleMappingDTO(), uriInfo, resourceRequest, project);
  }
  
  private CloudRoleMappingDTO items(CloudRoleMappingDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest) {
    AbstractFacade.CollectionInfo collectionInfo = cloudRoleMappingFacade.findAll(resourceRequest.getOffset(),
      resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort());
    dto.setCount(collectionInfo.getCount());
    return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }
  
  private CloudRoleMappingDTO items(CloudRoleMappingDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    Project project) {
    AbstractFacade.CollectionInfo collectionInfo = cloudRoleMappingFacade.findAll(resourceRequest.getOffset(),
      resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort(), project);
    dto.setCount(collectionInfo.getCount());
    return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }
  
  private CloudRoleMappingDTO items(CloudRoleMappingDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<CloudRoleMapping> cloudRoleMappings) {
    cloudRoleMappings.forEach(cloudRoleMapping -> dto.addItem(
      build(uriItem(new CloudRoleMappingDTO(), uriInfo, cloudRoleMapping), resourceRequest, cloudRoleMapping)));
    return dto;
  }
  
  public CloudRoleMappingDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
    CloudRoleMapping cloudRoleMapping) {
    CloudRoleMappingDTO dto = uri(new CloudRoleMappingDTO(), uriInfo);
    return build(dto, resourceRequest, cloudRoleMapping);
  }
  
  public CloudRoleMappingDTO buildItem(UriInfo uriInfo, ResourceRequest resourceRequest,
    CloudRoleMapping cloudRoleMapping) {
    CloudRoleMappingDTO dto = uriItem(new CloudRoleMappingDTO(), uriInfo, cloudRoleMapping);
    return build(dto, resourceRequest, cloudRoleMapping);
  }
  
  private CloudRoleMappingDTO build(CloudRoleMappingDTO dto, ResourceRequest resourceRequest,
    CloudRoleMapping cloudRoleMapping) {
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(cloudRoleMapping.getId());
      dto.setProjectId(cloudRoleMapping.getProjectId().getId());
      dto.setProjectName(cloudRoleMapping.getProjectId().getName());
      dto.setProjectRole(cloudRoleMapping.getProjectRole());
      dto.setCloudRole(cloudRoleMapping.getCloudRole());
      dto.setDefaultRole(cloudRoleMapping.isDefaultRole());
    }
    return dto;
  }
  
  public CloudRoleMappingDTO buildItem(UriInfo uriInfo, ResourceRequest resourceRequest, Integer id)
    throws CloudException {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingFacade.find(id);
    if (cloudRoleMapping == null) {
      throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_NOT_FOUND, Level.FINE, "Mapping not found.");
    }
    CloudRoleMappingDTO dto = uri(new CloudRoleMappingDTO(), uriInfo);
    return build(dto, resourceRequest, cloudRoleMapping);
  }
  
  public CloudRoleMappingDTO buildItem(UriInfo uriInfo, ResourceRequest resourceRequest, Integer id, Project project)
    throws CloudException {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingFacade.findByIdAndProject(id, project);
    if (cloudRoleMapping == null) {
      throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_NOT_FOUND, Level.FINE, "Mapping not found.");
    }
    CloudRoleMappingDTO dto = uri(new CloudRoleMappingDTO(), uriInfo);
    return build(dto, resourceRequest, cloudRoleMapping);
  }
  
  public CloudRoleMappingDTO buildItemDefault(UriInfo uriInfo, ResourceRequest resourceRequest, Users user,
    Project project)
    throws CloudException {
    CloudRoleMapping cloudRoleMapping = cloudRoleMappingController.getDefault(project, user);
    if (cloudRoleMapping == null) {
      throw new CloudException(RESTCodes.CloudErrorCode.MAPPING_NOT_FOUND, Level.FINE, "No default mapping found.");
    }
    CloudRoleMappingDTO dto = uri(new CloudRoleMappingDTO(), uriInfo);
    return build(dto, resourceRequest, cloudRoleMapping);
  }
  
}
