/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

import io.hops.hopsworks.api.tags.TagSchemasBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureStoreTagBuilder {
  @EJB
  protected TagSchemasBuilder tagSchemasBuilder;
  
  protected UriBuilder uri(UriInfo uriInfo, Integer projectId, Integer featureStoreId,
                           ResourceRequest.Name type, Integer artifactId) {
    return uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(projectId))
      .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
      .path(Integer.toString(featureStoreId))
      .path(type.toString().toLowerCase())
      .path(Integer.toString(artifactId))
      .path(ResourceRequest.Name.TAGS.toString().toLowerCase());
  }
  
  private TagsDTO build(UriInfo uriInfo, UriBuilder uri, ResourceRequest resourceRequest,
                        String tagName, String tagValue) throws FeatureStoreMetadataException {
    TagsDTO dto = new TagsDTO();
    dto.setHref(uri.path(tagName).build());
    if(resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TAGS)) {
      dto.setExpand(true);
      dto.setName(tagName);
      dto.setValue(tagValue);
      if (resourceRequest.contains(ResourceRequest.Name.TAG_SCHEMAS)) {
        dto.setSchema(tagSchemasBuilder.build(uriInfo, resourceRequest, tagName));
      }
    }
    return dto;
  }
  
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest,
                       Integer projectId, Integer featureStoreId, ResourceRequest.Name type, Integer artifactId,
                       Map<String, ? extends FeatureStoreTag> tags)
    throws FeatureStoreMetadataException {
    UriBuilder uri = uri(uriInfo, projectId, featureStoreId, type, artifactId);
    TagsDTO dto = new TagsDTO();
    for (Map.Entry<String, ? extends FeatureStoreTag> tag : tags.entrySet()) {
      dto.addItem(build(uriInfo, uri, resourceRequest, tag.getKey(), tag.getValue().getValue()));
    }
    dto.setCount(tags.size() == 0 ? 0L : (long) dto.getItems().size());
    return dto;
  }
}
