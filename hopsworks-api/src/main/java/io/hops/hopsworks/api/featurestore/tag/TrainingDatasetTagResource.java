/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

import io.hops.hopsworks.audit.logger.LogLevel;
import io.hops.hopsworks.audit.logger.annotation.Logged;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.metadata.AttachMetadataResult;
import io.hops.hopsworks.common.tags.TagsDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.UriInfo;
import java.util.Map;
import java.util.Optional;

@Logged
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TrainingDatasetTagResource extends FeatureStoreTagResource {

  private TrainingDataset trainingDataset;

  /**
   * Sets the training dataset of the tag resource
   *
   * @param trainingDataset
   */
  @Logged(logLevel = LogLevel.OFF)
  public void setTrainingDataset(TrainingDataset trainingDataset) {
    this.trainingDataset = trainingDataset;
  }

  @Override
  protected Optional<FeatureStoreTag> getTag(String name) throws FeatureStoreMetadataException {
    return tagController.getTag(trainingDataset, name);
  }

  @Override
  protected Map<String, FeatureStoreTag> getTags() {
    return tagController.getTags(trainingDataset);
  }

  @Override
  protected AttachMetadataResult<FeatureStoreTag> putTag(String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException {
    return tagController.upsertTag(trainingDataset, name, value);
  }

  @Override
  protected void deleteTag(String name) throws FeatureStoreMetadataException, FeaturestoreException {
    tagController.deleteTag(trainingDataset, name);
  }

  @Override
  protected void deleteTags() throws FeaturestoreException {
    tagController.deleteTags(trainingDataset);
  }

  @Override
  protected AttachMetadataResult<FeatureStoreTag> putTags(Map<String, String> tags)
    throws FeatureStoreMetadataException, FeaturestoreException {
    return tagController.upsertTags(trainingDataset, tags);
  }

  @Override
  protected TagsDTO buildPutTags(UriInfo uriInfo, ResourceRequest request, Map<String, FeatureStoreTag> tags)
    throws FeatureStoreMetadataException {
    return tagBuilder.build(uriInfo, request, project.getId(), featureStore.getId(),
      ResourceRequest.Name.TRAININGDATASETS, trainingDataset.getId(), tags);
  }
  
  @Override
  protected TagsDTO buildGetTags(UriInfo uriInfo, ResourceRequest request, Map<String, FeatureStoreTag> tags)
    throws FeatureStoreMetadataException {
    return tagBuilder.build(uriInfo, request, project.getId(), featureStore.getId(),
      ResourceRequest.Name.TRAININGDATASETS, trainingDataset.getId(), tags);
  }
}
