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

import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.featurestore.app.FsJobManagerController;
import io.hops.hopsworks.common.featurestore.featuregroup.FeaturegroupController;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewController;
import io.hops.hopsworks.common.jobs.JobController;
import io.hops.hopsworks.common.jobs.scheduler.JobScheduleV2Controller;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.exceptions.JobException;
import io.hops.hopsworks.persistence.entity.featurestore.Featurestore;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.descriptivestatistics.DescriptiveStatisticsComparisonConfig;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.FeatureView;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.scheduler.JobScheduleV2;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.user.Users;
import io.hops.hopsworks.restutils.RESTCodes.FeaturestoreErrorCode;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureMonitoringConfigurationController {
  private static final Logger LOGGER = Logger.getLogger(FeatureMonitoringConfigurationController.class.getName());
  
  @EJB
  FeatureMonitoringConfigurationFacade featureMonitoringConfigurationFacade;
  @EJB
  FeaturegroupController featureGroupController;
  @EJB
  FeatureViewController featureViewController;
  @EJB
  FsJobManagerController fsJobManagerController;
  @EJB
  JobController jobController;
  @EJB
  JobScheduleV2Controller scheduleV2Controller;
  
  ////////////////////////////////////////
  ////  CRUD Operations
  ////////////////////////////////////////
  public List<FeatureMonitoringConfiguration> getFeatureMonitoringConfigurationByEntityAndFeatureName(
    Featurestore featureStore, ResourceRequest.Name entityType, Integer entityId, String featureName)
    throws FeaturestoreException {
    Featuregroup featureGroup = null;
    FeatureView featureView = null;
    if (entityType == ResourceRequest.Name.FEATUREGROUPS) {
      featureGroup = featureGroupController.getFeaturegroupById(featureStore, entityId);
    } else if (entityType == ResourceRequest.Name.FEATUREVIEW) {
      featureView = featureViewController.getByIdAndFeatureStore(entityId, featureStore);
    }
    
    List<FeatureMonitoringConfiguration> configs = new ArrayList<>();
    if (entityType == ResourceRequest.Name.FEATUREGROUPS) {
      configs = featureMonitoringConfigurationFacade.findByFeatureGroupAndFeatureName(featureGroup, featureName);
    } else if (entityType == ResourceRequest.Name.FEATUREVIEW) {
      configs = featureMonitoringConfigurationFacade.findByFeatureViewAndFeatureName(featureView, featureName);
    }
    
    return configs;
  }
  
  public List<FeatureMonitoringConfiguration> getFeatureMonitoringConfigurationByEntity(Featurestore featureStore,
    ResourceRequest.Name entityType, Integer entityId) throws FeaturestoreException {
    Featuregroup featureGroup = null;
    FeatureView featureView = null;
    if (entityType == ResourceRequest.Name.FEATUREGROUPS) {
      featureGroup = featureGroupController.getFeaturegroupById(featureStore, entityId);
    } else if (entityType == ResourceRequest.Name.FEATUREVIEW) {
      featureView = featureViewController.getByIdAndFeatureStore(entityId, featureStore);
    }
    
    List<FeatureMonitoringConfiguration> configs = new ArrayList<>();
    if (entityType == ResourceRequest.Name.FEATUREGROUPS) {
      configs = featureMonitoringConfigurationFacade.findByFeatureGroup(featureGroup);
    } else if (entityType == ResourceRequest.Name.FEATUREVIEW) {
      configs = featureMonitoringConfigurationFacade.findByFeatureView(featureView);
    }
    
    return configs;
  }
  
  public FeatureMonitoringConfiguration getFeatureMonitoringConfigurationByConfigId(Integer configId)
    throws FeaturestoreException {
    
    Optional<FeatureMonitoringConfiguration> optConfig = featureMonitoringConfigurationFacade.findById(configId);
    
    if (!optConfig.isPresent()) {
      throw new FeaturestoreException(FeaturestoreErrorCode.FEATURE_MONITORING_ENTITY_NOT_FOUND, Level.WARNING,
        String.format("Feature Monitoring Config with id %d not found.", configId));
    }
    
    return optConfig.get();
  }
  
  public FeatureMonitoringConfiguration getFeatureMonitoringConfigurationByEntityAndName(Featurestore featureStore,
    ResourceRequest.Name entityType, Integer entityId, String name) throws FeaturestoreException {
    Optional<FeatureMonitoringConfiguration> optConfig = Optional.empty();
    if (entityType == ResourceRequest.Name.FEATUREGROUPS) {
      Featuregroup featureGroup = featureGroupController.getFeaturegroupById(featureStore, entityId);
      optConfig = featureMonitoringConfigurationFacade.findByFeatureGroupAndName(featureGroup, name);
    } else if (entityType == ResourceRequest.Name.FEATUREVIEW) {
      FeatureView featureView = featureViewController.getByIdAndFeatureStore(entityId, featureStore);
      optConfig = featureMonitoringConfigurationFacade.findByFeatureViewAndName(featureView, name);
    }
    
    if (!optConfig.isPresent()) {
      throw new FeaturestoreException(FeaturestoreErrorCode.FEATURE_MONITORING_ENTITY_NOT_FOUND, Level.WARNING,
        String.format("Feature Monitoring Config with name %s not found.", name));
    }
    
    return optConfig.get();
  }
  
  public FeatureMonitoringConfiguration createFeatureMonitoringConfiguration(Featurestore featureStore, Users user,
    ResourceRequest.Name entityType, FeatureMonitoringConfiguration config) throws FeaturestoreException, JobException {
    Featuregroup featureGroup = config.getFeatureGroup();
    FeatureView featureView = config.getFeatureView();
    String entityName = null;
    Integer entityVersion = null;
    if (entityType == ResourceRequest.Name.FEATUREGROUPS) {
      entityName = featureGroup.getName();
      entityVersion = featureGroup.getVersion();
    } else if (entityType == ResourceRequest.Name.FEATUREVIEW) {
      entityName = featureView.getName();
      entityVersion = featureView.getVersion();
    }
    
    Jobs monitoringJob =
      fsJobManagerController.setupFeatureMonitoringJob(user, featureStore.getProject(), entityType, entityName,
        entityVersion, config.getName());
    config.setJob(monitoringJob);
    JobScheduleV2 schedule = config.getJobSchedule();
    schedule.setJob(monitoringJob);
    config.setJobSchedule(scheduleV2Controller.createSchedule(schedule));
    
    return featureMonitoringConfigurationFacade.update(config);
  }
  
  public void deleteFeatureMonitoringConfiguration(Integer configId) throws FeaturestoreException {
    FeatureMonitoringConfiguration config = getFeatureMonitoringConfigurationByConfigId(configId);
    featureMonitoringConfigurationFacade.remove(config);
  }
  
  public FeatureMonitoringConfiguration updateFeatureMonitoringConfiguration(Integer configId,
    FeatureMonitoringConfiguration config) throws FeaturestoreException, JobException {
    Optional<FeatureMonitoringConfiguration> optConfig = featureMonitoringConfigurationFacade.findById(configId);
    
    if (!optConfig.isPresent()) {
      throw new FeaturestoreException(FeaturestoreErrorCode.FEATURE_MONITORING_ENTITY_NOT_FOUND, Level.WARNING,
        String.format("Feature Monitoring Config with id %d not found.", configId));
    }
    
    FeatureMonitoringConfiguration newConfig = setAllowedFeatureMonitoringConfigurationUpdates(optConfig.get(), config);
    featureMonitoringConfigurationFacade.update(newConfig);
    
    return newConfig;
  }
  
  public FeatureMonitoringConfiguration setAllowedFeatureMonitoringConfigurationUpdates(
    FeatureMonitoringConfiguration config, FeatureMonitoringConfiguration newConfig) throws JobException {
    // Allowed updates to the feature monitoring config metadata
    // name is not editable as it would also require to change the job name
    config.setDescription(newConfig.getDescription());
    
    // Allowed updates to the scheduler config
    JobScheduleV2 schedule = newConfig.getJobSchedule();
    schedule.setId(config.getJobSchedule().getId());
    schedule.setEnabled(newConfig.getJobSchedule().getEnabled());
    config.setJobSchedule(scheduleV2Controller.updateSchedule(schedule));
    
    // No allowed updates to the detection window
    
    // Allowed updates to the comparison config
    DescriptiveStatisticsComparisonConfig statsConfig = config.getDsComparisonConfig();
    statsConfig.setThreshold(newConfig.getDsComparisonConfig().getThreshold());
    statsConfig.setStrict(newConfig.getDsComparisonConfig().getStrict());
    // changing the metric is not allowed to keep historic data consistent
    // changing the relative flag is not allowed to keep historic data consistent
    config.setDsComparisonConfig(statsConfig);
    
    return config;
  }
  
  ////////////////////////////////////////
  //// Feature Monitoring Job methods
  ////////////////////////////////////////
  public Jobs setupFeatureMonitoringJob(Project project, Users user, Featurestore featureStore,
    ResourceRequest.Name entityType, Integer entityId, String configName) throws FeaturestoreException, JobException {
    
    String entityName = "";
    Integer entityVersion = -1;
    if (entityType == ResourceRequest.Name.FEATUREGROUPS) {
      Featuregroup featureGroup = featureGroupController.getFeaturegroupById(featureStore, entityId);
      entityName = featureGroup.getName();
      entityVersion = featureGroup.getVersion();
    } else if (entityType == ResourceRequest.Name.FEATUREVIEW) {
      FeatureView featureView = featureViewController.getByIdAndFeatureStore(entityId, featureStore);
      entityName = featureView.getName();
      entityVersion = featureView.getVersion();
    }
    
    return fsJobManagerController.setupFeatureMonitoringJob(user, project, entityType, entityName, entityVersion,
      configName);
  }
}