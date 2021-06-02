/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
import io.hops.hopsworks.common.tags.DatasetTagsControllerIface;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TagsBuilder {
  @EJB
  private TagSchemasBuilder tagSchemasBuilder;
  @Inject
  private DatasetTagsControllerIface tagsController;
  
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath datasetPath)
    throws SchematizedTagException, DatasetException, MetadataException {
    
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TAGS)) {
      Map<String, String> tags = tagsController.getAll(user, datasetPath);
      return build(uriInfo, resourceRequest, tags);
    } else {
      return new TagsDTO();
    }
  }
  
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Map<String, String> tags)
    throws SchematizedTagException, DatasetException, MetadataException {
    TagsDTO dto = new TagsDTO();
    dto.setExpand(true);
    dto.setCount((long) tags.size());
    for (Map.Entry<String, String> t : tags.entrySet()) {
      dto.addItem(build(uriInfo, resourceRequest, t.getKey(), t.getValue()));
    }
    return dto;
  }
    
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath datasetPath,
                       String name)
    throws SchematizedTagException, DatasetException, MetadataException {
    
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.TAGS)) {
      String value = tagsController.get(user, datasetPath, name);
      return build(uriInfo, resourceRequest, name, value);
    } else {
      return new TagsDTO();
    }
  }
  
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, String name, String value)
    throws SchematizedTagException {
    TagsDTO dto = new TagsDTO();
    dto.setExpand(true);
    dto.setName(name);
    dto.setValue(value);
    dto.setSchema(tagSchemasBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.TAG_SCHEMAS), name));
    return dto;
  }
}
