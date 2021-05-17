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

import io.hops.hopsworks.alert.AlertManager;
import io.hops.hopsworks.alert.exception.AlertManagerAccessControlException;
import io.hops.hopsworks.alert.exception.AlertManagerUnreachableException;
import io.hops.hopsworks.alert.util.Constants;
import io.hops.hopsworks.alert.util.PostableAlertBuilder;
import io.hops.hopsworks.alerting.api.alert.dto.Alert;
import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.alerting.exceptions.AlertManagerClientCreateException;
import io.hops.hopsworks.alerting.exceptions.AlertManagerResponseException;
import io.hops.hopsworks.common.featurestore.FeaturestoreFacade;
import io.hops.hopsworks.common.featurestore.featuregroup.ExpectationResult;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.Featuregroup;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.FeatureGroupValidation;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.ValidationRuleAlertStatus;
import io.hops.hopsworks.persistence.entity.jobs.configuration.history.JobState;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlert;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlertStatus;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class AlertController {
  private static final Logger LOGGER = Logger.getLogger(AlertController.class.getName());
  @EJB
  private AlertManager alertManager;
  @EJB
  private FeaturestoreFacade featurestoreFacade;
  
  /**
   * send feature group alert
   * @param featuregroup
   * @param results
   * @param status
   */
  public void sendAlert(Featuregroup featuregroup, List<ExpectationResult> results,
      FeatureGroupValidation.Status status) {
    List<PostableAlert> postableAlerts = getPostableAlerts(featuregroup, results, status);
    sendFgAlert(postableAlerts, featuregroup.getFeaturestore().getProject(), featuregroup.getName());
  }
  
  /**
   * Send job alert
   * @param newState
   * @param execution
   */
  public void sendAlert(JobState newState, Execution execution) {
    List<PostableAlert> postableAlerts = getAlerts(newState, execution);
    sendJobAlert(postableAlerts, execution.getJob().getProject(), execution.getJob().getName(), execution.getId());
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
        FeatureGroupValidation.Status.fromString(alert.getStatus().toString()));
  }
  
  /**
   * Test job alert
   * @param project
   * @param alert
   */
  public List<Alert> testAlert(Project project, JobAlert alert)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
      AlertManagerClientCreateException {
    return sendJobTestAlert(project, alert.getAlertType(), alert.getSeverity(), alert.getStatus().getName());
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
          FeatureGroupValidation.Status.fromString(alert.getStatus().toString()));
    } else if (ProjectServiceEnum.JOBS.equals(alert.getService())) {
      alerts = sendJobTestAlert(project, alert.getAlertType(), alert.getSeverity(), alert.getStatus().getName());
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
  
  private void sendFgAlert(List<PostableAlert> postableAlerts, Project project, String name) {
    try {
      sendAlert(postableAlerts, project);
    } catch (Exception e) {
      LOGGER.log(java.util.logging.Level.WARNING, "Failed to send alert. Featuregroup={0}. Exception: {1}",
          new Object[] {name, e.getMessage()});
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
      FeatureGroupValidation.Status status)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
      AlertManagerClientCreateException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    PostableAlert postableAlert = getPostableAlert(project.getName(), alertType, severity, status.getName(),
        getExpectationResult(status, new ArrayList<>()), Constants.TEST_ALERT_FG_ID, Constants.TEST_ALERT_FS_NAME,
        Constants.TEST_ALERT_FG_NAME, Constants.TEST_ALERT_FG_VERSION);
    postableAlerts.add(postableAlert);
    sendAlert(postableAlerts, project);
    String fgFilter = Constants.FILTER_BY_FG_FORMAT.replace(Constants.FG_PLACE_HOLDER, Constants.TEST_ALERT_FG_NAME) +
        Constants.FILTER_BY_FG_ID_FORMAT.replace(Constants.FG_ID_PLACE_HOLDER, Constants.TEST_ALERT_FG_ID.toString());
    return getAlerts(project, fgFilter);
  }
  
  private List<Alert> sendJobTestAlert(Project project, AlertType alertType, AlertSeverity severity, String status)
      throws AlertManagerUnreachableException, AlertManagerAccessControlException, AlertManagerResponseException,
      AlertManagerClientCreateException {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    PostableAlert postableAlert = getPostableAlert(project, alertType,
        severity, status, Constants.TEST_ALERT_JOB_NAME, Constants.TEST_ALERT_EXECUTION_ID);
    postableAlerts.add(postableAlert);
    sendAlert(postableAlerts, project);
    String jobFilter =
        Constants.FILTER_BY_JOB_FORMAT.replace(Constants.JOB_PLACE_HOLDER, Constants.TEST_ALERT_JOB_NAME) +
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
    List<Alert> alerts = alertManager.getAlerts(true, null, null, null, filters, null, project);
    return alerts;
  }
  
  private List<PostableAlert> getPostableAlerts(Featuregroup featuregroup, List<ExpectationResult> results,
      FeatureGroupValidation.Status status) {
    List<PostableAlert> postableAlerts = new ArrayList<>();
    if (FeatureGroupValidation.Status.NONE.equals(status)) {
      return postableAlerts;
    }
    if (featuregroup.getFeatureGroupAlerts() != null && !featuregroup.getFeatureGroupAlerts().isEmpty()) {
      for (FeatureGroupAlert alert : featuregroup.getFeatureGroupAlerts()) {
        if (alert.getStatus().equals(ValidationRuleAlertStatus.getStatus(status))) {
          String name = featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId());
          PostableAlert postableAlert =
              getPostableAlert(featuregroup.getFeaturestore().getProject().getName(), alert.getAlertType(),
                  alert.getSeverity(), alert.getStatus().getName(), getExpectationResult(status, results),
                  featuregroup.getId(), name, featuregroup.getName(), featuregroup.getVersion());
          postableAlerts.add(postableAlert);
        }
      }
    } else if (featuregroup.getFeaturestore().getProject().getProjectServiceAlerts() != null &&
        !featuregroup.getFeaturestore().getProject().getProjectServiceAlerts().isEmpty()) {
      for (ProjectServiceAlert alert : featuregroup.getFeaturestore().getProject().getProjectServiceAlerts()) {
        if (ProjectServiceEnum.FEATURESTORE.equals(alert.getService()) &&
            alert.getStatus().equals(ProjectServiceAlertStatus.getStatus(status))) {
          String name = featurestoreFacade.getHiveDbName(featuregroup.getFeaturestore().getHiveDbId());
          PostableAlert postableAlert =
              getPostableAlert(featuregroup.getFeaturestore().getProject().getName(), alert.getAlertType(),
                  alert.getSeverity(), alert.getStatus().getName(), getExpectationResult(status, results),
                  featuregroup.getId(), name, featuregroup.getName(), featuregroup.getVersion());
          postableAlerts.add(postableAlert);
        }
      }
    }
    return postableAlerts;
  }
  
  private PostableAlert getPostableAlert(String projectName, AlertType alertType, AlertSeverity severity,
      String status, String results, Integer id, String featureStoreName, String featureGroupName, int version) {
    return new PostableAlertBuilder
        .Builder(projectName, alertType, severity, status)
        .withFeatureGroupId(id)
        .withFeatureStoreName(featureStoreName)
        .withFeatureGroupName(featureGroupName)
        .withFeatureGroupVersion(version)
        .withSummary("Feature group validation " + status.toLowerCase())
        .withDescription("Feature group name=" + featureGroupName + results)
        .build();
  }
  
  private String getExpectationResult(FeatureGroupValidation.Status status, List<ExpectationResult> results) {
    if (FeatureGroupValidation.Status.SUCCESS.equals(status)) {
      return " validation succeeded with warning=0, error=0.";
    }
    StringBuilder resultStr = new StringBuilder();
    resultStr.append(" validation ended with ")
        .append(status.getName().toLowerCase())
        .append(".");
    List<String> detailedResults = new ArrayList<>();
    for (ExpectationResult result : results) {
      if (FeatureGroupValidation.Status.WARNING.equals(result.getStatus()) ||
          FeatureGroupValidation.Status.FAILURE.equals(result.getStatus())) {
        detailedResults
            .add("[expectation=" + result.getExpectation().getName() + ", status=" + result.getStatus().getName() +
                "]");
      }
    }
    if (!detailedResults.isEmpty()) {
      resultStr.append(" Detail: ")
          .append(String.join(", ", detailedResults))
          .append(".");
    }
    return resultStr.toString();
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
      if (execution.getJob().getJobAlertCollection() != null && !execution.getJob().getJobAlertCollection().isEmpty()) {
        for (JobAlert alert : execution.getJob().getJobAlertCollection()) {
          if (alert.getStatus().equals(JobAlertStatus.getJobAlertStatus(jobState))) {
            PostableAlert postableAlert = getPostableAlert(execution.getJob().getProject(), alert.getAlertType(),
                alert.getSeverity(), alert.getStatus().getName(), execution.getJob().getName(), execution.getId());
            postableAlerts.add(postableAlert);
          }
        }
      } else if (execution.getJob().getProject().getProjectServiceAlerts() != null &&
          !execution.getJob().getProject().getProjectServiceAlerts().isEmpty()) {
        for (ProjectServiceAlert alert : execution.getJob().getProject().getProjectServiceAlerts()) {
          if (ProjectServiceEnum.JOBS.equals(alert.getService()) &&
              alert.getStatus().equals(ProjectServiceAlertStatus.getJobAlertStatus(jobState))) {
            PostableAlert postableAlert = getPostableAlert(execution.getJob().getProject(), alert.getAlertType(),
                alert.getSeverity(), alert.getStatus().getName(), execution.getJob().getName(), execution.getId());
            postableAlerts.add(postableAlert);
          }
        }
      }
    }
    return postableAlerts;
  }
}
