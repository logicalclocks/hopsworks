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

import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.metadata.FeatureStoreKeyword;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDataset;

import java.util.List;
import java.util.Set;

public interface FeatureStoreKeywordControllerIface {
  List<String> getAllKeywords();
  
  List<String> getKeywords(Featuregroup featureGroup);
  
  List<String> getKeywords(FeatureView featureView);
  
  List<String> getKeywords(TrainingDataset trainingDataset);
  
  AttachMetadataResult<FeatureStoreKeyword> insertKeyword(Featuregroup featureGroup, String keyword)
    throws FeaturestoreException;
  
  AttachMetadataResult<FeatureStoreKeyword> insertKeyword(FeatureView featureView, String keyword)
    throws FeaturestoreException;
  
  AttachMetadataResult<FeatureStoreKeyword> insertKeyword(TrainingDataset trainingDataset, String keyword)
    throws FeaturestoreException;
  
  List<String> insertKeywords(Featuregroup featureGroup, Set<String> keywords)
    throws FeaturestoreException;
  
  List<String> insertKeywords(FeatureView featureView, Set<String> keywords)
    throws FeaturestoreException;
  
  List<String> insertKeywords(TrainingDataset trainingDataset, Set<String> keywords)
    throws FeaturestoreException;
  
  List<String> replaceKeywords(Featuregroup featureGroup, Set<String> keywords)
    throws FeaturestoreException;
  
  List<String> replaceKeywords(FeatureView featureView, Set<String> keywords)
    throws FeaturestoreException;
  
  List<String> replaceKeywords(TrainingDataset trainingDataset, Set<String> keywords)
    throws FeaturestoreException;
  
  void deleteKeyword(Featuregroup featureGroup, String keyword) throws FeaturestoreException;
  
  void deleteKeyword(TrainingDataset trainingDataset, String keyword) throws FeaturestoreException;
  
  void deleteKeyword(FeatureView featureView, String keyword) throws FeaturestoreException;
  
  void deleteKeywords(Featuregroup featureGroup) throws FeaturestoreException;
  
  void deleteKeywords(TrainingDataset trainingDataset) throws FeaturestoreException;
  
  void deleteKeywords(FeatureView featureView) throws FeaturestoreException;
  
  List<String> deleteKeywords(Featuregroup featureGroup, Set<String> keywords) throws FeaturestoreException;
  
  List<String> deleteKeywords(FeatureView featureView, Set<String> keywords) throws FeaturestoreException;
  
  List<String> deleteKeywords(TrainingDataset trainingDataset, Set<String> keywords) throws FeaturestoreException;
}
