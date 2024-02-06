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

import com.google.common.base.Strings;
import io.hops.hopsworks.alerting.api.alert.dto.PostableAlert;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertSeverity;
import io.hops.hopsworks.persistence.entity.alertmanager.AlertType;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PostableAlertBuilder {
  
  public static class Builder {
    private String projectName;
    private String jobName;
    private Integer executionId;
    private Integer featureGroupId;
    private String featureStoreName;
    private String featureGroupName;
    private Integer featureGroupVersion;
    private AlertType type;
    private AlertSeverity severity;
    private String status;
    private String summary;
    private String description;
    private URL generatorURL;
    private String featureMonitorConfigName;
    private Integer featureMonitorResultId;
    private String featureViewName;
    private Integer featureViewVersion;
    
    public Builder(String projectName, AlertType type, AlertSeverity severity, String status) {
      this.projectName = projectName;
      this.type = type;
      this.severity = severity;
      this.status = status;
    }
    
    public Builder withJobName(String jobName) {
      if (featureGroupName != null || featureGroupId != null) {
        throw new IllegalArgumentException("Alert can be either job or feature group validation.");
      }
      this.jobName = jobName;
      return this;
    }
    
    public Builder withExecutionId(Integer executionId) {
      if (featureGroupName != null || featureGroupId != null) {
        throw new IllegalArgumentException("Alert can be either job or feature group validation.");
      }
      this.executionId = executionId;
      return this;
    }
  
    public Builder withFeatureGroupName(String featureGroupName) {
      if (jobName != null || executionId != null) {
        throw new IllegalArgumentException("Alert can be either job or feature group validation.");
      }
      this.featureGroupName = featureGroupName;
      return this;
    }
  
    public Builder withFeatureGroupId(Integer featureGroupId) {
      if (jobName != null || executionId != null) {
        throw new IllegalArgumentException("Alert can be either job or feature group validation.");
      }
      this.featureGroupId = featureGroupId;
      return this;
    }
  
    public Builder withFeatureStoreName(String featureStoreName) {
      if (jobName != null || executionId != null) {
        throw new IllegalArgumentException("Alert can be either job or feature group validation.");
      }
      this.featureStoreName = featureStoreName;
      return this;
    }
  
    public Builder withFeatureGroupVersion(Integer featureGroupVersion) {
      if (jobName != null || executionId != null) {
        throw new IllegalArgumentException("Alert can be either job or feature group validation.");
      }
      this.featureGroupVersion = featureGroupVersion;
      return this;
    }
    
    public Builder withFeatureViewVersion(Integer version) {
      if (jobName != null || executionId != null || featureGroupId != null) {
        throw new IllegalArgumentException("Alert can be either job, feature group or feature view.");
      }
      this.featureViewVersion = version;
      return this;
    }
    
    public Builder withFeatureViewName(String name) {
      if (jobName != null || executionId != null || featureGroupId != null) {
        throw new IllegalArgumentException("Alert can be either job, feature group or feature view.");
      }
      this.featureViewName = name;
      return this;
    }
    
    public Builder withFeatureMonitorConfig(String configName, Integer resultId) {
      if (jobName != null || executionId != null) {
        throw new IllegalArgumentException("Alert can be either job or feature group validation.");
      }
      this.featureMonitorConfigName = configName;
      this.featureMonitorResultId = resultId;
      return this;
    }
    
    public Builder withSummary(String summary) {
      this.summary = summary;
      return this;
    }
  
    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }
    
    public Builder withGeneratorURL(String generatorURL) {
      try {
        this.generatorURL = new URL(generatorURL);
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException("GeneratorURL should be a valid url. " + e.getMessage());
      }
      return this;
    }
    
    public PostableAlert build() {
      Map<String, String> labels = new HashMap<>();
      Map<String, String> annotations = new HashMap<>();
      labels.put(Constants.ALERT_TYPE_LABEL, this.type.getValue());
      labels.put(Constants.LABEL_PROJECT, this.projectName);
      labels.put(Constants.LABEL_SEVERITY, this.severity.getName());
      labels.put(Constants.LABEL_STATUS, this.status);
      if (!Strings.isNullOrEmpty(this.jobName)) {
        labels.put(Constants.ALERT_NAME_LABEL, Constants.ALERT_NAME_JOB);
        labels.put(Constants.LABEL_JOB, this.jobName);
        annotations.put(Constants.LABEL_TITLE, this.jobName);
      }
      if (this.executionId != null) {
        labels.put(Constants.LABEL_EXECUTION_ID, this.executionId.toString());
      }
      if (!Strings.isNullOrEmpty(this.featureGroupName)) {
        labels.put(Constants.ALERT_NAME_LABEL, Constants.ALERT_NAME_FEATURE_VALIDATION);
        labels.put(Constants.LABEL_FEATURE_GROUP, this.featureGroupName);
        annotations.put(Constants.LABEL_TITLE, this.featureGroupName);
      }
      if (!Strings.isNullOrEmpty(this.featureStoreName)) {
        labels.put(Constants.LABEL_FEATURE_STORE, this.featureStoreName);
      }
      if (this.featureGroupId != null) {
        labels.put(Constants.LABEL_FEATURE_GROUP_ID, this.featureGroupId.toString());
      }
      if (this.featureGroupVersion != null) {
        labels.put(Constants.LABEL_FEATURE_GROUP_VERSION, this.featureGroupVersion.toString());
      }
      if (Strings.isNullOrEmpty(this.summary) || Strings.isNullOrEmpty(this.description)) {
        throw new IllegalArgumentException("Summary and description can not be empty.");
      }
      // if alert if feature monitoring config change title
      if (!Strings.isNullOrEmpty(this.featureMonitorConfigName)) {
        labels.put(Constants.ALERT_NAME_LABEL, Constants.ALERT_NAME_FEATURE_MONITOR);
        labels.put(Constants.LABEL_FM_CONFIG, this.featureMonitorConfigName);
        annotations.put(Constants.LABEL_TITLE, this.featureMonitorConfigName);
      }
      if (this.featureMonitorResultId != null) {
        labels.put(Constants.LABEL_FM_RESULT_ID, this.featureMonitorResultId.toString());
      }
      if (this.featureViewName != null) {
        labels.put(Constants.LABEL_FEATURE_VIEW_NAME, this.featureViewName);
      }
      if (this.featureViewVersion != null) {
        labels.put(Constants.LABEL_FEATURE_VIEW_VERSION, this.featureViewVersion.toString());
      }
      annotations.put(Constants.LABEL_SUMMARY, this.summary);
      annotations.put(Constants.LABEL_DESCRIPTION, this.description);
      PostableAlert postableAlert = new PostableAlert(labels, annotations);
      if (this.generatorURL != null) {
        postableAlert.setGeneratorURL(this.generatorURL);
      }
      return postableAlert;
    }
  }
  
}
