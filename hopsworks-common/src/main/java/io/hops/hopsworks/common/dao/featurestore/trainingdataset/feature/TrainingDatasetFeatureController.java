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

package io.hops.hopsworks.common.dao.featurestore.trainingdataset.feature;

import io.hops.hopsworks.common.dao.featurestore.feature.FeatureDTO;
import io.hops.hopsworks.common.dao.featurestore.trainingdataset.TrainingDataset;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class controlling the interaction with the training_dataset_feature table and required business logic
 */
@Stateless
public class TrainingDatasetFeatureController {
  @EJB
  private TrainingDatasetFeatureFacade trainingDatasetFeatureFacade;

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
    removeTrainingDatasetFeatures((List) trainingDataset.getFeatures());
    insertTrainingDatasetFeatures(trainingDataset, features);
  }

  /**
   * Removes a list of features from the database
   *
   * @param trainingDatasetFeatures list of features to remove
   */
  private void removeTrainingDatasetFeatures(List<TrainingDatasetFeature> trainingDatasetFeatures) {
    trainingDatasetFeatures.stream().forEach(tdf ->  trainingDatasetFeatureFacade.remove(tdf));
  }

  /**
   * Inserts a list of features into the database
   *
   * @param trainingDataset the traning dataset that the features are linked to
   * @param features the list of features to insert
   */
  private void insertTrainingDatasetFeatures(
      TrainingDataset trainingDataset, List<FeatureDTO> features) {
    List<TrainingDatasetFeature> trainingDatasetFeatures = convertFeaturesToTrainingDatasetFeatures(
        trainingDataset, features);
    trainingDatasetFeatures.forEach(tdf -> trainingDatasetFeatureFacade.persist(tdf));
  }

  /**
   * Utility method that converts a list of featureDTOs to TrainingDatasetFeature entities
   *
   * @param trainingDataset the training dataset that the features are linked to
   * @param features the list of feature DTOs to convert
   * @return a list of TrainingDatasetFeature entities
   */
  private List<TrainingDatasetFeature> convertFeaturesToTrainingDatasetFeatures(
      TrainingDataset trainingDataset, List<FeatureDTO> features) {
    return features.stream().map(f -> {
      TrainingDatasetFeature trainingDatasetFeature = new TrainingDatasetFeature();
      trainingDatasetFeature.setName(f.getName());
      trainingDatasetFeature.setTrainingDataset(trainingDataset);
      trainingDatasetFeature.setDescription(f.getDescription());
      trainingDatasetFeature.setPrimary(f.getPrimary()? 1 : 0);
      trainingDatasetFeature.setType(f.getType());
      return trainingDatasetFeature;
    }).collect(Collectors.toList());
  }

}
