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

package io.hops.hopsworks.common.featurestore.metadata;

import io.hops.hopsworks.exceptions.FeatureStoreMetadataException;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreTag;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import java.util.Map;
import java.util.Optional;

public interface FeatureStoreTagControllerIface {
  Optional<FeatureStoreTag> getTag(Featuregroup featureGroup, String name) throws FeatureStoreMetadataException;
  
  Optional<FeatureStoreTag> getTag(FeatureView featureView, String name) throws FeatureStoreMetadataException;
  
  Optional<FeatureStoreTag> getTag(TrainingDataset trainingDataset, String name) throws FeatureStoreMetadataException;
  
  Map<String, FeatureStoreTag> getTags(Featuregroup featureGroup);
  
  Map<String, FeatureStoreTag> getTags(FeatureView featureView);
  
  Map<String, FeatureStoreTag> getTags(TrainingDataset trainingDataset);
  
  AttachMetadataResult<FeatureStoreTag> upsertTag(Featuregroup featureGroup, String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  AttachMetadataResult<FeatureStoreTag> upsertTag(FeatureView featureView, String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  AttachMetadataResult<FeatureStoreTag> upsertTag(TrainingDataset trainingDataset, String name, String value)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  AttachMetadataResult<FeatureStoreTag> upsertTags(Featuregroup featureGroup, Map<String, String> tags)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  AttachMetadataResult<FeatureStoreTag> upsertTags(FeatureView featureView, Map<String, String> tags)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  AttachMetadataResult<FeatureStoreTag> upsertTags(TrainingDataset trainingDataset, Map<String, String> tags)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  void deleteTag(Featuregroup featureGroup, String name)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  void deleteTag(TrainingDataset trainingDataset, String name)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  void deleteTag(FeatureView featureView, String name)
    throws FeatureStoreMetadataException, FeaturestoreException;
  
  void deleteTags(Featuregroup featureGroup) throws FeaturestoreException;
  
  void deleteTags(TrainingDataset trainingDataset) throws FeaturestoreException;

  void deleteTags(FeatureView featureView) throws FeaturestoreException;
}
