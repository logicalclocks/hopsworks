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

package io.hops.hopsworks.common.featurestore.featuremonitoring.monitoringwindowconfiguration;

import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.WindowConfigurationType;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_TIME_OFFSET;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_WINDOW_LENGTH;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.INVALID_MONITORING_WINDOW_CONFIG_TIME_RANGE_REGEX;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.VALID_MONITORING_WINDOW_CONFIG_TIME_RANGE_REGEX;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MonitoringWindowConfigurationInputValidation {
  
  private static final String FIELD_SPECIFIC_VALUE = "specificValue";
  private static final String FIELD_ROW_PERCENTAGE = "rowPercentage";
  private static final String FIELD_WINDOW_LENGTH = "windowLength";
  private static final String FIELD_TIME_OFFSET = "timeOffset";
  private static final String FIELD_TRAINING_DATASET_VERSION = "trainingDatasetVersion";
  private static final String FIELD_EXCEEDS_MAXIMAL_LENGTH_MESSAGE =
    "%s exceeds maximum length of %d characters in feature monitoring congiguration %s";
  private static final String FIELD_MUST_BE_NOT_NULL =
    "Field %s cannot be null if window config type is %s in feature monitoring congiguration %s";
  private static final String FIELD_MUST_BE_NULL =
    "Field %s must be null if window config type is %s in feature monitoring congiguration %s";
  
  public boolean validateMonitoringWindowConfigDto(String configName, MonitoringWindowConfigurationDTO dto) {
    validateMonitoringWindowConfigDtoNullFieldBasedOnType(configName, dto);
    if (dto.getWindowConfigType() == WindowConfigurationType.ROLLING_TIME) {
      validateMonitoringWindowConfigDtoFieldMaximalLength(configName, dto);
      validateTimeOffsetAndWindowLengthBasedOnRegex(configName, dto);
    }
    if (dto.getWindowConfigType() == WindowConfigurationType.ROLLING_TIME ||
      dto.getWindowConfigType() == WindowConfigurationType.ALL_TIME) {
      validateRowPercentage(configName, dto.getRowPercentage());
    }
    return true;
  }
  
  public void validateMonitoringWindowConfigDtoNullFieldBasedOnType(String configName,
    MonitoringWindowConfigurationDTO dto) {
    if (dto.getWindowConfigType() == WindowConfigurationType.SPECIFIC_VALUE) {
      fieldToBeOrNotToBeNull(configName, dto, FIELD_SPECIFIC_VALUE, false);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_ROW_PERCENTAGE, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_WINDOW_LENGTH, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_TIME_OFFSET, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_TRAINING_DATASET_VERSION, true);
    } else if (dto.getWindowConfigType() == WindowConfigurationType.TRAINING_DATASET) {
      fieldToBeOrNotToBeNull(configName, dto, FIELD_TRAINING_DATASET_VERSION, false);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_ROW_PERCENTAGE, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_WINDOW_LENGTH, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_TIME_OFFSET, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_SPECIFIC_VALUE, true);
    } else if (dto.getWindowConfigType() == WindowConfigurationType.ALL_TIME) {
      fieldToBeOrNotToBeNull(configName, dto, FIELD_ROW_PERCENTAGE, false);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_TIME_OFFSET, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_WINDOW_LENGTH, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_SPECIFIC_VALUE, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_TRAINING_DATASET_VERSION, true);
    } else if (dto.getWindowConfigType() == WindowConfigurationType.ROLLING_TIME) {
      fieldToBeOrNotToBeNull(configName, dto, FIELD_ROW_PERCENTAGE, false);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_TIME_OFFSET, false);
      // windowLength is optional here
      fieldToBeOrNotToBeNull(configName, dto, FIELD_TRAINING_DATASET_VERSION, true);
      fieldToBeOrNotToBeNull(configName, dto, FIELD_SPECIFIC_VALUE, true);
    }
  }
  
  public void fieldToBeOrNotToBeNull(String configName, MonitoringWindowConfigurationDTO dto, String fieldName,
    boolean mustBeNull) {
    boolean isNull = false;
    switch (fieldName) {
      case FIELD_SPECIFIC_VALUE:
        if (dto.getSpecificValue() != null) {
          isNull = true;
        }
        break;
      case FIELD_ROW_PERCENTAGE:
        if (dto.getRowPercentage() != null) {
          isNull = true;
        }
        break;
      case FIELD_WINDOW_LENGTH:
        if (dto.getWindowLength() != null) {
          isNull = true;
        }
        break;
      case FIELD_TIME_OFFSET:
        if (dto.getTimeOffset() != null) {
          isNull = true;
        }
        break;
      case FIELD_TRAINING_DATASET_VERSION:
        if (dto.getTrainingDatasetVersion() != null) {
          isNull = true;
        }
        break;
      default:
        break;
    }
    if (isNull && mustBeNull) {
      throw new IllegalArgumentException(
        String.format(FIELD_MUST_BE_NULL, fieldName, dto.getWindowConfigType().toString(), configName));
    } else if (!isNull && !mustBeNull) {
      throw new IllegalArgumentException(
        String.format(FIELD_MUST_BE_NOT_NULL, fieldName, dto.getWindowConfigType().toString(), configName));
    }
  }
  
  public void validateMonitoringWindowConfigDtoFieldMaximalLength(String configName,
    MonitoringWindowConfigurationDTO dto) {
    if (dto.getWindowLength() != null &&
      dto.getWindowLength().length() > MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_WINDOW_LENGTH) {
      throw new IllegalArgumentException(String.format(FIELD_EXCEEDS_MAXIMAL_LENGTH_MESSAGE, "Window length",
        MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_WINDOW_LENGTH, configName));
    }
    
    if (dto.getTimeOffset().length() > MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_TIME_OFFSET) {
      throw new IllegalArgumentException(String.format(FIELD_EXCEEDS_MAXIMAL_LENGTH_MESSAGE, "Time offset",
        MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_TIME_OFFSET, configName));
    }
  }
  
  public void validateRowPercentage(String configName, Float rowPercentage) {
    if (rowPercentage < 0 || rowPercentage > 1) {
      throw new IllegalArgumentException(
        "Row percentage of monitoring configuration " + configName + " must be a float between 0 and 1, not" +
          rowPercentage);
    }
  }
  
  public void validateTimeOffsetAndWindowLengthBasedOnRegex(String configName, MonitoringWindowConfigurationDTO dto) {
    if (dto.getWindowConfigType() == WindowConfigurationType.ROLLING_TIME) {
      if (INVALID_MONITORING_WINDOW_CONFIG_TIME_RANGE_REGEX.matcher(dto.getTimeOffset()).matches() ||
        !VALID_MONITORING_WINDOW_CONFIG_TIME_RANGE_REGEX.matcher(dto.getTimeOffset()).matches()) {
        throw new IllegalArgumentException(
          "Time offset of monitoring configuration " + configName +
            " must be in format 1w2d3h for 1 week 2 day 3 hours.");
      }
      if (dto.getWindowLength() != null &&
        (INVALID_MONITORING_WINDOW_CONFIG_TIME_RANGE_REGEX.matcher(dto.getWindowLength()).matches() ||
          !VALID_MONITORING_WINDOW_CONFIG_TIME_RANGE_REGEX.matcher(dto.getWindowLength()).matches())) {
        throw new IllegalArgumentException(
          "Window length of monitoring configuration " + configName +
            " must be in format 1w2d3h for 1 week 2 day 3 hours.");
      }
    }
  }
}