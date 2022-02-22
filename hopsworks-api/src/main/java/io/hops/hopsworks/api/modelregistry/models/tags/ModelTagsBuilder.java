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

package io.hops.hopsworks.api.modelregistry.models.tags;

import io.hops.hopsworks.api.modelregistry.models.ModelUtils;
import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.api.tags.TagsBuilder;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.models.tags.ModelTagControllerIface;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.project.Project;
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
public class ModelTagsBuilder {

  @EJB
  private TagsBuilder tagsBuilder;
  @EJB
  private ModelUtils modelUtils;
  @Inject
  private ModelTagControllerIface tagController;
  @EJB
  private DatasetHelper datasetHelper;


  public TagsDTO uri(TagsDTO dto, UriInfo uriInfo, Project userProject, Project modelRegistryProject,
                       String modelId, String name) {
    dto.setHref(uriInfo.getBaseUriBuilder()
            .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
            .path(Integer.toString(userProject.getId()))
            .path(ResourceRequest.Name.MODELREGISTRIES.toString().toLowerCase())
            .path(Integer.toString(modelRegistryProject.getId()))
            .path(ResourceRequest.Name.MODELS.toString().toLowerCase())
            .path(modelId)
            .path(ResourceRequest.Name.TAGS.toString().toLowerCase())
            .path(name)
            .build());
    return dto;
  }

  public TagsDTO uri(TagsDTO dto, UriInfo uriInfo, Project userProject, Project modelRegistryProject,
                       String modelId) {
    dto.setHref(uriInfo.getBaseUriBuilder()
            .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
            .path(Integer.toString(userProject.getId()))
            .path(ResourceRequest.Name.MODELREGISTRIES.toString().toLowerCase())
            .path(Integer.toString(modelRegistryProject.getId()))
            .path(ResourceRequest.Name.MODELS.toString().toLowerCase())
            .path(modelId)
            .path(ResourceRequest.Name.TAGS.toString().toLowerCase())
            .build());
    return dto;
  }

  private TagsDTO uriAll(TagsDTO dto, UriInfo uriInfo, Project userProject, Project modelRegistryProject,
                           String modelId) {
    uri(dto, uriInfo, userProject, modelRegistryProject, modelId);
    if(dto.getItems() != null) {
      for(TagsDTO item : dto.getItems()) {
        uri(item, uriInfo, userProject, modelRegistryProject, modelId, item.getName());
      }
    }
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, Project userProject,
                       Project modelRegistryProject, ModelDTO model)
      throws SchematizedTagException, DatasetException, MetadataException {
    DatasetPath modelDsPath = datasetHelper.getDatasetPath(userProject,
        modelUtils.getModelFullPath(modelRegistryProject, model.getName(), model.getVersion()), DatasetType.DATASET);
    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, user, modelDsPath);
    uriAll(dto, uriInfo, userProject, modelRegistryProject, model.getId());
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project userProject,
                         Project modelRegistryProject, String modelId, Map<String, String> tags)
        throws SchematizedTagException, DatasetException, MetadataException {

    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, tags);
    uriAll(dto, uriInfo, userProject, modelRegistryProject, modelId);
    return dto;
  }

  public TagsDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Project userProject,
                         Project modelRegistryProject, String modelId, String name, String value)
        throws SchematizedTagException {

    TagsDTO dto = tagsBuilder.build(uriInfo, resourceRequest, name, value);
    uri(dto, uriInfo, userProject, modelRegistryProject, modelId, name);
    return dto;
  }
}
