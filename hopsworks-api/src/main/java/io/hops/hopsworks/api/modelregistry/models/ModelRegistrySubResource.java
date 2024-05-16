/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

import io.hops.hopsworks.api.project.ProjectSubResource;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.exceptions.ProjectException;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;
import io.hops.hopsworks.persistence.entity.project.Project;

public abstract class ModelRegistrySubResource extends ProjectSubResource {
  private Integer modelRegistryId;

  private String modelId;

  public Integer getModelRegistryId() {
    return modelRegistryId;
  }

  public void setModelRegistryId(Integer modelRegistryId) {
    this.modelRegistryId = modelRegistryId;
  }

  public String getModelId() {
    return modelId;
  }

  public void setModelId(String modelId) {
    this.modelId = modelId;
  }

  public Project getModelRegistryProject(Project project) throws ModelRegistryException {
    return getModelsController().verifyModelRegistryAccess(project, modelRegistryId).getParentProject();
  }

  public Project getModelRegistryProject() throws ProjectException, ModelRegistryException {
    return getModelsController().verifyModelRegistryAccess(getProject(), modelRegistryId).getParentProject();
  }

  public ModelVersion getModelVersion(Project modelRegistryProject) throws ModelRegistryException {
    return getModelsController().getModel(modelRegistryProject, modelId);
  }

  public ModelVersion getModelVersion() throws ProjectException, ModelRegistryException {
    return getModelsController().getModel(getModelRegistryProject(), modelId);
  }

  protected abstract ModelsController getModelsController();
}
