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

import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.result.FeatureMonitoringResultDTO;
import io.hops.hopsworks.common.featurestore.featuremonitoring.result.FeatureMonitoringResultInputValidation;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringType;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.MonitoringWindowConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.WindowConfigurationType;

import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestFeatureMonitoringResultInputValidation {
  @InjectMocks
  private FeatureMonitoringResultInputValidation featureMonitoringResultInputValidation = new FeatureMonitoringResultInputValidation();
  
  @Mock
  private FeatureMonitoringConfigurationController featureMonitoringConfigurationController;
  
  private Integer featureStoreId = 33;
  private Integer configIdStatsOnly = 22;
  private Integer configIdComparison = 23;
  private Integer configIdSpecificValue = 24;
  private Integer executionId = 16;
  private String featureName = "test_feature";
  private Integer detectionStatsId = 25;
  private Integer referenceStatsId = 66;
  
  @Before
  public void setup() throws FeaturestoreException {
    MockitoAnnotations.openMocks(this);
    
    FeatureMonitoringConfiguration config = new FeatureMonitoringConfiguration();
    config.setName("test_fm_config");
    config.setFeatureMonitoringType(FeatureMonitoringType.STATISTICS_COMPUTATION);
    
    Mockito.when(
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByConfigId(configIdStatsOnly)
    ).thenReturn(
      config
    );
    
    FeatureMonitoringConfiguration configWithComparison = new FeatureMonitoringConfiguration();
    MonitoringWindowConfiguration windowConfig = new MonitoringWindowConfiguration();
    configWithComparison.setName("test_fm_config");
    configWithComparison.setFeatureMonitoringType(FeatureMonitoringType.STATISTICS_COMPARISON);
    windowConfig.setWindowConfigType(WindowConfigurationType.ALL_TIME);
    configWithComparison.setReferenceWindowConfig(windowConfig);
    
    Mockito.when(
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByConfigId(configIdComparison)
    ).thenReturn(
      configWithComparison
    );
    
    FeatureMonitoringConfiguration configWithSpecificValue = new FeatureMonitoringConfiguration();
    MonitoringWindowConfiguration windowConfigSpecificValue = new MonitoringWindowConfiguration();
    configWithSpecificValue.setName("test_fm_config");
    configWithSpecificValue.setFeatureMonitoringType(FeatureMonitoringType.STATISTICS_COMPARISON);
    windowConfigSpecificValue.setWindowConfigType(WindowConfigurationType.SPECIFIC_VALUE);
    windowConfigSpecificValue.setSpecificValue(0.5);
    configWithSpecificValue.setReferenceWindowConfig(windowConfigSpecificValue);
    
    Mockito.when(
      featureMonitoringConfigurationController.getFeatureMonitoringConfigurationByConfigId(configIdSpecificValue)
    ).thenReturn(
      configWithSpecificValue
    );
  }
  
  private FeatureMonitoringResultDTO makeValidResultDTO(Integer referenceStatsId, Double specificValue) {
    FeatureMonitoringResultDTO resultDto = new FeatureMonitoringResultDTO();
    
    resultDto.setId(null);
    resultDto.setConfigId(configIdStatsOnly);
    resultDto.setFeatureStoreId(featureStoreId);
    resultDto.setExecutionId(executionId);
    resultDto.setFeatureName(featureName);
    resultDto.setDetectionStatisticsId(detectionStatsId);
    resultDto.setShiftDetected(false);
    resultDto.setRaisedException(false);
    resultDto.setEmptyDetectionWindow(false);
    resultDto.setEmptyReferenceWindow(false);
    resultDto.setMonitoringTime(new Date().getTime());
    
    if (referenceStatsId != null) {
      resultDto.setReferenceStatisticsId(referenceStatsId);
      resultDto.setDifference(0.);
    }
    if (specificValue != null) {
      resultDto.setSpecificValue(specificValue);
      resultDto.setEmptyReferenceWindow(true);
      resultDto.setDifference(0.);
    }
    
    return resultDto;
  }
  
  @Test
  public void testValidateOnCreateForRaisedException() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = new FeatureMonitoringResultDTO();
    
    resultDto.setConfigId(configIdStatsOnly);
    resultDto.setFeatureName("");
    resultDto.setRaisedException(true);
    
    assertTrue(featureMonitoringResultInputValidation.validateOnCreate(resultDto));
  }
  
  @Test
  public void testValidateOnCreateForMissingConfigId() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = new FeatureMonitoringResultDTO();
    
    resultDto.setConfigId(null);
    resultDto.setFeatureName("");
    resultDto.setRaisedException(false);
    
    assertThrows(IllegalArgumentException.class, () -> {
      featureMonitoringResultInputValidation.validateOnCreate(resultDto);
    });
  }
  
  @Test
  public void testValidateOnCreateDetectionStatsOnlyResult() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null,  null);
    resultDto.setConfigId(configIdStatsOnly);
    
    assertTrue(featureMonitoringResultInputValidation.validateOnCreate(resultDto));
  }
  
  @Test
  public void testValidateOnCreateWithReference() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(referenceStatsId, null);
    resultDto.setConfigId(configIdComparison);
    
    assertTrue(featureMonitoringResultInputValidation.validateOnCreate(resultDto));
  }
  
  @Test
  public void testValidateOnCreateWithSpecificValue() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, 0.);
    resultDto.setConfigId(configIdSpecificValue);
    
    assertTrue(featureMonitoringResultInputValidation.validateOnCreate(resultDto));
  }
  
  @Test
  public void testValidateOnCreateWithComparisonAndSpecificValue() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, 0.);
    resultDto.setConfigId(configIdComparison);
    
    assertThrows(IllegalArgumentException.class, () -> {
      featureMonitoringResultInputValidation.validateOnCreate(resultDto);
    });
  }
  
  @Test
  public void testValidateOnCreateWithEmptyReference() {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, null);
    resultDto.setConfigId(configIdComparison);
    resultDto.setEmptyReferenceWindow(true);
    resultDto.setDifference(0.);
    
    assertThrows(IllegalArgumentException.class, () -> {
      featureMonitoringResultInputValidation.validateOnCreate(resultDto);
    });
  }
  
  @Test
  public void testValidateOnCreateWithEmptyDetection() {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, null);
    resultDto.setConfigId(configIdComparison);
    resultDto.setDetectionStatisticsId(null);
    resultDto.setEmptyDetectionWindow(true);
    
    assertThrows(IllegalArgumentException.class, () -> {
      featureMonitoringResultInputValidation.validateOnCreate(resultDto);
    });
  }
  
  @Test
  public void testValidateOnCreateWithEmptyDetectionAndEmptyReference() {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, null);
    resultDto.setConfigId(configIdComparison);
    resultDto.setDetectionStatisticsId(null);
    resultDto.setReferenceStatisticsId(null);
    resultDto.setEmptyDetectionWindow(true);
    resultDto.setEmptyReferenceWindow(true);
    
    assertThrows(IllegalArgumentException.class, () -> {
      featureMonitoringResultInputValidation.validateOnCreate(resultDto);
    });
  }
  
  @Test
  public void testValidateOnCreateWithEmptyDetectionAndStatsId() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, null);
    resultDto.setConfigId(configIdStatsOnly);
    resultDto.setDetectionStatisticsId(detectionStatsId);
    resultDto.setEmptyDetectionWindow(true);
    
    assertTrue(featureMonitoringResultInputValidation.validateOnCreate(resultDto));
  }
  
  @Test
  public void testValidateOnCreateWithEmptyReferenceAndStatsId() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, null);
    resultDto.setConfigId(configIdComparison);
    resultDto.setReferenceStatisticsId(referenceStatsId);
    resultDto.setEmptyReferenceWindow(true);
    resultDto.setDifference(null);
    
    assertTrue(featureMonitoringResultInputValidation.validateOnCreate(resultDto));
  }
  
  @Test
  public void testValidateOnCreateBothEmptyWithStatsId() throws FeaturestoreException {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, null);
    resultDto.setConfigId(configIdComparison);
    resultDto.setReferenceStatisticsId(referenceStatsId);
    resultDto.setDetectionStatisticsId(detectionStatsId);
    resultDto.setEmptyReferenceWindow(true);
    resultDto.setEmptyDetectionWindow(true);
    resultDto.setDifference(null);
    
    assertTrue(featureMonitoringResultInputValidation.validateOnCreate(resultDto));
  }
  
  @Test
  public void testValidateOnCreateEmptyWithNotNullDifference() {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, null);
    resultDto.setConfigId(configIdComparison);
    resultDto.setReferenceStatisticsId(referenceStatsId);
    resultDto.setEmptyReferenceWindow(true);
    resultDto.setDifference(0.);
    
    assertThrows(IllegalArgumentException.class, () -> {
      featureMonitoringResultInputValidation.validateOnCreate(resultDto);
    });
  }
  
  @Test
  public void testValidateOnCreateStatsOnlyWithNotNullDifference() {
    FeatureMonitoringResultDTO resultDto = makeValidResultDTO(null, null);
    resultDto.setConfigId(configIdStatsOnly);
    resultDto.setDetectionStatisticsId(detectionStatsId);
    resultDto.setDifference(0.);
    
    assertThrows(IllegalArgumentException.class, () -> {
      featureMonitoringResultInputValidation.validateOnCreate(resultDto);
    });
  }
}