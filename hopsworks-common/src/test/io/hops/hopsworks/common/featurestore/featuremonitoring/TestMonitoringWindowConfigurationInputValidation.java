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
package io.hops.hopsworks.common.featurestore.featuremonitoring;

import io.hops.hopsworks.common.featurestore.featuremonitoring.monitoringwindowconfiguration.MonitoringWindowConfigurationDTO;
import io.hops.hopsworks.common.featurestore.featuremonitoring.monitoringwindowconfiguration.MonitoringWindowConfigurationInputValidation;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.WindowConfigurationType;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.apache.commons.lang.NotImplementedException;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_TIME_OFFSET;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_WINDOW_LENGTH;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestMonitoringWindowConfigurationInputValidation {
  private final MonitoringWindowConfigurationInputValidation inputValidation =
    new MonitoringWindowConfigurationInputValidation();
  private final String configName = "testConfigName";
  
  public MonitoringWindowConfigurationDTO makeValidMonitoringWindowConfiguration(
    WindowConfigurationType windowConfigType) {
    MonitoringWindowConfigurationDTO dto = new MonitoringWindowConfigurationDTO();
    dto.setWindowConfigType(windowConfigType);
    
    switch (windowConfigType) {
      case ROLLING_TIME:
        return populateValidRollingWindowConfiguration(dto);
      case ALL_TIME:
        return populateValidAllTimeWindowConfiguration(dto);
      case TRAINING_DATASET:
        return populateValidTrainingDatasetWindowConfiguration(dto);
      case SPECIFIC_VALUE:
        return populateValidSpecificValueWindowConfiguration(dto);
      default:
        throw new NotImplementedException("Implement unit test for this monitoring window configuration type");
    }
  }
  
  private MonitoringWindowConfigurationDTO populateValidRollingWindowConfiguration(
    MonitoringWindowConfigurationDTO windowConfigDTO) {
    windowConfigDTO.setWindowLength("2w");
    windowConfigDTO.setTimeOffset("1w");
    windowConfigDTO.setRowPercentage(0.1f);
    
    return windowConfigDTO;
  }
  
  private MonitoringWindowConfigurationDTO populateValidAllTimeWindowConfiguration(
    MonitoringWindowConfigurationDTO windowConfigDTO) {
    windowConfigDTO.setRowPercentage(0.1f);
    
    return windowConfigDTO;
  }
  
  private MonitoringWindowConfigurationDTO populateValidTrainingDatasetWindowConfiguration(
    MonitoringWindowConfigurationDTO windowConfigDTO) {
    windowConfigDTO.setTrainingDatasetVersion(12);
    
    return windowConfigDTO;
  }
  
  private MonitoringWindowConfigurationDTO populateValidSpecificValueWindowConfiguration(
    MonitoringWindowConfigurationDTO windowConfigDTO) {
    windowConfigDTO.setSpecificValue(1.2);
    
    return windowConfigDTO;
  }
  
  @Test
  public void testAcceptAllValidMonitoringWindowConfiguration() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ROLLING_TIME);
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    windowConfigDTO = makeValidMonitoringWindowConfiguration(WindowConfigurationType.ALL_TIME);
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    windowConfigDTO = makeValidMonitoringWindowConfiguration(WindowConfigurationType.TRAINING_DATASET);
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    windowConfigDTO = makeValidMonitoringWindowConfiguration(WindowConfigurationType.SPECIFIC_VALUE);
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
  }
  
  @Test
  public void testEnforceNullFieldsForSpecificValue() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.SPECIFIC_VALUE);
    
    windowConfigDTO.setSpecificValue(null);
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("SPECIFIC_VALUE"));
    assertTrue(illegalArgumentException.getMessage().contains("specificValue"));
    assertTrue(illegalArgumentException.getMessage().contains("cannot be null"));
    
    windowConfigDTO.setSpecificValue(1.);
    windowConfigDTO.setWindowLength("2w");
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("SPECIFIC_VALUE"));
    assertTrue(illegalArgumentException.getMessage().contains("windowLength"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setWindowLength(null);
    windowConfigDTO.setTimeOffset("1w");
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("SPECIFIC_VALUE"));
    assertTrue(illegalArgumentException.getMessage().contains("timeOffset"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setTimeOffset(null);
    windowConfigDTO.setRowPercentage(0.1f);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("SPECIFIC_VALUE"));
    assertTrue(illegalArgumentException.getMessage().contains("rowPercentage"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setRowPercentage(null);
    windowConfigDTO.setTrainingDatasetVersion(12);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("SPECIFIC_VALUE"));
    assertTrue(illegalArgumentException.getMessage().contains("trainingDatasetVersion"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
  }
  
  @Test
  public void testEnforceNullFieldsForTrainingDataset() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.TRAINING_DATASET);
    
    windowConfigDTO.setTrainingDatasetVersion(null);
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("TRAINING_DATASET"));
    assertTrue(illegalArgumentException.getMessage().contains("trainingDatasetVersion"));
    assertTrue(illegalArgumentException.getMessage().contains("cannot be null"));
    
    windowConfigDTO.setTrainingDatasetVersion(12);
    windowConfigDTO.setWindowLength("2w");
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("TRAINING_DATASET"));
    assertTrue(illegalArgumentException.getMessage().contains("windowLength"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setWindowLength(null);
    windowConfigDTO.setTimeOffset("1w");
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("TRAINING_DATASET"));
    assertTrue(illegalArgumentException.getMessage().contains("timeOffset"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setTimeOffset(null);
    windowConfigDTO.setRowPercentage(0.1f);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("TRAINING_DATASET"));
    assertTrue(illegalArgumentException.getMessage().contains("rowPercentage"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setRowPercentage(null);
    windowConfigDTO.setSpecificValue(1.2);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("TRAINING_DATASET"));
    assertTrue(illegalArgumentException.getMessage().contains("specificValue"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
  }
  
  @Test
  public void testEnforceNullFieldsForAllTime() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ALL_TIME);
    
    windowConfigDTO.setTrainingDatasetVersion(12);
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ALL_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("trainingDatasetVersion"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setTrainingDatasetVersion(null);
    windowConfigDTO.setWindowLength("2w");
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ALL_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("windowLength"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setWindowLength(null);
    windowConfigDTO.setTimeOffset("1w");
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ALL_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("timeOffset"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setTimeOffset(null);
    windowConfigDTO.setRowPercentage(null);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ALL_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("rowPercentage"));
    assertTrue(illegalArgumentException.getMessage().contains("cannot be null"));
    
    windowConfigDTO.setRowPercentage(0.1f);
    windowConfigDTO.setSpecificValue(1.2);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ALL_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("specificValue"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
  }
  
  @Test
  public void testEnforceNullFieldsForRollingTime() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ROLLING_TIME);
    
    windowConfigDTO.setTimeOffset(null);
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ROLLING_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("timeOffset"));
    assertTrue(illegalArgumentException.getMessage().contains("cannot be null"));
    
    windowConfigDTO.setTimeOffset("1w");
    windowConfigDTO.setRowPercentage(null);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ROLLING_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("rowPercentage"));
    assertTrue(illegalArgumentException.getMessage().contains("cannot be null"));
    
    windowConfigDTO.setRowPercentage(0.1f);
    windowConfigDTO.setSpecificValue(1.2);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ROLLING_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("specificValue"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
    
    windowConfigDTO.setSpecificValue(null);
    windowConfigDTO.setTrainingDatasetVersion(12);
    
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("ROLLING_TIME"));
    assertTrue(illegalArgumentException.getMessage().contains("trainingDatasetVersion"));
    assertTrue(illegalArgumentException.getMessage().contains("must be null"));
  }
  
  @Test
  public void testValidateMonitoringWindowConfigWindowFieldLength() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ROLLING_TIME);
    windowConfigDTO.setWindowLength(
      StringUtils.repeat("d", MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_WINDOW_LENGTH + 1));
    
    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDtoFieldMaximalLength(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("Window length"));
    assertTrue(illegalArgumentException.getMessage()
      .contains(Integer.toString(MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_WINDOW_LENGTH)));
    assertTrue(illegalArgumentException.getMessage().contains(configName));
  }
  
  @Test
  public void testValidateMonitoringWindowConfigTimeOffsetLength() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ROLLING_TIME);
    windowConfigDTO.setTimeOffset(StringUtils.repeat("d", MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_TIME_OFFSET + 1));
    
    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDtoFieldMaximalLength(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("Time offset"));
    assertTrue(illegalArgumentException.getMessage()
      .contains(Integer.toString(MAX_CHARACTERS_IN_MONITORING_WINDOW_CONFIG_TIME_OFFSET)));
    assertTrue(illegalArgumentException.getMessage().contains(configName));
  }
  
  @Test
  public void testMonitoringWindowConfigurationAllTimeRowPercentage() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ALL_TIME);
    windowConfigDTO.setRowPercentage(1.0001f);
    
    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("Row percentage"));
    assertTrue(illegalArgumentException.getMessage().contains(configName));
    
    windowConfigDTO.setRowPercentage(-0.0001f);
    illegalArgumentException = assertThrows(IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("Row percentage"));
    assertTrue(illegalArgumentException.getMessage().contains(configName));
  }
  
  @Test
  public void testMonitoringWindowConfigurationRollingTimeRowPercentage() {
    MonitoringWindowConfigurationDTO windowConfigDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ROLLING_TIME);
    windowConfigDTO.setRowPercentage(1.0001f);
    
    IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("Row percentage"));
    assertTrue(illegalArgumentException.getMessage().contains(configName));
    
    windowConfigDTO.setRowPercentage(-0.0001f);
    illegalArgumentException = assertThrows(IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("Row percentage"));
    assertTrue(illegalArgumentException.getMessage().contains(configName));
  }
  
  @Test
  public void testWindowLengthOptionalForRollingTime() {
    MonitoringWindowConfigurationDTO windowConfigurationDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ROLLING_TIME);
    windowConfigurationDTO.setWindowLength(null);
    
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigurationDTO));
  }
  
  @Test
  public void testRegexForTimeOffsetAndWindowLength() {
    MonitoringWindowConfigurationDTO windowConfigurationDTO =
      makeValidMonitoringWindowConfiguration(WindowConfigurationType.ROLLING_TIME);
    // minus signs not allowed
    windowConfigurationDTO.setTimeOffset("-1w");
    
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigurationDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("Time offset"));
    assertTrue(illegalArgumentException.getMessage().contains(configName));
    assertTrue(illegalArgumentException.getMessage().contains("1w2d3h"));
    
    // additional erroneous characters not allowed
    windowConfigurationDTO.setTimeOffset("1w2d3h4m");
    illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigurationDTO));
    
    assertTrue(illegalArgumentException.getMessage().contains("Time offset"));
    assertTrue(illegalArgumentException.getMessage().contains(configName));
    assertTrue(illegalArgumentException.getMessage().contains("1w2d3h"));
    
    // two out of three allowed
    windowConfigurationDTO.setTimeOffset("12h1d");
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigurationDTO));
    
    // one out of three allowed
    windowConfigurationDTO.setTimeOffset("1w");
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigurationDTO));
    windowConfigurationDTO.setTimeOffset("2d");
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigurationDTO));
    windowConfigurationDTO.setTimeOffset("3h");
    assertTrue(inputValidation.validateMonitoringWindowConfigDto(configName, windowConfigurationDTO));
  }
}