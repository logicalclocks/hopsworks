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
package io.hops.hopsworks.common.alert;

import com.google.common.base.Strings;
import io.hops.hopsworks.alert.AMClient;
import io.hops.hopsworks.alert.AlertManagerConfiguration;
import io.hops.hopsworks.alert.dao.AlertReceiverFacade;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alert.util.ConfigUtil;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alert.util.PostableAlertBuilder;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigCtrlCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigReadException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerConfigUpdateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerDuplicateEntryException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerNoSuchElementException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.common.api.ResourceRequest;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertReceiver;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlert;
import io.hops.hopsworks.persistence.entity.featurestore.alert.FeatureStoreAlertStatus;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featureview.alert.FeatureViewAlert;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobFinalStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlert;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlertStatus;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlertStatus;
import io.hops.hopsworks.persistence.entity.project.service.ProjectServiceEnum;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AlertController {
  private static final Logger LOGGER = Logger.getLogger(AlertController.class.getName());
  @EJB
  private AMClient alertManager;
  @EJB
  private AlertManagerConfiguration alertManagerConfiguration;
  @EJB
  private AlertReceiverFacade alertReceiverFacade;

  /**
   * Send job alert
   * @param newState
   * @param execution
   */
  public void sendAlert(JobState newState, Execution execution) {
    Jobs job = execution != null ? execution.getJob() : null;
    if (job != null) {
      List<PostableAlert> postableAlerts = getAlerts(newState, execution);
      sendJobAlert(postableAlerts, job.getProject(), job.getName(), execution.getId());
    }
  }

  /**
   * Send job alert
   * @param newState
   * @param execution
   */
  public void sendAlert(JobFinalStatus newState, Execution execution) {
    Jobs job = execution != null ? execution.getJob() : null;
    if (job != null) {
      List<PostableAlert> postableAlerts = getAlerts(newState, execution);
      sendJobAlert(postableAlerts, job.getProject(), job.getName(), execution.getId());
    }
  }

  /**
   * Test feature group alert
   * @param project
   * @param alert
   */
  public List<Alert> testAlert(Project project, FeatureGroupAlert alert)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
      AlertManagerClientCreateException {
    return sendFgTestAlert(project, alert.getAlertType(), alert.getSeverity(),
      FeatureStoreAlertStatus.fromString(alert.getStatus().toString()), alert.getFeatureGroup().getName());
  }
  
  /**
   * Test feature view alert
   *
   * @param project
   * @param alert
   */
  public List<Alert> testAlert(Project project, FeatureViewAlert alert)
    throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
    AlertManagerClientCreateException {
    return sendFvTestAlert(project, alert);
  }

  /**
   * Test job alert
   * @param project
   * @param alert
   */
  public List<Alert> testAlert(Project project, JobAlert alert)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
      AlertManagerClientCreateException {
    return sendJobTestAlert(project, alert.getAlertType(), alert.getSeverity(), alert.getStatus().getName(),
        alert.getJobId().getName());
  }

  /**
   * Test project alert
   * @param project
   * @param alert
   */
  public List<Alert> testAlert(Project project, ProjectServiceAlert alert)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
      AlertManagerClientCreateException {
    List<Alert> alerts = null;
    if (ProjectServiceEnum.FEATURESTORE.equals(alert.getService())) {
      alerts = sendFgTestAlert(project, alert.getAlertType(), alert.getSeverity(),
        FeatureStoreAlertStatus.fromString(alert.getStatus().toString()), null);
    } else if (ProjectServiceEnum.JOBS.equals(alert.getService())) {
      alerts = sendJobTestAlert(project, alert.getAlertType(), alert.getSeverity(), alert.getStatus().getName(), null);
    }
    return alerts;
  }

  private void sendAlert(List<PostableAlert> postableAlerts, Project project)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
      AlertManagerClientCreateException {
    if (!postableAlerts.isEmpty()) {
      alertManager.postAlerts(postableAlerts, project);
    }
  }

  public void sendFgAlert(List<PostableAlert> postableAlerts, Project project, String name) {
    try {
      sendAlert(postableAlerts, project);
    } catch (Exception e) {
      LOGGER.log(java.util.logging.Level.WARNING, "Failed to send alert. Featuregroup={0}. Exception: {1}",
        new Object[] {name, e.getMessage()});
    }
  }
  
  public void sendFeatureMonitorAlert(List<PostableAlert> postableAlerts, Project project, String name) {
    try {
      if (!postableAlerts.isEmpty()) {
        sendAlert(postableAlerts, project);
      }
    } catch (Exception e) {
      LOGGER.log(java.util.logging.Level.WARNING, "Failed to send alert. Feature Monitoring Config={0}. Exception: {1}",
        new Object[]{name, e.getMessage()});
    }
  }

  private void sendJobAlert(List<PostableAlert> postableAlerts, Project project, String name, Integer id) {
    try {
      sendAlert(postableAlerts, project);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING,
          "Failed to send alert. Job={0} executionId={1}. Exception: {2}", new Object[] {name, id, e.getMessage()});
    }
  }

  private List<Alert> sendFgTestAlert(Project project, AlertType alertType, AlertSeverity severity,
      FeatureStoreAlertStatus status, String fgName) throws AlertManagerUnreachableException,
      AlertManagerAccessControlException, AlertManagerResponseException, AlertManagerClientCreateException {
    String testAlertFgName = Strings.isNullOrEmpty(fgName) ? Constants.TEST_ALERT_FG_NAME : fgName;
    List<PostableAlert> postableAlerts = new ArrayList<>();
    PostableAlert postableAlert = getPostableFgAlert(project.getName(), alertType, severity, status.getName(),
      Constants.TEST_ALERT_FG_SUMMARY, Constants.TEST_ALERT_FG_DESCRIPTION, Constants.TEST_ALERT_FG_ID,
      Constants.TEST_ALERT_FS_NAME, testAlertFgName, Constants.TEST_ALERT_FG_VERSION);
    postableAlerts.add(postableAlert);
    sendAlert(postableAlerts, project);
    String fgFilter = Constants.FILTER_BY_FG_FORMAT.replace(Constants.FG_PLACE_HOLDER, testAlertFgName) +
        Constants.FILTER_BY_FG_ID_FORMAT.replace(Constants.FG_ID_PLACE_HOLDER, Constants.TEST_ALERT_FG_ID.toString());
    return getAlerts(project, fgFilter);
  }
  
   /**
   * create a test postable alert for feature view and send alert. Supports only for feature monitoring status.
   * @param project
   * @param alert
   * @return
     * @throws AlertManagerUnreachableException
   * @throws AlertManagerAccessControlException
   * @throws AlertManagerResponseException
   * @throws AlertManagerClientCreateException
   * @throws AlertException
   */
  private List<Alert> sendFvTestAlert(Project project, FeatureViewAlert alert) throws AlertManagerUnreachableException,
    AlertManagerAccessControlException, AlertManagerResponseException, AlertManagerClientCreateException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    PostableAlert postableAlert =
      getPostableFeatureMonitorAlert(project, alert, ResourceRequest.Name.FEATUREVIEW, Constants.TEST_ALERT_FM_NAME,
        Constants.TEST_ALERT_FG_VERSION,
        Constants.TEST_ALERT_FG_SUMMARY, Constants.TEST_ALERT_FG_DESCRIPTION, Constants.TEST_ALERT_FS_NAME);
    postableAlerts.add(postableAlert);
    sendAlert(postableAlerts, project);
    String fgFilter =
      Constants.FILTER_BY_FM_NAME_FORMAT.replace(Constants.FM_NAME_PLACE_HOLDER, Constants.TEST_ALERT_FM_NAME) +
        Constants.FILTER_BY_FM_RESULT_FORMAT.replace(Constants.FM_ID_PLACE_HOLDER,
          Constants.TEST_ALERT_FG_VERSION.toString());
    return getAlerts(project, fgFilter);
  }
  
  
  private List<Alert> sendJobTestAlert(Project project, AlertType alertType, AlertSeverity severity, String status,
      String jobName) throws AlertManagerUnreachableException, AlertManagerAccessControlException,
      AlertManagerResponseException, AlertManagerClientCreateException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    String testAlertJobName = Strings.isNullOrEmpty(jobName) ? Constants.TEST_ALERT_JOB_NAME : jobName;
    PostableAlert postableAlert = getPostableAlert(project, alertType,
        severity, status, testAlertJobName, Constants.TEST_ALERT_EXECUTION_ID);
    postableAlerts.add(postableAlert);
    sendAlert(postableAlerts, project);
    String jobFilter =
        Constants.FILTER_BY_JOB_FORMAT.replace(Constants.JOB_PLACE_HOLDER, testAlertJobName) +
            Constants.FILTER_BY_EXECUTION_FORMAT
                .replace(Constants.EXECUTION_ID_PLACE_HOLDER, Constants.TEST_ALERT_EXECUTION_ID.toString());
    return getAlerts(project, jobFilter);
  }

  private List<Alert> getAlerts(Project project, String filter)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
      AlertManagerClientCreateException {
    Set<String> filters = new HashSet<>();
    String projectFilter = Constants.FILTER_BY_PROJECT_FORMAT.replace(Constants.PROJECT_PLACE_HOLDER,
        project.getName());
    filters.add(projectFilter);
    filters.add(filter);
    return alertManager.getAlerts(true, null, null, null, filters, null, project);
  }

  public PostableAlert getPostableFgAlert(String projectName, AlertType alertType, AlertSeverity severity,
    String status, String summary, String description, Integer id, String featureStoreName, String featureGroupName,
    int version) {
    return new PostableAlertBuilder
      .Builder(projectName, alertType, severity, status)
      .withFeatureGroupId(id)
      .withFeatureStoreName(featureStoreName)
      .withFeatureGroupName(featureGroupName)
      .withFeatureGroupVersion(version)
      .withSummary(summary)
      .withDescription(description)
      .build();
  }
  /**
   * create a PostableAlert for FeatureMonitoring status for given ProjecServiceAlert
   * @param project
   * @param projectAlert
   * @param fmConfigName
   * @param fmResultId
   * @param summary
   * @param description
   * @param featureStoreName
   * @return PostableAlert
   */
  public PostableAlert getPostableFeatureMonitorAlert(Project project,
    ProjectServiceAlert projectAlert, String fmConfigName, Integer fmResultId, String summary,
    String description, String featureStoreName) {
    
    PostableAlertBuilder.Builder builder =
      new PostableAlertBuilder.Builder(project.getName(), projectAlert.getAlertType(),
        projectAlert.getSeverity()
        , projectAlert.getStatus().getName())
        .withFeatureStoreName(featureStoreName)
        .withFeatureMonitorConfig(fmConfigName, fmResultId)
        .withSummary(summary)
        .withDescription(description);
    return builder.build();
  }
  
  /**
   * Create PostableAlert for FeatureMonitoring status from FeatureStoreAlert.
   * @param project
   * @param featureStoreAlert
   * @param resourceName
   * @param fmConfigName
   * @param fmResultId
   * @param summary
   * @param description
   * @param featureStoreName
   * @return
   */
  public PostableAlert getPostableFeatureMonitorAlert(Project project, FeatureStoreAlert featureStoreAlert,
    ResourceRequest.Name resourceName, String fmConfigName,
    Integer fmResultId,
    String summary,
    String description,
    String featureStoreName) {
    
    PostableAlertBuilder.Builder builder =
      new PostableAlertBuilder.Builder(project.getName(), featureStoreAlert.getAlertType(),
        featureStoreAlert.getSeverity()
        , featureStoreAlert.getStatus().getName())
        .withFeatureStoreName(featureStoreName)
        .withFeatureMonitorConfig(fmConfigName, fmResultId)
        .withSummary(summary)
        .withDescription(description);
    if (resourceName.equals(ResourceRequest.Name.FEATUREGROUPS)) {
      builder.withFeatureGroupName(((FeatureGroupAlert) featureStoreAlert).getFeatureGroup().getName())
        .withFeatureGroupVersion(((FeatureGroupAlert) featureStoreAlert).getFeatureGroup().getVersion());
    } else if (resourceName.equals(ResourceRequest.Name.FEATUREVIEW)) {
      builder.withFeatureViewVersion(((FeatureViewAlert) featureStoreAlert).getFeatureView().getVersion())
        .withFeatureViewName(((FeatureViewAlert) featureStoreAlert).getFeatureView().getName());
    }
    return builder.build();
  }
  
  private PostableAlert getPostableAlert(Project project, AlertType alertType,  AlertSeverity severity, String status,
      String jobName, Integer id) {
    return new PostableAlertBuilder
        .Builder(project.getName(), alertType, severity, status)
        .withJobName(jobName)
        .withExecutionId(id)
        .withSummary("Job " + status)
        .withDescription("Job=" + jobName + " with executionId=" + id + " " + status.toLowerCase())
        .build();
  }

  private List<PostableAlert> getAlerts(JobState jobState, Execution execution) {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    if (jobState.isFinalState()) {
      postableAlerts = getAlerts(JobAlertStatus.getJobAlertStatus(jobState),
        ProjectServiceAlertStatus.getJobAlertStatus(jobState), execution);
    }
    return postableAlerts;
  }

  private List<PostableAlert> getAlerts(JobFinalStatus jobState, Execution execution) {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    if (!JobFinalStatus.UNDEFINED.equals(jobState)) {
      postableAlerts = getAlerts(JobAlertStatus.getJobAlertStatus(jobState),
        ProjectServiceAlertStatus.getJobAlertStatus(jobState), execution);
    }
    return postableAlerts;
  }

  //method expects execution not null.
  private List<PostableAlert> getAlerts(JobAlertStatus jobAlertStatus,
    ProjectServiceAlertStatus projectServiceAlertStatus, Execution execution) {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    Jobs job = execution.getJob();
    if (job != null && job.getJobAlertCollection() != null && !job.getJobAlertCollection().isEmpty()) {
      for (JobAlert alert : job.getJobAlertCollection()) {
        if (alert.getStatus().equals(jobAlertStatus)) {
          PostableAlert postableAlert = getPostableAlert(job.getProject(), alert.getAlertType(),
            alert.getSeverity(), alert.getStatus().getName(), job.getName(), execution.getId());
          postableAlerts.add(postableAlert);
        }
      }
    } else if (job != null && job.getProject().getProjectServiceAlerts() != null &&
      !job.getProject().getProjectServiceAlerts().isEmpty()) {
      for (ProjectServiceAlert alert : job.getProject().getProjectServiceAlerts()) {
        if (ProjectServiceEnum.JOBS.equals(alert.getService()) && alert.getStatus().equals(projectServiceAlertStatus)) {
          PostableAlert postableAlert = getPostableAlert(job.getProject(), alert.getAlertType(),
            alert.getSeverity(), alert.getStatus().getName(), job.getName(), execution.getId());
          postableAlerts.add(postableAlert);
        }
      }
    }
    return postableAlerts;
  }

  public void cleanProjectAlerts(Project project) throws AlertManagerConfigCtrlCreateException,
      AlertManagerConfigReadException, AlertManagerUnreachableException, AlertManagerResponseException,
      AlertManagerClientCreateException, AlertManagerConfigUpdateException {
    //TODO: clean silences
    alertManagerConfiguration.cleanProject(project);
  }

  private void addRouteIfNotExist(AlertType type, Route route, Project project)
      throws AlertManagerUnreachableException, AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerClientCreateException,
      AlertManagerAccessControlException {
    try {
      if (type.isGlobal()) {
        alertManagerConfiguration.addRoute(route);
      } else {
        alertManagerConfiguration.addRoute(route, project);
      }
    } catch (AlertManagerDuplicateEntryException e) {
      // route exists
    }
  }

  public void createRoute(ProjectServiceAlert alert) throws AlertManagerUnreachableException,
      AlertManagerAccessControlException, AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Project project = alert.getProject();
    Route route = ConfigUtil.getRoute(alert);
    addRouteIfNotExist(alert.getAlertType(), route, project);
  }
  
  public void createRoute(Project project, FeatureGroupAlert alert) throws AlertManagerUnreachableException,
      AlertManagerAccessControlException, AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Route route = ConfigUtil.getRoute(alert);
    addRouteIfNotExist(alert.getAlertType(), route, project);
  }

  public void createRoute(JobAlert alert) throws AlertManagerUnreachableException, AlertManagerAccessControlException,
      AlertManagerNoSuchElementException, AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException,
      AlertManagerConfigReadException, AlertManagerClientCreateException {
    Project project = alert.getJobId().getProject();
    Route route = ConfigUtil.getRoute(alert);
    addRouteIfNotExist(alert.getAlertType(), route, project);
  }

  public void createRoute(AlertType alertType)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerNoSuchElementException,
      AlertManagerConfigUpdateException, AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException,
      AlertManagerClientCreateException {
    Route route = ConfigUtil.getRoute(alertType);
    try {
      alertManagerConfiguration.addRoute(route);
    } catch (AlertManagerDuplicateEntryException e) {
      // route exists
    }
  }
  
  public void createRoute(Project project, FeatureViewAlert alert) throws AlertManagerUnreachableException,
    AlertManagerAccessControlException, AlertManagerNoSuchElementException, AlertManagerConfigUpdateException,
    AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Route route = ConfigUtil.getRoute(alert);
    addRouteIfNotExist(alert.getAlertType(), route, project);
  }
  
  public void deleteRoute(ProjectServiceAlert alert)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Project project = alert.getProject();
    Route route = ConfigUtil.getRoute(alert);
    if (!isUsedByOtherAlerts(route, alert.getId())) {
      alertManagerConfiguration.removeRoute(route, project);
    }
  }
  
  public void deleteRoute(Project project, FeatureGroupAlert alert)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Route route = ConfigUtil.getRoute(alert);
    if (!isUsedByOtherAlerts(route, alert.getId())) {
      alertManagerConfiguration.removeRoute(route, project);
    }
  }

  public void deleteRoute(JobAlert alert)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerConfigUpdateException,
      AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Project project = alert.getJobId().getProject();
    Route route = ConfigUtil.getRoute(alert);
    if (!isUsedByOtherAlerts(route, alert.getId())) {
      alertManagerConfiguration.removeRoute(route, project);
    }
  }
  
  public void deleteRoute(Project project, FeatureViewAlert alert)
    throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerConfigUpdateException,
    AlertManagerConfigCtrlCreateException, AlertManagerConfigReadException, AlertManagerClientCreateException {
    Route route = ConfigUtil.getRoute(alert);
    if (!isUsedByOtherAlerts(route, alert.getId())) {
      alertManagerConfiguration.removeRoute(route, project);
    }
  }
  
  private boolean isUsedByOtherAlerts(Route route, int id) {
    Optional<AlertReceiver> alertReceiver = alertReceiverFacade.findByName(route.getReceiver());
    if (!alertReceiver.isPresent()) {
      return false;
    }
    if (route.getMatch().get(Constants.LABEL_JOB) != null) {
      Collection<JobAlert> jobAlerts = alertReceiver.get().getJobAlertCollection();
      for (JobAlert alert : jobAlerts) {
        Route jobAlertRoute = ConfigUtil.getRoute(alert);
        if (alert.getId() != id && jobAlertRoute.equals(route)) {
          return true;
        }
      }
    } else if (route.getMatch().get(Constants.LABEL_FEATURE_GROUP) != null) {
      Collection<FeatureGroupAlert> featureGroupAlerts = alertReceiver.get().getFeatureGroupAlertCollection();
      for (FeatureGroupAlert alert : featureGroupAlerts) {
        Route fgAlertRoute = ConfigUtil.getRoute(alert);
        if (alert.getId() != id && fgAlertRoute.equals(route)) {
          return true;
        }
      }
    } else {
      Collection<ProjectServiceAlert> projectServiceAlerts = alertReceiver.get().getProjectServiceAlertCollection();
      for (ProjectServiceAlert alert : projectServiceAlerts) {
        Route projectAlertRoute = ConfigUtil.getRoute(alert);
        if (alert.getId() != id && projectAlertRoute.equals(route)) {
          return true;
        }
      }
    }
    return false;
  }

  public AlertType getAlertType(AlertReceiver receiver) {
    AlertType alertType = AlertType.fromReceiverName(receiver.getName());
    if (alertType == null) {
      return AlertType.PROJECT_ALERT;
    }
    return alertType;
  }

}
