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
package io.hops.hopsworks.api.tags;

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.tag.FeatureStoreTagFacade;
import io.hops.hopsworks.exceptions.FeatureStoreTagException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag;
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
public class TagSchemasBuilder {
  @EJB
  private FeatureStoreTagFacade featureStoreTagFacade;
  
  public SchemaDTO uri(SchemaDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getAbsolutePathBuilder().build());
    return dto;
  }
  
  public SchemaDTO uriItems(SchemaDTO dto, UriInfo uriInfo, FeatureStoreTag tag) {
    dto.setHref(uriInfo.getAbsolutePathBuilder()
      .path(tag.getName())
      .build());
    return dto;
  }
  
  public SchemaDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, String value)
    throws FeatureStoreTagException {
    FeatureStoreTag tag = featureStoreTagFacade.findByName(value);
    if(tag == null) {
      throw new FeatureStoreTagException(RESTCodes.FeatureStoreTagErrorCode.TAG_SCHEMA_NOT_FOUND, Level.FINE);
    }
    return buildItem(uriInfo, resourceRequest, tag);
  }
  
  public SchemaDTO build(UriInfo uriInfo, ResourceRequest resourceRequest) {
    SchemaDTO dto = new SchemaDTO();
    uri(dto, uriInfo);
    AbstractFacade.CollectionInfo collectionInfo = featureStoreTagFacade.findAll(resourceRequest.getOffset(),
      resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort());
    dto.setCount(collectionInfo.getCount());
    return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }
  
  private SchemaDTO items(SchemaDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
                          List<FeatureStoreTag> items) {
    items.forEach(tag -> dto.addItem(buildItem(uriInfo, resourceRequest, tag)));
    return dto;
  }
  
  private SchemaDTO buildItem(UriInfo uriInfo, ResourceRequest resourceRequest, FeatureStoreTag tag) {
    SchemaDTO dto = new SchemaDTO();
    uriItems(dto, uriInfo, tag);
    dto.setName(tag.getName());
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TAG_SCHEMAS)) {
      dto.setExpand(true);
      dto.setValue(tag.getSchema());
    }
    return dto;
  }
}
