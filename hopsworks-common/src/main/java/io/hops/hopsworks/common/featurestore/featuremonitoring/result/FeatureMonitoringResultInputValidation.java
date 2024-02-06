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

package io.hops.hopsworks.common.featurestore.featuremonitoring.result;

import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringType;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.WindowConfigurationType;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureMonitoringResultInputValidation {
  @EJB
  private FeatureMonitoringConfigurationController featureMonitoringConfigurationController;
  
  public boolean validateOnCreate(FeatureMonitoringResultDTO resultDto) throws FeaturestoreException {
    if (resultDto.getConfigId() == null) {
      throw new IllegalArgumentException("Feature monitoring config ID not provided");
    }
    
    if (resultDto.getRaisedException().equals(true)) {
      return true;
    }
    // If raising exception is false, then detection statistics id must be provided
    validateDetectionStatsField(resultDto);
    
    FeatureMonitoringConfiguration config =
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByConfigId(resultDto.getConfigId());
    
    validateReferenceStatsField(config, resultDto);
    validateDifferenceField(config, resultDto);
    
    return true;
  }
  
  private void validateDetectionStatsField(FeatureMonitoringResultDTO resultDto) {
    if (resultDto.getDetectionStatisticsId() == null) {
      throw new IllegalArgumentException("Descriptive statistics id not provided for the detection window.");
    }
    if (resultDto.getDetectionStatistics() != null) {
      throw new IllegalArgumentException("Descriptive statistics for the detection window should be registered prior " +
        "to the monitoring result.");
    }
  }
  
  private void validateReferenceStatsField(
    FeatureMonitoringConfiguration config, FeatureMonitoringResultDTO resultDto) {
    if (resultDto.getReferenceStatistics() != null) {
      throw new IllegalArgumentException("Descriptive statistics for the reference window should be registered prior " +
        "to the monitoring result.");
    }
    boolean hasReferenceField = (
      resultDto.getReferenceStatisticsId() != null || resultDto.getSpecificValue() != null
    );
    
    // Statistics only cannot have reference
    if (config.getFeatureMonitoringType().equals(FeatureMonitoringType.STATISTICS_COMPUTATION)
      && hasReferenceField) {
      throw new IllegalArgumentException(
        "Statistics Monitoring configuration " + config.getName()
          + " cannot have results with specific value or reference statistics id field."
      );
    } else if (config.getFeatureMonitoringType().equals(FeatureMonitoringType.STATISTICS_COMPUTATION)
      && !hasReferenceField)
      return;
    
    // Specific value only cannot have reference statistics and reference statistics cannot have specific value
    if (config.getReferenceWindowConfig().getWindowConfigType().equals(WindowConfigurationType.SPECIFIC_VALUE)
      && resultDto.getSpecificValue() == null) {
      throw new IllegalArgumentException(
        "Feature monitoring configuration " + config.getName() +
          " result cannot have null specific value field when the reference window is configured to use specific value."
      );
    } else if (!config.getReferenceWindowConfig().getWindowConfigType().equals(WindowConfigurationType.SPECIFIC_VALUE)
      && resultDto.getSpecificValue() != null) {
      throw new IllegalArgumentException(
        "Feature monitoring configuration " + config.getName() + " result cannot have non-null specific value field" +
          " when the reference window is configured to use descriptive statistics."
      );
    } else if (config.getReferenceWindowConfig().getWindowConfigType().equals(WindowConfigurationType.SPECIFIC_VALUE)
      && resultDto.getSpecificValue() != null) {
      return;
    }
    
    //
    if (config.getReferenceWindowConfig().getWindowConfigType().equals(WindowConfigurationType.TRAINING_DATASET) &&
      resultDto.getReferenceStatisticsId() == null) {
      throw new IllegalArgumentException(
        "Feature monitoring configuration " + config.getName() + " result cannot have null reference statistics id" +
          " field when the reference window is configured to use training dataset."
      );
    } else if (config.getReferenceWindowConfig().getWindowConfigType().equals(WindowConfigurationType.TRAINING_DATASET))
      return;
  }
  
  private void validateDifferenceField(FeatureMonitoringConfiguration config, FeatureMonitoringResultDTO resultDTO) {
    if (config.getFeatureMonitoringType().equals(FeatureMonitoringType.STATISTICS_COMPUTATION)
      && resultDTO.getDifference() != null) {
      throw new IllegalArgumentException(
        "Statistics Monitoring configuration " + config.getName()
          + " cannot have results with non-null difference field.");
    }
    
    // empty window and non-null difference field is not allowed, except for specific value
    if ((resultDTO.getEmptyDetectionWindow() || resultDTO.getEmptyReferenceWindow())
      && resultDTO.getDifference() != null && resultDTO.getSpecificValue() == null) {
      throw new IllegalArgumentException(
        "Feature Monitoring configuration " + config.getName()
          + " cannot have results with an empty window and non-null difference field.");
    }
  }
}