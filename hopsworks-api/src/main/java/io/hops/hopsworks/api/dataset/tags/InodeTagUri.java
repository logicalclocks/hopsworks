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
package io.hops.hopsworks.api.dataset.tags;

import io.hops.hopsworks.api.tags.TagUri;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.tags.TagsDTO;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

public class InodeTagUri implements TagUri {
  private final UriInfo uriInfo;
  
  public InodeTagUri(UriInfo uriInfo) {
    this.uriInfo = uriInfo;
  }
  
  @Override
  public TagsDTO addUri(TagsDTO dto, DatasetPath path) {
    dto.setHref(uri(uriInfo, path)
      .path("all")
      .path(path.getRelativePath().toString())
      .queryParam("datasetType", path.getDataset().getDsType())
      .build());
    return dto;
  }
  
  @Override
  public TagsDTO addUri(TagsDTO dto, DatasetPath path, String schemaName) {
    dto.setHref(uri(uriInfo, path)
      .path("schema")
      .path(schemaName)
      .path(path.getRelativePath().toString())
      .queryParam("datasetType", path.getDataset().getDsType())
      .build());
    return dto;
  }
  
  private UriBuilder uri(UriInfo uriInfo, DatasetPath path) {
    return uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(path.getAccessProject().getId()))
      //replace with ResourceRequest.Name.DATASET.toString() once it is corrected from DATASETS
      .path("dataset")
      .path(ResourceRequest.Name.TAGS.toString().toLowerCase());
  }
  
  @Override
  public UriInfo getUriInfo() {
    return uriInfo;
  }
}
