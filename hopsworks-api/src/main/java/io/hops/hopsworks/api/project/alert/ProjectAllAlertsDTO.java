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
package io.hops.hopsworks.api.project.alert;

import io.hops.hopsworks.api.featurestore.datavalidation.alert.FeatureGroupAlertDTO;
import io.hops.hopsworks.api.featurestore.featureview.FeatureViewAlertDTO;
import io.hops.hopsworks.api.jobs.alert.JobAlertsDTO;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ProjectAllAlertsDTO {
  private ProjectAlertsDTO projectAlerts;
  private JobAlertsDTO jobAlerts;
  private FeatureGroupAlertDTO featureGroupAlerts;
  
  private FeatureViewAlertDTO featureViewAlertDTO;
  
  public ProjectAllAlertsDTO() {
  }

  public ProjectAlertsDTO getProjectAlerts() {
    return projectAlerts;
  }

  public void setProjectAlerts(ProjectAlertsDTO projectAlerts) {
    this.projectAlerts = projectAlerts;
  }

  public JobAlertsDTO getJobAlerts() {
    return jobAlerts;
  }

  public void setJobAlerts(JobAlertsDTO jobAlerts) {
    this.jobAlerts = jobAlerts;
  }

  public FeatureGroupAlertDTO getFeatureGroupAlerts() {
    return featureGroupAlerts;
  }

  public void setFeatureGroupAlerts(FeatureGroupAlertDTO featureGroupAlerts) {
    this.featureGroupAlerts = featureGroupAlerts;
  }
  
  public FeatureViewAlertDTO getFeatureViewAlertDTO() {
    return featureViewAlertDTO;
  }
  
  public void setFeatureViewAlertDTO(FeatureViewAlertDTO featureViewAlertDTO) {
    this.featureViewAlertDTO = featureViewAlertDTO;
  }
}
