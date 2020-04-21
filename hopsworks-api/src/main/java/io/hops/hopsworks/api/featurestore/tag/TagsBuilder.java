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
    tags.forEach((k, v) -> dto.addItem(build(uriInfo, resourceRequest,
        project, featurestoreId, fsObjectResource, fsObjectId, k, v)));
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                        Project project, Integer featurestoreId, String fsObjectResource,
                       Integer featuregroupId, String name, String value) {
    TagsDTO dto = new TagsDTO();
    uri(dto, uriInfo, project, fsObjectResource, featurestoreId, featuregroupId, name);
    dto.setName(name);
    dto.setValue(value);
    return dto;
  }
}
