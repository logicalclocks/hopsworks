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
import io.hops.hopsworks.common.featurestore.feature.FeatureDTO;
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
    if (!Strings.isNullOrEmpty(featurestoreEntityDTO.getDescription()) &&
      featurestoreEntityDTO.getDescription().length() >
        FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_ENTITY_DESCRIPTION, Level.FINE,
        ", the provided description for the entity " + featurestoreEntityDTO.getName() + " is too long with "
          + featurestoreEntityDTO.getDescription().length() + " characters. Entity descriptions cannot be longer than "
          + FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH + " characters.");
    }
    
    // features
    verifyFeatureList(featurestoreEntityDTO.getFeatures());
  }
  
  /**
   * Verifies the user input feature list for a feature store entity
   *
   * @param featureDTOs the feature list to verify
   */
  private void verifyFeatureList(List<FeatureDTO> featureDTOs) throws FeaturestoreException {
    if (featureDTOs != null && !featureDTOs.isEmpty()) {
      Pattern namePattern = FeaturestoreConstants.FEATURESTORE_REGEX;
      for (FeatureDTO featureDTO : featureDTOs) {
        if (!namePattern.matcher(featureDTO.getName()).matches()) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_NAME, Level.FINE,
            ", the provided feature name " + featureDTO.getName() + " is invalid. Feature names can only contain " +
              "lower case characters, numbers and underscores and cannot be longer than " +
              FeaturestoreConstants.FEATURESTORE_ENTITY_NAME_MAX_LENGTH + " characters or empty.");
        }
        if (!Strings.isNullOrEmpty(featureDTO.getDescription()) &&
          featureDTO.getDescription().length() > FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH) {
          throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_FEATURE_DESCRIPTION, Level.FINE,
            ", the provided feature description of " + featureDTO.getName() + " is too long with " +
              featureDTO.getDescription().length() + " characters. Feature descriptions cannot be longer than " +
              FeaturestoreConstants.FEATURESTORE_ENTITY_DESCRIPTION_MAX_LENGTH + " characters.");
        }
      }
    }
  }
}
