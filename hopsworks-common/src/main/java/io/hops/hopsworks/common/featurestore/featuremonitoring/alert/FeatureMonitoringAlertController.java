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
package io.hops.hopsworks.common.featurestore.featuremonitoring.alert;

import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.common.alert.AlertController;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.common.dao.project.alert.ProjectServiceAlertsFacade;
import io.hops.hopsworks.common.featurestore.FeaturestoreController;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.datavalidation.FeatureGroupAlertFacade;
import io.hops.hopsworks.common.featurestore.featureview.FeatureViewAlertFacade;
import io.hops.hopsworks.exceptions.FeaturestoreException;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlert;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.config.FeatureMonitoringConfiguration;
import io.hops.hopsworks.persistence.entity.featurestore.featuremonitoring.result.FeatureMonitoringResult;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlertStatus;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class FeatureMonitoringAlertController {
  @EJB
  private FeaturestoreController featurestoreController;
  @EJB
  private AlertController alertController;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  @EJB
  private FeatureGroupAlertFacade featureGroupAlertFacade;
  @EJB
  private ProjectServiceAlertsFacade projectServiceAlertsFacade;
  @EJB
  private FeatureViewAlertFacade featureViewAlertFacade;
  
  public FeatureMonitoringAlertController() {
  }
  
  /**
   * This will retrieve alerts configs based on the feature group/feature view defined on feature monitoring.
   * Posts the alert if it matches the shift detected filter.
   *
   * @param config
   * @param result
   */
  public void triggerAlertsByStatus(FeatureMonitoringConfiguration config, FeatureMonitoringResult result)
    throws FeaturestoreException {
    validateInput(config, result);
    List<PostableAlert> postableAlerts;
    Project project;
    String fsName;
    if (config.getFeatureGroup() != null) {
      project = config.getFeatureGroup().getFeaturestore().getProject();
      fsName = featurestoreController.getOfflineFeaturestoreDbName(config.getFeatureGroup().getFeaturestore());
      postableAlerts = getValidPostableAlerts(project, fsName, config, result,
        ResourceRequest.Name.FEATUREGROUPS);
    } else {
      project = config.getFeatureView().getFeaturestore().getProject();
      fsName = featurestoreController.getOfflineFeaturestoreDbName(config.getFeatureView().getFeaturestore());
      postableAlerts = getValidPostableAlerts(project, fsName, config, result, ResourceRequest.Name.FEATUREVIEW);
    }
    alertController.sendFeatureMonitorAlert(postableAlerts, project, config.getName());
    
  }
  
  /**
   * find alerts based on status with feature group or feature view and contruct PostableAlert
   *
   * @param configuration
   * @param result
   * @param entityType
   * @return List of PostableAlerts to trigger
   */
  private List<PostableAlert> getValidPostableAlerts(Project project,
    String fsName, FeatureMonitoringConfiguration configuration,
    FeatureMonitoringResult result, ResourceRequest.Name entityType) {
    //TODO: add filter on alert and FM config severity
    List<PostableAlert> postableAlerts = new ArrayList<>();
    ProjectServiceAlert projectAlert;
    // get mapping status;if shift detected is FALSE status should be FEATURE_MONITOR_SHIFT_UNDETECTED
    FeatureStoreAlertStatus alertStatus =
      FeatureStoreAlertStatus.fromBooleanFeatureMonitorResultStatus(result.getShiftDetected());
    ProjectServiceAlertStatus projectAlertStatus =
      ProjectServiceAlertStatus.fromBooleanFeatureMonitorResultStatus(result.getShiftDetected());
    // check project alerts
    projectAlert = projectServiceAlertsFacade.findByProjectAndStatus(project, projectAlertStatus);
    if (projectAlert != null) {
      postableAlerts.add(geProjectPostableAlert(project, fsName, projectAlert,  configuration, result));
    }
    // check local alerts 
    FeatureStoreAlert featureStoreAlert = retrieveAlert(configuration, entityType, alertStatus);
    if (featureStoreAlert !=null) {
      postableAlerts.add(getFeatureMonitorAlert(project, fsName, featureStoreAlert, entityType, configuration,
        result ));
    }
    return postableAlerts;
  }
  
  private FeatureStoreAlert retrieveAlert(FeatureMonitoringConfiguration configuration,
    ResourceRequest.Name entityType, FeatureStoreAlertStatus alertStatus) {
    if (entityType.equals(ResourceRequest.Name.FEATUREGROUPS)) {
      return featureGroupAlertFacade.findByFeatureGroupAndStatus(configuration.getFeatureGroup(), alertStatus);
    } else {
      return featureViewAlertFacade.findByFeatureViewAndStatus(configuration.getFeatureView(), alertStatus);
    }
  }
  
  private PostableAlert geProjectPostableAlert(Project project, String fsName, ProjectServiceAlert projectAlert,
    FeatureMonitoringConfiguration configuration,
    FeatureMonitoringResult result) {
    
    return alertController.getPostableFeatureMonitorAlert(project, projectAlert,
      configuration.getName(),
      result.getId(), constructAlertSummary(configuration, result), constructAlertDescription(configuration),
      fsName);
  }
  
  private PostableAlert getFeatureMonitorAlert(Project project, String fsName, FeatureStoreAlert alert,
    ResourceRequest.Name entityType,
    FeatureMonitoringConfiguration configuration,
    FeatureMonitoringResult result) {
    
    return alertController.getPostableFeatureMonitorAlert(project, alert, entityType,
      configuration.getName(),
      result.getId(), constructAlertSummary(configuration, result), constructAlertDescription(configuration),
      fsName);
  }
  
  private String constructAlertSummary(FeatureMonitoringConfiguration configuration, FeatureMonitoringResult result) {
    return String.format("Feature name: %s; Shift detected: %s; Difference: %s",
      configuration.getFeatureName(), result.getShiftDetected(), result.getDifference());
  }
  
  private String constructAlertDescription(FeatureMonitoringConfiguration configuration) {
    return String.format("FeatureMonitoring Config Name: %s; Monitoring Type: %s",
      configuration.getName(), configuration.getFeatureMonitoringType());
  }
  
  public void validateInput(FeatureMonitoringConfiguration config, FeatureMonitoringResult result)
    throws FeaturestoreException {
    if (config == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.SEVERE,
        "Feature Monitoring Config should not be null.");
    }
    if (result == null) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.SEVERE,
        "Feature Monitoring Result should not be null.");
    }
    if (Boolean.FALSE.equals(config.getJobSchedule().getEnabled())) {
      throw new FeaturestoreException(RESTCodes.FeaturestoreErrorCode.ALERT_ILLEGAL_ARGUMENT, Level.FINE,
        "Feature Monitoring Config is disabled, skipping triggering alert.");
    }
  }
}