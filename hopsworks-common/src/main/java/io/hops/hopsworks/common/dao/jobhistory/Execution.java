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

package io.hops.hopsworks.common.dao.jobhistory;

import io.hops.hopsworks.common.dao.jobs.FilesToRemove;
import io.hops.hopsworks.common.dao.jobs.JobInputFile;
import io.hops.hopsworks.common.dao.jobs.JobOutputFile;
import io.hops.hopsworks.common.jobs.jobhistory.JobFinalStatus;
import io.hops.hopsworks.common.jobs.jobhistory.JobState;
import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;
import io.hops.hopsworks.common.dao.jobs.description.Jobs;
import io.hops.hopsworks.common.dao.user.Users;
import java.util.ArrayList;
import java.util.List;

/**
 * An Execution is an instance of execution of a specific Jobs.
 */
@Entity
@Table(name = "hopsworks.executions")
@XmlRootElement
@NamedQueries({
  @NamedQuery(name = "Execution.findAll",
          query
          = "SELECT e FROM Execution e"),
  @NamedQuery(name = "Execution.findById",
          query
          = "SELECT e FROM Execution e WHERE e.id = :id"),
  @NamedQuery(name = "Execution.findBySubmissionTime",
          query
          = "SELECT e FROM Execution e WHERE e.submissionTime = :submissionTime"),
  @NamedQuery(name = "Execution.findByJobIdAndSubmissionTime",
          query
          = "SELECT e FROM Execution e WHERE e.job = :job AND e.submissionTime = :submissionTime"),
  @NamedQuery(name = "Execution.findByState",
          query
          = "SELECT e FROM Execution e WHERE e.state = :state"),
  @NamedQuery(name = "Execution.findByStates",
          query
          = "SELECT e FROM Execution e WHERE e.state in :states"),
  @NamedQuery(name = "Execution.findByStdoutPath",
          query
          = "SELECT e FROM Execution e WHERE e.stdoutPath = :stdoutPath"),
  @NamedQuery(name = "Execution.findByStderrPath",
          query
          = "SELECT e FROM Execution e WHERE e.stderrPath = :stderrPath"),
  @NamedQuery(name = "Execution.findByAppId",
          query
          = "SELECT e FROM Execution e WHERE e.appId = :appId"),
  @NamedQuery(name = "Execution.findByProjectAndType",
          query
          = "SELECT e FROM Execution e WHERE e.job.type = :type AND e.job.project "
          + "= :project ORDER BY e.submissionTime DESC"),
  @NamedQuery(name = "Execution.findByJob",
          query
          = "SELECT e FROM Execution e WHERE e.job = :job ORDER BY e.submissionTime DESC"),
  @NamedQuery(name = "Execution.findByProjectAndJobId",
          query
          = "SELECT e FROM Execution e WHERE e.job.id = :jobid AND e.job.project "
          + "= :project ORDER BY e.submissionTime DESC"),
  @NamedQuery(name = "Execution.findJobsForExecutionInState",
          query
          = "SELECT DISTINCT e.job FROM Execution e WHERE e.job.project = :project "
      + "AND e.state IN :stateCollection ORDER BY e.submissionTime DESC"),
  @NamedQuery(name = "Execution.findUserJobsForExecutionInState",
      query
      = "SELECT DISTINCT e.job FROM Execution e WHERE e.job.project = :project AND e.hdfsUser = :hdfsUser "
      + "AND e.state IN :stateCollection ORDER BY e.submissionTime DESC"),
  @NamedQuery(name = "Execution.findUserJobsIdsForExecutionInState",
      query
      = "SELECT DISTINCT e.job FROM Execution e WHERE e.job.id IN :jobids AND e.job.project = :project "
      + "AND e.hdfsUser = :hdfsUser AND e.state IN :stateCollection ORDER BY e.submissionTime DESC")})
public class Execution implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Basic(optional = false)
  @Column(name = "id")
  private Integer id;

  @Basic(optional = false)
  @NotNull
  @Column(name = "submission_time")
  @Temporal(TemporalType.TIMESTAMP)
  private Date submissionTime;

  @Basic(optional = false)
  @NotNull
  @Column(name = "state")
  @Enumerated(EnumType.STRING)
  private JobState state;

  @Column(name = "execution_start")
  private long executionStart;

  @Column(name = "execution_stop")
  private long executionStop;
  
  @Size(max = 255)
  @Column(name = "stdout_path")
  private String stdoutPath;

  @Size(max = 255)
  @Column(name = "stderr_path")
  private String stderrPath;

  @Size(max = 30)
  @Column(name = "app_id")
  private String appId;

  @Size(max = 255)
  @Column(name = "hdfs_user")
  private String hdfsUser;
  
  @Basic(optional = false)
  @NotNull
  @Column(name = "finalStatus")
  @Enumerated(EnumType.STRING)
  private JobFinalStatus finalStatus;

  @Basic(optional = false)
  @NotNull
  @Column(name = "progress")
  private float progress;

  @JoinColumn(name = "job_id",
          referencedColumnName = "id")
  @ManyToOne(optional = false)
  private Jobs job;

  @JoinColumn(name = "user",
          referencedColumnName = "email")
  @ManyToOne(optional = false)
  private Users user;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "execution")
  private Collection<JobOutputFile> jobOutputFileCollection;

  @OneToMany(cascade = CascadeType.ALL,
          mappedBy = "execution")
  private Collection<JobInputFile> jobInputFileCollection;
  
  @OneToMany(cascade = CascadeType.ALL, mappedBy = "execution")
  private Collection<FilesToRemove> filesToRemove;

  public Execution() {
  }

  public Execution(JobState state, Jobs job, Users user, String hdfsUser) {
    this(state, job, user, new Date(), hdfsUser);
  }

  public Execution(JobState state, Jobs job, Users user,
          Date submissionTime, String hdfsUser) {
    this(state, job, user, submissionTime, null, null, hdfsUser);
  }

  public Execution(JobState state, Jobs job, Users user,
          String stdoutPath,
          String stderrPath, String hdfsUser) {
    this(state, job, user, new Date(), stdoutPath, stderrPath, hdfsUser);
  }

  public Execution(JobState state, Jobs job, Users user,
          Date submissionTime,
          String stdoutPath, String stderrPath, String hdfsUser) {
    this(state, job, user, submissionTime, stdoutPath, stderrPath, null,hdfsUser);
  }

  public Execution(JobState state, Jobs job, Users user,
          String stdoutPath,
          String stderrPath, Collection<JobInputFile> input,
          JobFinalStatus finalStatus, float progress, String hdfsUser) {
    this(state, job, user, new Date(), stdoutPath, stderrPath, input,
            finalStatus, progress, hdfsUser);
  }

  public Execution(Execution t) {
    this(t.state, t.job, t.user, t.submissionTime, t.stdoutPath, t.stderrPath,
            t.jobInputFileCollection, t.hdfsUser);
    this.id = t.id;
    this.appId = t.appId;
    this.jobOutputFileCollection = t.jobOutputFileCollection;
    this.executionStart = t.executionStart;
    this.executionStop = t.executionStop;
    this.filesToRemove = t.filesToRemove;
  }

  public Execution(JobState state, Jobs job, Users user,
          Date submissionTime,
          String stdoutPath, String stderrPath, Collection<JobInputFile> input, String hdfsUser) {
    this.submissionTime = submissionTime;
    this.state = state;
    this.stdoutPath = stdoutPath;
    this.stderrPath = stderrPath;
    this.job = job;
    this.user = user;
    this.hdfsUser = hdfsUser;
    this.jobInputFileCollection = input;
    this.executionStart = -1;
  }

  public Execution(JobState state, Jobs job, Users user,
          Date submissionTime,
          String stdoutPath, String stderrPath, Collection<JobInputFile> input,
          JobFinalStatus finalStatus, float progress, String hdfsUser) {
    this.submissionTime = submissionTime;
    this.state = state;
    this.stdoutPath = stdoutPath;
    this.stderrPath = stderrPath;
    this.job = job;
    this.user = user;
    this.hdfsUser = hdfsUser;
    this.jobInputFileCollection = input;
    this.finalStatus = finalStatus;
    this.progress = progress;
    this.executionStart = -1;
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

  public JobFinalStatus getFinalStatus() {
    return finalStatus;
  }

  public void setFinalStatus(JobFinalStatus finalStatus) {
    this.finalStatus = finalStatus;
  }

  public float getProgress() {
    return progress;
  }

  public void setProgress(float progress) {
    this.progress = progress;
  }

  public long getExecutionDuration() {
    if (executionStart == -1) {
      return 0;
    }
    if (executionStop > executionStart) {
      return executionStop - executionStart;
    } else {
      return System.currentTimeMillis() - executionStart;
    }
  }

  public void setExecutionStart(long executionStart) {
    this.executionStart = executionStart;
    this.executionStop = executionStart;
  }

  public void setExecutionStop(long executionStop) {
    this.executionStop = executionStop;
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

  public Jobs getJob() {
    return job;
  }

  public void setJob(Jobs job) {
    this.job = job;
  }

  @Override
  public int hashCode() {
    int hash = 0;
    hash += (id != null ? id.hashCode() : 0);
    return hash;
  }

  @Override
  public boolean equals(Object object) {
    // TODO: Warning - this method won't work in the case the id fields are not set
    if (!(object instanceof Execution)) {
      return false;
    }
    Execution other = (Execution) object;
    if ((this.id == null && other.id != null) || (this.id != null && !this.id.
            equals(other.id))) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Execution " + id + " of job " + job;
  }

  public Users getUser() {
    return user;
  }

  public void setUser(Users user) {
    this.user = user;
  }
  
  public String getHdfsUser() {
    return hdfsUser;
  }
  
  public void setHdfsUser(String hdfsUser){
    this.hdfsUser = hdfsUser;
  }

  public void setFilesToRemove(List<String> filesToRemove){
    List<FilesToRemove> toRemove = new ArrayList<>();
    for(String fileToRemove: filesToRemove){
      toRemove.add(new FilesToRemove(id, fileToRemove));
    }
    this.filesToRemove = toRemove;
  }
  
  public List<String> getFilesToRemove(){
    List<String> toRemove = new ArrayList<>();
    for(FilesToRemove fileToRemove : filesToRemove){
      toRemove.add(fileToRemove.getFilesToRemovePK().getFilepath());
    }
    return toRemove;
  }
  
  public Collection<JobOutputFile> getJobOutputFileCollection() {
    return jobOutputFileCollection;
  }

  public void setJobOutputFileCollection(
          Collection<JobOutputFile> jobOutputFileCollection) {
    this.jobOutputFileCollection = jobOutputFileCollection;
  }

  public Collection<JobInputFile> getJobInputFileCollection() {
    return jobInputFileCollection;
  }

  public void setJobInputFileCollection(
          Collection<JobInputFile> jobInputFileCollection) {
    this.jobInputFileCollection = jobInputFileCollection;
  }

}
