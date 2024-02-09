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

import io.hops.hopsworks.api.modelregistry.ModelRegistryTagResource;
import io.hops.hopsworks.api.modelregistry.models.ModelUtils;
import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dataset.util.DatasetHelper;
import io.hops.hopsworks.common.dataset.util.DatasetPath;
import io.hops.hopsworks.exceptions.DatasetException;
import io.hops.hopsworks.persistence.entity.dataset.DatasetType;
import io.hops.hopsworks.persistence.entity.models.version.ModelVersion;

import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelTagResource extends ModelRegistryTagResource {
  
  @EJB
  private ModelUtils modelUtils;
  @EJB
  private DatasetHelper datasetHelper;
  
  private ModelVersion modelVersion;
  
  /**
   * Sets the model version for the tag resource
   *
   * @param modelVersion
   */
  @Logged(logLevel = LogLevel.OFF)
  public void setModel(ModelVersion modelVersion) {
    this.modelVersion = modelVersion;
  }
  
  @Override
  protected DatasetPath getDatasetPath() throws DatasetException {
    return datasetHelper.getDatasetPath(project, modelUtils.getModelFullPath(modelRegistry,
      modelVersion.getModel().getName(), modelVersion.getModelVersionPK().getVersion()), DatasetType.DATASET);
  }
  
  @Override
  protected String getItemId() {
    return modelVersion.getMlId();
  }
  
  @Override
  protected ResourceRequest.Name getItemType() {
    return ResourceRequest.Name.MODELS;
  }
}