/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.api.modelregistry.models;

import io.hops.hopsworks.api.dataset.inode.InodeBuilder;
import io.hops.hopsworks.api.dataset.inode.InodeDTO;
import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.api.modelregistry.models.tags.ModelRegistryTagUri;
import io.hops.hopsworks.api.tags.TagBuilder;
import io.hops.hopsworks.api.user.UsersBuilder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dataset.FilePreviewMode;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.common.models.version.ModelVersionFacade;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.MetadataException;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelsBuilder {

  private static final Logger LOGGER = Logger.getLogger(ModelsBuilder.class.getName());

  @EJB
  private ModelVersionFacade modelVersionFacade;

  @EJB
  private InodeBuilder inodeBuilder;
  @EJB
  private DatasetHelper datasetHelper;
  @EJB
  private ModelUtils modelUtils;
  @EJB
  private TagBuilder tagsBuilder;
  @EJB
  private UsersBuilder usersBuilder;
  
  public ModelDTO uri(ModelDTO dto, UriInfo uriInfo, Project userProject, Project modelRegistryProject) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(userProject.getId()))
      .path(ResourceRequest.Name.MODELREGISTRIES.toString().toLowerCase())
      .path(Integer.toString(modelRegistryProject.getId()))
      .path(ResourceRequest.Name.MODELS.toString().toLowerCase())
      .build());
    return dto;
  }
  
  public ModelDTO uri(ModelDTO dto, UriInfo uriInfo, Project userProject, Project modelRegistryProject,
                      ModelVersion modelVersion) {
    dto.setHref(uriInfo.getBaseUriBuilder()
      .path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(userProject.getId()))
      .path(ResourceRequest.Name.MODELREGISTRIES.toString().toLowerCase())
      .path(Integer.toString(modelRegistryProject.getId()))
      .path(ResourceRequest.Name.MODELS.toString().toLowerCase())
      .path(modelVersion.getModel().getName() + "_" + modelVersion.getModelVersionPK().getVersion())
      .build());
    return dto;
  }

  public ModelDTO expand(ModelDTO dto, ResourceRequest resourceRequest) {
    if (resourceRequest != null && resourceRequest.contains(ResourceRequest.Name.MODELS)) {
      dto.setExpand(true);
    }
    return dto;
  }

  //Build collection
  public ModelDTO build(UriInfo uriInfo,
                        ResourceRequest resourceRequest,
                        Users user,
                        Project userProject,
                        Project modelRegistryProject
  )
          throws ModelRegistryException, GenericException, FeatureStoreMetadataException, MetadataException {
    ModelDTO dto = new ModelDTO();
    uri(dto, uriInfo, userProject, modelRegistryProject);
    expand(dto, resourceRequest);
    dto.setCount(0l);
    if(dto.isExpand()) {
      try {
        AbstractFacade.CollectionInfo<ModelVersion> models = modelVersionFacade.findByProject(
          resourceRequest.getOffset(), resourceRequest.getLimit(), resourceRequest.getFilter(),
          resourceRequest.getSort(), modelRegistryProject);
        dto.setCount(models.getCount());
        String modelsDatasetPath = modelUtils.getModelsDatasetPath(userProject, modelRegistryProject);
        for(ModelVersion modelVersion: models.getItems()) {
          ModelDTO modelDTO = build(uriInfo, resourceRequest, user, userProject, modelRegistryProject, modelVersion,
            modelsDatasetPath);
          if(modelDTO != null) {
            dto.addItem(modelDTO);
          }
        }
      } catch(DatasetException e) {
        throw new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_LIST_FAILED, Level.FINE,
          "Unable to list models for project " + modelRegistryProject.getName(), e.getMessage(), e);
      }
    }
    return dto;
  }

  //Build specific
  public ModelDTO build(UriInfo uriInfo,
                        ResourceRequest resourceRequest,
                        Users user,
                        Project userProject,
                        Project modelRegistryProject,
                        ModelVersion modelVersion,
                        String modelsFolder)
          throws ModelRegistryException, GenericException, FeatureStoreMetadataException,
                 MetadataException, DatasetException {
    ModelDTO modelDTO = new ModelDTO();
    uri(modelDTO, uriInfo, userProject, modelRegistryProject, modelVersion);
    if (expand(modelDTO, resourceRequest).isExpand()) {
      modelDTO.setId(modelVersion.getModel().getName() + "_" + modelVersion.getModelVersionPK().getVersion());
      modelDTO.setName(modelVersion.getModel().getName());
      modelDTO.setVersion(modelVersion.getModelVersionPK().getVersion());
      modelDTO.setUserFullName(modelVersion.getUserFullName());
      modelDTO.setCreated(modelVersion.getCreated().getTime());
      modelDTO.setMetrics(modelVersion.getMetrics().getAttributes());
      modelDTO.setDescription(modelVersion.getDescription());
      modelDTO.setProgram(modelVersion.getProgram());
      modelDTO.setFramework(modelVersion.getFramework());
      modelDTO.setEnvironment(modelVersion.getEnvironment());
      modelDTO.setExperimentId(modelVersion.getExperimentId());
      modelDTO.setExperimentProjectName(modelVersion.getExperimentProjectName());
      modelDTO.setProjectName(modelRegistryProject.getName());
      modelDTO.setModelRegistryId(modelRegistryProject.getId());
      modelDTO.setCreator(usersBuilder.build(uriInfo, resourceRequest, modelVersion.getCreator()));

      DatasetPath modelDsPath = datasetHelper.getDatasetPath(userProject,
        modelUtils.getModelFullPath(modelRegistryProject, modelVersion.getModel().getName(),
          modelVersion.getModelVersionPK().getVersion()),
        DatasetType.DATASET);
      ModelRegistryTagUri tagUri = new ModelRegistryTagUri(uriInfo, modelRegistryProject,
        ResourceRequest.Name.MODELS, modelDTO.getId());
      modelDTO.setTags(tagsBuilder.build(tagUri, resourceRequest, user, modelDsPath));

      String modelVersionPath = modelsFolder + "/" + modelDTO.getName() + "/" + modelDTO.getVersion() + "/";

      DatasetPath modelSchemaPath = datasetHelper.getDatasetPath(userProject,
          modelVersionPath + Settings.HOPS_MODELS_SCHEMA, DatasetType.DATASET);
      if(resourceRequest.contains(ResourceRequest.Name.MODELSCHEMA) && modelSchemaPath.getInode() != null) {
        InodeDTO modelSchemaDTO = inodeBuilder.buildBlob(uriInfo, new ResourceRequest(ResourceRequest.Name.INODES),
            user, modelSchemaPath, modelSchemaPath.getInode(), FilePreviewMode.HEAD);
        modelDTO.setModelSchema(modelSchemaDTO);
      } else {
        InodeDTO modelSchemaDTO = inodeBuilder.buildResource(uriInfo, modelRegistryProject, modelSchemaPath);
        modelDTO.setModelSchema(modelSchemaDTO);
      }

      DatasetPath inputExamplePath = datasetHelper.getDatasetPath(userProject,
          modelVersionPath + Settings.HOPS_MODELS_INPUT_EXAMPLE, DatasetType.DATASET);
      if(resourceRequest.contains(ResourceRequest.Name.INPUTEXAMPLE) && inputExamplePath.getInode() != null) {
        InodeDTO inputExampleDTO = inodeBuilder.buildBlob(uriInfo, new ResourceRequest(ResourceRequest.Name.INODES),
            user, inputExamplePath,
            inputExamplePath.getInode(), FilePreviewMode.HEAD);
        modelDTO.setInputExample(inputExampleDTO);
      } else {
        InodeDTO inputExampleDTO = inodeBuilder.buildResource(uriInfo, modelRegistryProject, inputExamplePath);
        modelDTO.setInputExample(inputExampleDTO);
      }
    }
    return modelDTO;
  }
}
