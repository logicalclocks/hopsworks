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

package io.hops.hopsworks.common.featurestore.featuremonitoring.config;

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.monitoringwindowconfiguration.MonitoringWindowConfigurationInputValidation;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringType;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.featurestore.trainingdataset.TrainingDatasetFeature;
import io.hops.hopsworks.persistence.entity.user.Users;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.stream.Collectors;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.ALLOWED_METRICS_IN_STATISTICS_COMPARISON_CONFIG;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_DESCRIPTION;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_FEATURE_NAME;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_NAME;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureMonitoringConfigurationInputValidation {
  @EJB
  private FeatureMonitoringConfigurationFacade featureMonitoringConfigurationFacade;
  @EJB
  private FeaturegroupController featureGroupController;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private MonitoringWindowConfigurationInputValidation monitoringWindowConfigurationInputValidation;
  
  public static final String FIELD_EXCEEDS_MAXIMAL_LENGTH_MESSAGE =
    "%s exceeds maximum length of %d characters in feature monitoring congiguration %s";
  public static final String FIELD_MUST_BE_NOT_NULL =
    "Field %s cannot be null if window config type is %s in feature monitoring congiguration %s";
  public static final String FIELD_MUST_BE_NULL =
    "Field %s must be null if window config type is %s in feature monitoring congiguration %s";
  
  public boolean validateConfigOnCreate(Users user, FeatureMonitoringConfigurationDTO dto,
    Featuregroup featureGroup, FeatureView featureView) throws FeaturestoreException {
    
    // On creation it should be checked that the name is unique for the entity
    validateUniqueConfigNameForEntity(dto.getName(), featureGroup, featureView,false);
    validateConfigBasedOnFeatureMonitoringType(dto);
    if (dto.getFeatureName() != null) {
      validateFeatureNameExists(user, featureGroup, featureView, dto.getFeatureName());
    }
    validateConfigDtoFieldMaximalLength(dto);
    // validate window config
    monitoringWindowConfigurationInputValidation.validateMonitoringWindowConfigDto(
      dto.getName(), dto.getDetectionWindowConfig());
    if (dto.getReferenceWindowConfig() != null) {
      monitoringWindowConfigurationInputValidation.validateMonitoringWindowConfigDto(
        dto.getName(), dto.getReferenceWindowConfig());
    }
    if (dto.getFeatureMonitoringType() == FeatureMonitoringType.STATISTICS_COMPARISON) {
      validateStatisticsComparisonConfig(dto.getName(), dto.getStatisticsComparisonConfig());
    }
    
    return true;
  }
  
  public boolean validateConfigOnUpdate(FeatureMonitoringConfigurationDTO dto, Featuregroup featureGroup,
    FeatureView featureView) {
    validateUniqueConfigNameForEntity(dto.getName(), featureGroup, featureView, true);
    validateConfigDtoFieldMaximalLength(dto);
    monitoringWindowConfigurationInputValidation.validateMonitoringWindowConfigDto(
      dto.getName(), dto.getDetectionWindowConfig());
    if (dto.getReferenceWindowConfig() != null) {
      monitoringWindowConfigurationInputValidation.validateMonitoringWindowConfigDto(
        dto.getName(), dto.getReferenceWindowConfig());
    }
    if (dto.getFeatureMonitoringType() == FeatureMonitoringType.STATISTICS_COMPARISON) {
      validateStatisticsComparisonConfig(dto.getName(), dto.getStatisticsComparisonConfig());
    }
    
    return true;
  }
  
  //////////////////////////
  ////// Config Metadata
  //////////////////////////
  
  public void validateUniqueConfigNameForEntity(String name, Featuregroup featureGroup, FeatureView featureView,
    boolean onUpdate) {
    // On creation it should throw an error if exists, on update it should throw an error if it does not exist
    String userMessage = "";
    boolean toThrowOrNotToThrow = false;
    if (onUpdate) {
      userMessage += " does not exist for ";
    } else {
      userMessage += " already exists for ";
    }
    if (featureGroup != null) {
      toThrowOrNotToThrow =
        featureMonitoringConfigurationFacade.findByFeatureGroupAndName(featureGroup, name).isPresent();
      userMessage += "feature group " + featureGroup.getName();
    } else if (featureView != null) {
      toThrowOrNotToThrow =
        featureMonitoringConfigurationFacade.findByFeatureViewAndName(featureView, name).isPresent();
      userMessage += "feature view " + featureView.getName();
    }
    if (onUpdate) {
      toThrowOrNotToThrow = !toThrowOrNotToThrow;
    }
    
    if (toThrowOrNotToThrow) {
      throw new IllegalArgumentException("A feature monitoring configuration with name " + name + userMessage);
    }
  }
  
  public void validateConfigDtoFieldMaximalLength(FeatureMonitoringConfigurationDTO dto) {
    if (dto.getName().length() > MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_NAME) {
      throw new IllegalArgumentException(
        String.format(FIELD_EXCEEDS_MAXIMAL_LENGTH_MESSAGE, "Name", MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_NAME,
          dto.getName()));
    }
    
    if (dto.getDescription() != null &&
      dto.getDescription().length() > MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_DESCRIPTION) {
      throw new IllegalArgumentException(String.format(FIELD_EXCEEDS_MAXIMAL_LENGTH_MESSAGE, "Description",
        MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_DESCRIPTION, dto.getName()));
    }
    
    if (dto.getFeatureName() != null &&
      dto.getFeatureName().length() > MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_FEATURE_NAME) {
      throw new IllegalArgumentException(String.format(FIELD_EXCEEDS_MAXIMAL_LENGTH_MESSAGE, "Feature name",
        MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_FEATURE_NAME, dto.getName()));
    }
  }
  
  public void validateFeatureNameExists(Users user, Featuregroup featureGroup, FeatureView featureView,
    String featureName) throws FeaturestoreException {
    if (featureGroup != null &&
      !featureGroupController.getFeatureNames(featureGroup, featureGroup.getFeaturestore().getProject(), user)
        .contains(featureName)) {
      throw new IllegalArgumentException(
        "The feature group " + featureGroup.getName() + " does not contain a feature with name " + featureName);
    } else if (featureView != null &&
      !featureView.getFeatures().stream().map(TrainingDatasetFeature::getName).collect(Collectors.toList())
        .contains(featureName)) {
      throw new IllegalArgumentException(
        "The feature view " + featureView.getName() + " does not contain a feature with name " + featureName);
    }
  }
  
  public void validateConfigBasedOnFeatureMonitoringType(FeatureMonitoringConfigurationDTO dto) {
    if (dto.getFeatureMonitoringType() == FeatureMonitoringType.STATISTICS_COMPUTATION) {
      if (dto.getStatisticsComparisonConfig() != null) {
        throw new IllegalArgumentException(
          "Statistics comparison configuration of feature monitoring configuration " + dto.getName() +
            " must be null if feature monitoring type is " + FeatureMonitoringType.STATISTICS_COMPUTATION +
            ". Use feature monitoring API if you do not wish to compare statistics to a reference.");
      }
      
      if (dto.getReferenceWindowConfig() != null) {
        throw new IllegalArgumentException(
          "Reference window configuration of feature monitoring configuration " + dto.getName() +
            " must be null if feature monitoring type is " + FeatureMonitoringType.STATISTICS_COMPUTATION +
            ". Use feature monitoring API if you do not wish to compare statistics to a reference.");
      }
    } else if (dto.getFeatureMonitoringType() == FeatureMonitoringType.STATISTICS_COMPARISON) {
      if (dto.getReferenceWindowConfig() == null) {
        throw new IllegalArgumentException(
          "Reference window configuration of feature monitoring configuration " + dto.getName() +
            " cannot be null if feature monitoring type is " + FeatureMonitoringType.STATISTICS_COMPARISON +
            ". Use scheduled statistics API if you wish to compare statistics to a reference.");
      }
      
      if (dto.getFeatureName() == null) {
        throw new IllegalArgumentException(
          "Feature name of feature monitoring configuration " + dto.getName() +
            " cannot be null if feature monitoring type is " + FeatureMonitoringType.STATISTICS_COMPARISON +
            ". Use monitoring statistics API to compute the statistics on a schedule of the whole entity or provide a" +
            " feature name to use single feature monitoring.");
      }
      
      if (dto.getStatisticsComparisonConfig() == null) {
        throw new IllegalArgumentException(
          "Statistics comparison configuration of feature monitoring configuration " + dto.getName() +
            " cannot be null if feature monitoring type is " + FeatureMonitoringType.STATISTICS_COMPARISON +
            ". Use scheduled statistics API if you wish to compare statistics to a reference.");
      }
    }
  }
  
  ////////////////////////////////////
  /////// Statistics Config
  ////////////////////////////////////
  public void validateStatisticsComparisonConfig(String configName,
    DescriptiveStatisticsComparisonConfigurationDTO dto) {
    if (!ALLOWED_METRICS_IN_STATISTICS_COMPARISON_CONFIG.contains(dto.getMetric().toString())) {
      throw new IllegalArgumentException(
        "The metric " + dto.getMetric() + " is not allowed in statistics comparison configuration " + configName +
          " allowed metrics are: " + ALLOWED_METRICS_IN_STATISTICS_COMPARISON_CONFIG);
    }
  }
}