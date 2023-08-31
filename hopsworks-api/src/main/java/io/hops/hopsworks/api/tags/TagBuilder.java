/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.tags;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.tags.TagControllerIface;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TagBuilder {
  @Inject
  private TagControllerIface tagController;
  @EJB
  private TagSchemasBuilder tagSchemasBuilder;
  
  public TagsDTO build(TagUri tagUri, ResourceRequest resourceRequest, Users user, DatasetPath path)
    throws DatasetException, MetadataException, FeatureStoreMetadataException {
    TagsDTO dto = tagUri.addUri(new TagsDTO(), path);
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TAGS)) {
      Map<String, String> tags = tagController.getAll(user, path);
      if(!tags.isEmpty()) {
        for(Map.Entry<String, String> tag : tags.entrySet()) {
          TagsDTO item = tagUri.addUri(new TagsDTO(), path, tag.getKey());
          buildFull(item, tagUri, resourceRequest, tag.getKey(), tag.getValue());
          dto.addItem(item);
        }
        dto.setCount((long) dto.getItems().size());
      }
    }
    return dto;
  }
  
  public TagsDTO build(TagUri tagUri, ResourceRequest resourceRequest, Users user, DatasetPath path,
                       String name)
    throws FeatureStoreMetadataException, DatasetException, MetadataException {
    TagsDTO dto = tagUri.addUri(new TagsDTO(), path, name);
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TAGS)) {
      String value = tagController.get(user, path, name);
      buildFull(dto, tagUri, resourceRequest, name, value);
    }
    return dto;
  }
  
  //This method is here so as not to break backwards compatibility of some endpoints, used mainly by front end.
  //TODO when all endpoints migrate to correct method, cleanup this method
  @Deprecated
  public TagsDTO buildAsMap(TagUri tagUri, ResourceRequest resourceRequest, Users user, DatasetPath path,
                            String name)
    throws FeatureStoreMetadataException, DatasetException, MetadataException {
    TagsDTO dto = tagUri.addUri(new TagsDTO(), path, name);
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TAGS)) {
      TagsDTO item = tagUri.addUri(new TagsDTO(), path, name);
      String value = tagController.get(user, path, name);
      buildFull(item, tagUri, resourceRequest, name, value);
      dto.addItem(item);
    }
    dto.setCount((long) dto.getItems().size());
    return dto;
  }
  
  public TagsDTO build(TagUri tagUri, DatasetPath path, Map<String, String> tags)
    throws FeatureStoreMetadataException {
    TagsDTO dto = tagUri.addUri(new TagsDTO(), path);
    for(Map.Entry<String, String> tag : tags.entrySet()) {
      TagsDTO item = tagUri.addUri(new TagsDTO(), path, tag.getKey());
      buildBase(item, tag.getKey(), tag.getValue());
      dto.addItem(item);
    }
    return dto;
  }
  
  private TagsDTO buildFull(TagsDTO dto, TagUri tagUri, ResourceRequest resourceRequest,
                            String name, String value)
    throws FeatureStoreMetadataException {
    buildBase(dto, name, value);
    dto.setSchema(tagSchemasBuilder.build(tagUri.getUriInfo(), resourceRequest, name));
    return dto;
  }
  
  private TagsDTO buildBase(TagsDTO dto, String name, String value) {
    dto.setExpand(true);
    dto.setName(name);
    dto.setValue(value);
    return dto;
  }
}
