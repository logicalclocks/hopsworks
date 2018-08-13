/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.common.dao.jobs;

import io.hops.hopsworks.common.dao.jobhistory.Execution;
import java.io.Serializable;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;

@Entity
@Table(name = "jobs_history",
        catalog = "hopsworks",
        schema = "")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "JobsHistory.findAll",
          query = "SELECT j FROM JobsHistory j"),
  @NamedQuery(name = "JobsHistory.findByAppId",
          query
          = "SELECT j FROM JobsHistory j WHERE j.appId = :appId AND j.appId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findByProjectId",
          query
          = "SELECT j FROM JobsHistory j WHERE j.projectId = :projectId AND j.projectId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findByJobType",
          query = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType"),
  @NamedQuery(name = "JobsHistory.findByClassName",
          query = "SELECT j FROM JobsHistory j WHERE j.className = :className"),
  @NamedQuery(name = "JobsHistory.findByBlocksInHdfs",
          query
          = "SELECT j FROM JobsHistory j WHERE j.inputBlocksInHdfs = :inputBlocksInHdfs"),
  @NamedQuery(name = "JobsHistory.findByExecutionDuration",
          query
          = "SELECT j FROM JobsHistory j WHERE j.executionDuration = :executionDuration"),
  @NamedQuery(name = "JobsHistory.findWithVeryHighSimilarity",
          query
          = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className "
          + "AND j.arguments = :arguments AND j.jarFile = :jarFile AND j.inputBlocksInHdfs = :inputBlocksInHdfs "
          + "AND j.finalStatus = :finalStatus AND j.appId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findWithHighSimilarity",
          query
          = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className "
          + "AND j.arguments = :arguments AND j.jarFile = :jarFile AND j.finalStatus = :finalStatus "
          + "AND j.appId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findWithMediumSimilarity",
          query
          = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className "
          + "AND j.jarFile = :jarFile AND j.finalStatus = :finalStatus AND j.appId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findWithLowSimilarity",
          query
          = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className "
          + "AND j.finalStatus = :finalStatus AND j.appId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findWithVeryHighSimilarityFilter",
          query
          = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className "
          + "AND j.arguments = :arguments AND j.jarFile = :jarFile AND j.inputBlocksInHdfs = :inputBlocksInHdfs "
          + "AND j.projectName = :projectName AND j.jobName = :jobName AND j.userEmail = :userEmail "
          + "AND j.finalStatus = :finalStatus AND j.appId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findWithHighSimilarityFilter",
          query
          = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className "
          + "AND j.arguments = :arguments AND j.jarFile = :jarFile "
          + "AND j.projectName = :projectName AND j.jobName = :jobName AND j.userEmail = :userEmail "
          + "AND j.finalStatus = :finalStatus AND j.appId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findWithMediumSimilarityFilter",
          query
          = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className "
          + "AND j.jarFile = :jarFile AND j.projectName = :projectName AND j.jobName = :jobName "
          + "AND j.userEmail = :userEmail "
          + "AND j.finalStatus = :finalStatus AND j.appId IS NOT NULL"),
  @NamedQuery(name = "JobsHistory.findWithLowSimilarityFilter",
          query
          = "SELECT j FROM JobsHistory j WHERE j.jobType = :jobType AND j.className = :className "
          + "AND j.projectName = :projectName AND j.jobName = :jobName AND j.userEmail = :userEmail "
          + "AND j.finalStatus = :finalStatus AND j.appId IS NOT NULL")})
public class JobsHistory implements Serializable {

  private static long serialVersionUID = 1L;

  /**
   * @return the serialVersionUID
   */
  public static long getSerialVersionUID() {
    return serialVersionUID;
  }

  /**
   * @param aSerialVersionUID the serialVersionUID to set
   */
  public static void setSerialVersionUID(long aSerialVersionUID) {
    serialVersionUID = aSerialVersionUID;
  }

  @Id
  @Basic(optional = false)
  @Column(name = "execution_id")
  private Integer executionId;

  @Basic(optional = false)
  @NotNull
  @Column(name = "job_id")
  private int jobId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "jar_file")
  private String jarFile;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "app_id")
  private String appId;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "job_type")
  private String jobType;

  @Basic(optional = false)
  @NotNull
  @Size(min = 1,
          max = 255)
  @Column(name = "class_name")
  private String className;

  @Basic(optional = false)
  @NotNull
  @Column(name = "arguments")
  private String arguments;

  @Basic(optional = false)
  @NotNull
  @Column(name = "input_blocks_in_hdfs")
  private String inputBlocksInHdfs;

  @Basic(optional = false)
  @NotNull
  @Column(name = "am_memory")
  private int amMemory;

  @Basic(optional = false)
  @NotNull
  @Column(name = "am_Vcores")
  private int amVcores;

  @Column(name = "execution_duration")
  private long executionDuration;

  @Basic(optional = false)
  @NotNull
  @Column(name = "user_email")
  private String userEmail;

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_name")
  private String projectName;

  @Basic(optional = false)
  @NotNull
  @Column(name = "job_name")
  private String jobName;

  @Basic(optional = false)
  @NotNull
  @Column(name = "state")
  @Enumerated(EnumType.STRING)
  private JobState state;

  @Basic(optional = false)
  @NotNull
  @Column(name = "final_status")
  @Enumerated(EnumType.STRING)
  private JobFinalStatus finalStatus;

  @Basic(optional = false)
  @NotNull
  @Column(name = "project_id")
  private Integer projectId;

  public JobsHistory() {
  }

  public JobsHistory(int executionId, int jobId, String jarFile, String appId,
          Jobs jobDesc,
          String inputBlocksInHdfs, SparkJobConfiguration configuration,
          String userEmail) {
    this.executionId = executionId;
    this.jobId = jobId;
    this.jarFile = jarFile;
    this.appId = appId;
    this.jobType = jobDesc.getJobType().toString();
    this.inputBlocksInHdfs = inputBlocksInHdfs;
    this.amMemory = configuration.getAmMemory();
    this.amVcores = configuration.getAmVCores();
    this.arguments = configuration.getArgs() == null ? "-" : configuration.
            getArgs();
    this.className = configuration.getMainClass();
    this.executionDuration = -1;
    this.userEmail = userEmail;
    this.projectName = jobDesc.getProject().getName();
    this.jobName = jobDesc.getName();
    this.state = JobState.NEW;
    this.finalStatus = JobFinalStatus.UNDEFINED;
    this.projectId = jobDesc.getProject().getId();
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getJobType() {
    return jobType;
  }

  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getArguments() {
    return arguments;
  }

  public void setArguments(String argument) {
    this.arguments = argument;
  }

  public String getInputBlocksInHdfs() {
    return inputBlocksInHdfs;
  }

  public void setInputBlocksInHdfs(String inputBlocksInHdfs) {
    this.inputBlocksInHdfs = inputBlocksInHdfs;
  }

  public long getExecutionDuration() {
    return executionDuration;
  }

  public void setExecutionDuration(long executionDuration) {
    this.executionDuration = executionDuration;
  }

  /**
   * @return the userEmail
   */
  public String getUserEmail() {
    return userEmail;
  }

  /**
   * @param userEmail the userEmail to set
   */
  public void setUserEmail(String userEmail) {
    this.userEmail = userEmail;
  }

  /**
   * @return the projectName
   */
  public String getProjectName() {
    return projectName;
  }

  /**
   * @param projectName the projectName to set
   */
  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  /**
   * @return the jobName
   */
  public String getJobName() {
    return jobName;
  }

  /**
   * @param jobName the jobName to set
   */
  public void setJobName(String jobName) {
    this.jobName = jobName;
  }

  /**
   * @return the state
   */
  public JobState getState() {
    return state;
  }

  /**
   * @param state the state to set
   */
  public void setState(JobState state) {
    this.state = state;
  }

  /**
   * @return the finalStatus
   */
  public JobFinalStatus getFinalStatus() {
    return finalStatus;
  }

  /**
   * @param finalStatus the finalStatus to set
   */
  public void setFinalStatus(JobFinalStatus finalStatus) {
    this.finalStatus = finalStatus;
  }

  /**
   * @return the amMemory
   */
  public int getAmMemory() {
    return amMemory;
  }

  /**
   * @param amMemory the amMemory to set
   */
  public void setAmMemory(int amMemory) {
    this.amMemory = amMemory;
  }

  /**
   * @return the amVcores
   */
  public int getAmVcores() {
    return amVcores;
  }

  /**
   * @param amVcores the amVcores to set
   */
  public void setAmVcores(int amVcores) {
    this.amVcores = amVcores;
  }

  /**
   * @return the executionId
   */
  public Integer getExecutionId() {
    return executionId;
  }

  /**
   * @param executionId the executionId to set
   */
  public void setExecutionId(Integer executionId) {
    this.executionId = executionId;
  }

  /**
   * @return the jobId
   */
  public int getJobId() {
    return jobId;
  }

  /**
   * @param jobId the jobId to set
   */
  public void setJobId(int jobId) {
    this.jobId = jobId;
  }

  /**
   * @return the jarFile
   */
  public String getJarFile() {
    return jarFile;
  }

  /**
   * @param jarFile the jarFile to set
   */
  public void setJarFile(String jarFile) {
    this.jarFile = jarFile;
  }

  public int getProjectId() {
    return projectId;
  }

  public void setProjectId(int projectId) {
    this.projectId = projectId;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (getExecutionId() != null ? getExecutionId().hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Execution)) {
      return false;
    }
    JobsHistory other = (JobsHistory) object;
    if ((this.getExecutionId() == null && other.getExecutionId() != null)
            || (this.getExecutionId() != null && !this.executionId.
            equals(other.executionId))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "JobsHistory " + getExecutionId();
  }

}
