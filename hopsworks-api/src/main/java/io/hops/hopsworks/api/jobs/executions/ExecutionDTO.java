/*
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
 */
package io.hops.hopsworks.api.jobs.executions;

import io.hops.hopsworks.api.user.UserDTO;
import io.hops.hopsworks.common.api.RestDTO;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.List;

@XmlRootElement
public class ExecutionDTO extends RestDTO<ExecutionDTO> {
  
  private Integer id;
  private Date submissionTime;
  private JobState state;
  private String stdoutPath;
  private String stderrPath;
  private String appId;
  private String hdfsUser;
  private JobFinalStatus finalStatus;
  private Float progress;
  private UserDTO user;
  private List<String> filesToRemove;
  private Long duration;
  
  public ExecutionDTO() {
  }
  
  public Integer getId() {
    return id;
  }
  
  public void setId(Integer id) {
    this.id = id;
  }
  
  public Date getSubmissionTime() {
    return submissionTime;
  }
  
  public void setSubmissionTime(Date submissionTime) {
    this.submissionTime = submissionTime;
  }
  
  public JobState getState() {
    return state;
  }
  
  public void setState(JobState state) {
    this.state = state;
  }
  
  public String getStdoutPath() {
    return stdoutPath;
  }
  
  public void setStdoutPath(String stdoutPath) {
    this.stdoutPath = stdoutPath;
  }
  
  public String getStderrPath() {
    return stderrPath;
  }
  
  public void setStderrPath(String stderrPath) {
    this.stderrPath = stderrPath;
  }
  
  public String getAppId() {
    return appId;
  }
  
  public void setAppId(String appId) {
    this.appId = appId;
  }
  
  public String getHdfsUser() {
    return hdfsUser;
  }
  
  public void setHdfsUser(String hdfsUser) {
    this.hdfsUser = hdfsUser;
  }
  
  public JobFinalStatus getFinalStatus() {
    return finalStatus;
  }
  
  public void setFinalStatus(JobFinalStatus finalStatus) {
    this.finalStatus = finalStatus;
  }
  
  public Float getProgress() {
    return progress;
  }
  
  public void setProgress(Float progress) {
    this.progress = progress;
  }
  
  public UserDTO getUser() {
    return user;
  }
  
  public void setUser(UserDTO user) {
    this.user = user;
  }
  
  public List<String> getFilesToRemove() {
    return filesToRemove;
  }
  
  public void setFilesToRemove(List<String> filesToRemove) {
    this.filesToRemove = filesToRemove;
  }
  
  public Long getDuration() {
    return duration;
  }
  
  public void setDuration(Long duration) {
    this.duration = duration;
  }
  
}
