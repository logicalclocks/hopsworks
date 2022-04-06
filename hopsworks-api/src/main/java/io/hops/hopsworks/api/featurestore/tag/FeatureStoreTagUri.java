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
package io.hops.hopsworks.api.featurestore.tag;

import io.hops.hopsworks.api.tags.TagUri;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.tags.TagsDTO;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class FeatureStoreTagUri implements TagUri {
  private final UriInfo uriInfo;
  private final Integer featureStoreId;
  private final ResourceRequest.Name itemType;
  private final Integer itemId;
  
  public FeatureStoreTagUri(UriInfo uriInfo, Integer featureStoreId, ResourceRequest.Name itemType, Integer itemId) {
    this.uriInfo = uriInfo;
    this.featureStoreId = featureStoreId;
    this.itemType = itemType;
    this.itemId = itemId;
  }
  
  @Override
  public TagsDTO addUri(TagsDTO dto, DatasetPath path) {
    dto.setHref(uri(path).build());
    return dto;
  }
  
  @Override
  public TagsDTO addUri(TagsDTO dto, DatasetPath path, String schemaName) {
    dto.setHref(uri(path).path(schemaName).build());
    return dto;
  }
  
  private UriBuilder uri(DatasetPath path) {
    UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(path.getAccessProject().getId()))
      .path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
      .path(Integer.toString(featureStoreId))
      .path(itemType.toString().toLowerCase())
      .path(Integer.toString(itemId))
      .path(ResourceRequest.Name.TAGS.toString().toLowerCase());
    return uriBuilder;
  }
  
  @Override
  public UriInfo getUriInfo() {
    return uriInfo;
  }
}
