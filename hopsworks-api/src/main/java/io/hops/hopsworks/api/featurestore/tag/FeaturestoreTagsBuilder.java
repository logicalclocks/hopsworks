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

import io.hops.hopsworks.api.tags.TagsBuilder;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreTagsBuilder {
  
  @EJB
  private TagsBuilder tagsBuilder;

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
  
  private TagsDTO uriAll(TagsDTO dto, UriInfo uriInfo, Project project, String tagsObjectResource,
                         Integer featurestoreId, Integer tagsObjectId) {
    uri(dto, uriInfo, project, tagsObjectResource, featurestoreId, tagsObjectId);
    if(dto.getItems() != null) {
      for(TagsDTO item : dto.getItems()) {
        uri(item, uriInfo, project, tagsObjectResource, featurestoreId, tagsObjectId, item.getName());
      }
    }
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Integer featurestoreId,
                       String fsObjectResource, Integer fsObjectId, Map<String, String> tags)
    throws SchematizedTagException, DatasetException, MetadataException {
    
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, tags);
    uriAll(dto, uriInfo, project, fsObjectResource, featurestoreId, fsObjectId);
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project project, Integer featurestoreId,
                       String fsObjectResource, Integer featuregroupId, String name, String value)
    throws SchematizedTagException {
    
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, name, value);
    uri(dto, uriInfo, project, fsObjectResource, featurestoreId, featuregroupId, name);
    return dto;
  }
}
