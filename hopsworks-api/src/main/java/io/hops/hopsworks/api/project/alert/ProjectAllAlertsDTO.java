package io.hops.hopsworks.api.project.alert;

import io.hops.hopsworks.api.featurestore.datavalidation.alert.FeatureGroupAlertDTO;
import io.hops.hopsworks.api.jobs.alert.JobAlertsDTO;

public class ProjectAllAlertsDTO {
  private ProjectAlertsDTO projectAlerts;
  private JobAlertsDTO jobAlerts;
  private FeatureGroupAlertDTO featureGroupAlerts;

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

  public void setFeatureGroupAlerts(
      FeatureGroupAlertDTO featureGroupAlerts) {
    this.featureGroupAlerts = featureGroupAlerts;
  }
}
