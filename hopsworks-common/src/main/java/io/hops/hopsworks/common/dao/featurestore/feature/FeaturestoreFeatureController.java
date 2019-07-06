/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.dao.featurestore.feature;

import io.hops.hopsworks.common.dao.featurestore.storageconnector.external_sql_query.FeaturestoreExternalSQLQuery;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the training_dataset_feature table and required business logic
 */
@Stateless
public class FeaturestoreFeatureController {
  @EJB
  private FeaturestoreFeatureFacade featurestoreFeatureFacade;

  /**
   * Updates the features of a training dataset, first deletes all existing features for the training dataset
   * and then insert the new ones.
   *
   * @param trainingDataset the training dataset to update
   * @param features the new features
   */
  public void updateTrainingDatasetFeatures(
      TrainingDataset trainingDataset, List<FeatureDTO> features) {
    if(features == null) {
      return;
    }
    removeFeatures((List) trainingDataset.getFeatures());
    insertTrainingDatasetFeatures(trainingDataset, features);
  }

  /**
   * Removes a list of features from the database
   *
   * @param featurestoreFeatures list of features to remove
   */
  private void removeFeatures(List<FeaturestoreFeature> featurestoreFeatures) {
    featurestoreFeatures.stream().forEach(tdf ->  featurestoreFeatureFacade.remove(tdf));
  }

  /**
   * Inserts a list of features into the database
   *
   * @param trainingDataset the traning dataset that the features are linked to
   * @param features the list of features to insert
   */
  private void insertTrainingDatasetFeatures(
      TrainingDataset trainingDataset, List<FeatureDTO> features) {
    List<FeaturestoreFeature> featurestoreFeatures = convertFeaturesToTrainingDatasetFeatures(
        trainingDataset, features);
    featurestoreFeatures.forEach(tdf -> featurestoreFeatureFacade.persist(tdf));
  }

  /**
   * Utility method that converts a list of featureDTOs to FeaturestoreFeature entities
   *
   * @param trainingDataset the training dataset that the features are linked to
   * @param features the list of feature DTOs to convert
   * @return a list of FeaturestoreFeature entities
   */
  private List<FeaturestoreFeature> convertFeaturesToTrainingDatasetFeatures(
      TrainingDataset trainingDataset, List<FeatureDTO> features) {
    return features.stream().map(f -> {
      FeaturestoreFeature featurestoreFeature = new FeaturestoreFeature();
      featurestoreFeature.setName(f.getName());
      featurestoreFeature.setTrainingDataset(trainingDataset);
      featurestoreFeature.setDescription(f.getDescription());
      featurestoreFeature.setPrimary(f.getPrimary()? 1 : 0);
      featurestoreFeature.setType(f.getType());
      return featurestoreFeature;
    }).collect(Collectors.toList());
  }
  
  
  /**
   * Updates the features of an external Feature Group, first deletes all existing features for the external Feature
   * Group and then insert the new ones.
   *
   * @param featurestoreExternalSQLQuery the external feature group to update
   * @param features the new features
   */
  public void updateExternalFgFeatures(
    FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery, List<FeatureDTO> features) {
    if(features == null) {
      return;
    }
    removeFeatures((List) featurestoreExternalSQLQuery.getFeatures());
    insertExternalFeaturegroupFeatures(featurestoreExternalSQLQuery, features);
  }
  
  /**
   * Inserts a list of features into the database
   *
   * @param featurestoreExternalSQLQuery the traning dataset that the features are linked to
   * @param features the list of features to insert
   */
  private void insertExternalFeaturegroupFeatures(
    FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery, List<FeatureDTO> features) {
    List<FeaturestoreFeature> featurestoreFeatures = convertFeaturesToExternalFeaturegroupFeatures(
      featurestoreExternalSQLQuery, features);
    featurestoreFeatures.forEach(tdf -> featurestoreFeatureFacade.persist(tdf));
  }
  
  /**
   * Utility method that converts a list of featureDTOs to FeaturestoreFeature entities
   *
   * @param featurestoreExternalSQLQuery the external featuregroup that the features are linked to
   * @param features the list of feature DTOs to convert
   * @return a list of FeaturestoreFeature entities
   */
  private List<FeaturestoreFeature> convertFeaturesToExternalFeaturegroupFeatures(
    FeaturestoreExternalSQLQuery featurestoreExternalSQLQuery, List<FeatureDTO> features) {
    return features.stream().map(f -> {
      FeaturestoreFeature featurestoreFeature = new FeaturestoreFeature();
      featurestoreFeature.setName(f.getName());
      featurestoreFeature.setFeaturestoreExternalSQLQuery(featurestoreExternalSQLQuery);
      featurestoreFeature.setDescription(f.getDescription());
      featurestoreFeature.setPrimary(f.getPrimary()? 1 : 0);
      featurestoreFeature.setType(f.getType());
      return featurestoreFeature;
    }).collect(Collectors.toList());
  }

}
