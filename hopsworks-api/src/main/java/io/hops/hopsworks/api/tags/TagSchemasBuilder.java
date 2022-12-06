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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.featurestore.tag.TagSchemasFacade;
import io.hops.hopsworks.common.tags.SchemaDTO;
import io.hops.hopsworks.common.tags.TagSchemasControllerIface;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagSchemas;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TagSchemasBuilder {
  @EJB
  private TagSchemasFacade tagSchemasFacade;
  @Inject
  private TagSchemasControllerIface tagSchemasCtrl;
  
  public SchemaDTO uri(SchemaDTO dto, UriInfo uriInfo) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.TAGS.toString())
      .build());
    return dto;
  }
  
  public SchemaDTO uriItems(SchemaDTO dto, UriInfo uriInfo, TagSchemas tag) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.TAGS.toString())
      .path(tag.getName())
      .build());
    return dto;
  }
  
  public SchemaDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, String value)
    throws SchematizedTagException, GenericException {
    TagSchemas tag = tagSchemasFacade.findByName(value);
    if(tag == null) {
      throw new SchematizedTagException(RESTCodes.SchematizedTagErrorCode.TAG_SCHEMA_NOT_FOUND, Level.FINE);
    }
    ObjectMapper objectMapper = new ObjectMapper();
    return buildItem(uriInfo, resourceRequest, tag, objectMapper);
  }
  
  public SchemaDTO build(UriInfo uriInfo, ResourceRequest resourceRequest)
    throws SchematizedTagException, GenericException {
    SchemaDTO dto = new SchemaDTO();
    uri(dto, uriInfo);
    AbstractFacade.CollectionInfo collectionInfo = tagSchemasFacade.findAll(resourceRequest.getOffset(),
      resourceRequest.getLimit(), resourceRequest.getFilter(), resourceRequest.getSort());
    dto.setCount(collectionInfo.getCount());
    return items(dto, uriInfo, resourceRequest, collectionInfo.getItems());
  }
  
  private SchemaDTO items(SchemaDTO dto, UriInfo uriInfo, ResourceRequest resourceRequest,
                          List<TagSchemas> items) throws SchematizedTagException, GenericException {
    ObjectMapper objectMapper = new ObjectMapper();
    for(TagSchemas tag : items) {
      dto.addItem(buildItem(uriInfo, resourceRequest, tag, objectMapper));
    }
    return dto;
  }
  
  private SchemaDTO buildItem(UriInfo uriInfo, ResourceRequest resourceRequest, TagSchemas tag,
                              ObjectMapper objectMapper)
    throws SchematizedTagException, GenericException {
    SchemaDTO dto = new SchemaDTO();
    uriItems(dto, uriInfo, tag);
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TAG_SCHEMAS)) {
      dto.setExpand(true);
      dto.setName(tag.getName());
      dto.setValue(tag.getSchema());
      dto.setHasNestedTypes(tagSchemasCtrl.schemaHasNestedTypes(tag.getSchema()));
      dto.setHasAdditionalRules(tagSchemasCtrl.schemaHasAdditionalRules(tag.getName(), tag.getSchema(), objectMapper));
    }
    return dto;
  }
}
