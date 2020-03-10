/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.jobs;

import io.hops.hopsworks.persistence.entity.featurestore.jobs.FeaturestoreJob;
import io.hops.hopsworks.persistence.entity.jobs.history.Execution;
import io.hops.hopsworks.persistence.entity.jobs.description.Jobs;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

/**
 * DTO containing the human-readable information of a feature store job, can be converted to JSON or
 * XML representation using jaxb.
 */
@XmlRootElement
@XmlType(propOrder = {"jobId", "jobName", "lastComputed", "jobStatus", "featurestoreId", "featuregroupId",
  "trainingDatasetId"})
public class FeaturestoreJobDTO {
  
  private Integer jobId;
  private String jobName;
  private Date lastComputed;
  private String jobStatus;
  private Integer featurestoreId;
  private Integer featuregroupId = null;
  private Integer trainingDatasetId = null;
  
  public FeaturestoreJobDTO() {}
  
  public FeaturestoreJobDTO(Integer jobId, String jobName, Date lastComputed, String jobStatus,
    Integer featurestoreId, Integer featuregroupId, Integer trainingDatasetId) {
    this.jobId = jobId;
    this.jobName = jobName;
    this.lastComputed = lastComputed;
    this.jobStatus = jobStatus;
    this.featurestoreId = featurestoreId;
    this.featuregroupId = featuregroupId;
    this.trainingDatasetId = trainingDatasetId;
  }
  
  public FeaturestoreJobDTO(FeaturestoreJob featurestoreJob) {
    this.jobId = featurestoreJob.getJob().getId();
    this.jobName = featurestoreJob.getJob().getName();
    extractLastComputed(featurestoreJob.getJob());
    if(featurestoreJob.getFeaturegroup() != null){
      this.featurestoreId = featurestoreJob.getFeaturegroup().getFeaturestore().getId();
      this.featuregroupId = featurestoreJob.getFeaturegroup().getId();
      this.trainingDatasetId = null;
    } else {
      this.featurestoreId = featurestoreJob.getTrainingDataset().getFeaturestore().getId();
      this.trainingDatasetId = featurestoreJob.getTrainingDataset().getId();
      this.featuregroupId = null;
    }
  }
  
  @XmlElement
  public Integer getJobId() {
    return jobId;
  }
  
  public void setJobId(Integer jobId) {
    this.jobId = jobId;
  }
  
  @XmlElement
  public String getJobName() {
    return jobName;
  }
  
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }
  
  @XmlElement
  public Date getLastComputed() {
    return lastComputed;
  }
  
  public void setLastComputed(Date lastComputed) {
    this.lastComputed = lastComputed;
  }
  
  @XmlElement
  public String getJobStatus() {
    return jobStatus;
  }
  
  public void setJobStatus(String jobStatus) {
    this.jobStatus = jobStatus;
  }
  
  @XmlElement
  public Integer getFeaturestoreId() {
    return featurestoreId;
  }
  
  public void setFeaturestoreId(Integer featurestoreId) {
    this.featurestoreId = featurestoreId;
  }
  
  @XmlElement
  public Integer getFeaturegroupId() {
    return featuregroupId;
  }
  
  public void setFeaturegroupId(Integer featuregroupId) {
    this.featuregroupId = featuregroupId;
  }
  
  @XmlElement
  public Integer getTrainingDatasetId() {
    return trainingDatasetId;
  }
  
  public void setTrainingDatasetId(Integer trainingDatasetId) {
    this.trainingDatasetId = trainingDatasetId;
  }
  
  private void extractLastComputed(Jobs job) {
    Collection<Execution> executions = job.getExecutions();
    this.lastComputed = null;
    this.jobStatus = null;
    if (!executions.isEmpty()) {
      Optional<Execution> optionalMax = executions.stream().max(Comparator.comparing(Execution::getSubmissionTime));
      if (optionalMax.isPresent()) {
        Execution latestExecution = optionalMax.get();
        this.lastComputed = latestExecution.getSubmissionTime();
        this.jobStatus = latestExecution.getFinalStatus().toString();
      }
    }
  }
  
  @Override
  public String toString() {
    return "FeaturestoreJobDTO{" +
      "jobId=" + jobId +
      ", jobName='" + jobName + '\'' +
      ", lastComputed=" + lastComputed +
      ", jobStatus='" + jobStatus + '\'' +
      ", featurestoreId=" + featurestoreId +
      ", featuregroupId=" + featuregroupId +
      ", trainingDatasetId=" + trainingDatasetId +
      '}';
  }
}
