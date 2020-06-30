/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.api.featurestore.tag;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TagsBuilder {

  public TagsDTO uri(TagsDTO dto, UriInfo uriInfo, Project project, String tagsObjectResource,
                      Integer featurestoreId, Integer tagsObjectId, String name) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(
        ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestoreId))
        .path(tagsObjectResource.toLowerCase())
        .path(Integer.toString(tagsObjectId))
        .path(ResourceRequest.Name.TAGS.toString().toLowerCase())
        .path(name)
        .build());
    return dto;
  }

  public TagsDTO uri(TagsDTO dto, UriInfo uriInfo, Project project, String tagsObjectResource,
                     Integer featurestoreId, Integer tagsObjectId) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(
        ResourceRequest.Name.PROJECT.toString().toLowerCase())
        .path(Integer.toString(project.getId()))
        .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
        .path(Integer.toString(featurestoreId))
        .path(tagsObjectResource.toLowerCase())
        .path(Integer.toString(tagsObjectId))
        .path(ResourceRequest.Name.TAGS.toString().toLowerCase())
        .build());
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                        Project project, Integer featurestoreId,
                        String fsObjectResource, Integer fsObjectId, String name) {
    TagsDTO dto = new TagsDTO();
    uri(dto, uriInfo, project, fsObjectResource, featurestoreId, fsObjectId, name);
    dto.setName(name);
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                        Project project, Integer featurestoreId, String fsObjectResource,
                        Integer fsObjectId, Map<String, String> tags) {
    TagsDTO dto = new TagsDTO();
    uri(dto, uriInfo, project, fsObjectResource, featurestoreId, fsObjectId);
    dto.setCount((long)tags.size());
    tags.forEach((k, v) -> dto.addItem(build(uriInfo, project, featurestoreId, fsObjectResource, fsObjectId, k, v)));
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, Project project, Integer featurestoreId, String fsObjectResource,
                       Integer featuregroupId, String name, String value) {
    TagsDTO dto = new TagsDTO();
    uri(dto, uriInfo, project, fsObjectResource, featurestoreId, featuregroupId, name);
    dto.setName(name);
    dto.setValue(value);
    return dto;
  }
}
