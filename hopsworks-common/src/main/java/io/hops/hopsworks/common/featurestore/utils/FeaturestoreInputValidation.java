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

package io.hops.hopsworks.common.featurestore.utils;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.FeaturestoreEntityDTO;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.feature.TrainingDatasetFeatureDTO;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupDTO;
import io.hops.hopsworks.common.featurestore.trainingdatasets.TrainingDatasetDTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Pattern;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeaturestoreInputValidation {
  
  public FeaturestoreInputValidation() {
  }
  
  /**
   * Verify entity names input by the user for creation of entities in the featurestore
   *
   * @param featurestoreEntityDTO the user input data for the entity
   * @throws FeaturestoreException
   */
  public void verifyUserInput(FeaturestoreEntityDTO featurestoreEntityDTO)
    throws FeaturestoreException {
    
    Pattern namePattern = FeaturestoreConstants.FEATURESTORE_REGEX;
    
    // name
    if (!namePattern.matcher(featurestoreEntityDTO.getName()).matches()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_ENTITY_NAME, Level.FINE,
        ", the provided name " + featurestoreEntityDTO.getName() + " is invalid. Entity names can only contain lower " +
          "case characters, numbers and underscores and cannot be longer than " +
          FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
    }
    
    // description - can be empty
    verifyDescription(featurestoreEntityDTO);
    
    // features
    if (featurestoreEntityDTO instanceof FeaturegroupDTO) {
      verifyFeatureGroupFeatureList(((FeaturegroupDTO)featurestoreEntityDTO).getFeatures());
    } else if (featurestoreEntityDTO instanceof TrainingDatasetDTO) {
      TrainingDatasetDTO trainingDatasetDTO = (TrainingDatasetDTO)featurestoreEntityDTO;
      if (trainingDatasetDTO.getQueryDTO() == null && trainingDatasetDTO.getFeatures() != null) {
        // during updates the features are null
        verifyTrainingDatasetFeatureList(((TrainingDatasetDTO) featurestoreEntityDTO).getFeatures());
      }
    }
  }
  
  public void verifyDescription(FeaturestoreEntityDTO featurestoreEntityDTO) throws FeaturestoreException {
    if (!Strings.isNullOrEmpty(featurestoreEntityDTO.getDescription()) &&
      featurestoreEntityDTO.getDescription().length() >
        FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_ENTITY_DESCRIPTION, Level.FINE,
        ", the provided description for the entity " + featurestoreEntityDTO.getName() + " is too long with "
          + featurestoreEntityDTO.getDescription().length() + " characters. Entity descriptions cannot be longer than "
          + FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH + " characters.");
    }
  }
  
  /**
   * Verifies the user input feature list for a feature group entity
   * @param featureGroupFeatureDTOS the feature list to verify
   */
  public void verifyFeatureGroupFeatureList(List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS)
      throws FeaturestoreException {
    if (featureGroupFeatureDTOS != null && !featureGroupFeatureDTOS.isEmpty()) {
      for (FeatureGroupFeatureDTO featureGroupFeatureDTO : featureGroupFeatureDTOS) {
        nameValidation(featureGroupFeatureDTO.getName());
        descriptionValidation(featureGroupFeatureDTO.getName(), featureGroupFeatureDTO.getDescription());
      }
    }
  }

  /**
   * Verifies the user input feature list for a training dataset entity with no query
   * @param trainingDatasetFeatureDTOS the feature list to verify
   */
  private void verifyTrainingDatasetFeatureList(List<TrainingDatasetFeatureDTO> trainingDatasetFeatureDTOS)
      throws FeaturestoreException {
    for (TrainingDatasetFeatureDTO trainingDatasetFeatureDTO : trainingDatasetFeatureDTOS) {
      nameValidation(trainingDatasetFeatureDTO.getName());
    }
  }

  private void nameValidation(String name) throws FeaturestoreException {
    Pattern namePattern = FeaturestoreConstants.FEATURESTORE_REGEX;
    if (!namePattern.matcher(name).matches()) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME, Level.FINE,
          ", the provided feature name " + name + " is invalid. Feature names can only contain lower case " +
              "characters, numbers and underscores and cannot be longer than " +
              FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
    }
  }

  private void descriptionValidation(String name, String description) throws FeaturestoreException {
    if (!Strings.isNullOrEmpty(description) &&
        description.length() > FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION, Level.FINE,
          ", the provided feature description of " + name + " is too long with " +
              description + " characters. Feature descriptions cannot " +
              "be longer than " + FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH + " characters.");
    }
  }
}
