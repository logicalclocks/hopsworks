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
package io.hops.hopsworks.api.featurestore.featuremonitoring.config;

import io.hops.hopsworks.api.jobs.scheduler.JobScheduleV2Builder;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.DescriptiveStatisticsComparisonConfigurationDTO;
import io.hops.hopsworks.common.featurestore.featuremonitoring.config.FeatureMonitoringConfigurationDTO;
import io.hops.hopsworks.common.featurestore.featuremonitoring.monitoringwindowconfiguration.MonitoringWindowConfigurationDTO;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.MonitoringWindowConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.descriptivestatistics.DescriptiveStatisticsComparisonConfig;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.project.Project;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;


@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureMonitoringConfigurationBuilder {
  @EJB
  private JobController jobController;
  @EJB
  private JobScheduleV2Builder jobScheduleV2Builder;
  @EJB
  private FeaturegroupController featureGroupController;
  @EJB
  private FeatureViewController featureViewController;
  @EJB
  private FsJobManagerController fsJobManagerController;
  
  
  public URI uri(UriInfo uriInfo, Project project, Featurestore featureStore, ResourceRequest.Name entityType,
    Integer entityId) {
    return uriInfo.getBaseUriBuilder().path(ResourceRequest.Name.PROJECT.toString().toLowerCase())
      .path(Integer.toString(project.getId())).path(ResourceRequest.Name.FEATURESTORES.toString().toLowerCase())
      .path(Integer.toString(featureStore.getId())).path(entityType.toString().toLowerCase())
      .path(Integer.toString(entityId)).path(ResourceRequest.Name.FEATURE_MONITORING.toString().toLowerCase())
      .path("config").build();
  }
  
  public FeatureMonitoringConfigurationDTO buildMany(UriInfo uri, Featurestore featureStore,
    ResourceRequest.Name entityType, Integer entityId, String entityName, Integer entityVersion,
    List<FeatureMonitoringConfiguration> configs) {
    FeatureMonitoringConfigurationDTO configsDto = new FeatureMonitoringConfigurationDTO();
    configsDto.setHref(uri(uri, featureStore.getProject(), featureStore, entityType, entityId));
    
    configsDto.setItems(
      configs.stream().map(config -> build(
        uri, featureStore, entityType, entityId, entityName, entityVersion, config)
      ).collect(Collectors.toList()));
    configsDto.setCount(((long) configsDto.getItems().size()));
    
    return configsDto;
  }
  
  public FeatureMonitoringConfigurationDTO build(UriInfo uri, Featurestore featureStore,
    ResourceRequest.Name entityType, Integer entityId, String entityName, Integer entityVersion,
    FeatureMonitoringConfiguration config) {
    FeatureMonitoringConfigurationDTO configDto = new FeatureMonitoringConfigurationDTO();
    
    configDto.setHref(uri(uri, featureStore.getProject(), featureStore, entityType, entityId));
    
    configDto.setId(config.getId());
    configDto.setFeatureStoreId(featureStore.getId());
    configDto.setFeatureName(config.getFeatureName());
    configDto.setJobName(config.getJob().getName());
    configDto.setName(config.getName());
    configDto.setDescription(config.getDescription());
    configDto.setFeatureMonitoringType(config.getFeatureMonitoringType());
    
    if (entityType == ResourceRequest.Name.FEATUREVIEW) {
      configDto.setFeatureViewName(entityName);
      configDto.setFeatureViewVersion(entityVersion);
    } else if (entityType == ResourceRequest.Name.FEATUREGROUPS) {
      configDto.setFeatureGroupId(entityId);
    }
    
    configDto.setStatisticsComparisonConfig(
      buildDescriptiveStatisticsComparisonConfigurationDTO(config.getDsComparisonConfig()));
    configDto.setJobSchedule(jobScheduleV2Builder.build(uri, config.getJobSchedule()));
    configDto.setDetectionWindowConfig(buildMonitoringWindowConfigurationDTO(config.getDetectionWindowConfig()));
    configDto.setReferenceWindowConfig(buildMonitoringWindowConfigurationDTO(config.getReferenceWindowConfig()));
    
    return configDto;
  }
  
  public MonitoringWindowConfigurationDTO buildMonitoringWindowConfigurationDTO(
    MonitoringWindowConfiguration monitoringWindowConfiguration) {
    if (monitoringWindowConfiguration == null) {
      return null;
    }
    MonitoringWindowConfigurationDTO dto = new MonitoringWindowConfigurationDTO();
    
    dto.setId(monitoringWindowConfiguration.getId());
    dto.setRowPercentage(monitoringWindowConfiguration.getRowPercentage());
    dto.setTrainingDatasetVersion(monitoringWindowConfiguration.getTrainingDatasetVersion());
    dto.setTimeOffset(monitoringWindowConfiguration.getTimeOffset());
    dto.setWindowLength(monitoringWindowConfiguration.getWindowLength());
    dto.setWindowConfigType(monitoringWindowConfiguration.getWindowConfigType());
    dto.setSpecificValue(monitoringWindowConfiguration.getSpecificValue());
    
    return dto;
  }
  
  public DescriptiveStatisticsComparisonConfigurationDTO buildDescriptiveStatisticsComparisonConfigurationDTO(
    DescriptiveStatisticsComparisonConfig statsConfig) {
    if (statsConfig == null) {
      return null;
    }
    
    DescriptiveStatisticsComparisonConfigurationDTO statsDto = new DescriptiveStatisticsComparisonConfigurationDTO();
    statsDto.setId(statsConfig.getId());
    statsDto.setMetric(statsConfig.getMetric());
    statsDto.setRelative(statsConfig.getRelative());
    statsDto.setStrict(statsConfig.getStrict());
    statsDto.setThreshold(statsConfig.getThreshold());
    
    return statsDto;
  }
  
  public FeatureMonitoringConfiguration buildFromDTO(Project project, Featuregroup featureGroup,
    FeatureView featureView, FeatureMonitoringConfigurationDTO dto) throws JobException {
    
    FeatureMonitoringConfiguration config = new FeatureMonitoringConfiguration();
    if (featureGroup != null) {
      config.setFeatureGroup(featureGroup);
    } else if (featureView != null) {
      config.setFeatureView(featureView);
    }
    
    config.setFeatureName(dto.getFeatureName());
    config.setFeatureMonitoringType(dto.getFeatureMonitoringType());
    config.setName(dto.getName());
    config.setDescription(dto.getDescription());
    
    config.setDetectionWindowConfig(buildWindowConfigurationFromDTO(dto.getDetectionWindowConfig()));
    config.setReferenceWindowConfig(buildWindowConfigurationFromDTO(dto.getReferenceWindowConfig()));
    
    // Descriptive Statistics specific
    if (dto.getStatisticsComparisonConfig() != null) {
      config.setDsComparisonConfig(
        buildDescriptiveStatisticsComparisonConfigurationFromDTO(dto.getStatisticsComparisonConfig())
      );
    }
    
    // Integration with other Hopsworks services
    if (dto.getId() != null) {
      config.setJob(jobController.getJob(project, dto.getJobName()));
      config.setJobSchedule(jobScheduleV2Builder.validateAndConvertOnUpdate(config.getJob(), dto.getJobSchedule()));
    } else {
      config.setJobSchedule(
        jobScheduleV2Builder.validateAndConvertOnCreate(null, dto.getJobSchedule())
      );
    }
    return config;
  }
  
  public MonitoringWindowConfiguration buildWindowConfigurationFromDTO(MonitoringWindowConfigurationDTO windowDto) {
    if (windowDto == null) {
      return null;
    }
    
    MonitoringWindowConfiguration window = new MonitoringWindowConfiguration();
    
    window.setId(windowDto.getId());
    window.setWindowLength(windowDto.getWindowLength());
    window.setTimeOffset(windowDto.getTimeOffset());
    window.setWindowConfigType(windowDto.getWindowConfigType());
    window.setTrainingDatasetVersion(windowDto.getTrainingDatasetVersion());
    window.setRowPercentage(windowDto.getRowPercentage());
    window.setSpecificValue(windowDto.getSpecificValue());
    
    return window;
  }
  
  public DescriptiveStatisticsComparisonConfig buildDescriptiveStatisticsComparisonConfigurationFromDTO(
    DescriptiveStatisticsComparisonConfigurationDTO statsDto) {
    DescriptiveStatisticsComparisonConfig statsConfig = new DescriptiveStatisticsComparisonConfig();
    
    statsConfig.setId(statsDto.getId());
    statsConfig.setThreshold(statsDto.getThreshold());
    statsConfig.setMetric(statsDto.getMetric());
    statsConfig.setRelative(statsDto.getRelative());
    statsConfig.setStrict(statsDto.getStrict());
    
    return statsConfig;
  }
}