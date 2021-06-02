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

package io.hops.hopsworks.api.dataset.tags;

import io.hops.hopsworks.api.tags.TagsBuilder;
import io.hops.hopsworks.api.tags.TagsDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Map;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DatasetTagsBuilder {
  
  @EJB
  private TagsBuilder tagsBuilder;

  public TagsDTO uri(TagsDTO dto, UriInfo uriInfo, DatasetPath dsPath, String key) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(dsPath.getAccessProject().getId()))
      //replace with ResourceRequest.Name.DATASET.toString() once it is corrected from DATASETS
      .path("dataset")
      .path(ResourceRequest.Name.TAGS.toString())
      .path("schema")
      .path(key)
      .path(dsPath.getRelativePath().toString())
      .queryParam("datasetType", dsPath.getDataset().getDsType())
      .build());
    return dto;
  }

  public TagsDTO uri(TagsDTO dto, UriInfo uriInfo, DatasetPath dsPath) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString())
      .path(Integer.toString(dsPath.getAccessProject().getId()))
      //replace with ResourceRequest.Name.DATASET.toString() once it is corrected from DATASETS
      .path("dataset")
      .path(ResourceRequest.Name.TAGS.toString())
      .path("all")
      .path(dsPath.getRelativePath().toString())
      .queryParam("datasetType", dsPath.getDataset().getDsType())
      .build());
    return dto;
  }
  
  private TagsDTO uriAll(TagsDTO dto, UriInfo uriInfo, DatasetPath dsPath) {
    uri(dto, uriInfo, dsPath);
    if(dto.getItems() != null) {
      for(TagsDTO item : dto.getItems()) {
        uri(item, uriInfo, dsPath, item.getName());
      }
    }
    return dto;
  }
  
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath dsPath)
    throws DatasetException, SchematizedTagException, MetadataException {
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, user, dsPath);
    uriAll(dto, uriInfo, dsPath);
    return dto;
  }
  
  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, DatasetPath dsPath, Map<String, String> tags)
    throws DatasetException, SchematizedTagException, MetadataException {
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, tags);
    uriAll(dto, uriInfo, dsPath);
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, DatasetPath dsPath, String name)
    throws SchematizedTagException, DatasetException, MetadataException {
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, user, dsPath, name);
    uri(dto, uriInfo, dsPath, name);
    return dto;
  }
}
