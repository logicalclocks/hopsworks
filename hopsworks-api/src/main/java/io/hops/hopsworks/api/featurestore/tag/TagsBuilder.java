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
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.tag.FeatureStoreTagFacade;
import io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TagsBuilder {
  
  @EJB
  private FeatureStoreTagFacade featureStoreTagFacade;
  
  public TagsDTO uri(TagsDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    return dto;
  }
  
  public TagsDTO uriItems(TagsDTO dto, UriInfo uriInfo, FeatureStoreTag tag) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(Integer.toString(tag.getId()))
      .build());
    return dto;
  }
  
  public TagsDTO uriItemsByName(TagsDTO dto, UriInfo uriInfo, FeatureStoreTag tag) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(tag.getName())
      .build());
    return dto;
  }
  
  public TagsDTO expand(TagsDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null) {
      dto.setExpand(true);
    }
    return dto;
  }
  
  public TagsDTO buildItem(UriInfo uriInfo, ResourceRequest resourceRequest, FeatureStoreTag tag) {
    TagsDTO dto = new TagsDTO();
    uriItems(dto, uriInfo, tag);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(tag.getId());
      dto.setName(tag.getName());
      dto.setType(tag.getType());
    }
    return dto;
  }
  
  public TagsDTO buildItemByName(UriInfo uriInfo, ResourceRequest resourceRequest, FeatureStoreTag tag) {
    TagsDTO dto = new TagsDTO();
    uriItemsByName(dto, uriInfo, tag);
    expand(dto, resourceRequest);
    if (dto.isExpand()) {
      dto.setId(tag.getId());
      dto.setName(tag.getName());
      dto.setType(tag.getType());
    }
    return dto;
  }
  
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, String value) {
    FeatureStoreTag tag = featureStoreTagFacade.findByName(value);
    return buildItemByName(uriInfo, resourceRequest, tag);
  }
  
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest) {
    TagsDTO dto = new TagsDTO();
    uri(dto, uriInfo);
    AbstractFacade.CollectionInfo collectionInfo = featureStoreTagFacade.findAll(resourceRequest.getOffset(),
      resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort());
    dto.setCount(collectionInfo.getCount());
    return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }
  
  private TagsDTO items(TagsDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest, List<FeatureStoreTag> items) {
    items.forEach(tag -> dto.addItem(buildItem(uriInfo, resourceRequest, tag)));
    return dto;
  }
  
  public TagsDTO buildByName(UriInfo uriInfo, ResourceRequest resourceRequest) {
    TagsDTO dto = new TagsDTO();
    uri(dto, uriInfo);
    AbstractFacade.CollectionInfo collectionInfo = featureStoreTagFacade.findAll(resourceRequest.getOffset(),
      resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort());
    dto.setCount(collectionInfo.getCount());
    return itemsByName(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }
  
  private TagsDTO itemsByName(TagsDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
    List<FeatureStoreTag> items) {
    items.forEach(tag -> dto.addItem(buildItemByName(uriInfo, resourceRequest, tag)));
    return dto;
  }
  
}
