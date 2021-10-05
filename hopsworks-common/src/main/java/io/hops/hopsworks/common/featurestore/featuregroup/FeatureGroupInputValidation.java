/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.featuregroup;

import io.hops.hopsworks.common.featurestore.FeaturestoreConstants;
import io.hops.hopsworks.common.featurestore.feature.FeatureGroupFeatureDTO;
import io.hops.hopsworks.common.featurestore.utils.FeaturestoreInputValidation;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureGroupInputValidation {
  
  @EJB
  private FeaturestoreInputValidation featureStoreInputValidation;
  
  /**
   * Verify entity names input by the user for creation of entities in the featurestore
   *
   * @param featuregroupDTO the user input data for the entity
   * @throws FeaturestoreException
   */
  public void verifyUserInput(FeaturegroupDTO featuregroupDTO)
    throws FeaturestoreException {
    featureStoreInputValidation.verifyUserInput(featuregroupDTO);
    
    // features
    verifyFeatureGroupFeatureList(featuregroupDTO.getFeatures());
  }

  /**
   * Verifies the user input feature list for a feature group entity
   * @param featureGroupFeatureDTOS the feature list to verify
   */
  public void verifyFeatureGroupFeatureList(List<FeatureGroupFeatureDTO> featureGroupFeatureDTOS)
    throws FeaturestoreException {
    if (featureGroupFeatureDTOS != null && !featureGroupFeatureDTOS.isEmpty()) {
      for (FeatureGroupFeatureDTO featureGroupFeatureDTO : featureGroupFeatureDTOS) {
        featureStoreInputValidation.nameValidation(featureGroupFeatureDTO.getName());
        featureStoreInputValidation.descriptionValidation(featureGroupFeatureDTO.getName(),
          featureGroupFeatureDTO.getDescription());
      }
    }
  }

  public void verifyEventTimeFeature(String name, List<FeatureGroupFeatureDTO> features) throws
    FeaturestoreException {
    // no event_time specified, skip validation
    if (name == null) return;
    Optional<FeatureGroupFeatureDTO> eventTimeFeature =
      features.stream().filter(feature -> feature.getName().equalsIgnoreCase(name)).findAny();
    if (eventTimeFeature.isPresent()) {
      if (!FeaturestoreConstants.EVENT_TIME_FEATURE_TYPES.contains(eventTimeFeature.get().getType().toUpperCase())) {
        throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ILLEGAL_EVENT_TIME_FEATURE_TYPE, Level.FINE,
          ", the provided event time feature `" + name + "` is of type `" + eventTimeFeature.get().getType() + "` " +
            "but can only be one of the following types: " + FeaturestoreConstants.EVENT_TIME_FEATURE_TYPES + ".");
      }
    } else {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.EVENT_TIME_FEATURE_NOT_FOUND, Level.FINE, ", " +
        "the provided event time feature `" + name + "` was not found among the available features: " +
        features.stream().map(FeatureGroupFeatureDTO::getName).collect(Collectors.joining(", ")) + ".");
    }
  }
}
