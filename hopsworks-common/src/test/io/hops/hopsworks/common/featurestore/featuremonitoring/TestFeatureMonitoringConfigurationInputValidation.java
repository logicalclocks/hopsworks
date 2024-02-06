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

import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.DescriptiveStatisticsComparisonConfigurationDTO;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationDTO;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationFacade;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationInputValidation;
import io.hops.hopsworks.common.featurestore.featuremonitoring.monitoringwindowconfiguration.MonitoringWindowConfigurationInputValidation;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2DTO;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringType;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.WindowConfigurationType;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.descriptivestatistics.MetricDescriptiveStatistics;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Optional;

import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_DESCRIPTION;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_FEATURE_NAME;
import static io.hops.hopsworks.common.featurestore.FeaturestoreConstants.MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_NAME;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TestFeatureMonitoringConfigurationInputValidation {
  @InjectMocks
  private final TestMonitoringWindowConfigurationInputValidation testMonitoringWindowConfigurationInputValidation =
    new TestMonitoringWindowConfigurationInputValidation();
  @InjectMocks
  private FeatureMonitoringConfigurationInputValidation featureMonitoringConfigurationInputValidation =
    new FeatureMonitoringConfigurationInputValidation();
  
  @Mock
  private FeaturegroupController featuregroupController;
  @Mock
  private FeatureMonitoringConfigurationFacade featureMonitoringConfigurationFacade;
  //
  @Mock
  private MonitoringWindowConfigurationInputValidation monitoringWindowConfigurationInputValidation;
  
  private final Project project = new Project();
  private final Users user = new Users(123);
  private final Featurestore featurestore = new Featurestore();
  private final Featuregroup featureGroup = new Featuregroup();
  
  @Before
  public void setup() throws FeaturestoreException {
    MockitoAnnotations.openMocks(this);
    
    featurestore.setProject(project);
    featureGroup.setFeaturestore(featurestore);
    featureGroup.setId(12);
    featureGroup.setName("fgtest");
    
    Mockito.when(
      featuregroupController.getFeatureNames(Mockito.any(), Mockito.any(), Mockito.any())
    ).thenReturn(
      Arrays.asList("testFeatureName")
    );
    
    Mockito.when(
      featuregroupController.getFeaturegroupById(Mockito.any(), Mockito.any())
    ).thenReturn(
      featureGroup
    );
  }
  
  private FeatureMonitoringConfigurationDTO makeValidFeatureMonitoringConfigDTO(boolean withFeatureName,
    WindowConfigurationType referenceWindowType) {
    FeatureMonitoringConfigurationDTO dto = new FeatureMonitoringConfigurationDTO();
    dto.setName("testName");
    dto.setDescription("testDescription");
    
    if (withFeatureName) {
      dto.setFeatureName("testFeatureName");
    }
    
    JobScheduleV2DTO jobSchedule = new JobScheduleV2DTO();
    jobSchedule.setEnabled(true);
    dto.setJobSchedule(jobSchedule);
    
    dto.setDetectionWindowConfig(
      testMonitoringWindowConfigurationInputValidation.makeValidMonitoringWindowConfiguration(
        WindowConfigurationType.ROLLING_TIME
      )
    );
    
    if (referenceWindowType != null) {
      dto.setFeatureMonitoringType(FeatureMonitoringType.STATISTICS_COMPARISON);
      dto.setReferenceWindowConfig(
        testMonitoringWindowConfigurationInputValidation.makeValidMonitoringWindowConfiguration(
          referenceWindowType
        )
      );
    } else {
      dto.setFeatureMonitoringType(FeatureMonitoringType.STATISTICS_COMPUTATION);
    }
    
    if (dto.getFeatureMonitoringType() == FeatureMonitoringType.STATISTICS_COMPARISON) {
      dto.setStatisticsComparisonConfig(makeValidStatisticsComparisonConfigDTO());
    }
    
    return dto;
  }
  
  private DescriptiveStatisticsComparisonConfigurationDTO makeValidStatisticsComparisonConfigDTO() {
    DescriptiveStatisticsComparisonConfigurationDTO statisticsComparisonConfigDTO =
      new DescriptiveStatisticsComparisonConfigurationDTO();
    statisticsComparisonConfigDTO.setMetric(MetricDescriptiveStatistics.MEAN);
    statisticsComparisonConfigDTO.setRelative(false);
    statisticsComparisonConfigDTO.setStrict(false);
    statisticsComparisonConfigDTO.setThreshold(0.1);
    
    return statisticsComparisonConfigDTO;
  }
  
  @Test
  public void testValidConfigIsValidated() throws FeaturestoreException {
    FeatureMonitoringConfigurationDTO dto = makeValidFeatureMonitoringConfigDTO(false, null);
    assertTrue(
      featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null)
    );
    
    dto = makeValidFeatureMonitoringConfigDTO(true, null);
    assertTrue(
      featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null)
    );
    
    dto = makeValidFeatureMonitoringConfigDTO(true, WindowConfigurationType.SPECIFIC_VALUE);
    assertTrue(
      featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null)
    );
    
    dto = makeValidFeatureMonitoringConfigDTO(true, WindowConfigurationType.TRAINING_DATASET);
    assertTrue(
      featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null)
    );
    
    dto = makeValidFeatureMonitoringConfigDTO(true, WindowConfigurationType.ROLLING_TIME);
    assertTrue(
      featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null)
    );
    
    dto = makeValidFeatureMonitoringConfigDTO(true, WindowConfigurationType.ALL_TIME);
    assertTrue(
      featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null)
    );
  }
  
  @Test
  public void testValidateConfigFeatureNameFieldLength() throws FeaturestoreException {
    FeatureMonitoringConfigurationDTO dto = makeValidFeatureMonitoringConfigDTO(true, null);
    String tooLongFeatureName = StringUtils.repeat("a", MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_FEATURE_NAME + 1);
    dto.setFeatureName(tooLongFeatureName);
    Mockito.when(
      featuregroupController.getFeatureNames(Mockito.any(), Mockito.any(), Mockito.any())
    ).thenReturn(
      Arrays.asList(tooLongFeatureName)
    );
    
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null));
    
    assertTrue(illegalArgumentException.getMessage().contains("Feature name"));
    assertTrue(illegalArgumentException.getMessage()
      .contains(Integer.toString(MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_FEATURE_NAME)));
    assertTrue(illegalArgumentException.getMessage().contains(dto.getName()));
  }
  
  @Test
  public void testValidateConfigNameFieldLength() {
    FeatureMonitoringConfigurationDTO dto = makeValidFeatureMonitoringConfigDTO(true, null);
    dto.setName(StringUtils.repeat("a", MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_NAME + 1));
    
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null));
    
    assertTrue(illegalArgumentException.getMessage().contains("Name"));
    assertTrue(illegalArgumentException.getMessage()
      .contains(Integer.toString(MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_NAME)));
    assertTrue(illegalArgumentException.getMessage().contains(dto.getName()));
  }
  
  @Test
  public void testValidateConfigFieldLength() {
    FeatureMonitoringConfigurationDTO dto = makeValidFeatureMonitoringConfigDTO(true, null);
    dto.setName(StringUtils.repeat("a", MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_NAME + 1));
    
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null));
    
    assertTrue(illegalArgumentException.getMessage().contains("Name"));
    assertTrue(illegalArgumentException.getMessage()
      .contains(Integer.toString(MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_NAME)));
    assertTrue(illegalArgumentException.getMessage().contains(dto.getName()));
  }
  
  @Test
  public void testValidateConfigDescriptionFieldLength() {
    FeatureMonitoringConfigurationDTO dto = makeValidFeatureMonitoringConfigDTO(true, null);
    dto.setDescription(StringUtils.repeat("a", MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_DESCRIPTION + 1));
    
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null));
    
    assertTrue(illegalArgumentException.getMessage().contains("Description"));
    assertTrue(illegalArgumentException.getMessage()
      .contains(Integer.toString(MAX_CHARACTERS_IN_FEATURE_MONITORING_CONFIG_DESCRIPTION)));
    assertTrue(illegalArgumentException.getMessage().contains(dto.getName()));
  }
  
  @Test
  public void testValidateFeatureNameExist() {
    FeatureMonitoringConfigurationDTO dto = makeValidFeatureMonitoringConfigDTO(true, WindowConfigurationType.ALL_TIME);
    dto.setFeatureName("testInvalidFeatureName");
    
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null)
    );
    
    assertTrue(illegalArgumentException.getMessage().contains(dto.getFeatureName()));
  }
  
  @Test
  public void testFailIfConfigNameAlreadyExistOnCreate() {
    Mockito.when(featureMonitoringConfigurationFacade.findByFeatureGroupAndName(Mockito.any(), Mockito.any()))
      .thenReturn(Optional.of(new FeatureMonitoringConfiguration()));
    
    FeatureMonitoringConfigurationDTO dto = makeValidFeatureMonitoringConfigDTO(false, null);
    
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> featureMonitoringConfigurationInputValidation.validateConfigOnCreate(user, dto, featureGroup, null)
    );
    
    assertTrue(illegalArgumentException.getMessage().contains(dto.getName()));
    assertTrue(illegalArgumentException.getMessage().contains("already exists"));
  }
  
  @Test
  public void testFailIfConfigNameNotExistOnUpdate() {
    Mockito.when(featureMonitoringConfigurationFacade.findByFeatureGroupAndName(Mockito.any(), Mockito.any()))
      .thenReturn(Optional.empty());
    
    FeatureMonitoringConfigurationDTO dto = makeValidFeatureMonitoringConfigDTO(false, null);
    
    IllegalArgumentException illegalArgumentException = assertThrows(
      IllegalArgumentException.class,
      () -> featureMonitoringConfigurationInputValidation.validateConfigOnUpdate(dto, featureGroup, null)
    );
    
    assertTrue(illegalArgumentException.getMessage().contains(dto.getName()));
    assertTrue(illegalArgumentException.getMessage().contains("does not exist"));
  }
}