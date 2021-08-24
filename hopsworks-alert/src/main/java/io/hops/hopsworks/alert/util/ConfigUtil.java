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
package io.hops.hopsworks.alert.util;

import io.hops.hopsworks.alerting.config.dto.Route;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;
import io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation.alert.FeatureGroupAlert;
import io.hops.hopsworks.persistence.entity.jobs.description.JobAlert;
import io.hops.hopsworks.persistence.entity.project.Project;
import io.hops.hopsworks.persistence.entity.project.alert.ProjectServiceAlert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigUtil {
  public static Map<String, String> getMatch(ProjectServiceAlert alert) {
    Project project = alert.getProject();
    Map<String, String> match = new HashMap<>();
    match.put(Constants.ALERT_TYPE_LABEL, alert.getAlertType().getValue());
    match.put(Constants.LABEL_PROJECT, project.getName());
    match.put(Constants.LABEL_STATUS, alert.getStatus().getName());
    return match;
  }
  
  public static Map<String, String> getMatch(FeatureGroupAlert alert) {
    Project project = alert.getFeatureGroup().getFeaturestore().getProject();
    Map<String, String> match = new HashMap<>();
    match.put(Constants.ALERT_TYPE_LABEL, alert.getAlertType().getValue());
    match.put(Constants.LABEL_PROJECT, project.getName());
    match.put(Constants.LABEL_FEATURE_GROUP, alert.getFeatureGroup().getName());
    match.put(Constants.LABEL_STATUS, alert.getStatus().getName());
    return match;
  }
  
  public static Map<String, String> getMatch(JobAlert alert) {
    Project project = alert.getJobId().getProject();
    Map<String, String> match = new HashMap<>();
    match.put(Constants.ALERT_TYPE_LABEL, alert.getAlertType().getValue());
    match.put(Constants.LABEL_PROJECT, project.getName());
    match.put(Constants.LABEL_JOB, alert.getJobId().getName());
    match.put(Constants.LABEL_STATUS, alert.getStatus().getName());
    return match;
  }
  
  public static Route getRoute(ProjectServiceAlert alert) {
    if (alert.getAlertType().isGlobal()) {
      return getRoute(alert.getAlertType());
    }
    Map<String, String> match = getMatch(alert);
    List<String> groupBy = new ArrayList<>();
    groupBy.add(Constants.LABEL_PROJECT);
    groupBy.add(Constants.LABEL_JOB);
    groupBy.add(Constants.LABEL_FEATURE_GROUP);
    groupBy.add(Constants.LABEL_STATUS);
    return new Route(alert.getReceiver().getName())
        .withContinue(true)
        .withMatch(match)
        .withGroupBy(groupBy);
  }
  
  public static Route getRoute(FeatureGroupAlert alert) {
    if (alert.getAlertType().isGlobal()) {
      return getRoute(alert.getAlertType());
    }
    Map<String, String> match = getMatch(alert);
    List<String> groupBy = new ArrayList<>();
    groupBy.add(Constants.LABEL_PROJECT);
    groupBy.add(Constants.LABEL_FEATURE_GROUP);
    groupBy.add(Constants.LABEL_STATUS);
    return new Route(alert.getReceiver().getName())
        .withContinue(true)
        .withMatch(match)
        .withGroupBy(groupBy);
  }
  
  public static Route getRoute(JobAlert alert) {
    if (alert.getAlertType().isGlobal()) {
      return getRoute(alert.getAlertType());
    }
    Map<String, String> match = getMatch(alert);
    List<String> groupBy = new ArrayList<>();
    groupBy.add(Constants.LABEL_PROJECT);
    groupBy.add(Constants.LABEL_JOB);
    groupBy.add(Constants.LABEL_STATUS);
    return new Route(alert.getReceiver().getName())
        .withContinue(true)
        .withMatch(match)
        .withGroupBy(groupBy);
  }
  
  public static Route getRoute(AlertType alertType) {
    Map<String, String> match = new HashMap<>();
    match.put(Constants.ALERT_TYPE_LABEL, alertType.getValue());
    List<String> groupBy = new ArrayList<>();
    groupBy.add(Constants.LABEL_PROJECT);
    groupBy.add(Constants.LABEL_JOB);
    groupBy.add(Constants.LABEL_FEATURE_GROUP);
    groupBy.add(Constants.LABEL_STATUS);
    return new Route(alertType.getReceiverName())
        .withContinue(true)
        .withMatch(match)
        .withGroupBy(groupBy);
  }
}
