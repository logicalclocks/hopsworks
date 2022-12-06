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
package io.hops.hopsworks.api.modelregistry;

import io.hops.hopsworks.api.modelregistry.dto.ModelRegistryDTO;
import io.hops.hopsworks.api.modelregistry.models.ModelsBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.SchematizedTagException;
import io.hops.hopsworks.persistence.entity.dataset.Dataset;
import io.hops.hopsworks.persistence.entity.dataset.DatasetSharedWith;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelRegistryBuilder {

  @EJB
  private ModelsBuilder modelsBuilder;
  
  public ModelRegistryDTO uri(ModelRegistryDTO dto, UriInfo uriInfo, Project project) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId()))
      .path(ResourceRequest.Name.MODELREGISTRIES.toString().toLowerCase())
      .build());
    return dto;
  }
  
  public ModelRegistryDTO uri(ModelRegistryDTO dto, UriInfo uriInfo, Project project, Project modelRegistryProject) {
    dto.setHref(uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
          .path(Integer.toString(project.getId()))
          .path(ResourceRequest.Name.MODELREGISTRIES.toString().toLowerCase())
          .path(modelRegistryProject.getId().toString())
          .build());
    return dto;
  }

  public ModelRegistryDTO expand(ModelRegistryDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.MODELREGISTRIES)) {
      dto.setExpand(true);
    }
    return dto;
  }

  //Build collection
  public ModelRegistryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, Project project)
    throws GenericException, ModelRegistryException, SchematizedTagException, MetadataException {
    ModelRegistryDTO dto = new ModelRegistryDTO();
    uri(dto, uriInfo, project);
    expand(dto, resourceRequest);

    Collection<Dataset> dsInProject = project.getDatasetCollection();
    // Add all datasets shared with the project
    dsInProject.addAll(project.getDatasetSharedWithCollection().stream()
            // Filter out datasets which have not been accepted
            .filter(DatasetSharedWith::getAccepted)
            .map(DatasetSharedWith::getDataset).collect(Collectors.toList()));

    Collection<Dataset> modelsDatasets = dsInProject.stream()
            .filter(ds -> ds.getName().equals(Settings.HOPS_MODELS_DATASET))
            .collect(Collectors.toList());

    dto.setCount((long)modelsDatasets.size());

    for(Dataset ds: modelsDatasets) {
      ModelRegistryDTO modelRegistryDTO = build(uriInfo, resourceRequest, user, project, ds.getProject());
      if(modelRegistryDTO != null) {
        dto.addItem(modelRegistryDTO);
      }
    }
    return dto;
  }

  //Build specific
  public ModelRegistryDTO build(UriInfo uriInfo, ResourceRequest resourceRequest, Users user, Project userProject,
                                Project modelRegistryProject) throws GenericException, ModelRegistryException,
    SchematizedTagException, MetadataException {
    ModelRegistryDTO dto = new ModelRegistryDTO();
    uri(dto, uriInfo, userProject, modelRegistryProject);
    expand(dto, resourceRequest);
    dto.setParentProjectId(modelRegistryProject.getId());
    dto.setParentProjectName(modelRegistryProject.getName());
    if(dto.isExpand()) {
      dto.setModels(modelsBuilder.build(uriInfo, resourceRequest.get(ResourceRequest.Name.MODELS),
              user, userProject, modelRegistryProject));
    }
    return dto;
  }
}